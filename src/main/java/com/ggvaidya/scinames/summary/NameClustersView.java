
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

import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.TaxonConcept;
import com.ggvaidya.scinames.SciNames;
import com.ggvaidya.scinames.model.ChangeType;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import com.ggvaidya.scinames.ui.ProjectView;
import com.ggvaidya.scinames.util.SimplifiedDate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.List;
import java.util.Set;
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
	
	private TableColumn<NameCluster, String> createColumnFromPrecalc(String colName, Table<NameCluster, String, String> precalc) {
		TableColumn<NameCluster, String> col = new TableColumn<>(colName);
		col.setCellValueFactory(cvf -> new ReadOnlyStringWrapper(precalc.get(cvf.getValue(), colName)));
		return col;
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
		
		// Precalculate!
		SciNames.reportMemoryStatus("Started precalculating name clusters");
		Table<NameCluster, String, String> precalc = HashBasedTable.create();
		
		// Set up columns.
		cols.add(createColumnFromPrecalc("name", precalc));
		cols.add(createColumnFromPrecalc("cluster", precalc));
		cols.add(createColumnFromPrecalc("cluster_count", precalc));
		cols.add(createColumnFromPrecalc("datasets", precalc));
		cols.add(createColumnFromPrecalc("dates", precalc));
		
		cols.add(createColumnFromPrecalc("recognized_in_first_dataset", precalc));
		cols.add(createColumnFromPrecalc("recognized_in_last_dataset", precalc));		
		
		Project project = projectView.getProject();
		Dataset firstDataset = project.getFirstDataset().orElse(null);
		Dataset lastDataset = project.getLastDataset().orElse(null);
		
		for(NameCluster cluster: project.getSpeciesNameClusters().collect(Collectors.toList())) {
			precalc.put(cluster, "name", cluster.getName().getFullName());
			
			Set<Name> namesInCluster = cluster.getNames();
			precalc.put(cluster, "cluster", namesInCluster.stream().map(n -> n.getFullName()).collect(Collectors.joining(", ")));
			precalc.put(cluster, "cluster_count", String.valueOf(namesInCluster.size()));
		
			List<Dataset> datasetsInCluster = cluster.getFoundIn().stream().sorted().collect(Collectors.toList());
			precalc.put(cluster, "datasets", datasetsInCluster.stream().map(ds -> ds.toString()).collect(Collectors.joining("; ")));
			
			List<SimplifiedDate> datesInCluster = datasetsInCluster.stream().map(ds -> ds.getDate()).collect(Collectors.toList());
			precalc.put(cluster, "dates", datesInCluster.stream().map(date -> date.toString()).distinct().collect(Collectors.joining("; ")));

			List<TaxonConcept> taxonConceptsInCluster = cluster.getTaxonConcepts(projectView.getProject());
			precalc.put(cluster, "taxon_concepts", taxonConceptsInCluster.stream().map(tc -> tc.toString()).collect(Collectors.joining("; ")));
			precalc.put(cluster, "taxon_concept_count", String.valueOf(taxonConceptsInCluster.size()));
			
			if(firstDataset != null)
				precalc.put(cluster, "recognized_in_first_dataset", cluster.containsAny(project.getRecognizedNames(firstDataset)) ? "yes" : "no");

			if(lastDataset != null)
				precalc.put(cluster, "recognized_in_last_dataset", cluster.containsAny(project.getRecognizedNames(lastDataset)) ? "yes" : "no");
		}
		
		SciNames.reportMemoryStatus("Completed precalculating name clusters");
		
		// Set table items.
		SortedList<NameCluster> sorted = FXCollections.observableArrayList(projectView.getProject().getSpeciesNameClusters().collect(Collectors.toList())).sorted();
		controller.getTableItemsProperty().set(sorted);
		sorted.comparatorProperty().bind(controller.getTableView().comparatorProperty());	
	}
}
