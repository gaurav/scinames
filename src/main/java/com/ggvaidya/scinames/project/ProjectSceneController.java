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
package com.ggvaidya.scinames.project;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.ggvaidya.scinames.SciNames;
import com.ggvaidya.scinames.complexquery.SearchView;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.summary.ChangeFiltersView;
import com.ggvaidya.scinames.summary.ChangesListView;
import com.ggvaidya.scinames.summary.DatasetSimilarityView;
import com.ggvaidya.scinames.summary.LumpsAndSplitsView;
import com.ggvaidya.scinames.summary.NameClustersView;
import com.ggvaidya.scinames.summary.ProjectCountsView;
import com.ggvaidya.scinames.summary.SpeciesNamesView;
import com.ggvaidya.scinames.summary.TaxonConceptsView;
import com.ggvaidya.scinames.ui.BulkChangeEditor;
import com.ggvaidya.scinames.ui.DataReconciliatorView;
import com.ggvaidya.scinames.ui.DatasetImporterView;
import com.ggvaidya.scinames.ui.PreferencesView;
import com.ggvaidya.scinames.util.SimplifiedDate;
import com.ggvaidya.scinames.validation.ValidationSuiteView;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.StringConverter;

/**
 * The Project Scene Controller.
 * 
 * @author Gaurav Vaidya
 */
public class ProjectSceneController {
	public static final Logger LOGGER = Logger.getLogger(ProjectSceneController.class.getSimpleName());
	private ProjectView projectView;
	
	/**
	 * Set the ProjectView that this controller is associated with.
	 * 
	 * @param p 
	 */
	public void setProjectView(ProjectView p) {
		projectView = p;
	}
	
	/* UI correction */
	
	/**
	 * Menubar that goes with the ProjectView.
	 * 
	 * @return 
	 */
	public MenuBar createMenuBar() {
		MenuBar mb = new MenuBar();		
		
		// File
		Menu fileMenu = new Menu("File");
		mb.getMenus().add(fileMenu);
		
		// File -> New
		MenuItem fileNew = new MenuItem("New");
		fileNew.onActionProperty().set((ActionEvent e) -> clearExistingProject(e));
		fileMenu.getItems().add(fileNew);
		
		// File -> Load
		MenuItem fileLoad = new MenuItem("Load");
		fileLoad.onActionProperty().set((ActionEvent e) -> loadProject(e));
		fileMenu.getItems().add(fileLoad);
		
		// File -> Save
		MenuItem fileSave = new MenuItem("Save");
		fileSave.onActionProperty().set((ActionEvent e) -> saveProject(e));
		fileMenu.getItems().add(fileSave);
		
		// Datasets
		Menu datasetsMenu = new Menu("Datasets");
		mb.getMenus().add(datasetsMenu);
		
		// Datasets -> Import dataset directly
		MenuItem datasetsImport = new MenuItem("Import dataset directly");
		datasetsImport.onActionProperty().set((ActionEvent e) -> addDataset(e));
		datasetsMenu.getItems().add(datasetsImport);
		
		// Datasets -> Import dataset via importer
		MenuItem datasetsImporter = new MenuItem("Import dataset via importer");
		datasetsImporter.onActionProperty().set((ActionEvent e) -> addDatasetViaImporter(e));
		datasetsMenu.getItems().add(datasetsImporter);
		
		// Names
		Menu namesMenu = new Menu("Names");
		mb.getMenus().add(namesMenu);
		
		// Names -> All names
		MenuItem namesAllNames = new MenuItem("All names");
		namesAllNames.onActionProperty().set(e -> displaySpeciesList(e));
		namesMenu.getItems().add(namesAllNames);
		
		// Names -> Name clusters
		MenuItem namesNameClusters = new MenuItem("Name clusters");
		namesNameClusters.onActionProperty().set(e -> displayNameClusters(e));
		namesMenu.getItems().add(namesNameClusters);
		
		// Changes
		Menu changesMenu = new Menu("Changes");
		mb.getMenus().add(changesMenu);
		
		// Changes -> All changes
		MenuItem changesAllChanges = new MenuItem("All changes");
		changesAllChanges.onActionProperty().set(e -> displayChanges(e));
		changesMenu.getItems().add(changesAllChanges);

		// Configuration
		Menu configMenu = new Menu("Configuration");
		mb.getMenus().add(configMenu);
		
		// Configuration -> View and edit
		MenuItem configViewAndEdit = new MenuItem("View and edit");
		configViewAndEdit.onActionProperty().set((ActionEvent e) -> editConfiguration(e));
		configMenu.getItems().add(configViewAndEdit);

		Menu helpMenu = new Menu("Help");
		helpMenu.getItems().addAll(
			new MenuItem("About")
		);
		
		// Final setups
		mb.setUseSystemMenuBar(true);
		return mb;
	}
	
