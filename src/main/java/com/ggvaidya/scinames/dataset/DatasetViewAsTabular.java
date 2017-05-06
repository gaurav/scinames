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
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.ChangeType;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.change.ChangeTypeStringConverter;
import com.ggvaidya.scinames.model.change.NameSetStringConverter;
import com.ggvaidya.scinames.project.ProjectView;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;

/**
 * Starts the TimepointScene for a given Timepoint.
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetViewAsTabular {
	private ProjectView projectView;
	private Dataset timepoint;
	private Stage stage;
	private Scene scene;
	private TabularDataViewController controller;

	public DatasetViewAsTabular(ProjectView pv, Dataset tp) {
		projectView = pv;
		timepoint = tp;
		stage = new Stage();
		
		// Set up tabular data view.
		controller = TabularDataViewController.createTabularDataView();
		scene = controller.getScene();
		
		init();
		
		stage.setTitle("Timepoint: " + timepoint);
		stage.setScene(scene);
	}
	
	private void init() {
		@SuppressWarnings("unchecked")
		TableView<Change> tv = controller.getTableView();
		
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
			ChangeType.SPLIT
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
		
		ChangeFilter cf = projectView.getProject().getChangeFilter();
		TableColumn<Change, String> colFiltered = new TableColumn<>("Eliminated by filter?");
		colFiltered.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) -> 
				new ReadOnlyStringWrapper(
					cf.test(features.getValue()) ? "Allowed" : "Eliminated"
				)
		);
		tv.getColumns().add(colFiltered);
		
		TableColumn<Change, String> colGenera = new TableColumn<>("Genera");
		colGenera.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) ->
				new ReadOnlyStringWrapper(
					String.join(", ", features.getValue().getAllNames().stream().map(n -> n.getGenus()).distinct().sorted().collect(Collectors.toList()))
				)
		);
		tv.getColumns().add(colGenera);
		
		TableColumn<Change, String> colSpecificEpithet = new TableColumn<>("Specific epithet");
		colSpecificEpithet.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) ->
				new ReadOnlyStringWrapper(
					String.join(", ", features.getValue().getAllNames().stream().map(n -> n.getSpecificEpithet()).filter(s -> s != null).distinct().sorted().collect(Collectors.toList()))
				)
		);
		tv.getColumns().add(colSpecificEpithet);
		
		// TODO: if we can get an ObservableList over tp.getAllChanges(), then this table
		// will update dynamically as changes are made. Won't that be something.
		
		// Yes, we'd like to get all changes, so we can see which ones have been filtered out.
		tv.setItems(FXCollections.observableList(timepoint.getAllChanges().collect(Collectors.toList())));
		tv.getSortOrder().add(colChangeType);
	}

	public Stage getStage() {
		return stage;
	}
	
	public Dataset getDataset() {
		return timepoint;
	}
	
	public ProjectView getProjectView() {
		return projectView;
	}
}
