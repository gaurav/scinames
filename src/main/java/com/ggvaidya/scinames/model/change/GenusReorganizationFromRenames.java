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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.ChangeType;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.NameClusterManager;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.Synonymy;

/**
 * A ChangeGenerator produces changes based on genus reorganization. This is based on two things:
 * 	- 1. Synonyms of currently recognized names, that
 * 	- 2. Involve other currently recognized genera.
 * 
 * This measures how many genera:
 * 	- 1. Contains
 *  - 2. 
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class GenusReorganizationFromRenames implements ChangeGenerator {
	private static final Logger LOGGER = Logger.getLogger(GenusReorganizationFromRenames.class.getSimpleName());
	
	public GenusReorganizationFromRenames() {
	}
	

	@Override
	public String getName() {
		return "Genus reorganization based on synonymy";
	}

	@Override
	public boolean needsDatasetColumn() {
		return false;
	}

	@Override
	public void setDatasetColumn(DatasetColumn ds) {
		throw new UnsupportedOperationException("GenusChangesFromComposition does not have a dataset column to set");
	}
	
	public Stream<PotentialChange> generate(Project project) {
		throw new UnsupportedOperationException("GenusChangesFromComposition cannot work without a target dataset");
		// return project.getDatasets().stream().flatMap(ds -> generate(project, ds));
	}

	public Stream<PotentialChange> generate(Project project, Dataset ds) {
		return getChanges(project, ds);
	}	
	
	public Stream<PotentialChange> getChanges(Project p, Dataset ds) {
		NameClusterManager ncm = p.getNameClusterManager();
		
		// Step 1. Find all name clusters recognized by this dataset.
		Set<Name> currentlyRecognized = new HashSet<>(p.getRecognizedNames(ds).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toList()));
		Set<String> currentlyRecognizedGenera = currentlyRecognized.stream()
			.map(n -> n.getGenus())
			.collect(Collectors.toSet());
		
		// Step 2. For each name, find synonyms involving currently recognized genera.
		int synonymCurrentlyRecognized = 0;
		LinkedList<Synonymy> synonyms = new LinkedList<>();
		for(Name currentName: currentlyRecognized) {
			String nameGenus = currentName.getGenus();
			Optional<NameCluster> optCluster = ncm.getCluster(currentName);
			
			if(!optCluster.isPresent()) {
				throw new RuntimeException("Name '" + currentName + "' does not have a cluster! This should not happen.");
			}
			
			NameCluster cluster = optCluster.get();
			
			Set<Name> otherNames = cluster.getNames();
			for(Name otherName: otherNames) {
				String otherGenus = otherName.getGenus();
				
				// Don't care about our own genus.
				if(otherGenus.equals(nameGenus)) continue;
				
				// Ignore genera not currently recognized.
				if(!currentlyRecognizedGenera.contains(otherGenus)) continue;
				
				// Okay, hang on. Is this "other" name still recognized?
				// Because that would be weird.
				if(currentlyRecognized.contains(otherName)) {
					synonymCurrentlyRecognized++;
					LOGGER.severe("[" + synonymCurrentlyRecognized + "] Name '" + otherName + "' is a synonym of '" + currentName + "', but it is also currently recognized! Ignoring.");
					continue;
				}
				
				// We have a synonymy that involves a currently recognized genus!
				// The only problem is, we don't know where the synonymy came from.
				// Oh well.
				synonyms.add(new Synonymy(
					otherName, currentName, ds,
					otherName + " synonym of " + currentName + " but in a currently recognized genus (" + currentlyRecognizedGenera + ")"
				));
				
				// If we have a case where:
				//		Alpha alpha <--synonym of currently recognized--> Gamma alpha
				//		Gamma beta <--synonym of currently recognized--> Epsilon beta  
				// will it still recognize the connection? Nope, but it doesn't matter:
				// Gamma in the past is not Epsilon in the present.
			}
		}
		
		// Looking through synonyms will find complex synonymies that link back to
		// current names, but it'll miss lumps and splits. However, to fully integrate
		// those in, we'll need to "run" this forward AND track synonymies to figure out
		// which lumps and splits have ended up 
		
		// If we do end up doing that, the algorithm is essentially the same as NameClusters,
		// except we track ALL from-tos, not just renames.
		
		// Finally: convert synonyms into potential changes.
		return synonyms.stream()
			.map(syn ->
				new PotentialChange(
					syn.getDataset(), ChangeType.RENAME, 
					Stream.of(syn.getFrom()), 
					Stream.of(syn.getTo()), 
					GenusReorganizationFromRenames.class, 
					syn.getNote()
				)
			);
	}

}