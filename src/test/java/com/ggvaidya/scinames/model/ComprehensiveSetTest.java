package com.ggvaidya.scinames.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.collections.ObservableSet;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A Comprehensive Set Test tests a set against a corresponding set of filters.
 * Each filter must match exactly one object in the set, and NOT match ANY
 * other items in the set. In this way, we can test whether a set contains
 * expectedly shaped elements without testing whether it contains a particular
 * set of elements.
 * 
 * (The real solution is to use Scala and case classes, but alas.)
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 *
 */
public class ComprehensiveSetTest {
	public static <T> void test(String setName, Set<T> objects, List<Predicate<T>> filters) {
		HashSet<T> objectsMatched = new HashSet<>();

		assertEquals(objects.size(), filters.size(), "Filter count should be the same as set count");
		
		for(Predicate<T> filter: filters) {
			Set<T> matchedObjects = objects.stream().filter(filter).collect(Collectors.toSet());
			
			if(matchedObjects.size() == 0) {
				fail("No object in set " + setName + " matched filter " + filter + ": " + objects);
			} else if(matchedObjects.size() > 1) {
				fail(matchedObjects.size() + " matches in set " + setName + " for filter " + filter + ": " + objects);
			} else {
				// Exactly one match! Perfect.
				T match = matchedObjects.iterator().next();
				
				// This should be logically impossible, but just in case ...
				if(objectsMatched.contains(match))
					throw new RuntimeException("Duplicate objects matched: " + match);
				
				// Record that we found it.
				objectsMatched.add(match);
			}
		};
		
		assertEquals(objects, objectsMatched, "Comparing matched objects with set objects");
	}

	public static <T> boolean containsOnly(Set<T> set, List<T> expected) {
		return (
			set.size() == expected.size() &&
			expected.stream().allMatch(exp -> set.contains(exp))
		);
	}
}
