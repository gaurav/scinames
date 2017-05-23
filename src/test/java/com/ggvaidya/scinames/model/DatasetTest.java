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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.ggvaidya.scinames.util.SimplifiedDate;

/**
 * Tests for the Change class.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class DatasetTest {
       
	/**
	 * Test serialization.
	 */
	@Test
	public void testSortOrder() {
		Dataset ds1 = new Dataset("ds1", new SimplifiedDate(1930), false);
		Dataset ds2 = new Dataset("ds2", new SimplifiedDate(1940), false);
		Dataset ds3 = new Dataset("ds3", new SimplifiedDate(1950), false);		
		
		assertTrue(ds1.compareTo(ds2) < 0);
		assertTrue(ds3.compareTo(ds2) > 0);
		assertTrue(ds3.compareTo(ds1) > 0);

		Dataset ds4 = new Dataset("ds1", new SimplifiedDate(1930), false);
		assertTrue(ds1.compareTo(ds4) != 0);
	}
}
