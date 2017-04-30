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
package com.ggvaidya.scinames.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.property.ReadOnlySetWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

/**
 * A cluster of names associated with each other through "synonymOf" relationships.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class NameCluster {
	private static final Logger LOGGER = Logger.getLogger(NameCluster.class.getSimpleName());
	
	private UUID id = UUID.randomUUID();
	private boolean containsSuperspecificNames = false;
	private Map<Dataset, Name> binomialNameByDataset = new HashMap<>();
	private ObservableSet<Name> names = FXCollections.observableSet(new HashSet<>());
	private ObservableSet<Dataset> foundIn = FXCollections.observableSet(new TreeSet<>());
	
	public UUID getId() {
		return id;
	}
	
	public boolean containsSuperspecificNames() {
		return containsSuperspecificNames;
	}
	
	public boolean isPolytypic(Project p) {
		// This cluster is polytypic if:
		//	- this contains any infraspecific names
		if(names.stream().anyMatch(n -> n.hasSubspecificEpithet()))
			return true;
		
		//  - any of the changes involving this cluster involves a lump.
		return foundIn.stream()
			// Get all lumps and splits
			.flatMap(ds -> Stream.concat(ds.getChanges(p, Change.LUMP), ds.getChanges(p, Change.SPLIT)))
			// Get those associated with names in this cluster.
			.anyMatch(ch -> ch.getAllNames().stream().anyMatch(n -> names.contains(n)))
		;
	}
	
	@Override
	public String toString() {
		if(names.size() < 6)
			return "Name cluster " + getId() + " containing " + 
				names.stream().map(n -> n.getFullName()).collect(Collectors.joining(" and ")) +
				" found between " + getDateRange() + " in " + foundIn.size() + " timepoints";
		else
			return "Name cluster " + getId() + " containing " + getName() + " and " + (names.size() - 1) + 
				" other names between " + getDateRange() + " in " + foundIn.size() + " timepoints";
	}
	
	public int size() {
		return names.size();
	}
	
	public boolean contains(Name n) {
		return names.contains(n);
	}
	
	public boolean containsAny(Collection<Name> setOfNames) {
		return setOfNames.stream().anyMatch(n -> contains(n));
	}
	
	public Name getName() {
		// Try for the most recent name.
		List<Dataset> foundInSorted = getFoundInSorted();
		
		if(!foundInSorted.isEmpty()) {
			Dataset mostRecent = foundInSorted.get(foundInSorted.size() - 1);
			if(binomialNameByDataset.containsKey(mostRecent))
				return binomialNameByDataset.get(mostRecent);
		}
		
		// Otherwise, fallback to just one of the names.
		List<Name> arrayList = new ArrayList<>(getNames());
		if(arrayList.size() > 0)
			return arrayList.get(0);
		return Name.EMPTY;
	}
	
	/**
	 * Add a name to this cluster.
	 * 
	 * Since this is a NameCluster, we'll add every trinomial name
 as a binomial name, so we record both species-level concepts and
 concepts more specifically.
 
 Since this is a NameCluster, we also ignore any names without 
 a specific epithet.
	 * 
	 * @param n 
	 * @param ds 
	 */
	public void addName(Name n, Dataset ds) {
		if(!n.hasSpecificEpithet())
			containsSuperspecificNames = true;
		
		// Add the name in its current form.
		names.add(n);
		
		// Add the binomial form of the name.
		// If it's the same as before, it's just a duplicate.
		n.asBinomial().ifPresent(name -> {
			names.add(name);
			binomialNameByDataset.put(ds, name);
		});
		
		// If the name was added, also add the found-in.
		foundIn.add(ds);
	}
	
	public void addFoundIn(Dataset tp) {
		foundIn.add(tp);
	}
	
	public ObservableSet<Name> getNames() {
		return new ReadOnlySetWrapper(names);
	}
	
	public Optional<Name> getBinomialNameForDataset(Dataset ds) {
		return Optional.ofNullable(binomialNameByDataset.get(ds));
	}
	
	public boolean containsNameMatching(String nameRegex) {
		LOGGER.fine("Matching '" + nameRegex + "' against cluster " + names);
		return names.stream().anyMatch(n -> n.getFullName().matches(nameRegex));
	}
	
	public boolean containsNameStartingWith(String startsWith) {
		LOGGER.fine("Starts with '" + startsWith + "' against cluster " + names);
		return names.stream().anyMatch(n -> n.getFullName().matches(startsWith));
	}
	
	public ObservableSet<Dataset> getFoundIn() {
		return foundIn;
	}
	
	public List<Dataset> getFoundInSorted() {
		if(foundIn.isEmpty())
			return new ArrayList();
		
		return foundIn.stream().sorted().collect(Collectors.toList());
	}
	
	public void addAll(NameCluster cluster) {
		names.addAll(cluster.getNames());
		foundIn.addAll(cluster.getFoundIn());
	}
	
	public void addNames(Dataset ds, Name... n) {
		addNames(ds, Arrays.asList(n));
	}
	
	public void addNames(Dataset ds, Collection<Name> names) {
		names.forEach(name -> addName(name, ds));
	}
	
	public NameCluster(Dataset t, Name n) {
		addName(n, t);
	}
	
	public NameCluster(Dataset t, Name... n) {
		addNames(t, n);
	}
	
	public NameCluster(Dataset tp, Collection<Name> names) {
		addNames(tp, names);
	}
	
	public NameCluster() {
		// Empty!
	}
	
	public String getDateRange() {
		List<Dataset> range = getFoundInSorted();
		
		if(range.isEmpty())
			return "No timepoints";
		
		if(range.size() == 1)
			return range.get(0).getDate().toString();
		
		return range.get(0).getDate().toString() + " to " + range.get(range.size() - 1).getDate().toString();
	}

	public Dataset getEarliestTimepoint() {
		return getFoundInSorted().get(0);
	}
	
	/* Manage taxon concepts */
	
	/**
	 * Return a list of taxon concepts within this NameCluster.
	 * 
	 * So the trick is that you can think about every name cluster as a taxon
 concept (a bunch of names referring to the same "thing"), but sometimes
 a Name Cluster will contain multiple concepts. This code will figure
 out when that happens by looking for lumps or splits associated with
 these names.
 
 - TODO There will never be a case where a taxon concept spans multiple name concepts.
 Will there?
 
 - TODO This currently ignores filtering. How can we incorporate that?
 
 For the purposes of this demo, the role of TaxonConcept will be played
 by NameCluster.
	 * 
	 * @return 
	 */
	public List<TaxonConcept> getTaxonConcepts(Project p) {
		List<TaxonConcept> concepts = new LinkedList<>();
		TaxonConcept current = null;
		
		// TODO: why are we creating taxon concepts without any name?
		for(Dataset tp: getFoundInSorted()) {
			if(current == null) {
				// Start first cluster with this dataset. Note that this
				// ISN'T necessarily a splump -- it might be an addition.
				current = new TaxonConcept(this);
				current.setStartsWith(tp.getChanges(p)
					.filter(ch -> containsAny(ch.getTo()))
					.collect(Collectors.toList())
				);
			}
			
			Set<Name> namesFromThisCluster = new HashSet<>();
			Stream<Change> changesInvolvingNameCluster = tp.getChanges(p)
				.filter(ch -> {
					Set<Name> matchingNames = ch.getAllNames().stream()
						.filter(n -> names.contains(n))
						.collect(Collectors.toSet());
					namesFromThisCluster.addAll(matchingNames);
					return !matchingNames.isEmpty();
				}
			);
			
			List<Change> splumps = changesInvolvingNameCluster
				.filter(ch -> ch.getType().equals(Change.LUMP) || ch.getType().equals(Change.SPLIT))
				.collect(Collectors.toList());
			
			current.addNames(tp, new ArrayList(namesFromThisCluster));
			
			if(!splumps.isEmpty()) {
				// Either a lump or a split will generate a new concept, so we switch
				// to a new NameCluster!
				current.setEndsWith(splumps);
				concepts.add(current);
				
				current = new TaxonConcept(this);
				current.addNames(tp, new ArrayList(namesFromThisCluster));
				current.setStartsWith(splumps);
			}
		};
		
		// Tag the current concept.
		if(current != null) {
			current.setEndsWith(null);
			concepts.add(current);
		}
		
		return concepts;
	}
}
