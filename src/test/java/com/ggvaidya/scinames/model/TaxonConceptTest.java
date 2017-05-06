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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.ggvaidya.scinames.model.filters.IgnoreErrorChangeTypeFilter;
import com.ggvaidya.scinames.util.SimplifiedDate;

/**
 * Tests for the TaxonConcept class.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class TaxonConceptTest {
	public static final Logger LOGGER = Logger.getLogger(TaxonConceptTest.class.getSimpleName());
	
	Dataset ds10 = new Dataset("ds10", new SimplifiedDate(1930), false);
	Dataset ds15 = new Dataset("ds15", new SimplifiedDate(1935), false);
	Dataset ds20 = new Dataset("ds20", new SimplifiedDate(1940), false);	
	Dataset ds30 = new Dataset("ds30", new SimplifiedDate(1950), false);	
	Dataset ds40 = new Dataset("ds40", new SimplifiedDate(1960), false);
	
	private Stream<Change> streamNamesToAdditions(Dataset ds, Name... names) {
		return Arrays.asList(names).stream().map(n -> new Change(
			ds,
			ChangeType.ADDITION,
			Stream.empty(),
			Stream.of(n)
		));
	}
	
	private Project buildProject() {
		Project project = new Project();
			
		ds10.explicitChangesProperty().addAll(
			streamNamesToAdditions(ds10,
				Name.get("Branta", "canadensis"),
				Name.get("Branta", "hutchinsii"),
				Name.get("Platypus", "anatinus")
			).collect(Collectors.toList())
		);
		project.addDataset(ds10);
		
		ds15.explicitChangesProperty().addAll(
			new Change(ds15, ChangeType.LUMP,
				Stream.of(Name.get("Branta", "canadensis"), Name.get("Branta", "hutchinsii")),
				Stream.of(Name.get("Branta", "canadensis"), Name.get("Branta", "canadensis", "hutchinsii"))
			)
		);
		project.addDataset(ds15);
		
		project.addDataset(ds20);
		
		ds30.explicitChangesProperty().addAll(
			Stream.concat(streamNamesToAdditions(ds30,
				Name.get("Ornithorhynchus", "paradoxus")
			), Stream.of(
				new Change(ds30, ChangeType.RENAME, 
					Stream.of(Name.get("Platypus", "anatinus")), 
					Stream.of(Name.get("Ornithorhynchus", "anatinus"))
				),
				new Change(ds30, ChangeType.DELETION, Stream.of(Name.get("Branta", "canadensis", "hutchinsii")), Stream.empty()),
				new Change(ds30, ChangeType.ERROR, Stream.of(Name.get("Branta", "canadensis")), Stream.of(
					Name.get("Branta", "canadensis"),
					Name.get("Branta", "hutchinsii")
				))
			)).collect(Collectors.toList())
		);
		project.addDataset(ds30);
		
		ds40.explicitChangesProperty().addAll(
			Stream.concat(streamNamesToAdditions(ds40
			), Stream.of(
				new Change(ds40, ChangeType.LUMP, 
					Stream.of(Name.get("Ornithorhynchus", "paradoxus"), Name.get("Ornithorhynchus", "anatinus")),
					Stream.of(Name.get("Ornithorhynchus", "anatinus"))
				),
				new Change(ds40, ChangeType.SPLIT, Stream.of(Name.get("Branta", "canadensis")), Stream.of(
						Name.get("Branta", "canadensis"),
						Name.get("Branta", "hutchinsii")
					))
			)).collect(Collectors.toList())
		);
		project.addDataset(ds40);
		
		return project;
	}
	
	private Set<Name> setOfNames(Name... names) {
		return new HashSet<Name>(
			Arrays.asList(names)	
		);
	}
	
    /**
     * Test whether the right changes and recognized names show up.
     */
	@Test
	public void testChangesAffectingRecognizedNamesOverTime() {
		Project project = buildProject();
		project.addChangeFilter(new IgnoreErrorChangeTypeFilter(project, true));
		
		assertEquals(4, project.getDatasets().size());
		
		Dataset last = project.getLastDataset().get();
		assertEquals(last, ds40);

		LOGGER.fine("ds10: " + ds10.getAllChanges().map(ch -> ch.toString()).collect(Collectors.joining("\n - ")));		
		assertEquals(3, ds10.getAllChanges().count());
		assertEquals(setOfNames(
			Name.get("Branta", "canadensis"),
			Name.get("Branta", "canadensis", "hutchinsii"),
			Name.get("Platypus", "anatinus")
		), ds10.getRecognizedNames(project).collect(Collectors.toSet()));
		
		LOGGER.fine("ds20: " + ds20.getAllChanges().map(ch -> ch.toString()).collect(Collectors.joining("\n - ")));		
		assertEquals(0, ds20.getAllChanges().count());
		assertEquals(setOfNames(
			Name.get("Branta", "canadensis"),
			Name.get("Branta", "canadensis", "hutchinsii"),
			Name.get("Platypus", "anatinus")
		), ds20.getRecognizedNames(project).collect(Collectors.toSet()));
		
		LOGGER.fine("ds30: " + ds30.getAllChanges().map(ch -> ch.toString()).collect(Collectors.joining("\n - ")));
		assertEquals(4, ds30.getAllChanges().count());	
		assertEquals(setOfNames(
			Name.get("Branta", "canadensis"),
			Name.get("Branta", "hutchinsii"),			
			Name.get("Ornithorhynchus", "anatinus"),
			Name.get("Ornithorhynchus", "paradoxus")
		), ds30.getRecognizedNames(project).collect(Collectors.toSet()));
		
		LOGGER.fine("ds40: " + ds40.getAllChanges().map(ch -> ch.toString()).collect(Collectors.joining("\n - ")));
		assertEquals(1, ds40.getAllChanges().count());
		assertEquals(setOfNames(
			Name.get("Branta", "canadensis"),
			Name.get("Branta", "hutchinsii"),			
			Name.get("Ornithorhynchus", "anatinus")
		), ds40.getRecognizedNames(project).collect(Collectors.toSet()));
		
		assertEquals(
			setOfNames(
				Name.get("Branta", "canadensis"),
				Name.get("Branta", "hutchinsii"),
				Name.get("Ornithorhynchus", "anatinus")
			),
			last.getRecognizedNames(project).collect(Collectors.toSet())
		);
	}
	
    /**
     * Test identifying taxon concepts from sets of lumps and splits.
     */
	@Test
	public void testTaxonConceptIdentification() {
	}
}
