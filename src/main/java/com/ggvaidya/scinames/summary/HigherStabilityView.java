
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.rank.Median;

import com.ggvaidya.scinames.model.ChangeType;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.NameClusterManager;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import com.ggvaidya.scinames.ui.ProjectView;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.stage.Stage;

/**
 * A HigherStabilityView shows the stability of higher taxa over time. This module
 * does this in the simplest way possible: EVERY unique set of compositions counts
 * as a separate higher taxon concept EXCEPT for synonymous changes. This includes
 * names being added in. So when this system tells you a taxon is stable, it pretty
 * much means untouched.
 * 
 * We use the Tabular Data View, with our one argument being the column name to group
 * higher taxonomy with.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class HigherStabilityView {
	private Logger LOGGER = Logger.getLogger(HigherStabilityView.class.getSimpleName());
	private Stage stage;
	private Scene scene;
	private ProjectView projectView;
	private TabularDataViewController controller;
	
	public Stage getStage() { return stage; }
	
	Table<String, String, String> precalc = HashBasedTable.create();
	Map<String, Dataset> datasetNames = new HashMap<>();
	List<String> datasetNamesInOrder = new LinkedList<>();
	private Set<DatasetColumn> datasetColumns = new HashSet<>();
	private ObservableList<String> higherTaxaList = FXCollections.observableList(new LinkedList<>());

	public HigherStabilityView(ProjectView pv) {
		projectView = pv;
		stage = new Stage();
		
		controller = TabularDataViewController.createTabularDataView();
		scene = controller.getScene();
		init();
		
		// Go go stagey scene.
		stage.setScene(scene);
	}
	
	private TableColumn<String, String> createTableColumn(String colName, Function<String, String> func) {
		TableColumn<String, String> tableColumn = new TableColumn<>(colName);
		tableColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(func.apply(features.getValue())));
		
		tableColumn.setPrefWidth(100.0);
		
		return tableColumn;
	}
	
	private TableColumn<String, String> createTableColumnFromPrecalc(Table<String, String, String> precalc, String colName) {
		return createTableColumn(colName, row -> precalc.get(row, colName));
	}
	
	public void init() {
		Project project = projectView.getProject();
		
		// Setup stage.
		stage.setTitle("Higher taxonomy stability between " + project.getDatasets().size() + " datasets");
		
		// Setup table.
		controller.getTableEditableProperty().set(false);
		//controller.setTableColumnResizeProperty(TableView.CONSTRAINED_RESIZE_POLICY);
		ObservableList<TableColumn> cols = controller.getTableColumnsProperty();
		cols.clear();
		
		// Precalculating.
		
		// Generate list of all dataset columns to search on
		datasetColumns = project.getDatasets().stream()
			.flatMap(ds -> ds.getColumns().stream())
			.collect(Collectors.toSet());
		
		// Set up columns.
		cols.add(createTableColumnFromPrecalc(precalc, "HigherTaxon"));
		
		// One column per dataset
		for(Dataset ds: project.getDatasets()) {
			String dsBaseName = ds.getName(); 
			String dsName = dsBaseName; 
			
			int index = 1;
			while(datasetNames.containsKey(dsName)) {
				dsName = dsBaseName + "_" + index;
				index += 1;
			}
			datasetNames.put(dsName, ds);
			datasetNamesInOrder.add(dsName);
			
			cols.add(createTableColumnFromPrecalc(precalc, dsName + "_with_synonymy"));
			cols.add(createTableColumnFromPrecalc(precalc, dsName + "_without_synonymy"));
		}

		// Set table items to an observable list.
		controller.getTableItemsProperty().set(higherTaxaList);
		
		// Set up callbacks so we modify that observable list whenever the user changes the header text.
		controller.getHeaderTextEditableProperty().set(true);
		controller.getHeaderTextProperty().set("Enter higher taxonomy column name here, or set blank for genus");
		controller.getHeaderTextProperty().addListener(a -> {
			generateHigherTaxonomyList(controller.getHeaderTextProperty().get());
		});
		
		// Start with "" (which means genus)
		generateHigherTaxonomyList("");
	}
	
	private void generateHigherTaxonomyList(String higherTaxonomyColName) {
		Project project = projectView.getProject();
		DatasetColumn GENUS = DatasetColumn.fakeColumnFor("genus");
		DatasetColumn column;
		
		if(higherTaxonomyColName.equals("")) column = GENUS;
		else if(datasetColumns.contains(DatasetColumn.of(higherTaxonomyColName))) {
			column = DatasetColumn.of(higherTaxonomyColName);
		} else {
			// Don't actually change until we have a valid column name.
			return;
		}
		
		// Group names by dataset column.
		Table<String, Dataset, Set<Name>> namesByDataset = HashBasedTable.create();
		
		for(String dsName: datasetNamesInOrder) {
			Dataset ds = datasetNames.get(dsName);
			
			if(column == GENUS) {
				Map<String, List<Name>> rowsByGenus = ds.getNamesInAllRows().stream().collect(Collectors.groupingBy(n -> n.getGenus()));
				
				for(String genus: rowsByGenus.keySet()) {
					namesByDataset.put(genus, ds, new HashSet<>(rowsByGenus.get(genus)));
				}
			} else {
				Map<DatasetRow, Set<Name>> namesByRow = ds.getNamesByRow();
				
				for(DatasetRow row: namesByRow.keySet()) {
					String colValue = row.get(column);
					if(colValue == null) colValue = "(null)";
					
					if(!namesByDataset.contains(colValue, ds))
						namesByDataset.put(colValue, ds, new HashSet<>());
					
					namesByDataset.get(colValue, ds).addAll(namesByRow.getOrDefault(row, new HashSet<>()));
				}
			}
		}
		
		// LOGGER.info("namesByDataset = " + namesByDataset);
		
		// By this point, namesByDataset should be ready to go.
		// So let's fill out precalc.
		precalc.clear();
		
		for(String rowName: namesByDataset.rowKeySet()) {
			precalc.put(rowName, "HigherTaxon", rowName);

			Set<Name> prevNames = new HashSet<>();
			
			for(String dsName: datasetNamesInOrder) {
				Dataset ds = datasetNames.get(dsName);
				Set<Name> names = namesByDataset.get(rowName, ds);
				
				// Missing?! Oh noes.
				if(names == null) names = new HashSet<>();
				
				// For now, let's just note down how many names we have.
				precalc.put(rowName, dsName + "_with_synonymy", calculateDifferenceWithSynonymy(prevNames, names));
				precalc.put(rowName, dsName + "_without_synonymy", calculateDifferenceWithoutSynonymy(prevNames, names));
				
				// Set up prevNames for next time 'round
				prevNames = names;
			}
		}
		
		// LOGGER.info("precalc = " + precalc);
		
		// Tell everybody what we did.
		higherTaxaList.clear();
		higherTaxaList.addAll(namesByDataset.rowKeySet());
		LOGGER.info("higherTaxaList = " + higherTaxaList);
		
		controller.getTableView().refresh();
	}
	
	private String calculateDifferenceWithSynonymy(Set<Name> prevNames, Set<Name> names) {
		return "NA";
	}
	
	private String calculateDifferenceWithoutSynonymy(Set<Name> prevNames, Set<Name> names) {
		// There are three possibilities:
		//		1. IDENTICAL: no difference between these two sets.
		//		2. CONTRACTED: names contains every but not all names in prevNames
		//		3. EXPANDED: prevNames contains every but not all names in names
		//		4. COMPLEX: names have both EXPANDED and CONTRACTED
		Set<Name> prevButNotCurrent = new HashSet<>(prevNames);
		prevButNotCurrent.removeAll(names);
		
		Set<Name> currentButNotPrev = new HashSet<>(names);
		currentButNotPrev.removeAll(prevNames);
		
		if(prevButNotCurrent.isEmpty() && currentButNotPrev.isEmpty()) {
			return "IDENTICAL";
		} else if(!prevButNotCurrent.isEmpty() && currentButNotPrev.isEmpty()) {
			return "CONTRACTED: " + prevButNotCurrent.size();
		} else if(prevButNotCurrent.isEmpty() && !currentButNotPrev.isEmpty()) {
			return "EXPANDED: " + currentButNotPrev.size();
		} else {
			return "COMPLEX: " + prevButNotCurrent.size() + " deleted, " + currentButNotPrev.size() + " added";
		}
		
	}
}
