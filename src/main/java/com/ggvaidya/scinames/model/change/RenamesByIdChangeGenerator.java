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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
 * A ChangeGenerator produces changes based on a project.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class RenamesByIdChangeGenerator implements ChangeGenerator {
	private static final Logger LOGGER = Logger.getLogger(RenamesByIdChangeGenerator.class.getSimpleName());
	
	private DatasetColumn idColumn;
	
	public RenamesByIdChangeGenerator(DatasetColumn col) {
		idColumn = col;
	}
	
	public Stream<PotentialChange> generate(Project project) {
		return project.getDatasets().stream().flatMap(ds -> generate(project, ds));
	}

	public Stream<PotentialChange> generate(Project project, Dataset ds) {
		return getRenamesById(project, ds);
	}	
	
	public Stream<PotentialChange> getRenamesById(Project p, Dataset ds) {
		// Nothing to do if there is no previous dataset;
		if(ds.getPreviousDataset() == null || idColumn == null)
			return Stream.empty();
		
		LOGGER.info("getRenamesById(" + p + ", " + ds + ")");
		
		Dataset prevDataset = ds.getPreviousDataset();
		
		// Index the values in the DatasetColumn for both this dataset and previous dataset.
		LOGGER.info("Indexing " + ds + " by column " + idColumn);
		Map<String, Set<Name>> rowsByNameDataset = ds.getRowsAsStream()
			.filter(row -> row.hasColumn(idColumn))
			.collect(Collectors.toMap(
				(DatasetRow row) -> row.get(idColumn),
				(DatasetRow row) -> ds.getNamesByRow().get(row)
			));
		LOGGER.info("Completed indexing " + ds + " by column " + idColumn);
		
		LOGGER.info("Indexing " + prevDataset + " by column " + idColumn);
		Map<String, Set<Name>> rowsByNamePrevDataset = prevDataset.getRowsAsStream()
			.filter(row -> row.hasColumn(idColumn))
			.collect(Collectors.toMap(
				(DatasetRow row) -> row.get(idColumn),
				(DatasetRow row) -> prevDataset.getNamesByRow().get(row)
			));
		LOGGER.info("Completed indexing " + prevDataset + " by column " + idColumn);
		
		Set<Change> implicitChanges = ds.getImplicitChanges(p).collect(Collectors.toSet());
		
		// Okay, so what we do specifically is:
		//	- Find all implicit changes
		return implicitChanges.parallelStream()
			// Identify only additions and deletions
			.filter(ch -> ch.getType().equals(ChangeType.ADDITION) || ch.getType().equals(ChangeType.DELETION))
			// For each addition or deletion, see if it's still recognized in the previous dataset.
			.flatMap(ch -> {
				if(ch.getType().equals(ChangeType.ADDITION)) {
					if(ch.getFrom().size() != 0 || ch.getTo().size() != 1) return Stream.empty();
					Name nameAdded = ch.getToStream().findAny().get();
					
					Set<String> myIds = ds.getRowsByName(nameAdded).stream()
						.map(row -> row.get(idColumn))
						.filter(val -> (val != null))
						.collect(Collectors.toSet());
					
					// LOGGER.fine(" - Searching " + prevDataset.rowsProperty().size() + " rows in the previous dataset " + prevDataset + " for one of these identifiers: " + myIds);
					
					return myIds.parallelStream()
						.filter(id -> rowsByNamePrevDataset.containsKey(id))
						.flatMap(id -> rowsByNamePrevDataset.get(id).stream())
						.map(prevName -> new Synonymy(prevName, nameAdded, ds));
					/*
					// Look for a name in the previous checklist that shares the same ID as/
					// in this checklist -- this might be a rename!
					return prevDataset.getRowsAsStream().parallel()
						.filter(row -> row.hasColumn(idColumn) && myIds.contains(row.get(idColumn)))
						.flatMap(row -> 
							prevDataset.getNamesByRow().get(row).stream()
								.map(prevName -> new Synonymy(prevName, nameAdded, ds))
						);
					*/
				} else if(ch.getType().equals(ChangeType.DELETION)) {
					if(ch.getFrom().size() != 1 || ch.getTo().size() != 0) return Stream.empty();
					Name nameDeleted = ch.getFromStream().findAny().get();
					
					Set<String> myIds = prevDataset.getRowsByName(nameDeleted).stream()
						.map(row -> row.get(idColumn))
						.filter(val -> (val != null))
						.collect(Collectors.toSet());

					// LOGGER.fine(" - Searching " + ds.rowsProperty().size() + " rows in the current dataset " + ds + " for one of these identifiers: " + myIds);
					
					return myIds.parallelStream()
						.filter(id -> rowsByNameDataset.containsKey(id))
						.flatMap(id -> rowsByNameDataset.get(id).stream())
						.map(currName -> new Synonymy(nameDeleted, currName, ds));
					
					/*
					// Look for a name in the current checklist that shares the same ID as
					// in this previous checklist -- this might be a rename!
					return ds.getRowsAsStream().parallel()
						.filter(row -> row.hasColumn(idColumn) && myIds.contains(row.get(idColumn)))							
						.flatMap(row -> 
							ds.getNamesByRow().get(row).stream()
								.map(currName -> new Synonymy(nameDeleted, currName, ds))
						);
					*/
				} else
					return Stream.empty();
			})
			// Remove duplicate synonymy objects.
			.distinct()
			// Produce a final list of potential renames.
			.map(syn -> new PotentialChange(syn.getDataset(), ChangeType.RENAME, Stream.of(syn.getFrom()), Stream.of(syn.getTo())));
	}
}