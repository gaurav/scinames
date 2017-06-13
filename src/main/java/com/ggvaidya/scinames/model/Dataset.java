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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.io.input.BOMInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ggvaidya.scinames.model.rowextractors.NameExtractor;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorFactory;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorParseException;
import com.ggvaidya.scinames.util.ModificationTimeProperty;
import com.ggvaidya.scinames.util.SimplifiedDate;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

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
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class Dataset implements Citable, Comparable<Dataset> {
	private static final Logger LOGGER = Logger.getLogger(Dataset.class.getSimpleName());
	
	/* Constants */
	public static final String TYPE_DATASET = "Dataset";
	public static final String TYPE_CHECKLIST = "Checklist";	
	
	/* Private variables */
	private Project project;
	private StringProperty nameProperty = new SimpleStringProperty();
	private ObjectProperty<SimplifiedDate> dateProperty = new SimpleObjectProperty<>(SimplifiedDate.MIN);
	private Dataset prevDataset;
	private StringProperty typeProperty = new SimpleStringProperty(TYPE_DATASET);
	private ModificationTimeProperty lastModified = new ModificationTimeProperty();
	
	// Data in this dataset.
	private ObservableList<DatasetColumn> columns = FXCollections.observableArrayList();
	private ObservableList<DatasetRow> rows = FXCollections.observableList(new LinkedList<>());
	private ObservableList<Change> explicitChanges = FXCollections.observableList(new LinkedList<>());
	private ObservableList<Change> implicitChanges = FXCollections.observableList(new LinkedList<>());	
	
	{
		/* Make sure that certain changes trigger modifications. */
		nameProperty.addListener(c -> lastModified.modified());
		dateProperty.addListener(c -> lastModified.modified());
		typeProperty.addListener(c -> lastModified.modified());
		columns.addListener((Observable c) -> lastModified.modified());
		rows.addListener((Observable c) -> lastModified.modified());
		explicitChanges.addListener((Observable o) -> lastModified.modified());
	}
	
	/* Accessors */
	public Optional<Project> getProject() { return Optional.ofNullable(project); }
	public void setProject(Project p) { project = p; lastModified.modified(); }
	public StringProperty nameProperty() { return nameProperty; }
	public String getName() { return nameProperty.get(); }
	public void setName(String n) { nameProperty.set(n); }
	public ObjectProperty<SimplifiedDate> dateProperty() { return dateProperty; }
	public SimplifiedDate getDate() { return dateProperty.getValue(); }
	public ModificationTimeProperty lastModifiedProperty() { return lastModified; }
	public ObservableList<DatasetColumn> getColumns() { return columns; }
	public ObservableList<DatasetRow> rowsProperty() { return rows; }
	public ObservableList<Change> explicitChangesProperty() { return explicitChanges; }
	public int getRowCount() { return rows.size(); }
	public Stream<DatasetRow> getRowsAsStream() { return rows.stream(); }
	public StringProperty typeProperty() { return typeProperty; }
	public String getType() { return typeProperty.getValue(); }
	public boolean isChecklist() { return getType().equals(TYPE_CHECKLIST); }
	
	/* Higher order accessors */
	public boolean isChangeImplicit(Change ch) {
		return implicitChanges.contains(ch);
	}
	
	@Override
	public int compareTo(Dataset tp) {
		int compare = getDate().compareTo(tp.getDate());
		if(compare != 0) return compare;
		
		// Identical dates! Let's try names.
		compare = getName().compareTo(tp.getName());
		if(compare != 0) return compare;
		
		// Identical date, identical name! Go with the smaller hashcode.
		compare = hashCode() - tp.hashCode();
		if(compare != 0) return compare;
		
		// Crap, identical hashcodes. Are we identical?
		if(this == tp) return 0;
		
		// No? Always pick us, I guess.
		return -1;
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
		if(!namesInAllRows.contains(name)) return new HashSet<>();
		
		return getNamesByRow().entrySet().stream()
			.filter(entry -> entry.getValue().contains(name))
			.map(entry -> entry.getKey())
			.collect(Collectors.toSet());
	}
	
	/* Managing previous timepoint */
	public Dataset getPreviousDataset() { return prevDataset; }
	
	/**
	 * Set the previous dataset. This is where we calculate implicit changes from the previous
	 * dataset that have not been explained in our explicit changes.
	 * 
	 * @param proj Optionally, the project this dataset is a part of. Used for change filtering.
	 * @param tp Optionally, the previous dataset. If null, means there isn't one: we should be considered
	 * 		the first checklist.
	 */
	public void setPreviousDataset(Optional<Project> proj, Optional<Dataset> tp) {
		prevDataset = tp.orElse(null);
		
		implicitChanges.clear();	
		if(isChecklist()) {
			// Implicit changes don't exist for non-checklists. If we're a checklist, figure out what
			// names are new or have been removed in this checklist.
			
			Set<Name> names = getNamesInAllRows();
			Set<Name> prevNames;

			if(proj.isPresent() && tp.isPresent()) {
				prevNames = prevDataset.getRecognizedNames(proj.get()).collect(Collectors.toSet());
			} else {
				prevNames = new HashSet<>();
			}
			
			/*
			 * Logically, at this point, we need to apply the change filter so that changes that
			 * should be filtered out, are filtered out. However, we haven't calculated name clusters
			 * at this point, so the filtering wouldn't be correct anyway.
			 * 
			 * So, instead, we accept all explicit changes and calculate implicit changes on that
			 * basis. We then filter changes out of the explicit and implicit changes as needed.
			 * 
			 */

			// What names do explicit changes add or remove?
			Set<Name> addedByExplicitChanges = explicitChanges.stream().flatMap(ch -> ch.getToStream()).collect(Collectors.toSet());
			Set<Name> deletedByExplicitChanges = explicitChanges.stream().flatMap(ch -> ch.getFromStream()).collect(Collectors.toSet());		
						
			// Calculate implicit changes that can't be explained by an explicit change.
			Stream<Change> additions = names.stream()
				.filter(n -> !prevNames.contains(n) && !addedByExplicitChanges.contains(n))
				.map(n -> new Change(this, ChangeType.ADDITION, Stream.empty(), Stream.of(n)));
			Stream<Change> deletions = prevNames.stream()
				.filter(n -> !names.contains(n) && !deletedByExplicitChanges.contains(n))
				.map(n -> new Change(this, ChangeType.DELETION, Stream.of(n), Stream.empty()));
			
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
	
	private Map<DatasetRow, Set<Name>> namesByRow = null;
	private Map<Name, Set<DatasetRow>> rowsByName = null;
	private Set<Name> namesInRows = null;
	
	/*
	 * Tracks when the namesByRow was last modified. For some reason, we have two systems to
	 * track this: you can look at namesByRow or namesByRowLastModified. getNamesByRow() looks
	 * at both.
	 */
	private ModificationTimeProperty namesByRowLastModified = new ModificationTimeProperty();
	
	{
		// If the columns or rows change, we need to reparse ALL names.
		columns.addListener((ListChangeListener.Change<? extends DatasetColumn> c) -> resetNamesCaches());
		rows.addListener((ListChangeListener.Change<? extends DatasetRow> c) -> resetNamesCaches());
		
		lastModified.addListener((a, b, c) -> namesByRowLastModified.modified());
	}
	
	private void resetNamesCaches() {
		LOGGER.entering(Dataset.class.getSimpleName(), "resetNamesCaches");
		namesByRow = null;
		namesInRows = null;
		rowsByName = null;
	}
	
	/**
	 * The workhorse method for name parsing.
	 * 
	 * @return Map of rows in this dataset against all the names in each row.
	 */
	public Map<DatasetRow, Set<Name>> getNamesByRow() {
		LOGGER.entering(Dataset.class.getSimpleName(), "getNamesByRow");
		
		if(namesByRow == null || namesByRowLastModified.isModified()) {
			LOGGER.log(Level.FINE, "Recalculating names using extractors: {0}", 
				NameExtractorFactory.serializeExtractorsToString(getNameExtractors()));
			
			long startTime = System.nanoTime();
			
			// Recalculate all.
			resetNamesCaches();
			
			namesByRow = new HashMap<>();
			namesInRows = new HashSet<>();
			rowsByName = new HashMap<>();
			
			for(DatasetRow row: rows) {
				Set<Name> names = new HashSet<>();
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
	
	public Set<Name> getNamesInRow(DatasetRow row) {
		// trigger parse if necessary
		Map<DatasetRow, Set<Name>> namesByRow = getNamesByRow();
		
		LOGGER.entering(Dataset.class.getSimpleName(), "getNamesInRow", row);
		
		if(namesByRow.containsKey(row))
			return namesByRow.get(row);
		else
			return new HashSet<>();
	}

	/**
	 * Returns the set of all names recorded in the rows of this dataset. Note that this
	 * does NOT include names referenced from the explicit changes -- please see getReferencedNames()
	 * for that!
	 * 
	 * @return The set of all names recorded in the rows of this dataset.
	 */
	public Set<Name> getNamesInAllRows() {
		// Make sure our caches are up to date.
		getNamesByRow();
		
		// At this point, they should be!
		return namesInRows;
	}
	
	/**
	 * @return The inverse of getNamesByRow(): returns the set of rows associated with each name.
	 */
	public Map<Name, Set<DatasetRow>> getRowsByName() {
		// Make sure our caches are up to date.
		getNamesByRow();
		
		// At this point, they should be!
		return rowsByName;
	}
	
	/*
	 * Name extractors subsystem.
	 * 
	 * See model.rowextractors for more information.
	 */
	
	private List<NameExtractor> nameExtractors = NameExtractorFactory.getDefaultExtractors();
	public List<NameExtractor> getNameExtractors() { return nameExtractors; }
	
	/**
	 * @return The current name extractors as a string.
	 */
	public String getNameExtractorsAsString() {
		return NameExtractorFactory.serializeExtractorsToString(nameExtractors);
	}
	
	/**
	 * Set the current name extractors as a string.
	 * 
	 * @param str String represention of name extractors
	 * @throws NameExtractorParseException If the string representation could not be parsed.
	 */
	public void setNameExtractorsString(String str) throws NameExtractorParseException {
		nameExtractors = NameExtractorFactory.getExtractors(str);
		LOGGER.log(Level.FINE, 
			"setNameExtractorsString() called, extractors now set to {0}", 
			NameExtractorFactory.serializeExtractorsToString(nameExtractors));
		resetNamesCaches();
	}
	
	/**
	 * Returns a Stream of all distinct names referenced from this dataset. This includes 
	 * names found in dataset rows and names found in ALL explicit changes (not just 
	 * filtered ones!), and nothing else.
	 * 
	 * @return A Stream of all distinct names referenced from this dataset.
	 */
	public Stream<Name> getReferencedNames() {
		Stream<Name> namesFromData = getNamesInAllRows().stream();
		Stream<Name> namesFromChanges = explicitChanges.stream().flatMap(ch -> ch.getAllNames().stream());
		
		return Stream.concat(namesFromData, namesFromChanges).distinct();
	}

	/**
	 * Returns a Stream of all distinct names recognized at the end of this checklist.
	 * 
	 * For a checklist, this is every name in every row, plus names added by explicit
	 * changes (which overrule the dataset), minus names removed by explicit changes.
	 * 
	 * For a dataset, it's (prevDataset's recognized names) + 
	 * (names added by explicit and implicit changes) - (names removed by explicit
	 * and implicit changes).
	 * 
	 * @param proj Required for filtering changes
	 * @return A Stream of recognized names as at the end of this checklist.
	 */
	public Stream<Name> getRecognizedNames(Project proj) {
		// Start with names we explicitly add.
		Set<Name> addedNames = getChanges(proj)
			.flatMap(ch -> ch.getToStream())
			.collect(Collectors.toSet());
		Set<Name> initialNames = new HashSet<>(addedNames);
		
		// If this is not a checklist, then pass through previously recognized names.
		if(prevDataset != null)
			initialNames.addAll(proj.getRecognizedNames(prevDataset));
		
		// Delete names we explicitly delete.
		Set<Name> deletedNames = getChanges(proj)
			.flatMap(ch -> ch.getFromStream())
			.collect(Collectors.toSet());
		
		Set<Name> finalList = initialNames.stream().filter(n -> {
			// Filter out names that have been deleted, EXCEPT those that
			// have been explicitly added (such as in a lump or split).
			if(deletedNames.contains(n)) {
				if(addedNames.contains(n))
					return true; // don't filter
				else
					return false; // do filter
			} else 
				return true; // don't filter
		}).collect(Collectors.toSet());
		
		// This should be the same as the names in a checklist!
		// Double-check!
		if(isChecklist() && !finalList.equals(getNamesInAllRows())) {
			// TODO: OKAY, so this is caused by the following scenario:
			//	- We explicitly rename "Osteocephalus vilmae" to "Hylomantis buckleyi" within a dataset
			// 	- We do that because AmphibiaWeb *says* they are duplicates.
			//	- However, this dataset has rows for *both* vilmae and buckleyi.
			//	- So how?
			
			Set<Name> finalListButNotInRows = new HashSet<>(finalList);
			finalListButNotInRows.removeAll(getNamesInAllRows());
			
			Set<Name> rowNamesButNotFinalList = new HashSet<>(getNamesInAllRows());
			rowNamesButNotFinalList.removeAll(finalList);
			
			LOGGER.severe("Discrepency in calculating recognized names for " + this + ":\n"
				+ "\t - Final list but not in rows: " + finalListButNotInRows + "\n"
				+ "\t - Rows but not in final list: " + rowNamesButNotFinalList + "\n"
				+ "\t - Name count: " + initialNames.size() + " + " + addedNames.size() + " - " + deletedNames.size() + " = " + 
					(initialNames.size() + addedNames.size() - deletedNames.size())
				+ " (but should be " + finalList.size() + ")\n"
			);
		}
		
		return finalList.stream();
	}
	
	/* Display options: provides information on what happened in this dataset for UI purposes */

	public String getRowCountSummary() {
		Map<DatasetRow, Set<Name>> namesByRow = getNamesByRow();
		long rowsWithNames = namesByRow.values().stream().filter(names -> names.size() > 0).count();
		
		return getRowCount() + " rows (" + ((double)rowsWithNames/getRowCount() * 100) + "% named with " + getNamesInAllRows().size() + " distinct names)";
	}
	
	// Calculating this ourselves is too slow, so we hook into Project's cache.
	public String getNameCountSummary(Project project) {
		if(isChecklist())
			return project.getRecognizedNames(this).size() + " recognized (" + getReferencedNames().count() + " referenced in rows and changes)";
		else
			return getReferencedNames().count() + " referenced (" + project.getRecognizedNames(this).size() + " recognized)";
	}

	public String getBinomialCountSummary(Project project) {
		if(isChecklist())
			return project.getRecognizedNames(this).stream().flatMap(n -> n.asBinomial()).distinct().count() + " recognized (" + getReferencedNames().flatMap(n -> n.asBinomial()).distinct().count() + " referenced)";
		else
			return getReferencedNames().flatMap(n -> n.asBinomial()).distinct().count() + " referenced (" + project.getRecognizedNames(this).stream().flatMap(n -> n.asBinomial()).distinct().count() + " recognized)";
	}
	
	/** 
	 * Set up a TableView to contain the data contained in this dataset.
	 * 
	 * @param tv The TableView to populate.
	 */
	public void displayInTableView(TableView<DatasetRow> tv) {
		// Setup table.
		tv.setEditable(false);
		//controller.setTableColumnResizeProperty(TableView.CONSTRAINED_RESIZE_POLICY);
		ObservableList<TableColumn<DatasetRow, ?>> cols = tv.getColumns();
		cols.clear();
		// We need to precalculate.
		ObservableList<DatasetRow> rows = this.rowsProperty();
		// Set up columns.
		TableColumn<DatasetRow, String> colRowName = new TableColumn<>("Name");
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
			TableColumn<DatasetRow, String> colColumn = new TableColumn<>(colName);
			colColumn.setCellValueFactory((TableColumn.CellDataFeatures<DatasetRow, String> features) -> {
				DatasetRow row = features.getValue();
				String val = row.get(colName);
				return new ReadOnlyStringWrapper(val == null ? "" : val);
			});
			colColumn.setPrefWidth(100.0);
			cols.add(colColumn);
		});
		
		// Set table items.
		// tv.getItems().clear();
		tv.setItems(rows);
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
		return implicitChanges.stream().filter(p.getChangeFilter());
	}
	
	public String getChangesCountSummary(Project p) {
		if(explicitChanges.size() == 0) {
			if(implicitChanges.size() == 0) {
				return "No changes";
			} else {
				// Implicit only
				return getImplicitChangesCountSummary(p);
			}
		} else {
			if(implicitChanges.size() == 0) {
				// Explicit only
				return getExplicitChangesCountSummary(p);
			} else {
				// Both explicit and implicit changes
				return getImplicitChangesCountSummary(p) + "; " + getExplicitChangesCountSummary(p);
			}
		}
	}
	
	public String getExplicitChangesCountSummary(Project p) {
		if(getExplicitChanges(p).count() == 0)
			return "None";
		
		Map<ChangeType,Long> changeCounts = getExplicitChanges(p)
			.collect(Collectors.groupingBy(Change::getType, Collectors.counting()));
		
		String changes_by_type = changeCounts.entrySet().stream()
				.sorted((a, b) -> b.getValue().compareTo(a.getValue()))
				.map(e -> e.getValue() + " " + e.getKey())
				.collect(Collectors.joining(", "));
		
		return getExplicitChanges(p).count() + " explicit changes (" + changes_by_type + ")";
	}
	
	public String getImplicitChangesCountSummary(Project p) {
		if(getImplicitChanges(p).count() == 0)
			return "None";
		
		Map<ChangeType,Long> implicitChangeCounts = getImplicitChanges(p)
			.collect(Collectors.groupingBy(Change::getType, Collectors.counting()));
		
		String implicit_changes_by_type = implicitChangeCounts.entrySet().stream()
			.sorted((a, b) -> b.getValue().compareTo(a.getValue()))
			.map(e -> e.getValue() + " " + e.getKey())
			.collect(Collectors.joining(", "));
		
		return getImplicitChanges(p).count() + " implicit changes (" + implicit_changes_by_type + ")";
	}

	/**
	 * Return all changes associated with this dataset, explicit or implicit. You almost certainly
	 * want to use getChanges(Project), which will give you all the changes after filtering those
	 * the project isn't interested in -- so please be careful about using this!
	 * 
	 * @return Stream of all changes associated with this dataset.
	 */
	public Stream<Change> getAllChanges() {
		return Stream.concat(explicitChanges.stream(), implicitChanges.stream());
	}
	
	public Stream<Change> getChanges(Project project) {
		return getAllChanges().filter(project.getChangeFilter());
	}

	@Override
	public String getCitation() {
		return getName() + " (" + getDate() + ")";
	}
	
	@Override
	public String toString() {
		return getType() + " " + getCitation();
	}
	
	public String asTitle() {
		return getType() + " " + getName() + " (" + getDate()  + ": " + rows.size() + " rows, " + getReferencedNames().count() + " referenced names)";
	}
	
	/* Data load */
	
	/**
	 * Attempt to load a dataset from a file. We use regular expressions to try to guess the file type,
	 * and then delegate the job out. Rather cleverly, we try extracting the names using every extractor
	 * this project knows about, and then pick the one that gives us the most number of names.
	 * 
	 * @param proj The project doing the loading, used to get the name extractors.
	 * @param f The file to open.
	 * @return The dataset loaded from that file.
	 * @throws IOException If there was an error loading the file.
	 */
	public static Dataset loadFromFile(Project proj, File f) throws IOException {
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
			Set<List<NameExtractor>> allAvailableNameExtractors = proj.getNameExtractors();
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
	public Dataset(String name, SimplifiedDate date, String checklistType) {
		nameProperty.setValue(name);
		dateProperty.setValue(date);
		typeProperty.setValue(checklistType);
	}
	
	// Blank constructor
	public Dataset() {
		nameProperty.setValue("(unnamed)");
	}

	/* Serialization */
	
	/**
	 * Load this dataset from a CSV file. We load the entire CSV file, except
	 * for blank cells.
	 * 
	 * @param project The project to which the resulting Dataset should belong
	 * @param csvFormat The CSV format of the input file.
	 * @param csvFile The input file to load.
	 * @param renamedColumns Rename these columns on the fly.
	 * @return
	 * @throws IOException 
	 */
	public static Dataset fromCSV(CSVFormat csvFormat, File csvFile) throws IOException {
		Dataset dataset = new Dataset(csvFile.getName(), new SimplifiedDate(), Dataset.TYPE_CHECKLIST);
		
		// Get ready to filter input files.
		InputStream ins = new FileInputStream(csvFile);
		
		// Look for BOMs and discard!
		ins = new BOMInputStream(ins, false);
		
		// Convert into a Reader.
		Reader reader = new BufferedReader(new InputStreamReader(ins));
		
		// Load CSV
		CSVParser parser = csvFormat.withHeader().parse(reader);
		Map<String, Integer> headerMap = parser.getHeaderMap();
		
		dataset.setColumns(headerMap.entrySet().stream().sorted((Object o1, Object o2) -> {
			Map.Entry<String, Integer> e1 = (Map.Entry) o1;
			Map.Entry<String, Integer> e2 = (Map.Entry) o2;
			
			return e1.getValue().compareTo(e2.getValue());
		}).map(e -> e.getKey())
		.map(colName -> DatasetColumn.of(colName))
		/*
		.map(col -> {
			// Rename any renamedColumns.
			if(renamedColumns.containsKey(col))
				return renamedColumns.get(col);
			else
				return col;
		})*/
		.collect(Collectors.toList()));
		
		dataset.rows.clear();
		dataset.rows.addAll(
			parser.getRecords().stream()
				.map(record -> {  DatasetRow row = new DatasetRow(dataset); row.putAll(record.toMap()); return row;})
				.collect(Collectors.toList())
		);
		
		return dataset;
	}
	
	public Element serializeToElement(Document doc) {
		Element datasetElement = doc.createElement("dataset");
		
		datasetElement.setAttribute("name", getName());
		datasetElement.setAttribute("type", getType());
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
}
