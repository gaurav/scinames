
/*
 *
 *  TaxonConceptsView
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
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.TaxonConcept;
import com.ggvaidya.scinames.project.ProjectView;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * A TaxonConceptsView displays all the taxon concepts in this dataset.
 * The way we do this is by assuming that any taxon cluster can be divided
 * into a 
 * It uses the TabularDataView to do this, so that values are
 * cleanly split up and can be exported.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class TaxonConceptsView {
	private Stage stage;
	private Scene scene;
	private ProjectView projectView;
	private TabularDataViewController controller;
	
	public Stage getStage() { return stage; }

	public TaxonConceptsView(ProjectView pv) {
		projectView = pv;
		stage = new Stage();
		
		controller = TabularDataViewController.createTabularDataView();
		scene = controller.getScene();
		init();
		
		// Go go stagey scene.
		stage.setScene(scene);
	}
	
	private TableColumn<NameCluster, String> createTableColumnForObservable(String colName, Callback<TableColumn.CellDataFeatures<NameCluster,String>,ObservableValue<String>> valueFunc) {
		TableColumn<NameCluster, String> col = new TableColumn<>(colName);
		col.setCellValueFactory(valueFunc);
		col.setPrefWidth(100);
		return col;
	}
	
	private TableColumn<TaxonConcept, String> createTableColumnForTaxonConcept(String colName, Callback<TaxonConcept, String> valueFunc) {
		TableColumn<TaxonConcept, String> col = new TableColumn<>(colName);
		col.setCellValueFactory((param) -> new ReadOnlyStringWrapper(valueFunc.call(param.getValue())));
		col.setPrefWidth(100);
		return col;
	}
	
	public void init() {
		// Setup stage.
		stage.setTitle("Taxon concepts via name clusters");
		
		// Setup table.
		controller.getTableEditableProperty().set(false);
		//controller.setTableColumnResizeProperty(TableView.CONSTRAINED_RESIZE_POLICY);
		ObservableList<TableColumn> cols = controller.getTableColumnsProperty();
		cols.clear();
		
		// Set table items.
		List<TaxonConcept> taxonConcepts = projectView.getProject().getSpeciesNameClusters()
			.flatMap(nc -> nc.getTaxonConcepts(projectView.getProject()).stream())
			.filter(nc -> nc.getName().hasSpecificEpithet())
			.distinct().collect(Collectors.toList());
		controller.getTableItemsProperty().set(FXCollections.observableList(taxonConcepts));
		
		// Set up columns.
		cols.add(createTableColumnForObservable("id", new PropertyValueFactory<>("id")));		
		cols.add(createTableColumnForObservable("name", new PropertyValueFactory<>("name")));
		cols.add(createTableColumnForTaxonConcept("names",
			tc -> tc.getNames().stream().map(n -> n.getFullName()).sorted().collect(Collectors.joining("; "))
		));
		cols.add(createTableColumnForTaxonConcept("name_cluster_id", 
			tc -> tc.getNameCluster().getId().toString()
		));
		cols.add(createTableColumnForObservable("date_range", (new PropertyValueFactory<>("dateRange"))));
		cols.add(createTableColumnForTaxonConcept("datasets", 
			cluster -> cluster.getFoundInSorted().stream().map(d -> d.getName()).collect(Collectors.joining(", "))
		));
		cols.add(createTableColumnForTaxonConcept("first_added_dataset", 
			cluster -> {
				List<Dataset> foundInSorted = cluster.getFoundInSorted();
				return foundInSorted.isEmpty() ? "NA" : foundInSorted.get(0).getName();
			}
		));
		cols.add(createTableColumnForTaxonConcept("last_recognized_dataset", 
			cluster -> {
				List<Dataset> foundInSorted = cluster.getFoundInSorted();
				return foundInSorted.isEmpty() ? "NA" : foundInSorted.get(foundInSorted.size() - 1).getName();
			}
		));
		cols.add(createTableColumnForTaxonConcept("dataset_years", 
			cluster -> cluster.getFoundInSorted().stream().map(d -> d.getDate().getYearAsString()).collect(Collectors.joining(", "))
		));
		cols.add(createTableColumnForTaxonConcept("first_added_year", 
			cluster -> {
				List<Dataset> foundInSorted = cluster.getFoundInSorted();
				return foundInSorted.isEmpty() ? "NA" : foundInSorted.get(0).getDate().getYearAsString();
			}
		));
		cols.add(createTableColumnForTaxonConcept("last_recognized_year", 
			cluster -> {
				List<Dataset> foundInSorted = cluster.getFoundInSorted();
				return foundInSorted.isEmpty() ? "NA" : foundInSorted.get(foundInSorted.size() - 1).getDate().getYearAsString();
			}
		));
		cols.add(createTableColumnForTaxonConcept("starts_with", 
			cluster -> {
				List<Change> startsWith = cluster.getStartsWith();
				if(startsWith == null)
					return "NA";
				else
					return startsWith.stream().map(ch -> ch.toString()).collect(Collectors.joining("; "));
			}
		));
		cols.add(createTableColumnForTaxonConcept("ends_with", 
			cluster -> {
				List<Change> endsWith = cluster.getEndsWith();
				if(endsWith == null)
					return "NA";
				else
					return endsWith.stream().map(ch -> ch.toString()).collect(Collectors.joining("; "));
			}
		));
		
		// Figure out if it's polytypic or not.
		cols.add(createTableColumnForTaxonConcept("is_polytypic",
			cluster -> cluster.isPolytypic(projectView.getProject()) ? "yes" : "no"
		));
		
		// Is it ongoing?
		cols.add(createTableColumnForTaxonConcept("is_ongoing", 
			cluster -> cluster.isOngoing(projectView.getProject()) ? "yes" : "no"
		));
		
		// Is it currently recognized?
		// Is it ongoing?
		cols.add(createTableColumnForTaxonConcept("recognized_in_last_dataset", 
			cluster -> {
				Optional<Dataset> opLast = projectView.getProject().getLastDataset();
				if(opLast.isPresent()) {
					Dataset last = opLast.get();
					
					if(cluster.containsAny(projectView.getProject().getRecognizedNames(last)))
						return "yes";
					else
						return "no";
				} else
					return "NA";
			}
		));
	}
}
