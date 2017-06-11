
/*
 *
 *  NameExtractorFactory
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

import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Name;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The NameExtractorFactory provides a set of NameExtractors that can be used
 * to extract names from dataset rows. There are different strategies that can
 * be employed in different combinations.
 * 
 * More importantly, each of these extractors get a unique name here, so
 * that a series of 
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class NameExtractorFactory {
	public static final Logger LOGGER = Logger.getLogger(NameExtractorFactory.class.getSimpleName());
	
	public static Set<Name> extractNamesUsingExtractors(List<NameExtractor> extractors, DatasetRow row) {
		return extractNamesUsingExtractors(extractors, row, false);
	}
	
	public static Set<Name> extractNamesUsingExtractors(List<NameExtractor> extractors, DatasetRow row, boolean findAllNames) {
		Set<Name> names = new HashSet<>();
		long startApplyName = System.nanoTime();
		
		for(NameExtractor extractor: extractors) {
			names.addAll(extractor.extractRow(row));
			if(!names.isEmpty() && !findAllNames) {
				LOGGER.fine("extractNamesUsingExtractors() extracted " + names.size() + " in " + (System.nanoTime() - startApplyName)/1e6d + " seconds.");
				return names;
			} else {
				LOGGER.fine("Name extractor " + extractor + " failed, falling back to next.");
			}
		}
				
		// System.err.println(" - extractNamesUsingExtractors() extracted " + names.size() + " in " + (System.nanoTime() - startApplyName)/1e6d + " seconds.");
		return names;
	}
	
	public static List<NameExtractor> getExtractors(String parse) throws NameExtractorParseException {
		LinkedList<NameExtractor> list = new LinkedList<>();
		
		if(parse == null || parse.equals(""))
			return list;
		
		String[] extractors = parse.split("\\s+or\\s+");
		for (String extractor: extractors) {
			list.add(getSingleExtractor(extractor));
		}
		
		return list;
	}
	
	public static String serializeExtractorsToString(List<NameExtractor> extractors) {
		return extractors.stream().map(ex -> ex.serializeToString()).collect(Collectors.joining(" or "));
	}
	
	/**
	 * Return an extractor for a particular string.
	 * 
	 * @param parse
	 * @return 
	 */
	public static NameExtractor getSingleExtractor(String parse) throws NameExtractorParseException {
		Pattern extractorPattern = Pattern.compile("\\s*(\\w+)\\(([\\w,\\s]+)\\)\\s*");
		Matcher matcher = extractorPattern.matcher(parse);
		
		if(!matcher.matches()) throw new NameExtractorParseException("Could not parse extractor '" + parse + "': expected 'function(value, value, ...)' format");

		String name = matcher.group(1);
		String arguments = matcher.group(2);
		
		switch(name) {
			case "scientificName": return new NameExtractor("scientificName", 1, 1, arguments, 
				(extractor, row) -> {
					try {
						return extractor.applyFunction(Name::getFromFullName, 0, row);
					} catch(NameExtractorParseException ex) {
						// TODO: create some kind of logging mechanism.
						LOGGER.severe("Error during name parsing: " + ex);
						return new HashSet<>();
					}
				}
			);
			
			case "binomialName": return new NameExtractor("binomialName", 1, 1, arguments, 
				(extractor, row) -> {
					try {
						return extractor.applyFunction(Name::getFromFullName, 0, row)
							.stream().flatMap(n -> n.asBinomial()).collect(Collectors.toSet());
					} catch(NameExtractorParseException ex) {
						// TODO: create some kind of logging mechanism.
						LOGGER.severe("Error during name parsing: " + ex);
						return new HashSet<>();
					}
				}
			);
			
			case "genusAndEpithets": return new NameExtractor("genusAndEpithets", 1, 3, arguments, 
				(extractor, row) -> {
					HashSet<Name> names = new HashSet<>();
					String[] args = extractor.getArguments();
					
					String genusCol = args[0];
					String specificEpithetCol = args[1];
					
					String genus = row.get(DatasetColumn.of(genusCol));
					String specificEpithet = row.get(DatasetColumn.of(specificEpithetCol));
					
					if(genus == null)
						return names;
					
					else if(specificEpithet == null)
						names.add(Name.getFromGenus(genus));
					
					else if(args.length > 2) {
						String subspecificEpithetCol = args[2];
						String subspecificEpithet = row.get(DatasetColumn.of(subspecificEpithetCol));
						
						names.add(Name.get(genus, specificEpithet, subspecificEpithet));
					} else {
						names.add(Name.get(genus, specificEpithet));			
					}
					
					return names;
				}
			);
		}
		
		throw new NameExtractorParseException("Could not parse extractor '" + parse + "': name '" + name + "' unknown.");
	}
	
	
	public static String getSupportedExtractors() {
		return 
			"The following extractors are supported (separated by \"or\"):\n"
			+ "\t - scientificName(colName): full scientific name\n"
			+ "\t - binomialName(colName): read as full scientific name, but truncate to binomial name\n"
			+ "\t - genusAndEpithets(genusCol, specificEpithetCol): genus and species name\n"
			+ "\t - genusAndEpithets(genusCol, specificEpithetCol, subspecificEpithet): genus, species name and subspecific information\n"				
		;
	}

	public static List<NameExtractor> getDefaultExtractors() {
		try {
			return getExtractors(getDefaultExtractorsAsString());
		} catch(NameExtractorParseException ex) {
			return new LinkedList<>();
		}
	}
	
	public static String getDefaultExtractorsAsString() {
		return "scientificName(scientificName)" +
			" or genusAndEpithets(genus, specificEpithet, subspecificEpithet)" +
			" or genusAndEpithets(genus, specificEpithet)" +
			" or genusAndEpithets(genus, species)" +
			" or scientificName(species)";
	}
}