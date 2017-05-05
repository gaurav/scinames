
/*
 *
 *  ValidationSuiteView
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

package com.ggvaidya.scinames.validation;

import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.project.ProjectView;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * The Validation Suite carries out a bunch of validation checks and reports
 * errors. It uses a tabular view to display them.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class ValidationSuiteView {
	private Stage stage;
	private Scene scene;
	private ProjectView projectView;
	private TabularDataViewController controller;
	private List<Validator> validators = new ArrayList();
	
	public Stage getStage() { return stage; }

	public ValidationSuiteView(ProjectView pv) {
		projectView = pv;
		stage = new Stage();
		
		// Set up validators.
		validators.add(new ChangeValidator());
		
		// Set up controller
		controller = TabularDataViewController.createTabularDataView();
		scene = controller.getScene();
		init();
	
		// Go go stagey scene.
		stage.setScene(scene);
	}
	
	/*
	private TableColumn<NameCluster, String> createTableColumnForObservable(String colName, Callback<TableColumn.CellDataFeatures<NameCluster,String>,ObservableValue<String>> valueFunc) {
		TableColumn<NameCluster, String> col = new TableColumn<>(colName);
		col.setCellValueFactory(valueFunc);
		col.setPrefWidth(100);
		return col;
	}*/
	
	private TableColumn<ValidationError, String> createTableColumnForValidationError(String colName, Callback<ValidationError, String> valueFunc) {
		TableColumn<ValidationError, String> col = new TableColumn<>(colName);
		col.setCellValueFactory((param) -> new ReadOnlyStringWrapper(valueFunc.call(param.getValue())));
		col.setPrefWidth(100);
		return col;
	}
	
	public void init() {
		// Setup stage.
		stage.setTitle("Validation errors from " + validators.size() + " validators");
		
		// Setup table.
		controller.getTableEditableProperty().set(false);
		//controller.setTableColumnResizeProperty(TableView.CONSTRAINED_RESIZE_POLICY);
		ObservableList<TableColumn> cols = controller.getTableColumnsProperty();
		cols.clear();
		
		// Set table items.
		controller.getTableItemsProperty().set(FXCollections.observableList(
			validators.stream().flatMap(v -> v.validate(projectView.getProject())).collect(Collectors.toList())
		));
		
		// Set up table columns.
		cols.add(createTableColumnForValidationError("Validator", ve -> ve.getValidator().getName()));
		cols.add(createTableColumnForValidationError("Dataset", ve -> {
			Optional<Dataset> dataset = ve.getDataset();
			if(dataset.isPresent())
				return dataset.get().getCitation();
			else
				return "(none)";
		}));
		
		TableColumn<ValidationError, String> col = createTableColumnForValidationError("Message", ve -> ve.getMessage());
		col.setPrefWidth(300.0);
		cols.add(col);
		
		col = createTableColumnForValidationError("Target", ve -> ve.getTarget().toString());
		col.setPrefWidth(500.0);
		cols.add(col);
		
		// Double-click on rows should take you to the entry.
		controller.getTableView().setOnMouseClicked(evt -> {
			if(evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2) {
				// Double-click!
				ValidationError ve = (ValidationError) controller.getTableView().getSelectionModel().getSelectedItem();
				projectView.openDetailedView(ve.getTarget());
				
				evt.consume();
			}
		});
	}
}
