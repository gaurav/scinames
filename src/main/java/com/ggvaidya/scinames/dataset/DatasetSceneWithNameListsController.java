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

import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.filters.ChangeFilter;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.NameClusterManager;
import com.ggvaidya.scinames.model.ChangeType;
import com.ggvaidya.scinames.model.change.ChangeTypeStringConverter;
import com.ggvaidya.scinames.model.change.NameSetStringConverter;
import com.ggvaidya.scinames.summary.DatasetTabularView;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableSet;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;

/**
 * FXML Controller class for a view of a Timepoint in a project.
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetSceneWithNameListsController {
	private DatasetViewWithNameLists timepointView;
	private Dataset timepoint;

	public void setTimepointView(DatasetViewWithNameLists tv) {
		timepointView = tv;
		timepoint = tv.getDataset();
		
		// Reinitialize UI to the selected timepoint.
		updateMainTextArea();
		fillTableWithNamesFrom(prevTimepointTableView, timepoint.getPreviousDataset());
		fillTableWithNamesFrom(currTimepointTableView, timepoint);
		fillTableWithChanges(changesTableView, timepoint);
		
		// Can we view this as data?
		if(timepoint != null) {
			viewDataButton.setDisable(!Dataset.class.isAssignableFrom(timepoint.getClass()));
		}
	}
	
	/**
	 * Initializes the controller class.
	 */
	public void initialize() {
		updateMainTextArea();
		
		// Tie the two tableviews together.
		//currTimepointTableView.onScrollProperty().bind(prevTimepointTableView.onScrollProperty());
		
		// When an item is selected on any of the views, select it in all
		// the other views as well.
		prevTimepointTableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener.Change change) -> {
			selectNames(prevTimepointTableView, change.getList());
		});
		currTimepointTableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener.Change change) -> {
			selectNames(currTimepointTableView, change.getList());
		});
		changesTableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener.Change change) -> {
			selectChanges(changesTableView, change.getList());
		});
	}
	
	/*
	 * User interface.
	 */
	@FXML private TableView prevTimepointTableView;
	@FXML private TableView currTimepointTableView;
	@FXML private TableView changesTableView;
	@FXML private TextArea mainTextArea;
	@FXML private Button viewDataButton;
	
	private void selectChanges(Control target, List<Change> selectedChanges) {
		updateMainTextArea();
	}

	private void selectNames(Control target, List<Name> selectedNames) {
		updateMainTextArea();
	}
	
	private void updateMainTextArea() {
		// No timepoint, no content.
		if(timepoint == null) {
			mainTextArea.setText("No timepoint loaded.");
			return;
		}
		
		NameClusterManager nameClusterManager = timepointView.getProjectView().getProject().getNameClusterManager();
		
		// We have three sections: timepoint, changes and names.
		StringBuilder timepointInfo = new StringBuilder();
		StringBuilder changesInfo = new StringBuilder();
		StringBuilder namesInfo = new StringBuilder();
		Set<Name> allNames = new HashSet<>();
		
		// What do we know about this timepoint?
		timepointInfo
			.append("Timepoint: ").append(timepoint.getCitation()).append(", published in ").append(timepoint.getDate()).append("\n")
			.append("Names: ").append(timepoint.getNameCountSummary(timepointView.getProjectView().getProject())).append("\n")
			.append("Binomial names: ").append(timepoint.getBinomialCountSummary(timepointView.getProjectView().getProject())).append("\n")
			.append("Explicit changes: ").append(timepoint.getExplicitChangesCountSummary(timepointView.getProjectView().getProject())).append("\n")
			.append("Implicit changes: ").append(timepoint.getImplicitChangesCountSummary(timepointView.getProjectView().getProject())).append("\n")
		;
		
		// What do we know about the selected changes?
		changesTableView.getSelectionModel().getSelectedItems().forEach((Object oChange) -> {
			Change change = (Change) oChange;
			changesInfo.append(" - ").append(change.toString()).append("\n");
			allNames.addAll(change.getAllNames());
		});
		
		// What do we know about the selected names?
		allNames.addAll(prevTimepointTableView.getSelectionModel().getSelectedItems());
		allNames.addAll(currTimepointTableView.getSelectionModel().getSelectedItems());		
			
		allNames.stream().sorted().forEach((Name name) -> {
			String clusterInfo = "(not found in clusters)";
			Optional<NameCluster> cluster = nameClusterManager.getCluster(name);
			if(cluster.isPresent())
				clusterInfo = cluster.get().toString();
				
			namesInfo.append(" - ").append(name.toString())
				.append(" in ")
				.append(clusterInfo)
				.append("\n");
			
			Map<DatasetColumn, Set<String>> dataByColumn = timepointView.getProjectView().getProject().getDataForName(name);
			for(DatasetColumn col: dataByColumn.keySet()) {
				List<String> vals = new LinkedList<>(dataByColumn.get(col));
				Collections.sort(vals);
				
				namesInfo.append("\t - ")
					.append(col)
					.append(": ")
					.append(String.join(", ", vals))
					.append("\n");
			}
		});
		
		// Put it all together.
		mainTextArea.setText(
			timepointInfo
			.append("\n== Selected Changes ==\n").append(changesInfo)
			.append("\n== Selected Names ==\n").append(namesInfo)
			.toString()
		);
	}
	
	// TODO: Think about colour coding additions and deletions?
	// TODO: Search
	
	private void fillTableWithNamesFrom(TableView tv, Dataset tp) {
		tv.setEditable(false);
		
		NameClusterManager nameClusterManager = timepointView.getProjectView().getProject().getNameClusterManager();
		
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
			tv.setItems(FXCollections.observableList(tp.getRecognizedNames(timepointView.getProjectView().getProject()).collect(Collectors.toList())));
		} else {
			tv.setItems(FXCollections.emptyObservableList());
		}
		tv.getSortOrder().add(colName);
	}
	
	private void fillTableWithChanges(TableView tv, Dataset tp) {
		tv.setEditable(true);
		tv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		tv.getColumns().clear();

		TableColumn<Change, ChangeType> colChangeType = new TableColumn("Type");
		colChangeType.setCellFactory(ComboBoxTableCell.forTableColumn(
			new ChangeTypeStringConverter(),
			ChangeType.ADDITION,
			ChangeType.DELETION,
			ChangeType.RENAME,			
			ChangeType.LUMP,
			ChangeType.SPLIT
		));
		colChangeType.setCellValueFactory(new PropertyValueFactory<>("type"));
		colChangeType.setPrefWidth(100.0);
		colChangeType.setEditable(true);
		tv.getColumns().add(colChangeType);
		
		TableColumn<Change, ObservableSet<Name>> colChangeFrom = new TableColumn("From");
		colChangeFrom.setCellFactory(TextFieldTableCell.forTableColumn(new NameSetStringConverter()));
		colChangeFrom.setCellValueFactory(new PropertyValueFactory<>("from"));
		colChangeFrom.setPrefWidth(200.0);
		colChangeFrom.setEditable(true);
		tv.getColumns().add(colChangeFrom);
		
		TableColumn<Change, ObservableSet<Name>> colChangeTo = new TableColumn("To");
		colChangeTo.setCellFactory(TextFieldTableCell.forTableColumn(new NameSetStringConverter()));	
		colChangeTo.setCellValueFactory(new PropertyValueFactory<>("to"));
		colChangeTo.setPrefWidth(200.0);
		colChangeTo.setEditable(true);
		tv.getColumns().add(colChangeTo);
		
		ChangeFilter cf = timepointView.getProjectView().getProject().getChangeFilter();
		TableColumn<Change, String> colFiltered = new TableColumn<>("Eliminated by filter?");
		colFiltered.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) -> 
				new ReadOnlyStringWrapper(
					cf.test(features.getValue()) ? "Allowed" : "Eliminated"
				)
		);
		tv.getColumns().add(colFiltered);
		
		TableColumn<Change, String> colGenera = new TableColumn("Genera");
		colGenera.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) ->
				new ReadOnlyStringWrapper(
					String.join(", ", features.getValue().getAllNames().stream().map(n -> n.getGenus()).distinct().sorted().collect(Collectors.toList()))
				)
		);
		tv.getColumns().add(colGenera);
		
		TableColumn<Change, String> colSpecificEpithet = new TableColumn("Specific epithet");
		colSpecificEpithet.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) ->
				new ReadOnlyStringWrapper(
					String.join(", ", features.getValue().getAllNames().stream().map(n -> n.getSpecificEpithet()).filter(s -> s != null).distinct().sorted().collect(Collectors.toList()))
				)
		);
		tv.getColumns().add(colSpecificEpithet);
		
		// TODO: if we can get an ObservableList over tp.getAllChanges(), then this table
		// will update dynamically as changes are made. Won't that be something.
		tv.setItems(FXCollections.observableList(tp.getAllChanges().collect(Collectors.toList())));
		tv.getSortOrder().add(colChangeType);
	}
	
	@FXML
	private void displayAssociatedData(ActionEvent evt) {
		if(Dataset.class.isAssignableFrom(timepoint.getClass())) {
			Dataset dataset = (Dataset) timepoint;
			
			DatasetTabularView datasetView = new DatasetTabularView(dataset);
			datasetView.getStage().show();
		} 
	}
}
