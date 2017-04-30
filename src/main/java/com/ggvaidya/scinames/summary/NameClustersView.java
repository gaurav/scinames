
/*
 *
 *  NameClustersView
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
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.project.ProjectView;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * A NameClustersView displays the name clusters used within a project.
 * It uses the TabularDataView to do this.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class NameClustersView {
	private Stage stage;
	private Scene scene;
	private ProjectView projectView;
	private TabularDataViewController controller;
	
	public Stage getStage() { return stage; }

	public NameClustersView(ProjectView pv) {
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
	
	public void init() {
		// Setup stage.
		stage.setTitle("Name clusters");
		
		// Setup headertext.
		controller.getHeaderTextProperty().set("All name clusters used in this project:");
		controller.getHeaderTextEditableProperty().set(false);
		
		// Setup table.
		controller.getTableEditableProperty().set(false);
		//controller.setTableColumnResizeProperty(TableView.CONSTRAINED_RESIZE_POLICY);
		ObservableList<TableColumn> cols = controller.getTableColumnsProperty();
		cols.clear();
		
		// Set up columns.
		TableColumn<NameCluster, String> colClusterName = new TableColumn("Name");
		colClusterName.setCellValueFactory(new PropertyValueFactory<>("name"));
		colClusterName.setSortType(TableColumn.SortType.ASCENDING);
		colClusterName.setPrefWidth(100.0);
		cols.add(colClusterName);
		
		TableColumn<NameCluster, String> colClusterList = new TableColumn("Cluster");
		colClusterList.setCellValueFactory(new PropertyValueFactory<>("names"));
		colClusterList.setPrefWidth(200.0);
		cols.add(colClusterList);
		
		TableColumn<NameCluster, String> colDateRange = new TableColumn("Dates");
		colDateRange.setCellValueFactory((TableColumn.CellDataFeatures<NameCluster, String> features) -> new ReadOnlyStringWrapper(features.getValue().getDateRange())
		);
		colClusterList.setPrefWidth(100.0);
		cols.add(colDateRange);
		
		TableColumn<NameCluster, String> colTaxonConcepts = new TableColumn("Taxon Concepts");
		colTaxonConcepts.setCellValueFactory(new PropertyValueFactory<>("TaxonConcepts"));
		colTaxonConcepts.setPrefWidth(200.0);
		cols.add(colTaxonConcepts);
		
		for(Change.Type type: FXCollections.observableArrayList(projectView.getProject().changeTypesProperty()).sorted()) {
			TableColumn<NameCluster, String> colChangesByType = new TableColumn(type.getType());
			colChangesByType.setCellValueFactory((TableColumn.CellDataFeatures<NameCluster, String> features) -> {
				NameCluster cluster = features.getValue();
				long numberOfChanges = projectView.getProject().getDatasets().stream()
					.flatMap(t -> t.getChanges(projectView.getProject()).filter(ch -> ch.getType().equals(type)))
					.filter(c -> cluster.containsAny(c.getAllNames()))
					.count();
				return new ReadOnlyStringWrapper(String.valueOf(numberOfChanges));
			});
			colChangesByType.setPrefWidth(50.0);
			cols.add(colChangesByType);
		}
		
		// Set table items.
		SortedList<NameCluster> sorted = FXCollections.observableArrayList(projectView.getProject().getSpeciesNameClusters().collect(Collectors.toList())).sorted();
		controller.getTableItemsProperty().set(sorted);
		sorted.comparatorProperty().bind(controller.getTableView().comparatorProperty());	
	}
}
