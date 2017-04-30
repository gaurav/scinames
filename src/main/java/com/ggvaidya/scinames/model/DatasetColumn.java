
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
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetColumn {
	/* All identically named columns as the same. */
	private static Map<String, DatasetColumn> singletons = new HashMap<>();
	public static DatasetColumn of(String colName) {
		if(colName == null) throw new IllegalArgumentException("Column name cannot be null");
		if(colName.equals("")) throw new IllegalArgumentException("Column name cannot be blank");

		if(!singletons.containsKey(colName))
			singletons.put(colName, new DatasetColumn(colName));
			
		return singletons.get(colName);
	}
	
	/* Private variables. Mapping, if necessary, comes later. */
	private final String colName;
	public String getName() { return colName; }
	
	/* Simple accessors */
	@Override public String toString() { return "column '" + getName() + "'"; }
	
	/* Constructors */
	private DatasetColumn(String c) {
		colName = c;
	}
}
