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

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.ggvaidya.scinames.dataset.DatasetChangesView;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorFactory;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorParseException;
import com.ggvaidya.scinames.summary.DataByBinomialNamesTabularView;
import com.ggvaidya.scinames.util.SimplifiedDate;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Paint;

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
		
		datasetDateTextField.textProperty().bindBidirectional(dataset.dateProperty(), new SimplifiedDate.SimplifiedDateStringConverter());
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
		DatasetColumn colToRename = columnComboBox.getValue();
		
		// This may be null. If so, ignore!
		if(colToRename == null) return;
		
		TextInputDialog textInputDialog = new TextInputDialog(colToRename.getName());
		textInputDialog.setTitle("Rename column '" + colToRename.getName() + "'");
		textInputDialog.setHeaderText("Please enter the new name you'd like to give column '" + colToRename.getName() + "'");
		textInputDialog.setContentText("Rename column to:");
		
		Optional<String> optNewName = textInputDialog.showAndWait();
		if(optNewName.isPresent()) {
			String newName = optNewName.get();
			DatasetColumn newColumn = DatasetColumn.of(newName);
			
			// Find out if this rename will require merging.
			if(!dataset.getColumns().contains(newColumn)) {
				// No merging required!
				dataset.rowsProperty().forEach(row -> {
					row.put(newColumn, row.get(colToRename));
					row.remove(colToRename);
				});
				
				// Update columns.
				dataset.getColumns().add(dataset.getColumns().indexOf(colToRename), newColumn);
				dataset.getColumns().remove(colToRename);
				
			} else {
				// Merging required!
				ButtonType result = new Alert(AlertType.CONFIRMATION,
					"Warning: this dataset already contains a column named '" + newColumn.getName() + "'. Would you like to merge these two columns together?",
					ButtonType.YES, ButtonType.NO
				).showAndWait().orElse(ButtonType.NO);
				
				if(result.equals(ButtonType.NO)) return;
				
				// Do the merge!
				dataset.rowsProperty().forEach(row -> {
					String val1 = row.get(colToRename);
					String val2 = row.get(newColumn);
					
					String finalVal = null;
					if(val1 == null || val1.equals("")) finalVal = val2;
					else if(val2 == null || val2.equals("")) finalVal = val1;
					else {
						// Both have values! Concatenate as '"1" and "2"'
						finalVal = '"' + val1 + "\" and \"" + val2 + '"'; 
					}
					
					// This will *replace* newColumn, not add it.
					row.put(newColumn, finalVal);
					row.remove(colToRename);
				});
				
				// Update columns. Since we already have newColumn, we don't need to add it!
				dataset.getColumns().remove(colToRename);
			}
		}
	}
	
	@FXML private void deleteColumn(ActionEvent e) {
	}
	
	@FXML private void editChanges(ActionEvent e) {
		DatasetChangesView view = new DatasetChangesView(datasetEditorView.getProjectView(), dataset);
		view.getStage().show();
	}
	
	@FXML private void displayDataByBinomialName(ActionEvent e) {
		DataByBinomialNamesTabularView view = new DataByBinomialNamesTabularView(datasetEditorView.getProjectView(), dataset);
		view.getStage().show();
	}
}
