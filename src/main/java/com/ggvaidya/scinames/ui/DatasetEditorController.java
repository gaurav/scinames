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
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;

import com.ggvaidya.scinames.dataset.DatasetView;
import com.ggvaidya.scinames.model.Checklist;
import com.ggvaidya.scinames.model.ChecklistDiff;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorFactory;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorParseException;
import com.ggvaidya.scinames.util.SimplifiedDate;

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
public class DatasetEditorController implements Initializable {
	private static final Logger LOGGER = Logger.getLogger(DatasetEditorController.class.getSimpleName());
	
	private static final Background BACKGROUND_RED = new Background(new BackgroundFill(Paint.valueOf("red"), CornerRadii.EMPTY, Insets.EMPTY));
	
	private Dataset dataset = null;
	
	private DatasetEditorView datasetEditorView;
	public void setDatasetEditorView(DatasetEditorView dev, Dataset ds) {
		datasetEditorView = dev;
		dataset = ds;
		
		// Set 'em up!
		datasetTypeComboBox.getSelectionModel().select(dataset.getType());
		datasetTypeComboBox.getSelectionModel().selectedItemProperty().addListener(chl -> {
			dataset.typeProperty().set(datasetTypeComboBox.getSelectionModel().getSelectedItem());
		});
		datasetNameTextField.textProperty().bindBidirectional(dataset.nameProperty());
		
		datasetDateTextField.textProperty().bindBidirectional(dataset.dateProperty(), new StringConverter<SimplifiedDate>() {

			@Override
			public String toString(SimplifiedDate sd) {
				return sd.toString();
			}

			@Override
			public SimplifiedDate fromString(String string) {
				return new SimplifiedDate(string);
			}
			
		});
		columnComboBox.setItems(dataset.getColumns());
		
		// Report.
		statusTextField.setText(
			dataset.getRowCount() + " rows in " + dataset.getColumns().size() + " columns; includes changes: " + dataset.getChangesCountSummary(datasetEditorView.getProjectView().getProject())
		);
		
		// TODO: make this editable!
		dataset.displayInTableView(datasetTableView);
		
		setupNameExtractors();
	}
	
	private void setupNameExtractors() {
		nameExtractorComboBox.getItems().clear();
		
		Set<String> previouslyAdded = new HashSet<>();
		
		// The first one should be the one from the dataset.
		if(dataset != null) {
			nameExtractorComboBox.getItems().add(dataset.getNameExtractorsAsString());
			previouslyAdded.add(dataset.getNameExtractorsAsString());
		}
		
		// Then get all unique name extractors from this project.
		List<String> nameExtractors = datasetEditorView.getProjectView().getProject().getNameExtractors().stream()
			.map(nex -> NameExtractorFactory.serializeExtractorsToString(nex))
			.sorted()
			.collect(Collectors.toList());
		
		for(String nameExtractor: nameExtractors) {
			if(!previouslyAdded.contains(nameExtractor)) {
				nameExtractorComboBox.getItems().add(nameExtractor);
				previouslyAdded.add(nameExtractor);
			}
		}
		
		// Try the default extractors.
		if(!previouslyAdded.contains(NameExtractorFactory.getDefaultExtractorsAsString())) {
			previouslyAdded.add(NameExtractorFactory.getDefaultExtractorsAsString());
			nameExtractorComboBox.getItems().add(NameExtractorFactory.getDefaultExtractorsAsString());
		}
		
		// Set the current one.
		nameExtractorComboBox.getSelectionModel().select(dataset.getNameExtractorsAsString());
		
		// When this changes, update accordingly.
		nameExtractorComboBox.getSelectionModel().selectedItemProperty().addListener(ch -> {
			String strNewNameExtractor = nameExtractorComboBox.getSelectionModel().getSelectedItem();
			String prevNameExtractor = dataset.getNameExtractorsAsString();
			
			// No change? Then ignore!
			if(strNewNameExtractor.trim().equals(prevNameExtractor.trim())) return;

			LOGGER.info("Changing name extractor for dataset " + dataset + " from " + prevNameExtractor + " to " + strNewNameExtractor);
			
			try {
				dataset.setNameExtractorsString(strNewNameExtractor);
			} catch(NameExtractorParseException ex) {
				new Alert(AlertType.ERROR, "Name extractor '" + strNewNameExtractor + "' is not valid: " + ex);
				return;
			}
			
			// LOGGER.info("Name extractor changed for " + dataset + ": " + strNewNameExtractor);
			// LOGGER.info("After changing the name extractor, dataset " + dataset + " has " + dataset.getRowCount() + " rows.");			
			
			dataset.displayInTableView(datasetTableView);
		});
	}
			
	/**
	 * Initializes the controller class.
	 */
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		datasetTypeComboBox.getItems().setAll(
			Dataset.TYPE_CHECKLIST,
			Dataset.TYPE_DATASET
		);
	}
	
	/* FXML objects */
	@FXML private ComboBox<String> datasetTypeComboBox;
	@FXML private TextField datasetNameTextField;
	@FXML private TextField datasetDateTextField;
	@FXML private ComboBox<DatasetColumn> columnComboBox;
	@FXML private ComboBox<String> nameExtractorComboBox;
	@FXML private TableView<DatasetRow> datasetTableView;
	@FXML private TextField statusTextField;
	
	/* FXML events */
	@FXML private void renameColumn(ActionEvent e) {
		
	}
	
	@FXML private void deleteColumn(ActionEvent e) {
	}
	
	@FXML private void editChanges(ActionEvent e) {
		DatasetView view = new DatasetView(datasetEditorView.getProjectView(), dataset);
		view.getStage().show();
	}
}
