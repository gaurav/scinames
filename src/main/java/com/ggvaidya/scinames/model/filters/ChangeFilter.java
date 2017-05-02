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
import com.ggvaidya.scinames.model.ChangeType;

import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * ChangeFilter is a filter for changes. These can be nested within each
 * other, so you can filter through several filters at one.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public abstract class ChangeFilter implements Predicate<Change> {
	// All ChangeFilters can be active or not.
	private boolean active = false;
	public void setActive(boolean b) { active = b; }
	public boolean isActive() { return active; }
	
	// Let's track what this cluster is doing.
	private ObservableSet<Change> changesFiltered = FXCollections.observableSet(new HashSet<>());
	public ObservableSet<Change> changesFilteredProperty() { return changesFiltered; }
	public void addFilteredChange(Change ch) {
		changesFiltered.add(ch);
	}
	public int getChangesFilteredCount() { return changesFiltered.size(); }
	public Map<ChangeType, Long> getChangesFilteredByType() { 
		return changesFiltered.stream().map(ch -> ch.getType()).collect(
				Collectors.groupingBy(
						Function.identity(),
						Collectors.counting()
				)
		);
	}
	
	private ChangeFilter prev = null;
	public ChangeFilter getPrevChangeFilter() { return prev; }
	
	public abstract String getShortName();
	public abstract boolean filter(Change ch);
	public abstract Element serializeToElement(Document doc);	
	
	public String getDescription() {
		if(prev == null)
			return getShortName() + " filter";
		else
			return getShortName() + " filter enclosing " + prev.getDescription();
	}
	
	public void addChangeFilter(ChangeFilter cf) {
		if(prev == null)
			prev = cf;
		else
			prev.addChangeFilter(cf);
	}
	
	@Override
	public String toString() {
		return "ChangeFilter: " + getDescription();
	}

	@Override
	public boolean test(Change t) {
		if(prev != null) {
			if(prev.test(t))
				return filter(t);
			else
				return false;
		} else return filter(t);
	}
}
