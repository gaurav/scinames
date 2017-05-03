/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ggvaidya.scinames.ui;

import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.TaxonConcept;
import com.ggvaidya.scinames.project.ProjectView;
import com.ggvaidya.scinames.util.SimplifiedDate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * FXML Controller class
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DataReconciliatorController implements Initializable {
	private static final Logger LOGGER = Logger.getLogger(DataReconciliatorController.class.getSimpleName());
	
	private static final Dataset ALL = new Dataset("All", SimplifiedDate.MIN);
	private static final String RECONCILE_BY_NAME = "Names";
	private static final String RECONCILE_BY_SPECIES_NAME_CLUSTER = "Species name clusters";
	private static final String RECONCILE_BY_NAME_CLUSTER = "All name clusters";	
	private static final String RECONCILE_BY_SPECIES_TAXON_CONCEPT = "Species taxon concepts";	
	
	private DataReconciliatorView dataReconciliatorView;
	public void setDataReconciliatorView(DataReconciliatorView drv) {
		dataReconciliatorView = drv;
		
		// Fill in the comboboxes.
		Project p = drv.getProjectView().getProject();
		
		ArrayList<Dataset> ds_useNamesFrom = new ArrayList(p.getDatasets());
		ds_useNamesFrom.add(0, ALL); // Hee.
		ObservableList<Dataset> useNamesFrom = FXCollections.observableList(ds_useNamesFrom);
		useNamesFromComboBox.setItems(useNamesFrom);
		useNamesFromComboBox.getSelectionModel().select(0);
		useNamesFromComboBox.onActionProperty().set((evt) -> reconcileData());
		
		ArrayList<Dataset> ds_includeDataFrom = new ArrayList(p.getDatasets());
		ds_includeDataFrom.add(0, ALL); // Hee.
		includeDataFromComboBox.setItems(FXCollections.observableList(ds_includeDataFrom));
		includeDataFromComboBox.getSelectionModel().select(0);
		includeDataFromComboBox.onActionProperty().set((evt) -> reconcileData());		
		
		reconcileUsingComboBox.getItems().add(RECONCILE_BY_NAME);
		reconcileUsingComboBox.getItems().add(RECONCILE_BY_SPECIES_NAME_CLUSTER);
		reconcileUsingComboBox.getItems().add(RECONCILE_BY_NAME_CLUSTER);		
		reconcileUsingComboBox.getItems().add(RECONCILE_BY_SPECIES_TAXON_CONCEPT);	
		reconcileUsingComboBox.getSelectionModel().select(RECONCILE_BY_SPECIES_NAME_CLUSTER);
		reconcileUsingComboBox.onActionProperty().set((evt) -> reconcileData());
		
		// Fill in the table with the defaults.
		reconcileData();
	}
		
	/**
	 * Initializes the controller class.
	 */
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		
	}
	
	private TableColumn<NameCluster, String> createColumnFromPrecalc(String colName, Table<NameCluster, String, Set<String>> precalc) {
		TableColumn<NameCluster, String> column = new TableColumn<>(colName);
		column.cellValueFactoryProperty().set(
			(TableColumn.CellDataFeatures<NameCluster, String> cdf) -> {
				NameCluster nc = cdf.getValue();
				
				// There might be columns found in some dataset but not in others
				// so we detect those cases here and put in "NA"s instead.
				String output = "NA";
				if(precalc.contains(nc, colName))
					output = precalc.get(nc, colName).stream().collect(Collectors.joining("; "));
				
				return new ReadOnlyStringWrapper(output);
			}
		);
		column.setPrefWidth(100.0);
		column.setEditable(false);
		return column;
	}
	
	private void reconcileData() {
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
	
	private Set<String> getOneElementSet(String str) {
		HashSet hs = new HashSet<>();
		hs.add(str);
		return hs;
	}
		
	private void reconcileDataFromOneDataset() {
		Project project = dataReconciliatorView.getProjectView().getProject();
		String reconciliationMethod = reconcileUsingComboBox.getValue();
		Table<NameCluster, String, Set<String>> precalc = HashBasedTable.create();
		
		Dataset namesDataset = useNamesFromComboBox.getSelectionModel().getSelectedItem();
		List<NameCluster> nameClusters = null;
		boolean flag_nameClustersAreTaxonConcepts = false;
		switch(reconciliationMethod) {
			case RECONCILE_BY_NAME:
				LOGGER.log(Level.SEVERE, "Reconciliation method ''{0}'' has not yet been implemented!", reconciliationMethod);
				return;
			
			case RECONCILE_BY_SPECIES_NAME_CLUSTER:
				if(namesDataset == ALL)
					// TODO This is either weird or wrong. Please fix!
					nameClusters = project.getNameClusterManager().getSpeciesClusters().collect(Collectors.toList());
				else
					nameClusters = project.getNameClusterManager().getClusters(namesDataset.getRecognizedNames(project).filter(n -> n.hasSpecificEpithet()).flatMap(n -> n.asBinomial()).distinct().collect(Collectors.toList()));
				
				break;
				
			case RECONCILE_BY_NAME_CLUSTER:
				if(namesDataset == ALL)
					nameClusters = project.getNameClusterManager().getClusters().collect(Collectors.toList());
				else
					nameClusters = project.getNameClusterManager().getClusters(namesDataset.getRecognizedNames(project).collect(Collectors.toList()));
				
				break;	
				
			case RECONCILE_BY_SPECIES_TAXON_CONCEPT:
				if(namesDataset == ALL)
					nameClusters = project.getNameClusterManager().getSpeciesClusters().flatMap(cl -> cl.getTaxonConcepts(project).stream()).collect(Collectors.toList());
				else
					nameClusters = project.getNameClusterManager().getClusters(
						namesDataset.getRecognizedNames(project).filter(n -> n.hasSpecificEpithet())
						.flatMap(n -> n.asBinomial()).distinct()
						.collect(Collectors.toList())
					).stream().flatMap(cl -> cl.getTaxonConcepts(project).stream()).collect(Collectors.toList());
				
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
		dataTableView.setItems(FXCollections.observableList(nameClusters));
		
		// What columns do we have from the other dataset?
		Dataset dataDataset = includeDataFromComboBox.getSelectionModel().getSelectedItem();
		List<Dataset> datasets = null;
		if(dataDataset == ALL)
			datasets = project.getDatasets();
		else
			datasets = Arrays.asList(dataDataset);
		
		// Precalculate.
		List<String> existingColNames = new ArrayList<>();
		existingColNames.add("id");
		existingColNames.add("name");
		existingColNames.add("names_in_dataset");		
		existingColNames.add("all_names_in_cluster");
		
		// If these are taxon concepts, there's three other columns we want
		// to emit.
		if(flag_nameClustersAreTaxonConcepts) {
			existingColNames.add("name_cluster_id");
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
		
		Set<Name> recognizedNamesInDataset = namesDataset.getRecognizedNames(project).collect(Collectors.toSet());
		
		for(NameCluster cluster: nameClusters) {
			precalc.put(cluster, "id", getOneElementSet(cluster.getId().toString()));
			
			// Okay, here's what we need to do:
			//	- If names is ALL, then we can't do better than cluster.getName().
			if(namesDataset == ALL) {
				precalc.put(cluster, "names_in_dataset",  cluster.getNames().stream().map(n -> n.getFullName()).collect(Collectors.toSet()));
				precalc.put(cluster, "name", getOneElementSet(cluster.getName().getFullName()));	
			} else {
				// hey, here's something cool we can do: figure out which name(s)
				// this dataset uses from this cluster!
				List<String> namesInDataset = cluster.getNames().stream()
					.filter(n -> recognizedNamesInDataset.contains(n))
					.map(n -> n.getFullName())
					.collect(Collectors.toList());
				String firstName = "";
				
				if(namesInDataset.size() > 0) {
					firstName = namesInDataset.get(0);
				} else {
					LOGGER.warning("Cluster " + cluster + " has names " + cluster.getNames() + " but no recognized name for " + namesDataset);
					
					// This happens in some taxon concepts.
					firstName = "(not found in dataset)";
				}
				
				precalc.put(cluster, "names_in_dataset", new HashSet<>(namesInDataset));
				precalc.put(cluster, "name", getOneElementSet(firstName));				
			}
			
			precalc.put(cluster, "all_names_in_cluster", cluster.getNames().stream().map(n -> n.getFullName()).collect(Collectors.toSet()));			
			
			// If it's a taxon concept, precalculate a few more columns.
			if(flag_nameClustersAreTaxonConcepts) {
				TaxonConcept tc = (TaxonConcept) cluster;
				
				precalc.put(cluster, "name_cluster_id", getOneElementSet(tc.getNameCluster().getId().toString()));
				precalc.put(cluster, "starts_with", tc.getStartsWith().stream().map(ch -> ch.toString()).collect(Collectors.toSet()));
				precalc.put(cluster, "ends_with", tc.getEndsWith().stream().map(ch -> ch.toString()).collect(Collectors.toSet()));
				precalc.put(cluster, "is_ongoing", getOneElementSet(tc.isOngoing(project) ? "yes" : "no"));
			} else {
				// If it's a true name cluster, then perhaps people will want
				// to know what taxon concepts are in here? Maybe for some sort
				// of PhD?
				List<TaxonConcept> tcs = cluster.getTaxonConcepts(project);
				
				precalc.put(cluster, "taxon_concept_count", getOneElementSet(String.valueOf(tcs.size())));
				precalc.put(cluster, "taxon_concepts", tcs.stream().map(tc -> tc.toString()).collect(Collectors.toSet()));
			}
			
			// When was this first added?
			List<Dataset> foundInSorted = cluster.getFoundInSorted();
			if(!foundInSorted.isEmpty()) {
				precalc.put(cluster, "first_added_dataset", getOneElementSet(foundInSorted.get(0).getCitation()));
				precalc.put(cluster, "first_added_year", getOneElementSet(foundInSorted.get(0).getDate().getYearAsString()));
			}
			
			// For name clusters we can also figure out trajectories!
			if(!flag_nameClustersAreTaxonConcepts) {
				List<String> trajectorySteps = cluster.getFoundInSorted().stream().map(
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
				
				precalc.put(cluster, "trajectory", getOneElementSet(
					String.join(" -> ", trajectorySteps)
				));
				
				precalc.put(cluster, "trajectory_without_renames", getOneElementSet(
					trajectorySteps.stream().filter(ch -> !ch.contains("rename")).collect(Collectors.joining(" -> "))
				));
				
				precalc.put(cluster, "trajectory_lumps_splits", getOneElementSet(
						trajectorySteps.stream().filter(ch -> ch.contains("split") || ch.equals("lump")).collect(Collectors.joining(" -> "))	
				));
			}
			
			// Okay, here's where we reconcile!
			for(Name n: cluster.getNames()) {
				// TODO: there's probably an optimization here, in which we should
				// loop on the smaller set (either loop on 'datasets' and compare
				// to cluster, or loop on cluster.foundIn and compare to 'datasets').
				for(Dataset ds: datasets) {
					Map<Name, Set<DatasetRow>> rowsByName = ds.getRowsByName();
					
					// Are we included in this name cluster? If not, skip!
					if(!cluster.getFoundIn().contains(ds)) continue;
				
					// Check to see if we have any rows for this name; if not, skip.
					if(!rowsByName.containsKey(n)) continue;

					Set<DatasetRow> matched = rowsByName.get(n);
					// LOGGER.log(Level.FINE, "Adding {0} rows under name ''{1}''", new Object[]{matched.size(), n.getFullName()});
				
					Map<Set<DatasetColumn>, List<DatasetRow>> rowsByCols = matched.stream().collect(
						Collectors.groupingBy((DatasetRow row) -> row.getColumns())
					);
				
					for(Set<DatasetColumn> cols: rowsByCols.keySet()) {
						for(DatasetColumn col: cols) {
							String colName = col.getName();

							if(existingColNames.contains(colName))
								colName = "datasets." + colName;

							if(!precalc.contains(cluster, colName))
								precalc.put(cluster, colName, new HashSet());

							for(DatasetRow row: rowsByCols.get(cols)) {
								if(!row.hasColumn(col)) continue;

								precalc.get(cluster, colName).add(row.get(col));											
							}

							LOGGER.log(Level.FINE, "Added {0} rows under name ''{1}''", new Object[]{rowsByCols.get(cols).size(), n.getFullName()});	
						}
					}
				}
			}
		}
		
		dataTableView.getColumns().clear();
		for(String colName: existingColNames) {
			dataTableView.getColumns().add(createColumnFromPrecalc(colName, precalc));
		}
		
		// Get distinct column names.
		Stream<String> colNames = precalc.cellSet().stream().map(set -> set.getColumnKey());
		
		// Eliminate columns that are in the existingColNames.
		colNames = colNames.filter(colName -> !existingColNames.contains(colName));
		
		// And add tablecolumns for the rest.
		List<TableColumn<NameCluster, String>> cols = colNames.distinct().sorted().map(colName -> createColumnFromPrecalc(colName, precalc)).collect(Collectors.toList());
		dataTableView.getColumns().addAll(cols);
		dataTableView.refresh();
		
		// Fill in status text field.
		statusTextField.setText(dataTableView.getItems().size() + " rows across " + cols.size() + " reconciled columns");
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
		List<TableColumn<NameCluster, ?>> columns = dataTableView.getColumns();
		
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
	@FXML private ComboBox<Dataset> useNamesFromComboBox;
	@FXML private ComboBox<Dataset> includeDataFromComboBox;
	@FXML private ComboBox<String> reconcileUsingComboBox;	
	@FXML private TabPane upperTabPane;
	@FXML private TableView<NameCluster> dataTableView;
	@FXML private TextField statusTextField;
}
