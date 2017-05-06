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

import java.io.IOException;

import com.ggvaidya.scinames.project.ProjectView;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * Starts the bulk change editor on the project.
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class BulkChangeEditor {
	private ProjectView projectView;
	private Stage stage;
	private Scene scene;
	private BulkChangeEditorController controller;

	public BulkChangeEditor(ProjectView pv) {
		projectView = pv;
		stage = new Stage();
		
		// Load scene from FXML.
		FXMLLoader loader = new FXMLLoader(BulkChangeEditor.class.getResource("/fxml/BulkChangeEditor.fxml"));
		AnchorPane ap;
		try {
			ap = (AnchorPane) loader.load();
		} catch(IOException e) {
			throw new RuntimeException("Could not load internal file 'BulkChangeEditor.fxml': " + e);
		}
		scene = new Scene(ap);
		controller = loader.getController();
		controller.setBulkChangeEditor(this);
		
		stage.setTitle("Bulk Change Editor");
		stage.setScene(scene);
	}

	public Stage getStage() {
		return stage;
	}
	
	public ProjectView getProjectView() {
		return projectView;
	}
}
