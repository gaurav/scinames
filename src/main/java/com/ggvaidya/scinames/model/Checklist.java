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
package com.ggvaidya.scinames.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ggvaidya.scinames.model.rowextractors.NameExtractorParseException;

/**
 * A taxonomic checklist. This used to be a data type, but most of what it does has now been
 * incorporated into Dataset, so it's just static methods to load taxonomic checklists as a
 * list of names.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class Checklist {
	public static Pattern pNameAndNumber = Pattern.compile("^(\\[?\\w+\\]?\\.)?\\s*(\\w+)\\s([\\w\\-]+)(?:\\s([\\w\\-]+))?\\s*$");
	
	/**
	 * Load a dataset from a list of taxonomic checklists.
	 *
	 * @param f File to load
	 * @return A Dataset loaded from the list of taxonomic checklists.
	 * @throws IOException If there was a problem reading or parsing from this file.
	 */
	public static Dataset fromListInFile(File f) throws IOException {
		LineNumberReader r = new LineNumberReader(new BufferedReader(new FileReader(f)));
		
		Dataset checklist = new Dataset();
		try {
			checklist.setNameExtractorsString("genusAndEpithets(genus, specificEpithet, subspecificEpithet)");
		} catch(NameExtractorParseException ex) {
			throw new RuntimeException("Checklist name extractor string could not be parsed: " + ex);
		}
		
		// project, f.getName(), new SimplifiedDate());
		String line;
		while((line = r.readLine()) != null) {
			Matcher m = pNameAndNumber.matcher(line);
			
			String number;
			String genus;
			String species;
			String subspecies;
			
			if(m.matches()) {
				number = m.group(1);
				genus = m.group(2);
				species = m.group(3);
				subspecies = m.group(4);
			} else {
				throw new IOException("Could not parse line '" + line + "'");
			}
			
			if(number != null && number.equals(""))
				number = null;
			
			DatasetRow row = new DatasetRow(checklist);
			
			row.put("verbatim", line);
			row.put("genus", genus);
			row.put("specificEpithet", species);
			
			if(subspecies != null)
				row.put("subspecificEpithet", subspecies);

			if(number != null)
				row.put("id", number);
			
			checklist.rowsProperty().add(row);
		}
		
		return checklist;
	}
}
