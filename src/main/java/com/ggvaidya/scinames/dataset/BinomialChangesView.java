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
 * Displays binomial changes by summarizing lower-level changes and ignoring higher taxonomic
 * changes. It is intended as a visualization tool rather than an editing tool, but tries to make it
 * easy to get to the actual changes to fix any errors. 
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class BinomialChangesView {
	private ProjectView projectView;
	private Stage stage;
	private Scene scene;
	private BinomialChangesSceneController controller;

	public BinomialChangesView(ProjectView pv) {
		projectView = pv;
		stage = new Stage();
		
		// Load scene from FXML.
		FXMLLoader loader = new FXMLLoader(DatasetSceneController.class.getResource("/fxml/BinomialChangesView.fxml"));
		AnchorPane ap;
		try {
			ap = (AnchorPane) loader.load();
		} catch(IOException e) {
			throw new RuntimeException("Could not load internal file 'BinomialChangesView.fxml': " + e);
		}
		scene = new Scene(ap);
		controller = loader.getController();
		controller.setBinomialChangesView(this);
		
		stage.setTitle("Binomial changes for " + pv.getProject());
		stage.setScene(scene);
	}

	public Stage getStage() {
		return stage;
	}
	
	public ProjectView getProjectView() {
		return projectView;
	}

	public Scene getScene() {
		return scene;
	}
}
