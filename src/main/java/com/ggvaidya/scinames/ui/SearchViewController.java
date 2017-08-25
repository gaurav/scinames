/*
 *
 *  SearchView
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
package com.ggvaidya.scinames.ui;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ggvaidya.scinames.dataset.DatasetChangesView;
import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.NameCluster;
import com.ggvaidya.scinames.model.NameClusterManager;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.Tag;
import com.ggvaidya.scinames.ui.ProjectView;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * A SearchView allows users to search for changes using a variety of criteria.
 * It uses ComplexQueryViewController to do this.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class SearchViewController implements Initializable {
	private static final Logger LOGGER = Logger.getLogger(SearchViewController.class.getSimpleName());
	private SearchView searchView;
	
	public void setSearchView(SearchView sv) {
		searchView = sv;
		
		searchByCurrentSearchBy();
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setupSearchBy();
		setupTableViews();
	}
	
	/* Search results */
	public static class SearchResult<T> {
		private String label;
		public String getName() { return label; }
		
		@SuppressWarnings("rawtypes")
		public Class getResultClass() {
			List<T> matches = getMatches();
			
			// Default to class
			if(matches.isEmpty()) return Change.class;
				
			return matches.get(0).getClass();
		}
		
		private Supplier<List<T>> listSupplier;
		public Supplier<List<T>> getSupplier() { return listSupplier; }
		
		private Function<String, Boolean> stringMatcher;
		public boolean matchesString(String filter) {
			return stringMatcher.apply(filter);
		}
		
		private List<T> results = null;
		private List<T> getMatches() {
			if(results == null)
				results = listSupplier.get();
			
			return results;
		}
		
		public int getCount() { 
			return getMatches().size(); 
		}
		
		public SearchResult(String label, Supplier<List<T>> listSupplier, Function<String, Boolean> stringMatcher) {
			assert label != null;
			assert listSupplier != null;
			assert stringMatcher != null;
			
			this.label = label;
			this.listSupplier = listSupplier;
			this.stringMatcher = stringMatcher; 
		}
	}
	
	/* FXML objects */
	@FXML private ChoiceBox<String> searchByChoiceBox;
	@FXML private TextField filterTextField;
	@FXML private TextField filterStatusTextField;
	@FXML private TextField resultsStatusTextField;
	
	@FXML private TableView<SearchResult> filteredTableView;
	@FXML private TableView resultsTableView;
	
	private List<SearchResult> searchResults = null;
	private ObservableList<SearchResult> filteredItems = FXCollections.observableList(new LinkedList<>());
	private List currentResultItems = null;
	
	/* Configuration and operation */
	private TableColumn<SearchResult, String> createFilteredTableColumn(String colName, Function<SearchResult, String> func) {
		TableColumn<SearchResult, String> col = new TableColumn<>(colName);
		
		col.setCellValueFactory(cvf -> new ReadOnlyStringWrapper(func.apply(cvf.getValue())));
		
		return col;
	}
	
	private TableColumn<Change, String> createChangeColumn(String colName, Function<Change, String> func) {
		TableColumn<Change, String> col = new TableColumn<>(colName);
		
		col.setCellValueFactory(cvf -> new ReadOnlyStringWrapper(func.apply(cvf.getValue())));
		
		return col;
	}
	
	private void setupTableViews() {
		// Step 1. Set up filteredTableView to reflect a particular list.
		filteredTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		filteredTableView.setItems(filteredItems);
		filteredTableView.getColumns().clear();
		filteredTableView.getColumns().add(
			createFilteredTableColumn("Name", sr -> sr.getName())
		);
		filteredTableView.getColumns().add(
			createFilteredTableColumn("Count", sr -> String.valueOf(sr.getCount()))
		);
		
		// Step 2. Set up resultsTableView.
		resultsTableView.getColumns().clear();
		resultsTableView.setPlaceholder(new Label("Please select a search result to display data."));
		resultsTableView.setRowFactory(table -> {
			TableRow<Object> row = new TableRow<>();
			
			row.setOnMouseClicked(event -> {
				if(row.isEmpty()) return;
				Object item = row.getItem();
				
				if(event.getClickCount() == 1 && event.isPopupTrigger()) {
					ContextMenu contextMenu = new ContextMenu();
					
					// Someday.
					
					contextMenu.show(searchView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
				} else if(event.getClickCount() == 2) {
					// So much easier.
					// But causes the entire application to crash, so I'm commenting out for now.
					// TODO
					// searchView.getProjectView().openDetailedView(item);
				}
			});
			
			return row;
		});
		
		// Step 3. When the filtered 
		filteredTableView.getSelectionModel().selectedItemProperty().addListener((ChangeListener) (a, b, c) -> {
			SearchResult selected = filteredTableView.getSelectionModel().getSelectedItem();
			
			if(selected == null) {
				resultsTableView.getItems().clear();
				return;
			}
			
			if(currentResultItems == null || currentResultItems != selected.getMatches()) {
				@SuppressWarnings("rawtypes")
				Class resultClass = selected.getResultClass();

				resultsTableView.getItems().clear();
				resultsTableView.getItems().addAll(selected.getMatches());
				currentResultItems = selected.getMatches();
				
				// Right now, the only options are Changes. Sorry, friends.
				resultsTableView.getColumns().clear();
				if(resultClass.isAssignableFrom(Change.class)) {
					resultsTableView.getColumns().add(createChangeColumn("Type", ch -> ch.getType().getType()));
					resultsTableView.getColumns().add(createChangeColumn("From", ch -> ch.getFromString()));
					resultsTableView.getColumns().add(createChangeColumn("To", ch -> ch.getToString()));
					resultsTableView.getColumns().add(createChangeColumn("Dataset", ch -> ch.getDataset().getName()));
					resultsTableView.getColumns().add(createChangeColumn("Note", ch -> ch.getNote().orElse("")));
				} else
					LOGGER.severe("Unable to display results of class '" + resultClass + "'");
			}
		});
	}
	
	private void setupSearchBy() {
		// Search by Choice Box.
		searchByChoiceBox.getItems().clear();
		searchByChoiceBox.getItems().addAll(
			// Eventually, we'll want to add data-related searches too, I guess 
			"Search by name",
			"Search by name cluster",
			"Search by tag"
		);
		searchByChoiceBox.getSelectionModel().clearAndSelect(0);
		searchByChoiceBox.getSelectionModel().selectedItemProperty().addListener((ChangeListener) (a, b, c) -> {
			searchByCurrentSearchBy();
		});
		
		// While we're here, set up filtering stuff too.
		filterTextField.textProperty().addListener(a -> {
			filterSearchResults();
		});
	}
	
	public void searchFor(String search) {
		// Don't do anything until we're all initialized and whatnot.
		if(searchView == null) return;
		
		// Trigger the search.
		searchByCurrentSearchBy();
		
		// Filter using the search string.
		filterTextField.setText(search);
		filterSearchResults();
	}
	
	private void searchByCurrentSearchBy() {
		String searchBy = searchByChoiceBox.getSelectionModel().getSelectedItem();
		Project project = searchView.getProjectView().getProject();
		
		if(searchBy == null) searchBy = "Search by name";
		
		switch(searchBy) {
			case "Search by name":
				Set<Name> names = project.getDatasets().stream()
					.flatMap(ds -> ds.getNamesInAllRows().stream())
					.collect(Collectors.toSet());
					
				searchResults = names.stream().sorted().map(key -> 
					new SearchResult<Change>(
						key.getFullName(),
						() -> project.getAllChanges().filter(ch -> ch.getAllNames().contains(key)).collect(Collectors.toList()),
						str -> (
							key.getFullName().contains(str)
						)
					)
				).collect(Collectors.toList());
				break;
				
			case "Search by name cluster":
				NameClusterManager ncm = project.getNameClusterManager();
				searchResults = ncm.getSpeciesClusters().sorted().map((NameCluster cluster) ->
					new SearchResult<Change>(
						cluster.toString(),
						() -> project.getAllChanges().filter(ch -> cluster.containsAny(ch.getAllNames())).collect(Collectors.toList()),
						str -> cluster.containsNameMatching(str)
					)
				).collect(Collectors.toList());
				break;
				
			case "Search by tag":
				Map<Tag, List<Change>> tagGroups = new HashMap<>();
				project.getAllChanges().forEach(ch -> {
					Set<Tag> tags = ch.getTags();
					
					if(tags.isEmpty()) tags.add(Tag.NONE);
					
					for(Tag tag: tags) {
						if(!tagGroups.containsKey(tag))
							tagGroups.put(tag, new LinkedList<Change>());
						
						tagGroups.get(tag).add(ch);
					}
				});
				
				searchResults = tagGroups.keySet().stream().sorted().map(
					tag -> new SearchResult<Change>(
						tag.getName(),
						() -> tagGroups.get(tag),
						str -> tag.getName().contains(str)
					)
				).collect(Collectors.toList());
					
				break;
		}
		
		// Okay, search results are ready!
		// In an alternate universe, we come up with a smart way to count the number of 
		// items associated with each in the background. For now, though, we'll just
		// ignore the heck out of it.
		
		// Re-run the filter on the new searchResults.
		filterSearchResults();
	}
	
	private void filterSearchResults() {
		String filter = filterTextField.getText().trim();
		
		filteredItems.setAll(
			searchResults.stream().filter(sr -> sr.matchesString(filter)).collect(Collectors.toList())
		);
		
		String filteredPercent = String.valueOf((int)(((double)filteredItems.size())/searchResults.size() * 100));
		filterStatusTextField.setText(searchResults.size() + " search results, displaying " + filteredItems.size() + " results (" + filteredPercent + "%)");
	}
	
	/* FXML events */
	@FXML
	private void copyToClipboard(ActionEvent evt) {
		
	}
	
	@FXML
	private void exportToCSV(ActionEvent evt) {
		
	}
}
