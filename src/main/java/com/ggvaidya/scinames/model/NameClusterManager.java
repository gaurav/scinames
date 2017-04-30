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
	1. Every Name is associated with a NameCluster.
  2. Downstream systems will use NameClusters instead of Name when the
     distinction is important. They will be able to lookup the NameCluster
     associated with a name, but only after ALL the names and synonymies
	   have been loaded in.
  3. 
 
 Usage:
	1. Enter all synonymies into the cluster manager.
  2. For a given Name, translate it into a NameCluster that represents
     that name and a set of 
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class NameClusterManager {
	private final ObservableMap<Name, NameCluster> clustersByName = FXCollections.observableMap(new HashMap<>());
	private final ObservableSet<NameCluster> clusters = FXCollections.observableSet(new HashSet<>());
	
	public Stream<NameCluster> getClusters() {
		return clusters.stream();
	}
	
	public Stream<NameCluster> getSpeciesClusters() {
		return getClusters().filter(c -> !c.containsSuperspecificNames());
	}
	
	public boolean hasCluster(Name n) {
		return clustersByName.containsKey(n);
	}
	
	public Optional<NameCluster> getCluster(Name name) {
		return Optional.ofNullable(clustersByName.get(name));
	}
	
	public List<NameCluster> getClusters(Collection<Name> allNames) {
		return allNames.stream()
			.map(n -> clustersByName.get(n))
			.collect(Collectors.toList());
	}
	
	public void addCluster(NameCluster newCluster) {
		// Are any of these names already known to us? If so, we need to merge
		// them.
		Set<Name> names = newCluster.getNames();
		
		if(!names.stream().anyMatch(n -> clustersByName.keySet().contains(n))) {
			// System.err.println("New cluster " + newCluster + " has no overlap with existing clusters.");
			newCluster.getNames().forEach(n -> clustersByName.put(n, newCluster));
			clusters.add(newCluster);
			return;
		}
		
		// Identify ALL matching names. This may correspond to multiple existing clusters.
		Set<Name> matchingNames = names.stream().filter(n -> clustersByName.keySet().contains(n)).collect(Collectors.toSet());
		Set<NameCluster> matchingClusters = matchingNames.stream().map(n -> clustersByName.get(n)).distinct().collect(Collectors.toSet());

		// For each matching cluster, combine it with the cluster we have now.
		matchingClusters.forEach(cluster -> {
			// System.err.println("New cluster " + newCluster + " overlaps with existing cluster " + cluster + ", merging.");
			
			cluster.getNames().forEach(n -> clustersByName.remove(n));
			clusters.remove(cluster);
			
			newCluster.addAll(cluster);
		});
		
		// Now add in the merged cluster, containing all the other clusters
		// we've removed to make space for it.
		newCluster.getNames().forEach(n -> clustersByName.put(n, newCluster));
		clusters.add(newCluster);
	}
	
	public NameClusterManager() {
	}
}

