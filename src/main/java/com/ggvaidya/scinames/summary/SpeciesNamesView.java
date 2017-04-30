
/*
 *
 *  SpeciesNamesView
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

import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.project.ProjectView;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * A SpeciesNamesView displays the species names used within a project.
 * It can summarize the results by change counts or by checklist.
 * 
 * It uses the TabularDataView to do this.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class SpeciesNamesView {
	private Stage stage;
	private Scene scene;
	private ProjectView projectView;
	private TabularDataViewController controller;
	
	public Stage getStage() { return stage; }

	public SpeciesNamesView(ProjectView pv) {
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
		stage.setTitle("Species names");
		
		// Setup headertext.
		controller.getHeaderTextProperty().set("All species used in this project:");
		controller.getHeaderTextEditableProperty().set(false);
		
		// Setup table.
		controller.getTableEditableProperty().set(false);
		// controller.setTableColumnResizeProperty(TableView.CONSTRAINED_RESIZE_POLICY);
		ObservableList<TableColumn> cols = controller.getTableColumnsProperty();
		cols.clear();
		
		// Set up columns.
		TableColumn<Name, String> colPropertyName = new TableColumn("Species name");
		colPropertyName.setCellValueFactory(new PropertyValueFactory<>("binomialName"));
		colPropertyName.setSortType(TableColumn.SortType.ASCENDING);
		colPropertyName.setPrefWidth(40.0);
		cols.add(colPropertyName);
		
		// One column per timepoint.
		ObservableMap<Name, List<Dataset>> timepointsByName = projectView.getProject().timepointsByNameProperty();
		for(Dataset tp: projectView.getProject().datasetsProperty()) {
			TableColumn<Name, String> colTimepoint = new TableColumn(tp.getName());
			colTimepoint.setCellValueFactory((TableColumn.CellDataFeatures<Name, String> features) -> {
				Name n = features.getValue();
				return new ReadOnlyStringWrapper((timepointsByName.get(n).contains(tp) ? "YES" : ""));
			});
			colTimepoint.setPrefWidth(50.0);
			cols.add(colTimepoint);
		}
		
		// Set table items.
		controller.getTableItemsProperty().set(
			projectView.getProject().getBinomialNamesAsList().sorted()
		);
	}
}
