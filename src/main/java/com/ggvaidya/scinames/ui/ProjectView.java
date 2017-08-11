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
import java.time.Instant;
import java.util.logging.Logger;

import com.ggvaidya.scinames.SciNames;
import com.ggvaidya.scinames.dataset.DatasetChangesView;
import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.Project;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * Provides a view (in tandem with ProjectSceneController) on a particular Project.
 * 
 * @author Gaurav Vaidya
 */
public class ProjectView {
	private static final Logger LOGGER = Logger.getLogger(ProjectView.class.getSimpleName());
	
	private Stage stage;
	private Scene scene;
	private ProjectSceneController controller;
	private ObjectProperty<Project> projectProperty = new SimpleObjectProperty<>(null);
	private ChangeListener<Instant> projectModifiedChangeListener;
	
	/* Accessors */
	public Stage getStage() { return stage; }
	public Scene getScene() { return scene; }
	public void setProject(Project p) { projectProperty.setValue(p); }	
	public ObjectProperty<Project> projectProperty() { return projectProperty; }

	/* Project control */
	public Project getProject() {
		if(projectProperty.get() == null)
			projectProperty.set(new Project());
		
		return projectProperty.getValue(); 
	}
	
	void addFile(File f) throws IOException {
		Dataset tp = Dataset.loadFromFile(getProject(), f);
		
		LOGGER.info("Timepoint " + tp + " added to project " + getProject());
		getProject().addDataset(tp);
	}
	
	/* Constructor */
	
	public ProjectView(Stage stage) throws IOException {
		this.stage = stage;
				
		FXMLLoader loader = new FXMLLoader(ProjectView.class.getResource("/fxml/ProjectScene.fxml"));
		AnchorPane ap = (AnchorPane) loader.load();
		scene = new Scene(ap);
		controller = loader.getController();
		controller.setProjectView(this);
				
		// Do stuff if the current project changes.
		this.projectModifiedChangeListener = (ObservableValue<? extends Instant> observable, Instant oldValue, Instant newValue) -> {
			// System.err.println("projectModifiedChangeListener: " + newValue + " on " + getProject());
			try {
				stage.setTitle(SciNames.APPNAME + ": " + this.getProject().toString());
			} catch(NullPointerException e) {
				// If path is changed to null.
				stage.setTitle(SciNames.APPNAME);
			}
			controller.updateProject(this.getProject());
		};
		
		// If the project changes, update the window title.
		projectProperty.addListener((ObservableValue<? extends Project> observable, Project oldValue, Project newValue) -> {
			if(oldValue != null)
				oldValue.lastModifiedProperty().removeListener(projectModifiedChangeListener);
			
			if(newValue != null) {
				newValue.lastModifiedProperty().addListener(projectModifiedChangeListener);
				projectModifiedChangeListener.changed(newValue.lastModifiedProperty(), Instant.now(), Instant.now());
				
				if(scene != null) {				
					Node projectName = scene.getRoot().lookup("#projectName");
					if(projectName != null)
						((TextField)projectName).setText(newValue.getName());
				}
			}
		});
		
		// On exit.
		stage.setOnCloseRequest((WindowEvent event) -> {
			if(getProject() != null && getProject().getFile() != null) {
				SciNames.getProperties().put(SciNames.PROPNAME_OPEN_ON_START, getProject().getFile().getAbsolutePath());
			} else {
				SciNames.getProperties().remove(SciNames.PROPNAME_OPEN_ON_START);
			}
			
			try {
				closeCurrentProject();
				
				// If we're here, the current project was closed successfully. Time to die.
				Platform.exit();
				
			} catch(IOException e) {
				// Don't close if the user tried to save, but couldn't!
				new Alert(Alert.AlertType.ERROR, "Unable to save project to file: " + e)
					.showAndWait();
				
				event.consume();
			}
		});
		
		/*
		// Add menubar.
		if(!System.getProperty("os.name").startsWith("Windows")) {
			// Doesn't work right on Windows, so don't use it there.
			ap.getChildren().add(controller.setupMenuBar());
		}
		*/
		
		// Handle files being dropped into this scene.
		scene.setOnDragOver((DragEvent event) -> {
			Dragboard db = event.getDragboard();
			if(db.hasFiles()) {
				event.acceptTransferModes(TransferMode.COPY);
			} else {
				event.consume();
			}
		});
		
		scene.setOnDragDropped((DragEvent event) -> {
			Dragboard db = event.getDragboard();
			boolean result = true;
			
			if(db.hasFiles()) {
				for(File f: db.getFiles()) {
					LOGGER.info("Dragged file '" + f + "' to be loaded.");
					try {
						addFile(f);
					} catch(IOException e) {
						new Alert(Alert.AlertType.ERROR, "Could not load dragged file '" + f + "': " + e).showAndWait();
						
						result = false;
					}
				}
			}
			
			event.setDropCompleted(result);
			event.consume();
		});

		Platform.runLater(() -> {
			// If we have a PROP_OPENONSTART in properties, try to load it back up.
			if(SciNames.getProperties().containsKey(SciNames.PROPNAME_OPEN_ON_START)) {
				File f = new File(SciNames.getProperties().get(SciNames.PROPNAME_OPEN_ON_START));

				if(f.canRead()) {
					Project p = null;
					
					try {
						SciNames.reportMemoryStatus("Loading file " + f + " as last-open project");
						p = Project.loadFromFile(f);
						SciNames.reportMemoryStatus("Project " + p + " loaded into memory");
					} catch(IOException e) {
						// We don't care.
					}

					if(p != null)
						setProject(p);
				}
			}
		});
	}

	public void closeCurrentProject() throws IOException {
		if(getProject() != null) {
			if(getProject().isModified()) {
				ButtonType result = new Alert(
					Alert.AlertType.CONFIRMATION, 
					"Current project has been modified! Save before closing?",
					ButtonType.YES,
					ButtonType.NO
				).showAndWait().orElse(ButtonType.NO);

				if(result.equals(ButtonType.YES)) {
					getProject().saveToFile();
				}
			}
			
			Project blank = new Project();
			setProject(blank);
			controller.updateProject(blank);
		}
	}
	
	/**
	 * Open a window showing a detailed view on the passed object.
	 * 
	 * @param o
	 */
	public void openDetailedView(Object obj) {
		if(obj instanceof Dataset)
			openDetailedView((Dataset)obj);
		else if(obj instanceof Change)
			openDetailedView((Change)obj);
		else
			LOGGER.severe("No detailed view available for " + obj + " (class " + obj.getClass() + ")");
	}
	
	public void openDetailedView(Dataset ds) {
		//DatasetView view = new DatasetView(this, ds);
		//view.getStage().show();
		DatasetEditorView view = new DatasetEditorView(this, ds);
		view.getStage().show();
	}
	
	public void openDetailedView(Change ch) {
		DatasetChangesView view = new DatasetChangesView(this, ch);
		view.getStage().show();
	}
}
