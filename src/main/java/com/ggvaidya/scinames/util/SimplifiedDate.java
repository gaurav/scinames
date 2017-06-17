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
package com.ggvaidya.scinames.util;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javafx.util.StringConverter;

/**
 * A simplified date is like a LocalDate, but can be a Year or YearMonth as
 * needed. The important thing is that it remembers what has been set, so a
 * SimplifiedDate without a month will only report itself at the level of
 * a Year.
 * 
 * It doesn't currently support times, but there's no reason we can't add that
 * if needed.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class SimplifiedDate implements Comparable<SimplifiedDate> {
	private final int year;
	private final int month;
	private final int day;
	
	/* Prearranged SimplifiedDates */
	public static final SimplifiedDate MIN = new SimplifiedDate(0);

	public static class SimplifiedDateStringConverter extends StringConverter<SimplifiedDate> {
		public String toString(SimplifiedDate sd) {
			return sd.toString();
		}

		public SimplifiedDate fromString(String string) {
			return new SimplifiedDate(string);
		}
	}
	
	/* Accessors */
	public int getYear() { return year; }
	public int getMonth() { return month; }
	public int getDay() { return day; }
	
	/* Higher-level accessors */
	
	/**
	 * Add 'year', 'month' and 'day' attributes to XML Element e.
	 * 
	 * @param e An XML Element to associated this date with
	 */
	
	public void setDateAttributesOnElement(Element e) {
		e.setAttribute("year", Integer.toString(year));
		if(day == 0) {
			if(month != 0) {
				e.setAttribute("month", Integer.toString(month));
			}
		} else {
			e.setAttribute("month", Integer.toString(month));
			e.setAttribute("day", Integer.toString(day));
		}
	}
	
	/**
	 * Return this simplified date as a LocalDate, which might be more
	 * convenient for comparisons. The date will be to the first day of the
	 * Simplified Date - so 1950 becomes January 1, 1950, and so on.
	 * 
	 * @return LocalDate representation of this date
	 */
	public LocalDate getLocalDate() {
		if(year == 0)
			return LocalDate.MIN;
		else if(day == 0) {
			if(month == 0) {
				return LocalDate.of(year, 1, 1);
			} else {
				return LocalDate.of(year, month, 1);
			} 
		} else {
			return LocalDate.of(year, month, day);			
		}
	}
	
	/*
	 * Compare this simplified date with another object. We try to cast it
	 * as a Year, LocalDate or SimplifiedDate; if none of that works, we
	 * throw a ClassCastException.
	 */
	
	public int compareTo(Year year) {
		return getLocalDate().compareTo(year.atDay(1));
	}
	
	@Override
	public int compareTo(SimplifiedDate sd) {
		return getLocalDate().compareTo(sd.getLocalDate());
	}	
	
	public int compareTo(LocalDate ld) {
		return getLocalDate().compareTo(ld);
	}
	
	@Override
	public String toString() {
		if(year == 0)
			return "(none)";
		
		if(day == 0) {
			if(month == 0)
				return Integer.toString(year);
			else
				return Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + year;
		} else {
			return Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + day + ", " + year;
		}
	}
	
	/* Constructors */
	public SimplifiedDate(int year, int month, int day) {
		this.year = year;
		this.month = month;
		this.day = day;
	}
	
	public SimplifiedDate(int year, int month) {
		this(year, month, 0);
	}
	
	public SimplifiedDate(int year) {
		this(year, 0, 0);
	}
	
	public SimplifiedDate() {
		this(0, 0, 0);
	}

	/**
	 * Create a Simplified Date from a string representation. At the moment,
	 * we only support YYYY-MM-DD, and weirdly enough it fails badly if either
	 * MM or DD is one digit instead of two.
	 * 
	 * @param simplifiedDate A string representation of a date.
	 * @throws DateTimeParseException If the date could not be parsed into a SimplifiedDate.
	 */
	public SimplifiedDate(String simplifiedDate) throws DateTimeParseException {
		// Try a series of DateTimeFormatters.
		List<DateTimeFormatter> parsers = Arrays.asList(
			DateTimeFormatter.ofPattern("MMMM d, y"),
			DateTimeFormatter.ofPattern("MMM d, y"),
			DateTimeFormatter.ofPattern("MMM y"),
			DateTimeFormatter.ofPattern("MMMM y"),
			DateTimeFormatter.ofPattern("y-M-d"),
			DateTimeFormatter.ofPattern("y-M"),
			DateTimeFormatter.ofPattern("y")
		);
		
		DateTimeParseException firstParseException = null;
		
		for(DateTimeFormatter parser: parsers) {
			TemporalAccessor ta;
			
			try {
				ta = parser.parseBest(simplifiedDate, LocalDate::from, YearMonth::from, Year::from);
			} catch(DateTimeParseException ex) {
				if(firstParseException == null)
					firstParseException = ex;
				
				continue;
			}
			
			if(ta instanceof LocalDate) {
				LocalDate ld = (LocalDate) ta;
				
				year = ld.getYear();
				month = ld.getMonthValue();
				day = ld.getDayOfMonth();
				
				return;
			} else if(ta instanceof YearMonth) {
				YearMonth ym = (YearMonth) ta;
				
				year = ym.getYear();
				month = ym.getMonthValue();
				day = 0;
				
				return;
			} else if(ta instanceof Year) {
				Year yr = (Year) ta;
				
				year = yr.getValue();
				month = 0;
				day = 0;
				
				return;
			} else {
				throw new RuntimeException("Unexpected temporal accessor while parsing simplified date '" + simplifiedDate + "': " + ta);
			}
		}
		
		if(firstParseException == null) {
			throw new RuntimeException("No exception through, no parser matched; what?");
		} else throw firstParseException;
	}
	
	/**
	 * Create a SimplifiedDate from an XML Node.
	 * 
	 * This is the opposite of 
	 * {@link #setDateAttributesOnElement(org.w3c.dom.Element) setDateAttributesOnElement}.
	 * 
	 * @param n An XML Node with 'year', 'month' and 'day' elements.
	 */
	public SimplifiedDate(Node n) {
		NamedNodeMap attr = n.getAttributes();
		
		Node node_year = attr.getNamedItem("year");
		if(node_year != null)
			year = Integer.parseInt(node_year.getNodeValue());
		else
			year = 0;
		
		Node node_month = attr.getNamedItem("month");
		if(node_month != null)
			month = Integer.parseInt(node_month.getNodeValue());
		else
			month = 0;
		
		Node node_day = attr.getNamedItem("day");
		if(node_day != null)
			day = Integer.parseInt(node_day.getNodeValue());
		else
			day = 0;
	}
	
	public SimplifiedDate(Map<String, String> attr) {
		year = Integer.parseInt(attr.getOrDefault("year", "0"));
		month = Integer.parseInt(attr.getOrDefault("month", "0"));
		day = Integer.parseInt(attr.getOrDefault("day", "0"));
	}

	public String asYYYYmmDD(String separator) {
		StringBuilder builder = new StringBuilder();
		builder.append(year);
		
		if(month == 0)
			return builder.toString();
		
		builder.append(separator);
		
		if(month >= 10)
			builder.append(month);
		else {
			builder.append('0');
			builder.append(month);
		}
		
		if(day == 0)
			return builder.toString();
		
		builder.append(separator);
		
		if(day >= 10)
			builder.append(day);
		else {
			builder.append('0');
			builder.append(day);
		}
		
		return builder.toString();
	}

	public String getYearAsString() {
		if(year == 0)
			return "NA";
		else
			return String.valueOf(year);
	}
}