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
package com.ggvaidya.scinames.dataset;

import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.ui.ProjectView;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * Starts the TimepointScene for a given Timepoint.
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetView {
	private ProjectView projectView;
	private Dataset timepoint;
	private Stage stage;
	private Scene scene;
	private DatasetSceneController controller;

	public DatasetView(ProjectView pv, Dataset tp) {
		projectView = pv;
		timepoint = tp;
		stage = new Stage();
		
		// Load scene from FXML.
		FXMLLoader loader = new FXMLLoader(DatasetSceneController.class.getResource("/fxml/DatasetScene.fxml"));
		AnchorPane ap;
		try {
			ap = (AnchorPane) loader.load();
		} catch(IOException e) {
			throw new RuntimeException("Could not load internal file 'DatasetScene.fxml': " + e);
		}
		scene = new Scene(ap);
		controller = loader.getController();
		controller.setTimepointView(this);
		
		stage.setTitle(timepoint.asTitle());
		stage.setScene(scene);
	}
	
	public DatasetView(ProjectView pv, Change ch) {
		this(pv, ch.getDataset());
		
		// select this particular change
		controller.selectChange(ch);
	}

	public Stage getStage() {
		return stage;
	}
	
	public Dataset getDataset() {
		return timepoint;
	}
	
	public ProjectView getProjectView() {
		return projectView;
	}
}
