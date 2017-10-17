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
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ggvaidya.scinames.model.Change;
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
		
		errors.addAll(reportContradictoryChangesInTheSameDataset(p).collect(Collectors.toList()));
		errors.addAll(reportChangesThatHaveNoEffectInDataset(p).collect(Collectors.toList()));
		errors.addAll(reportUnmappedRows(p).collect(Collectors.toList()));
		
		return errors.stream();
	}
	
	private Stream<ValidationError<Change>> reportChangesThatHaveNoEffectInDataset(Project p) {
		return p.getDatasets().stream().flatMap(ds -> reportChangesThatHaveNoEffectInDataset(p, ds));
	}
	
	private Stream<ValidationError<Change>> reportChangesThatHaveNoEffectInDataset(Project p, Dataset ds) {
		Optional<Dataset> optPrevDataset = ds.getPreviousDataset();
		if(!optPrevDataset.isPresent()) {
			LOGGER.info("Skipping dataset '" + ds + "' as it has no previous dataset.");
			return Stream.empty();
		}
		
		Dataset prevDataset = optPrevDataset.get();
		
		// What are all the binomial changes that took place between these two datasets?
		Set<Name> namesInPrev = prevDataset.getRecognizedNames(p).collect(Collectors.toSet());
		Set<Name> namesInCurrent = ds.getRecognizedNames(p).collect(Collectors.toSet());
		
		// Added and deleted binomial names.
		Set<Name> addedNames = new HashSet<>(namesInCurrent);
		namesInCurrent.removeAll(namesInPrev);
		
		Set<Name> deletedNames = new HashSet<>(namesInPrev);
		namesInPrev.removeAll(namesInCurrent);
		
		return ds.getChanges(p).flatMap(ch -> {
			List<ValidationError<Change>> errors = new LinkedList<>();
			
			Set<Name> from = ch.getFrom();
			Set<Name> to = ch.getTo();
			
			// Now, make sure that we don't have any:
			//	- 	Changes adding names that don't need to be added.
			//		Note that it's fine if the same name is added and deleted, as in a split.
			for(Name n: to) {
				if(!addedNames.contains(n) && !from.contains(n))
					errors.add(new ValidationError<Change>(Level.SEVERE, this, p, "Name '" + n + "' added but already recognized in dataset " + ds + " in change " + ch, ch));
			}
			
			//	- 	Changes deleting names that don't need to be deleted.
			// 		Note that it's fine if the same name is added and deleted, as in a lump.
			for(Name n: from) {
				if(!deletedNames.contains(n) && !to.contains(n))
					errors.add(new ValidationError<Change>(Level.SEVERE, this, p, "Name '" + n + "' deleted but already recognized in dataset " + ds + " in change " + ch, ch));
			}
			
			return errors.stream();
		});
	}
	
	private Stream<ValidationError<Change>> reportContradictoryChangesInTheSameDataset(Project p) {
		return p.getDatasets().stream().flatMap(ds -> reportContradictoryChangesInDataset(p, ds));
	}
	
	private Stream<ValidationError<Change>> reportContradictoryChangesInDataset(Project p, Dataset ds) {
		Set<Name> namesAdded = new HashSet<>();
		Set<Name> namesDeleted = new HashSet<>();
		
		return ds.getChanges(p).flatMap(ch -> {
			// Validation errors we've found.
			List<ValidationError<Change>> validationErrors = new LinkedList<>();
			
			// Tracking names being added and deleted.
			Set<Name> changeDeletesNames = ch.getFrom();
			Set<Name> changeAddsNames = ch.getTo();
			
			// 1. No change should add a name that a previous change has already added.
			validationErrors.addAll(
				changeAddsNames.stream()
					.filter(namesAdded::contains)
					.map(n -> new ValidationError<Change>(Level.SEVERE, this, p, "Name '" + n + "' added multiple times in dataset " + ds + ", including in change " + ch, ch))
					.collect(Collectors.toList())
			);
			
			// 2. No change should delete a name that a previous change has already deleted.
			validationErrors.addAll(
				changeDeletesNames.stream()
					.filter(namesDeleted::contains)
					.map(n -> new ValidationError<Change>(Level.SEVERE, this, p, "Name '" + n + "' deleted multiple times in dataset " + ds + ", including in change " + ch, ch))
					.collect(Collectors.toList())
			);
			
			// 3. No change should add a name that a previous change has deleted.
			validationErrors.addAll(
				changeAddsNames.stream()
					.filter(namesDeleted::contains)
					.map(n -> new ValidationError<Change>(Level.SEVERE, this, p, "Name '" + n + "' deleted and added in dataset " + ds + ", added in change " + ch, ch))
					.collect(Collectors.toList())
			);
			
			// 4. No change should delete a name that a previous change has added.
			validationErrors.addAll(
				changeDeletesNames.stream()
					.filter(namesAdded::contains)
					.map(n -> new ValidationError<Change>(Level.SEVERE, this, p, "Name '" + n + "' added and deleted in dataset " + ds + ", deleted in change " + ch, ch))
					.collect(Collectors.toList())
			);
				
			// Add added and deleted names for future checks.
			namesAdded.addAll(changeAddsNames);
			namesDeleted.addAll(changeDeletesNames);
			
			// Ready to go!
			return validationErrors.stream();
		});
	}
	
	private Stream<ValidationError<Dataset>> reportUnmappedRows(Project p) {
		Map<Name, NameCluster> nameClustersByName = new HashMap<>(); 
		LinkedList<ValidationError<Dataset>> errors = new LinkedList<>();
		
		for(Dataset ds: p.getDatasets()) {
			Set<DatasetRow> allRows = new HashSet<>(ds.rowsProperty());
			Map<DatasetRow, Set<Name>> namesByRow = ds.getNamesByRow();
			Set<DatasetRow> rowsWithNames = namesByRow.keySet().stream()
				.filter(row -> !namesByRow.get(row).isEmpty())
				.collect(Collectors.toSet());
			
			Set<DatasetRow> rowsWithNamesButNotInDS = rowsWithNames.stream().filter(r -> !allRows.contains(r)).collect(Collectors.toSet());
			if(rowsWithNamesButNotInDS.size() != 0)
				throw new RuntimeException("SHOULD NEVER HAPPEN: Row found in name index but not in dataset! Rows: " + rowsWithNamesButNotInDS);

			Set<DatasetRow> rowsWithoutNames = allRows.stream().filter(r -> !rowsWithNames.contains(r)).collect(Collectors.toSet());
			LOGGER.info("Rows without names in dataset " + ds + ": " + rowsWithoutNames);
			rowsWithoutNames.stream()
				.forEach(row -> errors.add(new ValidationError<Dataset>(Level.WARNING, this, p, "No scientific name found for row; it will be excluded from analyses: " + row, ds)));
			
			/*
			if(ds.getDate().getYear() == 2011) {
				LOGGER.severe("2011 dataset: " + allRows.size() + " rows, " + rowsWithNames.size() + " rows with names");
				LOGGER.severe("2011 dataset, rows without names: " + rowsWithoutNames);
				return Stream.empty();
			}
			*/
		}
		
		return errors.stream();
	}
	
	@Override
	public String getName() {
		return "Dataset validator";
	}
}
