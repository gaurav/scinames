
/*
 *
 *  ChangeFiltersView
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

import com.ggvaidya.scinames.model.filters.ChangeFilter;
import com.ggvaidya.scinames.project.ProjectView;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import java.util.LinkedList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * The ChangeFiltersView displays all the filters active on the current project.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class ChangeFiltersView {
	private Stage stage;
	private Scene scene;
	private ProjectView projectView;
	private TabularDataViewController controller;
	
	public Stage getStage() { return stage; }

	public ChangeFiltersView(ProjectView pv) {
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
		stage.setTitle("Currently active filters");
		
		// Setup headertext.
		controller.getHeaderTextEditableProperty().set(false);
		controller.getHeaderTextProperty().set("The following filters are currently active.");		
		
		// Setup table.
		controller.getTableEditableProperty().set(false);
		controller.setTableColumnResizeProperty(TableView.CONSTRAINED_RESIZE_POLICY);
		ObservableList<TableColumn> cols = controller.getTableColumnsProperty();
		cols.clear();
		
		// Set up columns.
		TableColumn<ChangeFilter, String> colFilterDesc = new TableColumn("Filter");
		colFilterDesc.setCellValueFactory(new PropertyValueFactory<>("Description"));
		colFilterDesc.setPrefWidth(40.0);
		cols.add(colFilterDesc);

		TableColumn<ChangeFilter, String> colCounts = new TableColumn("Count");
		colCounts.setCellValueFactory(new PropertyValueFactory<>("ChangesFilteredCount"));
		colCounts.setPrefWidth(20.0);
		cols.add(colCounts);
		
		TableColumn<ChangeFilter, String> colBreakdown = new TableColumn("Breakdown");
		colBreakdown.setCellValueFactory(new PropertyValueFactory<>("ChangesFilteredByType"));
		colBreakdown.setEditable(true);
		colBreakdown.setPrefWidth(100.0);
		cols.add(colBreakdown);
		
		// Set table items.
		List<ChangeFilter> listChangeFilters = new LinkedList<>();
		ChangeFilter cf = projectView.getProject().getChangeFilter();
		while(cf != null) {
			listChangeFilters.add(cf);
			cf = cf.getPrevChangeFilter();
		}
		controller.getTableItemsProperty().set(
			FXCollections.observableArrayList(listChangeFilters)
		);
	}
}
