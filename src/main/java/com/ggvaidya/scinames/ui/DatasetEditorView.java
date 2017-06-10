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
 * The DatasetEditorView provides a UI for editing a dataset. This is the preferred view
 * for editing datasets, including:
 * 
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetEditorView {
	private ProjectView projectView;
	private Dataset dataset;
	private Stage stage;
	private Scene scene;
	private DatasetEditorController controller;

	public DatasetEditorView(ProjectView pv, Dataset ds) {
		projectView = pv;
		dataset = ds;
		
		// Load scene from FXML.
		FXMLLoader loader = new FXMLLoader(DatasetSceneController.class.getResource("/fxml/DatasetEditor.fxml"));
		AnchorPane ap;
		try {
			ap = (AnchorPane) loader.load();
		} catch(IOException e) {
			throw new RuntimeException("Could not load internal file 'DatasetImporter.fxml': " + e);
		}
		scene = new Scene(ap);
		controller = loader.getController();
		controller.setDatasetEditorView(this, dataset);
		
		stage = new Stage();
		stage.setTitle("Dataset editor: " + dataset.asTitle());
		stage.setScene(scene);
	}

	public Stage getStage() {
		return stage;
	}
	
	public ProjectView getProjectView() {
		return projectView;
	}

	public Dataset getDataset() {
		return dataset;
	}
}
