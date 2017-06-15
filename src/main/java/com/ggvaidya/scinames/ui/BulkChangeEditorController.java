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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.ggvaidya.scinames.SciNames;
import com.ggvaidya.scinames.model.ChangeType;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.change.ChangeTypeStringConverter;
import com.ggvaidya.scinames.model.change.NameSetStringConverter;
import com.ggvaidya.scinames.model.change.PotentialChange;
import com.ggvaidya.scinames.model.change.RenamesByIdChangeGenerator;
import com.ggvaidya.scinames.model.change.SynonymsFromColumnChangeGenerator;
import com.ggvaidya.scinames.model.filters.ChangeFilter;
import com.ggvaidya.scinames.util.SimplifiedDate;

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
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;

/**
 * FXML Controller class for bulk-creating changes using different methods.
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class BulkChangeEditorController {
	private static final Logger LOGGER = Logger.getLogger(BulkChangeEditor.class.getSimpleName());
	private static final Dataset ALL = new Dataset("All datasets", SimplifiedDate.MIN, Dataset.TYPE_DATASET);
	
	private BulkChangeEditor bulkChangeEditor;
	private Project project;
	
	public BulkChangeEditorController() {}

	public void setBulkChangeEditor(BulkChangeEditor bce) {
		bulkChangeEditor = bce;
		project = bce.getProjectView().getProject();
		
		List<DatasetColumn> columns = project.getDatasets().stream().flatMap(ds -> ds.getColumns().stream()).distinct().collect(Collectors.toList());
		comboBoxNameIdentifiers.setItems(FXCollections.observableArrayList(columns));
		
		comboBoxMethods.getSelectionModel().selectedItemProperty().addListener((a, b, c) -> {
			if(methodsThatNeedAColumn.contains(c)) {
				// Activate it if this method needs a column
				comboBoxNameIdentifiers.setDisable(false);
			} else {
				// Otherwise 
				comboBoxNameIdentifiers.setDisable(true);
			}
		});
		
		ObservableList<Dataset> datasets = FXCollections.observableArrayList();
		datasets.add(ALL);
		datasets.addAll(project.getDatasets());
		datasetsComboBox.setItems(datasets);
		datasetsComboBox.getSelectionModel().clearAndSelect(0);
		
		setupChangesTableView();
		// findChanges();
	}
	
	/**
	 * Initializes the controller class.
	 */
	public void initialize() {
		comboBoxMethods.setItems(availableMethods);
		comboBoxMethods.getSelectionModel().clearAndSelect(0);
		changesTableView.setOnMouseClicked(ms -> {
			if(ms.getClickCount() == 2 && ms.getButton().equals(MouseButton.PRIMARY)) {
				selectChange(changesTableView.getSelectionModel().getSelectedItem());
			}
		});
		
		foundChanges.addListener((ListChangeListener) evt -> {
			statusTextField.setText(
				foundChanges.size() + " changes generated from " 
				+ foundChanges.stream().map(ch -> ch.getDataset()).distinct().count()
				+ " datasets"
			);
		});
	}
	
	/*
	 * User interface.
	 */
	@FXML private ComboBox<String> comboBoxMethods;
	@FXML private ComboBox<DatasetColumn> comboBoxNameIdentifiers;
	@FXML private ComboBox<Dataset> datasetsComboBox;
	@FXML private TableView<PotentialChange> changesTableView;
	@FXML private TextField statusTextField;
	
	/*
	 * Methods for finding changes
	 */
	
	private final ObservableList<String> availableMethods = FXCollections.observableArrayList(Arrays.asList(
		"Find renames using a name identifier field",
		"Find renames using subspecific names",
		"Find renames using species name changes",
		"Find renames using a synonym column",
		"Find lumps/splits using renames"
	));
	private final HashSet<String> methodsThatNeedAColumn = new HashSet<>(Arrays.asList(
		"Find renames using a name identifier field",
		"Find renames using a synonym column"
	));
	
	private ObservableList<PotentialChange> foundChanges = FXCollections.observableList(new LinkedList<>());
	
	@FXML
	public void findChanges() {
		// Clear existing.
		foundChanges.clear();
		
		// Which datasets are we working on?
		Dataset dataset = datasetsComboBox.getValue();
		
		// Which method should we use?
		String method = comboBoxMethods.getSelectionModel().getSelectedItem();
		if(method == null)
			return;
		
		switch(method) {
			case "Find renames using a name identifier field":
				if(dataset == ALL) {
					foundChanges.setAll(
						new RenamesByIdChangeGenerator(comboBoxNameIdentifiers.getSelectionModel().getSelectedItem())
							.generate(project)
							.collect(Collectors.toList())
					);
				} else {
					foundChanges.setAll(
						new RenamesByIdChangeGenerator(comboBoxNameIdentifiers.getSelectionModel().getSelectedItem())
							.generate(project, dataset)
							.collect(Collectors.toList())
					);	
				}
				
				break;
				
			case "Find renames using subspecific names":
				break;
		
			case "Find renames using species name changes":
				break;
				
			case "Find renames using a synonym column":
				if(dataset == ALL) {
					foundChanges.setAll(
						new SynonymsFromColumnChangeGenerator(comboBoxNameIdentifiers.getSelectionModel().getSelectedItem())
							.generate(project)
							.collect(Collectors.toList())
					);
				} else {
					foundChanges.setAll(
						new SynonymsFromColumnChangeGenerator(comboBoxNameIdentifiers.getSelectionModel().getSelectedItem())
							.generate(project, dataset)
							.collect(Collectors.toList())
					);
				}
				break;
				
			case "Find lumps/splits using renames":
				break;
				
			default:
				throw new RuntimeException("No such method known: '" + method + "'");
		}
	}
	
	private void setupChangesTableView() {
		changesTableView.setEditable(true);
		changesTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		changesTableView.getColumns().clear();

		TableColumn<PotentialChange, ChangeType> colChangeType = new TableColumn<>("Type");
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
		changesTableView.getColumns().add(colChangeType);
		
		TableColumn<PotentialChange, ObservableSet<Name>> colChangeFrom = new TableColumn<>("From");
		colChangeFrom.setCellFactory(TextFieldTableCell.forTableColumn(new NameSetStringConverter()));
		colChangeFrom.setCellValueFactory(new PropertyValueFactory<>("from"));
		colChangeFrom.setPrefWidth(200.0);
		colChangeFrom.setEditable(true);
		changesTableView.getColumns().add(colChangeFrom);
		
		TableColumn<PotentialChange, ObservableSet<Name>> colChangeTo = new TableColumn<>("To");
		colChangeTo.setCellFactory(TextFieldTableCell.forTableColumn(new NameSetStringConverter()));	
		colChangeTo.setCellValueFactory(new PropertyValueFactory<>("to"));
		colChangeTo.setPrefWidth(200.0);
		colChangeTo.setEditable(true);
		changesTableView.getColumns().add(colChangeTo);
		
		TableColumn<PotentialChange, String> colChangeDataset = new TableColumn<>("Dataset");
		colChangeDataset.setCellValueFactory(new PropertyValueFactory<>("dataset"));
		colChangeDataset.setPrefWidth(100.0);
		changesTableView.getColumns().add(colChangeDataset);
		
		ChangeFilter cf = project.getChangeFilter();
		TableColumn<PotentialChange, String> colFiltered = new TableColumn<>("Eliminated by filter?");
		colFiltered.setCellValueFactory(
			(TableColumn.CellDataFeatures<PotentialChange, String> features) -> 
				new ReadOnlyStringWrapper(
					cf.test(features.getValue()) ? "Allowed" : "Eliminated"
				)
		);
		changesTableView.getColumns().add(colFiltered);
		
		TableColumn<PotentialChange, String> colNote = new TableColumn<>("Note");
		colNote.setCellFactory(TextFieldTableCell.forTableColumn());
		colNote.setCellValueFactory(new PropertyValueFactory<>("note"));
		colNote.setPrefWidth(100.0);
		colNote.setEditable(true);
		changesTableView.getColumns().add(colNote);
		
		TableColumn<PotentialChange, String> colProperties = new TableColumn<>("Properties");
		colProperties.setCellValueFactory(
			(TableColumn.CellDataFeatures<PotentialChange, String> features) -> 
				new ReadOnlyStringWrapper(
					features.getValue().getProperties().entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).sorted().collect(Collectors.joining("; "))
				)
		);
		changesTableView.getColumns().add(colProperties);

		TableColumn<PotentialChange, String> colCitations = new TableColumn<>("Citations");
		colCitations.setCellValueFactory(
			(TableColumn.CellDataFeatures<PotentialChange, String> features) -> 
				new ReadOnlyStringWrapper(
					features.getValue().getCitationStream().map(citation -> citation.getCitation()).sorted().collect(Collectors.joining("; "))
				)
		);
		changesTableView.getColumns().add(colCitations);
		
		TableColumn<PotentialChange, String> colGenera = new TableColumn<>("Genera");
		colGenera.setCellValueFactory(
			(TableColumn.CellDataFeatures<PotentialChange, String> features) ->
				new ReadOnlyStringWrapper(
					String.join(", ", features.getValue().getAllNames().stream().map(n -> n.getGenus()).distinct().sorted().collect(Collectors.toList()))
				)
		);
		changesTableView.getColumns().add(colGenera);
		
		TableColumn<PotentialChange, String> colSpecificEpithet = new TableColumn<>("Specific epithet");
		colSpecificEpithet.setCellValueFactory(
			(TableColumn.CellDataFeatures<PotentialChange, String> features) ->
				new ReadOnlyStringWrapper(
					String.join(", ", features.getValue().getAllNames().stream().map(n -> n.getSpecificEpithet()).filter(s -> s != null).distinct().sorted().collect(Collectors.toList()))
				)
		);
		changesTableView.getColumns().add(colSpecificEpithet);
		
		// TODO: if we can get an ObservableList over tp.getAllChanges(), then this table
		// will update dynamically as changes are made. Won't that be something.
		// Yes, we want to getAllChanges() so we can see which ones are filtered out.
		changesTableView.setItems(foundChanges);
		changesTableView.getSortOrder().add(colChangeType);
	}

	public void selectChange(PotentialChange ch) {
		int row = changesTableView.getItems().indexOf(ch);
		if(row == -1)
			row = 0;
		
		LOGGER.fine("Selecting change in row " + row + " (change " + ch + ")");
		
		changesTableView.getSelectionModel().clearAndSelect(row);
		changesTableView.scrollTo(row);
	}
	
	@FXML
	private void backupCurrentDataset(ActionEvent evt) {
		// We just need to save this somewhere that isn't the project's actual file location.
		File currentFile = project.getFile();
		
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Save project to ...");
		chooser.setSelectedExtensionFilter(
			new FileChooser.ExtensionFilter("Project XML.gz file", "*.xml.gz")
		);
		File f = chooser.showSaveDialog(bulkChangeEditor.getStage());
		if(f != null) {
			project.setFile(f);
			
			try {
				SciNames.reportMemoryStatus("Saving project " + project + " to disk");
				project.saveToFile();
				SciNames.reportMemoryStatus("Project saved to disk");
				
				new Alert(Alert.AlertType.INFORMATION, "Project saved as " + f + "; subsequent saves will return to " + currentFile)
					.showAndWait();
			} catch (IOException ex) {
				new Alert(Alert.AlertType.ERROR, "Could not save project to file '" + f + "': " + ex)
					.showAndWait();
			}
		}
		
		project.setFile(currentFile);
	}
	
	@FXML
	private void addSelectedChanges(ActionEvent evt) {
		foundChanges.stream().forEach(ch -> {
			ch.getDataset().explicitChangesProperty().add(ch);
		});
		
		new Alert(Alert.AlertType.INFORMATION, foundChanges.size() + " changes added to the project!")
			.showAndWait();
		
		foundChanges.clear();
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
		List<TableColumn<PotentialChange, ?>> columns = changesTableView.getColumns();
		
		columns.forEach(col -> {
			List<String> column = new LinkedList<>();
			
			// Add the header.
			column.add(col.getText());
			
			// Add the data.
			for(int x = 0; x < changesTableView.getItems().size(); x++) {
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
	
	@FXML
	private void exportToCSV(ActionEvent evt) {
		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().setAll(
			new FileChooser.ExtensionFilter("CSV file", "*.csv"),
			new FileChooser.ExtensionFilter("Tab-delimited file", "*.txt")			
		);
		File file = chooser.showSaveDialog(bulkChangeEditor.getStage());
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
