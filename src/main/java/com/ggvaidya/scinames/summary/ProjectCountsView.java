
/*
 *
 *  ProjectCountsView
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
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.project.ProjectView;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import java.util.List;
import java.util.stream.Stream;
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
 * A ProjectCountsView displays all the datasets in a project.
 * It uses the TabularDataView to do this, so that values are
 * cleanly split up and can be exported.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class ProjectCountsView {
	private Stage stage;
	private Scene scene;
	private ProjectView projectView;
	private TabularDataViewController controller;
	
	public Stage getStage() { return stage; }

	public ProjectCountsView(ProjectView pv) {
		projectView = pv;
		stage = new Stage();
		
		controller = TabularDataViewController.createTabularDataView();
		scene = controller.getScene();
		init();
		
		// Go go stagey scene.
		stage.setScene(scene);
	}
	
	private TableColumn<Dataset, String> createTableColumnForObservable(String colName, Callback<TableColumn.CellDataFeatures<Dataset,String>,ObservableValue<String>> valueFunc) {
		TableColumn<Dataset, String> col = new TableColumn<>(colName);
		col.setCellValueFactory(valueFunc);
		col.setPrefWidth(100);
		return col;
	}
	
	private TableColumn<Dataset, String> createTableColumnForDataset(String colName, Callback<Dataset, String> valueFunc) {
		TableColumn<Dataset, String> col = new TableColumn<>(colName);
		col.setCellValueFactory((param) -> new ReadOnlyStringWrapper(valueFunc.call(param.getValue())));
		col.setPrefWidth(100);
		return col;
	}
	
	public void init() {
		// Setup stage.
		stage.setTitle("Project counts");
		
		// Setup table.
		controller.getTableEditableProperty().set(false);
		//controller.setTableColumnResizeProperty(TableView.CONSTRAINED_RESIZE_POLICY);
		ObservableList<TableColumn> cols = controller.getTableColumnsProperty();
		cols.clear();
		
		// Set up columns.
		cols.add(createTableColumnForObservable("dataset", new PropertyValueFactory<>("name")));
		cols.add(createTableColumnForObservable("date", (new PropertyValueFactory<>("date"))));
		cols.add(createTableColumnForDataset("year", dataset -> dataset.getDate().getYearAsString()));
		cols.add(createTableColumnForDataset("binomial_count", 
			dataset -> String.valueOf(
				dataset.getRecognizedNames(projectView.getProject())
					.filter(n -> n.hasSpecificEpithet())
					.map(n -> n.getBinomialName())
					.distinct()
					.count()
			)
		));
		Stream<Change.Type> changes = projectView.getProject().getChanges().map(ch -> ch.getType()).distinct().sorted();
		changes.forEach(chType -> 
			cols.add(
				createTableColumnForDataset(
					"count_" + chType.getType(), 
					ds -> String.valueOf(ds.getChanges(projectView.getProject()).filter(ch -> ch.getType().equals(chType)).count())
				)
			)
		);
		
		// Set table items.
		List<Dataset> timepoints = projectView.getProject().getDatasets();
		controller.getTableItemsProperty().set(FXCollections.observableList(timepoints));
	}
}