	/**
	 * The project has changed -- change all the UI elements to match.
	 * 
	 * @param project The currently loaded project. May be null.
	 */
	public void updateProject(Project project) {
		projectName.setText(project.getName());
		timepointTable.setItems(project.getDatasets());
	}
	
	/**
	 * On initializable, set up the table and a few focus-lose callbacks.
	 */
	public void initialize() {
		// If projectName loses focus, check to see if it
		// changed -- if so, fire off an event.
		projectName.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
			if(!newValue) {
				projectNameChanged(null);
			}
		});
		
		// Set up timepointTable columns.
		TableColumn<Dataset, String> nameCol = new TableColumn<>("Checklist Name");
		nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
		nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
		nameCol.setPrefWidth(150);
		nameCol.setEditable(true);
		
		TableColumn<Dataset, SimplifiedDate> dateCol = new TableColumn<>("Date");
		dateCol.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<SimplifiedDate>() {
			@Override
			public String toString(SimplifiedDate date) {
				String strYYYYmmDD = date.asYYYYmmDD("-");
				if(strYYYYmmDD.equals("0"))
					return "(None)";
				return strYYYYmmDD;
			}

			@Override
			public SimplifiedDate fromString(String string) {
				try {
					return new SimplifiedDate(string);
				} catch(DateTimeParseException ex) {
					new Alert(Alert.AlertType.ERROR, "Could not parse date '" + string + "': " + ex).showAndWait();
					return SimplifiedDate.MIN;
				}
			}
		}));
		dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
		dateCol.setPrefWidth(100);
		dateCol.setEditable(true);

		TableColumn<Dataset, String> nameCount = new TableColumn<>("Names");
		nameCount.setPrefWidth(150);
		nameCount.setCellValueFactory((CellDataFeatures<Dataset, String> cdf) -> {
			Dataset dataset = cdf.getValue();
			return new ReadOnlyStringWrapper(
				projectView.getProject().getRecognizedNames(dataset).size() + " (" + dataset.getReferencedNames().count() + " in this dataset)"
			);
		});
		
		TableColumn<Dataset, String> binomialCount = new TableColumn<>("Binomials");
		binomialCount.setPrefWidth(150);
		binomialCount.setCellValueFactory((CellDataFeatures<Dataset, String> cdf) -> {
			Dataset dataset = cdf.getValue();
			return new ReadOnlyStringWrapper(
				projectView.getProject().getRecognizedNames(dataset).stream().map(n -> n.getBinomialName()).distinct().count() + " (" + dataset.getReferencedNames().map(n -> n.getBinomialName()).distinct().count() + " in this dataset)"
			);
		});
		
		TableColumn<Dataset, String> explicitChangesCount = new TableColumn<>("Explicit changes");
		explicitChangesCount.setPrefWidth(400);
		explicitChangesCount.setCellValueFactory(cvf -> new ReadOnlyStringWrapper(cvf.getValue().getExplicitChangesCountSummary(projectView.getProject())));
		
		TableColumn<Dataset, String> implicitChangesCount = new TableColumn<>("Implicit changes");
		implicitChangesCount.setPrefWidth(400);
		implicitChangesCount.setCellValueFactory(cvf -> new ReadOnlyStringWrapper(cvf.getValue().getImplicitChangesCountSummary(projectView.getProject())));		
		
		// Set up timepointTable itself.
		timepointTable.getColumns().setAll(nameCol, dateCol, nameCount, binomialCount, explicitChangesCount, implicitChangesCount);
		timepointTable.setEditable(true);
		
		// Set up behavior for all future rows.
		timepointTable.setRowFactory(table -> {
			TableRow<Dataset> row = new TableRow<>();
			
			row.setOnMouseClicked(event -> {
				if(event.getClickCount() == 2 && !row.isEmpty()) {
					Dataset tp = row.getItem();
					
					projectView.openDetailedView(tp);
				}
			});
			
			return row;
		});

	}
	
	/* FXML variables */
	
	@FXML
	private TextField projectName;
	
	@FXML
	private TableView<Dataset> timepointTable;
	
	@FXML
	private ProgressBar progressBar;
	
	public void startProgressBar() {
		progressBar.setVisible(true);
	}
	
	public void stopProgressBar() {
		progressBar.setVisible(false);
	}
	
	/**
	 * Called when the project name field changes.
	 * 
	 * @param evt ActionEvent for 
	 */
	@FXML
	private void projectNameChanged(ActionEvent evt) {
		String newName = projectName.getText();
		
		String oldName = projectView.getProject().getName();
		// System.err.println("oldName = " + oldName + ", newName = " + newName);
		if(!newName.equals("") && !newName.equals(oldName)) {
			// Any other project name, we accept!
			projectView.getProject().setName(newName);
		}
		
		updateProject(projectView.getProject());
	}

	/**
	 * Called when the "Add File" button is hit.
	 * 
	 * @param evt ActionEvent for the "Add File" button.
	 */
	@FXML
	private void addDataset(ActionEvent evt) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Choose file to add to project ...");
		chooser.getExtensionFilters().addAll(
			new FileChooser.ExtensionFilter("Dataset file (*.csv, *.tsv)", "*.csv", "*.tsv"),
			new FileChooser.ExtensionFilter("TaxDiff file (*.txt)", "*.txt"),			
			new FileChooser.ExtensionFilter("Checklist (*.txt)", "*.txt")
		);
		File f = chooser.showOpenDialog(projectView.getStage());
		if(f == null) return;
		
		try {
			SciNames.reportMemoryStatus("Loading new dataset from file " + f);
			projectView.addFile(f);
			SciNames.reportMemoryStatus("New dataset loaded");
		} catch(IOException e) {
			new Alert(
				Alert.AlertType.ERROR, 
				"Could not load file '" + f + "': " + e
			).showAndWait();
		}
		
		updateProject(projectView.getProject());
	}
	
	@FXML
	private void addDatasetViaImporter(ActionEvent evt) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Choose file to add to project ...");
		chooser.getExtensionFilters().addAll(
			new FileChooser.ExtensionFilter("Dataset file (*.csv, *.tsv)", "*.csv", "*.tsv"),
			new FileChooser.ExtensionFilter("TaxDiff file (*.txt)", "*.txt"),			
			new FileChooser.ExtensionFilter("Checklist (*.txt)", "*.txt")
		);
		File f = chooser.showOpenDialog(projectView.getStage());
		if(f == null) return;
		
		DatasetImporterView view = new DatasetImporterView(projectView, f);
		view.getStage().initModality(Modality.APPLICATION_MODAL);
		view.getStage().showAndWait();
		
		view.getDatasetsToImport().collect(Collectors.toList()).forEach(dt -> projectView.getProject().addDataset(dt));
		updateProject(projectView.getProject());
	}
	
	@FXML
	private void removeDataset(ActionEvent evt) {
		Dataset selectedItem = timepointTable.selectionModelProperty().get().getSelectedItem();
		ObservableList<Dataset> datasets = projectView.getProject().getDatasets();
		
		int index = datasets.indexOf(selectedItem);
		
		// It's just easier this way.
		Dataset prev = null;
		try { prev = datasets.get(index - 1); } catch(IndexOutOfBoundsException e) {}
		
		Dataset next = null;
		try { next = datasets.get(index + 1); } catch(IndexOutOfBoundsException e) {}
		
		// Remove!
		datasets.remove(index);
		
		// Rewire.
		if(prev != null && next != null) {
			next.setPreviousDataset(Optional.of(projectView.getProject()), Optional.of(prev));
			updateProject(projectView.getProject());
		}
	}
	
	/**
	 * Called when the "Load" button is hit.
	 * 
	 * @param evt ActionEvent for the load project.
	 */
	@FXML
	private void loadProject(ActionEvent evt) {
		try {
			projectView.closeCurrentProject();
		} catch (IOException ex) {
			new Alert(Alert.AlertType.ERROR, "Could not save current project, load aborted: " + ex)
				.showAndWait();
		}
		
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Load project from ...");
		chooser.getExtensionFilters().addAll(
			new FileChooser.ExtensionFilter("Project XML.gz file", "*.xml.gz")
		);
		File f = chooser.showOpenDialog(projectView.getStage());
		if(f == null) {
			return;
		}
		
		try {
			projectView.setProject(Project.loadFromFile(f));
		} catch (IOException ex) {
			new Alert(Alert.AlertType.ERROR, "Could not load project from file '" + f + "': " + ex)
				.showAndWait();
		}
		
		updateProject(projectView.getProject());
	}
	
	/**
	 * Called when the "Save" button is hit.
	 * 
	 * @param evt ActionEvent for the save button.
	 */
	@FXML
	private void saveProject(ActionEvent evt) {
		Project project = projectView.getProject();
		
		// Does this project already have a filename?
		File f = project.getFile();
		if(f == null) {
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Save project to ...");
			chooser.setSelectedExtensionFilter(
				new FileChooser.ExtensionFilter("Project XML.gz file", "*.xml.gz")
			);
			f = chooser.showSaveDialog(projectView.getStage());
			if(f == null)
				return;
			project.setFile(f);
		}
		
		try {
			SciNames.reportMemoryStatus("Saving project " + project + " to disk");
			project.saveToFile();
			SciNames.reportMemoryStatus("Project saved to disk");
		} catch (IOException ex) {
			new Alert(Alert.AlertType.ERROR, "Could not save project to file '" + f + "': " + ex)
				.showAndWait();
		}
	}
	
	/* Display species name lists */
	@FXML
	private void displaySpeciesList(ActionEvent evt) {
		SpeciesNamesView view = new SpeciesNamesView(projectView);
		view.getStage().show();
	}
	
	@FXML
	private void displayNameClusters(ActionEvent evt) {
		NameClustersView view = new NameClustersView(projectView);
		view.getStage().show();
	}
	
	@FXML
	private void displayChanges(ActionEvent evt) {
		ChangesListView view = new ChangesListView(projectView);
		view.getStage().show();
	}
	
	@FXML
	private void displayFilters(ActionEvent evt) {
		ChangeFiltersView view = new ChangeFiltersView(projectView);
		view.getStage().show();
	}
	
	/* Start the configuration view */
	@FXML
	private void editConfiguration(ActionEvent evt) {
		PreferencesView prefView = new PreferencesView(projectView);
		prefView.getStage().show();
	}
	
	@FXML
	private void displayLumpsAndSplits(ActionEvent evt) {
		LumpsAndSplitsView splumpsView = new LumpsAndSplitsView(projectView);
		splumpsView.getStage().show();
	}
	
	@FXML
	private void displayTimepointSimilarity(ActionEvent evt) {
		DatasetSimilarityView timepointSimilarity = new DatasetSimilarityView(projectView);
		timepointSimilarity.getStage().show();
	}
	
	@FXML
	private void displayProjectTabularView(ActionEvent evt) {
		ProjectCountsView projectTabular = new ProjectCountsView(projectView);
		projectTabular.getStage().show();
	}
	
	@FXML
	private void displayTaxonConcepts(ActionEvent evt) {
		TaxonConceptsView taxonConcepts = new TaxonConceptsView(projectView);
		taxonConcepts.getStage().show();
	}
	
	@FXML
	private void displayValidationSuite(ActionEvent evt) {
		ValidationSuiteView validationSuite = new ValidationSuiteView(projectView);
		validationSuite.getStage().show();
	}
	
	@FXML
	private void displaySearch(ActionEvent evt) {
		SearchView view = new SearchView(projectView);
		view.getStage().show();
	}
	
	@FXML
	private void clearExistingProject(ActionEvent evt) {
		try {
			projectView.closeCurrentProject();
		} catch(IOException e) {
			new Alert(Alert.AlertType.ERROR, "Could not save project: " + e)
				.showAndWait();
		}
	}

	@FXML
	private void reconcileData(ActionEvent evt) {
		DataReconciliatorView view = new DataReconciliatorView(projectView);
		view.getStage().show();
	}
	
	@FXML
	private void displayBulkChangeEditor(ActionEvent evt) {
		BulkChangeEditor editor = new BulkChangeEditor(projectView);
		editor.getStage().show();
	}
}
