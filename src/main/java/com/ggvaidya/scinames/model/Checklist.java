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

/**
 * A taxonomic checklist. It recognizes a comprehensive set of taxon concepts
 * with distinct Names.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class Checklist {
	// TODO: extend this to include more complex subspecific epithets.
	public static Pattern pNameAndNumber = Pattern.compile("^(\\[?\\w+\\]?\\.)?\\s*(\\w+)\\s([\\w\\-]+)(?:\\s([\\w\\-]+))?\\s*$");
	
	public static Dataset fromListInFile(File f) throws IOException {
		LineNumberReader r = new LineNumberReader(new BufferedReader(new FileReader(f)));
		
		Dataset checklist = new Dataset();
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
			
			addName(checklist, Optional.ofNullable(number), line, genus, species, Optional.ofNullable(subspecies));
		}
		
		return checklist;
	}

	private static void addName(Dataset dataset, Optional<String> number, String line, String genus, String specificEpithet, Optional<String> subspecificEpithets) {
		// Construct a row.
		DatasetRow row = new DatasetRow();
		
		row.put("verbatim", line);
		row.put("genus", genus);
		row.put("specificEpithet", specificEpithet);
		if(subspecificEpithets.isPresent())
			row.put("subspecificEpithet", subspecificEpithets.get());
		
		if(number.isPresent())
			row.put("id", number.get());
		
		dataset.rowsProperty().add(row);
	}
}
