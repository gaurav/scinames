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
	private ObservableSet<Name> names = FXCollections.observableSet(new HashSet<>());
	private ObservableSet<Dataset> foundIn = FXCollections.observableSet(new TreeSet<>());
	
	// Binomial name per dataset; allows people to ask "give me one name this cluster had in this dataset". 
	private Map<Dataset, Name> binomialNameByDataset = new HashMap<>();
	
	// i.e. contains genus-only names.
	private boolean containsSuperspecificNames = false;
	
	/* Accessors */
	
	public UUID getId() {
		return id;
	}
	
	public boolean containsSuperspecificNames() {
		return containsSuperspecificNames;
	}
	
	public void addFoundIn(Dataset tp) {
		foundIn.add(tp);
	}
	
	public Optional<Name> getBinomialNameForDataset(Dataset ds) {
		return Optional.ofNullable(binomialNameByDataset.get(ds));
	}
	
	public ObservableSet<Name> getNames() { return new ReadOnlySetWrapper<>(names); }
	
	
	/**
	 * Attempts to determine if this name cluster is "polytypic". We use a pretty straightforward
	 * view of this -- either:
	 * 
	 * 	- 1. It contains any infraspecific names, or
	 *  - 2. It contains any lumps.
	 * 
	 * @param p
	 * @return
	 */
	public boolean isPolytypic(Project p) {
		// This cluster is polytypic if:
		//	- it contains any infraspecific names
		if(names.stream().anyMatch(n -> n.hasSubspecificEpithet()))
			return true;
		
		//  - OR any of the changes involving this cluster involves a lump.
		return foundIn.stream()
			// Get all lumps
			.flatMap(ds -> ds.getChanges(p)
				.filter(ch -> ch.getType().equals(ChangeType.LUMP))
			)
			// Get those associated with names in this cluster.
			.anyMatch(ch -> ch.getAllNames().stream().anyMatch(n -> names.contains(n)))
		;
	}
	
	@Override
	public String toString() {
		if(names.size() < 6)
			return "Name cluster " + getId() + " containing " + 
				names.stream().map(n -> "'" + n.getFullName() + "'").collect(Collectors.joining(" and ")) +
				" found between " + getDateRange() + " in " + foundIn.size() + " timepoints";
		else
			return "Name cluster " + getId() + " containing '" + getName() + "' and " + (names.size() - 1) + 
				" other names between " + getDateRange() + " in " + foundIn.size() + " timepoints";
	}
	
	public int size() {
		return names.size();
	}
	
	// Since we are species name clusters.
	public boolean contains(Name n) {
		return names.contains(n) || n.asBinomial().anyMatch(n2 -> names.contains(n2));
	}
	
	public boolean containsAny(Collection<Name> setOfNames) {
		return setOfNames.stream().anyMatch(n -> contains(n));
	}
	
	/**
	 * Returns a single name to represent this taxon concept. We could theoretically
	 * choose a name at random, but we try to get the most recent name it had, as 
	 * hopefully that'll be most useful visually and for synthesis.
	 * 
	 * @return A single name that represents this taxon concept.
	 */
	public Name getName() {
		// Try for the most recent name.
		List<Dataset> foundInSorted = getFoundInSorted();
		
		if(!foundInSorted.isEmpty()) {
			Dataset mostRecent = foundInSorted.get(foundInSorted.size() - 1);
			if(binomialNameByDataset.containsKey(mostRecent))
				return binomialNameByDataset.get(mostRecent);
		}
		
		// Otherwise, fall back to just one of the names.
		List<Name> arrayList = new ArrayList<>(getNames());
		if(arrayList.size() > 0)
			return arrayList.get(0);
		
		// No name could be found.
		return Name.EMPTY;
	}
	
	/**
	 * Add a name to this cluster.
	 * 
	 * Since this is a NameCluster, we'll add every trinomial name as a binomial name, 
	 * so we record both species-level concepts and concepts more specifically.
	 *
	 * Since this is a NameCluster, we also ignore any names without a specific epithet.
	 * 
	 * @param n Name to add
	 * @param ds Dataset in which this name was found.
	 */
	public void addName(Name n, Dataset ds) {
		if(!n.hasSpecificEpithet())
			containsSuperspecificNames = true;
		
		// Add the name in its current form.
		names.add(n);
		
		// Add the binomial form of the name.
		// If it's the same as before, it's just a duplicate.
		n.asBinomial().forEach(name -> {
			names.add(name);
			binomialNameByDataset.put(ds, name);
		});
		
		// Also add the found-in.
		foundIn.add(ds);
	}
	
	/**
	 * Look for names matching a particular regular expression.
	 * 
	 * @param nameRegex The regular expression to match against.
	 * @return True if it matches, false otherwise.
	 */
	public boolean containsNameMatching(String nameRegex) {
		LOGGER.fine("Matching '" + nameRegex + "' against cluster " + names);
		return names.stream().anyMatch(n -> n.getFullName().matches(nameRegex));
	}
	
	public boolean containsNameStartingWith(String startsWith) {
		LOGGER.fine("Starts with '" + startsWith + "' against cluster " + names);
		return names.stream().anyMatch(n -> n.getFullName().matches(startsWith));
	}
	
	public ObservableSet<Dataset> getFoundIn() {
		/*
		if(contains(Name.get("Tringa", "ptilocnemis"))) {
			LOGGER.fine(" - Name cluster containing 'Tringa ptilocnemis' has foundIn: " + foundIn + ", concept: " + this);
		}*/
		
		return foundIn;
	}
	
	public List<Dataset> getFoundInSorted() {
		if(foundIn.isEmpty())
			return new ArrayList<Dataset>();
		
		return foundIn.stream().sorted().collect(Collectors.toList());
	}
	
	/* Different ways of adding names */
	
	public void addAll(NameCluster cluster) {
		names.addAll(cluster.getNames());
		foundIn.addAll(cluster.getFoundIn());
		
		// merge the other cluster's "binomialNameByDataset" data.
		cluster.binomialNameByDataset.forEach((Dataset ds, Name n) -> {
			// Overwrite our dataset/name info.
			binomialNameByDataset.put(ds, n);
		});
	}
	
	public void addNames(Dataset ds, Name... n) {
		addNames(ds, Arrays.asList(n));
	}
	
	public void addNames(Dataset ds, Collection<Name> names) {
		names.forEach(name -> addName(name, ds));
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
	 * So the trick is that you can think about every name cluster as a 
	 * taxon concept (a bunch of names referring to the same "thing"), but 
	 * sometimes a Name Cluster will contain multiple concepts. This code 
	 * will figure out when that happens by looking for lumps or splits 
	 * associated with these names.
	 * 
	 * A NameCluster is therefore our equivalent of a "nominal concept"!
	 * 
	 * Note that this incorporates filtering.
	 * 
	 */
	public List<TaxonConcept> getTaxonConcepts(Project p) {
		List<TaxonConcept> concepts = new LinkedList<>();
		TaxonConcept current = null;
		
		// We go through all datasets this name cluster is found in.
		for(Dataset ds: getFoundInSorted()) {
			if(current == null) {
				// Start first cluster with this dataset. Note that this
				// ISN'T necessarily a splump -- it might be an addition
				// or a 'recognition'.
				
				// However, let's make sure that initial event isn't 
				// filtered out!
				List<Change> changes = ds.getChanges(p)
					.filter(ch -> ch.getAllNames().stream().anyMatch(n -> contains(n)))
					.collect(Collectors.toList());
				
				if(changes.isEmpty()) {
					continue;
				} else {
					current = new TaxonConcept(this);
					current.setStartsWith(changes);
					
					//if(contains(Name.get("Junco", "hyemalis")))
					//	LOGGER.info(" - New empty taxon concept started in dataset " + ds + ": " + current);
					
				}
			}
			
			// Find all changes involving this name cluster in this dataset, and
			// extract all the names in this name cluster used in this one dataset.
			Set<Name> namesFromThisDataset = ds.getChanges(p)
				.flatMap(ch -> ch.getAllNames().stream()
					.filter(n -> contains(n))
				)
				.collect(Collectors.toSet());
			current.addNames(ds, new ArrayList<>(namesFromThisDataset));
			
			//if(contains(Name.get("Junco", "hyemalis")))
			//	LOGGER.info(" - Added names to " + current + " for " + ds + ": " + namesFromThisDataset);
			
			// Then filter that down to just the lumps and splits.
			List<Change> splumps = ds.getChanges(p)
				.filter(ch -> ch.getAllNames().stream().anyMatch(
					n -> contains(n)
				))
				.filter(ch -> ch.getType().equals(ChangeType.LUMP) || ch.getType().equals(ChangeType.SPLIT))
				.collect(Collectors.toList());
			
			if(!splumps.isEmpty()) {
				// Either a lump or a split will generate a new concept, so we switch
				// to a new NameCluster!
				current.setEndsWith(splumps);
				concepts.add(current);
				
				current = new TaxonConcept(this);
				current.addNames(ds, new ArrayList<>(namesFromThisDataset));
				current.setStartsWith(splumps);
			}
		};
		
		// Tag the current concept.
		if(current != null) {
			current.setEndsWith(null);
			concepts.add(current);
		}
		
		//if(contains(Name.get("Junco", "hyemalis")))
		//	LOGGER.info(" - Taxon concepts for 'Junco hyemalis':\n" + concepts.stream().map(c -> c.toString()).collect(Collectors.joining("\n - ")));
		
		return concepts;
	}
	
	/* Constructors */
	
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
}
