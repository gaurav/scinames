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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.ggvaidya.scinames.util.SimplifiedDate;

/**
 * Tests for the NameClusterManager class.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class NameClusterManagerTest {
	Dataset ds1 = new Dataset("ds1", new SimplifiedDate(1930), Dataset.TYPE_DATASET);
	Dataset ds2 = new Dataset("ds2", new SimplifiedDate(1940), Dataset.TYPE_DATASET);
	Dataset ds3 = new Dataset("ds3", new SimplifiedDate(1950), Dataset.TYPE_DATASET);
	Dataset ds4 = new Dataset("ds4", new SimplifiedDate(1960), Dataset.TYPE_DATASET);
	
	private NameClusterManager buildNameClusterManager() {
		NameClusterManager ncm = new NameClusterManager();
		
		ncm.addCluster(new NameCluster(ds1, Name.get("Ornithorhynchus", "anatinus")));
		ncm.addCluster(new NameCluster(ds1, Name.get("Platypus", "anatinus")));
		
		ncm.addCluster(new Synonymy(
			Name.get("Ornithorhynchus", "anatinus"), 
			Name.get("Platypus", "anatinus"),
			ds1
		));
		
		ncm.addCluster(new NameCluster(ds2, Name.get("Ornithorhynchus", "paradoxus")));
		ncm.addCluster(new NameCluster(ds2, Name.get("Alpha", "beta")));
		
		ncm.addCluster(new Synonymy(
			Name.get("Ornithorhynchus", "paradoxus"),
			Name.get("Alpha", "beta"),			// Made up name for testing!
			ds2
		));
		
		return ncm;
	}
	
	@Test
	public void identicalClustersInSet() {
		NameClusterManager ncm = buildNameClusterManager();
		
		List<NameCluster> clusters = ncm.getClusters(Arrays.asList(
			Name.get("Ornithorhynchus", "anatinus"),
			Name.get("Platypus", "anatinus")
		));
		
		// Should return TWO clusters.
		assertEquals(2, clusters.size());
		
		// But two IDENTICAL clusters.
		assertEquals(1, new HashSet<>(clusters));
	}
	
    /**
     * Test clustering using the NameClusterManager.
     */
	@Test
	public void testClustering() {
		NameClusterManager ncm = buildNameClusterManager();
				
		assertEquals(ncm.getClusters().count(), 2);
		List<Dataset> earliest = ncm.getClusters().map(cl -> cl.getFoundInSorted().get(0)).collect(Collectors.toList());
		assertEquals(new HashSet<>(earliest), new HashSet<>(Arrays.asList(ds2, ds1)));
		
		ncm.addCluster(new Synonymy(
			Name.get("Ornithorhynchus", "paradoxus"),
			Name.get("Ornithorhynchus", "anatinus"),
			ds4
		));

		assertEquals(ncm.getClusters().count(), 1);
		
		Optional<NameCluster> opt = ncm.getCluster(Name.get("Platypus", "anatinus"));
		assertTrue(opt.isPresent());
		
		NameCluster cluster = opt.get();
		assertEquals(cluster.size(), 4);
		
		assertTrue(cluster.getNames().containsAll(Arrays.asList(
			Name.get("Ornithorhynchus", "anatinus"), 
			Name.get("Platypus", "anatinus"),
			Name.get("Ornithorhynchus", "paradoxus"),
			Name.get("Alpha", "beta")			
		)));
		
		assertEquals(cluster.getFoundInSorted().get(0), ds1);
	}
}
