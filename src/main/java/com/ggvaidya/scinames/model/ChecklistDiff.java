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

import com.ggvaidya.scinames.util.SimplifiedDate;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.collections.ObservableMap;

/**
 * Used to be a datatype, but now is just loading modules for TaxDiff files.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */

public class ChecklistDiff {
	public static final Logger LOGGER = Logger.getLogger(ChecklistDiff.class.getSimpleName());
	
	// Regular expression patterns to parse a TaxDiff file.
	public static final Pattern pTaxDiffFirstLine = Pattern.compile("^== (\\w+) \\((\\d+)\\) ==(.*)$");
	public static final Pattern pTaxDiffAction = Pattern.compile("^(#)?(IGNORE )?([\\w\\s]+)\\s+(\".+\")\\s+to\\s+(\".+\")\\s*(?:#.*)?$", Pattern.CASE_INSENSITIVE);
	public static final Pattern pTaxDiffAdditionDeletion = Pattern.compile("^(#)?(IGNORE )?(added|deleted)\\s+(.+)\\s*(?:#.*)?$", Pattern.CASE_INSENSITIVE);	
	public static final Pattern pBlankLine = Pattern.compile("^\\s*(##.*)?$");
		// To simplify parsing existing files, single-line comments need TWO hashes.
	public static final Pattern pPropertyLine = Pattern.compile("^ - (\\w+)\\s*:\\s*(.*)\\s*$");
	public static final Pattern pCitationLine = Pattern.compile("^ - citation ([\\w\\:\\-\\,]+) \\((\\d+)\\):\\s*(.+)\\s+\\[(.*)\\]\\s*(.*)?\\s*$", Pattern.CASE_INSENSITIVE);
	public static final Pattern pCitationPropertyLine = Pattern.compile("^  - (\\w+)\\s*:\\s*(.*)\\s*$");
	public static final Pattern pEndOfTaxDiff = Pattern.compile("^== end of checklist ==$", Pattern.CASE_INSENSITIVE);
	
