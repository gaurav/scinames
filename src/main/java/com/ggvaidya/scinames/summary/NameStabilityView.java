
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
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.rank.Median;

import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.NameClusterManager;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.project.ProjectView;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import javafx.beans.property.ReadOnlyStringWrapper;
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
		// TODO: is it any faster if we do Table<String, Dataset, String>?
		Table<Dataset, String, String> precalc = HashBasedTable.create();
		
		// Set up columns.
		cols.add(createTableColumnFromPrecalc(precalc, "dataset"));
		cols.add(createTableColumnFromPrecalc(precalc, "date"));
		cols.add(createTableColumnFromPrecalc(precalc, "year"));
		cols.add(createTableColumnFromPrecalc(precalc, "count_binomial"));
		cols.add(createTableColumnFromPrecalc(precalc, "count_genera"));
		cols.add(createTableColumnFromPrecalc(precalc, "mean_binomials_per_genera"));
		cols.add(createTableColumnFromPrecalc(precalc, "median_binomials_per_genera"));
		cols.add(createTableColumnFromPrecalc(precalc, "mode_binomials_per_genera_list"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_prev"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_prev_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_prev_pc_union"));		
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_prev"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_prev_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_prev_pc_union"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_next"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_next_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_next_pc_union"));		
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_next"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_next_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_next_pc_union"));	
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_first"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_first_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_first_pc_union"));		
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_first"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_first_pc_this"));	
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_first_pc_union"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_last"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_last_pc_this"));
		cols.add(createTableColumnFromPrecalc(precalc, "names_identical_to_last_pc_union"));		
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_last"));
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_last_pc_this"));	
		cols.add(createTableColumnFromPrecalc(precalc, "clusters_identical_to_last_pc_union"));		
		
		Dataset firstDataset = project.getDatasets().get(0);
		Dataset lastDataset = project.getDatasets().get(project.getDatasets().size() - 1);
		int index = -1;
		for(Dataset ds: project.getDatasets()) {
			index++;
			
			Dataset prevDataset = ds.getPreviousDataset();
			Dataset nextDataset = (index < (project.getDatasets().size() - 1) ? project.getDatasets().get(index + 1) : null);
			
			precalc.put(ds, "dataset", ds.getName());
			precalc.put(ds, "date", ds.getDate().toString());
			precalc.put(ds, "year", ds.getDate().getYearAsString());
			
			Set<Name> recognizedBinomials = project.getRecognizedNames(ds).stream().flatMap(n -> n.asBinomial()).collect(Collectors.toSet());
			precalc.put(ds, "count_binomial", String.valueOf(recognizedBinomials.size()));
		
			Set<Name> recognizedGenera = recognizedBinomials.stream().flatMap(n -> n.asGenus()).collect(Collectors.toSet());
			precalc.put(ds, "count_genera", String.valueOf(recognizedGenera.size()));
			precalc.put(ds, "mean_binomials_per_genera", new BigDecimal(((double)recognizedBinomials.size())/recognizedGenera.size()).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
			
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

			if(firstDataset == null) {
//				precalc.put(ds, "names_identical_to_first", "NA");
//				precalc.put(ds, "names_identical_to_first_pc", "NA");
			} else {
				precalc.put(ds, "names_identical_to_first", String.valueOf(getBinomialNamesIntersection(project, ds, firstDataset).size()));
				precalc.put(ds, "names_identical_to_first_pc_this", new BigDecimal((double)getBinomialNamesIntersection(project, ds, firstDataset).size()/recognizedBinomials.size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "names_identical_to_first_pc_union", new BigDecimal((double)getBinomialNamesIntersection(project, ds, firstDataset).size()/getBinomialNamesUnion(project, ds, firstDataset).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());

				int clustersForDataset = project.getNameClusterManager().getClusters(recognizedBinomials).size();
				if(clustersForDataset != recognizedBinomials.size()) {
					throw new RuntimeException("We have " + clustersForDataset + " clusters for this dataset, but " + recognizedBinomials.size() + " recognized binomials. What?");
				}
				precalc.put(ds, "clusters_identical_to_first", String.valueOf(getBinomialClustersIntersection(project, ds, firstDataset).size()));
				precalc.put(ds, "clusters_identical_to_first_pc_this", new BigDecimal((double)getBinomialClustersIntersection(project, ds, firstDataset).size()/clustersForDataset * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "clusters_identical_to_first_pc_union", new BigDecimal((double)getBinomialClustersIntersection(project, ds, firstDataset).size()/getBinomialClustersUnion(project, ds, firstDataset).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
			}
			
			if(lastDataset == null) {
//				precalc.put(ds, "names_identical_to_first", "NA");
//				precalc.put(ds, "names_identical_to_first_pc", "NA");
			} else {
				precalc.put(ds, "names_identical_to_last", String.valueOf(getBinomialNamesIntersection(project, ds, lastDataset).size()));
				precalc.put(ds, "names_identical_to_last_pc_this", new BigDecimal((double)getBinomialNamesIntersection(project, ds, lastDataset).size()/recognizedBinomials.size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "names_identical_to_last_pc_union", new BigDecimal((double)getBinomialNamesIntersection(project, ds, lastDataset).size()/getBinomialNamesUnion(project, ds, firstDataset).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());

				int clustersForDataset = project.getNameClusterManager().getClusters(recognizedBinomials).size();
				if(clustersForDataset != recognizedBinomials.size()) {
					throw new RuntimeException("We have " + clustersForDataset + " clusters for this dataset, but " + recognizedBinomials.size() + " recognized binomials. What?");
				}
				precalc.put(ds, "clusters_identical_to_last", String.valueOf(getBinomialClustersIntersection(project, ds, lastDataset).size()));
				precalc.put(ds, "clusters_identical_to_last_pc_this", new BigDecimal((double)getBinomialClustersIntersection(project, ds, lastDataset).size()/clustersForDataset * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "clusters_identical_to_last_pc_union", new BigDecimal((double)getBinomialClustersIntersection(project, ds, lastDataset).size()/getBinomialClustersUnion(project, ds, lastDataset).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
			}
			
			if(prevDataset == null) {
//				precalc.put(ds, "names_identical_to_prev", "NA");
//				precalc.put(ds, "names_identical_to_prev_pc", "NA");				
			} else {
				precalc.put(ds, "names_identical_to_prev", String.valueOf(getBinomialNamesIntersection(project, ds, prevDataset).size()));
				precalc.put(ds, "names_identical_to_prev_pc_this", new BigDecimal((double)getBinomialNamesIntersection(project, ds, prevDataset).size()/recognizedBinomials.size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "names_identical_to_prev_pc_union", new BigDecimal((double)getBinomialNamesIntersection(project, ds, prevDataset).size()/getBinomialNamesUnion(project, ds, prevDataset).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				
				int clustersForDataset = project.getNameClusterManager().getClusters(recognizedBinomials).size();
				if(clustersForDataset != recognizedBinomials.size()) {
					throw new RuntimeException("We have " + clustersForDataset + " clusters for this dataset, but " + recognizedBinomials.size() + " recognized binomials. What?");
				}
				precalc.put(ds, "clusters_identical_to_prev", String.valueOf(getBinomialClustersIntersection(project, ds, prevDataset).size()));
				precalc.put(ds, "clusters_identical_to_prev_pc_this", new BigDecimal((double)getBinomialClustersIntersection(project, ds, prevDataset).size()/clustersForDataset * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "clusters_identical_to_prev_pc_union", new BigDecimal((double)getBinomialClustersIntersection(project, ds, prevDataset).size()/getBinomialClustersUnion(project, ds, prevDataset).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
			}
		
			if(nextDataset == null) {
	//			precalc.put(ds, "names_identical_to_prev", "NA");
	//			precalc.put(ds, "names_identical_to_prev_pc", "NA");				
			} else {
				precalc.put(ds, "names_identical_to_next", String.valueOf(getBinomialNamesIntersection(project, ds, nextDataset).size()));
				precalc.put(ds, "names_identical_to_next_pc_this", new BigDecimal((double)getBinomialNamesIntersection(project, ds, nextDataset).size()/recognizedBinomials.size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "names_identical_to_next_pc_union", new BigDecimal((double)getBinomialNamesIntersection(project, ds, nextDataset).size()/getBinomialNamesUnion(project, ds, nextDataset).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				
				int clustersForDataset = project.getNameClusterManager().getClusters(recognizedBinomials).size();
				if(clustersForDataset != recognizedBinomials.size()) {
					throw new RuntimeException("We have " + clustersForDataset + " clusters for this dataset, but " + recognizedBinomials.size() + " recognized binomials. What?");
				}
				precalc.put(ds, "clusters_identical_to_next", String.valueOf(getBinomialClustersIntersection(project, ds, nextDataset).size()));
				precalc.put(ds, "clusters_identical_to_next_pc_this", new BigDecimal((double)getBinomialClustersIntersection(project, ds, nextDataset).size()/clustersForDataset * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
				precalc.put(ds, "clusters_identical_to_next_pc_union", new BigDecimal((double)getBinomialClustersIntersection(project, ds, nextDataset).size()/getBinomialClustersUnion(project, ds, nextDataset).size() * 100).setScale(2, BigDecimal.ROUND_HALF_EVEN).toPlainString());
			}
		}
		
		// Set table items.
		controller.getTableItemsProperty().set(projectView.getProject().getDatasets());
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
}
