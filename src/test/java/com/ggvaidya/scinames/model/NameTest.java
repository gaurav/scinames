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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

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
        
        private void testProperty(Name n, String desc, Function<Name, Boolean> test) {
            assertTrue(test.apply(n), desc);
        }
    
        private void testProperty(Optional<Name> opt, String desc, Function<Name, Boolean> test) {
            assertTrue(opt.isPresent());
            assertTrue(test.apply(opt.get()), desc);
        }
        
        private void testAssertion(Name n, Consumer<Name> test) {
            test.accept(n);
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
            testAssertion(Name.getFromFullName("Alpha"), n -> assertFalse(n.asBinomial().isPresent()));
            testFullName(Name.getFromFullName("Alpha sp"), "Alpha sp sp");
            testAssertion(Name.getFromFullName("Alpha sp"), n -> assertFalse(n.hasSpecificEpithet()));
            testAssertion(Name.getFromFullName("Alpha sp"), n -> assertEquals(null, n.getBinomialName()));
            testAssertion(Name.getFromFullName("Alpha sp"), n -> assertFalse(n.asBinomial().isPresent()));
        
            // Subspecies
            testProperty(Name.getFromFullName("Alpha beta    subsp gamma   "), "getInfraspecificEpithetsAsString() should be 'subsp gamma'", n -> n.getInfraspecificEpithetsAsString().equals("subsp gamma"));
        }
}
