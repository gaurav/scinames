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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

/**
 * The Name Cluster Manager manages name clusters: it accepts statements of
 * synonymy, and can translate lists of names into name clusters.
 * 
 * Data structure:
 * 	1. 	Every Name is associated with a NameCluster.
 * 	2. 	Downstream systems will use NameClusters instead of Name when the
 * 		distinction is important. They will be able to lookup the NameCluster
 * 		associated with a name, but only after ALL the names and synonymies
 * 		have been loaded in.
 * 
 * Usage:
 * 	1. Enter all synonymies into the cluster manager.
 * 	2. For a given Name, translate it into a NameCluster that represents
 * 		that name and the set of datasets it was found in.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class NameClusterManager {
	private static final Logger LOGGER = Logger.getLogger(NameClusterManager.class.getSimpleName());
	
	private final ObservableMap<Name, NameCluster> clustersByName = FXCollections.observableMap(new HashMap<>());
	private final ObservableSet<NameCluster> clusters = FXCollections.observableSet(new HashSet<>());
	
	public Stream<NameCluster> getClusters() {
		return clusters.stream();
	}
	
	@Override
	public String toString() {
		return "NameClusterManager containing " + clusters.size() + " clusters: " + 
			clusters.stream().map(cl -> cl.toString()).collect(Collectors.joining(", "));
	}
	
	// TODO: This is either wrong or weird. Please fix!
	public Stream<NameCluster> getSpeciesClusters() {
		return getClusters().filter(c -> !c.containsSuperspecificNames());
	}
	
	public boolean hasCluster(Name n) {
		return clustersByName.containsKey(n);
	}
	
	public Optional<NameCluster> getCluster(Name name) {
		return Optional.ofNullable(clustersByName.get(name));
	}
	
	/**
	 * Returns a list of name clusters for a list of names.
	 * There is a one-to-one correspondence between the lists,
	 * so if we don't have a name cluster for a name, we'll
	 * return 'null' in that slot.
	 * 
	 * @param allNames Names to query.
	 * @return List of all NameClusters corresponding to the provided names.
	 */
	public List<NameCluster> getClusters(Collection<Name> allNames) {
		return allNames.stream()
			.map(n -> clustersByName.get(n))
			.collect(Collectors.toList());
	}
	
	/**
	 * Add a new cluster to this NameClusterManager. This may change ANY of the
	 * name clusters in this manager, so you should include all renames FIRST
	 * before making any other changes anywhere.
	 * 
	 * TODO We might want to turn this into a factory method that returns a NameClusterState,
	 * which is then unmodifiable except by adding a namecluster to produce ANOTHER NameClusterState.
	 * 
	 * @param newCluster The new cluster to add.
	 */
	public void addCluster(NameCluster newCluster) {
		// debugging
		if(newCluster.contains(Name.get("Tringa", "ptilocnemis"))) {
			LOGGER.info(" - Cluster for 'Tringa ptilocnemis' query: " + newCluster);
		}	
		
		// Are any of these names already known to us? If so, we need to merge
		// them.
		Set<Name> names = newCluster.getNames();

		if(!names.stream().anyMatch(n -> clustersByName.keySet().contains(n))) {
			LOGGER.finest("New cluster " + newCluster + " has no overlap with existing clusters.");
			newCluster.getNames().forEach(n -> clustersByName.put(n, newCluster));
			clusters.add(newCluster);
			return;
		}
		
		// Identify ALL matching names. This may correspond to multiple existing clusters.
		Set<Name> matchingNames = names.stream().filter(n -> clustersByName.keySet().contains(n)).collect(Collectors.toSet());
		Set<NameCluster> matchingClusters = matchingNames.stream().map(n -> clustersByName.get(n)).distinct().collect(Collectors.toSet());

		// For each matching cluster, combine it with the cluster we have now.
		matchingClusters.forEach(cluster -> {
			LOGGER.finest("New cluster " + newCluster + " overlaps with existing cluster " + cluster + ", merging.");
			
			// Remove each of the names of this cluster from clustersByName, and remove this cluster from the
			// clusters list.
			cluster.getNames().forEach(n -> clustersByName.remove(n));
			clusters.remove(cluster);
			
			// Now that we've "unindexed" that cluster, add all of its names and timepoints into
			// our new merged cluster.
			newCluster.addAll(cluster);
		});
		
		// debugging
		if(newCluster.contains(Name.get("Tringa", "ptilocnemis"))) {
			LOGGER.info(" - Cluster for 'Tringa ptilocnemis' result: " + newCluster);
		}
		
		// Now index the merged cluster.
		newCluster.getNames().forEach(n -> clustersByName.put(n, newCluster));
		clusters.add(newCluster);
	}
	
	/**
	 * Create a new name cluster manager.
	 */
	public NameClusterManager() {
	}
}

