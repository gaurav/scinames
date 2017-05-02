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

import com.ggvaidya.scinames.util.SimplifiedDate;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import com.ggvaidya.scinames.util.ModificationTimeProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A citationText references a publication. It consists of a citationText text and
 the date of publication, but it may also contain properties and a URL to
 the document.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class Citation implements Citable {
	private String citationText;
	private SimplifiedDate date;
	private String url;
	private SetProperty<Tag> tags = new SimpleSetProperty<>(FXCollections.observableSet());
	private MapProperty<String, String> properties = new SimpleMapProperty<>(FXCollections.observableMap(new HashMap<>()));
	private ModificationTimeProperty lastModified = new ModificationTimeProperty();
	
	/* Accessors */
	@Override public String getCitation() { return citationText; }
	public void setCitation(String s) { citationText = s; lastModified.modified(); }
	@Override public SimplifiedDate getDate() { return date; }
	@Override public void setDate(SimplifiedDate sd) { date = sd; lastModified.modified(); }
	public String getURLAsString() { return url; }
	public URL getURL() throws MalformedURLException { return new URL(url); }
	public void setURL(String url) { this.url = url; lastModified.modified(); }
	public ObservableMap<String, String> getProperties() { return properties.get(); }
	public MapProperty<String, String> propertiesProperty() { return properties; }
	public ObservableSet<Tag> getTags() { return tags.get(); }
	public SetProperty<Tag> tagsProperty() { return tags; }
	
	/* Higher level accessors */
	@Override
	public String toString() {
		return getCitation() + " (" + getDate() + ")";
	}
	
	/* Constructors */
	
	public Citation(String text, SimplifiedDate sd) {
		this.citationText = text;
		this.date = sd;
	}

	public Element serializeToElement(Document d) {
		Element e = d.createElement("citation");
		
		/*
			We'll go for:
				<citation year="2007" month="7">
					<cite>Text text</cite>
					<properties>
						<property name="name">value</property>
					</properties>
				</citation>
		*/
		
		date.setDateAttributesOnElement(e);
		
		Element cite = d.createElement("cite");
		cite.setTextContent(getCitation());
		e.appendChild(cite);
		
		Element propertiesElement = d.createElement("properties");
		properties.keySet().forEach((String propKey) -> {
			Element propertyElement = d.createElement("property");
			propertyElement.setAttribute("name", propKey);
			propertyElement.setTextContent(properties.get(propKey));
			propertiesElement.appendChild(propertyElement);
		});
		e.appendChild(propertiesElement);
		
		Element tagsElement = d.createElement("tags");
		tags.forEach(tag -> {
			Element tagProperty = d.createElement("tag");
			tagProperty.setTextContent(tag.getName());
			tagsElement.appendChild(tagProperty);
		});
		e.appendChild(tagsElement);
		
		return e;
	}
	
	public static Citation serializeFromNode(Node n) throws SAXException {
		throw new UnsupportedOperationException("Serializing citations from XML using SAX is no longer supported.");
		
		/* 
		SimplifiedDate sd = new SimplifiedDate(n);
		Map<String, String> props = new HashMap<>();
		String citationText = null;
		
		NodeList children = n.getChildNodes();
		for(int x = 0; x < children.getLength(); x++) {
			Node child = children.item(x);
			
			if(child.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			if(child.getNodeName().equalsIgnoreCase("cite")) {
				if(citationText != null)
					citationText = citationText + child.getTextContent();
				else
					citationText = child.getTextContent();
			} else if(child.getNodeName().equalsIgnoreCase("properties")) {
				
				NodeList propNodes = child.getChildNodes();
				for(int y = 0; y < propNodes.getLength(); y++) {
					Node propNode = propNodes.item(y);
			
					if(propNode.getNodeType() != Node.ELEMENT_NODE)
						continue;
					
					String propName = propNode.getAttributes().getNamedItem("name").getNodeValue();
					String propValue = propNode.getTextContent();
					
					if(props.containsKey(propName))
						propValue = props.get(propName) + "\n" + propValue;

					props.put(propName, propValue);
				}
			} else {
				throw new SAXException("Unexpected node found in Citation: " + n);
			}
		}
		
		Citation c = new Citation(citationText, sd);
		c.getProperties().putAll(props);
		
		return c;
		*/
	}
}
