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
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.ggvaidya.scinames.util.SimplifiedDate;

/**
 * Tests for the TaxonConcept class.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class TaxonConceptTest {
	public static final Logger LOGGER = Logger.getLogger(TaxonConceptTest.class.getSimpleName());
	
	Dataset ds1 = new Dataset("ds1", new SimplifiedDate(1930), false);	
	Dataset ds2 = new Dataset("ds2", new SimplifiedDate(1940), false);	
	Dataset ds3 = new Dataset("ds3", new SimplifiedDate(1950), false);	
	Dataset ds4 = new Dataset("ds4", new SimplifiedDate(1960), false);
	
	private Stream<Change> streamNamesToAdditions(Dataset ds, Name... names) {
		return Arrays.asList(names).stream().map(n -> new Change(
			ds,
			ChangeType.ADDITION,
			Stream.empty(),
			Stream.of(n)
		));
	}
	
	private Change rename(Dataset ds, Name from, Name to) {
		return new Change(
			ds,
			ChangeType.RENAME,
			Stream.of(from),
			Stream.of(to)
		);
	}
	
	private Project buildProject() {
		Project project = new Project();
			
		ds1.explicitChangesProperty().addAll(
			streamNamesToAdditions(ds1,
				Name.get("Branta", "canadensis"),
				Name.get("Branta", "canadensis", "hutchinsii"),
				Name.get("Platypus", "anatinus")
			).collect(Collectors.toList())
		);
		project.addDataset(ds1);
		
		project.addDataset(ds2);
		
		ds3.explicitChangesProperty().addAll(
			Stream.concat(streamNamesToAdditions(ds3,
				Name.get("Ornithorhynchus", "paradoxus")
			), Stream.of(
				new Change(ds3, ChangeType.RENAME, 
					Stream.of(Name.get("Platypus", "anatinus")), 
					Stream.of(Name.get("Ornithorhynchus", "anatinus"))
				),
				new Change(ds3, ChangeType.DELETION, Stream.of(Name.get("Branta", "canadensis", "hutchinsii")), Stream.empty()),
				new Change(ds3, ChangeType.SPLIT, Stream.of(Name.get("Branta", "canadensis")), Stream.of(
					Name.get("Branta", "canadensis"),
					Name.get("Branta", "hutchinsii")
				))
			)).collect(Collectors.toList())
		);
		project.addDataset(ds3);
		
		ds4.explicitChangesProperty().addAll(
			Stream.concat(streamNamesToAdditions(ds4
			), Stream.of(
				new Change(ds4, ChangeType.LUMP, 
					Stream.of(Name.get("Ornithorhynchus", "paradoxus"), Name.get("Ornithorhynchus", "anatinus")),
					Stream.of(Name.get("Ornithorhynchus", "anatinus"))
				)
			)).collect(Collectors.toList())
		);
		project.addDataset(ds4);
		
		return project;
	}
	
	private Set<Name> setOfNames(Name... names) {
		return new HashSet<Name>(
			Arrays.asList(names)	
		);
	}
	
    /**
     * Test identifying taxon concepts.
     */
	@Test
	public void testTaxonConceptCreation() {
		Project project = buildProject();
		
		assertEquals(4, project.getDatasets().size());
		
		Dataset last = project.getLastDataset().get();
		assertEquals(last, ds4);

		LOGGER.fine("ds1: " + ds1.getAllChanges().map(ch -> ch.toString()).collect(Collectors.joining("\n - ")));		
		assertEquals(3, ds1.getAllChanges().count());
		assertEquals(setOfNames(
			Name.get("Branta", "canadensis"),
			Name.get("Branta", "canadensis", "hutchinsii"),
			Name.get("Platypus", "anatinus")
		), ds1.getRecognizedNames(project).collect(Collectors.toSet()));
		
		LOGGER.fine("ds2: " + ds2.getAllChanges().map(ch -> ch.toString()).collect(Collectors.joining("\n - ")));		
		assertEquals(0, ds2.getAllChanges().count());
		assertEquals(setOfNames(
			Name.get("Branta", "canadensis"),
			Name.get("Branta", "canadensis", "hutchinsii"),
			Name.get("Platypus", "anatinus")
		), ds2.getRecognizedNames(project).collect(Collectors.toSet()));
		
		LOGGER.fine("ds3: " + ds3.getAllChanges().map(ch -> ch.toString()).collect(Collectors.joining("\n - ")));
		assertEquals(4, ds3.getAllChanges().count());	
		assertEquals(setOfNames(
			Name.get("Branta", "canadensis"),
			Name.get("Branta", "hutchinsii"),			
			Name.get("Ornithorhynchus", "anatinus"),
			Name.get("Ornithorhynchus", "paradoxus")
		), ds3.getRecognizedNames(project).collect(Collectors.toSet()));
		
		LOGGER.fine("ds4: " + ds4.getAllChanges().map(ch -> ch.toString()).collect(Collectors.joining("\n - ")));
		assertEquals(1, ds4.getAllChanges().count());
		assertEquals(setOfNames(
			Name.get("Branta", "canadensis"),
			Name.get("Branta", "hutchinsii"),			
			Name.get("Ornithorhynchus", "anatinus")
		), ds4.getRecognizedNames(project).collect(Collectors.toSet()));
		
		assertEquals(
			setOfNames(
				Name.get("Branta", "canadensis"),
				Name.get("Branta", "hutchinsii"),
				Name.get("Ornithorhynchus", "anatinus")
			),
			last.getRecognizedNames(project).collect(Collectors.toSet())
		);
	}
}
