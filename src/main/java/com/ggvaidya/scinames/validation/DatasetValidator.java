/*
 *
 *  Validator
 *  Copyright (C) 2017 Gaurav Vaidya
 *
 *  This file is part of SciNames.
 *
 *  SciNames is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  SciNames is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with SciNames.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.ggvaidya.scinames.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.Project;

/**
 * Validate entire datasets.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetValidator implements Validator {
	private Logger LOGGER = Logger.getLogger(DatasetValidator.class.getSimpleName());
	
	@SuppressWarnings("rawtypes")
	@Override
	public Stream<ValidationError> validate(Project p) {
		List<ValidationError> errors = new LinkedList<>();
		
		errors.addAll(reportUnmappedRows(p).collect(Collectors.toList()));
		
		// - This results in lots of spurious errors, so we're ignoring this for now.
		// errors.addAll(identifyNameClustersWithMultipleBinomials(p).collect(Collectors.toList()));
		
		return errors.stream();
	}
	
	private Stream<ValidationError<Dataset>> reportUnmappedRows(Project p) {
		Map<Name, NameCluster> nameClustersByName = new HashMap<>(); 
		LinkedList<ValidationError<Dataset>> errors = new LinkedList<>();
		
		for(Dataset ds: p.getDatasets()) {
			Set<DatasetRow> allRows = new HashSet<>(ds.rowsProperty());
			Set<DatasetRow> rowsWithNames = ds.getNamesByRow().keySet();
			
			Set<DatasetRow> rowsWithNamesButNotInDS = rowsWithNames.stream().filter(r -> !allRows.contains(r)).collect(Collectors.toSet());
			if(rowsWithNamesButNotInDS.size() != 0)
				throw new RuntimeException("SHOULD NEVER HAPPEN: Row found in name index but not in dataset! Rows: " + rowsWithNamesButNotInDS);

			Set<DatasetRow> rowsWithoutNames = allRows.stream().filter(r -> !rowsWithNames.contains(r)).collect(Collectors.toSet());
			LOGGER.info("Rows without names in dataset " + ds + ": " + rowsWithoutNames);
			rowsWithoutNames.stream()
				.forEach(row -> errors.add(new ValidationError<Dataset>(Level.SEVERE, this, p, "No scientific name found for row; it will be excluded from analyses: " + row, ds)));
		}
		
		return errors.stream();
	}
	
	@Override
	public String getName() {
		return "Dataset validator";
	}
}
