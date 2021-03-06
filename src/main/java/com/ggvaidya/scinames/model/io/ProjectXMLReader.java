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
package com.ggvaidya.scinames.model.io;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.ChangeType;
import com.ggvaidya.scinames.model.Citation;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.Tag;
import com.ggvaidya.scinames.model.filters.ChangeFilterFactory;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorParseException;
import com.ggvaidya.scinames.util.SimplifiedDate;

/**
 * Read an XML file as a Project. We used to use a SAX parser distributed through our model to
 * make that happen, but that took too much memory, so now we use an event-based parser to do it
 * using far less memory. 
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class ProjectXMLReader {
	private static final Logger LOGGER = Logger.getLogger(ProjectXMLReader.class.getSimpleName());
	
	/**
	 * Read all attributes from an XMLEvent (must be a StartElement!).
	 * 
	 * @param elem The XMLEvent to read attributes from.
	 * @param allowedAttrs (Optionally) provide a list of attributes allowed.
	 * @return A map of attributes and values from this StartElement.
	 * @throws XMLStreamException unless XMLEvent is a StartElement, if allowedAttrs are provided, 
	 *		throws an exception if an unexpected attribute is provided.
	 */
	public static Map<String, String> getAllAttributes(XMLEvent elem, String... allowedAttrs) throws XMLStreamException {
		Set<String> allowed = new HashSet<>(Arrays.asList(allowedAttrs));
		Map<String, String> attrs = new HashMap<>();
		
		@SuppressWarnings("unchecked")
		Iterator<Attribute> iter = elem.asStartElement().getAttributes();
		while(iter.hasNext()) {
			Attribute attr = iter.next();
			
			String name = attr.getName().getLocalPart();
			if(allowed.size() > 0 && !allowed.contains(name))
				throw new XMLStreamException("Element '" + elem + "' contains unexpected attribute '" + name + "'");
			
			attrs.put(name, attr.getValue());
		}
		
		return attrs;
	}
	
	/**
	 * An XMLKeyValue encodes a common key-value pattern in XML.
	 * 
	 * Contains:
	 *	- An element (e.g. 'key')
	 *  - With one or more attributes (e.g. 'name=...')
	 *  - With a value.
	 * 
	 * The cool thing here is that we often have an element that ONLY
	 * contains XMLKeyValue-s, so we can send our system out to read
	 * all of them at once.
	 */
	public static class XMLKeyValue {
		private QName name;
		public QName getQName() { return name; }
		public void setQName(QName n) { name = n; }
		
		private Map<String, String> attrs;
		public Map<String, String> getAttributes() { return attrs; }
		public void setAttributes(Map<String, String> a) { attrs = a; }
		
		private String value;
		public String getValue() { return value; }
		public void setValue(String v) { value = v; }
		
		public XMLKeyValue() {}
	}
	
	/**
	 * A tag series is a set of XMLKeyValue's found within a node. These nodes
	 * should all have the same name.
	 * 
	 * @param reader XMLEventReader to read from
	 * @param tagName Name of the node to expect (e.g. 'property').
	 * @return List of XMLKeyValues found
	 * @throws XMLStreamException 
	 */
	public static List<XMLKeyValue> getTagSeries(XMLEventReader reader, String tagName) throws XMLStreamException {
		List<XMLKeyValue> values = new LinkedList<>();
		
		while(reader.hasNext()) {
			XMLEvent nextTag = reader.nextTag();
			
			LOGGER.fine(" - tag series '" + tagName + "': " + nextTag);
			
			if(!nextTag.isStartElement() && nextTag.isEndElement()) {
				// We're done with the series!
				return values;
			} else if(nextTag.isStartElement()) {
				// Is this an element we're interested in?
				StartElement elem = nextTag.asStartElement();
				
				if(elem.getName().getLocalPart().equals(tagName)) {
					// Save the name.
					XMLKeyValue kv = new XMLKeyValue();
					kv.setQName(elem.getName());

					// Get the attributes.
					kv.setAttributes(getAllAttributes(nextTag));
					
					// Parse this element and return it as an XMLKeyValue pair.
					StringBuilder content = new StringBuilder();
					while(!nextTag.isEndElement()) {
						nextTag = reader.nextEvent();
						
						if(nextTag.isEndElement())
							break;
						else if(nextTag.isCharacters())
							content.append(nextTag.asCharacters().getData());
						else
							throw new XMLStreamException("Unexpected content in element '" + elem + "': " + nextTag);
					}
					kv.setValue(content.toString());
					
					values.add(kv);
				} else
					throw new XMLStreamException("Unexpected element found in tag series '" + tagName + "': " + elem);
			} else
				throw new XMLStreamException("Unexpected content found in tag series '" + tagName + "': " + nextTag);
		}
		
		return values;
	}
	
	public static String getElementName(XMLEvent evt) throws XMLStreamException {
		if(!evt.isStartElement())
			throw new XMLStreamException("Expected start element, found " + evt);
		
		return evt.asStartElement().getName().getLocalPart();
	}
	
	public static Project read(XMLEventReader reader) throws XMLStreamException, IllegalStateException {
		XMLEvent projectElem = reader.nextTag();
		
		if(!getElementName(projectElem).equals("project"))
			throw new XMLStreamException("Expected 'project' as first tag, missing.");
		
		Project newProject = new Project();
		Map<String, String> attrs = getAllAttributes(projectElem, "name");
		newProject.setName(attrs.get("name"));
		
		while(reader.hasNext()) {
			XMLEvent nextTag = reader.nextTag();
			
			if(nextTag.isEndElement())
				break;
			
			if(!nextTag.isStartElement())
				throw new XMLStreamException("Unexpected content, expected 'properties', 'filters' or 'datasets': " + nextTag);
			
			// What element is starting now? We figure that out, then
			// dispatch it to someone else to load.
			String name = nextTag.asStartElement().getName().getLocalPart();
			
			switch(name) {
				case "properties":
					List<XMLKeyValue> properties = getTagSeries(reader, "property");
					
					newProject.propertiesProperty().putAll(
						properties.stream().collect(Collectors.toMap(
							kv -> kv.getAttributes().get("name"),
							kv -> kv.getValue()
						))
					);
					
					break;
					
				case "filters":
					List<XMLKeyValue> filters = getTagSeries(reader, "filter");
					for(XMLKeyValue filter: filters) {
						newProject.addChangeFilter(ChangeFilterFactory.createFilterFromXMLKeyView(newProject, filter));
					}
					
					break;
					
				case "datasets":
					while(reader.hasNext()) {
						nextTag = reader.nextTag();
						
						if(nextTag.isEndElement())
							break;

						if(!nextTag.isStartElement() || !nextTag.asStartElement().getName().getLocalPart().equals("dataset"))
							throw new XMLStreamException("Unexpected content, expected 'dataset': " + nextTag);

						Map<String, String> attributes = getAllAttributes(nextTag, "name", "type", "is_checklist", "year", "month", "day", "nameExtractors");
						SimplifiedDate date = new SimplifiedDate(attributes);
						
						// What type is it? 
						String datasetType = Dataset.TYPE_DATASET;
						
						// We support both the old style:
						//		is_checklist: yes or anything else
						if(attributes.containsKey("is_checklist") && attributes.get("is_checklist").equalsIgnoreCase("yes")) {
							datasetType = Dataset.TYPE_CHECKLIST;
						}
						// as well as the new style:
						//		type: (type string)
						else if(attributes.containsKey("type")) {
							datasetType = attributes.get("type");
						}

						Dataset dataset = new Dataset(
							attributes.get("name"), 
							date, 
							datasetType
						);
						if(attributes.containsKey("nameExtractors")) {
							try {
							    dataset.setNameExtractorsString(attributes.get("nameExtractors"));
							} catch(NameExtractorParseException ex) {
								// TODO set up some kind of warnings system
								LOGGER.warning("WARNING: could not set name parser extracter on " + dataset + " to " + attributes.get("nameExtractors") + ", " + ex);
							}
						}
							
						readDataset(dataset, reader);
						LOGGER.info("Added dataset: " + dataset);
						newProject.addDataset(dataset);
					}
						
					continue;
			}
		}
		
		return newProject;
	}
	
	/**
	 * Helper function for processing a single dataset.
	 * 
	 * @param newProject
	 * @param reader 
	 */
	private static void readDataset(Dataset dataset, XMLEventReader reader) throws XMLStreamException {
		// Read a dataset.
		while(reader.hasNext()) {
			XMLEvent nextTag = reader.nextTag();
			
			if(nextTag.isEndElement())
				return;
			
			else if(nextTag.isStartElement()) {
				StartElement start = nextTag.asStartElement();
				
				switch (start.getName().getLocalPart()) {
					case "properties":
						List<XMLKeyValue> properties = getTagSeries(reader, "property");
						
						dataset.propertiesProperty().putAll(
							properties.stream().collect(Collectors.toMap(
								kv -> kv.getAttributes().get("name"),
								kv -> kv.getValue()
							))
						);
						
						break;
				
					case "changes":
						if(!dataset.explicitChangesProperty().isEmpty()) {
							throw new XMLStreamException("Dataset contains multiple sets of changes in, with the second set starting with: " + start);
						}
						
						List<Change> changes = new LinkedList<>();
						
						while(reader.hasNext()) {
							nextTag = reader.nextTag();
							
							if(nextTag.isEndElement())
								break;
							else if(nextTag.isStartElement() && nextTag.asStartElement().getName().getLocalPart().equals("change")) {
								changes.add(readChange(reader, nextTag.asStartElement(), dataset));
							} else
								throw new XMLStreamException("Unexpected element in 'change':" +  nextTag);
						}
						
						dataset.explicitChangesProperty().setAll(changes);
						
						break;
						
					case "columns":
						List<XMLKeyValue> tagSeries = getTagSeries(reader, "column");
						dataset.setColumns(
							tagSeries.stream()
								.map(keyValue -> keyValue.getAttributes())
								.map(colData -> DatasetColumn.of(colData.getOrDefault("name", "(unnamed)")))
								.collect(Collectors.toList())
						);
						
						LOGGER.fine("Columns: " + dataset.getColumns());
						break;
						
					case "rows":
						while(reader.hasNext()) {
							nextTag = reader.nextTag();
							
							if(nextTag.isEndElement())
								break;
							else if(nextTag.isStartElement()) {
								start = nextTag.asStartElement();
								
								if(start.getName().getLocalPart().equals("row")) {
									List<XMLKeyValue> keyValues = getTagSeries(reader, "key");
									Map<DatasetColumn, String> rowData = keyValues.stream()
										.collect(Collectors.toMap(
												kv -> DatasetColumn.of(kv.getAttributes().get("name")),
												kv -> kv.getValue()
										));
									
									dataset.rowsProperty().add(new DatasetRow(dataset, rowData));
								} else
									throw new XMLStreamException("Unexpected start element found in 'rows': " + start);
							} else
								throw new XMLStreamException("Unexpected content found in 'rows': " + nextTag);
						}
						break;
						
					default:
						throw new XMLStreamException("Unexpected start element found in 'datasets': " + start);
				}
			} else
				throw new XMLStreamException("Unexpected content found in 'datasets': " + nextTag);
		}
	}
	
	private static Change readChange(XMLEventReader reader, XMLEvent changeElement, Dataset tp) throws XMLStreamException {
		Map<String, String> changeElementAttrs = getAllAttributes(changeElement, "type");
		List<Name> fromNames = null;
		List<Name> toNames = null;
		List<Citation> citations = null;
		Map<String, String> properties = null;

		while(reader.hasNext()) {
			XMLEvent nextTag = reader.nextTag();
			
			if(nextTag.isEndElement()) {
				Stream<Name> from = (fromNames == null) ? Stream.empty() : fromNames.stream();
				Stream<Name> to = (toNames == null) ? Stream.empty() : toNames.stream();
				ChangeType type = ChangeType.of(changeElementAttrs.get("type"));
				Change ch = new Change(tp, type, from, to);
				
				if(properties != null)
					ch.propertiesProperty().putAll(properties);
				
				return ch;
			} else if(nextTag.isStartElement()) {
				StartElement start = nextTag.asStartElement();
				
				switch(start.getName().getLocalPart()) {
					case "from":
					case "to":
						List<XMLKeyValue> keyValues = getTagSeries(reader, "name");
						List<Name> names = new LinkedList<>();
						
						for(XMLKeyValue kv: keyValues) {
							Map<String, String> attrs = kv.getAttributes();

							if(attrs.containsKey("genus")) {
								Name name = null;
								String genus = attrs.get("genus");

								String specificEpithet = null;
								if(attrs.containsKey("specificEpithet"))
									specificEpithet = attrs.get("specificEpithet");
								
								if(attrs.containsKey("infraspecificEpithets")) {
									String infraspecificEpithets = attrs.get("infraspecificEpithets");
									
									if(specificEpithet == null)
										name = Name.get(genus, null, infraspecificEpithets);	
									else
										name = Name.get(genus, specificEpithet, infraspecificEpithets);
								} else {
									if(specificEpithet == null)
										name = Name.getFromGenus(genus);	
									else
										name = Name.get(genus, specificEpithet);
								}
								
								names.add(name);
							} else
								throw new XMLStreamException("Could not extract Name from: " + start);
						}
						
						if(start.getName().getLocalPart().equals("from")) {
							if(fromNames != null)
								throw new XMLStreamException("Duplicate 'from' names provided at: " + start);
						
							fromNames = names;
						} else if(start.getName().getLocalPart().equals("to")) {
							if(toNames != null)
								throw new XMLStreamException("Duplicate 'to' names provided at: " + start);
						
							toNames = names;
						} else
							throw new XMLStreamException("Unexpected name type: " + start);
						
						break;
					
					case "properties":
						List<XMLKeyValue> propertyKV = getTagSeries(reader, "property");
						properties = propertyKV.stream().collect(
							Collectors.toMap(
								kv -> kv.getAttributes().get("name"),
								kv -> kv.getValue()
							)
						);
						
						break;
						
					case "citations":
						citations = new LinkedList<>();
						
						while(reader.hasNext()) {
							nextTag = reader.nextTag();
							
							if(nextTag.isEndElement())
								break;
							else if(nextTag.isStartElement()) {
								start = nextTag.asStartElement();
								
								if(!start.getName().getLocalPart().equals("citation"))
									throw new XMLStreamException("Unexpected tag in 'citations': " + nextTag);
								
								if(start.isEndElement())
									// Ignore empty citations
									break;
								
								Citation c = readCitation(reader, start);
								citations.add(c);
								
							} else
								throw new XMLStreamException("Unexpected content in 'citations': " + nextTag);
						}
						
						break;
				}
			} else
				throw new XMLStreamException("Unexpected content, expecting start element: " + nextTag);
		}
		
		throw new XMLStreamException("Unexpected end of stream; no 'change' found");
	}
	
	public static Citation readCitation(XMLEventReader reader, StartElement citationTag) throws XMLStreamException {
		String citationText = null;
		Set<Tag> tags = null;
		Map<String, String> props = null;
		
		SimplifiedDate date = new SimplifiedDate(getAllAttributes(citationTag, "year", "month", "day"));
		
		while(reader.hasNext()) {
			XMLEvent nextTag = reader.nextTag();
			
			if(nextTag.isEndElement()) {
				if(citationText == null)
					throw new XMLStreamException("Citation text missing for citation ending at: " + nextTag);
				
				Citation citation = new Citation(citationText, date);
				if(props != null) {
					citation.getProperties().clear();
					citation.getProperties().putAll(props);
				}
				return citation;
			} else if(nextTag.isStartElement()) {
				StartElement start = nextTag.asStartElement();
				
				switch (start.getName().getLocalPart()) {
					case "cite":
						if(citationText != null)
							throw new XMLStreamException("Duplicate citation text provided at: " + start);
						StringBuilder citationSB = new StringBuilder();
						while(reader.hasNext()) {
							nextTag = reader.nextEvent();
							
							if(nextTag.isEndElement())
								break;
							else if(nextTag.isCharacters()) {
								citationSB.append(nextTag.asCharacters().getData());
							} else
								throw new XMLStreamException("Unexpected content in 'cite' in 'citation': " + nextTag);
						}	
						citationText = citationSB.toString();
						break;
						
					case "tags":
						List<XMLKeyValue> tagTags = getTagSeries(reader, "tag");
						if(tags != null)
							throw new XMLStreamException("Duplicate tags provided at: " + start);
						tags = tagTags.stream().map(kv -> Tag.fromName(kv.getValue())).collect(Collectors.toSet());
						break;
						
					case "properties":
						List<XMLKeyValue> properties = getTagSeries(reader, "property");
						if(props != null)
							throw new XMLStreamException("Duplicate properties provided at: " + start);
						props = properties.stream().collect(
								Collectors.toMap(
										kv -> kv.getAttributes().get("name"),
										kv -> kv.getValue()
								)
						);	
						break;
						
					default:
						throw new XMLStreamException("Unexpected content inside 'citation', expecting 'cite' or 'properties': " + start);
				}
			} else
				throw new XMLStreamException("Unexpected content in citation: " + nextTag);
		}
		
		throw new XMLStreamException("Document ended mid-citation");
	}
}
