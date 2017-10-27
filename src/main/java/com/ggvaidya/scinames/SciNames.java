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
package com.ggvaidya.scinames;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ggvaidya.scinames.ui.ProjectView;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The main class figures out what the user wants and sets it up.
 * 
 * The default is to start ProjectView.
 */
public class SciNames extends Application {
	public static final Logger LOGGER = Logger.getLogger(SciNames.class.getSimpleName());
	
	/* Application-level constants */
	public static final String APPNAME = "SciNames";
	public static final String VERSION = "0.2";
	
	public static final String PROPNAME_OPEN_ON_START = "project_openOnStart";
	
	/* Application-level properties */
	private static final ObservableMap<String, String> Properties = FXCollections.observableHashMap();

	public static boolean isPropertySet(String propName) {
		if(!Properties.containsKey(propName))
			return false;
		return Properties.get(propName).equalsIgnoreCase("yes");
	}
	
	/** 
	 * Get the application-level properties. These will automatically be saved
	 * on exit.
	 * @return Properties object on which properties can be set or changed.
	 */
	public static ObservableMap<String, String> getProperties() {
		return Properties;
	}
	
        /**
         * Return properties as a list of Map.Entry objects. Convenient for
         * displaying in a table or list box!
         * 
         * @return list of properties
         */
	public static List<Map.Entry<String, String>> getPropertiesAsList() {
		return new ArrayList<>(Properties.entrySet());
	}
	
	/**
	 * Get the file in which the properties will be stored. 
	 * @return File object representing the file.
	 */
	public static File getPropertiesFile() {
		return new File(System.getProperty("user.home") + File.separator + APPNAME + ".properties");
	}
	
	/**
	 * Start SciNames. Since we're all JavaFX-y, this just starts the
	 * JavaFX machinery up.
	 * 
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		SciNames.launch(args);
	}
	
	/**
	 * Initialize the application.
	 * 
	 *  - Try to load the properties.
	 */
	@Override
	public void init() {
		// Try to save the properties file.
		try {
			Properties p = new Properties();
			
			// Load actual properties.
			p.load(new FileReader(getPropertiesFile()));
			for(String name: p.stringPropertyNames()) {
				Properties.put(name, p.getProperty(name));
			}
		} catch(IOException e) {
			// Ignore.
		}
	}
	
	/**
	 * When the application exits.
	 * 
	 *  - Try to store properties in the properties file.
	 */
	@Override
	public void stop() {
		LOGGER.info("Stopping application, saving properties.");
		
		// Save preferences.
		try {
			Properties p = new Properties();
			for(String name: Properties.keySet()) {
				p.setProperty(name, Properties.get(name));
			}
			p.store(new FileWriter(getPropertiesFile()), APPNAME);
			LOGGER.info(p.size() + " properties stored in " + getPropertiesFile());
		} catch(IOException e) {
			// Ignore.
		}
	}

	/**
	 * Check command-line arguments, decide which view to set in motion,
	 * and then set it in motion.
	 * 
	 * @param primaryStage The primary stage to act on.
	 */
	@Override
	public void start(Stage primaryStage) {
		// Eventually, we will support command-line parameters.
		// For now, let's just load up the ProjectView.
		Scene firstScene;
		
		try {
			ProjectView projectScene = new ProjectView(primaryStage);
			firstScene = projectScene.getScene();
		} catch(IOException e) {
			// This is only thrown if the FXML file could not be loaded, so
			// this should never be run otherwise.
			throw new RuntimeException("(Developer error) Unable to load internal development file, check packaging: " + e.toString());
		}
		
		// Execute the firstScene.
		primaryStage.setScene(firstScene);
		primaryStage.setTitle(SciNames.APPNAME);
		primaryStage.show();
	}
	
	public static void reportMemoryStatus(String currentAction) {
		// Because why not.
		System.gc();
		
		// Calculate available memory.
		double freeMem = Runtime.getRuntime().freeMemory();
		long totalMem = Runtime.getRuntime().totalMemory();
		long maxMem = Runtime.getRuntime().maxMemory();
		
		LOGGER.log(Level.INFO, "{0}: used {1}M out of {2}M ({3,number,percent}), max memory available: {4}M", new Object[]{
			currentAction, 
			(totalMem - freeMem)/(1024*1024), 
			totalMem/(1024*1024),
			(totalMem - freeMem)/totalMem, 
			maxMem/(1024*1024)
		});
	}
}
