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

import com.ggvaidya.scinames.dataset.*;
import com.ggvaidya.scinames.model.Dataset;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * The DatasetImporterView provides a UI for loading a new dataset into the system.
 * We're going to divide its functionality up with the DatasetEditor, which allows
 * you to "reconfigure" that dataset once it's loaded. The easiest way to think about
 * these two is that you call the DatasetImporter when you want to load something,
 * and then once you have a loaded Dataset you can send that to a DatasetEditor
 * to tweak columns or name parsing and other suchlike.
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetImporterView {
	private ProjectView projectView;
	private Stage stage;
	private Scene scene;
	private DatasetImporterController controller;

	public DatasetImporterView(ProjectView pv) {
		projectView = pv;
		stage = new Stage(StageStyle.UTILITY);
		
		// Load scene from FXML.
		FXMLLoader loader = new FXMLLoader(DatasetSceneController.class.getResource("/fxml/DatasetImporter.fxml"));
		AnchorPane ap;
		try {
			ap = (AnchorPane) loader.load();
		} catch(IOException e) {
			throw new RuntimeException("Could not load internal file 'DatasetImporter.fxml': " + e);
		}
		scene = new Scene(ap);
		controller = loader.getController();
		controller.setDatasetImporterView(this);
		
		stage.setOnCloseRequest((event) -> {
			// Closing this window is fine, but turn off the controller first.
			// This will prevent us from reporting an improperly converted dataset.
			controller = null;
			
			// See http://stackoverflow.com/a/29711376/27310 for how to get around
			// this.
		});
		
		stage.setTitle("Import datasets ...");
		stage.setScene(scene);
	}

	public Stage getStage() {
		return stage;
	}
	
	public ProjectView getProjectView() {
		return projectView;
	}

	public Stream<Dataset> getDatasetsToImport() {
		if(controller != null)
			return controller.getImportedDatasets();
		else
			return Stream.empty();
	}
}
