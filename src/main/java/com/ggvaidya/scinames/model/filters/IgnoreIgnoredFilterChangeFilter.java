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
import com.ggvaidya.scinames.model.Project;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Ignores changes with a property 'ignored' set to 'yes'.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class IgnoreIgnoredFilterChangeFilter extends ChangeFilter {
	
	public IgnoreIgnoredFilterChangeFilter(Project p, boolean active) {
		setActive(active);
	}

	@Override
	public String getShortName() {
		return "ignoreIgnored";
	}

	@Override
	public boolean filter(Change ch) {
		if (!isActive()) {
			return true;
		}
		boolean ignored = ch.isPropertySetTrue("ignored");
		if (!ignored) {
			return true;
		}
		addFilteredChange(ch);
		return false;
	}

	@Override
	public Element serializeToElement(Document doc) {
		Element filter = doc.createElement("filter");
		filter.setAttribute("name", "ignoreIgnored");
		filter.setAttribute("active", isActive() ? "yes" : "no");
		return filter;
	}
	
}
