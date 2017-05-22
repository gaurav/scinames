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
package com.ggvaidya.scinames.complexquery;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.project.ProjectView;

import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * A SearchView allows users to search for changes using a variety of criteria.
 * It uses ComplexQueryViewController to do this.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class SearchView {
	private static final Logger LOGGER = Logger.getLogger(SearchView.class.getSimpleName());
	
	private Stage stage;
	private Scene scene;
	private ProjectView projectView;
	private ComplexQueryViewController controller;
	
	public Stage getStage() { return stage; }

	public SearchView(ProjectView pv) {
		projectView = pv;
		stage = new Stage();
		
		controller = ComplexQueryViewController.createComplexQueryView(pv);
		scene = controller.getScene();
		
		// Go go stagey scene.
		stage = new Stage();
		init();
		stage.setScene(scene);
	}
	
	public void init() {
		// Setup stage.
		stage.setTitle("Search");
		
		// Setup help.
		controller.setQueryHelp(String.join("",
			"Eventually, this is going to be a complex, sophisticated tool",
			" supporting multiple kinds of searches; for now, all we support",
			" is regular-expression matches of names. So try that out!"
		));
		
		// What to do on a search?
		controller.addListener(c -> {
			String query = controller.getQuery();
			
			Project project = projectView.getProject();
			Set<Change> changesThatMatch = getChangesThatMatch(project, query).collect(Collectors.toSet());
			
			LOGGER.info("Rendering table with matches: " + changesThatMatch);	
			//controller.updateTableWithChangesUsingNames(project, clustersThatMatch.stream().sorted((a, b) -> a.getName().compareTo(b.getName())).collect(Collectors.toList()), project.getDatasets());
			controller.updateTableWithChanges(project, changesThatMatch, project.getDatasets());
			
			return "ok";
		});
	}
	
	private Stream<Change> getChangesThatMatch(Project project, String query) {
		Set<String> names = new HashSet<>(Arrays.asList(query.split("\\s*\\n\\s*")));
		
		LOGGER.info("Names to match against changes: " + names);
		
		// treat newlines as ORs.
		return names.stream().flatMap(nameRegexLine -> {
			String nameRegex = nameRegexLine.replaceAll("^\\s*-\\s*", "");
		
			// Yes, we want to use getAllChanges() here, so we can search for names 
			return project.getDatasets().stream().flatMap(ds -> ds.getAllChanges()).filter(
				ch -> ch.getAllNames().stream()
					.anyMatch(name -> 
						name.getFullName().startsWith(nameRegex)
						|| name.getFullName().matches(nameRegex)
					)
				);
		});
	}
	
	/*
	private Stream<NameCluster> getNameClustersThatMatch(Project project, String query) {
		Set<String> names = new HashSet(Arrays.asList(query.split("\\s*\\n\\s*")));
		
		LOGGER.info("Names to match against name clusters: " + names);		
		
		// treat newlines as ORs.
		return names.stream().flatMap(nameRegexLine -> {
			String nameRegex = nameRegexLine.replaceAll("^\\s*-\\s*", "");
			
			return project.getNameClusterManager().getClusters()
				.filter(cluster -> 
					cluster.containsNameMatching(nameRegex) ||
					cluster.containsNameStartingWith(nameRegex)
				);
		}).distinct();
	}
	*/
}
