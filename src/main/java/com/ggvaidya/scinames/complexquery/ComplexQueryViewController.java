/*
 *  ComplexQueryViewController
 *  Copyright (C) 2017 Gaurav Vaidya
 *
 *  This file is part of SciNames.
 *
 *  SciNames is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  SciNames is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with SciNames.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.ggvaidya.scinames.complexquery;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.ggvaidya.scinames.dataset.DatasetView;
import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.NameClusterManager;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.TaxonConcept;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import com.ggvaidya.scinames.ui.ProjectView;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;

/**
 * A ComplexQueryView is like a TabularDataView: it does a lot of the work,
 * so you don't have to. A complex query consists of a query in a text area, 
 * which is a serialization of the query. A button can be used to call up a
 * list of available commands.
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class ComplexQueryViewController implements Initializable {
	private static final Logger LOGGER = Logger.getLogger(ComplexQueryViewController.class.getSimpleName());
	
	public static ComplexQueryViewController createComplexQueryView(ProjectView pv) {
		Scene scene;
		FXMLLoader loader = new FXMLLoader(TabularDataViewController.class.getResource("/fxml/ComplexQueryView.fxml"));
		AnchorPane ap;
		try {
			ap = (AnchorPane) loader.load();
		} catch(IOException e) {
			throw new RuntimeException("Could not load internal file 'ComplexQueryView.fxml': " + e);
		}
		scene = new Scene(ap);
		ComplexQueryViewController controller = loader.getController();
		controller.scene = scene;
		controller.projectView = pv;
		return controller;
	}
	
	private ProjectView projectView;
	public ProjectView getProjectView() { return projectView; }
	
	private Scene scene;
	public Scene getScene() { return scene; }
		
	/**
	 * Initializes the controller class.
	 */
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		updatePrerecordedQueries();
		initDataTable();
	}
	
	private void initDataTable() {
		dataTableView.setOnMouseClicked(evt -> {
			if(evt.getButton().equals(MouseButton.PRIMARY) && evt.getClickCount() >= 2) {
				// Double-click on row!
				Object row = dataTableView.getSelectionModel().getSelectedItem();
				
				// What is this?
				if(Change.class.isAssignableFrom(row.getClass())) {
					// Be the change you want to see in the world.
					Change ch = (Change) row;
					
					DatasetView view = new DatasetView(projectView, ch);
					view.getStage().show();
				} else {
					LOGGER.warning("Could not find handler for double-clicking on " + row);
				}
			}
		});
	}
	
	private void updatePrerecordedQueries() {
		ObservableList<String> items = prerecordedQueries.getItems();
		items.clear();
		items.add("Choose one of the pre-recorded queries, or enter your own below:");
		prerecordedQueries.getSelectionModel().clearAndSelect(0);
	}
	
	private List<Function<ComplexQueryViewController, String>> listeners = new LinkedList<>();
	public List<Function<ComplexQueryViewController, String>> getListeners() { return listeners; }
	public void addListener(Function<ComplexQueryViewController, String> listener) { listeners.add(listener); }	
	
	@FXML
	private void executeQuery() {
		for(Function<ComplexQueryViewController, String> listener: listeners) {
			setQueryStatus(listener.apply(this));
		}
	}
	
	public String getQuery() { return queryTextArea.getText(); }
	
	public void setQueryStatus(String status) {
		if(status.equals("ok")) {
			queryStatusTextField.setBackground(new Background(new BackgroundFill(Paint.valueOf("green"), CornerRadii.EMPTY, Insets.EMPTY)));
			queryStatusTextField.setAlignment(Pos.CENTER);
			queryStatusTextField.setText("Query executed successfully");
		} else {
			queryStatusTextField.setBackground(new Background(new BackgroundFill(Paint.valueOf("red"), CornerRadii.EMPTY, Insets.EMPTY)));
			queryStatusTextField.setAlignment(Pos.CENTER);
			queryStatusTextField.setText(status);
		}
			
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
		List<TableColumn<NameCluster, ?>> columns = dataTableView.getColumns();
		
		for(TableColumn<NameCluster, ?> col: columns) {
			List<String> column = new LinkedList<>();
			
			// Add the header.
			column.add(col.getText());
			
			// Add the data.
			for(int x = 0; x < dataTableView.getItems().size(); x++) {
				ObservableValue cellObservableValue = col.getCellObservableValue(x);
				column.add(cellObservableValue.getValue().toString());
			}
			
			result.add(column);
		};
		
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
	
	/* Actions */
	@FXML
	private void copyToClipboard(ActionEvent evt) {
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
	
	@FXML
	private void exportToCSV(ActionEvent evt) {
		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().setAll(
			new FileChooser.ExtensionFilter("CSV file", "*.csv"),
			new FileChooser.ExtensionFilter("Tab-delimited file", "*.txt")			
		);
		File file = chooser.showSaveDialog(scene.getWindow());
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
	
	/* FXML objects */
	@FXML private ComboBox<String> prerecordedQueries;
	@FXML private TableView<String> propertiesTableView;
	@FXML private TextArea queryTextArea;
	@FXML private TextField queryStatusTextField;
	@FXML private TableView dataTableView;
	@FXML private TextField statusTextField;
	
	/* Return FXML objects */
	public TableView<String> getPropertiesTableView() { return propertiesTableView; }

	/* 
	 * Complicated fun things that make ComplexQueryViewControllers easy to use. 
	 */

	/* Part 1: Help */
	private String queryHelp;
	public String getQueryHelp() { return queryHelp; }
	public void setQueryHelp(String help) { queryHelp = help; }
	@FXML
	private void displayQueryHelp() {
		new Alert(Alert.AlertType.INFORMATION, queryHelp).showAndWait();
	}
	
	/* Part 2: Displaying data into the table */
	public static final Dataset ALL = new Dataset();
	
	private TableColumn<Change, String> createTableColumnFromChange(String colName, Function<Change, String> func) {
		TableColumn<Change, String> column = new TableColumn<>(colName);
		column.cellValueFactoryProperty().set(cvf -> new ReadOnlyStringWrapper(func.apply(cvf.getValue())));
		column.setPrefWidth(100.0);
		column.setEditable(false);
		return column;
	}
	
	public void updateTableWithChanges(Project project, Set<Change> changesToDisplay, List<Dataset> datasets) {
		List<Change> changes = changesToDisplay.stream().sorted((a, b) -> a.getDataset().compareTo(b.getDataset())).collect(Collectors.toList());
		
		NameClusterManager ncm = project.getNameClusterManager();
		
		// And add tablecolumns for the rest.
		dataTableView.getColumns().clear();
		dataTableView.getColumns().addAll(
			createTableColumnFromChange("id", ch -> ch.getId().toString()),
			createTableColumnFromChange("dataset", ch -> ch.getDataset().getName()),			
			createTableColumnFromChange("type", ch -> ch.getType().getType()),
			createTableColumnFromChange("from", ch -> ch.getFromString()),
			createTableColumnFromChange("from_name_cluster_ids", ch ->  ncm.getClusters(ch.getFrom()).stream()
					.map(cl -> cl.getId().toString())
					.collect(Collectors.joining(" and "))),
			createTableColumnFromChange("from_name_clusters", 
				ch -> ncm.getClusters(ch.getFrom()).stream()
					.map(cl -> cl.getNames().stream().map(n -> n.getFullName()).collect(Collectors.joining("; ")))
					.collect(Collectors.joining(" and "))),
			createTableColumnFromChange("to", ch -> ch.getToString()),
			createTableColumnFromChange("to_name_cluster_ids", ch ->  ncm.getClusters(ch.getTo()).stream()
					.map(cl -> cl.getId().toString())
					.collect(Collectors.joining(" and "))),			
			createTableColumnFromChange("to_name_clusters", 
				ch -> ncm.getClusters(ch.getTo()).stream()
					.map(cl -> cl.getNames().stream().map(n -> n.getFullName()).collect(Collectors.joining("; ")))
					.collect(Collectors.joining(" and "))),
			createTableColumnFromChange("filter_status", ch -> project.getChangeFilter().test(ch) ? "retained" : "eliminated"),
			createTableColumnFromChange("properties", ch -> ch.getProperties().entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).sorted().collect(Collectors.joining("; "))),		
			createTableColumnFromChange("citations", ch -> ch.getCitationStream().map(cit -> cit.getCitation()).sorted().collect(Collectors.joining("; ")))
		);

		dataTableView.getItems().clear();
		dataTableView.getItems().addAll(changes);

		dataTableView.refresh();
		
		// Fill in status text field.
		statusTextField.setText(dataTableView.getItems().size() + " changes across " + 
			changes.stream().map(ch -> ch.getDataset()).distinct().count()
			+ " distinct datasets");
	}
	
	public void updateTableWithChangesUsingNameClusters(Project project, List<NameCluster> nameClusters, List<Dataset> datasets) {
		Set<Change> changesToDisplay = new HashSet<>();
		for(NameCluster cluster: nameClusters) {
			// Yes, we want to use getAllChanges() here, because we'd like to match eliminated changes too.
			changesToDisplay.addAll(datasets.stream().flatMap(ds -> ds.getAllChanges()).collect(Collectors.toSet()));
		}
		
		List<Change> changes = changesToDisplay.stream().sorted((a, b) -> a.getDataset().compareTo(b.getDataset())).collect(Collectors.toList());
		
		NameClusterManager ncm = project.getNameClusterManager();
		
		// And add tablecolumns for the rest.
		dataTableView.getColumns().clear();
		dataTableView.getColumns().addAll(
			createTableColumnFromChange("id", ch -> ch.getId().toString()),
			createTableColumnFromChange("dataset", ch -> ch.getDataset().getName()),			
			createTableColumnFromChange("type", ch -> ch.getType().getType()),
			createTableColumnFromChange("from", ch -> ch.getFromString()),
			createTableColumnFromChange("from_name_cluster_ids", ch ->  ncm.getClusters(ch.getFrom()).stream()
					.map(cl -> cl.getId().toString())
					.collect(Collectors.joining(" and "))),
			createTableColumnFromChange("from_name_clusters", 
				ch -> ncm.getClusters(ch.getFrom()).stream()
					.map(cl -> cl.getNames().stream().map(n -> n.getFullName()).collect(Collectors.joining("; ")))
					.collect(Collectors.joining(" and "))),
			createTableColumnFromChange("to", ch -> ch.getToString()),
			createTableColumnFromChange("to_name_cluster_ids", ch ->  ncm.getClusters(ch.getTo()).stream()
					.map(cl -> cl.getId().toString())
					.collect(Collectors.joining(" and "))),			
			createTableColumnFromChange("to_name_clusters", 
				ch -> ncm.getClusters(ch.getTo()).stream()
					.map(cl -> cl.getNames().stream().map(n -> n.getFullName()).collect(Collectors.joining("; ")))
					.collect(Collectors.joining(" and "))),
			createTableColumnFromChange("filter_status", ch -> project.getChangeFilter().test(ch) ? "retained" : "eliminated"),
			createTableColumnFromChange("citations", ch -> ch.getCitationStream().map(cit -> cit.getCitation()).collect(Collectors.joining("; ")))
		);

		dataTableView.getItems().clear();
		dataTableView.getItems().addAll(changes);

		dataTableView.refresh();
		
		// Fill in status text field.
		statusTextField.setText(dataTableView.getItems().size() + " changes across " + 
			changes.stream().map(ch -> ch.getDataset()).distinct().count()
			+ " distinct datasets");
	}
	
	private TableColumn<NameCluster, String> createColumnFromPrecalc(String colName, Table<NameCluster, String, Set<String>> precalc) {
		TableColumn<NameCluster, String> column = new TableColumn<>(colName);
		column.cellValueFactoryProperty().set(
			(TableColumn.CellDataFeatures<NameCluster, String> cdf) -> {
				NameCluster nc = cdf.getValue();
				
				// There might be columns found in some dataset but not in others
				// so we detect those cases here and put in "NA"s instead.
				String output = "NA";
				if(precalc.contains(nc, colName))
					output = precalc.get(nc, colName).stream().collect(Collectors.joining("; "));
				
				return new ReadOnlyStringWrapper(output);
			}
		);
		column.setPrefWidth(100.0);
		column.setEditable(false);
		return column;
	}	
	
	public void updateTableWithNameClusters(Project project, List<NameCluster> nameClusters, List<Dataset> datasets) {
		Table<NameCluster, String, Set<String>> precalc = HashBasedTable.create();
		
		if(nameClusters == null) {
			dataTableView.setItems(FXCollections.emptyObservableList());	
			return;
		}
		boolean flag_nameClustersAreTaxonConcepts = false;
		
		if(nameClusters.size() > 0 && TaxonConcept.class.isAssignableFrom(nameClusters.get(0).getClass()))
			flag_nameClustersAreTaxonConcepts = true;
		dataTableView.setItems(FXCollections.observableList(nameClusters));
		
		// Precalculate.
		List<String> existingColNames = new ArrayList<>();
		existingColNames.add("id");
		existingColNames.add("name");
		existingColNames.add("names_in_dataset");		
		existingColNames.add("all_names_in_cluster");
		
		// If these are taxon concepts, there's three other columns we want
		// to emit.
		if(flag_nameClustersAreTaxonConcepts) {
			existingColNames.add("name_cluster_id");
			existingColNames.add("starts_with");
			existingColNames.add("ends_with");
			existingColNames.add("is_ongoing");
		} else {
			existingColNames.add("taxon_concept_count");
			existingColNames.add("taxon_concepts");
		}
		
		// Set<Name> recognizedNamesInDataset = namesDataset.getRecognizedNames(project).collect(Collectors.toSet());
		
		for(NameCluster cluster: nameClusters) {
			precalc.put(cluster, "id", getOneElementSet(cluster.getId().toString()));
			
			// Okay, here's what we need to do:
			//	- If names is ALL, then we can't do better than cluster.getName().
			// if(namesDataset == ALL) {
				precalc.put(cluster, "names_in_dataset",  cluster.getNames().stream().map(n -> n.getFullName()).collect(Collectors.toSet()));
				precalc.put(cluster, "name", getOneElementSet(cluster.getName().getFullName()));	
			//} else {
			/*
				// hey, here's something cool we can do: figure out which name(s)
				// this dataset uses from this cluster!
				List<String> namesInDataset = cluster.getNames().stream()
					.filter(n -> recognizedNamesInDataset.contains(n))
					.map(n -> n.getFullName())
					.collect(Collectors.toList());
				String firstName = "";
				if(namesInDataset.size() > 0)
					firstName = namesInDataset.get(0);
				
				precalc.put(cluster, "names_in_dataset", new HashSet<>(namesInDataset));
				precalc.put(cluster, "name", getOneElementSet(firstName));				
			}*/
			
			precalc.put(cluster, "all_names_in_cluster", cluster.getNames().stream().map(n -> n.getFullName()).collect(Collectors.toSet()));			
			
			// If it's a taxon concept, precalculate a few more columns.
			if(flag_nameClustersAreTaxonConcepts) {
				TaxonConcept tc = (TaxonConcept) cluster;
				
				precalc.put(cluster, "name_cluster_id", getOneElementSet(tc.getNameCluster().getId().toString()));
				precalc.put(cluster, "starts_with", tc.getStartsWith().stream().map(ch -> ch.toString()).collect(Collectors.toSet()));
				precalc.put(cluster, "ends_with", tc.getEndsWith().stream().map(ch -> ch.toString()).collect(Collectors.toSet()));
				precalc.put(cluster, "is_ongoing", getOneElementSet(tc.isOngoing(project) ? "yes" : "no"));
			} else {
				// If it's a true name cluster, then perhaps people will want
				// to know what taxon concepts are in here? Maybe for some sort
				// of PhD?
				List<TaxonConcept> tcs = cluster.getTaxonConcepts(project);
				
				precalc.put(cluster, "taxon_concept_count", getOneElementSet(String.valueOf(tcs.size())));
				precalc.put(cluster, "taxon_concepts", tcs.stream().map(tc -> tc.toString()).collect(Collectors.toSet()));
			}
			
			// Okay, here's where we reconcile!
			for(Name n: cluster.getNames()) {
				// TODO: there's probably an optimization here, in which we should
				// loop on the smaller set (either loop on 'datasets' and compare
				// to cluster, or loop on cluster.foundIn and compare to 'datasets').
				for(Dataset ds: datasets) {
					Map<Name, Set<DatasetRow>> rowsByName = ds.getRowsByName();
					
					// Are we included in this name cluster? If not, skip!
					if(!cluster.getFoundIn().contains(ds)) continue;
				
					// Check to see if we have any rows for this name; if not, skip.
					if(!rowsByName.containsKey(n)) continue;

					Set<DatasetRow> matched = rowsByName.get(n);
					LOGGER.log(Level.FINER, "Adding {0} rows under name ''{1}''", new Object[]{matched.size(), n.getFullName()});
				
					Map<Set<DatasetColumn>, List<DatasetRow>> rowsByCols = matched.stream().collect(
						Collectors.groupingBy((DatasetRow row) -> row.getColumns())
					);
				
					for(Set<DatasetColumn> cols: rowsByCols.keySet()) {
						for(DatasetColumn col: cols) {
							String colName = col.getName();

							if(existingColNames.contains(colName))
								colName = "datasets." + colName;

							if(!precalc.contains(cluster, colName))
								precalc.put(cluster, colName, new HashSet());

							for(DatasetRow row: rowsByCols.get(cols)) {
								if(!row.hasColumn(col)) continue;

								precalc.get(cluster, colName).add(row.get(col));											
							}

							LOGGER.log(Level.FINER, "Added {0} rows under name ''{1}''", new Object[]{rowsByCols.get(cols).size(), n.getFullName()});	
						}
					}
				}
			}
		}
		
		dataTableView.getColumns().clear();
		for(String colName: existingColNames) {
			dataTableView.getColumns().add(createColumnFromPrecalc(colName, precalc));
		}
		
		// Get distinct column names.
		Stream<String> colNames = precalc.cellSet().stream().map(set -> set.getColumnKey());
		
		// Eliminate columns that are in the existingColNames.
		colNames = colNames.filter(colName -> !existingColNames.contains(colName));
		
		// And add tablecolumns for the rest.
		List<TableColumn<NameCluster, String>> cols = colNames.distinct().sorted().map(colName -> createColumnFromPrecalc(colName, precalc)).collect(Collectors.toList());
		dataTableView.getColumns().addAll(cols);
		dataTableView.refresh();
		
		// Fill in status text field.
		statusTextField.setText(dataTableView.getItems().size() + " rows across " + cols.size() + " reconciled columns");
	}
		
	private Set<String> getOneElementSet(String str) {
		HashSet hs = new HashSet<>();
		hs.add(str);
		return hs;
	}
		
}
