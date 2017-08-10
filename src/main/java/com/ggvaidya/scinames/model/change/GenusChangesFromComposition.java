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
		//Set<Name> newlyAddedGenera = namesByGenus.keySet();
		//newlyAddedGenera.removeAll(prevNamesByGenus.keySet());
		//newlyAddedGenera.stream().forEach(n -> prevNamesByGenus.put(n, new ArrayList<>()));
		
		// Group changes by genera
		Map<Name, Set<Change>> changesByGenus = new HashMap<>();
		List<Change> datasetChanges = ds.getChanges(p).collect(Collectors.toList());
		for(Change ch: datasetChanges) {
			for(Name genus: ch.getAllNames().stream().flatMap(n -> n.asGenus()).collect(Collectors.toList())) {
				if(!changesByGenus.containsKey(genus))
					changesByGenus.put(genus, new HashSet<>());
				
				changesByGenus.get(genus).add(ch);
			}
		}
		
		// Go through every genus that's changed and figure out what happened.
		return changesByGenus.keySet().stream().flatMap(genus -> {
			Set<Change> changes = changesByGenus.get(genus);
			
			Set<Name> genusExpanded = new HashSet<>();
			Set<Name> genusShrank = new HashSet<>();
			Set<Name> genusSplitInto = new HashSet<>();
			Set<Name> genusLumpedFrom = new HashSet<>();
			Set<Name> genusUnclear = new HashSet<>();
			
			for(Change ch: changes) {
				if(ch.getFrom().isEmpty() && ch.getTo().isEmpty()) {
					// Ignore empty changes
				} else if(ch.getFrom().isEmpty() && !ch.getTo().isEmpty()) {
					genusExpanded.addAll(ch.getTo());
					
				} else if(!ch.getFrom().isEmpty() && ch.getTo().isEmpty()) {
					genusShrank.addAll(ch.getFrom());
					
				} else if(!ch.getFrom().isEmpty() && !ch.getTo().isEmpty()) {
					Set<Name> from = ch.getFrom();
					Set<Name> to = ch.getTo();

					// Which names were added and which were deleted.
					
					
					/*
					Set<Name> namesInFromNotInThisGenus = from.stream().filter(n -> !n.asGenus().findFirst().get().equals(genus)).collect(Collectors.toSet());
					Set<Name> namesInToNotInThisGenus = to.stream().filter(n -> !n.asGenus().findFirst().get().equals(genus)).collect(Collectors.toSet());
					
					Set<Name> generaInFrom = from.stream().filter(n -> n.asGenus().findFirst().get().equals(genus));
					Set<Name> generaInTo = to.stream().filter(n -> n.asGenus().findFirst().get().equals(genus));
					
					if(genusInFrom && genusInTo) {
						// e.g. A + B + C -> A + D + E (where genus = A)
						// 	So: we really don't know what B, C, D, E mean
						genusUnclear.addAll(namesInFromNotInThisGenus);
						genusUnclear.addAll(namesInToNotInThisGenus);
						
						// But names in A are just moving within this genus. Ignore!
					} else if(genusInFrom) {
						// e.g. A + B + C -> B + D + E
						// 	So: B, C are unclear
						genusUnclear.addAll(namesInFromNotInThisGenus);
						
						// But we are clearly lumping into 	
					}
					
					for(Name from: new ArrayList<>(ch.getFrom())) {
						for(Name to: new ArrayList<>(ch.getTo())) {
							Name genusFrom = from.asGenus().findFirst().get();
							Name genusTo = to.asGenus().findFirst().get();
							
							if(genusFrom.equals(genus) && genusTo.equals(genus)) {
								// Species renamed within genus, ignore.
							} else if(genusFrom.equals(genus)) {
								// Species split from this genus into another genus.
								genusSplitInto.add(to); 
							} else if(genusTo.equals(genus)) {
								// Species lumped into this genus from another genus.
								genusLumpedFrom.add(to);
							} else {
								// So, we get here if there's a lump or split that involves other genera
								// 	e.g. A + B -> B (where genus = A)
								// Or even worse:
								//	e.g. A + B -> C (where genus = A)
								// Either way, we basically just get expanded by
								// the opposite side and shrunk by our side, so let's
								// just do that.
								if()
							}
						}
					}*/
					
				} else {
					throw new RuntimeException("Impossible code branch");
				}
			}
			
			return Stream.empty();
		});
		
		/*
				
		// Every change affects a genus!
		Map<Name, Set<String>> genusResults = new HashMap<>();

		for(Change ch: changes) {
			// All deleted genera have shrunk, possibly lumped, possibly deleted.
			for(Name nameDeleted: ch.getFromStream().collect(Collectors.toList())) {
				// Possibility 1. This genus has been deleted altogether.
			}
			
			// All added genera have expanded, possibly split, possibly added.
			for(Name nameAdded: ch.getToStream().collect(Collectors.toList())) {
				
			}
		}
		
		LOGGER.info("Comparing dataset " + ds + " with " + prevDataset + ".");
		LOGGER.info("Genus results: " + genusResults);

		return Stream.empty();
		
		// Go through every previously existing genus (as well as newly added genera).
		return prevNamesByGenus.keySet().stream()
			.flatMap(prevGenus -> {
				// Figure out previous and current composition of this genus.
				Set<Name> prevNames = prevNamesByGenus.get(prevGenus).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toSet());
				Set<Name> currentNames = new HashSet<>();
				Map<Name, Name> genusPerName = new HashMap<>();
				if(namesByGenus.containsKey(prevGenus)) {
					currentNames = namesByGenus.get(prevGenus).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toSet());
					currentNames.forEach(n -> {
						genusPerName.put(n, n.asGenus().findFirst().get());
					});
				}
				
				// Maybe it's identical
				if(prevNames.equals(currentNames)) {
					return Stream.empty();
				// Maybe it ain't.
				} else {
					// Okay, so: where did the prevNames *go*?
					List<PotentialChange> generaChanges = new LinkedList<>();
					Set<Name> prevNamesUnexplained = new HashSet<>(prevNames);
					
					for(Name n: prevNamesUnexplained) {
						Name currentGenus = genusPerName.get(n);
						
						// There are three options:
						//		- expansion/contraction: names being added or deleted altogether
						if(prevGenus.equals(obj))
						
						if(currentGenus.equals(Name.EMPTY)) {
							// Name 'n' has been deleted altogether!
							// Which means prevGenus has contracted!
						}
						
						//		- We now have new names from (another genus) or 
						//		- We've lost some of our names to another genus or 
						
						// Maybe some stayed in the same genus.
					}
						
					
					
					
					
					
					return Stream.of(new PotentialChange(
						ds, 
						ChangeType.of("changed"), 
						Stream.of(prevGenus), // old name 
						Stream.of(prevGenus), // new name
						GenusChangesFromComposition.class, 
						"Genus changed from " + prevNames + " to " + currentNames
					));
				}
			});
		*/
	}

}