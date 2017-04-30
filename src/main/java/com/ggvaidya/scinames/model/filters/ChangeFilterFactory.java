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
import com.ggvaidya.scinames.model.io.ProjectXMLReader;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * This ChangeFilterFactory produces ChangeFilters.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class ChangeFilterFactory {

	public static ChangeFilter getNullChangeFilter() {
		return new ChangeFilter() {
			@Override
			public String getShortName() {
				return "null";
			}

			@Override
			public boolean filter(Change ch) {
				return true;
			}

			@Override
			public Element serializeToElement(Document doc) {
				Element filter = doc.createElement("filter");
				
				filter.setAttribute("name", getShortName());
				
				return filter;}
		};
	}
	
	public static ChangeFilter createFilterFromNode(Project p, Node filterNode) throws SAXException {
		NamedNodeMap attributes = filterNode.getAttributes();
		String name = attributes.getNamedItem("name").getNodeValue();
		
		switch (name) {
			case "null":
				return getNullChangeFilter();
			case "ignoreIgnored":
			{
				String activeStr = attributes.getNamedItem("active").getNodeValue();
				return new IgnoreIgnoredFilterChangeFilter(p, activeStr.equalsIgnoreCase("yes"));
			}
			case "ignoreErrorChangeType":
			{
				String activeStr = attributes.getNamedItem("active").getNodeValue();
				return new IgnoreIgnoredFilterChangeFilter(p, activeStr.equalsIgnoreCase("yes"));
			}
			case "skipChangesUnlessAddedBefore":
			{
				String yearStr = attributes.getNamedItem("year").getNodeValue();
				String activeStr = attributes.getNamedItem("active").getNodeValue();
				return new SkipChangesUnlessAddedBeforeChangeFilter(p, Integer.parseInt(yearStr), activeStr.equalsIgnoreCase("yes"));
			}
			default:
				throw new SAXException("Unknown filter type '" + name + "' in " + filterNode);
		}
	}	

	public static ChangeFilter createFilterFromXMLKeyView(Project newProject, ProjectXMLReader.XMLKeyValue filterKV) throws XMLStreamException {
		Map<String, String> attributes = filterKV.getAttributes();
		String name = attributes.get("name");
		
		switch (name) {
			case "null":
				return getNullChangeFilter();
			case "ignoreIgnored":
			{
				String activeStr = attributes.get("active");
				return new IgnoreIgnoredFilterChangeFilter(newProject, activeStr.equalsIgnoreCase("yes"));
			}
			case "ignoreSelfRenames":
			{
				String activeStr = attributes.get("active");
				return new IgnoreSelfRenamesChangeFilter(newProject, activeStr.equalsIgnoreCase("yes"));
			}
			case "ignoreErrorChangeType":
			{
				String activeStr = attributes.get("active");
				return new IgnoreErrorChangeTypeFilter(newProject, activeStr.equalsIgnoreCase("yes"));
			}
			case "skipChangesUnlessAddedBefore":
			{
				String yearStr = attributes.get("year");
				String activeStr = attributes.get("active");
				return new SkipChangesUnlessAddedBeforeChangeFilter(newProject, Integer.parseInt(yearStr), activeStr.equalsIgnoreCase("yes"));
			}
			default:
				throw new XMLStreamException("Unknown filter type '" + name + "' in " + filterKV);
		}
	}
}


