
/*
 *
 *  NameClustersView
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

package com.ggvaidya.scinames.summary;

import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.tabulardata.TabularDataViewController;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * A DatasetTabularView displays an entire dataset.
 * It uses the TabularDataView to do this.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public final class DatasetTabularView {
	private Stage stage;
	private Scene scene;
	private Dataset dataset;
	private TabularDataViewController controller;
	
	public Stage getStage() { return stage; }

	public DatasetTabularView(Dataset dataset) {
		this.dataset = dataset;
		stage = new Stage();
		
		controller = TabularDataViewController.createTabularDataView();
		scene = controller.getScene();
		init();
		
		// Go go stagey scene.
		stage.setScene(scene);
	}
	
	public void init() {
		// Setup stage.
		stage.setTitle("Dataset: " + dataset.getName());
		
		// Setup headertext.
		controller.getHeaderTextProperty().set("Values from this dataset.");
		controller.getHeaderTextEditableProperty().set(true);
		
		dataset.displayInTableView(controller.getTableView());
	}
}
