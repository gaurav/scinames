/*
 * Copyright (C) 2017 Gaurav Vaidya <gaurav@ggvaidya.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.ggvaidya.scinames.model.change;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.apache.poi.ss.formula.eval.NotImplementedException;

import com.ggvaidya.scinames.model.ChangeType;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.Synonymy;

/**
 * A "Renames from all additions" change generator 
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class RenamesFromIdsInData implements ChangeGenerator {
	private static final Logger LOGGER = Logger.getLogger(RenamesFromIdsInData.class.getSimpleName());
	
	private DatasetColumn idColumn;
	
	public RenamesFromIdsInData() {
	}
	
	@Override
	public String getName() {
		return "Find renames using identifiers in data";
	}

	@Override
	public boolean needsDatasetColumn() {
		return true;
	}

	@Override
	public void setDatasetColumn(DatasetColumn ds) {
		idColumn = ds;
	}
	
	@Override
	public Stream<PotentialChange> generate(Project project, Dataset ds) {
		throw new NotImplementedException("Cannot generate per dataset generate(" + project + ", " + ds + "): not supported");
	}
	
	@Override
	public Stream<PotentialChange> generate(Project project) {
		// No idColumn? This won't work, then.
		if(idColumn == null) return Stream.empty();
		
		// We need atleast one dataset.
		Dataset lastDataset = project.getLastDataset().orElse(null);
		if(lastDataset == null) return Stream.empty();
		
		// To start with, make a list of every name we have associated with every
		// unique ID in this project.
		Map<Name, List<Dataset>> datasetsByName = new HashMap<>();
		Map<String, Set<Name>> namesByIdentifier = new HashMap<>();
		for(Dataset ds: project.getDatasets()) {
			ds.getReferencedNames().forEach(n -> {
				if(!datasetsByName.containsKey(n)) datasetsByName.put(n, new LinkedList<>());
				datasetsByName.get(n).add(ds);
			});
			
			// We're only interested in rows that have a name associated with them.
			Map<Name, Set<DatasetRow>> rowsByName = ds.getRowsByName();
			for(Name n: rowsByName.keySet()) {
				for(DatasetRow row: rowsByName.get(n)) {
					String identifier = row.get(idColumn);
					if(identifier == null || identifier.trim().equals("")) continue;
					
					if(!namesByIdentifier.containsKey(identifier))
						namesByIdentifier.put(identifier, new HashSet<>());
					
					namesByIdentifier.get(identifier).add(n);
				}
			}
		}
		
		// Now, make a list of every partially overlapping set of names we know about.
		Map<Name, Set<Name>> partiallyOverlappingCircumscriptions = new HashMap<>();
		project.getChanges().forEach(ch -> {
			for(Name from: ch.getFrom()) {
				for(Name to: ch.getTo()) {
					// Add 'from' -> 'to'
					if(!partiallyOverlappingCircumscriptions.containsKey(from))
						partiallyOverlappingCircumscriptions.put(from, new HashSet<>());
					
					partiallyOverlappingCircumscriptions.get(from).add(to);
					
					// Add 'to' -> 'from'
					if(!partiallyOverlappingCircumscriptions.containsKey(to))
						partiallyOverlappingCircumscriptions.put(to, new HashSet<>());
					
					partiallyOverlappingCircumscriptions.get(to).add(from);					
				}
			}
		});
		
		// Finally, look at every identifier set, and see if there's a pair of names that aren't
		// associated with each other.
		Map<Name, Set<Name>> newSynonyms = new HashMap<>();
		
		for(String identifier: namesByIdentifier.keySet()) {
			Set<Name> names = namesByIdentifier.get(identifier);
			
			for(Name from: names) {
				for(Name to: names) {
					// We don't care if one is related to the other.
					if(from == to) continue;
					
					// We don't care if we already know about this.
					if(
						partiallyOverlappingCircumscriptions.containsKey(from) &&
						partiallyOverlappingCircumscriptions.get(from).contains(to)
					) continue;
					
					// Have we already reported this synonym in the opposite direction?
					if(
						newSynonyms.containsKey(to) && 
						newSynonyms.get(to).contains(from)
					) continue;
					
					// A relation we don't know about! Report, report!
					if(!newSynonyms.containsKey(from)) newSynonyms.put(from, new HashSet<>());
					newSynonyms.get(from).add(to);
				}
			}
		}
		
		// Write synonyms out!
		List<PotentialChange> potentialChanges = new LinkedList<>();
		for(Name from: newSynonyms.keySet()) {
			for(Name to: newSynonyms.get(from)) {
				Set<Dataset> datasets = new HashSet<>(datasetsByName.getOrDefault(from, new LinkedList<>()));
				datasets.retainAll(new HashSet<>(datasetsByName.getOrDefault(to, new LinkedList<>())));
				
				// Now, which was first dataset where we see both names?
				Dataset dataset = datasets.stream().sorted().findFirst().orElse(null);
				
				// No first dataset? Just use the last dataset.
				dataset = lastDataset;
				
				potentialChanges.add(
					new PotentialChange(
						dataset, 
						ChangeType.RENAME, 
						Stream.of(from), 
						Stream.of(to), 
						RenamesFromIdsInData.class, 
						"Found novel association between partially overlapping names"
					)
				);
			}
		}
		
		return potentialChanges.stream();
	}
}