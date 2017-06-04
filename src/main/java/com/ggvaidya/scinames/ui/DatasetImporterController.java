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
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;

import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorFactory;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorParseException;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanPropertyBase;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * FXML Controller class
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetImporterController implements Initializable {
	private static final Logger LOGGER = Logger.getLogger(DatasetImporterController.class.getSimpleName());
	
	private File inputFile;
	
	private DatasetColumn columnToSplitBy = null;
	private ObservableList<MappedColumn> mappedColumns = FXCollections.observableArrayList();
	
	private TableColumn<MappedColumn, String> colRenameTo;
	private TableColumn<MappedColumn, Boolean> colSplitBy;
	private List<Dataset> datasets = new LinkedList<>();
	public Stream<Dataset> getImportedDatasets() { return datasets.stream(); }
	
	private DatasetImporterView datasetImporterView;
	public void setDatasetImporterView(DatasetImporterView div, File f) {
		datasetImporterView = div;
		inputFile = f;
		
		reloadFile();
	}
	
	/**
	 * A MappedColumn class to store column mappings.
	 */
	public class MappedColumn {
		public MappedColumn(String from, String to) {
			fromProperty.set(from);
			toProperty.set(to);
		}
		
		public MappedColumn(DatasetColumn from, DatasetColumn to) {
			fromProperty.set(from.getName());
			toProperty.set(to.getName());
		}
		
		private StringProperty fromProperty = new SimpleStringProperty();
		private StringProperty toProperty = new SimpleStringProperty();
		
		public StringProperty fromProperty() { return fromProperty; }
		public StringProperty toProperty() { return toProperty; }
		public BooleanProperty splitOnProperty() {
			// Only one MappedColumn can be splitOn at a given point in time!
			MappedColumn thisMappedColumn = this;
			
			return new BooleanPropertyBase() {
				@Override
				public boolean get() {
					return (columnToSplitBy != null && columnToSplitBy == DatasetColumn.of(fromProperty.get()));	
				}

				@Override
				public void set(boolean value) {
					if(value) {
						columnToSplitBy = DatasetColumn.of(fromProperty.get());
					}
				}

				@Override
				public Object getBean() {
					return thisMappedColumn;
				}

				@Override
				public String getName() {
					return "splitBy";
				}
			};
		}
	}
		
	/**
	 * Initializes the controller class.
	 */
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		// Clear out any previous renames.
		mappedColumns.clear();
		
		// Fill in fileFormatComboBox
		initFileFormatComboBox();
		
		// Set up 
		customExtractorsComboBox.setOnAction(evt -> {
			currentNameExtractor = customExtractorsComboBox.getValue();
			System.err.println("Custom extractor set to: " + currentNameExtractor);			
			reloadTextDelimitedFile();
		});
	}
	
	/** Load and display input file */
	public void reloadFile() {
		// Figure out which tab we're in.
		Tab tab = upperTabPane.getSelectionModel().getSelectedItem();
		switch(tab.getText()) {
			case "Import delimited data": reloadTextDelimitedFile(); break;
			default: System.err.println("Unexpected tab in DatasetImporter: '" + tab.getText() + "'");
		}
	}
	
	private String currentNameExtractor = null;
	private void reloadTextDelimitedFile() {
		datasets.clear();
		
		CSVFormat format = getCSVFormatFor(fileFormatComboBox.getSelectionModel().getSelectedItem());
		
		// Reload renamedColumns from columns
		Map<DatasetColumn, DatasetColumn> renamedColumns = mappedColumns.stream().collect(
			Collectors.toMap(
				map -> DatasetColumn.of(map.fromProperty().get()), 
				map -> DatasetColumn.of(map.toProperty().get())
			)
		);
		
		Dataset dataset;
		try {
			dataset = Dataset.fromCSV(format, inputFile, renamedColumns);
		} catch(IOException e) {
			new Alert(Alert.AlertType.ERROR, "Could not read from file '" + inputFile + "': " + e).showAndWait();
			return;
		}
		
		// TODO: make this flag-controllable!
		dataset.typeProperty().set(Dataset.TYPE_CHECKLIST);
		
		if(currentNameExtractor != null && !currentNameExtractor.equals("")) {
			try {
				dataset.setNameExtractorsString(currentNameExtractor);
			} catch(NameExtractorParseException ex) {
				System.err.println("ERROR: Could not set name extractors string: " + ex);
				// never mind
			}
		}
		
		System.err.println(" - dataset " + dataset + " with name extractor: " + dataset.getNameExtractorsAsString());

		// Make a list of every unique name extractor string available and store in customExtractorsComboBox.
		List<String> nameExtractors = new LinkedList<>();

		if(currentNameExtractor != null && !currentNameExtractor.equals(""))
			nameExtractors.add(currentNameExtractor);

		if(dataset.getNameExtractorsAsString() != null && !dataset.getNameExtractorsAsString().equals(""))
			nameExtractors.add(dataset.getNameExtractorsAsString());

		Map<String, String> props = datasetImporterView.getProjectView().getProject().propertiesProperty();
		if(props.containsKey(Project.PROP_NAME_EXTRACTORS)) {
			nameExtractors.addAll(Arrays.asList(props.get(Project.PROP_NAME_EXTRACTORS).split("\\s*;\\s*")));				
		}
		nameExtractors.add(NameExtractorFactory.getDefaultExtractorsAsString());		
		customExtractorsComboBox.getItems().setAll(nameExtractors.stream().distinct().collect(Collectors.toList()));

		// Set up columns.
		TableColumn<MappedColumn, String> colColumnName = new TableColumn<>("Name");
		colColumnName.cellValueFactoryProperty().set(new PropertyValueFactory<>("from"));
		colColumnName.setPrefWidth(400.0);
		colColumnName.setEditable(false);
		colColumnName.setCellFactory(TextFieldTableCell.forTableColumn());
		
		colRenameTo = new TableColumn<>("Rename to");
		colRenameTo.cellValueFactoryProperty().set(new PropertyValueFactory<>("to"));
		colRenameTo.setPrefWidth(200.0);
		colRenameTo.setEditable(true);
		colRenameTo.setCellFactory(TextFieldTableCell.forTableColumn());
		colRenameTo.onEditCommitProperty().addListener(cl -> reloadFile());
		
		colSplitBy = new TableColumn<>("Split on");
		colSplitBy.setEditable(true);
		colSplitBy.setCellValueFactory(new PropertyValueFactory<>("splitOn"));
		colSplitBy.setCellFactory(cf -> new CheckBoxTableCell());
		
		columnsTableView.getColumns().setAll(
			colColumnName,
			colRenameTo,
			colSplitBy
		);
		columnsTableView.setEditable(true);
		
		for(DatasetColumn col: dataset.getColumns()) {
			if(renamedColumns.containsKey(col))
				// we've already got one
				;
			else
				mappedColumns.add(new MappedColumn(col, col));
		}
		columnsTableView.setItems(mappedColumns);

		// Display in the main table.
		dataset.displayInTableView(importDataTableView);
		datasets.add(dataset);
		
		// Refresh everything.
		columnsTableView.refresh();
		importDataTableView.refresh();
	}
	
	/* File format management */
	private void initFileFormatComboBox() {
		fileFormatComboBox.getItems().addAll(
			"Default CSV",
			"Microsoft Excel CSV",
			"RFC 4180 CSV",
			"Oracle MySQL CSV",
			"Tab-delimited file"
		);
		fileFormatComboBox.getSelectionModel().clearAndSelect(0);
		fileFormatComboBox.getSelectionModel().selectedItemProperty().addListener((a, b, c) -> reloadFile());
	}
	private CSVFormat getCSVFormatFor(String format) {
		if(format == null) return CSVFormat.DEFAULT;
		
		switch(format) {
			case "Default CSV": return CSVFormat.DEFAULT;
			case "Microsoft Excel CSV": return CSVFormat.EXCEL;
			case "RFC 4180 CSV": return CSVFormat.RFC4180;
			case "Oracle MySQL CSV": return CSVFormat.MYSQL;
			case "Tab-delimited file": return CSVFormat.TDF;
			default: 
				System.err.println("File format '" + format + "' not found, using default.");
				return CSVFormat.DEFAULT;
		}
	}
	
	@FXML
	private void importDataset(ActionEvent e) {
		datasetImporterView.getStage().close();
	}
	
	@FXML
	private void cancelImport(ActionEvent e) {
		// Close this without resetting the controller.
		// From http://stackoverflow.com/a/29711376/27310
		
		Stage stage = datasetImporterView.getStage();
		
		stage.fireEvent(
            new WindowEvent(
				stage,
                WindowEvent.WINDOW_CLOSE_REQUEST
            )
        );
	}
	
	/* FXML objects */
	@FXML private ComboBox<String> fileFormatComboBox;
	@FXML private TabPane upperTabPane;
	@FXML private TableView<MappedColumn> columnsTableView;
	@FXML private TableView<DatasetRow> importDataTableView;
	@FXML private TextField statusTextField;
	@FXML private ComboBox<String> customExtractorsComboBox;
}
