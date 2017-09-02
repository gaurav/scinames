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
package com.ggvaidya.scinames.ui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.NameClusterManager;
import com.ggvaidya.scinames.model.Project;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.stage.FileChooser;

/**
 * FXML Controller class
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetDiffController implements Initializable {
	private static final Logger LOGGER = Logger.getLogger(DatasetDiffController.class.getSimpleName());
	
	private static final DatasetColumn DATASET_COLUMN_ALL = DatasetColumn.fakeColumnFor("All fields");
	private static final DatasetColumn DATASET_COLUMN_NAME_ONLY = DatasetColumn.fakeColumnFor("Extracted scientific name");
	private static final DatasetColumn DATASET_COLUMN_BINOMIAL_NAME_CLUSTER = DatasetColumn.fakeColumnFor("Binomial name cluster for extracted scientific name");
	private static final DatasetColumn DATASET_COLUMN_NAME_SPECIFIC_EPITHET = DatasetColumn.fakeColumnFor("Subspecific epithet only");
	
	private ObservableList<DatasetColumn> byUniques = FXCollections.observableArrayList();
	
	private DatasetDiffView datasetDiffView;
	public void setDatasetDiffView(DatasetDiffView ddv) {
		datasetDiffView = ddv;
		
		// Set up the available datasets.
		Project project = ddv.getProjectView().getProject();
		
		dataset1ComboBox.setItems(project.getDatasets());
		dataset1ComboBox.getSelectionModel().selectedItemProperty().addListener(a -> regenerateByUniques());
		dataset1ComboBox.getSelectionModel().clearAndSelect(0);
		
		dataset2ComboBox.setItems(project.getDatasets());
		dataset1ComboBox.getSelectionModel().selectedItemProperty().addListener(a -> regenerateByUniques());
		dataset2ComboBox.getSelectionModel().clearAndSelect(0);
		
		regenerateByUniques();
		byUniqueComboBox.setItems(byUniques);
		byUniqueComboBox.getSelectionModel().clearAndSelect(0);
	}
	
	private void regenerateByUniques() {
		byUniques.clear();
		addUniqueMaps(byUniques);
		
		// Get columns from both datasets
		Set<DatasetColumn> cols = new HashSet<>(dataset1ComboBox.getSelectionModel().getSelectedItem().getColumns());
		cols.retainAll(dataset2ComboBox.getSelectionModel().getSelectedItem().getColumns());
		
		byUniques.addAll(cols.stream().sorted().collect(Collectors.toList()));
	}
	
	public void setFirst(Dataset ds) {
		dataset1ComboBox.getSelectionModel().select(ds);
	}

	public void setSecond(Dataset ds) {
		dataset2ComboBox.getSelectionModel().select(ds);
	}
	
	/**
	 * Initializes the controller class.
	 */
	@Override
	public void initialize(URL url, ResourceBundle rb) {
	}
	
	/* FXML objects */
	@FXML private ComboBox<Dataset> dataset1ComboBox;
	@FXML private ComboBox<Dataset> dataset2ComboBox;
	@FXML private ComboBox<DatasetColumn> byUniqueComboBox;
	
	@FXML private TextField datasetNameTextField;
	@FXML private TextField datasetDateTextField;
	@FXML private ComboBox<DatasetColumn> columnComboBox;
	@FXML private ComboBox<String> nameExtractorComboBox;
	@FXML private TableView comparisonTableView;
	@FXML private TextField statusTextField;
	
	/* Helper methods */
	
	private ObservableList<String> getComparisonStatRowHeaders() {
		ObservableList<String> rowHeadings = FXCollections.observableArrayList();
		
		rowHeadings.add("Number of rows");
		rowHeadings.add("Number of columns");
		rowHeadings.add("Number of names recognized");
		rowHeadings.add("Number of names in rows");
		
		return rowHeadings;
	}
	
	private Table<String, Dataset, String> getComparisonStats(Dataset... datasets) {
		Table<String, Dataset, String> precalc = HashBasedTable.create();
		
		// No datasets? Give up now.
		if(datasets.length == 0) return precalc;
		
		// For each row, we start with the actual stats for the first dataset and 
		// then provide diffs to subsequent datasets.
		Dataset ds1 = datasets[0];
		Project project = datasetDiffView.getProjectView().getProject();

		precalc.put("Number of rows", ds1, String.valueOf(ds1.getRowCount()));
		
		Set<Name> namesInRows1 = ds1.getNamesInAllRows();
		precalc.put("Number of names in rows", ds1, String.valueOf(namesInRows1.size()));
		
		Set<Name> recognizedNames1 = project.getRecognizedNames(ds1);
		precalc.put("Number of names recognized", ds1, String.valueOf(recognizedNames1.size()));
		
		Set<DatasetColumn> ds1_cols = new HashSet<>(ds1.getColumns());
		precalc.put("Number of columns", ds1, String.valueOf(ds1_cols.size()));
		
		// Now do comparison stats for each subsequent dataset.
		for(Dataset ds: datasets) {
			if(ds == ds1) continue;
			
			precalc.put("Number of rows", ds, 
				ds.getRowCount()
				+ ": " + (ds.getRowCount() - ds1.getRowCount())
				+ " (" + percentage(ds.getRowCount() - ds1.getRowCount(), ds1.getRowCount()) + ")"
			);
			
			Set<Name> recognizedNames = project.getRecognizedNames(ds);
			
			precalc.put("Number of names recognized", ds, 
				recognizedNames.size()
				+ ": " + (recognizedNames.size() - recognizedNames1.size())
				+ " (" + compareSets(recognizedNames1, recognizedNames)
				+ ", " + percentage(recognizedNames.size() - recognizedNames1.size(), recognizedNames1.size()) + ")"
			);
			
			Set<Name> namesInRows = ds.getNamesInAllRows();
			precalc.put("Number of names in rows", ds, 
				namesInRows.size()
				+ ": " + (namesInRows.size() - namesInRows1.size())
				+ " (" + compareSets(namesInRows1, namesInRows)
				+ ", " + percentage(namesInRows.size() - namesInRows1.size(), namesInRows1.size()) + ")"
			);
			
			Set<DatasetColumn> ds_cols = new HashSet<>(ds.getColumns());
			precalc.put("Number of columns", ds, 
				ds_cols.size()
				+ ": " + (ds_cols.size() - ds1_cols.size())
				+ " (" + compareSets(ds1.getColumns(), ds.getColumns())
				+ ", " + percentage(ds.getColumns().size() - ds1.getColumns().size(), ds1.getColumns().size()) + ")"
			);
		}
		
		return precalc;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String compareSets(Collection a, Collection b) {
		return compareSets(new HashSet(a), new HashSet(b)); 
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String compareSets(Set a, Set b) {
		Set added = new HashSet(b);
		added.removeAll(a);
		
		Set removed = new HashSet(a);
		removed.removeAll(b);
		
		return "+" + added.size() + ", -" + removed.size(); 
	}
	
	private String percentage(double numer, double denom) {
		return new BigDecimal(numer/denom*100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString() + "%";
	}
	
	/* FXML events */
	@FXML private void displayComparisonStats(ActionEvent e) {
		Dataset ds1 = dataset1ComboBox.getValue();
		Dataset ds2 = dataset2ComboBox.getValue();
		
		Table<String, Dataset, String> precalc = getComparisonStats(ds1, ds2);
		
		ObservableList<TableColumn> cols = comparisonTableView.getColumns();
		cols.clear();
		
		TableColumn<String, String> rowName = new TableColumn<>("");
		rowName.setCellValueFactory(cvf -> new ReadOnlyStringWrapper(cvf.getValue()));
		cols.add(rowName);
		
		for(Dataset ds: Arrays.asList(ds1, ds2)) {
			TableColumn<String, String> datasetCol = new TableColumn<>(ds.getName());
			datasetCol.setCellValueFactory(cvf -> new ReadOnlyStringWrapper(precalc.get(cvf.getValue(), ds)));
			cols.add(datasetCol);
		}
		
		// The "items" here are just the rows we've calculated.
		comparisonTableView.setItems(getComparisonStatRowHeaders());
	}
	
	private TableColumn<DatasetRow, String> createTableColumnForDatasetColumn(String colName, DatasetColumn column) {
		return createTableColumnForDatasetRow(colName, row -> row.get(column));
	}
	
	private TableColumn<DatasetRow, String> createTableColumnForDatasetRow(String colName, Function<DatasetRow, String> func) {
		TableColumn<DatasetRow, String> col = new TableColumn<>(colName);
		col.setCellValueFactory(cvf -> new ReadOnlyStringWrapper(func.apply(cvf.getValue())));
		return col;
	}
	
	private String truncateString(String str, int truncateTo) {
		if(str == null) return "(none)";
		if(str.length() < truncateTo) return str;
		return str.substring(0, truncateTo - 3) + "...";
	}
	
	private void displayRows(List<DatasetRow> rows) {
		ObservableList<TableColumn> cols = comparisonTableView.getColumns();
		cols.clear();
		
		List<DatasetColumn> datasetCols = rows.stream().flatMap(row -> row.getColumns().stream()).distinct().collect(Collectors.toList());
		for(DatasetColumn datasetCol: datasetCols) {
			cols.add(createTableColumnForDatasetColumn(datasetCol.getName(), datasetCol));
		}
		
		// Add the by-unique before the columns.
		Function<DatasetRow, String> uniqueMap = getByUniqueMap();
		cols.add(0, createTableColumnForDatasetRow("Unique", row -> truncateString(uniqueMap.apply(row), 30)));
		
		// Add the dataset after the columns.
		cols.add(createTableColumnForDatasetRow("Dataset", row -> row.getDataset().getCitation()));
		
		comparisonTableView.setItems(FXCollections.observableList(rows));
		statusTextField.setText("Displaying " + rows.size() + " rows across " + cols.size() + " columns");
	}
	
	private void addUniqueMaps(ObservableList<DatasetColumn> byUniques) {
		byUniques.add(DATASET_COLUMN_ALL);
		byUniques.add(DATASET_COLUMN_NAME_ONLY);
		byUniques.add(DATASET_COLUMN_BINOMIAL_NAME_CLUSTER);
		byUniques.add(DATASET_COLUMN_NAME_SPECIFIC_EPITHET);
	}
	
	private Function<DatasetRow, String> getByUniqueMap() {
		DatasetColumn colByEqual = byUniqueComboBox.getValue();
		if(colByEqual.equals(DATASET_COLUMN_ALL)) {
			return row -> row.toString();
		} else if(colByEqual.equals(DATASET_COLUMN_NAME_ONLY)) {
			// Note that this will combine rows that have identical names, which is not
			// what we want.
			return row -> row.getDataset().getNamesInRow(row).toString();
		} else if(colByEqual.equals(DATASET_COLUMN_BINOMIAL_NAME_CLUSTER)) {
			return row -> {
				Project project = datasetDiffView.getProjectView().getProject();
				NameClusterManager ncm = project.getNameClusterManager();
				
				List<Name> binomialNames = row.getDataset().getNamesInRow(row).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toList());
				List<NameCluster> nameClusters = ncm.getClusters(binomialNames);
				nameClusters.sort(null);
						
				return nameClusters.toString();
			};
		} else if(colByEqual.equals(DATASET_COLUMN_NAME_SPECIFIC_EPITHET)) {
			return row -> row.getDataset().getNamesInRow(row).stream().map(n -> n.getSpecificEpithet()).collect(Collectors.toSet()).toString();
		} else {
			return row -> row.get(colByEqual);
		}
	}
	
	@FXML private void displayIntersection(ActionEvent evt) {
		Dataset ds1 = dataset1ComboBox.getValue();
		Dataset ds2 = dataset2ComboBox.getValue();
		
		Map<String, DatasetRow> hashMap = new HashMap<>();
		Function<DatasetRow, String> byUnique = getByUniqueMap();
		
		// Step 1. Add and map all unique rows.
		List<DatasetRow> intersect_rows = new LinkedList();
		for(DatasetRow row: ds1.rowsProperty()) {
			hashMap.put(byUnique.apply(row), row);
		}
		
		for(DatasetRow row: ds2.rowsProperty())	{
			if(hashMap.containsKey(byUnique.apply(row))) {
				intersect_rows.add(row);
			}
		}
		
		displayRows(intersect_rows);
	}

	@FXML private void displayUnion(ActionEvent evt) {
		Dataset ds1 = dataset1ComboBox.getValue();
		Dataset ds2 = dataset2ComboBox.getValue();
		
		Map<String, DatasetRow> hashMap = new HashMap<>();
		Function<DatasetRow, String> byUnique = getByUniqueMap();
		
		// Step 1. Add and map all unique rows.
		List<DatasetRow> union_rows = new LinkedList(ds1.rowsProperty());
		for(DatasetRow row: union_rows) {
			hashMap.put(byUnique.apply(row), row);
		}
		
		for(DatasetRow row: ds2.rowsProperty())	{
			if(!hashMap.containsKey(byUnique.apply(row))) {
				union_rows.add(row);
			}
		}
		
		displayRows(union_rows);
	}	
	
	@FXML private void displayDifference(ActionEvent evt) {
		Dataset ds1 = dataset1ComboBox.getValue();
		Dataset ds2 = dataset2ComboBox.getValue();
		
		Map<String, DatasetRow> hashMap = new HashMap<>();
		Function<DatasetRow, String> byUnique = getByUniqueMap();
		
		// Find rows in ds1 but not in ds2
		List<DatasetRow> rows_in_ds1_not_ds2 = new LinkedList();
		for(DatasetRow row: ds2.rowsProperty()) {
			hashMap.put(byUnique.apply(row), row);
		}
		
		for(DatasetRow row: ds1.rowsProperty())	{
			if(!hashMap.containsKey(byUnique.apply(row))) {
				rows_in_ds1_not_ds2.add(row);
			}
		}
		
		// Find rows in ds2 but not in ds1
		hashMap = new HashMap<>();
		
		List<DatasetRow> rows_in_ds2_not_ds1 = new LinkedList();
		for(DatasetRow row: ds1.rowsProperty()) {
			hashMap.put(byUnique.apply(row), row);
		}
		
		for(DatasetRow row: ds2.rowsProperty())	{
			if(!hashMap.containsKey(byUnique.apply(row))) {
				rows_in_ds2_not_ds1.add(row);
			}
		}
		
		// Combine them!
		rows_in_ds1_not_ds2.addAll(rows_in_ds2_not_ds1);
		displayRows(rows_in_ds1_not_ds2);
		
		// Sort by the first column.
		TableColumn col = (TableColumn) comparisonTableView.getColumns().get(0);
		col.setSortType(SortType.ASCENDING);
	}
	
	@FXML private void displayDataset1Not2(ActionEvent evt) {
		Dataset ds1 = dataset1ComboBox.getValue();
		Dataset ds2 = dataset2ComboBox.getValue();
		
		Map<String, DatasetRow> hashMap = new HashMap<>();
		Function<DatasetRow, String> byUnique = getByUniqueMap();
		
		List<DatasetRow> rows_in_ds1_not_ds2 = new LinkedList();
		for(DatasetRow row: ds2.rowsProperty()) {
			hashMap.put(byUnique.apply(row), row);
		}
		
		for(DatasetRow row: ds1.rowsProperty())	{
			if(!hashMap.containsKey(byUnique.apply(row))) {
				rows_in_ds1_not_ds2.add(row);
			}
		}
		
		displayRows(rows_in_ds1_not_ds2);
	}

	@FXML private void displayDataset2Not1(ActionEvent evt) {
		Dataset ds1 = dataset1ComboBox.getValue();
		Dataset ds2 = dataset2ComboBox.getValue();
		
		Map<String, DatasetRow> hashMap = new HashMap<>();
		Function<DatasetRow, String> byUnique = getByUniqueMap();
		
		List<DatasetRow> rows_in_ds2_not_ds1 = new LinkedList();
		for(DatasetRow row: ds1.rowsProperty()) {
			hashMap.put(byUnique.apply(row), row);
		}
		
		for(DatasetRow row: ds2.rowsProperty())	{
			if(!hashMap.containsKey(byUnique.apply(row))) {
				rows_in_ds2_not_ds1.add(row);
			}
		}
		
		displayRows(rows_in_ds2_not_ds1);
	}
	
	/**
	 * Provide an export of the data in the TableView as a "table". In its
	 * simplest Java representation, that is a list of columns, with each
	 * column starting with a column header and then all the rest of the data.
	 * 
	 * Warning: this can be a long-running function!
	 * 
	 * @return A list of columns of data.
	 */
	public List<List<String>> getDataAsTable() {
		// What columns do we have?
		List<List<String>> result = new LinkedList<>();		
		List<TableColumn> columns = comparisonTableView.getColumns();
		
		columns.forEach(col -> {
			List<String> column = new LinkedList<>();
			
			// Add the header.
			column.add(col.getText());
			
			// Add the data.
			for(int x = 0; x < comparisonTableView.getItems().size(); x++) {
				ObservableValue cellObservableValue = col.getCellObservableValue(x);
				Object val = cellObservableValue.getValue();
				if(val == null)
					column.add("NA");
				else
					column.add(val.toString());
			}
			
			result.add(column);
		});
		
		return result;
	}
	
	private void fillCSVFormat(CSVFormat format, Appendable destination, List<List<String>> data) throws IOException {
		try (CSVPrinter printer = format.print(destination)) {
			List<List<String>> dataAsTable = data;
			if(dataAsTable.isEmpty())
				return;

			for(int x = 0; x < dataAsTable.get(0).size(); x++) {
				for(int y = 0; y < dataAsTable.size(); y++) {
					String value = dataAsTable.get(y).get(x);
					printer.print(value);
				}
				printer.println();
			}
		}
	}
	
	@FXML private void copyToClipboard(ActionEvent evt) {
		try {
			StringWriter writer = new StringWriter();
			List<List<String>> dataAsTable = getDataAsTable();
			
			fillCSVFormat(CSVFormat.TDF, writer, getDataAsTable());
			
			Clipboard clipboard = Clipboard.getSystemClipboard();
			HashMap<DataFormat, Object> content = new HashMap<>();
			content.put(DataFormat.PLAIN_TEXT, writer.getBuffer().toString());
			clipboard.setContent(content);
			
			Alert window = new Alert(Alert.AlertType.CONFIRMATION, (dataAsTable.get(0).size() - 1) + " rows written to clipboard.");
			window.showAndWait();
		} catch(IOException e) {
			Alert window = new Alert(Alert.AlertType.ERROR, "Could not save CSV to the clipboard: " + e);
			window.showAndWait();
		}
	}
	
	@FXML private void exportToCSV(ActionEvent evt) {
		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().setAll(
			new FileChooser.ExtensionFilter("CSV file", "*.csv"),
			new FileChooser.ExtensionFilter("Tab-delimited file", "*.txt")			
		);
		File file = chooser.showSaveDialog(datasetDiffView.getStage());
		if(file != null) {
			CSVFormat format = CSVFormat.RFC4180;
			
			String outputFormat = chooser.getSelectedExtensionFilter().getDescription();
			if(outputFormat.equalsIgnoreCase("Tab-delimited file"))
				format = CSVFormat.TDF;
			
			try {
				List<List<String>> dataAsTable = getDataAsTable();
				fillCSVFormat(format, new FileWriter(file), dataAsTable);
				
				Alert window = new Alert(Alert.AlertType.CONFIRMATION, "CSV file '" + file + "' saved with " + (dataAsTable.get(0).size() - 1) + " rows.");
				window.showAndWait();
				
			} catch(IOException e) {
				Alert window = new Alert(Alert.AlertType.ERROR, "Could not save CSV to '" + file + "': " + e);
				window.showAndWait();
			}
		}
	}
}