	/* 
		Read ChecklistDiff ("TaxDiff") from file.
	*/
	public static Dataset fromTaxDiffFile(File f) throws IOException {
		LineNumberReader r = new LineNumberReader(new BufferedReader(new FileReader(f)));
		
		// Read header line.
		String header = r.readLine();
		Matcher m = pTaxDiffFirstLine.matcher(header);
		if(!m.matches()) {
			r.close();
			throw new IOException("Header line '" + header + "' doesn't match TaxDiff header!");
		}
			
		String nameStr = m.group(1);
		String yearStr = m.group(2);
		
		Dataset checklistDiff = new Dataset(nameStr, new SimplifiedDate(Integer.parseInt(yearStr), 0, 0), false);
		String line;
		int MARK_READ_AHEAD_LIMIT = 1024 * 1024;
		if(!r.markSupported()) {
			r.close();
			throw new RuntimeException("ChecklistDiff.fromTaxDiffFile requires a Reader than supports marks.");
		}
			
		while((line = r.readLine()) != null) {
			// Every line can be one of two things:
			// (1) A new action, which we will encode as a Change.
			Matcher m_action = pTaxDiffAction.matcher(line);
			Matcher m_additionDeletion = pTaxDiffAdditionDeletion.matcher(line);
			
			if(m_action.matches() || m_additionDeletion.matches()) {
				boolean unchecked;
				boolean ignored;
				String action;
				String from_str;
				String to_str;
				
				if(m_action.matches()) {
					unchecked = (m_action.group(1) != null);
					ignored = (m_action.group(2) != null && m_action.group(2).equalsIgnoreCase("IGNORE "));
					action = m_action.group(3);
					from_str = m_action.group(4);
					to_str = m_action.group(5);
					
				} else if(m_additionDeletion.matches()) {
					unchecked = (m_additionDeletion.group(1) != null);
					ignored = (m_additionDeletion.group(2) != null && m_additionDeletion.group(2).equalsIgnoreCase("IGNORE "));
					action = m_additionDeletion.group(3);
					from_str = m_additionDeletion.group(4);
					to_str = "";
					
					if(action.equalsIgnoreCase("added")) {
						to_str = from_str;
						from_str = "";
					}

				} else {
					throw new RuntimeException("Impossible code branch");
				}
				
				LOGGER.fine("Action: " + action + " from (" + from_str + ") to (" + to_str + "): '" + line + "'");
				
				Change ch;
				
				try {
					ch = new Change(checklistDiff, ChangeType.of(action), from_str, to_str);
				} catch(IllegalStateException e) {
					throw new IOException("Unable to parse change type '" + action + "', from '" + from_str + "' to '" + to_str + "': " + e);
				}
				ch.getProperties().put("unchecked", Boolean.toString(unchecked));
				ch.getProperties().put("ignored", Boolean.toString(ignored));				
					
				r.mark(MARK_READ_AHEAD_LIMIT);
				while((line = r.readLine()) != null) {
					Matcher m_propertyLine = pPropertyLine.matcher(line);
					Matcher m_citationLine = pCitationLine.matcher(line);	
					
					if(pTaxDiffAction.matcher(line).matches()) {
						// These may look like blank lines, so we need to
						// eliminate them first.
						r.reset();
						break;
					} else if(pBlankLine.matcher(line).matches()) {
						// Blank line, ignore.
						LOGGER.fine("Ignoring blank link in action properties: " + line);	
						
					} else if(m_propertyLine.matches()) {
						String name = m_propertyLine.group(1);
						String value = m_propertyLine.group(2);
						
						// Ignore blank values.
						if(!value.trim().equals("")) {
							// Concatenate existing values.
							ObservableMap<String, String> props = ch.getProperties();
							if(props.containsKey(name))
								value = props.get(name) + "\n" + value;
							
							// Set property.
							ch.getProperties().put(name, value);
						}
					} else if(m_citationLine.matches()) {
						String citation_id = m_citationLine.group(1);
						String year = m_citationLine.group(2);		
						String citation_title = m_citationLine.group(3);
						String citation_url = m_citationLine.group(4);
						String reason_string = (m_citationLine.group(5) == null ? "" : m_citationLine.group(5));
						
						Citation citation = new Citation(citation_title, new SimplifiedDate(year));
						citation.getProperties().put("id", citation_id);
						citation.setURL(citation_url);
						
						String[] reasons = reason_string.split("\\s+");
						for(String reason: reasons) {
							if(reason.startsWith("#"))
								reason = reason.substring(1);
							
							citation.getTags().add(Tag.fromName(reason));
						}
						
						r.mark(MARK_READ_AHEAD_LIMIT);
						while((line = r.readLine()) != null) {
							Matcher m_citationPropertyLine = pCitationPropertyLine.matcher(line);
							
							if(pBlankLine.matcher(line).matches()) {
								// Ignore
								LOGGER.fine("Ignoring blank link in citation properties: " + line);
							} else if(m_citationPropertyLine.matches()) {
								String name = m_citationPropertyLine.group(1);
								String value = m_citationPropertyLine.group(2);
								
								// Ignore blank values.
								if(!value.trim().equals("")) {
									// Concatenate existing values.
									ObservableMap<String, String> map = citation.getProperties();
									if(map.containsKey(name))
										value = map.get(name) + "\n" + value;
							
									// Set property.
									citation.getProperties().put(name, value);
								}
							} else {
								r.reset();
								break;
							}
							
							r.mark(MARK_READ_AHEAD_LIMIT);
						}
						
						ch.addCitation(citation);
					} else {
						r.reset();
						break;
					}
					
					r.mark(MARK_READ_AHEAD_LIMIT);
				}
				
				r.close();
				checklistDiff.explicitChangesProperty().add(ch);
			}
			
			// (2) A blank line, containing an optional comment.
			else if(pBlankLine.matcher(line).matches()) {
				// Blank line, ignore.
				LOGGER.fine("Ignoring blank link: " + line);
			}
			
			// (3) The taxdiff file has been terminated correctly!
			else if(pEndOfTaxDiff.matcher(line).matches()) {
				// All done!
				break;
			}
			
			// (4) An error!
			else {
				throw new IOException("Unable to parse non-action line in TaxDiff file: " + line);
			}
			
			r.mark(MARK_READ_AHEAD_LIMIT);
		}
		return checklistDiff;
	}
}
