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

import java.awt.image.BufferedImageFilter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.ggvaidya.scinames.model.Checklist;
import com.ggvaidya.scinames.model.ChecklistDiff;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorFactory;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorParseException;
import com.ggvaidya.scinames.util.ExcelImporter;

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
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
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
	
	private static final Background BACKGROUND_RED = new Background(new BackgroundFill(Paint.valueOf("red"), CornerRadii.EMPTY, Insets.EMPTY));
	
	private File currentFile = null;
	private Dataset currentDataset = null;
	public Stream<Dataset> getImportedDatasets() { 
		if(currentDataset == null) return Stream.empty();
		
		return Stream.of(currentDataset); 
	}
	
	private DatasetImporterView datasetImporterView;
	public void setDatasetImporterView(DatasetImporterView div) {
		datasetImporterView = div;
	}
			
	/**
	 * Initializes the controller class.
	 */
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		// Fill in fileFormatComboBox
		initFileFormatComboBox();
	}
	
	private void displayPreview() {
		filePreviewTextArea.setText("");
		if(currentFile == null) return;
		
		if(currentFile.getName().endsWith("xls") || currentFile.getName().endsWith("xlsx")) {
			// Excel files are special! We need to load it special and then preview it.
			ExcelImporter imp;
			
			String excelPreviewText;
			try {
				imp = new ExcelImporter(currentFile);
				List<Sheet> sheets = imp.getWorksheets();
				
				StringBuffer preview = new StringBuffer();
				preview.append("Excel file version " + imp.getWorkbook().getSpreadsheetVersion() + " containing " + sheets.size() + " sheets.\n");
				for(Sheet sh: sheets) {
					preview.append(" - " + sh.getSheetName() + " contains " + sh.getPhysicalNumberOfRows() + " rows.\n");
					
					// No rows?
					if(sh.getPhysicalNumberOfRows() == 0) continue;
					
					// Header row?
					Row headerRow = sh.getRow(0);
					boolean headerEmitted = false;
					
					for(int rowIndex = 1; rowIndex < sh.getPhysicalNumberOfRows(); rowIndex++) {
						if(rowIndex >= 10) break;
						
						Row row = sh.getRow(rowIndex);
						
						if(!headerEmitted) {
							preview.append("  - " + String.join("\t", ExcelImporter.getCellsAsValues(headerRow)) + "\n");
							headerEmitted = true;
						}
						preview.append("  - " + String.join("\t", ExcelImporter.getCellsAsValues(row)) + "\n");
					}
			
					preview.append("\n");
				}
				
				excelPreviewText = preview.toString();
			} catch(IOException ex) {
				excelPreviewText = "Could not open '" + currentFile + "': " + ex;
			}
			
			filePreviewTextArea.setText(excelPreviewText);
			
			return;
		}
		
		// If we're here, then this is some sort of text file, so let's preview the text content directly.
		try {
			LineNumberReader reader = new LineNumberReader(new BufferedReader(new FileReader(currentFile)));
		
			// Load the first ten lines.
			StringBuffer head = new StringBuffer();
			for(int x = 0; x < 10; x++) {
				head.append(reader.readLine());
				head.append('\n');
			}
		
			reader.close();
			filePreviewTextArea.setText(head.toString());
		} catch(IOException ex) {
			filePreviewTextArea.setBackground(BACKGROUND_RED);
			filePreviewTextArea.setText("ERROR: Could not load file '" + currentFile + "': " + ex);
		}
	}
	
	@FXML
	private void loadFromFile(ActionEvent e) {
		if(currentFile == null) return;
		
		try {
			currentDataset = loadDataset();
		} catch(IOException ex) {
			statusTextField.setBackground(BACKGROUND_RED);
			statusTextField.setText("ERROR opening file: " + ex);
			new Alert(AlertType.ERROR, "Could not open file '" + currentFile + "': " + ex).showAndWait();
			return;
		}
		
		datasetNameTextField.setText(currentDataset.asTitle());
		dateTextField.setText(currentDataset.getDate().toString());
		currentDataset.displayInTableView(datasetTableView);
		statusTextField.setText("Displaying " + currentDataset.getRowCount() + " rows from " + currentDataset.getColumns().size() + " columns");
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
	
	/* File format management */
	private void initFileFormatComboBox() {
		fileFormatComboBox.getItems().addAll(
			"List of names",
			"Default CSV",
			"Microsoft Excel CSV",
			"RFC 4180 CSV",
			"Oracle MySQL CSV",
			"Tab-delimited file",
			"TaxDiff file",
			"Excel file"
		);
		fileFormatComboBox.getSelectionModel().clearAndSelect(1);
	}
		
	private Dataset loadDataset() throws IOException {
		String format = fileFormatComboBox.getSelectionModel().getSelectedItem();
		CSVFormat csvFormat = null;
		if(format == null) {
			csvFormat = CSVFormat.DEFAULT;
		} else {
			switch(format) {
				case "List of names": 		return Checklist.fromListInFile(currentFile);
				case "Default CSV": 		csvFormat = CSVFormat.DEFAULT; break;
				case "Microsoft Excel CSV":	csvFormat = CSVFormat.EXCEL; break;
				case "RFC 4180 CSV":		csvFormat = CSVFormat.RFC4180; break;
				case "Oracle MySQL CSV": 	csvFormat = CSVFormat.MYSQL; break;
				case "Tab-delimited file": 	csvFormat = CSVFormat.TDF; break;
				case "TaxDiff file":		return ChecklistDiff.fromTaxDiffFile(currentFile);
				case "Excel file":			return new ExcelImporter(currentFile).asDataset(0);
			}
		}
		
		if(csvFormat == null) {
			LOGGER.info("Could not determine CSV format from format '" + format + "', using CSV default.");
			csvFormat = CSVFormat.DEFAULT;
		}
		
		return Dataset.fromCSV(csvFormat, currentFile);
	}
	
	@FXML
	private void chooseFile(ActionEvent e) {
		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().addAll(
			new FileChooser.ExtensionFilter("Comma-separated values (CSV) file", "*.csv"),				
			new FileChooser.ExtensionFilter("Tab-delimited values file", "*.txt", "*.tab", "*.tsv"),
			new FileChooser.ExtensionFilter("List of names", "*.txt"),
			new FileChooser.ExtensionFilter("TaxDiff file", "*.taxdiff"),
			new FileChooser.ExtensionFilter("Excel file", "*.xls", "*.xlsx")
		);
		
		currentFile = chooser.showOpenDialog(datasetImporterView.getStage());
		if(currentFile == null)
			return;
		
		filePathTextField.setText(currentFile.getAbsolutePath());
		String filterDesc = chooser.getSelectedExtensionFilter().getDescription();
		
		if(filterDesc.startsWith("Comma")) {
			fileFormatComboBox.getSelectionModel().select("Default CSV");
		} else if(filterDesc.startsWith("Tab")) {
			fileFormatComboBox.getSelectionModel().select("Tab-delimited file");
		} else if(filterDesc.startsWith("List of names")) {
			fileFormatComboBox.getSelectionModel().select("List of names");
		} else if(filterDesc.startsWith("TaxDiff")) {
			fileFormatComboBox.getSelectionModel().select("TaxDiff file");
		} else if(filterDesc.startsWith("Excel")) {
			fileFormatComboBox.getSelectionModel().select("Excel file");
		}
		
		displayPreview();
	}
	
	/* FXML objects */
	@FXML private TextField filePathTextField;
	@FXML private TextArea filePreviewTextArea;
	@FXML private ComboBox<String> fileFormatComboBox;
	@FXML private TextField datasetNameTextField;
	@FXML private TextField dateTextField;
	@FXML private TableView<DatasetRow> datasetTableView;
	@FXML private TextField statusTextField;
}
