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

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.ggvaidya.scinames.util.SimplifiedDate;

/**
 * Tests for the Synonymy class.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class SynonymyTest {
       
	/**
	 * Test serialization.
	 */
	@Test
	public void testSynonymyDistinctification() {
		Dataset ds1 = new Dataset("ds1", new SimplifiedDate(1930), Dataset.TYPE_DATASET);
		Synonymy syn1 = new Synonymy(Name.get("Felis", "tigris"), Name.get("Panthera", "tigris"), ds1, "Synonym 1");
		Synonymy syn2 = new Synonymy(Name.get("Felis", "tigris"), Name.get("Panthera", "tigris"), ds1, "Synonym 2");		
		
		assertTrue(syn1.equals(syn2));
		assertTrue(syn2.equals(syn1));
		assertEquals(syn1, syn2);
		
		Stream<Synonymy> syns = Stream.of(syn1, syn2);
		assertEquals(1, syns.distinct().count());
	}
}
