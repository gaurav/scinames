
/*
 *
 *  DatasetColumn
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

package com.ggvaidya.scinames.model;

import java.util.HashMap;
import java.util.Map;

/**
 * A class for dataset columns.
 * 
 * For convenience, identically named columns are represented by the same column object. This
 * simplifies data aggregation, the dataset column can be retrieved by name. This is probably
 * not ideal, but clearing it out will require quite a bit of reorganization, so I'll defer it
 * for now.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetColumn implements Comparable<DatasetColumn> {
	/* All identically named columns as the same. */
	private static Map<String, DatasetColumn> singletons = new HashMap<>();
	private boolean isFake = false;
	
	/**
	 * Return the DatasetColumn for a particular name. This prevents per-dataset customization of columns,
	 * but greatly simplifies aggregation. We should really clean this up at some point.
	 * 
	 * @param colName The unique common name to retrieve a DatasetColumn for.
	 * @return The DatasetColumn corresponding to that column name.
	 */
	public static DatasetColumn of(String colName) {
		if(colName == null) throw new IllegalArgumentException("Column name cannot be null");
		if(colName.equals("")) throw new IllegalArgumentException("Column name cannot be blank");

		if(!singletons.containsKey(colName))
			singletons.put(colName, new DatasetColumn(colName));
			
		return singletons.get(colName);
	}
	
	/**
	 * Fake columns are NOT tracked on the singleton table! How cool!
	 * 
	 * @param colName
	 * @return
	 */
	public static DatasetColumn fakeColumnFor(String colName) {
		if(colName == null) throw new IllegalArgumentException("Column name cannot be null");
		if(colName.equals("")) throw new IllegalArgumentException("Column name cannot be blank");
		
		DatasetColumn fakeCol = new DatasetColumn(colName);
		fakeCol.isFake = true;
		return fakeCol;
	}
	
	/* Private variables. Mapping, if necessary, comes later. */
	private final String colName;
	public String getName() { return colName; }
	
	/* Simple accessors */
	@Override public String toString() {
		if(isFake)
			return getName();
		else
			return "column '" + getName() + "'";
	}
	
	/* Constructors */
	private DatasetColumn(String c) {
		colName = c;
	}

	@Override
	public int compareTo(DatasetColumn other) {
		return colName.compareTo(other.colName);
	}
}
