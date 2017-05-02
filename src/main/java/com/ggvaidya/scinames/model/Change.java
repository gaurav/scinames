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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ggvaidya.scinames.util.ModificationTimeProperty;

import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

/**
 * A single taxonomic change: an addition, deletion, merge, lump or split.
 * 
 * Change you can believe in.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class Change {
	/* Private variables and properties */
	private UUID id = UUID.randomUUID();
	private Dataset dataset;
	private ObjectProperty<ChangeType> typeProperty = new SimpleObjectProperty<>();
	private SetProperty<Name> from = new SimpleSetProperty<>(FXCollections.observableSet());
	private SetProperty<Name> to = new SimpleSetProperty<>(FXCollections.observableSet());
	private SetProperty<Citation> citations = new SimpleSetProperty<>(FXCollections.observableSet());
	private ModificationTimeProperty lastModified = new ModificationTimeProperty();
	private MapProperty<String, String> properties = new SimpleMapProperty<>(FXCollections.observableHashMap());
	
	{
		// If any of our properties change, we've changed.
		this.typeProperty.addListener((a, b, c) -> lastModified.modified());
		this.from.addListener((a, b, c) -> lastModified.modified());
		this.to.addListener((a, b, c) -> lastModified.modified());
		this.citations.addListener((a, b, c) -> lastModified.modified());
	}
	
	/* Accessors */
	public UUID getId() { return id; }
	public Dataset getDataset() { return dataset; }
	public ChangeType getType() { return typeProperty.getValue(); }
	public ObservableValue<ChangeType> typeProperty() { return typeProperty; }
	public Set<Name> getFrom() { return from.get(); }
	public Set<Name> getTo() { return to.get(); }
	public SetProperty<Name> fromProperty() { return from; }
	public SetProperty<Name> toProperty() { return to; }
	public Stream<Name> getFromStream() { return from.stream(); }
	public Stream<Name> getToStream() { return to.stream(); }
	public ObservableSet<Citation> getCitations() { return citations.get(); }
	public Stream<Citation> getCitationStream() { return citations.stream(); }
	public SetProperty<Citation> citationsProperty() { return citations; }
	public ModificationTimeProperty lastModifiedProperty() { return lastModified; }
	public ObservableMap<String, String> getProperties() { return properties.get(); }
	public MapProperty propertiesProperty() { return properties; }	
	
	/* Higher-level accessors */
	public boolean isPropertySet(String propName) {
		if(!properties.containsKey(propName))
			return false;
		
		return properties.get(propName).equals("yes");
	}
	
	/**
	 * Used to store notes associated with this change. This is actually a property ("note"), so
	 * we create a StringProperty 
	 */
	public StringProperty noteProperty() {
		Change change = this;
		
		return new StringPropertyBase() {
			
			@Override
			public String getName() {
				return "note";
			}
			
			@Override
			public Object getBean() {
				return change;
			}
			
			@Override
			public String get() {
				return change.getProperties().get("note");
			}

			@Override
			public void set(String value) {
				change.getProperties().put("note", value);
				change.lastModified.modified();
			}
		};
	}
	
	public String getFromString() {
		return String.join(" and ", from.stream().map(n -> n.getFullName()).collect(Collectors.toList()));
	}
	
	public String getToString() {
		return String.join(" and ", to.stream().map(n -> n.getFullName()).collect(Collectors.toList()));
	}
	
	public Set<Name> getAllNames() {
		Set<Name> all = new HashSet<>(from.get());
		all.addAll(to.get());
		return all;
	}
	
	@Override
	public String toString() {
		// (from 1) + (from 2) -> (to 1) + (to 2) [type, dataset]
		StringBuilder response = new StringBuilder();	
		
		ChangeType type = typeProperty.getValue();
		if(type.equals(ChangeType.ADDITION) && getFrom().isEmpty())
			response.append("added ").append(getToStream().map(n -> n.getFullName()).collect(Collectors.joining(" + ")));
		
		else if(type.equals(ChangeType.DELETION) && getTo().isEmpty())
			response.append("deleted ").append(getFromStream().map(n -> n.getFullName()).collect(Collectors.joining(" + ")));
		
		else
			response
				.append(getFromStream().map(n -> n.getFullName()).collect(Collectors.joining(" + ")))
				.append(" -> ")
				.append(getToStream().map(n -> n.getFullName()).collect(Collectors.joining(" + ")))
				.append(" [")
				.append(type)
				.append(", ")
				.append(dataset.getCitation())
				.append("]");
		
		return response.toString();
	}
	
	/**
	 * Create a Change of a particular type, using Streams to transmit the
	 * from- and to- names.
	 * 
	 * @param type Change type, ideally one of the Change.* constants.
	 * @param from Stream of 'from' Names
	 * @param to Stream of 'to' Names
	 */
	public Change(Dataset d, ChangeType type, Stream<Name> from, Stream<Name> to) {
		dataset = d;
		this.typeProperty.setValue(type);
		this.from.addAll(from.collect(Collectors.toSet()));
		this.to.addAll(to.collect(Collectors.toSet()));
		
		this.lastModified.addListener((a, b, c) -> dataset.onChangeChanged(d.getProject(), this));
	}
	
	/**
	 * Converts an "and-string" to a stream of Names. An and-string is a set
	 * of quoted species names separated by " and ", such as:
	 *		"Homo sapiens" and "Mus musculus" and "Canis lupus familiaris"
	 * 
	 * @param names A string containing species names separated by 'and'
	 * @return A stream of Names from that set.
	 */
	public static Stream<Name> convertAndStringToNames(String names) throws IllegalStateException {
		if(names.equals(""))
			return Stream.empty();
		
		return Arrays.asList(names.split("\\s*and\\s*")).stream()
				.map(s -> s.replaceAll("^\"\\s*", ""))
				.map(s -> s.replaceAll("\\s*\"$", ""))
				.map(s -> Name.getFromFullName(s).orElseThrow(() -> new IllegalStateException("Unable to parse name '" + s + "'")));
	}
	
	/**
	 * Create a Change from a series of 'and'-strings. 'And'-strings are 
	 * quotes species names separated by " and ", such as:
	 *		"Homo sapiens" and "Mus musculus" and "Canis lupus familiaris"
	 * 
	 * @param type Change type, ideally one of the Change.* constants.
	 * @param from_str The and-string of input names.
	 * @param to_str The and-string of output names.
	 */
	public Change(Dataset d, ChangeType type, String from_str, String to_str) throws IllegalStateException {
		Stream<Name> from_stream = convertAndStringToNames(from_str);
		Stream<Name> to_stream = convertAndStringToNames(to_str);
		
		dataset = d;
		this.typeProperty.setValue(type);
		this.from.addAll(from_stream.collect(Collectors.toSet()));
		this.to.addAll(to_stream.collect(Collectors.toSet()));
		
		this.lastModified.addListener((a, b, c) -> dataset.onChangeChanged(d.getProject(), this));
	}
	
	/* Serialization */
	
	/**
	 * Read information from a <change ...> node and convert it into a Change.
	 * 
	 * @param n Node containing Change node.
	 * @return Change contained in that Change node.
	 * @throws SAXException If the Change node isn't formatted correctly.
	 */
	public static Change serializeFromNode(Dataset d, Node n) throws SAXException {
		if(!n.getNodeName().equalsIgnoreCase("change"))
			throw new SAXException("Change.serializeFromNode called on non-Change node: " + n);
		
		NamedNodeMap attr = n.getAttributes();
		
		String typeStr = attr.getNamedItem("type").getNodeValue();
		LinkedList<Name> fromList = new LinkedList<>();
		LinkedList<Name> toList = new LinkedList<>();
		LinkedList<Citation> citations = new LinkedList<>();
		Map<String, String> properties = new HashMap<>();
		
		NodeList children = n.getChildNodes();
		for(int x = 0; x < children.getLength(); x++) {
			Node child = children.item(x);
			
			if(child.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			LinkedList<Name> target;
			if(child.getNodeName().equals("from")) {
				target = fromList;
			} else if(child.getNodeName().equals("to")) {
				target = toList;
			} else if(child.getNodeName().equals("properties")) {
				NodeList propList = child.getChildNodes();
				
				for(int y = 0; y < propList.getLength(); y++) {
					Node propNode = propList.item(y);
					
					if(propNode.getNodeType() != Node.ELEMENT_NODE)
						continue;
					
					String name = propNode.getAttributes().getNamedItem("name").getNodeValue();
					String value = propNode.getTextContent();
					
					if(value.equalsIgnoreCase("true")) value = "yes";
					if(value.equalsIgnoreCase("false")) value = "no";
					
					if(properties.containsKey(name)) {
						value = properties.get(name) + "\n" + value;
					}
					
					properties.put(name, value);
				}
				
				continue;
			} else if(child.getNodeName().equals("citations")) {
				NodeList citationsList = child.getChildNodes();
				
				for(int y = 0; y < citationsList.getLength(); y++) {
					Node citationNode = citationsList.item(y);
					
					if(citationNode.getNodeType() != Node.ELEMENT_NODE)
						continue;
					
					Citation c = Citation.serializeFromNode(citationNode);
					citations.add(c);
				}		
				
				continue;
			} else {
				throw new SAXException("Unexpected node '" + child + "' found in Change.");
			}
			
			NodeList names = child.getChildNodes();
			for(int y = 0; y < names.getLength(); y++) {
				Node nameNode = names.item(y);
				
				if(nameNode.getNodeType() != Node.ELEMENT_NODE)
					continue;
				
				Name name = Name.serializeFromNode(nameNode);
				target.add(name);
			}
		}
		
		Change ch = new Change(d, ChangeType.of(typeStr), fromList.stream(), toList.stream());
		ch.getProperties().putAll(properties);
		ch.getCitations().addAll(citations);
		ch.lastModifiedProperty().saved();
		
		return ch;
	}

	/**
	 * Convert this Change into an Element to be stored in an XML document. 
	 * 
	 * @param doc The XML document in which this Element is to be created.
	 * @return The Element representing this Change.
	 */
	public Element serializeToElement(Document doc) {
		Element element = doc.createElement("change");
		element.setAttribute("type", typeProperty.getValue().getType());
		
		// Save from-names.
		Element fromElement = doc.createElement("from");
		for(Name n: from) {
			Element nameElement = n.serializeToElement(doc);
			
			fromElement.appendChild(nameElement);
		}
		element.appendChild(fromElement);
		
		// Save to-names.
		Element toElement = doc.createElement("to");
		for(Name n: to) {
			Element nameElement = n.serializeToElement(doc);
			
			toElement.appendChild(nameElement);
		}
		element.appendChild(toElement);
		
		// Save properties.
		Element propElement = doc.createElement("properties");
		properties.keySet().forEach((String propName) -> {
			Element prop = doc.createElement("property");
			prop.setAttribute("name", propName);
			prop.setTextContent(properties.get(propName));
			propElement.appendChild(prop);
		});
		if(propElement.hasChildNodes())
			element.appendChild(propElement);
		
		// Save citations.
		Element citationsElement = doc.createElement("citations");
		citations.get().forEach((Citation citation) -> {
			Element citationElement = citation.serializeToElement(doc);
			citationsElement.appendChild(citationElement);
		});
		if(citationsElement.hasChildNodes())
			element.appendChild(citationsElement);
		
		return element;
	}

	void addCitation(Citation citation) {
		citations.add(citation);
	}
}