
/*
 *
 *  LumpsAndSplitsView
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

import java.util.List;
import java.util.stream.Collectors;

import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.ChangeType;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import com.ggvaidya.scinames.ui.ProjectView;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.stage.Stage;

/**
 * A LumpsAndSplitsView displays all the lumps and splits within a project.
 * It uses the TabularDataView to do this, and this functionality will
 * probably be merged in with ChangesListView eventually.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class LumpsAndSplitsView {
	private Stage stage;
	private Scene scene;
	private ProjectView projectView;
	private TabularDataViewController controller;
	private StringProperty headerText;	
	
	public Stage getStage() { return stage; }

	public LumpsAndSplitsView(ProjectView pv) {
		projectView = pv;
		stage = new Stage();
		
		controller = TabularDataViewController.createTabularDataView();
		scene = controller.getScene();
		
		stage = new Stage();
		init();
		stage.setScene(scene);
		
		// Go go stagey scene.
		stage.setScene(scene);
	}
	
	/*
	private TableColumn<Change, String> createTableColumnForChange(String colName, double prefWidth, Callback<Change, String> valueFunc) {
		TableColumn<Change, String> col = new TableColumn<>(colName);
		col.setCellValueFactory((param) -> new ReadOnlyStringWrapper(valueFunc.call(param.getValue())));
		col.setPrefWidth(prefWidth);
		return col;
	}*/
	
	private TableColumn<Change, String> createTableColumnForTable(String colName, double prefWidth, Table<Change, String, String> precalc) {
		TableColumn<Change, String> col = new TableColumn<>(colName);
		col.setCellValueFactory((param) -> new ReadOnlyStringWrapper(precalc.get(param.getValue(), colName)));
		col.setPrefWidth(prefWidth);
		return col;
	}
	
	public void init() {
		// Setup stage.
		stage.setTitle("Lumps and splits");
		
		// Setup headertext.
		controller.getHeaderTextEditableProperty().set(false);
		headerText = controller.getHeaderTextProperty();
		//if(headerText.get().equals("")) {
			headerText.set("Display all lumps and splits.");
		//}
		controller.getHeaderTextProperty().addListener((c, a, b) -> { init(); });		
		
		// Load up name cluster manager.
		// NameClusterManager nameClusterManager = projectView.getProject().getNameClusterManager();
		
		// Setup table.
		controller.getTableEditableProperty().set(false);
		//controller.setTableColumnResizeProperty(TableView.CONSTRAINED_RESIZE_POLICY);
		ObservableList<TableColumn> cols = controller.getTableColumnsProperty();
		cols.clear();
		
		// Set table items.
		List<Change> changes = projectView.getProject().getChanges()
				.filter(c -> c.getType().equals(ChangeType.LUMP) || c.getType().equals(ChangeType.SPLIT))
				.collect(Collectors.toList());
		SortedList<Change> sorted = FXCollections.observableArrayList(changes).sorted();
		
		controller.getTableItemsProperty().set(sorted);
		sorted.comparatorProperty().bind(controller.getTableView().comparatorProperty());
		
		// Precalculate.
		Project project = projectView.getProject();		
		Table<Change, String, String> precalc = HashBasedTable.create();
		for(Change change: changes) {
			precalc.put(change, "id", change.getId().toString());
			precalc.put(change, "type", change.getType().toString());
			precalc.put(change, "from", change.getFromStream().map(n -> n.getFullName()).collect(Collectors.joining(", ")));
			precalc.put(change, "to", change.getToStream().map(n -> n.getFullName()).collect(Collectors.joining(", ")));
			precalc.put(change, "dataset", change.getDataset().getName() + " (" + change.getDataset().getDate().toString() + ")");
			precalc.put(change, "year", change.getDataset().getDate().getYearAsString());
			precalc.put(change, "change", change.toString());
			precalc.put(change, "reversions", project.getChangesReversing(change)
				.map(ch -> ch.toString())
				.collect(Collectors.joining("; ")));
			precalc.put(change, "reversion_count", String.valueOf(project.getChangesReversing(change).count()));
			precalc.put(change, "reverts_a_previous_change", 
				(project.getChangesReversing(change)
					// Did any change take place before this change?
					.anyMatch(ch -> ch.getDataset().getDate().compareTo(change.getDataset().getDate()) < 0)
				) ? "yes" : "no"		
			);
			precalc.put(change, "reverts_a_later_change", 
				(project.getChangesReversing(change)
					// Did any change take place before this change?
					.anyMatch(ch -> ch.getDataset().getDate().compareTo(change.getDataset().getDate()) > 0)
				) ? "yes" : "no"
			);	
			
			// TODO: broken! This returns 'yes' when changes are empty.			
			precalc.put(change, "reverts_all_previous_changes", 
				(project.getChangesReversing(change)
					// Did every change take place before this change?
					.allMatch(ch -> ch.getDataset().getDate().compareTo(change.getDataset().getDate()) < 0)
				) ? "yes" : "no"
			);
			
			precalc.put(change, "perfect_reversions", project.getChangesPerfectlyReversing(change)
				.map(ch -> ch.toString())
				.collect(Collectors.joining("; ")));
			precalc.put(change, "perfect_reversions_summary", project.getPerfectlyReversingSummary(change));
			precalc.put(change, "perfect_reversion_count", String.valueOf(project.getChangesPerfectlyReversing(change).count()));
			precalc.put(change, "perfectly_reverts_a_previous_change", 
				(project.getChangesPerfectlyReversing(change)
					// Did any change take place before this change?
					.anyMatch(ch -> ch.getDataset().getDate().compareTo(change.getDataset().getDate()) < 0)
				) ? "yes" : "no"		
			);
			precalc.put(change, "perfectly_reverts_a_later_change", 
				(project.getChangesPerfectlyReversing(change)
					// Did any change take place before this change?
					.anyMatch(ch -> ch.getDataset().getDate().compareTo(change.getDataset().getDate()) > 0)
				) ? "yes" : "no"		
			);
			
			// TODO: broken! This returns 'yes' when changes are empty.
			precalc.put(change, "perfectly_reverts_all_previous_changes", 
				(project.getChangesPerfectlyReversing(change)
					// Did every change take place before this change?
					.allMatch(ch -> ch.getDataset().getDate().compareTo(change.getDataset().getDate()) < 0)
				) ? "yes" : "no"
			);
		}
		
		// Set up columns.
		cols.add(createTableColumnForTable("id", 40.0, precalc));		
		cols.add(createTableColumnForTable("type", 40.0, precalc));
		cols.add(createTableColumnForTable("from", 200.0, precalc));
		cols.add(createTableColumnForTable("to", 200.0, precalc));
		cols.add(createTableColumnForTable("dataset", 100.0, precalc));
		cols.add(createTableColumnForTable("year", 100.0, precalc));
		
		// Identify reversions, classified as:
		//  (3) â€œreversion rateâ€� as the proportion of all corrections that partially reverted an earlier correction
		//  (4) "perfect revisionary rate", in which a lump is paired with a split that perfectly reverts the change made earlier

		cols.add(createTableColumnForTable("change", 50.0, precalc));
		cols.add(createTableColumnForTable("reversions", 200.0, precalc));
		cols.add(createTableColumnForTable("reversion_count", 200.0, precalc));
		cols.add(createTableColumnForTable("reverts_a_previous_change", 100.0, precalc));
		cols.add(createTableColumnForTable("reverts_a_later_change", 100.0, precalc));
		
		//cols.add(createTableColumnForTable("reverts_all_previous_changes", 100.0, precalc));
		cols.add(createTableColumnForTable("perfect_reversions", 200.0, precalc));
		cols.add(createTableColumnForTable("perfect_reversions_summary", 200.0, precalc));
		cols.add(createTableColumnForTable("perfect_reversion_count", 200.0, precalc));
		cols.add(createTableColumnForTable("perfectly_reverts_a_previous_change", 100.0, precalc));
		cols.add(createTableColumnForTable("perfectly_reverts_a_later_change", 100.0, precalc));		
		// cols.add(createTableColumnForTable("perfectly_reverts_all_previous_changes", 100.0, precalc));
	}
}
