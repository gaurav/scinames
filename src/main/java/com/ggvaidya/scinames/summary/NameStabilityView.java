
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
import java.util.Optional;
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
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.NameClusterManager;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.TaxonConcept;
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
 * A NameStabilityView displays name stability statistics over the course of a project.
 * It uses the TabularDataView to do this.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class NameStabilityView {
	private Logger LOGGER = Logger.getLogger(NameStabilityView.class.getSimpleName());
	private Stage stage;
	private Scene scene;
	private ProjectView projectView;
	private TabularDataViewController controller;
	
	public Stage getStage() { return stage; }

	public NameStabilityView(ProjectView pv) {
		projectView = pv;
		stage = new Stage();
		
		controller = TabularDataViewController.createTabularDataView();
		scene = controller.getScene();
		init();
		
		// Go go stagey scene.
		stage.setScene(scene);
	}
	
	private TableColumn<Dataset, String> createTableColumn(String colName, Function<Dataset, String> func) {
		TableColumn<Dataset, String> tableColumn = new TableColumn<>(colName);
		tableColumn.setCellValueFactory(features -> new ReadOnlyStringWrapper(func.apply(features.getValue())));
		
		tableColumn.setPrefWidth(100.0);
		
		return tableColumn;
	}
	
	private TableColumn<Dataset, String> createTableColumnFromPrecalc(Table<Dataset, String, String> precalc, String colName) {
		return createTableColumn(colName, ds -> precalc.get(ds, colName));
	}
	
	public void init() {
		Project project = projectView.getProject();
		
		// Setup stage.
		stage.setTitle("Name stability between " + project.getDatasets().size() + " datasets");
		
		// Setup table.
		controller.getTableEditableProperty().set(false);
		//controller.setTableColumnResizeProperty(TableView.CONSTRAINED_RESIZE_POLICY);
		ObservableList<TableColumn> cols = controller.getTableColumnsProperty();
		cols.clear();
		
		// Precalculating.
		Table<Dataset, String, String> precalc = HashBasedTable.create();
		
		// Set up columns.
		cols.add(createTableColumnFromPrecalc(precalc, "dataset"));
		cols.add(createTableColumnFromPrecalc(precalc, "date"));
		cols.add(createTableColumnFromPrecalc(precalc, "year"));
		cols.add(createTableColumnFromPrecalc(precalc, "count_binomial"));
		cols.add(createTableColumnFromPrecalc(precalc, "count_genera"));
		cols.add(createTableColumnFromPrecalc(precalc, "count_monotypic_genera"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_added"));
		//cols.add(createTableColumnFromPrecalc(precalc, "names_added_list"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_deleted"));
		//cols.add(createTableColumnFromPrecalc(precalc, "names_deleted_list"));
		cols.add(createTableColumnFromPrecalc(precalc, "species_added"));
		//cols.add(createTableColumnFromPrecalc(precalc, "species_added_list"));
		cols.add(createTableColumnFromPrecalc(precalc, "species_deleted"));
		//cols.add(createTableColumnFromPrecalc(precalc, "species_deleted_list"));
		cols.add(createTableColumnFromPrecalc(precalc, "mean_binomials_per_genera"));
		cols.add(createTableColumnFromPrecalc(precalc, "median_binomials_per_genera"));
		cols.add(createTableColumnFromPrecalc(precalc, "mode_binomials_per_genera_list"));
		
		/* All them stability calculations */
		
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_prev"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_prev_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_prev_pc_union"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_prev_pc_prev"));	
		
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_prev"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_prev_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_prev_pc_union"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_prev_pc_prev"));
		
		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_prev"));
		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_prev_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_prev_pc_union"));
		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_prev_pc_prev"));
		
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_next"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_next_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_next_pc_union"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_next_pc_next"));
		
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_next"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_next_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_next_pc_union"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_next_pc_next"));
		
		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_next"));
		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_next_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_next_pc_union"));
		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_next_pc_next"));
		
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_first"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_first_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_first_pc_union"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_first_pc_first"));		
		
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_first"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_first_pc_this"));	
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_first_pc_union"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_first_pc_first"));
		
		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_first"));
		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_first_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_first_pc_union"));
		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_first_pc_first"));
		
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_last"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_last_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_last_pc_union"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_last_pc_last"));
		
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_last"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_last_pc_this"));	
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_last_pc_union"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_last_pc_last"));

		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_last"));
		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_last_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_last_pc_union"));
		cols.add(createTableColumnFromPrecalc(precalc, "circumscriptions_identical_to_last_pc_last"));
		
		Set<String> recognitionColumns = new HashSet<>();

		// Calculate binomials per dataset.
		Map<Name, Set<Dataset>> datasetsPerName = new HashMap<>();
		
		// Prepare to loop!
		List<Dataset> checklists = project.getChecklists();
		
		// BIRD HACK! Include all datasets!
		// checklists = project.getDatasets();
		
		// Set table items. We're only interested in checklists, because
		// there's no such thing as "name stability" between non-checklist datasets.
		controller.getTableItemsProperty().set(
			FXCollections.observableArrayList(checklists)
		);
		
		List<Dataset> prevChecklists = new LinkedList<>();
		Dataset firstChecklist = checklists.get(0);
		Dataset lastChecklist = checklists.get(checklists.size() - 1);
		
		// TODO: This used to be prevDataset, but prevChecklist makes a lot more sense, since we
		// want to compare checklists with each other, ignoring datasets. Would be nice if someone
		// with copious free time could look over the calculations and make sure they don't assume
		// that the previous checklist is also the previous dataset?
		Dataset prevChecklist = null;
		
		int index = -1;
		for(Dataset ds: checklists) {
			index++;
			
			Dataset nextChecklist = (index < (checklists.size() - 1) ? checklists.get(index + 1) : null);
			
			precalc.put(ds, "dataset", ds.getName());
			precalc.put(ds, "date", ds.getDate().asYYYYmmDD("-"));
			precalc.put(ds, "year", ds.getDate().getYearAsString());
						
			Set<Name> recognizedBinomials = project.getRecognizedNames(ds).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toSet());
			precalc.put(ds, "count_binomial", String.valueOf(recognizedBinomials.size()));
			
			Set<Name> recognizedGenera = recognizedBinomials.stream().flatMap(n -> n.asGenus()).collect(Collectors.toSet());
			precalc.put(ds, "count_genera", String.valueOf(recognizedGenera.size()));
			precalc.put(ds, "mean_binomials_per_genera", new BigDecimal(((double)recognizedBinomials.size())/recognizedGenera.size()).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
			
			Map<Name, List<Name>> countBinomialsPerGenus = recognizedBinomials.stream()
				// Eliminate names that have zero (or more than one?!) genus name.
				.filter(n -> (n.asGenus().count() == 1))
				.collect(
					Collectors.groupingBy(n -> n.asGenus().findAny().get())
				);
			
			/*
			LOGGER.info("Debugging: list of " + recognizedGenera.size() + " genera: " + 
				recognizedGenera.stream().map(n -> n.getFullName()).collect(Collectors.joining(", "))
			);
			*/
			
			precalc.put(ds, "count_monotypic_genera", 
				String.valueOf(
					countBinomialsPerGenus.entrySet().stream()
						.filter(entry -> new HashSet<>(entry.getValue()).size() == 1)
						.count()
				)
			);
			
			/*
			LOGGER.info("Debugging: list of monotypic genera: " + 
				countBinomialsPerGenus.entrySet().stream()
					.filter(entry -> new HashSet<>(entry.getValue()).size() == 1)
					.map(entry -> entry.getKey().getFullName())
					.collect(Collectors.joining(", "))
			);
			*/
			
			// Species added and deleted
			Set<Name> namesAdded = ds.getChanges(project).filter(ch -> ch.getType().equals(ChangeType.ADDITION)).flatMap(ch -> ch.getToStream()).collect(Collectors.toSet());
			Set<Name> namesDeleted = ds.getChanges(project).filter(ch -> ch.getType().equals(ChangeType.DELETION)).flatMap(ch -> ch.getFromStream()).collect(Collectors.toSet());
			
			// TODO: This isn't so useful -- the more useful measure would be the number of all species added
			// and all species deleted, making sure there isn't a cluster-al overlap.
			precalc.put(ds, "names_added", String.valueOf(namesAdded.size()));
			//precalc.put(ds, "names_added_list", namesAdded.stream().sorted().map(n -> n.getFullName()).collect(Collectors.joining(", ")));
			precalc.put(ds, "names_deleted", String.valueOf(namesDeleted.size()));
			//precalc.put(ds, "names_deleted_list", namesDeleted.stream().sorted().map(n -> n.getFullName()).collect(Collectors.joining(", ")));

			// Eliminate names that have been added, but were previously recognized at the species level.
			Set<Name> speciesAdded = namesAdded;
			if(prevChecklist != null) {
				Set<Name> prevRecognizedNames = project.getNameClusterManager().getClusters(project.getRecognizedNames(prevChecklist)).stream().flatMap(nc -> nc.getNames().stream()).collect(Collectors.toSet());
				speciesAdded = namesAdded.stream().filter(n -> !prevRecognizedNames.contains(n)).collect(Collectors.toSet());
			}
			
			// Eliminate names that are still represented in the checklist by a species cluster.
			// (Note that this includes cases where a subspecies is removed, but another subspecies
			// or the nominal species is still recognized!)
			Set<Name> currentlyRecognizedBinomialNames = project.getNameClusterManager().getClusters(project.getRecognizedNames(ds)).stream().flatMap(nc -> nc.getNames().stream()).flatMap(n -> n.asBinomial()).collect(Collectors.toSet());
			Set<Name> speciesDeleted = namesDeleted.stream().filter(n -> !n.asBinomial().anyMatch(bn -> currentlyRecognizedBinomialNames.contains(bn))).collect(Collectors.toSet());
			
			precalc.put(ds, "species_added", String.valueOf(speciesAdded.size()));
			precalc.put(ds, "species_added_list", speciesAdded.stream().sorted().map(n -> n.getFullName()).collect(Collectors.joining(", ")));
			precalc.put(ds, "species_deleted", String.valueOf(speciesDeleted.size()));
			precalc.put(ds, "species_deleted_list", speciesDeleted.stream().sorted().map(n -> n.getFullName()).collect(Collectors.joining(", ")));
			
			// Measures of species per genera
			java.util.Map<String, Set<Name>> binomialsPerGenera = recognizedBinomials.stream().collect(
				Collectors.toMap(
					n -> n.getGenus(),
					n -> { Set<Name> set = new HashSet<Name>(); set.add(n); return set; },
					(a, b) -> { a.addAll(b); return a; }
				)
			);
			
			List<Integer> binomialsPerGeneraCounts = binomialsPerGenera.values().stream().map(set -> set.size()).sorted().collect(Collectors.toList());
			
			Frequency freq = new Frequency();
			for(String genus: binomialsPerGenera.keySet()) {
				// Blech.
				for(Name binom: binomialsPerGenera.get(genus)) {
					freq.addValue(genus);
				}
			}
			List<Comparable<?>> modeGenera = freq.getMode();
			precalc.put(ds, "mode_binomials_per_genera_list", modeGenera.stream()
				.map(o -> o.toString() + ": " + freq.getCount(o) + " binomials")
				.collect(Collectors.joining("; ")));
			
			double[] binomialsPerGeneraCountsAsDouble = binomialsPerGeneraCounts.stream().mapToDouble(Integer::doubleValue).toArray();
			Median median = new Median();
			precalc.put(ds, "median_binomials_per_genera", String.valueOf(median.evaluate(binomialsPerGeneraCountsAsDouble)));

			if(firstChecklist == null) {
//				precalc.put(ds, "names_identical_to_first", "NA");
//				precalc.put(ds, "names_identical_to_first_pc", "NA");
			} else {
				precalc.put(ds, "names_identical_to_first", String.valueOf(getBinomialNamesIntersection(project, ds, firstChecklist).size()));
				precalc.put(ds, "names_identical_to_first_pc_this", new BigDecimal((double)getBinomialNamesIntersection(project, ds, firstChecklist).size()/recognizedBinomials.size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "names_identical_to_first_pc_union", new BigDecimal((double)getBinomialNamesIntersection(project, ds, firstChecklist).size()/getBinomialNamesUnion(project, ds, firstChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "names_identical_to_first_pc_first", new BigDecimal((double)getBinomialNamesIntersection(project, ds, firstChecklist).size()/getBinomialNamesUnion(project, firstChecklist, firstChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());

				int clustersForDataset = project.getNameClusterManager().getClusters(recognizedBinomials).size();
				if(clustersForDataset != recognizedBinomials.size()) {
					throw new RuntimeException("We have " + clustersForDataset + " clusters for this dataset, but " + recognizedBinomials.size() + " recognized binomials. What?");
				}
				precalc.put(ds, "clusters_identical_to_first", String.valueOf(getBinomialClustersIntersection(project, ds, firstChecklist).size()));
				precalc.put(ds, "clusters_identical_to_first_pc_this", new BigDecimal((double)getBinomialClustersIntersection(project, ds, firstChecklist).size()/getBinomialClustersUnion(project, ds, ds).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "clusters_identical_to_first_pc_union", new BigDecimal((double)getBinomialClustersIntersection(project, ds, firstChecklist).size()/getBinomialClustersUnion(project, ds, firstChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "clusters_identical_to_first_pc_first", new BigDecimal((double)getBinomialClustersIntersection(project, ds, firstChecklist).size()/getBinomialClustersUnion(project, firstChecklist, firstChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				
				precalc.put(ds, "circumscriptions_identical_to_first", String.valueOf(getBinomialTaxonConceptsIntersection(project, ds, firstChecklist).size()));
				precalc.put(ds, "circumscriptions_identical_to_first_pc_this", new BigDecimal((double)getBinomialTaxonConceptsIntersection(project, ds, firstChecklist).size()/getBinomialTaxonConceptsUnion(project, ds, ds).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "circumscriptions_identical_to_first_pc_union", new BigDecimal((double)getBinomialTaxonConceptsIntersection(project, ds, firstChecklist).size()/getBinomialTaxonConceptsUnion(project, ds, firstChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "circumscriptions_identical_to_first_pc_first", new BigDecimal((double)getBinomialTaxonConceptsIntersection(project, ds, firstChecklist).size()/getBinomialTaxonConceptsUnion(project, firstChecklist, firstChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
			}
			
			if(lastChecklist == null) {
//				precalc.put(ds, "names_identical_to_first", "NA");
//				precalc.put(ds, "names_identical_to_first_pc", "NA");
			} else {
				precalc.put(ds, "names_identical_to_last", String.valueOf(getBinomialNamesIntersection(project, ds, lastChecklist).size()));
				precalc.put(ds, "names_identical_to_last_pc_this", new BigDecimal((double)getBinomialNamesIntersection(project, ds, lastChecklist).size()/recognizedBinomials.size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "names_identical_to_last_pc_union", new BigDecimal((double)getBinomialNamesIntersection(project, ds, lastChecklist).size()/getBinomialNamesUnion(project, ds, lastChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "names_identical_to_last_pc_last", new BigDecimal((double)getBinomialNamesIntersection(project, ds, lastChecklist).size()/getBinomialNamesUnion(project, lastChecklist, lastChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());

				int clustersForDataset = project.getNameClusterManager().getClusters(recognizedBinomials).size();
				if(clustersForDataset != recognizedBinomials.size()) {
					throw new RuntimeException("We have " + clustersForDataset + " clusters for this dataset, but " + recognizedBinomials.size() + " recognized binomials. What?");
				}
				precalc.put(ds, "clusters_identical_to_last", String.valueOf(getBinomialClustersIntersection(project, ds, lastChecklist).size()));
				precalc.put(ds, "clusters_identical_to_last_pc_this", new BigDecimal((double)getBinomialClustersIntersection(project, ds, lastChecklist).size()/getBinomialClustersUnion(project, ds, ds).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "clusters_identical_to_last_pc_union", new BigDecimal((double)getBinomialClustersIntersection(project, ds, lastChecklist).size()/getBinomialClustersUnion(project, ds, lastChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "clusters_identical_to_last_pc_last", new BigDecimal((double)getBinomialClustersIntersection(project, ds, lastChecklist).size()/getBinomialClustersUnion(project, lastChecklist, lastChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
			
				precalc.put(ds, "circumscriptions_identical_to_last", String.valueOf(getBinomialTaxonConceptsIntersection(project, ds, lastChecklist).size()));
				precalc.put(ds, "circumscriptions_identical_to_last_pc_this", new BigDecimal((double)getBinomialTaxonConceptsIntersection(project, ds, lastChecklist).size()/getBinomialTaxonConceptsUnion(project, ds, ds).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "circumscriptions_identical_to_last_pc_union", new BigDecimal((double)getBinomialTaxonConceptsIntersection(project, ds, lastChecklist).size()/getBinomialTaxonConceptsUnion(project, ds, lastChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "circumscriptions_identical_to_last_pc_last", new BigDecimal((double)getBinomialTaxonConceptsIntersection(project, ds, lastChecklist).size()/getBinomialTaxonConceptsUnion(project, lastChecklist, lastChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
			}
			
			if(prevChecklist == null) {
//				precalc.put(ds, "names_identical_to_prev", "NA");
//				precalc.put(ds, "names_identical_to_prev_pc", "NA");				
			} else {
				precalc.put(ds, "names_identical_to_prev", String.valueOf(getBinomialNamesIntersection(project, ds, prevChecklist).size()));
				precalc.put(ds, "names_identical_to_prev_pc_this", new BigDecimal((double)getBinomialNamesIntersection(project, ds, prevChecklist).size()/recognizedBinomials.size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "names_identical_to_prev_pc_union", new BigDecimal((double)getBinomialNamesIntersection(project, ds, prevChecklist).size()/getBinomialNamesUnion(project, ds, prevChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "names_identical_to_prev_pc_prev", new BigDecimal((double)getBinomialNamesIntersection(project, ds, prevChecklist).size()/getBinomialNamesUnion(project, prevChecklist, prevChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				
				int clustersForDataset = project.getNameClusterManager().getClusters(recognizedBinomials).size();
				if(clustersForDataset != recognizedBinomials.size()) {
					throw new RuntimeException("We have " + clustersForDataset + " clusters for this dataset, but " + recognizedBinomials.size() + " recognized binomials. What?");
				}
				precalc.put(ds, "clusters_identical_to_prev", String.valueOf(getBinomialClustersIntersection(project, ds, prevChecklist).size()));
				precalc.put(ds, "clusters_identical_to_prev_pc_this", new BigDecimal((double)getBinomialClustersIntersection(project, ds, prevChecklist).size()/getBinomialClustersUnion(project, ds, ds).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "clusters_identical_to_prev_pc_union", new BigDecimal((double)getBinomialClustersIntersection(project, ds, prevChecklist).size()/getBinomialClustersUnion(project, ds, prevChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "clusters_identical_to_prev_pc_prev", new BigDecimal((double)getBinomialClustersIntersection(project, ds, prevChecklist).size()/getBinomialClustersUnion(project, prevChecklist, prevChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				
				precalc.put(ds, "circumscriptions_identical_to_prev", String.valueOf(getBinomialTaxonConceptsIntersection(project, ds, prevChecklist).size()));
				precalc.put(ds, "circumscriptions_identical_to_prev_pc_this", new BigDecimal((double)getBinomialTaxonConceptsIntersection(project, ds, prevChecklist).size()/getBinomialTaxonConceptsUnion(project, ds, ds).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds,  "circumscriptions_identical_to_prev_pc_union", new BigDecimal((double)getBinomialTaxonConceptsIntersection(project, ds, prevChecklist).size()/getBinomialTaxonConceptsUnion(project, ds, prevChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds,  "circumscriptions_identical_to_prev_pc_prev", new BigDecimal((double)getBinomialTaxonConceptsIntersection(project, ds, prevChecklist).size()/getBinomialTaxonConceptsUnion(project, prevChecklist, prevChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				
				// FYI, getBinomialTaxonConceptsUnion(project, ds, prevChecklist).size() should always be equal to the number of species in the dataset.
			}
		
			if(nextChecklist == null) {
	//			precalc.put(ds, "names_identical_to_prev", "NA");
	//			precalc.put(ds, "names_identical_to_prev_pc", "NA");				
			} else {
				precalc.put(ds, "names_identical_to_next", String.valueOf(getBinomialNamesIntersection(project, ds, nextChecklist).size()));
				precalc.put(ds, "names_identical_to_next_pc_this", new BigDecimal((double)getBinomialNamesIntersection(project, ds, nextChecklist).size()/recognizedBinomials.size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "names_identical_to_next_pc_union", new BigDecimal((double)getBinomialNamesIntersection(project, ds, nextChecklist).size()/getBinomialNamesUnion(project, ds, nextChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "names_identical_to_next_pc_next", new BigDecimal((double)getBinomialNamesIntersection(project, ds, nextChecklist).size()/getBinomialNamesUnion(project, nextChecklist, nextChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				
				int clustersForDataset = project.getNameClusterManager().getClusters(recognizedBinomials).size();
				if(clustersForDataset != recognizedBinomials.size()) {
					throw new RuntimeException("We have " + clustersForDataset + " clusters for this dataset, but " + recognizedBinomials.size() + " recognized binomials. What?");
				}
				precalc.put(ds, "clusters_identical_to_next", String.valueOf(getBinomialClustersIntersection(project, ds, nextChecklist).size()));
				precalc.put(ds, "clusters_identical_to_next_pc_this", new BigDecimal((double)getBinomialClustersIntersection(project, ds, nextChecklist).size()/getBinomialClustersUnion(project, ds, ds).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "clusters_identical_to_next_pc_union", new BigDecimal((double)getBinomialClustersIntersection(project, ds, nextChecklist).size()/getBinomialClustersUnion(project, ds, nextChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "clusters_identical_to_next_pc_next", new BigDecimal((double)getBinomialClustersIntersection(project, ds, nextChecklist).size()/getBinomialClustersUnion(project, nextChecklist, nextChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
			
				precalc.put(ds, "circumscriptions_identical_to_next", String.valueOf(getBinomialTaxonConceptsIntersection(project, ds, nextChecklist).size()));
				precalc.put(ds, "circumscriptions_identical_to_next_pc_this", new BigDecimal((double)getBinomialTaxonConceptsIntersection(project, ds, nextChecklist).size()/getBinomialTaxonConceptsUnion(project, ds, ds).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "circumscriptions_identical_to_next_pc_union", new BigDecimal((double)getBinomialTaxonConceptsIntersection(project, ds, nextChecklist).size()/getBinomialTaxonConceptsUnion(project, ds, nextChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "circumscriptions_identical_to_next_pc_next", new BigDecimal((double)getBinomialTaxonConceptsIntersection(project, ds, nextChecklist).size()/getBinomialTaxonConceptsUnion(project, nextChecklist, nextChecklist).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
			}
			
			// For the visualization thingie.
			int total = prevChecklists.size();
			List<Integer> counts = new LinkedList<>();
			for(Name name: recognizedBinomials) {
				int prevRecognized = 0;
				
				if(!datasetsPerName.containsKey(name)) {
					datasetsPerName.put(name, new HashSet<>());
				} else {
					prevRecognized = datasetsPerName.get(name).size();
				}
				
				datasetsPerName.get(name).add(ds);
				counts.add(
					(int)(
						((double)prevRecognized)/total*100
					)
				);
			}
			
			Map<Integer, List<Integer>> countsByPercentage = counts.stream().sorted().collect(Collectors.groupingBy(n -> (int)(n/10)*10));
			for(int percentage: countsByPercentage.keySet()) {
				precalc.put(ds, "previously_recognized_" + percentage + "pc", String.valueOf(countsByPercentage.get(percentage).size()));	
				recognitionColumns.add("previously_recognized_" + percentage + "pc");
			}
			prevChecklists.add(ds);
			
			// Set up the previous checklist for the next loop.
			prevChecklist = ds;
		}
		
		LinkedList<String> recognitionColumnsList = new LinkedList<>(recognitionColumns);
		recognitionColumnsList.sort(null);		
		for(String colName: recognitionColumnsList) {
			cols.add(createTableColumnFromPrecalc(precalc, colName));
		}
	}
	
	private Set<Name> getBinomialNamesIntersection(Project p, Dataset ds1, Dataset ds2) {
		Set<Name> recog1 = p.getRecognizedNames(ds1).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toSet());
		Set<Name> recog2 = p.getRecognizedNames(ds2).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toSet());
		
		return recog1.stream().filter(n -> recog2.contains(n)).collect(Collectors.toSet());
	}
	
	private Set<Name> getBinomialNamesUnion(Project p, Dataset ds1, Dataset ds2) {
		Set<Name> recog1 = p.getRecognizedNames(ds1).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toSet());
		Set<Name> recog2 = p.getRecognizedNames(ds2).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toSet());
		
		recog1.addAll(recog2);
		
		return recog1;
	}
	
	private Set<NameCluster> getBinomialClustersIntersection(Project p, Dataset ds1, Dataset ds2) {
		NameClusterManager ncm = p.getNameClusterManager();
		
		Set<NameCluster> clusters1 = new HashSet<>(ncm.getClusters(p.getRecognizedNames(ds1).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toList())));
		Set<NameCluster> clusters2 = new HashSet<>(ncm.getClusters(p.getRecognizedNames(ds2).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toList())));
		
		//if(ds1 == ds2)
		//	LOGGER.info("Present in " + ds1 + " but not in " + ds2 + ": " + clusters1.stream().filter(c -> !clusters2.contains(c)).map(c -> c.toString()).collect(Collectors.joining("; ")));
		
		return clusters1.stream().filter(c -> clusters2.contains(c)).collect(Collectors.toSet());
	}
	
	private Set<NameCluster> getBinomialClustersUnion(Project p, Dataset ds1, Dataset ds2) {
		NameClusterManager ncm = p.getNameClusterManager();
		
		Set<NameCluster> clusters1 = new HashSet<>(ncm.getClusters(p.getRecognizedNames(ds1).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toList())));
		Set<NameCluster> clusters2 = new HashSet<>(ncm.getClusters(p.getRecognizedNames(ds2).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toList())));
		
		clusters1.addAll(clusters2);
	
		return clusters1;
	}
	
	// TODO: sleepy, tired, stressed! Please recheck!
	private Map<NameCluster, List<TaxonConcept>> nameClusterTaxonConceptCache = new HashMap<>();
	private Map<Dataset, Set<TaxonConcept>> taxonConceptsByDataset = new HashMap<>(); 
	private Set<TaxonConcept> getTaxonConceptsForDataset(Project p, Dataset ds) {
		if(taxonConceptsByDataset.containsKey(ds)) return new HashSet<>(taxonConceptsByDataset.get(ds));
		
		LOGGER.info("Starting getTaxonConceptsForDataset(" + p + ", " + ds + ")");
		NameClusterManager ncm = p.getNameClusterManager();
		
		// Get all binomial name clusters
		List<NameCluster> nameClusterStream = ncm.getClusters(
			p.getRecognizedNames(ds).stream()
				.flatMap(n -> n.asBinomial())
				.distinct()
				.collect(Collectors.toList())
		);
		
		LOGGER.info("Starting name cluster to taxon concept conversion");
		
		// Get the corresponding taxon concepts, caching them as we go.
		List<TaxonConcept> taxonConceptStream = nameClusterStream.stream()
			.flatMap(nc -> {
				if(nameClusterTaxonConceptCache.containsKey(nc)) return nameClusterTaxonConceptCache.get(nc).stream();
				
				List<TaxonConcept> tcs = nc.getTaxonConcepts(p);
				nameClusterTaxonConceptCache.put(nc, tcs);
				
				return tcs.stream();
			})
			.distinct()
			.collect(Collectors.toList());
		
		LOGGER.info("Finished name cluster to taxon concept conversion");
		
		/*
		// BIRD HACK!
		List<Dataset> allChecklists = projectView.getProject().getDatasets();
		int dsIndex = allChecklists.indexOf(ds);
		*/
		
		// Finally, this includes taxon concepts that don't apply to this dataset.
		// So: we filter it down here!
		Set<TaxonConcept> taxonConcepts = taxonConceptStream.stream()
			.filter(tc -> tc.getFoundIn().contains(ds))
			
			/*
			// BIRD HACK! getFoundIn only includes datasets in which the name
			// was actually noticed, which means that "in between" checklists
			// don't appear to count. So instead we'll approximate with ranges.
			.filter(tc -> {
				List<Dataset> foundIn = tc.getFoundInSorted();
				if(foundIn.isEmpty()) return false;
				
				int foundInMin = allChecklists.indexOf(foundIn.get(0));
				int foundInMax = allChecklists.indexOf(foundIn.get(foundIn.size() - 1));
				
				// Is "this" checklist inside that range?
				if(foundInMin <= dsIndex && dsIndex <= foundInMax) {
					// yay, inside that range!
					return true;
				} else
					return false;
			})
			*/
			
			.collect(Collectors.toSet());
		
		LOGGER.info("Finished getTaxonConceptsForDataset(" + p + ", " + ds + ")");
		
		taxonConceptsByDataset.put(ds, taxonConcepts);
		
		// For chrissake don't return the actual HashSet otherwise we're going to keep
		// accumulating taxon concepts like some kind of crazy person.
		return new HashSet<>(taxonConcepts);
	}
	
	private Set<TaxonConcept> getBinomialTaxonConceptsIntersection(Project p, Dataset ds1, Dataset ds2) {
		Set<TaxonConcept> clusters1 = getTaxonConceptsForDataset(p, ds1);
		Set<TaxonConcept> clusters2 = getTaxonConceptsForDataset(p, ds2);
		
		return clusters1.stream().filter(c -> clusters2.contains(c)).collect(Collectors.toSet());
	}
	
	private Set<TaxonConcept> getBinomialTaxonConceptsUnion(Project p, Dataset ds1, Dataset ds2) {
		Set<TaxonConcept> clusters1 = getTaxonConceptsForDataset(p, ds1);
		Set<TaxonConcept> clusters2 = getTaxonConceptsForDataset(p, ds2);
		
		HashSet<TaxonConcept> combined = new HashSet<>(clusters1); 
		combined.addAll(clusters2);
	
		return combined;
	}
}
