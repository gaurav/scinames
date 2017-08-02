
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
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import com.ggvaidya.scinames.ui.ProjectView;

import java.util.function.Function;
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
	
	private TableColumn<Change, String> createTableColumnForChange(String colName, Function<Change, String> func) {
		TableColumn<Change, String> col = new TableColumn<>(colName);
		col.setCellValueFactory(cvf -> new ReadOnlyStringWrapper(func.apply(cvf.getValue())));
		return col;
	}
	
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
		cols.add(createTableColumnForChange("Type", ch -> ch.getType().toString()));
		cols.add(createTableColumnForChange("From", ch -> ch.getFromString()));
		cols.add(createTableColumnForChange("To", ch -> ch.getToString()));
		cols.add(createTableColumnForChange("Dataset", ch -> ch.getDataset().getName()));
		cols.add(createTableColumnForChange("Date", ch -> ch.getDataset().getDate().asYYYYmmDD("-")));
		cols.add(createTableColumnForChange("Year", ch -> ch.getDataset().getDate().getYearAsString()));
		cols.add(createTableColumnForChange("Note", ch -> ch.noteProperty().get()));
		cols.add(createTableColumnForChange("Names", 
			ch -> {
				return ch.getAllNames().stream().map(n -> n.getFullName()).sorted().collect(Collectors.joining("; "));
			}
		));
		cols.add(createTableColumnForChange("BinomialNames", 
			ch -> {
				return ch.getAllNames().stream().flatMap(n -> n.asBinomial()).map(n -> n.getFullName()).sorted().collect(Collectors.joining("; "));
			}
		));
		cols.add(createTableColumnForChange("OnlyInvolvesSpeciesOrLower", 
			ch -> {
				return ch.getAllNames().stream().allMatch(n -> (n.getBinomialName() != null)) ? "yes" : "no";
			}
		));
		
		controller.getTableItemsProperty().get().addAll(projectView.getProject().getChanges().collect(Collectors.toList()));
	}
}
