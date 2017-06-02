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
package com.ggvaidya.scinames.dataset;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.ChangeType;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.NameClusterManager;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.change.ChangeTypeStringConverter;
import com.ggvaidya.scinames.model.change.NameSetStringConverter;
import com.ggvaidya.scinames.model.filters.ChangeFilter;
import com.ggvaidya.scinames.project.ProjectView;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;

import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * FXML Controller class for a view of a Timepoint in a project.
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetSceneController {
	private static final Logger LOGGER = Logger.getLogger(DatasetSceneController.class.getSimpleName());
	
	private DatasetView datasetView;
	private Dataset dataset;
	
	public DatasetSceneController() {}

	public void setTimepointView(DatasetView tv) {
		datasetView = tv;
		dataset = tv.getDataset();
		
		// Reinitialize UI to the selected timepoint.
		updateMainTextArea();
		fillTableWithChanges(changesTableView, dataset);
		additionalDataTypeChanged();
	}
	
	/**
	 * Initializes the controller class.
	 */
	public void initialize() {
		updateMainTextArea();
		
		additionalDataCombobox.getItems().setAll(additionalDataTypeNames);
		additionalDataCombobox.getSelectionModel().selectedItemProperty().addListener((Observable o) -> additionalDataTypeChanged());
		// additionalDataCombobox.getSelectionModel().select("Data"); // Display the data first.
		additionalDataCombobox.getSelectionModel().select(0);
		
		changesTableView.getSelectionModel().getSelectedItems().addListener(
			(ListChangeListener<Change>) c -> additionalDataTypeChanged()
		);
	}
	
	/*
	 * User interface.
	 */
	@SuppressWarnings("rawtypes")
	@FXML private TableView additionalDataTableView;
	@SuppressWarnings("rawtypes")
	@FXML private ListView additionalListView;
	@FXML private ComboBox<String> additionalDataCombobox;
	@FXML private TableView<Change> changesTableView;
	
	private void updateMainTextArea() {
		// No timepoint, no content.
		if(dataset == null) {
			// mainTextArea.setText("No timepoint loaded.");
			return;
		}
		
		NameClusterManager nameClusterManager = datasetView.getProjectView().getProject().getNameClusterManager();
		
		// We have three sections: timepoint, changes and names.
		StringBuilder timepointInfo = new StringBuilder();
		StringBuilder changesInfo = new StringBuilder();
		StringBuilder namesInfo = new StringBuilder();
		Set<Name> allNames = new HashSet<>();
		
		// What do we know about this timepoint?
		timepointInfo
			.append("Timepoint: ").append(dataset.getCitation()).append(", published in ").append(dataset.getDate()).append("\n")
			.append("Names: ").append(dataset.getNameCountSummary(datasetView.getProjectView().getProject())).append("\n")
			.append("Binomial names: ").append(dataset.getBinomialCountSummary(datasetView.getProjectView().getProject())).append("\n")
			.append("Explicit changes: ").append(dataset.getExplicitChangesCountSummary(datasetView.getProjectView().getProject())).append("\n")
			.append("Implicit changes: ").append(dataset.getImplicitChangesCountSummary(datasetView.getProjectView().getProject())).append("\n")	
		;
		
		// What do we know about the selected changes?
		changesTableView.getSelectionModel().getSelectedItems().forEach((Object oChange) -> {
			Change change = (Change) oChange;
			changesInfo.append(" - ").append(change.toString()).append("\n");
			allNames.addAll(change.getAllNames());
		});
		
		// What do we know about the selected names?
		// allNames.addAll(prevTimepointTableView.getSelectionModel().getSelectedItems());
		// allNames.addAll(currTimepointTableView.getSelectionModel().getSelectedItems());		
			
		allNames.stream().sorted().forEach((Name name) -> {
			String clusterInfo = "(not found in clusters)";
			Optional<NameCluster> cluster = nameClusterManager.getCluster(name);
			if(cluster.isPresent())
				clusterInfo = cluster.get().toString();
				
			namesInfo.append(" - ").append(name.toString())
				.append(" in ")
				.append(clusterInfo)
				.append("\n");
			
			Map<DatasetColumn, Set<String>> dataByColumn = datasetView.getProjectView().getProject().getDataForName(name);
			for(DatasetColumn col: dataByColumn.keySet()) {
				List<String> vals = new LinkedList<>(dataByColumn.get(col));
				Collections.sort(vals);
				
				namesInfo.append("\t - ")
					.append(col.getName())
					.append(": ")
					.append(String.join(", ", vals))
					.append("\n");
			}
		});
		
		// Put it all together.
		/*
		mainTextArea.setText(
			timepointInfo
			.append("\n== Selected Changes ==\n").append(changesInfo)
			.append("\n== Selected Names ==\n").append(namesInfo)
			.toString()
		);*/
	}
	
	/*
	private void fillTableWithNamesFrom(TableView tv, Dataset tp) {
		tv.setEditable(false);
		
		NameClusterManager nameClusterManager = datasetView.getProjectView().getProject().getNameClusterManager();
		
		TableColumn<Name, String> colName = new TableColumn<>("Name");
		colName.setCellValueFactory(new PropertyValueFactory<>("FullName"));
		colName.setPrefWidth(250.0);
		
		TableColumn<Name, String> colCluster = new TableColumn<>("Cluster");
		colCluster.setCellValueFactory(
			(TableColumn.CellDataFeatures<Name, String> features) -> 
				new ReadOnlyStringWrapper(
					nameClusterManager.getClusters(Arrays.asList(features.getValue())).toString()
				)
		);
		tv.getColumns().setAll(colName, colCluster);
		
		// TODO: for now, this only uses recognized names, but eventually we
		// want all referenced names, and then to use some way of visually
		// distinguishing the two.
		if(tp != null) {
			tv.setItems(FXCollections.observableList(tp.getRecognizedNames(datasetView.getProjectView().getProject()).collect(Collectors.toList())));
		} else {
			tv.setItems(FXCollections.emptyObservableList());
		}
		tv.getSortOrder().add(colName);
	}*/
	
	private void fillTableWithChanges(TableView<Change> tv, Dataset tp) {
		tv.setEditable(true);
		tv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		tv.getColumns().clear();

		TableColumn<Change, ChangeType> colChangeType = new TableColumn<>("Type");
		colChangeType.setCellFactory(ComboBoxTableCell.forTableColumn(
			new ChangeTypeStringConverter(),
			ChangeType.ADDITION,
			ChangeType.DELETION,
			ChangeType.RENAME,			
			ChangeType.LUMP,
			ChangeType.SPLIT,
			ChangeType.ERROR
		));
		colChangeType.setCellValueFactory(new PropertyValueFactory<>("type"));
		colChangeType.setPrefWidth(100.0);
		colChangeType.setEditable(true);
		tv.getColumns().add(colChangeType);
		
		TableColumn<Change, ObservableSet<Name>> colChangeFrom = new TableColumn<>("From");
		colChangeFrom.setCellFactory(TextFieldTableCell.forTableColumn(new NameSetStringConverter()));
		colChangeFrom.setCellValueFactory(new PropertyValueFactory<>("from"));
		colChangeFrom.setPrefWidth(200.0);
		colChangeFrom.setEditable(true);
		tv.getColumns().add(colChangeFrom);
		
		TableColumn<Change, ObservableSet<Name>> colChangeTo = new TableColumn<>("To");
		colChangeTo.setCellFactory(TextFieldTableCell.forTableColumn(new NameSetStringConverter()));	
		colChangeTo.setCellValueFactory(new PropertyValueFactory<>("to"));
		colChangeTo.setPrefWidth(200.0);
		colChangeTo.setEditable(true);
		tv.getColumns().add(colChangeTo);
		
		TableColumn<Change, String> colExplicit = new TableColumn<>("Explicit or implicit?");
		colExplicit.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) -> 
				new ReadOnlyStringWrapper(
					features.getValue().getDataset().isChangeImplicit(features.getValue()) ? "Implicit" : "Explicit"
				)
		);
		tv.getColumns().add(colExplicit);
		
		ChangeFilter cf = datasetView.getProjectView().getProject().getChangeFilter();
		TableColumn<Change, String> colFiltered = new TableColumn<>("Eliminated by filter?");
		colFiltered.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) -> 
				new ReadOnlyStringWrapper(
					cf.test(features.getValue()) ? "Allowed" : "Eliminated"
				)
		);
		tv.getColumns().add(colFiltered);
		
		TableColumn<Change, String> colNote = new TableColumn<>("Note");
		colNote.setCellFactory(TextFieldTableCell.forTableColumn());
		colNote.setCellValueFactory(new PropertyValueFactory<>("note"));
		colNote.setPrefWidth(100.0);
		colNote.setEditable(true);
		tv.getColumns().add(colNote);
		
		TableColumn<Change, String> colProperties = new TableColumn<>("Properties");
		colProperties.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) -> 
				new ReadOnlyStringWrapper(
					features.getValue().getProperties().entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).sorted().collect(Collectors.joining("; "))
				)
		);
		tv.getColumns().add(colProperties);

		TableColumn<Change, String> colCitations = new TableColumn<>("Citations");
		colCitations.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) -> 
				new ReadOnlyStringWrapper(
					features.getValue().getCitationStream().map(citation -> citation.getCitation()).sorted().collect(Collectors.joining("; "))
				)
		);
		tv.getColumns().add(colCitations);
		
		TableColumn<Change, String> colGenera = new TableColumn<>("Genera");
		colGenera.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) ->
				new ReadOnlyStringWrapper(
					String.join(", ", features.getValue().getAllNames().stream().map(n -> n.getGenus()).distinct().sorted().collect(Collectors.toList()))
				)
		);
		tv.getColumns().add(colGenera);
		
		TableColumn<Change, String> colSpecificEpithet = new TableColumn<>("Specific epithet");
		colSpecificEpithet.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) ->
				new ReadOnlyStringWrapper(
					String.join(", ", features.getValue().getAllNames().stream().map(n -> n.getSpecificEpithet()).filter(s -> s != null).distinct().sorted().collect(Collectors.toList()))
				)
		);
		tv.getColumns().add(colSpecificEpithet);
		
		// TODO: if we can get an ObservableList over tp.getAllChanges(), then this table
		// will update dynamically as changes are made. Won't that be something.
		// Yes, we want to getAllChanges() so we can see which ones are filtered out.
		tv.setItems(FXCollections.observableList(tp.getAllChanges().collect(Collectors.toList())));
		tv.getSortOrder().add(colChangeType);
	}
	
	/*
	private void displayAssociatedData(ActionEvent evt) {
		DatasetTabularView datasetView = new DatasetTabularView(dataset);
		datasetView.getStage().show();
	}*/
	
	private final List<String> additionalDataTypeNames = Arrays.asList(
		"All recognized names",
		"Citations",
		"Data",
		"Names with changes",
		"Names with data"
	);
	
	private void additionalDataTypeChanged() {
		String selected = (String) additionalDataCombobox.getSelectionModel().getSelectedItem();
		
		@SuppressWarnings("rawtypes")
		ListView listView = additionalListView;
		@SuppressWarnings("rawtypes")
		TableView tableView = additionalDataTableView;
				
		switch(selected) {
			case "All recognized names":
				showAllRecognizedNames(listView, tableView);			
			
			case "Citations":
				showCitationsInTable(listView, tableView);
				break;
				
			case "Data":
				showDataInTable(listView, tableView);
				break;
				
			case "Names with changes":
				showNamesWithChangesInTable(listView, tableView);
				break;
				
			case "Names with data":
				showNamesWithDataInTable(listView, tableView);
				break;
		}
	}
	
	private void showCitationsInTable(ListView listView, TableView tableView) {
		tableView.getItems().clear();
		
		// TODO Add a viewer for citations
	}
	
	private void showNamesWithChangesInTable(ListView<Name> listView, TableView tableView) {
		// Clear table.
		tableView.editableProperty().set(false);
		tableView.getColumns().clear();
		
		// Choose all the names we're interested in showing off.
		ObservableList<Change> selectedChanges = changesTableView.getSelectionModel().getSelectedItems();
		Stream<Name> names = selectedChanges.stream().flatMap(ch -> ch.getAllNames().stream());
		
		// Put them into the treeView.
		listView.getItems().setAll(names.sorted().collect(Collectors.toSet()));
		listView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Name>) change -> {
			/*
			 * Displays all data associated with this Name.
			 */

			
			// Set up table view
			tableView.editableProperty().set(false);
			
			// Set up 
			
			// Set up columns.
			ObservableList<TableColumn> cols = tableView.getColumns();
			cols.clear();
			
			// Columns 
			
			// TODO provide this output
			
		});
	}
	
	private void showNamesWithDataInTable(ListView<Name> listView, TableView tableView) {
		// Clear table.
		tableView.editableProperty().set(false);
		tableView.getColumns().clear();
		
		// Choose all the names we're interested in showing off.
		ObservableList<Change> selectedChanges = changesTableView.getSelectionModel().getSelectedItems();
		Stream<Name> names = selectedChanges.stream().flatMap(ch -> ch.getAllNames().stream());
		
		// Put them into the treeView.
		listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		listView.getItems().setAll(names.sorted().collect(Collectors.toSet()));
		listView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Name>) change -> {
			/*
			 * Displays all data associated with this Name.
			 */
			
			Name name = (Name) listView.getSelectionModel().getSelectedItem();
			Stream<Dataset> timepoints = datasetView.getProjectView().getProject().getDatasets().stream();
			
			// Set up table view
			tableView.editableProperty().set(false);
			
			// Set up data.
			
			// Set up columns.
			ObservableList<TableColumn> cols = tableView.getColumns();
			cols.clear();
			
			// Columns 
			
			// TODO make this work at some point
			
		});
	}
	
	private void showAllRecognizedNames(ListView<Name> listView, TableView<Name> tableView) {
		// No project view? Don't do nothing.
		if(datasetView == null || datasetView.getProjectView() == null)
			return;
		
		// List view should be all recognized names.
		Project project = datasetView.getProjectView().getProject();
		
		listView.getItems().setAll(FXCollections.observableArrayList(dataset.getRecognizedNames(project).sorted().collect(Collectors.toList())));
		listView.getSelectionModel().clearAndSelect(0);
		
		// Setup table.
		tableView.editableProperty().set(false);
		
		ObservableList<TableColumn<Name, ?>> cols = tableView.getColumns();
		cols.clear();
		
		// Set up columns.
		TableColumn<Name, String> colFullName = new TableColumn<>("Name");
		colFullName.setCellValueFactory((TableColumn.CellDataFeatures<Name, String> features) -> {
			Name name = features.getValue();
					
			return new ReadOnlyStringWrapper(name.getFullName());
		});
		colFullName.setPrefWidth(100.0);
		cols.add(colFullName);
		
		NameClusterManager ncm = project.getNameClusterManager();
		
		TableColumn<Name, String> colNameCluster = new TableColumn<>("Name cluster");
		colNameCluster.setCellValueFactory((TableColumn.CellDataFeatures<Name, String> features) -> {
			Name name = features.getValue();
					
			Optional<NameCluster> nc = ncm.getCluster(name);
			if(nc.isPresent())
				return new ReadOnlyStringWrapper(nc.get().toString());
			
			LOGGER.severe("NO NAME CLUSTER FOUND FOR NAME '" + name + "'!!!");
			return new ReadOnlyStringWrapper("No name cluster found");
		});
		colNameCluster.setPrefWidth(100.0);
		cols.add(colNameCluster);
		
		TableColumn<Name, String> colNameClusterNames = new TableColumn<>("Name cluster names");
		colNameCluster.setCellValueFactory((TableColumn.CellDataFeatures<Name, String> features) -> {
			Name name = features.getValue();
			
			Optional<NameCluster> nc = ncm.getCluster(name);
			if(nc.isPresent())
				return new ReadOnlyStringWrapper(nc.get().getNames().toString());
			
			return new ReadOnlyStringWrapper("No name cluster found");
		});
		colNameClusterNames.setPrefWidth(200.0);
		cols.add(colNameClusterNames);
		
		TableColumn<Name, String> colNameClusterFoundIn = new TableColumn("Name cluster found in");
		colNameCluster.setCellValueFactory((TableColumn.CellDataFeatures<Name, String> features) -> {
			Name name = features.getValue();
			
			Optional<NameCluster> nc = ncm.getCluster(name);
			if(nc.isPresent())
				return new ReadOnlyStringWrapper(nc.get().getFoundInSorted().toString());
			
			return new ReadOnlyStringWrapper("No name cluster found");
		});
		colNameClusterFoundIn.setPrefWidth(200.0);
		cols.add(colNameClusterFoundIn);
		
		// What if it's empty?
		tableView.setPlaceholder(new Label("No names recognized in this dataset."));
	}
	
	private void showDataInTable(ListView<String> listView, TableView<DatasetRow> tableView) {
		// How did we get here without a dataset?
		if(dataset == null)
			return;
		
		// We need to precalculate.
		ObservableList<DatasetRow> rows = dataset.rowsProperty();
		
		// List view should be a single item, "Dataset records (<len>)".
		listView.getItems().setAll("Dataset (" + rows.size() + " rows)");
		listView.getSelectionModel().clearAndSelect(0);
		
		fillTableViewWithDatasetRows(tableView);
	}
	
	private void fillTableViewWithDatasetRows(TableView<DatasetRow> tableView) {
		// We need to precalculate.
		ObservableList<DatasetRow> rows = dataset.rowsProperty();
		
		// Setup table.
		tableView.editableProperty().set(false);
		
		ObservableList<TableColumn<DatasetRow, ?>> cols = tableView.getColumns();
		cols.clear();
		
		// Set up columns.
		TableColumn<DatasetRow, String> colRowName = new TableColumn<>("Name");
		colRowName.setCellValueFactory((TableColumn.CellDataFeatures<DatasetRow, String> features) -> {
			DatasetRow row = features.getValue();
			Set<Name> names = dataset.getNamesInRow(row);
					
			if(names.isEmpty()) {
				return new ReadOnlyStringWrapper("(None)");
			} else {
				return new ReadOnlyStringWrapper(names.stream().map(n -> n.getFullName()).collect(Collectors.joining("; ")));
			}
		});
		colRowName.setPrefWidth(100.0);
		cols.add(colRowName);
		
		// Create a column for every column here.
		dataset.getColumns().forEach((DatasetColumn col) -> {
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
		tableView.itemsProperty().set(rows);
		
		// What if it's empty?
		tableView.setPlaceholder(new Label("No data contained in this dataset."));
	}

	public void selectChange(Change ch) {
		int row = changesTableView.getItems().indexOf(ch);
		if(row == -1)
			row = 0;
		
		LOGGER.fine("Selecting change in row " + row + " (change " + ch + ")");
		
		changesTableView.getSelectionModel().clearAndSelect(row);
		changesTableView.scrollTo(row);
	}
	
	// Export to CSV
	public List<List<String>> getDataAsTable(TableView tv) {
		// What columns do we have?
		List<List<String>> result = new LinkedList<>();		
		List<TableColumn> columns = tv.getColumns();
		
		columns.forEach(col -> {
			List<String> column = new LinkedList<>();
			
			// Add the header.
			column.add(col.getText());
			
			// Add the data.
			for(int x = 0; x < tv.getItems().size(); x++) {
				ObservableValue cellObservableValue = col.getCellObservableValue(x);
				column.add(cellObservableValue.getValue().toString());
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
	
	private void exportToCSV(TableView tv, ActionEvent evt) {
		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().setAll(
			new FileChooser.ExtensionFilter("CSV file", "*.csv"),
			new FileChooser.ExtensionFilter("Tab-delimited file", "*.txt")			
		);
		File file = chooser.showSaveDialog(datasetView.getStage());
		if(file != null) {
			CSVFormat format = CSVFormat.RFC4180;
			
			String outputFormat = chooser.getSelectedExtensionFilter().getDescription();
			if(outputFormat.equalsIgnoreCase("Tab-delimited file"))
				format = CSVFormat.TDF;
			
			try {
				List<List<String>> dataAsTable = getDataAsTable(tv);
				fillCSVFormat(format, new FileWriter(file), dataAsTable);
				
				Alert window = new Alert(Alert.AlertType.CONFIRMATION, "CSV file '" + file + "' saved with " + (dataAsTable.get(0).size() - 1) + " rows.");
				window.showAndWait();
				
			} catch(IOException e) {
				Alert window = new Alert(Alert.AlertType.ERROR, "Could not save CSV to '" + file + "': " + e);
				window.showAndWait();
			}
		}
	}
	
	@FXML
	private void exportChangesToCSV(ActionEvent evt) {
		exportToCSV(changesTableView, evt);
	}
	
	@FXML
	private void displayData(ActionEvent evt) {
		TabularDataViewController tdvc = TabularDataViewController.createTabularDataView();
		
		// TODO: modify this so we can edit that data, too!
		tdvc.getHeaderTextProperty().set("Data contained in dataset " + dataset);
		fillTableViewWithDatasetRows(tdvc.getTableView());
		
		Stage stage = new Stage();
		stage.setScene(tdvc.getScene());
		stage.show();
	}
	
	@FXML
	private void addNewChange(ActionEvent evt) {
		int selectedIndex = changesTableView.getSelectionModel().getSelectedIndex();
		if(selectedIndex < 0)
			selectedIndex = 0;
		
		changesTableView.getItems().add(selectedIndex, new Change(dataset, ChangeType.ERROR, Stream.empty(), Stream.empty()));
	}
}
