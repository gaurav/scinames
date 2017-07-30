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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
 * A ChangeGenerator produces changes based on genus composition.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class GenusChangesFromComposition implements ChangeGenerator {
	private static final Logger LOGGER = Logger.getLogger(GenusChangesFromComposition.class.getSimpleName());
	
	public GenusChangesFromComposition() {
	}
	

	@Override
	public String getName() {
		return "Genus changes from previous composition";
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
		return project.getDatasets().stream().flatMap(ds -> generate(project, ds));
	}

	public Stream<PotentialChange> generate(Project project, Dataset ds) {
		return getChanges(project, ds);
	}	
	
	public Stream<PotentialChange> getChanges(Project p, Dataset ds) {
		// Nothing to do if there is no previous dataset;
		if(ds.getPreviousDataset() == null)
			return Stream.empty();
		
		LOGGER.info("getChanges(" + p + ", " + ds + ")");
		
		Dataset prevDataset = ds.getPreviousDataset();
		
		// Index genus compositions in this dataset and in the previous dataset.
		LOGGER.info("Indexing " + ds + " by genus");
		Map<Name, List<Name>> namesByGenus = ds.getNamesInAllRows()
			.stream()
			.collect(Collectors.groupingBy(n -> n.asGenus().findFirst().get()));
		LOGGER.info("Completed indexing " + ds + " by genus");
		
		LOGGER.info("Indexing " + prevDataset + " by genus");
		Map<Name, List<Name>> prevNamesByGenus = prevDataset.getNamesInAllRows()
			.stream()
			.collect(Collectors.groupingBy(n -> n.asGenus().findFirst().get()));
		LOGGER.info("Completed indexing " + prevDataset + " by genus");

		// For every genus previously defined, we need to figure out where names ended up.
		// To do that correctly, we need to put the newly added genera in.
		Set<Name> newlyAddedGenera = namesByGenus.keySet();
		newlyAddedGenera.removeAll(prevNamesByGenus.keySet());
		newlyAddedGenera.stream().forEach(n -> prevNamesByGenus.put(n, new ArrayList<>()));
		
		// Go through every previously existing genus (as well as newly added genera).
		return prevNamesByGenus.keySet().stream()
			.flatMap(genus -> {
				// Figure out previous and current composition of this genus.
				Set<Name> prevNames = prevNamesByGenus.get(genus).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toSet());
				Set<Name> currentNames = new HashSet<>();
				if(namesByGenus.containsKey(genus))
					currentNames = namesByGenus.get(genus).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toSet());
				
				// Maybe it's identical
				if(prevNames.equals(currentNames)) {
					return Stream.empty();
				// Maybe it ain't.
				} else {
					return Stream.of(new PotentialChange(
						ds, 
						ChangeType.of("changed"), 
						Stream.of(genus), // old name 
						Stream.of(genus), // new name
						GenusChangesFromComposition.class, 
						"Genus changed from " + prevNames + " to " + currentNames
					));
				}
			});
		
	}

}