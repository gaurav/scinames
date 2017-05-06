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

import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.rowextractors.NameExtractor;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorFactory;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorParseException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.csv.CSVFormat;

/**
 * FXML Controller class
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetImporterController implements Initializable {
	private File inputFile;
	
	private List<Dataset> datasets = new LinkedList<>();
	public Stream<Dataset> getImportedDatasets() { return datasets.stream(); }
	
	private DatasetImporterView datasetImporterView;
	public void setDatasetImporterView(DatasetImporterView div, File f) {
		datasetImporterView = div;
		inputFile = f;
		
		reloadFile();
	}
		
	/**
	 * Initializes the controller class.
	 */
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		// Fill in fileFormatComboBox
		initFileFormatComboBox();
		
		// Set up 
		customExtractorTextField.setOnAction(evt -> {
			currentNameExtractor = customExtractorTextField.getText();
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
	
	/*
	private TableColumn<DatasetColumn, String> createColumnForDatasetColumn(String colName, Function<DatasetColumn, String> valFunc) {
		TableColumn<DatasetColumn, String> column = new TableColumn<>(colName);
		column.cellValueFactoryProperty().set(
			(TableColumn.CellDataFeatures<DatasetColumn, String> cdf) -> new ReadOnlyStringWrapper(valFunc.apply(cdf.getValue()))
		);
		column.setPrefWidth(100.0);
		column.setEditable(false);
		return column;
	}*/
	
	private String currentNameExtractor = null;
	private void reloadTextDelimitedFile() {
		datasets.clear();
		
		File textDelimitedFile = inputFile;
		CSVFormat format = getCSVFormatFor(fileFormatComboBox.getSelectionModel().getSelectedItem());
		
		Dataset dataset;
		try {
			dataset = Dataset.fromCSV(format, inputFile);
		} catch(IOException e) {
			new Alert(Alert.AlertType.ERROR, "Could not read from file '" + inputFile + "': " + e).showAndWait();
			return;
		}
		
		// TODO: make this flag-controllable!
		dataset.isChecklistProperty().set(true);
		
		if(currentNameExtractor != null && !currentNameExtractor.equals("")) {
			try {
				dataset.setNameExtractorsString(currentNameExtractor);
			} catch(NameExtractorParseException ex) {
				System.err.println("ERROR: Could not set name extractors string: " + ex);
				// never mind
			}
		}
		
		System.err.println(" - dataset " + dataset + " with name extractor: " + dataset.getNameExtractorsAsString());

		// Display extractors in the delimitedDataTableView.
		delimitedDataTableView.setEditable(true);
		delimitedDataTableView.getColumns().clear();

		// Make a list of every unique name extractor string available.
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
		delimitedDataTableView.getItems().setAll(nameExtractors.stream().distinct().collect(Collectors.toList()));

		// Set up columns.
		TableColumn<String, String> colNameExtractors = new TableColumn<>("Extractors");
		colNameExtractors.cellValueFactoryProperty().set(
			(TableColumn.CellDataFeatures<String, String> cdf) -> new ReadOnlyStringWrapper(cdf.getValue())
		);
		colNameExtractors.setPrefWidth(400.0);
		colNameExtractors.setEditable(false);
		colNameExtractors.cellFactoryProperty().set(TextFieldTableCell.forTableColumn());

		TableColumn<String, String> colStatus = new TableColumn<>("Valid?");
		colStatus.cellValueFactoryProperty().set(
			(TableColumn.CellDataFeatures<String, String> cdf) -> {
				String result;
				String nameExtractorStr = cdf.getValue();

				try {
					List<NameExtractor> xs = NameExtractorFactory.getExtractors(nameExtractorStr);
					result = "Successful (" + xs.size() + " extractors)";
				} catch(NameExtractorParseException ex) {
					result = "Failed: " + ex;
				}

				if(nameExtractorStr != null && nameExtractorStr.equals(dataset.getNameExtractorsAsString()))
					result += " (currently used)";

				return new ReadOnlyStringWrapper(result);
			}
		);
		colStatus.setPrefWidth(100.0);
		colStatus.setEditable(false);

		delimitedDataTableView.getColumns().addAll(
			colNameExtractors,
			colStatus
		);

		// Display in the main table.
		dataset.displayInTableView(importDataTableView);
		datasets.add(dataset);
		
		// Refresh everything.
		delimitedDataTableView.refresh();
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
	@FXML private TableView delimitedDataTableView;
	@FXML private TableView<DatasetRow> importDataTableView;
	@FXML private TextField statusTextField;
	@FXML private TextField customExtractorTextField;
}
