
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.NameClusterManager;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import com.ggvaidya.scinames.ui.ProjectView;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * A DatasetSimilarityView displays similarity between timepoints.
 * It uses the TabularDataView to do this.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class DatasetSimilarityView {
	private Stage stage;
	private Scene scene;
	private ProjectView projectView;
	private TabularDataViewController controller;
	
	public Stage getStage() { return stage; }

	public DatasetSimilarityView(ProjectView pv) {
		projectView = pv;
		stage = new Stage();
		
		controller = TabularDataViewController.createTabularDataView();
		scene = controller.getScene();
		init();
		
		// Go go stagey scene.
		stage.setScene(scene);
	}
	
	public void init() {
		// Setup stage.
		stage.setTitle("Timepoint similarity");
		
		// Setup table.
		controller.getTableEditableProperty().set(false);
		//controller.setTableColumnResizeProperty(TableView.CONSTRAINED_RESIZE_POLICY);
		ObservableList<TableColumn> cols = controller.getTableColumnsProperty();
		cols.clear();
		
		// Set up columns.
		TableColumn<Dataset, String> colTimepointName = new TableColumn<>("Timepoint");
		colTimepointName.setCellValueFactory(new PropertyValueFactory<>("name"));
		colTimepointName.setPrefWidth(100.0);
		cols.add(colTimepointName);
		
		// Precalculating.
		double lowest = 100.0;
		Dataset tpLowest1 = null;
		Dataset tpLowest2 = null;		
		projectView.startProgressBar();
		System.err.println("Starting precalculating.");
		Table<Dataset, Dataset, String> data = HashBasedTable.create();
		for(Dataset tp: projectView.getProject().getDatasets()) {
			for(Dataset colTP: projectView.getProject().getDatasets()) {
				if(data.contains(tp, colTP))
					continue;
				
				NameClusterManager manager = projectView.getProject().getNameClusterManager();
				Set<NameCluster> leftTP = tp.getRecognizedNames(projectView.getProject()).map(n -> manager.getCluster(n).get()).collect(Collectors.toSet());
				Set<NameCluster> rightTP = colTP.getRecognizedNames(projectView.getProject()).map(n -> manager.getCluster(n).get()).collect(Collectors.toSet());
				
				// Overlapping name concepts.
				Sets.SetView<NameCluster> union = Sets.union(leftTP, rightTP);
				Sets.SetView<NameCluster> intersection = Sets.intersection(leftTP, rightTP);
				
				double res = (((double)intersection.size()) / union.size() * 100);
				
				if(lowest > res) {
					lowest = res;
					tpLowest1 = tp;
					tpLowest2 = colTP;
				}
				
				String result = new BigDecimal(res).setScale(2, RoundingMode.DOWN).toPlainString() + "% (" + intersection.size() + " identical out of " + union.size() + ")";
				
				data.put(tp, colTP, result);
				data.put(colTP, tp, result);
			}
		}
		System.err.println("Precalculating done.");
		projectView.stopProgressBar();
		
		// Setup headertext.
		String str_lowest = "";
		if(tpLowest1 != null && tpLowest2 != null) {
			str_lowest = " (lowest: " + 
				new BigDecimal(lowest).setScale(2, RoundingMode.DOWN).toPlainString() + 
				"% between " + tpLowest1.getName() + " and " + tpLowest2.getName() +
				")";
		}
		controller.getHeaderTextProperty().set("How similar is each timepoint to every other?" + str_lowest);
		controller.getHeaderTextEditableProperty().set(false);
		
		// Create a column for every timepoint here.
		projectView.getProject().getDatasets().forEach((Dataset colTP) -> {
			TableColumn<Dataset, String> colTimepoint = new TableColumn<>(colTP.getName());
			colTimepoint.setCellValueFactory((TableColumn.CellDataFeatures<Dataset, String> features) -> {
				Dataset tp = features.getValue();
				
				return new ReadOnlyStringWrapper(data.get(tp, colTP));
			});
			colTimepoint.setPrefWidth(100.0);
			cols.add(colTimepoint);
		});
		
		// Set table items.
		List<Dataset> timepoints = projectView.getProject().getDatasets();
		controller.getTableItemsProperty().set(FXCollections.observableList(timepoints));
	}
}
