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

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * The DatasetImporterView displays a new dataset and allows it to be imported
 * properly into SciNames.
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DataReconciliatorView {
	private ProjectView projectView;
	private Stage stage;
	private Scene scene;
	private DataReconciliatorController controller;

	public DataReconciliatorView(ProjectView pv) {
		projectView = pv;
		stage = new Stage();
		
		// Load scene from FXML.
		FXMLLoader loader = new FXMLLoader(DatasetSceneController.class.getResource("/fxml/DataReconciliatorScene.fxml"));
		AnchorPane ap;
		try {
			ap = (AnchorPane) loader.load();
		} catch(IOException e) {
			throw new RuntimeException("Could not load internal file 'DataReconciliatorScene.fxml': " + e);
		}
		scene = new Scene(ap);
		controller = loader.getController();
		controller.setDataReconciliatorView(this);

		stage.setTitle("Data reconciliation");
		stage.setScene(scene);
	}

	public Stage getStage() {
		return stage;
	}
	
	public ProjectView getProjectView() {
		return projectView;
	}
}
