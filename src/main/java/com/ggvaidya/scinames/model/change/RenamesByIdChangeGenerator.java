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
		return project.getDatasets().stream().flatMap(ds -> getRenamesById(project, ds));
	}
	
	public Stream<PotentialChange> getRenamesById(Project p, Dataset ds) {
		// Nothing to do if there is no previous dataset;
		if(ds.getPreviousDataset() == null)
			return Stream.empty();
		
		LOGGER.info("getRenamesById(" + p + ", " + ds + ")");
		
		Dataset prevDataset = ds.getPreviousDataset();
		Set<Change> implicitChanges = ds.getImplicitChanges(p).collect(Collectors.toSet());
		
		// Okay, so what we do specifically is:
		//	- Find all implicit changes
		return implicitChanges.stream()
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
					
					// Look for a name in the previous checklist that shares the same ID as
					// in this checklist -- this might be a rename!
					return prevDataset.getRowsAsStream()
						.flatMap(row -> {
							String val = row.get(idColumn);
							
							if(val == null) return Stream.empty();
							if(myIds.contains(val)) {
								// A match! Here are some potential renames!
								Set<Name> prevNames = prevDataset.getNamesByRow().get(row);
								
								return prevNames.stream()
									.map(prevName -> new PotentialChange(ds, ChangeType.RENAME, Stream.of(prevName), Stream.of(nameAdded)));
							} else
								return Stream.empty();
						});
				} else if(ch.getType().equals(ChangeType.DELETION)) {
					if(ch.getFrom().size() != 1 || ch.getTo().size() != 0) return Stream.empty();
					Name nameDeleted = ch.getFromStream().findAny().get();
					
					// TODO the same sort of thing, I guess, except we want to make sure we don't
					// duplicate!
					return Stream.empty();
				} else
					return Stream.empty();
			});
	}
}