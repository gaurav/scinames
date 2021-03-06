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

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.change.ChangeTypeStringConverter;
import com.ggvaidya.scinames.model.change.NameSetStringConverter;
import com.ggvaidya.scinames.model.filters.ChangeFilter;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;

import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * FXML Controller class for a view of a Dataset in a project. This does a bunch of cool
 * things:
 * 
 * - 1. We provide editable information on dataset rows for a dataset.
 * - 2. We provide editable information on changes for a checklist.
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetSceneController {
	private static final Logger LOGGER = Logger.getLogger(DatasetSceneController.class.getSimpleName());
	
	/**
	 * If a dataset contains more than this number of changes, then we won't calculate additional
	 * data on them at all. (Eventually, we should just calculate additional data 
	 */
	public static final int ADDITIONAL_DATA_CHANGE_COUNT_LIMIT = 150;
	
	private DatasetChangesView datasetView;
	private Dataset dataset;
	
	public DatasetSceneController() {}

	public void setTimepointView(DatasetChangesView tv) {
		datasetView = tv;
		dataset = tv.getDataset();
		
		// Reinitialize UI to the selected timepoint.
		setupTableWithChanges(changesTableView, dataset);
		dataset.lastModifiedProperty().addListener(cl -> {
			fillTableWithChanges(changesTableView, dataset);
		});
		
		updateAdditionalData();
		
		LOGGER.info("Finished setTimepointView()");
	}
	
	/**
	 * Initializes the controller class.
	 */
	public void initialize() {
		initAdditionalData();
		setupMagicButtons();
	}
	
	/*
	 * User interface.
	 */
	@FXML private TableView<Change> changesTableView;
	
	private void setupTableWithChanges(TableView<Change> tv, Dataset tp) {
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
			ChangeType.COMPLEX,
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
		
		TableColumn<Change, String> colSpecificEpithet = new TableColumn<>("Specific epithets");
		colSpecificEpithet.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) ->
				new ReadOnlyStringWrapper(
					String.join(", ", features.getValue().getAllNames().stream().map(n -> n.getSpecificEpithet()).filter(s -> s != null).distinct().sorted().collect(Collectors.toList()))
				)
		);
		tv.getColumns().add(colSpecificEpithet);
		
		// The infraspecific string.
		TableColumn<Change, String> colInfraspecificEpithet = new TableColumn<>("Infraspecific epithets");
		colInfraspecificEpithet.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) ->
				new ReadOnlyStringWrapper(
					String.join(", ", features.getValue().getAllNames().stream().map(n -> n.getInfraspecificEpithetsAsString()).filter(s -> s != null).distinct().sorted().collect(Collectors.toList()))
				)
		);
		tv.getColumns().add(colInfraspecificEpithet);
		
		// The very last epithet of all
		TableColumn<Change, String> colTerminalEpithet = new TableColumn<>("Terminal epithet");
		colTerminalEpithet.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) ->
				new ReadOnlyStringWrapper(
					String.join(", ", features.getValue().getAllNames().stream().map(n -> {
						List<Name.InfraspecificEpithet> infraspecificEpithets = n.getInfraspecificEpithets();
						if(!infraspecificEpithets.isEmpty()) {
							return infraspecificEpithets.get(infraspecificEpithets.size() - 1).getValue();
						} else {
							return n.getSpecificEpithet();
						}
					}).filter(s -> s != null).distinct().sorted().collect(Collectors.toList()))
				)
		);
		tv.getColumns().add(colTerminalEpithet);
		
		// Properties
		TableColumn<Change, String> colProperties = new TableColumn<>("Properties");
		colProperties.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) -> 
				new ReadOnlyStringWrapper(
					features.getValue().getProperties().entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).sorted().collect(Collectors.joining("; "))
				)
		);
		tv.getColumns().add(colProperties);
		
		fillTableWithChanges(tv, tp);
		
		// When someone selects a cell in the Table, try to select the appropriate data in the
		// additional data view.
		tv.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Change>) lcl -> {
			AdditionalData aData = additionalDataCombobox.getSelectionModel().getSelectedItem();
			
			if(aData != null) {
				aData.onSelectChange(tv.getSelectionModel().getSelectedItems());
			}	
		});
		
		// Create a right-click menu for table rows.
		changesTableView.setRowFactory(table -> {
			TableRow<Change> row = new TableRow<>();
			
			row.setOnContextMenuRequested(event -> {
				if(row.isEmpty()) return;
				
				// We don't currently use the clicked change, since currently all options
				// change *all* the selected changes, but this may change in the future.
				Change change = row.getItem();
				
				ContextMenu changeMenu = new ContextMenu();
				
				Menu searchForName = new Menu("Search for name");
				searchForName.getItems().addAll(
					change.getAllNames().stream().sorted()
						.map(n -> createMenuItem(n.getFullName(), action -> {
							datasetView.getProjectView().openDetailedView(n);
						}))
						.collect(Collectors.toList())
				);
				changeMenu.getItems().add(searchForName);
				changeMenu.getItems().add(new SeparatorMenuItem());
				
				changeMenu.getItems().add(createMenuItem("Edit note", action -> {
					List<Change> changes = new ArrayList<>(changesTableView.getSelectionModel().getSelectedItems());
					
					String combinedNotes = changes.stream()
							.map(ch -> ch.getNote().orElse("").trim())
							.distinct()
							.collect(Collectors.joining("\n"))
							.trim();
					
					Optional<String> result = askUserForTextArea("Modify the note for these " + changes.size() + " changes:", combinedNotes);
					
					if(result.isPresent()) {
						String note = result.get().trim();
						LOGGER.info("Using 'Edit note' to set note to '" + note + "' on changes " + changes);
						changes.forEach(ch -> ch.noteProperty().set(note));
					}
				}));
				changeMenu.getItems().add(new SeparatorMenuItem());
				
				// Create a submenu for tags and urls.
				String note = change.noteProperty().get();
				
				Menu removeTags = new Menu("Tags");
				removeTags.getItems().addAll(
					change.getTags().stream().sorted()
						.map(tag -> new MenuItem(tag.getName()))
						.collect(Collectors.toList())
				);
				
				Menu lookupURLs = new Menu("Lookup URL");
				change.getURIs().stream().sorted().map(
					uri -> {
						return createMenuItem(uri.toString(), evt -> {
							try {
								Desktop.getDesktop().browse(uri);
							} catch(IOException ex) {
								LOGGER.warning("Could not open URL '" + uri + "': " + ex);
							}
						});
					}
				).forEach(mi -> lookupURLs.getItems().add(mi));
				changeMenu.getItems().add(lookupURLs);
				
				changeMenu.getItems().add(new SeparatorMenuItem());
				changeMenu.getItems().add(createMenuItem("Prepend text to all notes", action -> {
					List<Change> changes = new ArrayList<>(changesTableView.getSelectionModel().getSelectedItems());
					
					Optional<String> result = askUserForTextField("Enter tags to prepend to notes in " + changes.size() + " changes:");
					
					if(result.isPresent()) {
						String tags = result.get().trim();
						changes.forEach(ch -> {
							String prevValue = change.getNote().orElse("").trim();
							
							LOGGER.info("Prepending tags '" + tags + "' to previous value '" + prevValue + "' for change " + ch);
							
							ch.noteProperty().set((tags + " " + prevValue).trim());
						});
					}
				}));
				changeMenu.getItems().add(createMenuItem("Append text to all notes", action -> {
					List<Change> changes = new ArrayList<>(changesTableView.getSelectionModel().getSelectedItems());
					Optional<String> result = askUserForTextField("Enter tags to append to notes in " + changes.size() + " changes:");
					
					if(result.isPresent()) {
						String tags = result.get().trim();
						changes.forEach(ch -> {
							String prevValue = ch.getNote().orElse("").trim();
							
							LOGGER.info("Appending tags '" + tags + "' to previous value '" + prevValue + "' for change " + ch);
							
							ch.noteProperty().setValue((prevValue + " " + tags).trim());
						});
					}
				}));
				
				changeMenu.show(datasetView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
			
			});
			
			return row;
		});
		
		LOGGER.info("setupTableWithChanges() completed");
	}
	
	private MenuItem createMenuItem(String name, EventHandler<ActionEvent> handler) {
		MenuItem mi = new MenuItem(name);
		mi.onActionProperty().set(handler);
		return mi;
	}
	
	private Optional<String> askUserForTextField(String text) {
		TextField textfield = new TextField();
		
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.getDialogPane().headerTextProperty().set(text);
		dialog.getDialogPane().contentProperty().set(textfield);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		Optional<ButtonType> result = dialog.showAndWait();
		
		if(result.isPresent() && result.get().equals(ButtonType.OK))
			return Optional.of(textfield.getText());
		else
			return Optional.empty();
	}
	
	private Optional<String> askUserForTextArea(String label) {
		return askUserForTextArea(label, null);
	}
	
	private Optional<String> askUserForTextArea(String label, String initialText) {
		TextArea textarea = new TextArea();
		if(initialText != null)
			textarea.setText(initialText);
		
		Dialog<ButtonType> dialog = new Dialog<>();
		dialog.getDialogPane().headerTextProperty().set(label);
		dialog.getDialogPane().contentProperty().set(textarea);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		Optional<ButtonType> result = dialog.showAndWait();
		
		if(result.isPresent() && result.get().equals(ButtonType.OK))
			return Optional.of(textarea.getText());
		else
			return Optional.empty();
	}
	
	private void fillTableWithChanges(TableView<Change> tv, Dataset tp) {
		// Preserve search order and selected item.
		List<TableColumn<Change, ?>> sortByCols = new LinkedList<>(tv.getSortOrder());
		List<Change> selectedChanges = new LinkedList<>(tv.getSelectionModel().getSelectedItems());
		
		LOGGER.info("About to set changes table items: sortByCols = " + sortByCols + ", selectedChanges = " + selectedChanges);
		tv.setItems(FXCollections.observableList(tp.getAllChangesAsList()));
		LOGGER.info("tv.setItems() completed");
		
		for(Change ch: selectedChanges) {
			tv.getSelectionModel().select(ch);
		}
		tv.getSortOrder().addAll(sortByCols);
		LOGGER.info("fillTableWithChanges() completed");
	}

	public void selectChange(Change ch) {
		int row = changesTableView.getItems().indexOf(ch);
		if(row == -1)
			row = 0;
		
		LOGGER.info("Selecting change in row " + row + " (change " + ch + ")");
		
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
		tdvc.getHeaderTextProperty().set("Data contained in dataset " + dataset); // TODO we can search for names here, dude.
		fillTableViewWithDatasetRows(tdvc.getTableView());
		
		Stage stage = new Stage();
		stage.setTitle("Rows from " + dataset.asTitle());
		stage.setScene(tdvc.getScene());
		stage.show();
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
	
	@FXML
	private void addNewChange(ActionEvent evt) {
		int selectedIndex = changesTableView.getSelectionModel().getSelectedIndex();
		if(selectedIndex < 0)
			selectedIndex = 0;
		
		changesTableView.getItems().add(selectedIndex, new Change(dataset, ChangeType.ERROR, Stream.empty(), Stream.empty()));
	}
	
	@FXML
	private void deleteExplicitChange(ActionEvent evt) {
		List<Change> changesToDelete = new ArrayList<>(changesTableView.getSelectionModel().getSelectedItems());
		List<Change> explicitChangesToDelete = changesToDelete.stream()
			.filter(ch -> !ch.getDataset().isChangeImplicit(ch))
			.collect(Collectors.toList());
		
		if(explicitChangesToDelete.isEmpty())
			return;
		
		// Explicit changes! Verify before deleting.
		Optional<ButtonType> opt = new Alert(AlertType.CONFIRMATION, 
			"Are you sure you want to delete " + explicitChangesToDelete.size() + " explicit changes starting with " +
			explicitChangesToDelete.get(0).toString() + "? This cannot be undone!"
		).showAndWait();
		if(!opt.isPresent() || !opt.get().equals(ButtonType.OK))
			return;
		
		// Okay, we're verified! Time to die.
		for(Change ch: explicitChangesToDelete) {
			ch.getDataset().deleteChange(ch);
		}
	}
	
	@FXML
	private void combineChanges(ActionEvent evt) {
		List<Change> changes = new ArrayList<>(changesTableView.getSelectionModel().getSelectedItems());
		
		if(changes.size() < 2) {
			// Need two or more changes!
			return;
		}
		
		// Combine them changes! This means:
		//	1. 	Get the first change.
		//	2. 	For every subsequent change, add *everything* about that change to the first change,
		//		then delete it.
		
		Change firstChange = null;
		Map<String, Set<String>> combinedProperties = new HashMap<>();
		for(Change ch: changes) {
			if(firstChange == null) {
				firstChange = ch;
				
				// Set up the combined properties.
				combinedProperties = firstChange.getProperties().entrySet().stream().collect(Collectors.toMap(
					(Map.Entry<String, String> entry) -> entry.getKey(),
					(Map.Entry<String, String> entry) -> { 
						HashSet<String> hs = new HashSet<String>(); 
						hs.add(entry.getValue()); 
						return hs; 
					}
				));
				
				continue;
			}
			
			// Add 'from's to firstChange.
			firstChange.getFrom().addAll(ch.getFrom());
			firstChange.getTo().addAll(ch.getTo());
			firstChange.getCitations().addAll(ch.getCitations());
			
			// Combine properties.
			for(Map.Entry<String, String> entry: ch.getProperties().entrySet()) {
				if(!combinedProperties.containsKey(entry.getKey()))
					combinedProperties.put(entry.getKey(), new HashSet<>());
				
				combinedProperties.get(entry.getKey()).add(entry.getValue());
			}
			
			// Done!
			dataset.deleteChange(ch);
		}
		
		// First change might be implicit! Make it explicit!
		dataset.makeChangeExplicit(firstChange);
		
		// Add all the combined properties back into firstChange.
		for(String key: combinedProperties.keySet()) {
			firstChange.getProperties().put(key, combinedProperties.get(key).stream().collect(Collectors.joining("; ")));
		}
		
		// Guess the new type.
		int fromCount = firstChange.getFrom().size();
		int toCount = firstChange.getTo().size();
		
		if(fromCount > 0 && toCount > 0)
			firstChange.typeProperty().setValue(ChangeType.RENAME);
		else if(fromCount > 0 && toCount == 0)
			firstChange.typeProperty().setValue(ChangeType.DELETION);
		else if(fromCount == 0 && toCount > 0)
			firstChange.typeProperty().setValue(ChangeType.ADDITION);
		else
			firstChange.typeProperty().setValue(ChangeType.ERROR);
	}
	
	@FXML
	private void refreshChanges(ActionEvent evt) {
		fillTableWithChanges(changesTableView, dataset);
	}
	
	@FXML
	private void divideChange(ActionEvent evt) {
		List<Change> changes = changesTableView.getSelectionModel().getSelectedItems();
				
		for(Change ch: changes) {
			// Divide them changes! For our purposes,
			// this works like this:
			//		- Remove all the 'froms' from the change.
			//		- Create a new change identical to the first chnage, and
			//		  remove all its 'tos'.
			
			// TODO
		}
	}
	
	/* Some buttons are magic. */
	@FXML private Button combineChangesButton;
	@FXML private Button divideChangeButton;
	@FXML private Button deleteExplicitChangeButton;
	
	private void setupMagicButtons() {
		// Disable everything to begin with.
		combineChangesButton.disableProperty().set(true);
		divideChangeButton.disableProperty().set(true);
		deleteExplicitChangeButton.disableProperty().set(true);
		
		// Switch them on and off based on the selection.
		changesTableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Change>) ch -> {
			int countSelectionItems = ch.getList().size();
			
			if(countSelectionItems == 0) {
				// No selection? None of those buttons should be on.
				combineChangesButton.disableProperty().set(true);
				divideChangeButton.disableProperty().set(true);
				deleteExplicitChangeButton.disableProperty().set(true);
			} else if(countSelectionItems == 1) {
				// Exactly one? We can split, but not combine.
				combineChangesButton.disableProperty().set(true);
				divideChangeButton.disableProperty().set(false);
				deleteExplicitChangeButton.disableProperty().set(false);				
			} else {
				// More than one? We can combine, but not split.
				combineChangesButton.disableProperty().set(false);
				divideChangeButton.disableProperty().set(true);
				deleteExplicitChangeButton.disableProperty().set(false);				
			}
		});
	}
	
	/*
	 * The additional data system.
	 * 
	 * Here's how this works:
	 * 	- Everything is wrapped up into an AdditionalData class.
	 *  - There's a bunch of code that knows how to convert AdditionalData objects into
	 *    list/table combinations.
	 *  - There's a separate bunch of code that builds AdditionalData objects.
	 */
	
	private class AdditionalData<ListOf, TableOf> {
		private String name;
		public String getName() { return name; }
		@Override public String toString() { return name; }
		
		private ObservableList<ListOf> listOf;
		private ObservableMap<ListOf, List<TableOf>> tableOf;
		private List<TableColumn<TableOf, String>> columns;
		private Function<List<Change>, List<ListOf>> onSelectChange;
		
		private AdditionalData(String name, List<ListOf> listOf, Map<ListOf, List<TableOf>> tableOfMap, List<TableColumn<TableOf, String>> columns) {
			this(name, listOf, tableOfMap, columns, null);
		}
		
		private AdditionalData(String name, List<ListOf> listOf, Map<ListOf, List<TableOf>> tableOfMap, List<TableColumn<TableOf, String>> columns, Function<List<Change>, List<ListOf>> onSelectChange) {
			this.name = name;
			this.listOf = FXCollections.observableList(listOf);
			this.tableOf = FXCollections.observableMap(tableOfMap);
			this.columns = FXCollections.observableList(columns);
			this.onSelectChange = onSelectChange;
		}
		
		public List<ListOf> getList() {
			return listOf;
		}
		
		public List<TableOf> getTableRowsFor(ListOf listOfItem) {
			return tableOf.getOrDefault(listOfItem, new ArrayList<>());
		}
		
		public List<TableColumn<TableOf, String>> getColumns() {
			return columns;
		}
		
		public void onSelectChange(List<Change> selectedChanges) {
			if(onSelectChange == null) return;
			
			additionalListView.getSelectionModel().clearSelection();
			List<ListOf> listOfs = onSelectChange.apply(selectedChanges);
			if(listOfs.isEmpty()) return;
			
			for(ListOf lo: listOfs) {
				additionalListView.getSelectionModel().select(lo);
			}
			
			// Scroll to the first name.
			additionalListView.scrollTo(listOfs.get(0));
		}
	}
	
	@SuppressWarnings("rawtypes")
	@FXML private TableView additionalDataTableView;
	@SuppressWarnings("rawtypes")
	@FXML private ListView additionalListView;
	@SuppressWarnings("rawtypes")
	@FXML private ComboBox<AdditionalData> additionalDataCombobox;
	
	@SuppressWarnings("rawtypes")
	private ObservableList tableItems = FXCollections.observableList(new LinkedList());
	
	// The following methods switch between additional data views.
	@SuppressWarnings("unchecked")
	private void initAdditionalData() {
		// Resize to fit columns, as per https://stackoverflow.com/a/22488513/27310
		additionalDataTableView.setColumnResizePolicy((param) -> true);
		
		// Set up additional data objects.
		additionalDataTableView.setRowFactory(table -> {
			@SuppressWarnings("rawtypes")
			TableRow row = new TableRow<>();
			
			row.setOnMouseClicked(event -> {
				if(row.isEmpty()) return;
				Object item = row.getItem();
				
				if(event.getClickCount() == 2) {
					// Try opening the detailed view on this item -- if we can.
					datasetView.getProjectView().openDetailedView(item);
				}
			});
			
			return row;
		});
		additionalDataTableView.setItems(new SortedList<>(tableItems));
		
		// Set up events.
		additionalDataCombobox.getSelectionModel().selectedItemProperty().addListener((Observable o) -> additionalDataUpdateList());
		additionalListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		additionalListView.getSelectionModel().selectedItemProperty().addListener((Observable o) -> additionalDataUpdateTable());
		
		additionalDataCombobox.getSelectionModel().select(0);
		
		// When the change is changed, select an item.
		changesTableView.getSelectionModel().getSelectedItems().addListener(
			(ListChangeListener<Change>) c -> additionalDataUpdateList()
		);
	}
	
	private void additionalDataUpdateList() {
		// Which AdditionalData and ListOf are we in right now?
		AdditionalData aData = additionalDataCombobox.getSelectionModel().getSelectedItem();
		
		// No aData? Do nothing!
		if(aData == null) return;
				
		// Object currentSelection = additionalListView.getSelectionModel().getSelectedItem();
		
		additionalListView.setItems(FXCollections.observableList(aData.getList()));
		additionalListView.getSelectionModel().clearAndSelect(0);
		
		// This is also the right time to set up columns for the table.
		additionalDataTableView.getColumns().clear();
		additionalDataTableView.getColumns().addAll(aData.getColumns());
		
		// additionalListView.getSelectionModel().select(prevSelection);
	}
	
	private void additionalDataUpdateTable() {
		// Which AdditionalData and ListOf are we in right now?
		AdditionalData aData = additionalDataCombobox.getSelectionModel().getSelectedItem();
		
		// Redraw the table.
		tableItems.clear();
		tableItems.addAll(aData.getTableRowsFor(additionalListView.getSelectionModel().getSelectedItem()));
	}
	
	// The following AdditionalData objects provide all the additional data views we need.
	@SuppressWarnings("rawtypes")
	private void updateAdditionalData() {
		ObservableList<AdditionalData> addDataItems = FXCollections.observableArrayList();

		// Done!
		additionalDataCombobox.setItems(addDataItems);
		
		// We can just about get away with doing this for around ADDITIONAL_DATA_CHANGE_COUNT_LIMIT changes.
		if(dataset.getAllChanges().count() > ADDITIONAL_DATA_CHANGE_COUNT_LIMIT) return;
		// TODO: fix this by lazy-evaluating these durned lists.
		
		// 1. Changes by name
		LOGGER.info("Creating changes by name additional data");
		addDataItems.add(createChangesByNameAdditionalData());
		LOGGER.info("Finished changes by name additional data");
		
		// 2. Data by name
		LOGGER.info("Creating data by name additional data");
		addDataItems.add(createDataByNameAdditionalData());
		LOGGER.info("Finished changes by name additional data");
		
		// 3. Changes by subname
		LOGGER.info("Creating changes by subnames additional data");
		addDataItems.add(createChangesBySubnamesAdditionalData());
		LOGGER.info("Finished changes by subname additional data");

		// 4. Data in this dataset
		/*
		LOGGER.info("Creating data by name additional data");
		addDataItems.add(createDataAdditionalData());
		LOGGER.info("Finished changes by name additional data");
		*/
			
		// 5. Properties
		LOGGER.info("Creating properties additional data");
		addDataItems.add(createPropertiesAdditionalData());
		LOGGER.info("Finished properties additional data");			

		additionalDataCombobox.getSelectionModel().clearAndSelect(0);
		
		LOGGER.info("Finished updateAdditionalData()");
	}
	
	private AdditionalData<String, DatasetRow> createDataAdditionalData() {
		Map<String, List<DatasetRow>> map = new HashMap<>();
		map.put("All data (" + dataset.getRowCount() + " rows)", new ArrayList<DatasetRow>(dataset.rowsProperty()));
		
		List<TableColumn<DatasetRow, String>> cols = new LinkedList<>(); 
		for(DatasetColumn col: dataset.getColumns()) {
			TableColumn<DatasetRow, String> column = new TableColumn<>(col.getName());
			column.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().get(col)));
			cols.add(column);
		}
		
		return new AdditionalData(
			"Data",
			Arrays.asList("All data (" + dataset.getRowCount() + " rows)"),
			map,
			cols
		);
	}
	
	private AdditionalData<String, Map.Entry<String, String>> createPropertiesAdditionalData() {
		List<Map.Entry<String, String>> datasetProperties = new ArrayList<>(dataset.getProperties().entrySet());
		
		Map<String, List<Map.Entry<String, String>>> map = new HashMap<>();
		map.put("Dataset (" + datasetProperties.size() + ")", datasetProperties);
		
		List<TableColumn<Map.Entry<String, String>, String>> cols = new ArrayList<>();
		
		TableColumn<Map.Entry<String, String>, String> colKey = new TableColumn<>("Key");
		colKey.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getKey()));
		cols.add(colKey);
		
		TableColumn<Map.Entry<String, String>, String> colValue = new TableColumn<>("Value");
		colValue.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getValue()));
		cols.add(colValue);
		
		return new AdditionalData(
			"Properties",
			Arrays.asList(
				"Dataset (" + datasetProperties.size() + ")"
			),
			map,
			cols
		);
	}
	
	private AdditionalData<Name, Map.Entry<String, String>> createDataByNameAdditionalData() {
		// Which names area we interested in?
		List<Change> selectedChanges = changesTableView.getItems();
		
		List<Name> names = selectedChanges.stream()
			.flatMap(ch -> {
				Set<Name> allNames = ch.getAllNames();
				List<Name> binomials = allNames.stream().flatMap(n -> n.asBinomial()).collect(Collectors.toList());
				List<Name> genus = allNames.stream().flatMap(n -> n.asGenus()).collect(Collectors.toList());
				
				allNames.addAll(binomials);
				allNames.addAll(genus);
				
				return allNames.stream();
			})
			.distinct()
			.sorted()
			.collect(Collectors.toList());
		
		Project proj = datasetView.getProjectView().getProject();
		
		Map<Name, List<Map.Entry<String, String>>> map = new HashMap<>();
		for(Name n: names) {
			Map<DatasetColumn, Set<String>> dataForName = proj.getDataForName(n);
			Map<String, String> mapForName = dataForName.entrySet().stream().collect(Collectors.toMap(
				(Map.Entry<DatasetColumn, Set<String>> entry) -> entry.getKey().toString(), 
				(Map.Entry<DatasetColumn, Set<String>> entry) -> entry.getValue().toString()
			));
			map.put(n, new ArrayList<>(mapForName.entrySet()));
		}
		
		List<TableColumn<Map.Entry<String, String>, String>> cols = new ArrayList<>();
		
		TableColumn<Map.Entry<String, String>, String> colKey = new TableColumn<>("Key");
		colKey.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getKey()));
		cols.add(colKey);
		
		TableColumn<Map.Entry<String, String>, String> colValue = new TableColumn<>("Value");
		colValue.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getValue()));
		cols.add(colValue);
		
		return new AdditionalData<Name, Map.Entry<String, String>>(
			"Data by name",
			names,
			map,
			cols,
			changes -> changes.stream().flatMap(ch -> ch.getAllNames().stream()).collect(Collectors.toList())
		);
	}
	
	private TableColumn<Change, String> getChangeTableColumn(String colName, Function<Change, String> func) {
		TableColumn<Change, String> col = new TableColumn<>(colName);
		col.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(func.apply(cdf.getValue())));
		return col;
	}
	
	private AdditionalData<Name, Change> createChangesByNameAdditionalData() {
		// Which names area we interested in?
		List<Change> selectedChanges = changesTableView.getItems();
		
		List<Name> names = selectedChanges.stream()
			.flatMap(ch -> {
				Set<Name> allNames = ch.getAllNames();
				List<Name> binomials = allNames.stream().flatMap(n -> n.asBinomial()).collect(Collectors.toList());
				List<Name> genus = allNames.stream().flatMap(n -> n.asGenus()).collect(Collectors.toList());
				
				allNames.addAll(binomials);
				allNames.addAll(genus);
				
				return allNames.stream();
			})
			.distinct()
			.sorted()
			.collect(Collectors.toList());
		
		Project proj = datasetView.getProjectView().getProject();
		
		Map<Name, List<Change>> map = new HashMap<>();
		for(Name n: names) {
			map.put(n,  proj.getDatasets().stream()
				.flatMap(ds -> ds.getAllChanges())
				.filter(ch -> ch.getAllNames().contains(n))
				.collect(Collectors.toList()));
		}
		
		List<TableColumn<Change, String>> cols = new ArrayList<>();
		
		cols.add(getChangeTableColumn("Dataset", ch -> ch.getDataset().toString()));
		cols.add(getChangeTableColumn("Type", ch -> ch.getType().toString()));
		cols.add(getChangeTableColumn("From", ch -> ch.getFrom().toString()));
		cols.add(getChangeTableColumn("To", ch -> ch.getTo().toString()));
		cols.add(getChangeTableColumn("Note", ch -> ch.getNote().orElse("")));
		
		return new AdditionalData<Name, Change>(
			"Changes by name",
			names,
			map,
			cols,
			changes -> changes.stream().flatMap(ch -> ch.getAllNames().stream()).collect(Collectors.toList())
		);
	}
	
	private AdditionalData<Name, Change> createChangesBySubnamesAdditionalData() {
		// Which names area we interested in?
		List<Change> selectedChanges = changesTableView.getItems();
		
		List<Name> names = selectedChanges.stream()
			.flatMap(ch -> {
				Set<Name> allNames = ch.getAllNames();
				List<Name> binomials = allNames.stream().flatMap(n -> n.asBinomial()).collect(Collectors.toList());
				List<Name> genus = allNames.stream().flatMap(n -> n.asGenus()).collect(Collectors.toList());
				
				allNames.addAll(binomials);
				allNames.addAll(genus);
				
				return allNames.stream();
			})
			.distinct()
			.sorted()
			.collect(Collectors.toList());
		
		Project proj = datasetView.getProjectView().getProject();
		
		Map<Name, List<Change>> map = new HashMap<>();
		for(Name query: names) {
			map.put(query,  
				proj.getDatasets().stream()
					.flatMap(ds -> ds.getAllChanges())
					.filter(ch ->
							ch.getAllNames().contains(query)
							|| ch.getAllNames().stream().flatMap(n -> n.asBinomial()).anyMatch(binomial -> query.equals(binomial))
							|| ch.getAllNames().stream().flatMap(n -> n.asGenus()).anyMatch(genus -> query.equals(genus))
					)
					.collect(Collectors.toList())
			);
		}
		
		List<TableColumn<Change, String>> cols = new ArrayList<>();
		
		cols.add(getChangeTableColumn("Dataset", ch -> ch.getDataset().toString()));
		cols.add(getChangeTableColumn("Type", ch -> ch.getType().toString()));
		cols.add(getChangeTableColumn("From", ch -> ch.getFrom().toString()));
		cols.add(getChangeTableColumn("To", ch -> ch.getTo().toString()));
		cols.add(getChangeTableColumn("Note", ch -> ch.getNote().orElse("")));
		
		return new AdditionalData<Name, Change>(
			"Changes by subname",
			names,
			map,
			cols,
			changes -> changes.stream().flatMap(ch -> ch.getAllNames().stream()).collect(Collectors.toList())
		);
	}
}
