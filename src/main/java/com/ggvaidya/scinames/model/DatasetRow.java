
/*
 *
 *  DatasetRow
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
import java.util.Set;

/**
 *
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetRow {
	private Map<DatasetColumn, String> data = new HashMap<>();
	
	public DatasetRow() {}
	public DatasetRow(Map<DatasetColumn, String> entries) {
		data.putAll(entries);
	}
	public void putAll(Map<String, String> map) {
		for(String key: map.keySet()) {
			put(key, map.get(key));
		}
	}

	public String get(DatasetColumn col)			{ return data.get(col); }
	public String get(String colName)				{ return data.get(DatasetColumn.of(colName)); }	
	public void put(DatasetColumn col, String val)	{ data.put(col, val); }	
	public void put(String colName, String val)		{ data.put(DatasetColumn.of(colName), val); }		
	public Set<DatasetColumn> getColumns()			{ return data.keySet(); }
	public boolean hasColumn(DatasetColumn col)		{ return data.containsKey(col); }
	public boolean hasColumn(String colName)		{ return data.containsKey(DatasetColumn.of(colName)); }	
}
