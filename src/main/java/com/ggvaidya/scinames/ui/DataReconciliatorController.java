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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.TaxonConcept;
import com.ggvaidya.scinames.util.SimplifiedDate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.stage.FileChooser;

/**
 * FXML Controller class for a data reconciliator, which provides different streams
 * of recognized names, name clusters and  from within the system 
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DataReconciliatorController implements Initializable {
	private static final Logger LOGGER = Logger.getLogger(DataReconciliatorController.class.getSimpleName());
	
	private static final Dataset ALL = new Dataset("All", SimplifiedDate.MIN, Dataset.TYPE_DATASET);
	private static final Dataset NONE = new Dataset("None", SimplifiedDate.MIN, Dataset.TYPE_DATASET);
	private static final String RECONCILE_BY_NAME = "Names";
	private static final String RECONCILE_BY_SPECIES_NAME = "Species (binomial) names";
	private static final String RECONCILE_BY_SPECIES_NAME_CLUSTER = "Species name clusters";
	private static final String RECONCILE_BY_NAME_CLUSTER = "All name clusters";	
	private static final String RECONCILE_BY_SPECIES_TAXON_CONCEPT = "Species taxon concepts";
	
	private static final String USE_NAMES_IN_DATASET_ROWS = "Use names in dataset rows";
	private static final String USE_ALL_RECOGNIZED_NAMES = "Use all recognized names";
	private static final String USE_ALL_REFERENCED_NAMES = "Use all referenced names";
	
	private DataReconciliatorView dataReconciliatorView;
	public void setDataReconciliatorView(DataReconciliatorView drv) {
		dataReconciliatorView = drv;
		
		// Fill in the comboboxes.
		Project p = drv.getProjectView().getProject();
		
		namesToUseComboBox.getItems().addAll(
			USE_NAMES_IN_DATASET_ROWS,
			USE_ALL_REFERENCED_NAMES,
			USE_ALL_RECOGNIZED_NAMES
		);
		namesToUseComboBox.getSelectionModel().clearAndSelect(0);
		
		ArrayList<Dataset> ds_useNamesFrom = new ArrayList<>(p.getDatasets());
		ds_useNamesFrom.add(0, ALL); // Hee.
		ObservableList<Dataset> useNamesFrom = FXCollections.observableList(ds_useNamesFrom);
		useNamesFromComboBox.setItems(useNamesFrom);
		useNamesFromComboBox.getSelectionModel().select(0);
		
		ArrayList<Dataset> ds_includeDataFrom = new ArrayList<>(p.getDatasets());
		ds_includeDataFrom.add(0, ALL); 	// Hee.
		ds_includeDataFrom.add(0, NONE); 	// Also hee.
		includeDataFromComboBox.setItems(FXCollections.observableList(ds_includeDataFrom));
		includeDataFromComboBox.getSelectionModel().select(0);		
		
		reconcileUsingComboBox.getItems().add(RECONCILE_BY_NAME);
		reconcileUsingComboBox.getItems().add(RECONCILE_BY_SPECIES_NAME);
		reconcileUsingComboBox.getItems().add(RECONCILE_BY_SPECIES_NAME_CLUSTER);
		reconcileUsingComboBox.getItems().add(RECONCILE_BY_NAME_CLUSTER);		
		// reconcileUsingComboBox.getItems().add(RECONCILE_BY_SPECIES_TAXON_CONCEPT);
			// Using a new, UNTESTED algorithm now! Please test before using!
		reconcileUsingComboBox.getSelectionModel().select(RECONCILE_BY_SPECIES_NAME_CLUSTER);
		
		// Fill in the table with the defaults.
		// reconcileData();
	}
		
	/**
	 * Initializes the controller class.
	 */
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		
	}
	
	private TableColumn<String, String> createColumnFromPrecalc(String colName, Table<String, String, Set<String>> precalc) {
		TableColumn<String, String> column = new TableColumn<>(colName);
		column.cellValueFactoryProperty().set(
			(TableColumn.CellDataFeatures<String, String> cdf) -> {
				String clusterID = cdf.getValue();
				
				// There might be columns found in some dataset but not in others
				// so we detect those cases here and put in "NA"s instead.
				String output = "NA";
				if(precalc.contains(clusterID, colName))
					output = precalc.get(clusterID, colName).stream().collect(Collectors.joining("; "));
				
				return new ReadOnlyStringWrapper(output);
			}
		);
		column.setPrefWidth(100.0);
		column.setEditable(false);
		return column;
	}
	
	@FXML
	private void reconcileData(ActionEvent evt) {
		// Step 1. Figure out what kind of reconciliation we need to perform.
		// We skip this step, as there is only one kind.
		String tabText = upperTabPane.getSelectionModel().getSelectedItem().getText();
		
		switch(tabText) {
			case "Reconcile data from one dataset": 
				reconcileDataFromOneDataset();
				break;
				
			default:
				LOGGER.severe("Unknown tab: '" + tabText + "'");
				break;
		}
	}
	
	private Set<String> getOneElementSet(long val) {
		return getOneElementSet(String.valueOf(val));
	}
	
	private Set<String> getOneElementSet(String str) {
		HashSet<String> hs = new HashSet<>();
		hs.add(str);
		return hs;
	}
	
	private List<NameCluster> createSingleNameClusters(Dataset dt, Collection<Name> names) {
		// Convenience method: we create a set of NameClusters, each 
		// consisting of a single name.
		return names.stream()
			.sorted()
			.map(n -> new NameCluster(dt, n))
			.collect(Collectors.toList());
	}
		
	private void reconcileDataFromOneDataset() {
		Project project = dataReconciliatorView.getProjectView().getProject();
		String reconciliationMethod = reconcileUsingComboBox.getValue();
		Table<String, String, Set<String>> precalc = HashBasedTable.create();
		
		Dataset namesDataset = useNamesFromComboBox.getSelectionModel().getSelectedItem();
		List<NameCluster> nameClusters = null;
		List<Name> namesInDataset = null;
		
		// Set up namesInDataset.
		switch(namesToUseComboBox.getValue()) {
			case USE_NAMES_IN_DATASET_ROWS:
				if(namesDataset == ALL) {
					namesInDataset = project.getDatasets().stream()
						.flatMap(ds -> ds.getNamesInAllRows().stream())
						.distinct()
						.sorted()
						.collect(Collectors.toList());
				} else {
					namesInDataset = namesDataset.getNamesInAllRows().stream().sorted().distinct().collect(Collectors.toList());
				}
				break;
				
			case USE_ALL_REFERENCED_NAMES:
				if(namesDataset == ALL) {
					namesInDataset = project.getDatasets().stream()
						.flatMap(ds -> ds.getReferencedNames())
						.distinct()
						.sorted()
						.collect(Collectors.toList());
				} else {
					namesInDataset = namesDataset.getReferencedNames().sorted().collect(Collectors.toList());
				}
				
				break;
				
			case USE_ALL_RECOGNIZED_NAMES:
				if(namesDataset == ALL) {
					namesInDataset = project.getDatasets().stream()
						.flatMap(ds -> project.getRecognizedNames(ds).stream())
						.distinct()
						.sorted()
						.collect(Collectors.toList());
				} else {
					namesInDataset = project.getRecognizedNames(namesDataset).stream().sorted().collect(Collectors.toList());
				}
				
				break;
		}
		
		// IMPORTANT NOTE
		// This algorithm now relies on nameClusters and namesInDataset
		// having EXACTLY the same size. So please make sure every combination
		// of logic here lines up exactly.
		
		boolean flag_nameClustersAreTaxonConcepts = false;
		switch(reconciliationMethod) {
			case RECONCILE_BY_NAME:
				// namesInDataset already has all the names we want.
				
				nameClusters = createSingleNameClusters(namesDataset, namesInDataset);
				
				break;
				
			case RECONCILE_BY_SPECIES_NAME:
				namesInDataset = namesInDataset.stream()
					.filter(n -> n.hasSpecificEpithet())
					.flatMap(n -> n.asBinomial())
					.distinct()
					.sorted()
					.collect(Collectors.toList());
			
				nameClusters = createSingleNameClusters(namesDataset, namesInDataset);

				break;
			
			case RECONCILE_BY_SPECIES_NAME_CLUSTER:
				// nameClusters = project.getNameClusterManager().getSpeciesClustersAfterFiltering(project).collect(Collectors.toList());
				
				namesInDataset = namesInDataset.stream()
					.filter(n -> n.hasSpecificEpithet())
					.flatMap(n -> n.asBinomial())
					.distinct()
					.sorted()
					.collect(Collectors.toList());
				
				nameClusters = project.getNameClusterManager().getClusters(namesInDataset);
				
				break;
				
			case RECONCILE_BY_NAME_CLUSTER:
				// Note that this includes genus name clusters!
				nameClusters = project.getNameClusterManager().getClusters(namesInDataset);
				
				break;	
				
			case RECONCILE_BY_SPECIES_TAXON_CONCEPT:
				/*
				 * WARNING: untested! Please test before using!
				 */
				
				List<NameCluster> nameClustersByName = project.getNameClusterManager().getClusters(
					namesInDataset
				);
				
				List<Name> namesInDatasetCorresponding = new LinkedList<>();
				List<NameCluster> nameClustersCorresponding = new LinkedList<>();
				
				for(int x = 0; x < namesInDataset.size(); x++) {
					Name name = namesInDataset.get(0);
					NameCluster nameCluster = nameClustersByName.get(0);
					List<TaxonConcept> taxonConcepts;
					
					if(nameCluster == null) {
						taxonConcepts = new ArrayList<>();
					} else {
						taxonConcepts = nameCluster.getTaxonConcepts(project); 
					}
					
					// Now we need to unwind this data structure: each entry in nameClusters  
					// should have a corresponding entry in namesInDataset.
					for(TaxonConcept tc: taxonConcepts) {
						namesInDatasetCorresponding.add(name);
						nameClustersCorresponding.add((NameCluster) tc);
					}
				}
				
				// All good? Let's swap in those variables to replace their actual counterparts.
				namesInDataset = namesInDatasetCorresponding;
				nameClusters = nameClustersCorresponding;
				
				// This is special, at least for now. Maybe some day it won't?
				flag_nameClustersAreTaxonConcepts = true;
				
				break;
				
			default:
				LOGGER.log(Level.SEVERE, "Reconciliation method ''{0}'' has not yet been implemented!", reconciliationMethod);
				return;
		}
		
		if(nameClusters == null) {
			dataTableView.setItems(FXCollections.emptyObservableList());	
			return;
		}
		
		LOGGER.info("Name clusters ready to display: " + nameClusters.size() + " clusters");
		LOGGER.info("Based on " + namesInDataset.size() + " names from " + namesDataset + ": " + namesInDataset);		
				
		// What columns do we have from the other dataset?
		Dataset dataDataset = includeDataFromComboBox.getSelectionModel().getSelectedItem();
		List<Dataset> datasets = null;
		if(dataDataset == ALL)
			datasets = project.getDatasets();
		else if(dataDataset == NONE)
			datasets = new ArrayList<>();
		else
			datasets = Arrays.asList(dataDataset);
		
		// Precalculate.
		List<String> existingColNames = new ArrayList<>();
		existingColNames.add("id");
		existingColNames.add("name");
		existingColNames.add("names_in_dataset");		
		existingColNames.add("all_names_in_cluster");
		existingColNames.add("dataset_rows_for_name");
		existingColNames.add("name_cluster_id");
		// existingColNames.add("distinct_dataset_rows_for_name");
		
		// If these are taxon concepts, there's three other columns we want
		// to emit.
		if(flag_nameClustersAreTaxonConcepts) {
			existingColNames.add("starts_with");
			existingColNames.add("ends_with");
			existingColNames.add("is_ongoing");
		} else {
			existingColNames.add("taxon_concept_count");
			existingColNames.add("taxon_concepts");
			existingColNames.add("trajectory");
			existingColNames.add("trajectory_without_renames");
			existingColNames.add("trajectory_lumps_splits");
		}
		
		existingColNames.add("first_added_dataset");
		existingColNames.add("first_added_year");
		
		existingColNames.add("reconciliation_duplicate_of");
		
		// Precalculate all dataset rows.
		Map<Name, Set<DatasetRow>> datasetRowsByName = new HashMap<>();
		for(Dataset ds: datasets) {
			Map<Name, Set<DatasetRow>> rowsByName = ds.getRowsByName();
			
			// Merge into the main list.
			for(Name n: rowsByName.keySet()) {
				Set<DatasetRow> rows = rowsByName.get(n);
				
				if(
					!reconciliationMethod.equals(RECONCILE_BY_NAME)
				) {
					// If we're reconciling by binomial names, then
					// we should include binomial names for each row, too.
					Optional<Name> binomialName = n.asBinomial().findAny();
					if(binomialName.isPresent()) {
						Set<DatasetRow> rowsForBinomial = rowsByName.get(binomialName.get());
						if(rowsForBinomial != null)
							rows.addAll(rowsForBinomial);
						
						// Don't write this to the sub-binomial name,
						// just write to the binomial name.
						n = binomialName.get();
					}
				}
				
				if(!datasetRowsByName.containsKey(n))
					datasetRowsByName.put(n, new HashSet<>());
				
				datasetRowsByName.get(n).addAll(rows);
			}
		}
				
		LOGGER.info("Precalculating all dataset rows");
		
		// Finally, come up with unique names for every dataset we might have.
		Map<DatasetColumn, String> datasetColumnMap = new HashMap<>();

		existingColNames.addAll(
			datasets.stream().flatMap(ds -> ds.getColumns().stream())
				.distinct()
				.map(col -> {
					String colName = col.getName();
					String baseName = colName;
					
					int uniqueCounter = 0;
					while(existingColNames.contains(colName)) {
						// Duplicate column name! Map it elsewhere.
						uniqueCounter++;
						colName = baseName + "." + uniqueCounter;
					}
					
					// Where did we map it to?
					datasetColumnMap.put(col, colName);
					
					// Okay, now return the new column name we need to create.
					return colName;
				})
				.collect(Collectors.toList())
		);

		LOGGER.info("Precalculating " + nameClusters.size() + " name clusters");
		
		// Make sure names and name clusters are unique, otherwise bail.
		// Earlier this was being ensured by keeping namesInDataset as a
		// Set, but since it's a List now, duplicates might sneak in.
		assert(namesInDataset.size() == new HashSet<>(namesInDataset).size());
		
		// Since it's a list, we can set it up so that it always corresponds to
		// the correct name cluster.
		assert(namesInDataset.size() == nameClusters.size());
		
		// Now, nameClusters should NOT be de-duplicated: we might have the same
		// cluster appear multiple times! If so, we'll set 
		// "reconciliation_duplicate_of" to point to the first reconciliation,
		// so we don't duplicate reconciliations.
		
		
		// Let's track which IDs we use for duplicated name clusters.
		Map<NameCluster, List<String>> idsForNameClusters = new HashMap<>();
		
		if(nameClusters.size() != new HashSet<>(nameClusters).size()) {
			
			LOGGER.warning(
				"Clusters not unique: " + nameClusters.size() + 
				" clusters found, but only " + new HashSet<>(nameClusters).size() + " are unique."
			);
		}
		
		// Track duplicates.
		Map<NameCluster, List<String>> clusterIDsPerNameCluster = new HashMap<>(); 
		
		int totalClusterCount = nameClusters.size();
		int currentClusterCount = 0;
		List<String> nameClusterIDs = new LinkedList<>();
		for(NameCluster cluster: nameClusters) {
			currentClusterCount++;
			
			// Probably don't need GUIDs here, right?
			String clusterID = String.valueOf(currentClusterCount);
			nameClusterIDs.add(clusterID);
			
			LOGGER.info("(" + currentClusterCount + "/" + totalClusterCount + ") Precalculating name cluster: " + cluster);			

			precalc.put(clusterID, "id", getOneElementSet(clusterID));
			precalc.put(clusterID, "name_cluster_id", getOneElementSet(cluster.getId().toString()));
			
			// The 'name' should come from namesInDataset.
			precalc.put(clusterID, "name", getOneElementSet(namesInDataset.get(currentClusterCount - 1).getFullName()));
			
			// Okay, here's what we need to do:
			//	- If names is ALL, then we can't do better than cluster.getName().
			if(namesDataset == ALL) {
				precalc.put(clusterID, "names_in_dataset",  cluster.getNames().stream().map(n -> n.getFullName()).collect(Collectors.toSet()));
			} else {
				// hey, here's something cool we can do: figure out which name(s)
				// this dataset uses from this cluster!
				Set<Name> namesToFilterTo = new HashSet<>(namesInDataset);
				
				List<String> namesInCluster = cluster.getNames().stream()
					.filter(n -> namesToFilterTo.contains(n))
					.map(n -> n.getFullName())
					.collect(Collectors.toList());
				
				precalc.put(clusterID, "names_in_dataset", new HashSet<>(namesInCluster));
			}
			
			precalc.put(clusterID, "all_names_in_cluster", cluster.getNames().stream().map(n -> n.getFullName()).collect(Collectors.toSet()));
			
			// Is this a duplicate?
			if(clusterIDsPerNameCluster.containsKey(cluster)) {
				List<String> duplicatedRows = clusterIDsPerNameCluster.get(cluster);
				
				// Only the first one should have the actual data.
				
				precalc.put(clusterID, "reconciliation_duplicate_of", getOneElementSet(duplicatedRows.get(0)));
				duplicatedRows.add(clusterID);
				
				// Okay, do no other work on this cluster, since all the actual information is
				// in the other entry.
				continue;
				
			} else {
				precalc.put(clusterID, "reconciliation_duplicate_of", getOneElementSet("NA"));
				
				List<String> clusterIds = new LinkedList<>();
				clusterIds.add(clusterID);
				clusterIDsPerNameCluster.put(cluster, clusterIds);
			}
			
			LOGGER.fine("Cluster calculation began for " + cluster);
			
			// If it's a taxon concept, precalculate a few more columns.
			if(flag_nameClustersAreTaxonConcepts) {
				TaxonConcept tc = (TaxonConcept) cluster;
				
				precalc.put(clusterID, "starts_with", tc.getStartsWith().stream().map(ch -> ch.toString()).collect(Collectors.toSet()));
				precalc.put(clusterID, "ends_with", tc.getEndsWith().stream().map(ch -> ch.toString()).collect(Collectors.toSet()));
				precalc.put(clusterID, "is_ongoing", getOneElementSet(tc.isOngoing(project) ? "yes" : "no"));
			} else {
				// If it's a true name cluster, then perhaps people will want
				// to know what taxon concepts are in here? Maybe for some sort
				// of PhD?
				List<TaxonConcept> tcs = cluster.getTaxonConcepts(project);
				
				precalc.put(clusterID, "taxon_concept_count", getOneElementSet(String.valueOf(tcs.size())));
				precalc.put(clusterID, "taxon_concepts", tcs.stream().map(tc -> tc.toString()).collect(Collectors.toSet()));
			}
			
			LOGGER.fine("Cluster calculation ended for " + cluster);
			
			// When was this first added?
			List<Dataset> foundInSorted = cluster.getFoundInSortedWithDates();
			if(!foundInSorted.isEmpty()) {
				precalc.put(clusterID, "first_added_dataset", getOneElementSet(foundInSorted.get(0).getCitation()));
				precalc.put(clusterID, "first_added_year", getOneElementSet(foundInSorted.get(0).getDate().getYearAsString()));
			}
			
			LOGGER.fine("Trajectory began for " + cluster);
			
			// For name clusters we can also figure out trajectories!
			if(!flag_nameClustersAreTaxonConcepts) {
				List<String> trajectorySteps = cluster.getFoundInSortedWithDates().stream().map(
					dataset -> {
						String changes = dataset.getChanges(project)
							.filter(ch -> cluster.containsAny(ch.getAllNames()))
							.map(ch -> ch.getType().toString())
							.collect(Collectors.joining("|"));
						if(!changes.isEmpty()) return changes;
						
						// This can happen when a change is referenced without an explicit addition.
						if(cluster.containsAny(dataset.getReferencedNames().collect(Collectors.toList())))
							return "referenced";
						else
							return "missing";
					}
				).collect(Collectors.toList());
				
				precalc.put(clusterID, "trajectory", getOneElementSet(
					String.join(" -> ", trajectorySteps)
				));
				
				precalc.put(clusterID, "trajectory_without_renames", getOneElementSet(
					trajectorySteps.stream().filter(ch -> !ch.contains("rename")).collect(Collectors.joining(" -> "))
				));
				
				precalc.put(clusterID, "trajectory_lumps_splits", getOneElementSet(
					trajectorySteps.stream().filter(ch -> ch.contains("split") || ch.contains("lump")).collect(Collectors.joining(" -> "))	
				));
			}
			
			LOGGER.fine("Trajectory ended for " + cluster);
			
			// Okay, here's where we reconcile!
			LOGGER.fine("Reconciliation began for " + cluster);
			
			// Now we need to actually reconcile the data from these unique row objects.
			Set<DatasetRow> allDatasetRowsCombined = new HashSet<>();
			
			for(Name name: cluster.getNames()) {
				// We don't have to convert cluster names to binomial,
				// because the cluster formation -- or the hacky thing we do
				// for RECONCILE_SPECIES_NAME -- should already have done that!
				//
				// Where necessary, the previous code will automatically
				// set up datasetRowsByName so it matched binomial names.
				Set<DatasetRow> rowsToReconcile = datasetRowsByName.get(name);
				if(rowsToReconcile == null) continue;
				
				allDatasetRowsCombined.addAll(rowsToReconcile);
				
				Set<DatasetColumn> columns = rowsToReconcile.stream()
					.flatMap(row -> row.getColumns().stream())
					.collect(Collectors.toSet());				
			
				for(DatasetColumn col: columns) {
					// We've precalculated column names.
					String colName = datasetColumnMap.get(col);
	
					// Make sure we get this column down into 'precalc'. 
					if(!precalc.contains(clusterID, colName))
						precalc.put(clusterID, colName, new HashSet<>());
	
					// Add all values for all rows in this column.
					Set<String> vals = rowsToReconcile.stream().flatMap(row -> {
						if(!row.hasColumn(col)) return Stream.empty();
						else return Stream.of(row.get(col));
					}).collect(Collectors.toSet());
					
					precalc.get(clusterID, colName).addAll(vals);
					
					LOGGER.fine("Added " + vals.size() + " rows under name cluster '" + cluster + "'");
				}
			}
			
			LOGGER.info("(" + currentClusterCount + "/" + totalClusterCount + ") Reconciliation completed for " + cluster);
			
			precalc.put(clusterID, "dataset_rows_for_name", getOneElementSet(allDatasetRowsCombined.size()));
		}
		
		// Set up table items.
		dataTableView.setItems(FXCollections.observableList(nameClusterIDs));
		
		LOGGER.info("Setting up columns: " + existingColNames);
		
		dataTableView.getColumns().clear();
		for(String colName: existingColNames) {
			dataTableView.getColumns().add(createColumnFromPrecalc(colName, precalc));
		}
		
		// Get distinct column names.
		Stream<String> colNames = precalc.cellSet().stream().map(set -> set.getColumnKey());
		
		// Eliminate columns that are in the existingColNames.
		colNames = colNames.filter(colName -> !existingColNames.contains(colName));
		
		// And add tablecolumns for the rest.
		List<TableColumn<String, String>> cols = colNames.distinct().sorted().map(colName -> createColumnFromPrecalc(colName, precalc)).collect(Collectors.toList());
		dataTableView.getColumns().addAll(cols);
		dataTableView.refresh();
		
		// Fill in status text field.
		long distinctNameCount = precalc.cellSet().stream()
			.map(cluster -> precalc.get(cluster, "name"))
			.distinct()
			.count();
		String str_duplicates = "";
		if(distinctNameCount != dataTableView.getItems().size()) {
			str_duplicates = " for " + distinctNameCount + " distinct names";
		}
		
		statusTextField.setText(
			dataTableView.getItems().size() + " rows across " + cols.size() + " reconciled columns"
				+ str_duplicates
		);
		
		LOGGER.info("All done!");
	}
	
	
	/**
	 * Provide an export of the data in the TableView as a "table". In its
	 * simplest Java representation, that is a list of columns, with each
	 * column starting with a column header and then all the rest of the data.
	 * 
	 * Warning: this can be a long-running function!
	 * 
	 * @return A list of columns of data.
	 */
	public List<List<String>> getDataAsTable() {
		// What columns do we have?
		List<List<String>> result = new LinkedList<>();		
		List<TableColumn<String, ?>> columns = dataTableView.getColumns();
		
		columns.forEach(col -> {
			List<String> column = new LinkedList<>();
			
			// Add the header.
			column.add(col.getText());
			
			// Add the data.
			for(int x = 0; x < dataTableView.getItems().size(); x++) {
				ObservableValue cellObservableValue = col.getCellObservableValue(x);
				column.add(cellObservableValue.getValue().toString());
			}
			
			result.add(column);
		});
		
		return result;
	}
	
	private void fillCSVFormat(CSVFormat format, Appendable destination, List<List<String>> data) throws IOException {
		try (CSVPrinter printer = format.print(destination)) {
			List<List<String>> dataAsTable = data;
			if(dataAsTable.isEmpty())
				return;

			for(int x = 0; x < dataAsTable.get(0).size(); x++) {
				for(int y = 0; y < dataAsTable.size(); y++) {
					String value = dataAsTable.get(y).get(x);
					printer.print(value);
				}
				printer.println();
			}
		}
	}
	
	/* Actions */
	@FXML
	private void copyToClipboard(ActionEvent evt) {
		try {
			StringWriter writer = new StringWriter();
			List<List<String>> dataAsTable = getDataAsTable();
			
			fillCSVFormat(CSVFormat.TDF, writer, getDataAsTable());
			
			Clipboard clipboard = Clipboard.getSystemClipboard();
			HashMap<DataFormat, Object> content = new HashMap<>();
			content.put(DataFormat.PLAIN_TEXT, writer.getBuffer().toString());
			clipboard.setContent(content);
			
			Alert window = new Alert(Alert.AlertType.CONFIRMATION, (dataAsTable.get(0).size() - 1) + " rows written to clipboard.");
			window.showAndWait();
		} catch(IOException e) {
			Alert window = new Alert(Alert.AlertType.ERROR, "Could not save CSV to the clipboard: " + e);
			window.showAndWait();
		}		
	}
	
	@FXML
	private void exportToCSV(ActionEvent evt) {
		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().setAll(
			new FileChooser.ExtensionFilter("CSV file", "*.csv"),
			new FileChooser.ExtensionFilter("Tab-delimited file", "*.txt")			
		);
		File file = chooser.showSaveDialog(dataReconciliatorView.getStage());
		if(file != null) {
			CSVFormat format = CSVFormat.RFC4180;
			
			String outputFormat = chooser.getSelectedExtensionFilter().getDescription();
			if(outputFormat.equalsIgnoreCase("Tab-delimited file"))
				format = CSVFormat.TDF;
			
			try {
				List<List<String>> dataAsTable = getDataAsTable();
				fillCSVFormat(format, new FileWriter(file), dataAsTable);
				
				Alert window = new Alert(Alert.AlertType.CONFIRMATION, "CSV file '" + file + "' saved with " + (dataAsTable.get(0).size() - 1) + " rows.");
				window.showAndWait();
				
			} catch(IOException e) {
				Alert window = new Alert(Alert.AlertType.ERROR, "Could not save CSV to '" + file + "': " + e);
				window.showAndWait();
			}
		}
	}
	
	
	/* FXML objects */
	@FXML private ComboBox<String> namesToUseComboBox;
	@FXML private ComboBox<Dataset> useNamesFromComboBox;
	@FXML private ComboBox<Dataset> includeDataFromComboBox;
	@FXML private ComboBox<String> reconcileUsingComboBox;	
	@FXML private TabPane upperTabPane;
	@FXML private TableView<String> dataTableView;
	@FXML private TextField statusTextField;
}
