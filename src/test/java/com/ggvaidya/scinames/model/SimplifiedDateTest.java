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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.ggvaidya.scinames.util.SimplifiedDate;

/**
 * Tests for the SimplifiedDate class.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class SimplifiedDateTest {
       
	/**
	 * Created simplified dates.
	 */
	@Test
	public void testCreateSimplifiedDates() {
		assertEquals(
			LocalDate.of(2009, 4, 17),
			new SimplifiedDate("2009-04-17").getLocalDate()
		);
		
		assertEquals(
			LocalDate.of(2009, 4, 17),
			new SimplifiedDate("2009-04-17").getLocalDate()
		);
		
		// Test whether it supports shorter strings.
		assertEquals(
			LocalDate.of(2009, 4, 7),
			new SimplifiedDate("2009-4-7").getLocalDate()
		);
		
		assertEquals(
			LocalDate.of(2009, 4, 1),
			new SimplifiedDate("2009-4").getLocalDate()
		);
		
		assertEquals(
			LocalDate.of(2009, 1, 1),
			new SimplifiedDate("2009").getLocalDate()
		);
	}
	
	@Test
	public void displaySimplifiedDates() {
		assertEquals(
			"October 3, 2017",
			new SimplifiedDate("2017-10-03").toString()
		);
	}
}
