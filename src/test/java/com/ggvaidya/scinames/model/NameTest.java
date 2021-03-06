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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Tests for the Name class.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class NameTest {
    
        private void testFullName(Optional<Name> opt, String expectedFullName) {
            assertTrue(opt.isPresent());
            testFullName(opt.get(), expectedFullName);
        }
	
        private void testFullName(Name n, String expectedFullName) {
            assertEquals(expectedFullName, n.getFullName());
        }
        
        private void testProperty(Optional<Name> opt, String desc, Function<Name, Boolean> test) {
            assertTrue(opt.isPresent());
            assertTrue(test.apply(opt.get()), desc);
        }
        
        private void testAssertion(Optional<Name> opt, Consumer<Name> test) {
            assertTrue(opt.isPresent());
            test.accept(opt.get());
        }
        
	/**
	 * Test of get method, of class Name.
	 */
	@Test
	public void testGet() {
        testFullName(Name.get("Alpha", "beta", "gamma"), "Alpha beta gamma");
        testFullName(Name.get("Alpha", "beta ", "gamma"), "Alpha beta gamma");
        testFullName(Name.get("Alpha", "beta"), "Alpha beta");
        testFullName(Name.getFromFullName("Alpha beta"), "Alpha beta");
        testFullName(Name.getFromFullName("      Alpha beta       "), "Alpha beta");
            
        // Genus names
        testFullName(Name.getFromFullName("Alpha"), "Alpha");
        testAssertion(Name.getFromFullName("Alpha"), n -> assertEquals(null, n.getBinomialName()));
        testAssertion(Name.getFromFullName("Alpha"), n -> assertEquals(n.asBinomial().count(), 0));
        testFullName(Name.getFromFullName("Alpha sp"), "Alpha sp sp");
        testAssertion(Name.getFromFullName("Alpha sp"), n -> assertFalse(n.hasSpecificEpithet()));
        testAssertion(Name.getFromFullName("Alpha sp"), n -> assertEquals(null, n.getBinomialName()));
        testAssertion(Name.getFromFullName("Alpha sp"), n -> assertEquals(n.asBinomial().count(), 0));
        
        // With quotes
        testFullName(Name.getFromFullName("'Alpha'"), "Alpha");
        assertEquals(Name.getFromFullName("'Alpha' beta"), Optional.empty());
        testFullName(Name.getFromFullName("'Alpha beta'"), "Alpha beta");
    
        // Subspecies
        testProperty(Name.getFromFullName("Alpha beta    subsp gamma   "), "getInfraspecificEpithetsAsString() should be 'subsp gamma'", n -> n.getInfraspecificEpithetsAsString().equals("subsp gamma"));
    }
	
	/**
	 * Can we 
	 */
	@Test
	public void testAsBinomial() {
		List<Optional<Name>> namesFromString = Arrays.asList(
			Name.getFromFullName("Panthera tigris"),
			Name.getFromFullName("Panthera tigris tigris"),
			Name.getFromFullName("Mus musculus"),
			Name.getFromFullName("Homo sapiens sapiens"),
			Name.getFromFullName("Branta canadensis subsp. hutchinsii"),
			Name.getFromFullName("Branta"),
			Name.getFromFullName("Yersinia")
		);
		List<Name> names = namesFromString.stream().map(opt -> opt.orElse(null)).collect(Collectors.toList());
		
		// Did they all parse?
		assertFalse(names.contains(null));
		
		// If we collapse to binomial, do we get the expected list?
		List<Name> expected = Arrays.asList(
			Name.get("Panthera", "tigris"),
			Name.get("Panthera", "tigris"),
			Name.get("Mus", "musculus"),
			Name.get("Homo", "sapiens"),
			Name.get("Branta", "canadensis")
		);
		assertEquals(
			names.stream().flatMap(n -> n.asBinomial()).collect(Collectors.toList()),
			expected
		);
	}
	
	@Test
	public void testNameParsingFromAndStrings() {
		assertEquals(
			Change.convertAndStringToNames("\"Dryobates arizonae\" and \"Dryobates stricklandi\"").collect(Collectors.toSet()),
			new HashSet<>(Arrays.asList(Name.get("Dryobates", "arizonae"), Name.get("Dryobates", "stricklandi")))
		);
	}

	@Test
	public void testNameSorting() {
		List<Name> listToSort = Arrays.asList(Name.get("Mus", "musculus"), Name.get("Homo", "sapiens"), Name.EMPTY);
		listToSort.sort(null);
		
		assertEquals(
			Arrays.asList(Name.EMPTY, Name.get("Homo", "sapiens"), Name.get("Mus", "musculus")),
			listToSort
		);
	}

	@Test
	public void testOddNames() {
		Name n1 = Name.getFromFullName("Anabaena füllebornii").get();
		assertTrue(n1.hasSpecificEpithet());
		assertEquals(n1.getGenus(), "Anabaena");
		assertEquals(n1.getSpecificEpithet(), "füllebornii");
		
	}
	
	@Test
	public void testInfraspecificEpithets() {
		// TODO
	}
}
