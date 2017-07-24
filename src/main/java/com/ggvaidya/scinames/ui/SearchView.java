/*
 *
 *  SearchView
 *  Copyright (C) 2017 Gaurav Vaidya
 *
 *  This file is part of SciNames.
 *
 *  SciNames is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  SciNames is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with SciNames.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.ggvaidya.scinames.ui;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ggvaidya.scinames.dataset.DatasetSceneController;
import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.ui.ProjectView;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * A SearchView lets you sort through items.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class SearchView {
	private static final Logger LOGGER = Logger.getLogger(SearchView.class.getSimpleName());
	
	private Stage stage;
	private Scene scene;
	private ProjectView projectView;
	private SearchViewController controller;
	
	public Stage getStage() { return stage; }
	public Scene getScene() { return scene; }
	public ProjectView getProjectView() { return projectView; }

	public SearchView(ProjectView pv) {
		projectView = pv;
		
		// Load the fxml.
		FXMLLoader loader = new FXMLLoader(DatasetSceneController.class.getResource("/fxml/SearchView.fxml"));
		AnchorPane ap;
		try {
			ap = (AnchorPane) loader.load();
		} catch(IOException e) {
			throw new RuntimeException("Could not load internal file 'SearchView.fxml': " + e);
		}
		scene = new Scene(ap);
		controller = loader.getController();
		controller.setSearchView(this);
		
		// Go go stagey scene.
		stage = new Stage();
		stage.setTitle("Search");
		stage.setScene(scene);
	}
}
