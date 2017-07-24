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
import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.ggvaidya.scinames.SciNames;
import com.ggvaidya.scinames.complexquery.ComplexSearchView;
import com.ggvaidya.scinames.dataset.DatasetSceneController;
import com.ggvaidya.scinames.dataset.DatasetChangesView;
import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.summary.ChangeFiltersView;
import com.ggvaidya.scinames.summary.ChangesListView;
import com.ggvaidya.scinames.summary.DatasetSimilarityView;
import com.ggvaidya.scinames.summary.DatasetTabularView;
import com.ggvaidya.scinames.summary.LumpsAndSplitsView;
import com.ggvaidya.scinames.summary.NameClustersView;
import com.ggvaidya.scinames.summary.NameStabilityView;
import com.ggvaidya.scinames.summary.ProjectCountsView;
import com.ggvaidya.scinames.summary.SpeciesNamesView;
import com.ggvaidya.scinames.summary.TaxonConceptsView;
import com.ggvaidya.scinames.util.SimplifiedDate;
import com.ggvaidya.scinames.validation.ValidationSuiteView;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
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
	
	/* UI correction */
	
	/**
	 * Menubar that goes with the ProjectView.
	 * 
	 * @return 
	 */
	public MenuBar setupMenuBar() {
		MenuBar mb = menuBar;	
		mb.getMenus().clear();
		
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
	
	@FXML
	private void refreshProject(ActionEvent evt) {
		updateProject(projectView.getProject());
		
		// Warning: this causes name clusters to be recalculated, so
		// use only when the user can wait a few seconds!
		timepointTable.refresh();
	}
	
	/**
	 * On initializable, set up the table and a few focus-lose callbacks.
	 */
	public void initialize() {
		// Update the menubar!
		setupMenuBar();
	}
	
	/**
	 * Set the ProjectView that this controller is associated with.
	 * 
	 * @param p 
	 */
	public void setProjectView(ProjectView p) {
		projectView = p;
		
		// If the project changes, update project.
		projectView.getProject().lastModifiedProperty().addListener(lm -> {
			LOGGER.info("Project modified! Updating project view.");
			updateProject(projectView.getProject());
		});
	
		// If projectName loses focus, check to see if it
		// changed -- if so, fire off an event.
		projectName.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
			if(!newValue) {
				projectNameChanged(null);
			}
		});
		
		// Set up timepointTable columns.
		List<TableColumn<Dataset, ?>> cols = timepointTable.getColumns();
		cols.clear();
		
		TableColumn<Dataset, String> typeCol = new TableColumn<>("Type");
		typeCol.setCellFactory(ComboBoxTableCell.forTableColumn(
			Dataset.TYPE_DATASET,
			Dataset.TYPE_CHECKLIST
		));
		typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
		typeCol.setPrefWidth(80);
		typeCol.setEditable(true);
		cols.add(typeCol);
		
		TableColumn<Dataset, String> nameCol = new TableColumn<>("Names");
		nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
		nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
		nameCol.setPrefWidth(180);
		nameCol.setEditable(true);
		cols.add(nameCol);
		
		TableColumn<Dataset, SimplifiedDate> dateCol = new TableColumn<>("Date");
		dateCol.setCellFactory(TextFieldTableCell.forTableColumn(new SimplifiedDate.SimplifiedDateStringConverter()));
		dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
		dateCol.setPrefWidth(150);
		dateCol.setEditable(true);
		cols.add(dateCol);
		
		TableColumn<Dataset, String> rowsCol = new TableColumn<>("Rows");
		rowsCol.setCellValueFactory(new PropertyValueFactory<>("rowCountSummary"));
		rowsCol.setPrefWidth(200);
		rowsCol.setEditable(false);
		cols.add(rowsCol);
		
		TableColumn<Dataset, String> nameCount = new TableColumn<>("All names");
		nameCount.setPrefWidth(200);
		//nameCount.setCellValueFactory(new PropertyValueFactory<>("NameCountSummary"));
		nameCount.setCellValueFactory((CellDataFeatures<Dataset, String> cdf) -> {
			Dataset dataset = cdf.getValue();
			return new ReadOnlyStringWrapper(
				dataset.getNameCountSummary(projectView.getProject())
				//projectView.getProject().getRecognizedNames(dataset).size() + " (" + dataset.getReferencedNames().count() + " in this dataset)"
			);
		});
		cols.add(nameCount);
		
		TableColumn<Dataset, String> binomialCount = new TableColumn<>("Binomial names");
		binomialCount.setPrefWidth(200);
		// nameCount.setCellValueFactory(new PropertyValueFactory<>("BinomialCountSummary"));		
		binomialCount.setCellValueFactory((CellDataFeatures<Dataset, String> cdf) -> {
			Dataset dataset = cdf.getValue();
			return new ReadOnlyStringWrapper(
				dataset.getBinomialCountSummary(projectView.getProject())
				// projectView.getProject().getRecognizedNames(dataset).stream().map(n -> n.getBinomialName()).distinct().count() + " (" + dataset.getReferencedNames().map(n -> n.getBinomialName()).distinct().count() + " in this dataset)"
			);
		});
		cols.add(binomialCount);
		
		TableColumn<Dataset, String> explicitChangesCount = new TableColumn<>("Changes");
		explicitChangesCount.setPrefWidth(400);
		explicitChangesCount.setCellValueFactory(cvf -> new ReadOnlyStringWrapper(cvf.getValue().getChangesCountSummary(projectView.getProject())));
		cols.add(explicitChangesCount);
		
		TableColumn<Dataset, String> colNote = new TableColumn<>("Note");
		colNote.setCellFactory(TextFieldTableCell.forTableColumn());
		colNote.setCellValueFactory(new PropertyValueFactory<>("note"));
		colNote.setPrefWidth(100.0);
		colNote.setEditable(true);
		cols.add(colNote);
				
		// Set up timepointTable itself.
		timepointTable.setEditable(true);
		
		// Set up behavior for all future rows.
		timepointTable.setRowFactory(table -> {
			TableRow<Dataset> row = new TableRow<>();
			
			row.setOnMouseClicked(event -> {
				if(row.isEmpty()) return;
				Dataset dataset = row.getItem();
				
				if(event.getClickCount() == 1 && event.isPopupTrigger()) {
					ContextMenu contextMenu = new ContextMenu();
					
					Project project = projectView.getProject();
					contextMenu.getItems().add(menuItemThat("Display dataset", evt -> new DatasetEditorView(projectView, dataset).getStage().show()));
					contextMenu.getItems().add(menuItemThat("Display changes", evt -> new DatasetChangesView(projectView, dataset).getStage().show()));
					
					Optional<Dataset> datasetFirst = project.getFirstDataset();
					if(datasetFirst.isPresent())
						contextMenu.getItems().add(menuItemThat("Diff with first", evt -> new DatasetDiffView(projectView, datasetFirst.get(), dataset).getStage().show()));
					
					Optional<Dataset> datasetLast = project.getLastDataset();
					if(datasetLast.isPresent())
						contextMenu.getItems().add(menuItemThat("Diff with last", evt -> new DatasetDiffView(projectView, dataset, datasetLast.get()).getStage().show()));
					
					contextMenu.show(projectView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
				} else if(event.getClickCount() == 2) {
					// So much easier.
					projectView.openDetailedView(dataset);
				}
			});
			
			return row;
		});

	}
	
	private MenuItem menuItemThat(String name, EventHandler<ActionEvent> action) {
		MenuItem mn = new MenuItem(name);
		mn.setOnAction(action);
		return mn;
	}
	
	/* FXML variables */
	
	@FXML
	private TextField projectName;
	
	@FXML
	private TableView<Dataset> timepointTable;
	
	@FXML
	private MenuBar menuBar;
	
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
			new FileChooser.ExtensionFilter("Dataset file (*.csv, *.tsv, *.xls, *.xlsx)", "*.csv", "*.tsv", "*.xls", "*.xlsx"),
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
		DatasetImporterView view = new DatasetImporterView(projectView);
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
			chooser.getExtensionFilters().setAll(
				new FileChooser.ExtensionFilter("Project XML.gz file", "*.xml.gz")
			);
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
	private void displayNameStability(ActionEvent evt) {
		NameStabilityView nameStabilityView = new NameStabilityView(projectView);
		nameStabilityView.getStage().show();
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
	
	@FXML
	private void diffDatasets(ActionEvent evt) {
		DatasetDiffView view = new DatasetDiffView(projectView);
		view.getStage().show();
	}
}
