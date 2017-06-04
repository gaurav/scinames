
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

import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.filters.ChangeFilter;
import com.ggvaidya.scinames.model.ChangeType;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import com.ggvaidya.scinames.ui.ProjectView;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
		cols.add(createTableColumnForDataset("type", dataset -> dataset.isChecklist() ? "Checklist" : "Dataset"));		
		cols.add(createTableColumnForDataset("binomial_count", 
			dataset -> String.valueOf(
				dataset.getRecognizedNames(projectView.getProject())
					.filter(n -> n.hasSpecificEpithet())
					.flatMap(n -> n.asBinomial())
					.distinct()
					.count()
			)
		));
		
		Project project = projectView.getProject();
		ChangeFilter cf = project.getChangeFilter();
		cols.add(createTableColumnForDataset("count_changes_filtered", ds -> String.valueOf(ds.getAllChanges().filter(ch -> !cf.test(ch)).count())));
		cols.add(createTableColumnForDataset("count_changes_explicit", ds -> String.valueOf(ds.getChanges(project).filter(ch -> !ds.isChangeImplicit(ch)).count())));
		cols.add(createTableColumnForDataset("count_changes_implicit", ds -> String.valueOf(ds.getChanges(project).filter(ch -> ds.isChangeImplicit(ch)).count())));
		
		Stream<ChangeType> changeTypes = projectView.getProject().getChanges().map(ch -> ch.getType()).distinct().sorted();
		changeTypes.collect(Collectors.toList()).forEach(chType -> {
			cols.add(
				createTableColumnForDataset(
					"count_implicit_" + chType.getType(), 
					ds -> String.valueOf(
						ds.getChanges(projectView.getProject())
							.filter(ch -> ds.isChangeImplicit(ch) && ch.getType().equals(chType))
							.count()
					)
				)
			);
			
			cols.add(
					createTableColumnForDataset(
						"count_explicit_" + chType.getType(), 
						ds -> String.valueOf(
							ds.getChanges(projectView.getProject())
								.filter(ch -> !ds.isChangeImplicit(ch) && ch.getType().equals(chType))
								.count()
						)
					)
				);
		});
		
		// Set table items.
		List<Dataset> timepoints = projectView.getProject().getDatasets();
		controller.getTableItemsProperty().set(FXCollections.observableList(timepoints));
	}
}
