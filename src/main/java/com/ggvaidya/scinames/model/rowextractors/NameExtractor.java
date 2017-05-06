
/*
 *
 *  NameExtractor
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

package com.ggvaidya.scinames.model.rowextractors;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Name;

/**
 * A NameExtractor extracts names from rows. There are different strategies
 * for doing this, so we have an interface for pulling that off.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class NameExtractor {
	public NameExtractor(String n, int min, int max, String[] a, BiFunction<NameExtractor, DatasetRow, Set<Name>> rm) {
		name = n;
		rowMapper = rm;
		
		if(a.length < min || a.length > max) {
			String arguments = String.join(", ", a);
			throw new IllegalArgumentException(
				"Name extractor '" + name + "(" + arguments + ")' has " + a.length 
				+ " arguments, outside of range (" + min + ", " + max + ")"
			);
		}
		
		arguments = a;
		
		// System.err.println(" - NameExtractor " + name + " created with arguments " + Arrays.asList(arguments).toString());
	}
	
	public NameExtractor(String n, int min, int max, String argument, BiFunction<NameExtractor, DatasetRow, Set<Name>> rm) {
		this(n, min, max, argument.split("\\s*,\\s*"), rm);
	}
	
	public Set<Name> applyFunction(Function<String, Optional<Name>> func, int argIndex, DatasetRow row) throws NameExtractorParseException {
		Set<Name> results = new HashSet<>();
		
		// Do we have an argument value?
		if(argIndex < 0 || argIndex >= arguments.length) throw new NameExtractorParseException("Argument " + argIndex + " not found in name extractor " + getName());
		String arg = arguments[argIndex];
		
		// Get a row value
		if(row.hasColumn(arg)) {
			Optional<Name> name = func.apply(row.get(DatasetColumn.of(arg)));
			if(name.isPresent()) results.add(name.get());
		} else
			return new HashSet<>();
			// throw new NameExtractorParseException("Column '" + arg + "' missing in row " + row);
		
		return results;
	}
	
	public Set<Name> applyFunction(BiFunction<String, String, Name> func, int argIndex1, int argIndex2, DatasetRow row) {
		Set<Name> results = new HashSet<>();
		
		// Do we have an argument value?
		if(argIndex1 >= arguments.length || argIndex2 >= arguments.length) return results;
		String arg1 = arguments[argIndex1];
		String arg2 = arguments[argIndex2];
		
		// Get a row value
		if(row.hasColumn(arg1)) {
			if(row.hasColumn(arg2)) {
				results.add(func.apply(row.get(arg1), row.get(arg2)));
			} else {
				// One argument! We'll try passing null.
				results.add(func.apply(row.get(arg1), null));
			}
		} else {
			// No arguments!
		}
		
		return results;
	}
	
	public Set<Name> applyFunctionWithFallback(BiFunction<String, String, Name> func, Function<String, Name> fallback, int argIndex1, int argIndex2, DatasetRow row) {
		Set<Name> results = new HashSet<>();
		
		// Do we have an argument value?
		if(argIndex1 >= arguments.length || argIndex2 >= arguments.length) return results;
		String arg1 = arguments[argIndex1];
		String arg2 = arguments[argIndex2];
		
		// Get a row value
		if(row.hasColumn(arg1)) {
			if(row.hasColumn(arg2)) {
				results.add(func.apply(row.get(arg1), row.get(arg2)));
			} else {
				results.add(fallback.apply(row.get(arg1)));
			}
		} else {
			// No arguments!
		}
		
		return results;
	}
	
	public static Set<Name> createOneNameSet(Name n) {
		if(n == null)
			throw new IllegalArgumentException("createOneNameSet called with null");
		
		Set<Name> set = new HashSet<Name>();
		set.add(n);
		return set;
	}
	
	/* So we know what this name extractor is called. */
	private String name;
	public String getName() { return name; }

	/* 
	 * NameExtractor are serialized by using "arguments". Arguments are 
	 * comma-and-newline-separated strings that don't contain brackets, so that 
	 * the entire NameExtractor can be serialized as "name(arg1, args2)".
	 */
	private String[] arguments;
	public String[] getArguments()			{ return arguments; }
	public void setArguments(String[] a)	{ arguments = a; }
	
	public String serializeToString() {
		return getName() + "(" + String.join(", ", arguments) + ")";
	}
	
	@Override
	public String toString() { return serializeToString(); }
	
	/*
	 * NameExtractors are run with an underlying function.
	 */
	private BiFunction<NameExtractor, DatasetRow, Set<Name>> rowMapper;
	
	public Set<Name> extractRow(DatasetRow row) {
		return rowMapper.apply(this, row);
	}
	
	public List<Set<Name>> extractAll(Dataset dataset) {
		return dataset.getRowsAsStream()
			.map(row -> rowMapper.apply(this, row))
			.collect(Collectors.toList());
	}
}
