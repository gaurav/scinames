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
package com.ggvaidya.scinames.model.filters;

import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.NameClusterManager;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.Dataset;
import java.time.Year;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class SkipChangesUnlessAddedBeforeChangeFilter extends ChangeFilter {
	private Project project;
	private Year filterAfterDate;

	public SkipChangesUnlessAddedBeforeChangeFilter(Project p, int year, boolean active) {
		project = p;
		filterAfterDate = Year.of(year);
		setActive(active);
	}

	@Override
	public String getShortName() {
		return "skip changes unless added before " + filterAfterDate;
	}

	@Override
	public boolean filter(Change ch) {
		if (!isActive())
			return true;
		
		NameClusterManager nameClusterManager = project.getNameClusterManager();
		List<NameCluster> nameClusters = nameClusterManager.getClusters(ch.getAllNames());
		
		// What if we have no name clusters?
		if(nameClusters.contains(null))
			throw new RuntimeException("One or more names from " + ch.getAllNames() + " have not been added to the name clusters yet: " + nameClusters);
		
		// As long as one of the name clusters date from before the date, we keep the change.
		boolean filter = !nameClusters.stream().anyMatch((NameCluster nc) -> {
			if (nc == null)
				throw new RuntimeException("eh null what");
			
			Dataset tp = nc.getEarliestTimepoint();
			boolean nameAddedBeforeDate = tp.getDate().compareTo(filterAfterDate) < 0;
			
			/*
			System.err.println(
				" - " + 
				(nameAddedAfterDate ? "Added after date" : "Added before date") +
				" name cluster " + nc + " as it has timepoint at " + tp.getDate() + " later than " + filterAfterDate
			);
			*/
			
			return nameAddedBeforeDate;
		});
		/*
		if (filter) {
			System.err.println("Filtering out change " + ch.getFromString() + " -> " + ch.getToString());
		} else {
			System.err.println("Allowing change " + ch.getFromString() + " -> " + ch.getToString());
		}*/
		if (!filter) {
			return true;
		}
		
		addFilteredChange(ch);
		return false;
	}

	@Override
	public Element serializeToElement(Document doc) {
		Element filter = doc.createElement("filter");
		filter.setAttribute("name", "skipChangesUnlessAddedBefore");
		filter.setAttribute("year", String.valueOf(filterAfterDate.getValue()));
		filter.setAttribute("active", isActive() ? "yes" : "no");
		return filter;
	}	
}
