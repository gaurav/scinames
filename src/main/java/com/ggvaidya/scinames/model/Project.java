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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.input.XmlStreamReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ggvaidya.scinames.model.filters.ChangeFilter;
import com.ggvaidya.scinames.model.filters.ChangeFilterFactory;
import com.ggvaidya.scinames.model.io.ProjectXMLReader;
import com.ggvaidya.scinames.model.rowextractors.NameExtractor;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorFactory;
import com.ggvaidya.scinames.util.ModificationTimeProperty;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

/**
 * A project consists of a series of Datasets arranged in a particular order.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class Project {
	public static final Logger LOGGER = Logger.getLogger(Project.class.getSimpleName());
	
	/* Constants */
	public static final String PROP_NAME_EXTRACTORS = "com.ggvaidya.scinames.model.Project.name_extractors";
	
	/* Instance variables */
	private StringProperty projectName;
	private ObjectProperty<File> projectFile;
	private ObservableMap<String, String> properties = FXCollections.observableHashMap();
	private ObjectProperty<ChangeFilter> changeFilterProperty = new SimpleObjectProperty<ChangeFilter>(ChangeFilterFactory.getNullChangeFilter());
	private NameClusterManager nameClusterManager = new NameClusterManager();
	private ObservableSet<ChangeType> changeTypes = FXCollections.observableSet(new HashSet<>());
	private ListProperty<Dataset> datasets = new SimpleListProperty<>(FXCollections.observableList(new LinkedList<Dataset>()));
	private ModificationTimeProperty lastModified = new ModificationTimeProperty();
	
	{
		datasets.addListener(new ListChangeListener<Dataset>() {
			@Override
			public void onChanged(ListChangeListener.Change<? extends Dataset> c) {
				lastModified.modified();
			}
		});
	}
	
	private ObservableMap<Name, List<Dataset>> timepointsByName = FXCollections.observableMap(new HashMap<>());
	private ObservableSet<Name> names = FXCollections.observableSet(new HashSet<>());
	private ObservableSet<Name> binomialNames = FXCollections.observableSet(new HashSet<>());
	
	/* Accessors */
	public String getName() { return projectName.getValue(); }
	public void setName(String newProjectName) { projectName.setValue(newProjectName); lastModified.modified(); }	
	public StringProperty projectNameProperty() { return projectName; }	
	public File getFile() { return projectFile.getValue(); }	
	public void setFile(File f) { projectFile.setValue(f); lastModified.modified(); }
	public ObjectProperty<File> projectFileProperty() { return projectFile; }
	public boolean isModified() { return lastModified.isModified(); }
	public ListProperty<Dataset> datasetsProperty() { return datasets; }
	public ObservableList<Dataset> getDatasets() { return datasets; }
	public ModificationTimeProperty lastModifiedProperty() { return lastModified; }	
	public ObservableSet<Name> namesProperty() { return names; }
	public ObservableSet<Name> binomialNamesProperty() { return binomialNames; }	
	public ObservableMap<Name, List<Dataset>> timepointsByNameProperty() { return timepointsByName; }
	public ObservableMap<String, String> propertiesProperty() { return properties; }
	public Stream<NameCluster> getSpeciesNameClusters() { return nameClusterManager.getSpeciesClustersAfterFiltering(this); }
	public ObservableSet<ChangeType> changeTypesProperty() { return changeTypes; }
	public ObjectProperty<ChangeFilter> changeFilterProperty() { return changeFilterProperty; }
	public ChangeFilter getChangeFilter() { return changeFilterProperty.get(); }
	public NameClusterManager getNameClusterManager() { return nameClusterManager; }
	
	public List<Dataset> getChecklists() {
		return datasets.stream().filter(ds -> ds.isChecklist()).collect(Collectors.toList());
	}
	
	/* Check if property is set. The default is always false. */
	public boolean isPropertySetTrue(String propName) {
		if(!properties.containsKey(propName))
			return false;
		
		return properties.get(propName).equalsIgnoreCase("yes");
	}
	
	/* Higher-order accessors */
	public Optional<Dataset> getFirstDataset() { 
		if(datasets.isEmpty())
			return Optional.empty();
		else
			return Optional.of(datasets.get(0));
	}
	
	public Optional<Dataset> getLastDataset() {
		if(datasets.isEmpty())
			return Optional.empty();
		else
			return Optional.of(datasets.get(datasets.size() - 1));
	}
	
	// getRecognizedNames() is one of the most expensive methods we have.
	// SO: if you call it through Project(), it gets cached for later use.
	// and telling the project its modified (through lastModified) blows
	// away the cache.
	private Map<Dataset, Set<Name>> recognizedNamesCache = new HashMap<>();
	{
		lastModified.addListener((a, b, c) -> recognizedNamesCache.clear());
	}
	
	/**
	 * Get all the recognized names at the end of a particular dataset.
	 * We memoize this with a cache, so this is more efficient than
	 * calling Dataset.getRecognizedName(Dataset). Given that implicit and
	 * explicit changes are cached now, though, it might not be *much* more
	 * efficient.
	 * 
	 * @param d The dataset you want recognized names from.
	 * @return The set of names recognized at the end of this dataset.
	 */
	public Set<Name> getRecognizedNames(Dataset d) {
		if(recognizedNamesCache.containsKey(d))
			return recognizedNamesCache.get(d);
		
		recognizedNamesCache.put(d, d.getRecognizedNames(this).collect(Collectors.toSet()));
		return recognizedNamesCache.get(d);
	}
	
	/**
	 * @return All the changes in this project.
	 */
	public Stream<Change> getChanges() {
		return datasets.stream().flatMap(t -> t.getChanges(this));
	}
	
	/**
	 * Add a ChangeFilter. These are stored as a linked list, so we tell the
	 * current ChangeFilter to add it, which will pass it on to the next and
	 * to the next and so on.
	 * 
	 * @param cf The ChangeFilter to be added.
	 */
	public void addChangeFilter(ChangeFilter cf) {
		if(changeFilterProperty.get() == null)
			changeFilterProperty.set(cf);
		else
			changeFilterProperty.get().addChangeFilter(cf);
	}
	
	/**
	 * Returns a list of dataset rows across all datasets for a particular name.
	 * 
	 * Note that while the API signature allows for duplicate rows to be summarized, this hasn't
	 * yet been implemented: we'll return a unique dataset row from multiple datasets even where
	 * the data in those rows is identical.
	 * 
	 * @param n The name to identify across all datasets.
	 * @return A Map<DatasetRow, Set<Dataset>> indicating rows to be returned.
	 */
	public Map<DatasetRow, Set<Dataset>> getRowsForName(Name n) {
		Map<Dataset, Set<DatasetRow>> timepointsPerRow = getDatasets().stream().collect(Collectors.toMap(
			tp -> tp,
			tp -> tp.getRowsByName(n)
		));
		
		final Map<DatasetRow, Set<Dataset>> results = new HashMap<>();
		for(Dataset tp: timepointsPerRow.keySet()) {
			Set<DatasetRow> rowsForTP = timepointsPerRow.get(tp);
			
			rowsForTP.forEach(r -> {
				if(!results.containsKey(r))
					results.put(r, new HashSet<>());
				
				results.get(r).add(tp);
			});
		}
		
		return results;
	}
	
	/**
	 * Summarize information on a particular Name across all datasets.
	 * 
	 * @param n Name to search all datasets for.
	 * @return A Map<DatasetColumn, Set<String>> showing values we know about for each dataset column.
	 */
	public Map<DatasetColumn, Set<String>> getDataForName(Name n) {
		Map<DatasetRow, Set<Dataset>> rowsForName = getRowsForName(n);
		final Map<DatasetColumn, Set<String>> results = new HashMap<>();
		
		for(DatasetRow row: rowsForName.keySet()) {
			for(DatasetColumn col: row.getColumns()) {
				String value = row.get(col);
				
				if(!results.containsKey(col))
					results.put(col, new HashSet<>());
				results.get(col).add(value);
			}
		}
		
		return results;
	}
	
	/**
	 * @return A set of all name extractors used in this project.
	 */
	public Set<List<NameExtractor>> getNameExtractors() {
		Set<List<NameExtractor>> set = datasets.stream().map(ds -> ds.getNameExtractors()).collect(Collectors.toSet());
		set.remove(NameExtractorFactory.getDefaultExtractors());
		return set;
	}
	
	@Override
	public String toString() {
		String modifiedStr = "";
		if(isModified())
			modifiedStr = "*";
		
		String fileStr = "";
		if(projectFile.isNotNull().get()) {
			try {
				fileStr = " (" + projectFile.get().getCanonicalPath() + ")";
			} catch(IOException e) {
				fileStr = " (" + projectFile.get().getAbsolutePath() + ")";
			}
		}
		
		String sizeStr = "";
		if(datasets.isNotNull().get())
			sizeStr = ": " + datasets.size() + " time points";
		
		return projectName.get() + modifiedStr + fileStr + sizeStr;
	}
	
	/* Managing timepoints */
	
	/**
	 * Add a new dataset to this project. This manages setting the previous dataset for
	 * the new dataset and calculates name clusters for all names.
	 * 
	 * @param ds Dataset to add.
	 */
	public void addDataset(Dataset ds) {
		Dataset prev = null;
		
		if(!datasets.isEmpty())
			prev = datasets.get(datasets.size() - 1);
		
		// Add dataset to list of datasets.
		datasets.add(ds);
		
		// Tell dataset who its previous submitter is.
		ds.setPreviousDataset(Optional.of(this), Optional.ofNullable(prev));
		lastModified.modified();
		
		// Debugging code!
		//if(ds.getName().startsWith("aou_1_07"))
		//	LOGGER.info("Referenced names for aou_1_07: " + ds.getReferencedNames().collect(Collectors.toList()));
		
		// Add all referenced names to the nameClusterManager. That way, a name cluster
		// must exist for each of them (and they'll be lumped in with others if we have
		// existing renames to that effect).
		ds.getReferencedNames().forEach(n -> nameClusterManager.addCluster(new NameCluster(ds, n)));
		
		// Old debugging code, leaving it just in case I need something like this again.
		if(ds.getName().startsWith("aou_1.csv")) {
			LOGGER.fine("aou_1.csv has been loaded: " + ds.getReferencedNames().count());
			LOGGER.fine("Name clusters created: " + nameClusterManager.getClusters().count());
			LOGGER.fine("Is Tringa ptilocnemis referenced? " + 
				ds.getReferencedNames().filter(n -> n.equals(Name.get("Tringa", "ptilocnemis")))
					.collect(Collectors.toList())
			);
			LOGGER.fine("Name clusters created for Tringa ptilocnemis: " + 
				nameClusterManager.getClusters()
					.filter(cluster -> cluster.contains(Name.get("Tringa", "ptilocnemis")))
					.collect(Collectors.toList())
			);
			LOGGER.fine("Name cluster for '" + Name.get("Tringa", "ptilocnemis") + "': " + nameClusterManager.getCluster(Name.get("Tringa", "ptilocnemis")));
		}
		
		// Renames lead to synonymies. This is the first time we use getChanges(...), so renames
		// that are being filtered out will NOT affect name clusters.
		ds.getChanges(this).filter(ch -> ch.getType().equals(ChangeType.RENAME)).forEach(c ->
			c.getFrom().forEach(from ->
				c.getTo().forEach(
					to -> nameClusterManager.addCluster(new Synonymy(from, to, ds))
				)
			)
		);
		
		// Add names referenced in this dataset to our project-level indices.
		ds.getReferencedNames().forEach((Name n) -> {
			names.add(n);
			n.asBinomial().forEach(binomialName -> binomialNames.add(binomialName));
			
			if(!timepointsByName.containsKey(n))
				timepointsByName.put(n, new ArrayList<Dataset>());
			
			timepointsByName.get(n).add(ds);
		});
		
		// Add change types referenced in this dataset to our project-level indices.
		// As before, filtered changes will be ignored.
		Set<ChangeType> newChangeTypes = ds.getChanges(this).map(c -> c.getType()).collect(Collectors.toSet());
		changeTypes.addAll(newChangeTypes);
		
		// Finally, changes in the new dataset should change this database.
		ds.lastModifiedProperty().addListener((a, b, c) -> lastModified.modified());
		
		LOGGER.fine("After adding " + ds + ", name clusters look like this: " + getNameClusterManager().toString());
	}	
	
	/* Constructors */
	
	public Project(String projectName, File projectFile) {
		this.projectName = new SimpleStringProperty(projectName);
		this.projectFile = new SimpleObjectProperty<>(projectFile);
	}
	
	public Project() {
		this("Unnamed project", null);
	}
	
	public void serializeToDocument(Document doc) {
		// Add top-level element.
		Element project = doc.createElement("project");
		project.setAttribute("name", getName());
		doc.appendChild(project);
		
		// Set up some properties.
		properties.put(PROP_NAME_EXTRACTORS, 
			getNameExtractors().stream().map(lne -> 
				NameExtractorFactory.serializeExtractorsToString(lne)).distinct()
					.sorted().collect(Collectors.joining("; "))
		);
		
		// Write out properties.
		Element propertiesElement = doc.createElement("properties");
		for(String key: properties.keySet()) {
			Element p = doc.createElement("property");
			p.setAttribute("name", key);
			p.setTextContent(properties.get(key));
			propertiesElement.appendChild(p);
		}
		project.appendChild(propertiesElement);
		
		// Add filters.
		Element filtersElement = doc.createElement("filters");
		Deque<ChangeFilter> changeFilters = new LinkedList<>();
		{
			// We need to read the filters inside-out, so they'll be recreated
			// the right way around.
			ChangeFilter cf = getChangeFilter();
			while(cf != null) {
				changeFilters.addLast(cf);
				cf = cf.getPrevChangeFilter();
			}
		}
		changeFilters.forEach(cf -> {
			// Skip any nulls.
			if(cf.getShortName().equals("null")) return;
			
			filtersElement.appendChild(cf.serializeToElement(doc));
		});
		
		project.appendChild(filtersElement);
		
		// List all timepoints.
		Element timepointsElement = doc.createElement("datasets");
		for(Dataset tp: getDatasets()) {
			Element t = tp.serializeToElement(doc);
			timepointsElement.appendChild(t);
		}
		project.appendChild(timepointsElement);
	}
	
	/*
	public static Project serializeFromDocument(Document doc, File file) throws SAXException, IllegalStateException {
		NodeList projects = doc.getElementsByTagName("project");
		if(projects.getLength() == 0) {
			throw new SAXException("No projects in project XML file");
		} else if(projects.getLength() > 1) {
			throw new SAXException("Too many projects in project XML file: " + projects.toString());
		}
		
		Node project = projects.item(0);
		String projName = "No name provided";

		Node projNameNode = project.getAttributes().getNamedItem("name");
		if(projNameNode != null) {
			projName = projNameNode.getNodeValue();
		}
		
		Project newProject = new Project(projName, file);
		
		// Iterate all children
		NodeList children = project.getChildNodes();
		
		for(int x = 0; x < children.getLength(); x++) {
			Node node = children.item(x);
			
			if(node.getNodeType() != Node.ELEMENT_NODE)
				continue;
			
			if(node.getNodeName().equals("properties")) {
				NodeList properties = node.getChildNodes();

				for(int y = 0; y < properties.getLength(); y++) {
					Node child = properties.item(y);

					if(child.getNodeType() != Node.ELEMENT_NODE)
						continue;
					
					if(!child.getNodeName().equals("property"))
						throw new SAXException("Unexpected node '" + child.getNodeName() + "' found in project properties: " + child);
					
					Node propNameNode = child.getAttributes().getNamedItem("name");
					if(propNameNode == null)
						throw new SAXException("Unnamed property in properties: " + child);
					String propName = propNameNode.getNodeValue();
					String propValue = node.getTextContent();
					
					newProject.propertiesProperty().put(propName, propValue);
				}
			} else if(node.getNodeName().equals("timepoints")) {
				NodeList timepoints = node.getChildNodes();

				for(int y = 0; y < timepoints.getLength(); y++) {
					Node child = timepoints.item(y);

					if(child.getNodeType() != Node.ELEMENT_NODE)
						continue;
					
					Dataset timepoint = Dataset.serializeFromNode(newProject, child);
					newProject.addDataset(timepoint);
				}
			} else if(node.getNodeName().equals("filters")) {
				NodeList filters = node.getChildNodes();

				Deque<ChangeFilter> changeFilters = new ArrayDeque<>();
				for(int y = 0; y < filters.getLength(); y++) {
					Node filterNode = filters.item(y);

					if(filterNode.getNodeType() != Node.ELEMENT_NODE)
						continue;
					
					if(!filterNode.getNodeName().equals("filter"))
						throw new SAXException("Unexpected node in 'filters': " + filterNode);
					
					changeFilters.add(ChangeFilterFactory.createFilterFromNode(newProject, filterNode));
				}
				
				changeFilters.forEach(
					cf -> newProject.addChangeFilter(cf)
				);
			} else {
				throw new SAXException("Unexpected node: " + node);
			}
		}
		
		// Load all timepoints.
		NodeList timepointsNodes = doc.getElementsByTagName("timepoints");
		if(timepointsNodes.getLength() > 1) {
			throw new SAXException("Too many timepoints in project XML file: " + timepointsNodes.toString());
		}

		newProject.lastModified.saved();
		return newProject;
	}*/
	
	public static Project loadFromFile(File loadFromFile) throws IOException {
		Project project = null;
		
		XMLInputFactory factory = XMLInputFactory.newFactory();
		factory.setXMLReporter(new XMLReporter() {
			@Override
			public void report(String message, String errorType, Object relatedInformation, Location location)
					throws XMLStreamException {
				LOGGER.warning(
					errorType + " while loading project from XML file '" + loadFromFile + "': " +
						message +
						" (related info: " + relatedInformation.toString() + ", location: " + location
				);
			}
		});
		
		try {
			XMLEventReader reader = factory.createXMLEventReader(new XmlStreamReader(new GZIPInputStream(new FileInputStream(loadFromFile))));
			
			project = ProjectXMLReader.read(reader);
			project.setFile(loadFromFile);
			project.lastModifiedProperty().saved();
			
			reader.close();
			
		} catch (XMLStreamException ex) {
			throw new IOException("Could not read project from XML file '" + loadFromFile + "': " + ex, ex);
		}
		
		return project;
		
		/*
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		// Configure upcoming document builder.
		dbf.setIgnoringComments(true);
		dbf.setIgnoringElementContentWhitespace(true);
		
		Document docProject;
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			docProject = db.parse(loadFromFile);
			
		} catch (SAXException ex) {
			throw new IOException("Could not load project XML file: " + ex);
			
		} catch (ParserConfigurationException ex) {
			throw new RuntimeException("Could not load XML parser configuration: " + ex);
		
		}
		
		// Load project.
		Project newProject;
		try {
			newProject = serializeFromDocument(docProject, loadFromFile);
		} catch(SAXException e) {
			throw new IOException("XML file loaded but project could not be read: " + e);
		} catch(IllegalStateException e) {
			throw new IOException("XML file contains errors in content: " + e);
		}
		
		return newProject;
		*/
	}
	
	public void saveToFile() throws IOException {
		File saveToFile = projectFile.getValue();
		
		if(saveToFile == null)
			throw new IOException("Project file not set: nowhere to save to!");
		
		/*
		XMLOutputFactory factory = XMLOutputFactory.newFactory();
		
		try {
			XMLStreamWriter writer = factory.createXMLStreamWriter(new FileWriter(saveToFile));
			
			writer.writeStartDocument();
			
			serializeToXMLStream(writer);
			
			writer.writeEndDocument();
			writer.flush();
			
			// Success!
			lastModified.saved();
			
		} catch (XMLStreamException ex) {
			throw new IOException("Could not write project to XML file '" + saveToFile + "': " + ex);
		}*/
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		// Create a document representation of this project.
		Document docProject;
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			docProject = db.newDocument();
			serializeToDocument(docProject);
			
		} catch (ParserConfigurationException ex) {
			Logger.getLogger(Project.class.getName()).log(Level.SEVERE, null, ex);
			return;
		}
		
		// Write the document representation of this project
		// as XML.
		TransformerFactory tfc = TransformerFactory.newInstance();
		try {
			OutputStream outputStream = new GZIPOutputStream(new FileOutputStream(saveToFile));
			StreamResult res = new StreamResult(outputStream);
			
			Transformer t = tfc.newTransformer();
			DOMSource ds = new DOMSource(docProject);
			
			t.setOutputProperty(OutputKeys.METHOD, "xml");
			t.setOutputProperty(OutputKeys.VERSION, "1.0"); // Do NOT change to 1.1 -- this leads to complex problems!
			t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			t.setOutputProperty(OutputKeys.STANDALONE, "yes");
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			t.transform(ds, res);
			
			// Success!
			lastModified.saved();
			outputStream.close();
		} catch (TransformerConfigurationException ex) {
			throw new IOException("Could not write out XML to '" + saveToFile + "': " + ex);
		} catch (TransformerException ex) {
			throw new IOException("Could not write out XML to '" + saveToFile + "': " + ex);
		}
	}
	
	/* Find lumps and splits */
	public Stream<Change> getLumpsAndSplits() {
		return getChanges().filter(ch -> ch.getType().equals(ChangeType.LUMP) || ch.getType().equals(ChangeType.SPLIT));
	}
	
	/* Finding partial and complete reversions */
	
	/**
	 * Find all cases of lumps and splits that partially revert previous lumps and splits. We define these
	 * as sharing two names in opposite slots (i.e. two names from 'from' in 'to' or vice versa) after inverting
	 * the type (lump -> split, split -> lump). 
	 * 
	 * @param changeReversed The change to reverse.
	 * @return Stream of changes that are reversions or repeats of previous lumps and splits.
	 */
	public Stream<Change> getChangesReversing(Change changeReversed) {
		List<NameCluster> changeReversedTo = nameClusterManager.getClusters(changeReversed.getTo());
		List<NameCluster> changeReversedFrom = nameClusterManager.getClusters(changeReversed.getFrom());
		
		return getLumpsAndSplits().filter(
			// When change is inverted (LUMP -> SPLIT, SPLIT -> LUMP):
			ch -> (ch.getType().equals(changeReversed.getType().invert()) && (
				// AND either contains TWO of the 'from' clusters in the 'to' slot.
				nameClusterManager.getClusters(ch.getFrom()).stream().filter(nc -> changeReversedTo.contains(nc)).count() >= 2

				// OR contains TWO of the 'to' clusters in the 'from' slot.
				|| nameClusterManager.getClusters(ch.getTo()).stream().filter(nc -> changeReversedFrom.contains(nc)).count() >= 2
			))
		);
	} 
	
	/**
	 * Return a stream of perfectly reversing changes. This is cases where the change is the exact opposite,
	 * with all the names in 'from' found in 'to' and all the names in 'to' found in 'from'.
	 * 
	 * @param changeReversed The change to reverse.
	 * @return A Stream of all changes that perfectly reverse the source change.
	 */
	public Stream<Change> getChangesPerfectlyReversing(Change changeReversed) {
		return getChangesReversing(changeReversed).filter(
			ch -> (
				// How to be a perfect reversal: be the same but opposite
				changeReversed.getType().equals(ch.getType().invert())
				&& nameClusterManager.getClusters(ch.getFrom()).equals(nameClusterManager.getClusters(changeReversed.getTo()))
				&& nameClusterManager.getClusters(ch.getTo()).equals(nameClusterManager.getClusters(changeReversed.getFrom()))
			)
		);
	}
	
	public String getPerfectlyReversingSummary(Change changeReversed) {
		List<Change> perfectlyReversingChanges = getChangesPerfectlyReversing(changeReversed).collect(Collectors.toList());
		
		if(perfectlyReversingChanges.isEmpty())
			return "";
		
		// Our change is also part of this sequence.
		perfectlyReversingChanges.add(changeReversed);
		
		// Figure out the order in which the changes took place.
		Collections.sort(
			perfectlyReversingChanges, 
			(Change c1, Change c2) -> c1.getDataset().getDate().compareTo(c2.getDataset().getDate())
		);

		return perfectlyReversingChanges.stream()
			.map(
				ch -> ch.getType().getType()
				+ " (" + ch.getDataset().getDate().getYearAsString() + ")"
			).collect(Collectors.joining(" -> "))
				
			// Allows change sequences to be uniquely identified
			+ " [starting with change id " + perfectlyReversingChanges.get(0).getId() + "]";
	}
}