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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.ggvaidya.scinames.model.change.PotentialChange;
import com.ggvaidya.scinames.model.filters.ChangeFilter;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import com.ggvaidya.scinames.util.SimplifiedDate;

import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
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
import javafx.scene.control.TableColumn.SortType;
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
public class BinomialChangesSceneController {
	private static final Logger LOGGER = Logger.getLogger(BinomialChangesSceneController.class.getSimpleName());
	
	/**
	 * If a dataset contains more than this number of changes, then we won't calculate additional
	 * data on them at all. (Eventually, we should just calculate additional data 
	 */
	public static final int ADDITIONAL_DATA_CHANGE_COUNT_LIMIT = 1500;
	
	private BinomialChangesView binomialChangesView;
	private Project project;
	
	public BinomialChangesSceneController() {}

	public void setBinomialChangesView(BinomialChangesView tv) {
		binomialChangesView = tv;
		project = tv.getProjectView().getProject();
		
		// Reinitialize UI to the selected timepoint.
		setupTableWithBinomialChanges();
		updateAdditionalData();
		
		LOGGER.info("Finished setBinomialChangesView()");
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
	@FXML private TableView<PotentialChange> changesTableView;
	
	private void setupTableWithBinomialChanges() {
		changesTableView.setEditable(false);
		changesTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		changesTableView.setItems(potentialChanges);
		
		changesTableView.getColumns().clear();

		TableColumn<PotentialChange, ChangeType> colChangeType = new TableColumn<>("Type");
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
		
		TableColumn<PotentialChange, String> colDataset = new TableColumn<>("Dataset");
		colDataset.setCellValueFactory(cvf -> {
			return new ReadOnlyStringWrapper(
				cvf.getValue().getDataset().toString()
			);
		});
		colDataset.setPrefWidth(150.0);
		changesTableView.getColumns().add(colDataset);
		
		TableColumn<PotentialChange, SimplifiedDate> dateCol = new TableColumn<>("Date");
		dateCol.setCellFactory(TextFieldTableCell.forTableColumn(new SimplifiedDate.SimplifiedDateStringConverter()));
		dateCol.setCellValueFactory(cvf -> new ReadOnlyObjectWrapper<>(cvf.getValue().getDataset().getDate()));
		dateCol.setPrefWidth(150);
		dateCol.setSortable(true);
		dateCol.setSortType(SortType.ASCENDING);
		changesTableView.getColumns().add(dateCol);
		changesTableView.getSortOrder().add(dateCol);
		
		TableColumn<PotentialChange, String> colChangeSummary = new TableColumn<>("Changes summary");
		colChangeSummary.setCellValueFactory(cvf -> {
			Set<Change> changes = changesByPotentialChange.get(cvf.getValue());
			return new ReadOnlyStringWrapper(
				changes.size() + ": " + 
				changes.stream().map(ch -> ch.toString()).collect(Collectors.joining("; "))
			);
		});
		colChangeSummary.setPrefWidth(200.0);
		changesTableView.getColumns().add(colChangeSummary);
		
		/*
		TableColumn<PotentialChange, String> colExplicit = new TableColumn<>("Explicit or implicit?");
		colExplicit.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) -> 
				new ReadOnlyStringWrapper(
					features.getValue().getDataset().isChangeImplicit(features.getValue()) ? "Implicit" : "Explicit"
				)
		);
		tv.getColumns().add(colExplicit);
		
		ChangeFilter cf = binomialChangesView.getProjectView().getProject().getChangeFilter();
		TableColumn<Change, String> colFiltered = new TableColumn<>("Eliminated by filter?");
		colFiltered.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) -> 
				new ReadOnlyStringWrapper(
					cf.test(features.getValue()) ? "Allowed" : "Eliminated"
				)
		);
		tv.getColumns().add(colFiltered);
		*/
		
		TableColumn<PotentialChange, String> colNote = new TableColumn<>("Note");
		colNote.setCellFactory(TextFieldTableCell.forTableColumn());
		colNote.setCellValueFactory(new PropertyValueFactory<>("note"));
		colNote.setPrefWidth(100.0);
		colNote.setEditable(true);
		changesTableView.getColumns().add(colNote);
		
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
		
		TableColumn<PotentialChange, String> colSpecificEpithet = new TableColumn<>("Specific epithets");
		colSpecificEpithet.setCellValueFactory(
			(TableColumn.CellDataFeatures<PotentialChange, String> features) ->
				new ReadOnlyStringWrapper(
					String.join(", ", features.getValue().getAllNames().stream().map(n -> n.getSpecificEpithet()).filter(s -> s != null).distinct().sorted().collect(Collectors.toList()))
				)
		);
		changesTableView.getColumns().add(colSpecificEpithet);
		
		// The infraspecific string.
		TableColumn<PotentialChange, String> colInfraspecificEpithet = new TableColumn<>("Infraspecific epithets");
		colInfraspecificEpithet.setCellValueFactory(
			(TableColumn.CellDataFeatures<PotentialChange, String> features) ->
				new ReadOnlyStringWrapper(
					String.join(", ", features.getValue().getAllNames().stream().map(n -> n.getInfraspecificEpithetsAsString()).filter(s -> s != null).distinct().sorted().collect(Collectors.toList()))
				)
		);
		changesTableView.getColumns().add(colInfraspecificEpithet);
		
		// The very last epithet of all
		TableColumn<PotentialChange, String> colTerminalEpithet = new TableColumn<>("Terminal epithet");
		colTerminalEpithet.setCellValueFactory(
			(TableColumn.CellDataFeatures<PotentialChange, String> features) ->
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
		changesTableView.getColumns().add(colTerminalEpithet);
		
		// Properties
		TableColumn<PotentialChange, String> colProperties = new TableColumn<>("Properties");
		colProperties.setCellValueFactory(
			(TableColumn.CellDataFeatures<PotentialChange, String> features) -> 
				new ReadOnlyStringWrapper(
					features.getValue().getProperties().entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).sorted().collect(Collectors.joining("; "))
				)
		);
		changesTableView.getColumns().add(colProperties);
		
		fillTableWithBinomialChanges();
		
		// When someone selects a cell in the Table, try to select the appropriate data in the
		// additional data view.
		changesTableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<PotentialChange>) lcl -> {
			AdditionalData aData = additionalDataCombobox.getSelectionModel().getSelectedItem();
			
			if(aData != null) {
				aData.onSelectChange(changesTableView.getSelectionModel().getSelectedItems());
			}	
		});
		
