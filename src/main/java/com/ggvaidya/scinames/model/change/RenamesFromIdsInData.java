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

import java.util.Map;
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
	
	public RenamesFromIdsInData(DatasetColumn col) {
		idColumn = col;
	}
	
	public Stream<PotentialChange> generate(Project project) {
		return project.getDatasets().stream().flatMap(ds -> generate(project, ds));
	}

	public Stream<PotentialChange> generate(Project project, Dataset ds) {
		return getRenamesById(project, ds);
	}	
	
	public Stream<PotentialChange> getRenamesById(Project project, Dataset ds) {
		// No idColumn? This won't work, then.
		if(idColumn == null) return Stream.empty();
		
		// Which names were added in this database?
		Map<Name, Set<String>> identifiersByName = ds.getChanges(project)
			.filter(ch -> ch.getType().equals(ChangeType.ADDITION))
			.flatMap(ch -> ch.getTo().stream())
			.distinct()
			.collect(Collectors.toMap(
				n -> n, 
				n -> ds.getRowsByName(n).stream().flatMap(row -> {
					String val = row.get(idColumn);
					if(val == null) return Stream.empty();
					else return Stream.of(val);
				}).collect(Collectors.toSet())
			));
		
		LOGGER.info("Identifiers by name: " + 
				identifiersByName.keySet().size() + " names mapped to " + 
				identifiersByName.values().stream().distinct().count() + " distinct identifiers");
		
		// Do we see that identifier in *any* other rows anywhere in this project?
		return identifiersByName.entrySet().stream()
			.flatMap(es -> {
				Name name = es.getKey();
				Set<String> matchingIdentifiers = es.getValue();
				
				// TODO: we could make this a LOT faster if we can skip the row we started with,
				// but how?
				
				// Find all project rows that have one of these identifiers.
				return project.getRows()
					.filter(row -> {
						String val = row.get(idColumn);
						if(val == null) return false;
						if(matchingIdentifiers.contains(val)) return true;
						return false;
					})
					.distinct()
					.flatMap(row -> {
						// Turn each other name into a synonym. Since the original name
						// was ADDED here, we need to synonym from that name to this.

						return row.getDataset().getNamesInRow(row).stream()
							// A lot of these matches are, um, to the name we started out with. Filter those out.
							.filter(n -> (n != name))
							.map(
								otherName -> new Synonymy(
									otherName, name, ds,
									"Dataset " + row.getDataset().getCitation() + " has synonym under one of matching identifier " + matchingIdentifiers
								)
							);
					});
			})
			.distinct() // deduplicate synonyms
			.map(syn -> new PotentialChange(syn.getDataset(), ChangeType.RENAME, Stream.of(syn.getFrom()), Stream.of(syn.getTo()), RenamesFromIdsInData.class, syn.getNote()));
	}
}