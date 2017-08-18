
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import com.ggvaidya.scinames.ui.ProjectView;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.stage.Stage;

/**
 * A DataByBinomialNamesTabularView displays an entire dataset.
 * It uses the TabularDataView to do this.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class DataByBinomialNamesTabularView {
	private static final Logger LOGGER = Logger.getLogger(DataByBinomialNamesTabularView.class.getSimpleName());
	
	private Stage stage;
	private Scene scene;
	private Dataset dataset;
	private ProjectView pv;
	private TabularDataViewController controller;
	
	public Stage getStage() { return stage; }

	public DataByBinomialNamesTabularView(ProjectView pv, Dataset dataset) {
		this.dataset = dataset;
		stage = new Stage();
		
		controller = TabularDataViewController.createTabularDataView();
		scene = controller.getScene();
		init();
		
		// Go go stagey scene.
		stage.setScene(scene);
	}
	
	ObservableList<Name> binomialNames = FXCollections.observableArrayList();
	
	public TableColumn<Name, String> createNameColumn(String colName, Function<Name, String> funcValue) {
		TableColumn<Name, String> col = new TableColumn<Name, String>(colName);
		col.setCellValueFactory(cvf -> new ReadOnlyStringWrapper(funcValue.apply(cvf.getValue())));
		return col;
	}
	
	public void init() {
		// Setup stage.
		stage.setTitle("Dataset: " + dataset.getName());
		
		// Setup headertext.
		controller.getHeaderTextProperty().set("Values from this dataset.");
		controller.getHeaderTextEditableProperty().set(true);
		
		// Clear columns, add columns for names, then add three for each dataset column.
		ObservableList<TableColumn> cols = controller.getTableColumnsProperty();
		cols.clear();
		

		// Set items.
		controller.getTableItemsProperty().set(binomialNames);

		// Pretty much everything we get is from RowsByName, so tie that in.
		Map<Name, Set<DatasetRow>> rowsByName = dataset.getRowsByName();		
		
		// Regroup these using binomial names.
		Map<Name, Set<Name>> namesAsBinomials = new HashMap<>();
		
		Name noBinomialName = Name.EMPTY;
		
		for(Name n: rowsByName.keySet()) {
			Name binom = n.asBinomial().findAny().orElse(noBinomialName);
			
			if(!namesAsBinomials.containsKey(binom))
				namesAsBinomials.put(binom, new HashSet<>());
			
			namesAsBinomials.get(binom).add(n);
		}
		
		binomialNames.clear();
		binomialNames.addAll(namesAsBinomials.keySet().stream().sorted().collect(Collectors.toList()));
		
		// Add a column for the binomial name and all the real names.
		cols.add(createNameColumn("Binomial", binom -> (binom == Name.EMPTY) ? "No binomial name" : binom.getFullName()));
		cols.add(createNameColumn("Names", binom -> namesAsBinomials.get(binom).stream().map(Name::getFullName).collect(Collectors.joining("; "))));
		cols.add(createNameColumn("row_count", binom -> String.valueOf(namesAsBinomials.get(binom).stream()
			.flatMap(n -> rowsByName.get(n).stream())
			.distinct()
			.count()
		)));
	}
}