		// Create a right-click menu for table rows.
		changesTableView.setRowFactory(table -> {
			TableRow<PotentialChange> row = new TableRow<>();
			
			row.setOnMouseClicked(event -> {
				if(row.isEmpty()) return;
				
				// We don't currently use the clicked change, since currently all options
				// change *all* the selected changes, but this may change in the future.
				Change change = row.getItem();
				
				if(event.getClickCount() == 1 && event.isPopupTrigger()) {
					ContextMenu changeMenu = new ContextMenu();
					
					Menu searchForName = new Menu("Search for name");
					searchForName.getItems().addAll(
						change.getAllNames().stream().sorted()
							.map(n -> createMenuItem(n.getFullName(), action -> {
								binomialChangesView.getProjectView().openDetailedView(n);
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
					
					changeMenu.show(binomialChangesView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
				}
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
	

	/**
	 * Fill the main changes table with potential changes representing binomial changes
	 * made in this dataset. Also updates an additional data object that allows us to
	 * see which changes have been combined for each potential change.  
	 * 
	 */
	private void fillTableWithBinomialChanges() {
		// Preserve search order and selected item.
		List<TableColumn<PotentialChange, ?>> sortByCols = new LinkedList<>(changesTableView.getSortOrder());
		List<PotentialChange> selectedChanges = new LinkedList<>(changesTableView.getSelectionModel().getSelectedItems());
		
		LOGGER.info("About to set changes table items: sortByCols = " + sortByCols + ", selectedChanges = " + selectedChanges);
		calculateAllBinomialChanges();
		LOGGER.info("tv.setItems() completed");
		
		for(PotentialChange ch: selectedChanges) {
			changesTableView.getSelectionModel().select(ch);
		}
		changesTableView.getSortOrder().addAll(sortByCols);
		LOGGER.info("fillTableWithChanges() completed");
	}
	
	ObservableList<PotentialChange> potentialChanges = FXCollections.observableList(new LinkedList<>());
	ObservableMap<PotentialChange, Set<Change>> changesByPotentialChange = FXCollections.observableHashMap();
	
	private void calculateAllBinomialChanges() {
		potentialChanges.clear();
		changesByPotentialChange.clear();
		
		Dataset prevDataset = null;
		for(Dataset ds: project.getDatasets()) {
			if(prevDataset == null) {
				prevDataset = ds;
				continue;
			}
			
			// Step 1. Figure out which binomial names were added and removed.
			Set<Name> binomialNamesInPrev = prevDataset.getRecognizedNames(project).flatMap(n -> n.asBinomial()).collect(Collectors.toSet());
			Set<Name> binomialNamesInCurrent = ds.getRecognizedNames(project).flatMap(n -> n.asBinomial()).collect(Collectors.toSet()); 

			Set<Name> namesAdded = new HashSet<>(binomialNamesInCurrent);
			namesAdded.removeAll(binomialNamesInPrev);
			
			Set<Name> namesDeleted = new HashSet<>(binomialNamesInPrev);
			namesDeleted.removeAll(binomialNamesInCurrent);
			
			// Step 2. Map all changes involving binomial name changes to the
			// binomial names they involve.
			// 
			// Note that this means deliberately skipping changes that *don't* affect
			// binomial composition, such as if a form or variety is deleted but that
			// doesn't result in the binomial name changing.
			List<Change> datasetChanges = ds.getChanges(project).collect(Collectors.toList());
			Map<Name, Set<Change>> changesByBinomialName = new HashMap<>();
			
			for(Change ch: datasetChanges) {
				Set<Name> changeNames = ch.getAllNames();
				Set<Name> changeBinomialNames = changeNames.stream().flatMap(n -> n.asBinomial()).collect(Collectors.toSet());
				
				boolean involvesAddedNames = changeBinomialNames.stream().anyMatch(n -> namesAdded.contains(n));
				boolean involvesDeletedNames = changeBinomialNames.stream().anyMatch(n -> namesDeleted.contains(n));
				
				if(involvesAddedNames || involvesDeletedNames) {
					// Oh goody, involves one of our binomial names.
					//
					// Record all the changes by binomial name
					for(Name binomialName: changeBinomialNames) {
						if(!changesByBinomialName.containsKey(binomialName))
							changesByBinomialName.put(binomialName, new HashSet<>());
						
						changesByBinomialName.get(binomialName).add(ch);
					}
					
				} else {
					// This change is an error or involves non-binomial names only.
					// Ignore!
				}
			}
			
			// Step 3. Convert the additions and deletions into potential changes,
			// based on the changes they include.
			Set<Name> namesChanged = new HashSet<>(namesAdded);
			namesChanged.addAll(namesDeleted);
			
			for(Name n: namesChanged) {
				Set<Change> changes = changesByBinomialName.get(n);
				
				PotentialChange potentialChange = new PotentialChange(
					ds, 
					(namesAdded.contains(n) ? ChangeType.ADDITION : ChangeType.DELETION), 
					(namesAdded.contains(n) ? Stream.empty() : Stream.of(n) ), 
					(namesAdded.contains(n) ? Stream.of(n) : Stream.empty()), 
					BinomialChangesSceneController.class, 
					"Created from " + changes.size() + " changes: " + changes.stream().map(ch -> ch.toString()).collect(Collectors.joining(";"))
				);
				
				// Now, by default, the potential change writes in a long creation note, but
				// we don't want that, do we?
				potentialChange.getProperties().put("created", potentialChange.getNote().orElse(""));
				potentialChange.getProperties().remove("note");
				
				Set<ChangeType> changeTypes = new HashSet<>();
				
				for(Change ch: changes) {
					changeTypes.add(ch.getType());
					
					potentialChange.fromProperty().addAll(ch.getFrom());
					potentialChange.toProperty().addAll(ch.getTo());
					
					Optional<String> currentNote = potentialChange.getNote();
					Optional<String> changeNote = ch.getNote();
					
					if(currentNote.isPresent() && changeNote.isPresent()) {
						potentialChange.noteProperty().set(
							currentNote.get() + "; " +
							changeNote.get()
						);
						
					} else if(!currentNote.isPresent() && changeNote.isPresent()) {
						potentialChange.noteProperty().set(changeNote.get());
						
					} else {
						// Nothing to get hung about.
					}
				}
				
				// Finally, figure out this potential change's type.
				if(changeTypes.size() == 1)
					potentialChange.typeProperty().set(changeTypes.iterator().next());
				else {
					potentialChange.typeProperty().set(ChangeType.COMPLEX);
				}
				
				// All done!
				potentialChanges.add(potentialChange);
				changesByPotentialChange.put(potentialChange, changes);
			}
			
			// Ready for next!
			prevDataset = ds;
		}
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
		File file = chooser.showSaveDialog(binomialChangesView.getStage());
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
	private void refreshChanges(ActionEvent evt) {
		fillTableWithBinomialChanges();
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

		/*
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
		*/
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
					binomialChangesView.getProjectView().openDetailedView(item);
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
		// if(dataset.getAllChanges().count() > ADDITIONAL_DATA_CHANGE_COUNT_LIMIT) return;
		// TODO: fix this by lazy-evaluating these durned lists.
		
		// 0. Summary.
		addDataItems.add(createSummaryAdditionalData());

		// 1.5. Changes by name
		LOGGER.info("Creating changes by potential change additional data");
		addDataItems.add(createChangesByPotentialChangeAdditionalData());
		LOGGER.info("Finished changes by potential change additional data");		
		
		// 1. Changes by name
		/*
		LOGGER.info("Creating changes by name additional data");
		addDataItems.add(createChangesByNameAdditionalData());
		LOGGER.info("Finished changes by name additional data");
		
		// 2. Data by name
		LOGGER.info("Creating data by name additional data");
		addDataItems.add(createDataByNameAdditionalData());
		LOGGER.info("Finished changes by name additional data");
		*/
		
		// 3. Changes by subname
		//LOGGER.info("Creating changes by subnames additional data");
		//addDataItems.add(createChangesBySubnamesAdditionalData());
		//LOGGER.info("Finished changes by subname additional data");

		// 4. Data in this dataset
		/*
		LOGGER.info("Creating data by name additional data");
		addDataItems.add(createDataAdditionalData());
		LOGGER.info("Finished changes by name additional data");
		*/
			
		// 5. Properties
		//LOGGER.info("Creating properties additional data");
		//addDataItems.add(createPropertiesAdditionalData());
		//LOGGER.info("Finished properties additional data");			

		additionalDataCombobox.getSelectionModel().clearAndSelect(0);
		
		LOGGER.info("Finished updateAdditionalData()");
	}
	
	/*
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
	}*/
	
	private AdditionalData<String, Map.Entry<String, String>> createSummaryAdditionalData() {
		List<Map.Entry<String, String>> summary = new ArrayList<>();
		
		// Calculate some summary values.
		long numChanges = potentialChanges.size();
		summary.add(new AbstractMap.SimpleEntry<String, String>(
			"Number of binomial changes", 
			String.valueOf(potentialChanges.size()))
		);
		
		// How many have a note?
		summary.add(new AbstractMap.SimpleEntry<String, String>(
			"Number of changes with annotations", 
			String.valueOf(potentialChanges.stream().filter(ch -> ch.getNote().isPresent()).count()))
		);
		
		// Summarize by types of change.
		Map<ChangeType, List<Change>> potentialChangesByType = potentialChanges.stream().collect(Collectors.groupingBy(ch -> ch.getType()));
		summary.addAll(
			potentialChangesByType.keySet().stream().sorted()
				.map(type -> new AbstractMap.SimpleEntry<String, String>(
					"Number of binomial changes of type '" + type + "'", 
					String.valueOf(potentialChangesByType.get(type).size())
				))
				.collect(Collectors.toList())
		);
		
		// Make an additional data about it.
		Map<String, List<Map.Entry<String, String>>> map = new HashMap<>();
		map.put("Summary", summary);
		
		List<TableColumn<Map.Entry<String, String>, String>> cols = new ArrayList<>();
		
		TableColumn<Map.Entry<String, String>, String> colKey = new TableColumn<>("Property");
		colKey.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getKey()));
		cols.add(colKey);
		
		TableColumn<Map.Entry<String, String>, String> colValue = new TableColumn<>("Value");
		colValue.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getValue()));
		cols.add(colValue);
		
		TableColumn<Map.Entry<String, String>, String> colPercent = new TableColumn<>("Percentage");
		colPercent.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(
			(numChanges == 0 ? "NA" : ((double)Long.parseLong(cdf.getValue().getValue()) / numChanges * 100) + "%")
		));
		cols.add(colPercent);
		
		return new AdditionalData<String, Entry<String, String>>(
			"Summary",
			Arrays.asList(
				"Summary"
			),
			map,
			cols
		);
	}
	
	
	private AdditionalData<Name, Map.Entry<String, String>> createDataByNameAdditionalData() {
		// Which names area we interested in?
		List<PotentialChange> selectedChanges = changesTableView.getItems();
		
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
		
		Project proj = binomialChangesView.getProjectView().getProject();
		
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
	
	private AdditionalData<PotentialChange, Change> createChangesByPotentialChangeAdditionalData() {
		// Which names area we interested in?
		Project proj = binomialChangesView.getProjectView().getProject();
		
		Map<PotentialChange, List<Change>> changesByPotentialChangeAsList = new HashMap<>();
		for(PotentialChange pc: potentialChanges) {
			changesByPotentialChangeAsList.put(pc, new ArrayList<>(changesByPotentialChange.get(pc)));
		}
		
		List<TableColumn<Change, String>> cols = new ArrayList<>();
		
		cols.add(getChangeTableColumn("Dataset", ch -> ch.getDataset().toString()));
		cols.add(getChangeTableColumn("Type", ch -> ch.getType().toString()));
		cols.add(getChangeTableColumn("From", ch -> ch.getFrom().toString()));
		cols.add(getChangeTableColumn("To", ch -> ch.getTo().toString()));
		cols.add(getChangeTableColumn("Note", ch -> ch.getNote().orElse("")));
		
		return new AdditionalData<PotentialChange, Change>(
			"Changes by potential change",
			potentialChanges,
			changesByPotentialChangeAsList,
			cols,
			changes -> (List) changes
		);
	}
	
	private AdditionalData<Name, Change> createChangesByNameAdditionalData() {
		// Which names area we interested in?
		List<PotentialChange> selectedChanges = changesTableView.getItems();
		
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
		
		Project proj = binomialChangesView.getProjectView().getProject();
		
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
		List<PotentialChange> selectedChanges = changesTableView.getItems();
		
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
		
		Project proj = binomialChangesView.getProjectView().getProject();
		
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
