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
package com.ggvaidya.scinames.tabulardata;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * FXML Controller class for displaying tabular data: the goal is that any
 * tabular data can be displayed and exported from this view.
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class TabularDataViewController {
	private Scene scene;
	
	public TabularDataViewController() {}
	
	public static TabularDataViewController createTabularDataView() {
		Scene scene;
		FXMLLoader loader = new FXMLLoader(TabularDataViewController.class.getResource("/fxml/TabularDataView.fxml"));
		AnchorPane ap;
		try {
			ap = (AnchorPane) loader.load();
		} catch(IOException e) {
			throw new RuntimeException("Could not load internal file 'TabularDataView.fxml': " + e);
		}
		scene = new Scene(ap);
		TabularDataViewController controller = loader.getController();
		controller.setScene(scene);		
		return controller;
	}
	
	public Scene getScene() { return scene; }
	public void setScene(Scene s) { scene = s; }
	
	/**
	 * Initializes the controller class.
	 */
	public void initialize() {
		tableView.itemsProperty().addListener(
			(ObservableValue observable, Object oldValue, Object newValue) -> {
				ObservableList<Object> list;
				
				if(ObservableList.class.isAssignableFrom(newValue.getClass())) {
					list = (ObservableList<Object>) newValue;
					
					if(list.isEmpty()) {
						statusTextField.setText("Table contains an empty list.");
					} else {
						Object example = list.get(0);
						statusTextField.setText("Table contains " + list.size() + " items of " + example.getClass());
					}
					
				} else {
					statusTextField.setText("Table contains " + newValue);
				}
		});
	}
	
	@FXML
	private TextField headerTextField;

	@FXML
	private TextField statusTextField;	
	
	public StringProperty getHeaderTextProperty() { return headerTextField.textProperty(); }
	public BooleanProperty getHeaderTextEditableProperty() { return headerTextField.editableProperty(); }
	
	@SuppressWarnings("rawtypes")
	@FXML
	private TableView tableView;
	
	@FXML
	private Button addRowButton;
	
	@FXML
	private void addRowToTable(ActionEvent e) {
		// Not implemented.
	}

	public ObservableList<TableColumn> getTableColumnsProperty() { return tableView.getColumns(); }
	public BooleanProperty getTableEditableProperty() { return tableView.editableProperty(); }
	public ObjectProperty<ObservableList> getTableItemsProperty() { return tableView.itemsProperty(); }
	public void setTableColumnResizeProperty(Callback<TableView.ResizeFeatures, Boolean> resizePolicy) { 
		tableView.setColumnResizePolicy(resizePolicy); 
	}
	
	@SuppressWarnings("rawtypes")
	public TableView getTableView() { return tableView; }
	
	/* Exports */
	
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
		List<TableColumn> columns = tableView.getColumns();
		
		columns.forEach(col -> {
			List<String> column = new LinkedList<>();
			
			// Add the header.
			column.add(col.getText());
			
			// Add the data.
			for(int x = 0; x < tableView.getItems().size(); x++) {
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
	
	@FXML private void exportToCSV(ActionEvent evt) {
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
}
