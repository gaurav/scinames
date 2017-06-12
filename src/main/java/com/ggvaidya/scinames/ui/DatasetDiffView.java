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
 * The DatasetDiffView provides operations that compare individual datasets. It is designed to be
 * especially helpful where the project is too large for data reconciliation to be used efficiently.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetDiffView {
	private ProjectView projectView;
	private Stage stage;
	private Scene scene;
	private DatasetDiffController controller;

	public DatasetDiffView(ProjectView pv) {
		projectView = pv;
		
		// Load scene from FXML.
		FXMLLoader loader = new FXMLLoader(DatasetSceneController.class.getResource("/fxml/DatasetDiffView.fxml"));
		AnchorPane ap;
		try {
			ap = (AnchorPane) loader.load();
		} catch(IOException e) {
			throw new RuntimeException("Could not load internal file 'DatasetDiffView.fxml': " + e);
		}
		scene = new Scene(ap);
		controller = loader.getController();
		controller.setDatasetDiffView(this);
		
		stage = new Stage();
		stage.setTitle("Dataset comparison");
		stage.setScene(scene);
	}

	public Stage getStage() {
		return stage;
	}
	
	public ProjectView getProjectView() {
		return projectView;
	}
}
