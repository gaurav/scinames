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

import com.ggvaidya.scinames.model.rowextractors.NameExtractor;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorFactory;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorParseException;
import com.ggvaidya.scinames.util.ModificationTimeProperty;
import com.ggvaidya.scinames.util.SimplifiedDate;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A Dataset includes names and other data tied to those names.
 * 
 * DESIGN: Each dataset contains three pieces of information:
 *	- A list of rows, each associated with ONE Name 
 *  - A set of explicit changes that the project asserts took place at this dataset
 *  - A set of unexpected changes that have taken place since the previous checklist.
 * 
 * This means that we can get rid of our other two data types, since:
 *	- A Checklist is simply a Dataset without any data apart from name.
 *  - A ChecklistDiff is simply a set of explicit changes without any data.
 * 
 * TIMEPOINT DEPRECATION STRATEGY (as of March 7, 2017):
 *  - 1. Rewrite dataset so that it incorporates the change management logic of
 *		 a ChecklistDiff.
 *  - 2. Rewrite XML export code so that we use XMLEventWriter to write everything.
 *  - 3. Rewrite XML import code so that we can use XMLEventReader to read everything.
 *		- At this point, everything will work on this one datatype.
 *  - 4. Add support for providing metadata on columns: so a particular column
 *		 can be marked as "scientificName", or "genus" or "specificEpithet".
 *		 This needs to be an entire subsystem within Dataset.
 *  - 4. ... ?
 *  - 5. Profit!
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class Dataset implements Citable, Comparable {
	private static final Logger LOGGER = Logger.getLogger(Dataset.class.getSimpleName());
	
	/* Private variables */
	private Project project;
	private String name;
	private ObjectProperty<SimplifiedDate> dateProperty = new SimpleObjectProperty<>(SimplifiedDate.MIN);
	private Dataset prevDataset;
	private boolean flag_isChecklist = true;
	private ModificationTimeProperty lastModified = new ModificationTimeProperty();
	
	// Data in this dataset.
	private ObservableList<DatasetColumn> columns = FXCollections.observableArrayList();
	private ObservableList<DatasetRow> rows = FXCollections.observableList(new LinkedList<>());
	private ObservableList<Change> explicitChanges = FXCollections.observableList(new LinkedList<>());
	private ObservableList<Change> implicitChanges = FXCollections.observableList(new LinkedList<>());	
	
	{
		// columns.addListener(c -> lastModified.modified());
		explicitChanges.addListener((Observable o) -> {
			lastModified.modified();
		});
		dateProperty.addListener(c -> lastModified.modified());
	}
	
	/* Accessors */
	public Optional<Project> getProject() { return Optional.ofNullable(project); }
	public void setProject(Project p) { project = p; lastModified.modified(); }
	public String getName() { return name; }
	public void setName(String n) { name = n; lastModified.modified(); }
	public ObjectProperty<SimplifiedDate> dateProperty() { return dateProperty; }
	public SimplifiedDate getDate() { return dateProperty.getValue(); }
	public ModificationTimeProperty lastModifiedProperty() { return lastModified; }
	public ObservableList<DatasetColumn> getColumns() { return columns; }
	public ObservableList<DatasetRow> rowsProperty() { return rows; }
	public ObservableList<Change> explicitChangesProperty() { return explicitChanges; }
	public Stream<DatasetRow> getRowsAsStream() { return rows.stream(); }
	public boolean isChecklist() { return flag_isChecklist; }
	public void setIsChecklist(boolean flag) { flag_isChecklist = flag; lastModified.modified(); }
	
	/* Higher order accessors */
	@Override
	public int compareTo(Object o) {
		Dataset tp = (Dataset) o;
		
		return getDate().compareTo(tp.getDate());
	}
	
	@Override public void setDate(SimplifiedDate sd) { 
		dateProperty.setValue(sd);
		lastModified.modified();
	}
	
	public void setColumns(List<DatasetColumn> cols) { 
		columns.clear();
		columns.addAll(cols);
	}
	
	public Set<DatasetRow> getRowsByName(Name name) {
		Set<Name> namesInAllRows = getNamesInAllRows();
		if(!namesInAllRows.contains(name)) return new HashSet();
		
		return getNamesByRow().entrySet().stream()
			.filter(entry -> entry.getValue().contains(name))
			.map(entry -> entry.getKey())
			.collect(Collectors.toSet());
	}
	
	/* Managing previous timepoint */
	public Dataset getPreviousDataset() { return prevDataset; }
	
	public void setPreviousDataset(Optional<Project> proj, Optional<Dataset> tp) {
		prevDataset = tp.orElse(null);
		
		// Implicit changes are meaningless for non-checklists.
		implicitChanges.clear();	
		if(flag_isChecklist) {
			Set<Name> names = getNamesInAllRows();
			Set<Name> prevNames;

			if(proj.isPresent() && tp.isPresent()) {
				Project project = proj.orElse(null);
				prevNames = prevDataset.getRecognizedNames(project).collect(Collectors.toSet());
			} else {
				prevNames = new HashSet<>();
			}

			// What names do explicit changes add or remove?
			Set<Name> addedByExplicitChanges = explicitChanges.stream().flatMap(ch -> ch.getToStream()).collect(Collectors.toSet());
			Set<Name> deletedByExplicitChanges = explicitChanges.stream().flatMap(ch -> ch.getFromStream()).collect(Collectors.toSet());		

			// Calculate implicit changes that can't be explained by an explicit change.
			Stream<Change> additions = names.stream()
				.filter(n -> !prevNames.contains(n) && !addedByExplicitChanges.contains(n))
				.map(n -> new Change(this, Change.ADDITION, Stream.empty(), Stream.of(n)));
				//.filter(project.getChangeFilter());
			Stream<Change> deletions = prevNames.stream()
				.filter(n -> !names.contains(n) && !deletedByExplicitChanges.contains(n))
				.map(n -> new Change(this, Change.DELETION, Stream.of(n), Stream.empty()));
				//.filter(project.getChangeFilter());
			
			implicitChanges.addAll(additions.collect(Collectors.toList()));
			implicitChanges.addAll(deletions.collect(Collectors.toList()));
		}
	}
	
	/* Names management */
	/*
		There are several ways in which we could organize this. The approach
		we're going with is:
			- Each row does not know what names are in it.
			- We keep a centralized list of names for each row.
			- We also keep a centralized list of all names.
			- The ONLY way to access either of these datasets is through
			  memoized functions, i.e. they will NOT be stored.
			- That way, you won't trigger name parsing until you need it;
			  when you need it, it'll be done once and will be fairly efficient.
			- Updating a row or column will reset the memoized caches, so we
			  don't waste any more time.
	*/
	
	{
		// If the columns or rows change, we need to reparse ALL names.
		columns.addListener((ListChangeListener.Change<? extends DatasetColumn> c) -> resetNamesCaches());
		rows.addListener((ListChangeListener.Change<? extends DatasetRow> c) -> resetNamesCaches());
		
	}
	
	private void resetNamesCaches() {
		LOGGER.entering(Dataset.class.getSimpleName(), "resetNamesCaches");
		namesByRow = null;
		namesInRows = null;
		rowsByName = null;
	}
	
	public Set<Name> getNamesInRow(DatasetRow row) {
		Map<DatasetRow, Set<Name>> namesByRow = getNamesByRow();
		
		LOGGER.entering(Dataset.class.getSimpleName(), "getNamesInRow", row);
		
		if(namesByRow.containsKey(row))
			return namesByRow.get(row);
		else
			return new HashSet();
	}

	public Set<Name> getNamesInAllRows() {
		// Make sure our caches are up to date.
		getNamesByRow();
		
		// At this point, they should be!
		return namesInRows;
	}
	
	public Map<Name, Set<DatasetRow>> getRowsByName() {
		getNamesByRow();
		
		return rowsByName;
	}
	
	public Map<Name, Set<DatasetRow>> rowsByName = null;
	private Map<DatasetRow, Set<Name>> namesByRow = null;
	private Set<Name> namesInRows = null;
	private ModificationTimeProperty namesByRowLastModified = new ModificationTimeProperty();
	{
		lastModified.addListener((a, b, c) -> namesByRowLastModified.modified());
	}
	public Map<DatasetRow, Set<Name>> getNamesByRow() {
		LOGGER.entering(Dataset.class.getSimpleName(), "getNamesByRow");
		
		if(namesByRow == null || namesByRowLastModified.isModified()) {
			LOGGER.log(Level.FINE, "Recalculating names using extractors: {0}", NameExtractorFactory.serializeExtractorsToString(getNameExtractors()));
			
			long startTime = System.nanoTime();
			
			// Recalculate all.
			resetNamesCaches();
			
			namesByRow = new HashMap<>();
			namesInRows = new HashSet<>();
			rowsByName = new HashMap<>();
			
			for(DatasetRow row: rows) {
				Set<Name> names = new HashSet();
				if(getNameExtractors() != null && getNameExtractors().size() > 0)
					names = NameExtractorFactory.extractNamesUsingExtractors(getNameExtractors(), row);
				namesByRow.put(row, names);
				namesInRows.addAll(names);
				
				for(Name n: names) {
					if(!rowsByName.containsKey(n))
						rowsByName.put(n, new HashSet<>());
					
					rowsByName.get(n).add(row);
				}
			}
			namesByRowLastModified.saved();
			
			// Report on how long this took.
			double timeTaken = (System.nanoTime() - startTime)/1e6d;
			double timePerRow  = 0;
			if(rows.size() > 0)
				timePerRow = timeTaken/rows.size();
			
			LOGGER.log(Level.FINE, "getNamesByRow() extracted {0} in {1} seconds ({2} seconds/row) on dataset {3}", 
				new Object[]{
					namesInRows.size(), 
					timeTaken, 
					timePerRow, 
					this});
		}
		
		return namesByRow;
	}
	
	public String getNameExtractorsAsString() {
		return NameExtractorFactory.serializeExtractorsToString(nameExtractors);
	}
	
	public void setNameExtractorsString(String str) throws NameExtractorParseException {
		nameExtractors = NameExtractorFactory.getExtractors(str);
		LOGGER.log(Level.FINE, "setNameExtractorsString() called, extractors now set to {0}", NameExtractorFactory.serializeExtractorsToString(nameExtractors));
		resetNamesCaches();
	}
	
	private List<NameExtractor> nameExtractors = NameExtractorFactory.getDefaultExtractors();
	public List<NameExtractor> getNameExtractors() { return nameExtractors; }
	
	public Stream<Name> getReferencedNames() {
		Stream<Name> namesFromData = getNamesInAllRows().stream();
		Stream<Name> namesFromChanges = explicitChanges.stream().flatMap(ch -> Stream.concat(ch.getFromStream(), ch.getToStream()));

		// LOGGER.log(Level.FINE, "dataset.getReferencedNames(): getNamesInAllRows() at {0}", getNamesInAllRows().size());
		
		return Stream.concat(namesFromData, namesFromChanges).distinct();
	}

	public Stream<Name> getRecognizedNames(Project project) {
		// Start with names we explicitly add.
		Stream<Name> names = getChanges(project).flatMap(ch -> ch.getToStream());
		
		if(flag_isChecklist) {
			// If this is a checklist, then we use the names we have now.
			names = Stream.concat(names, getNamesInAllRows().stream());
		} else {
			// If this is not a checklist, then pass through previously recognized names.
			if(prevDataset != null)
				names = Stream.concat(names, prevDataset.getRecognizedNames(project));
		}
		
		// Delete names we explicitly delete.
		Set<Name> deletedNames = getChanges(project)
			.flatMap(ch -> ch.getFromStream())
			.collect(Collectors.toSet());
		return names.filter(n -> !deletedNames.contains(n)).distinct();
	}

	public String getNameCountSummary(Project project) {
		return getRecognizedNames(project).count() + " (" + getReferencedNames().count() + " in this dataset)";
	}

	public String getBinomialCountSummary(Project project) {
		return getRecognizedNames(project).map(n -> n.getBinomialName()).distinct().count() + " (" + getReferencedNames().map(n -> n.getBinomialName()).distinct().count() + " in this dataset)";
	}
	
	/* Change management */
	public void onChangeChanged(Optional<Project> project, Change change) {
		LOGGER.entering(Dataset.class.getSimpleName(), "project = " + project + ", change = " + change);
		
		if(explicitChanges.contains(change)) {
			// Explicit changes affect how implicit changes are processed;
			// so if explicit changes change, then we need to recalculate
			// implicit changes.
			setPreviousDataset(project, Optional.ofNullable(prevDataset));
		} else if(implicitChanges.contains(change)) {
			// It's an implicit change? Well, it just got promoted to 
			// an explicit change.
			implicitChanges.remove(change);
			explicitChanges.add(change);
		} else {
			// We don't know about this change? Add it now!
			explicitChanges.add(change);
			
			// And then refire the notification.
			onChangeChanged(project, change);
			return;
		}
		
		// Whatever happens, we've changed, as has the project.
		lastModified.modified();
	}
	
	public Stream<Change> getExplicitChanges(Project p) {
		return explicitChanges.stream().filter(p.getChangeFilter());
	}
	
	public Stream<Change> getImplicitChanges(Project p) {
		// TODO: do we trust onChangeChanged() to track this
		return implicitChanges.stream().filter(p.getChangeFilter());
	}
	
	public String getExplicitChangesCountSummary(Project p) {
		if(getExplicitChanges(p).count() == 0)
			return "None";
		
		Map<Change.Type,Long> changeCounts = getExplicitChanges(p).collect(Collectors.groupingBy(Change::getType, Collectors.counting()));
		String changes_by_type = changeCounts.entrySet().stream()
				.sorted((a, b) -> b.getValue().compareTo(a.getValue()))
				.map(e -> e.getValue() + " " + e.getKey())
				.collect(Collectors.joining(", "));
		
		return getExplicitChanges(p).count() + " (" + changes_by_type + ")";
	}
	
	public String getImplicitChangesCountSummary(Project p) {
		if(getImplicitChanges(p).count() == 0)
			return "None";
		
		Map<Change.Type,Long> implicitChangeCounts = getImplicitChanges(p).collect(Collectors.groupingBy(Change::getType, Collectors.counting()));;
		String implicit_changes_by_type = implicitChangeCounts.entrySet().stream()
			.sorted((a, b) -> b.getValue().compareTo(a.getValue()))
			.map(e -> e.getValue() + " " + e.getKey())
			.collect(Collectors.joining(", "));
		
		return getImplicitChanges(p).count() + " implicit changes (" + implicit_changes_by_type + ")";
	}

	public Stream<Change> getAllChanges() {
		return Stream.concat(explicitChanges.stream(), implicitChanges.stream());
	}

	public Stream<Change> getAllChanges(Change.Type type) {
		return getAllChanges().filter(ch -> ch.getType().equals(type));
	}
	
	public Stream<Change> getChanges(Project project) {
		return getAllChanges().filter(project.getChangeFilter());
	}
	
	public Stream<Change> getChanges(Project project, Change.Type type) {
		return getAllChanges().filter(ch -> ch.getType().equals(type)).filter(project.getChangeFilter());
	}

	@Override
	public String getCitation() {
		return getName() + " (" + getDate() + ")";
	}
	
	@Override
	public String toString() {
		String str_treatAsChecklist = (flag_isChecklist ? "Checklist " : "Dataset ");
		
		return str_treatAsChecklist + getCitation();
	}
	
	public String asTitle() {
		return toString() + " (" + rows.size() + " rows, " + getReferencedNames().count() + " names)";
	}
	
	/* Data load */
	/**
	 * Load a timepoint from a file. Checklists can be loaded from plain text
	 * files containing a list of names, while ChecklistDiffs can be loaded
	 * in TaxDiff format.
	 * 
	 * @param project Project within which this load is happening.
	 * @param f File containing timepoint.
	 * @return Timepoint contained in that file.
	 * @throws IOException If the file isn't in the right format.
	 */
	public static Dataset loadFromFile(Project project, File f) throws IOException {
		String firstLine;
		try (LineNumberReader r = new LineNumberReader(new FileReader(f))) {
			// Load the first line to try to identify the file type.
			firstLine = r.readLine();
		}
		
		// The most basic type of file is a TaxDiff file, which always
		// begins with:
		if(ChecklistDiff.pTaxDiffFirstLine.matcher(firstLine).matches()) {
			return ChecklistDiff.fromTaxDiffFile(f);
		}
		
		// Any file with a '.csv' extension must be a Dataset.
		String fileName = f.getName().toLowerCase();
		if(fileName.endsWith(".csv") || fileName.endsWith(".tsv")) {
			CSVFormat csvFormat = CSVFormat.DEFAULT;
			if(fileName.endsWith(".tsv"))
				csvFormat = CSVFormat.TDF
					.withQuote(null); // We need this to load the AmphibiaWeb files.
			
			Dataset ds = Dataset.fromCSV(csvFormat, f);
			
			// Try all name extractors, see which one matches the most names.
			Set<List<NameExtractor>> allAvailableNameExtractors = project.getNameExtractors();
			allAvailableNameExtractors.add(NameExtractorFactory.getDefaultExtractors());

			LOGGER.info("Starting name extractor comparisons");
			List<NameExtractor> bestExtractor = null;
			long bestExtractorCount = Long.MIN_VALUE;
			for(List<NameExtractor> extractor: allAvailableNameExtractors) {
				long count = ds.rows.stream().flatMap(
						row -> NameExtractorFactory.extractNamesUsingExtractors(extractor, row).stream()
				).distinct().count();

				if(count > bestExtractorCount) {
					bestExtractorCount = count;
					bestExtractor = extractor;
				}
			}
			LOGGER.info("Finished name extractor comparisons: best extractor at " + bestExtractorCount + " names was " + NameExtractorFactory.serializeExtractorsToString(bestExtractor));
			
			try {
				ds.setNameExtractorsString(NameExtractorFactory.serializeExtractorsToString(bestExtractor));
			} catch(NameExtractorParseException ex) {
				// Forget about it. We'll go with the default.
			}
			
			return ds;
		}
		
		// If all else fails, try to parse this as a list of recognized species.
		return Checklist.fromListInFile(f);
	}
	
	/* Constructor  */
	public Dataset(String name, SimplifiedDate date) {
		this.name = name;
		dateProperty.setValue(date);
	}
	
	// Blank constructor
	public Dataset() {
		
	}

	/* Serialization */
	
	/**
	 * Load this dataset from a CSV file. We load the entire CSV file, except
	 * for blank cells.
	 * 
	 * @param project The project to which the resulting Dataset should belong
	 * @param csvFormat The CSV format of the input file.
	 * @param csvFile The input file to load.
	 * @return
	 * @throws IOException 
	 */
	public static Dataset fromCSV(CSVFormat csvFormat, File csvFile) throws IOException {
		Dataset dataset = new Dataset(csvFile.getName(), new SimplifiedDate());
		
		CSVParser parser = csvFormat.withHeader().parse(new FileReader(csvFile));
		Map<String, Integer> headerMap = parser.getHeaderMap();
		
		dataset.setColumns(headerMap.entrySet().stream().sorted((Object o1, Object o2) -> {
			Map.Entry<String, Integer> e1 = (Map.Entry) o1;
			Map.Entry<String, Integer> e2 = (Map.Entry) o2;
			
			return e1.getValue().compareTo(e2.getValue());
		}).map(e -> e.getKey()).map(colName -> DatasetColumn.of(colName)).collect(Collectors.toList()));
		
		dataset.rows.clear();
		dataset.rows.addAll(
			parser.getRecords().stream()
				.map(record -> {  DatasetRow row = new DatasetRow(); row.putAll(record.toMap()); return row;})
				.collect(Collectors.toList())
		);
		
		return dataset;
	}
	
	public Element serializeToElement(Document doc) {
		Element datasetElement = doc.createElement("dataset");
		
		datasetElement.setAttribute("name", name);
		datasetElement.setAttribute("is_checklist", flag_isChecklist ? "yes" : "no");
		dateProperty.getValue().setDateAttributesOnElement(datasetElement);
		
		// Properties
		//  - 1. name extractor
		datasetElement.setAttribute("nameExtractors", getNameExtractorsAsString());
		
		Element changesElement = doc.createElement("changes");
		for(Change ch: explicitChanges) {
			Element changeElement = ch.serializeToElement(doc);
			changesElement.appendChild(changeElement);
		}
		datasetElement.appendChild(changesElement);
		
		Element columnsElement = doc.createElement("columns");
		for(DatasetColumn col: columns) {
			Element columnElement = doc.createElement("column");
			columnElement.setAttribute("name", col.getName());
			columnsElement.appendChild(columnElement);
		}
		datasetElement.appendChild(columnsElement);
		
		Element rowsElement = doc.createElement("rows");
		for(DatasetRow row: rows) {
			Element rowElement = doc.createElement("row");
			
			for(DatasetColumn col: row.getColumns()) {
				// Ignore elements without a value.
				String val = row.get(col);
				if(val == null || val.equals(""))
					continue;
				
				Element itemElement = doc.createElement("key");
				itemElement.setAttribute("name", col.getName());
				itemElement.setTextContent(val);
				rowElement.appendChild(itemElement);
			}
			
			rowsElement.appendChild(rowElement);
		}
		datasetElement.appendChild(rowsElement);
		
		return datasetElement;
	}
	
	/*
	public static Dataset serializeFromNode(Project p, Node node) throws SAXException {
		
		System.err.println(" - starting serialization of dataset from " + node + ", memory usage: " + Runtime.getRuntime().freeMemory());
		
		NamedNodeMap attr = node.getAttributes();
		String name = attr.getNamedItem("name").getNodeValue();
		SimplifiedDate date = new SimplifiedDate(node);
		
		Dataset dataset = new Dataset(name, date);
		
		NodeList children = node.getChildNodes();
		for(int x = 0; x < children.getLength(); x++) {
			Node child = children.item(x);
			
			if(child.getNodeType() != Node.ELEMENT_NODE) continue;
			
			if(child.getNodeName().equalsIgnoreCase("columns")) {
				dataset.columns.clear();
				
				NodeList columns = child.getChildNodes();
				for(int y = 0; y < columns.getLength(); y++) {
					Node column = columns.item(y);
					
					if(column.getNodeType() != Node.ELEMENT_NODE) continue;
					
					String colName = column.getAttributes().getNamedItem("name").getNodeValue();
					dataset.columns.add(DatasetColumn.of(colName));
				}
				
				continue;
			}
			else if(child.getNodeName().equalsIgnoreCase("rows")) {
				dataset.rows.clear();
				
				NodeList rows = child.getChildNodes();
				for(int y = 0; y < rows.getLength(); y++) {
					Node rowElement = rows.item(y);
					
					if(rowElement.getNodeType() != Node.ELEMENT_NODE) continue;
					
					if(!rowElement.getNodeName().equalsIgnoreCase("row"))
						throw new SAXException("Unexpected element in 'rows': " + rowElement);
					
					DatasetRow row = new DatasetRow();
					
					NodeList items = rowElement.getChildNodes();
					for(int z = 0; z < items.getLength(); z++) {
						Node item = items.item(z);
						
						if(item.getNodeType() != Node.ELEMENT_NODE) continue;
						
						if(!item.getNodeName().equalsIgnoreCase("key"))
							throw new SAXException("Unexpected element in 'row': " + item);
						
						String key = item.getAttributes().getNamedItem("name").getNodeValue();
						String value = item.getTextContent();

						row.put(key, value);
					}
					
					dataset.rows.add(row);
				}
			} else
				throw new SAXException("Unexpected node in 'dataset': " + child);
		}
		
		System.err.println(" - dataset loaded: " + dataset + ", memory usage: " + Runtime.getRuntime().freeMemory());
		
		return dataset;
	}*/
	
	public void displayInTableView(TableView tv) {
		// Setup table.
		tv.setEditable(false);
		//controller.setTableColumnResizeProperty(TableView.CONSTRAINED_RESIZE_POLICY);
		ObservableList<TableColumn> cols = tv.getColumns();
		cols.clear();
		// We need to precalculate.
		ObservableList<DatasetRow> rows = this.rowsProperty();
		// Set up columns.
		TableColumn<DatasetRow, String> colRowName = new TableColumn("Name");
		colRowName.setCellValueFactory((TableColumn.CellDataFeatures<DatasetRow, String> features) -> {
			DatasetRow row = features.getValue();
			Set<Name> names = getNamesInRow(row);
			if (names.isEmpty()) {
				return new ReadOnlyStringWrapper("(None)");
			} else {
				return new ReadOnlyStringWrapper(names.stream().map(name -> name.getFullName()).collect(Collectors.joining("; ")));
			}
		});
		colRowName.setPrefWidth(100.0);
		cols.add(colRowName);
		// Create a column for every column here.
		this.getColumns().forEach((DatasetColumn col) -> {
			String colName = col.getName();
			TableColumn<DatasetRow, String> colColumn = new TableColumn(colName);
			colColumn.setCellValueFactory((TableColumn.CellDataFeatures<DatasetRow, String> features) -> {
				DatasetRow row = features.getValue();
				String val = row.get(colName);
				return new ReadOnlyStringWrapper(val == null ? "" : val);
			});
			colColumn.setPrefWidth(100.0);
			cols.add(colColumn);
		});
		// Set table items.
		tv.getItems().clear();
		tv.setItems(rows);
	}
}
