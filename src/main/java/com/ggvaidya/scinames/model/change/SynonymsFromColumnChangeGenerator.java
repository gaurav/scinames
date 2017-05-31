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

import java.util.Arrays;
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
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.Synonymy;

/**
 * A ChangeGenerator that produces "renames" based on a synonym column.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class SynonymsFromColumnChangeGenerator implements ChangeGenerator {
	private static final Logger LOGGER = Logger.getLogger(SynonymsFromColumnChangeGenerator.class.getSimpleName());
	
	private DatasetColumn synonymColumn;
	
	public SynonymsFromColumnChangeGenerator(DatasetColumn col) {
		synonymColumn = col;
	}
	
	public Stream<PotentialChange> generate(Project project) {
		return project.getDatasets().stream().flatMap(ds -> generate(project, ds));
	}

	public Stream<PotentialChange> generate(Project project, Dataset ds) {
		return getRenamesFromSynonymColumn(project, ds);
	}	
	
	public Stream<PotentialChange> getRenamesFromSynonymColumn(Project p, Dataset ds) {
		// Here's what we do:
		//	- Does this dataset have any values in the synonym column?
		//	- If so, we split it using \s*[,;]\s* and record each one as a synonym of the existing name.
		// 	- Filter out renames we already know about.
		
		LOGGER.info("getRenamesFromSynonymColumn(" + p + ", " + ds + ")");

		// Does this dataset have any values in the synonym column?
		if(!ds.getColumns().contains(synonymColumn))
			return Stream.empty();
		
		Map<DatasetRow, Set<Name>> namesByRow = ds.getNamesByRow();
		
		Set<DatasetRow> rowsWithSynonymInformation = ds.getRowsAsStream().filter(row -> (row.get(synonymColumn) != null && !row.get(synonymColumn).equals(""))).collect(Collectors.toSet());
		Set<Synonymy> synonymies = rowsWithSynonymInformation.stream().flatMap(
			row -> {
				List<Synonymy> syns = new LinkedList<>();
				Set<Name> namesForRow = namesByRow.get(row);
				List<String> synonymStrs = Arrays.asList(row.get(synonymColumn).split("\\s*[,;|]\\s*"));
				
				return namesForRow.stream().flatMap(name -> {
					return synonymStrs.stream().flatMap(synonymStr -> {
						Optional<Name> synonym = Name.getFromFullName(synonymStr);
						
						if(!synonym.isPresent()) {
							LOGGER.warning("Synonym '" + synonym + "' could not be parsed!");
							return Stream.empty();
						} else
							return Stream.of(new Synonymy(synonym.get(), name, ds));
					});
				});
			}
		).collect(Collectors.toSet());
		
		// Any already in?
		Set<Synonymy> existingSynonymies = ds.getAllChanges()
			.filter(ch -> ch.getType().equals(ChangeType.RENAME))
			.flatMap(ch -> {
				Optional<Name> fromOpt = ch.getFromStream().findFirst();
				Optional<Name> toOpt = ch.getToStream().findFirst();
				
				if(!fromOpt.isPresent() || !toOpt.isPresent()) {
					LOGGER.severe("Rename found without any from or to values: " + ch);
					return Stream.empty();
				}
				
				Name from = fromOpt.get();
				Name to = toOpt.get();
				
				return Stream.of(new Synonymy(from, to, ch.getDataset()));
			})
			.collect(Collectors.toSet());
		
		// Okay! Which synonymies are worthy of import?
		return synonymies.stream()
			.filter(syn -> !existingSynonymies.contains(syn))
			.map((Synonymy syn) -> new PotentialChange(syn.getDataset(), ChangeType.RENAME, Stream.of(syn.getFrom()), Stream.of(syn.getTo())));
		
		/*
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
						.map(row -> row.get(synonymColumn))
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
					
				} else if(ch.getType().equals(ChangeType.DELETION)) {
					if(ch.getFrom().size() != 1 || ch.getTo().size() != 0) return Stream.empty();
					Name nameDeleted = ch.getFromStream().findAny().get();
					
					Set<String> myIds = prevDataset.getRowsByName(nameDeleted).stream()
						.map(row -> row.get(synonymColumn))
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
					
				} else
					return Stream.empty();
			})
			// Remove duplicate synonymy objects.
			.distinct()
			// Produce a final list of potential renames.
			.map(syn -> new PotentialChange(syn.getDataset(), ChangeType.RENAME, Stream.of(syn.getFrom()), Stream.of(syn.getTo())));
			
			*/
	}
}