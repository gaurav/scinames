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
package com.ggvaidya.scinames.ui;

import com.ggvaidya.scinames.SciNames;
import com.ggvaidya.scinames.project.ProjectView;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import java.util.Map;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

/**
 * Displays the system and project preferences in an editable tabular view.
 * 
 * This is also how the testbed for the editable tabular view: we should be
 * able to drive and operate that table entirely from this class.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class PreferencesView {
	private final ProjectView projectView;
	private final Stage stage;
	private final Scene scene;
	private final TabularDataViewController controller;
	
	public Stage getStage() { return stage; }	
	
	public PreferencesView(ProjectView view) {
		projectView = view;
		stage = new Stage();
		
		controller = TabularDataViewController.createTabularDataView();
		scene = controller.getScene();
		
		init();
		stage.setScene(scene);
	}
	
	private void init() {
		// Setup stage.
		stage.setTitle("Preferences");
		
		// Setup headertext.
		controller.getHeaderTextProperty().set("You may modify any of these preferences:");
		controller.getHeaderTextEditableProperty().set(false);
		
		// Setup table.
		controller.getTableEditableProperty().set(true);
		controller.setTableColumnResizeProperty(TableView.CONSTRAINED_RESIZE_POLICY);
		ObservableList<TableColumn> cols = controller.getTableColumnsProperty();
		cols.clear();
		
		// Set up columns.
		TableColumn<Map.Entry<String, String>, String> colPropertyName = new TableColumn("Property");
		colPropertyName.setCellValueFactory((TableColumn.CellDataFeatures<Map.Entry<String, String>, String> features) -> {
			Map.Entry value = features.getValue();
			return new ReadOnlyStringWrapper(value.getKey().toString());
		});
		colPropertyName.setSortType(TableColumn.SortType.ASCENDING);
		colPropertyName.setPrefWidth(40.0);
		cols.add(colPropertyName);
		
		TableColumn<Map.Entry<String, String>, String> colValue = new TableColumn("Value");
		// colValue.setCellFactory(TextFieldTableCell.forTableColumn());
		colValue.setCellValueFactory((TableColumn.CellDataFeatures<Map.Entry<String, String>, String> features) -> {
			Map.Entry value = features.getValue();
			return new ReadOnlyStringWrapper(value.getValue().toString());
		});
		colValue.setPrefWidth(400.0);
		cols.add(colValue);
		
		// Set table items.
		controller.getTableItemsProperty().set(FXCollections.observableList(SciNames.getPropertiesAsList()));
	}

}
