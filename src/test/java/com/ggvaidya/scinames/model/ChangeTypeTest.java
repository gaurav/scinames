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

import org.junit.jupiter.api.Test;

/**
 * Tests for the ChangeType class.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class ChangeTypeTest {
       
	/**
	 * Test the ChangeType class.
	 */
	@Test
	public void testChangeType() {
		// Make sure we can get singletons correctly.
		assertEquals(ChangeType.of("nobody"), ChangeType.of("nobody"));
		
		// Test all inversions.
		assertEquals(ChangeType.ADDITION.invert(), ChangeType.DELETION);
		assertEquals(ChangeType.DELETION.invert(), ChangeType.ADDITION);
		assertEquals(ChangeType.RENAME.invert(), ChangeType.RENAME);
		assertEquals(ChangeType.LUMP.invert(), ChangeType.SPLIT);		
		assertEquals(ChangeType.SPLIT.invert(), ChangeType.LUMP);		
		assertEquals(ChangeType.ERROR.invert(), ChangeType.ERROR);
		
		// We should exactly five recognized change types.
		assertEquals(ChangeType.RECOGNIZED_TYPES.size(), 6);
	}
}
