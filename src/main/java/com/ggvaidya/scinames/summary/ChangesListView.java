
/*
 *
 *  ChangesListView
 *  Copyright (C) 2017 Gaurav Vaidya
 *
 *  This file is part of SciNames.
 *
 *  SciNames is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  SciNames is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with SciNames.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.ggvaidya.scinames.summary;

import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.NameClusterManager;
import com.ggvaidya.scinames.model.ChangeType;
import com.ggvaidya.scinames.project.ProjectView;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * A ChangesListView displays all the changes listed within a project.
 * It uses the TabularDataView to do this.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class ChangesListView {
	private Stage stage;
	private Scene scene;
	private ProjectView projectView;
	private TabularDataViewController controller;
	
	public Stage getStage() { return stage; }

	public ChangesListView(ProjectView pv) {
		projectView = pv;
		stage = new Stage();
		
		controller = TabularDataViewController.createTabularDataView();
		scene = controller.getScene();
		
		init();
		stage.setScene(scene);
		
		// Go go stagey scene.
		stage = new Stage();
		stage.setScene(scene);
	}
	
	private StringProperty headerText;
	
	public void init() {
		// Setup stage.
		stage.setTitle("Changes");
		
		// Setup headertext.
		controller.getHeaderTextEditableProperty().set(true);
		headerText = controller.getHeaderTextProperty();
		if(headerText.get().equals("")) {
			headerText.set("all");
		}
		controller.getHeaderTextProperty().addListener((c, a, b) -> { init(); });		
		
		// What are we filtering to?
		ChangeType filterChangeType = (headerText.get().equals("all") ? null : ChangeType.of(headerText.get()));
		
		// Load up name cluster manager.
		NameClusterManager nameClusterManager = projectView.getProject().getNameClusterManager();
		
		// Setup table.
		controller.getTableEditableProperty().set(false);
		//controller.setTableColumnResizeProperty(TableView.CONSTRAINED_RESIZE_POLICY);
		ObservableList<TableColumn> cols = controller.getTableColumnsProperty();
		cols.clear();
		
		// Set up columns.
		TableColumn<Change, String> colChangeType = new TableColumn("Type");
		colChangeType.setCellValueFactory(new PropertyValueFactory<>("type"));
		//colChangeType.setSortType(TableColumn.SortType.ASCENDING);
		colChangeType.setPrefWidth(40.0);
		cols.add(colChangeType);
		
		TableColumn<Change, String> colChangeFrom = new TableColumn("From");
		colChangeFrom.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) -> 
				new ReadOnlyStringWrapper(
					nameClusterManager.getClusters(
						features.getValue().getFrom()
					).toString()
				)
		);
		colChangeFrom.setPrefWidth(200.0);
		cols.add(colChangeFrom);
		
		TableColumn<Change, String> colChangeTo = new TableColumn("To");
		colChangeTo.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) -> 
				new ReadOnlyStringWrapper(
					nameClusterManager.getClusters(
						features.getValue().getTo()
					).toString()
				)
		);
		colChangeTo.setPrefWidth(200.0);
		cols.add(colChangeTo);
		
		TableColumn<Change, String> colDate = new TableColumn("Date");
		colDate.setCellValueFactory(
			(TableColumn.CellDataFeatures<Change, String> features) -> 
				new ReadOnlyStringWrapper(
					features.getValue().getDataset().getName() + " (" + 
					features.getValue().getDataset().getDate().toString() + ")"
				)
		);
		colDate.setPrefWidth(100.0);
		cols.add(colDate);
		
		/*
		for(Change.Type type: FXCollections.observableArrayList(projectView.getProject().changeTypesProperty()).sorted()) {
			TableColumn<NameCluster, String> colChangesByType = new TableColumn(type.getType());
			colChangesByType.setCellValueFactory((TableColumn.CellDataFeatures<NameCluster, String> features) -> {
				NameCluster cluster = features.getValue();
				long numberOfChanges = projectView.getProject().getDatasets().stream()
					.flatMap(t -> t.getAllChanges(type))
					.filter(c -> cluster.containsAny(c.getReferencedNames()))
					.count();
				return new ReadOnlyStringWrapper(String.valueOf(numberOfChanges));
			});
			colChangesByType.setPrefWidth(50.0);
			cols.add(colChangesByType);
		}
		*/
		
		// Set table items.
		SortedList<Change> sorted;
		if(filterChangeType == null)
			sorted = FXCollections.observableArrayList(
				projectView.getProject().getChanges().collect(Collectors.toList())
			).sorted();
		else
			sorted = FXCollections.observableArrayList(
				projectView.getProject().getChanges().filter(ch -> ch.getType().equals(filterChangeType)).collect(Collectors.toList())
			).sorted();
		
		controller.getTableItemsProperty().set(sorted);
		sorted.comparatorProperty().bind(controller.getTableView().comparatorProperty());
	}
}
