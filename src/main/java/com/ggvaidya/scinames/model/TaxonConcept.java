
/*
 *
 *  TaxonConcept
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

package com.ggvaidya.scinames.model;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A taxon concept is a NameConcept that is a subset of a parent NameConcept.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class TaxonConcept extends NameCluster {
	private NameCluster nameCluster;
	public NameCluster getNameCluster() { return nameCluster; }
	
	/* Starts with/ends with system, so we know how taxon concepts are being delimited */
	private List<Change> startsWith = new LinkedList<>();
	public List<Change> getStartsWith() { return (startsWith); }
	
	private List<Change> endsWith = new LinkedList<>();
	public List<Change> getEndsWith() { return (endsWith); }	

	public void setStartsWith(Collection<Change> splumps) {
		startsWith.clear();
		if(splumps != null)
			startsWith.addAll(splumps);
	}	
	
	public void setEndsWith(Collection<Change> splumps) {
		endsWith.clear();
		if(splumps != null)
			endsWith.addAll(splumps);
	}

	/**
	 * Returns true if this cluster appears to be ongoing, i.e. if it exists in the last dataset in the current project.
	 * 
	 * @param p
	 * @return 
	 */
	public boolean isOngoing(Project project) {
		if(endsWith != null) return false;
		List<Dataset> datasets = project.getDatasets();
		
		if(datasets.size() < 1) return false;
		Dataset last = datasets.get(datasets.size() - 1);
		
		if(containsAny(project.getRecognizedNames(last)))
			return true;
		
		return false;
	}
	
	/**
	 * Construct a new taxon concept.
	 * 
	 * @param parent The name cluster this taxon concept is a subset of.
	 */
	public TaxonConcept(NameCluster parent) {
		nameCluster = parent;
	}
}
