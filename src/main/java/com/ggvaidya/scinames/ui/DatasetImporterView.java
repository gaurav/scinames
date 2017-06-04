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
 * The DatasetImporterView displays a new dataset and allows it to be imported
 * properly into SciNames.
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetImporterView {
	private ProjectView projectView;
	private File file;
	private Stage stage;
	private Scene scene;
	private DatasetImporterController controller;

	public DatasetImporterView(ProjectView pv, File f) {
		projectView = pv;
		file = f;
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
		controller.setDatasetImporterView(this, file);
		
		stage.setOnCloseRequest((event) -> {
			// Closing this window is fine, but turn off the controller first.
			// This will prevent us from reporting an improperly converted dataset.
			controller = null;
			
			// See http://stackoverflow.com/a/29711376/27310 for how to get around
			// this.
		});
		
		stage.setTitle("Importing dataset from: " + file.getAbsolutePath());
		stage.setScene(scene);
	}

	public Stage getStage() {
		return stage;
	}
	
	public File getFile() {
		return file;
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
