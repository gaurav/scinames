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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.ggvaidya.scinames.model.filters.ChangeFilterFactory;
import com.ggvaidya.scinames.model.filters.IgnoreErrorChangeTypeFilter;
import com.ggvaidya.scinames.util.SimplifiedDate;

import java.util.function.Predicate;

/**
 * Tests for the TaxonConcept class.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class TaxonConceptTest {
	public static final Logger LOGGER = Logger.getLogger(TaxonConceptTest.class.getSimpleName());
	
	Dataset ds1930 = new Dataset("ds1930", new SimplifiedDate(1930), false);
	Dataset ds1935 = new Dataset("ds1935", new SimplifiedDate(1935), false);
	Dataset ds1940 = new Dataset("ds1940", new SimplifiedDate(1940), false);	
	Dataset ds1950 = new Dataset("ds1950", new SimplifiedDate(1950), false);	
	Dataset ds1960 = new Dataset("ds1960", new SimplifiedDate(1960), false);
	
	private Stream<Change> streamNamesToAdditions(Dataset ds, Name... names) {
		return Arrays.asList(names).stream().map(n -> new Change(
			ds,
			ChangeType.ADDITION,
			Stream.empty(),
			Stream.of(n)
		));
	}
	
	Project builtProject = null;
	private Project getBuiltProject() {
		if(builtProject == null) builtProject = buildProject();
		return builtProject;
	}
	
	// warning: don't call this more than once -- call getBuiltProject() instead!
	private Project buildProject() {
		Project project = new Project();
		project.getChangeFilter().addChangeFilter(new IgnoreErrorChangeTypeFilter(project, true));
			
		ds1930.explicitChangesProperty().addAll(
			streamNamesToAdditions(ds1930,
				Name.get("Branta", "canadensis"),
				Name.get("Branta", "hutchinsii"),
				Name.get("Platypus", "anatinus")
			).collect(Collectors.toList())
		);
		project.addDataset(ds1930);
		
		ds1935.explicitChangesProperty().addAll(
			new Change(ds1935, ChangeType.LUMP,
				Stream.of(Name.get("Branta", "canadensis"), Name.get("Branta", "hutchinsii")),
				Stream.of(Name.get("Branta", "canadensis"), Name.get("Branta", "canadensis", "hutchinsii"))
			)
		);
		project.addDataset(ds1935);
		
		Map<DatasetColumn, String> data = new HashMap<>();
		data.put(DatasetColumn.of("scientificName"), "Branta canadensis hutchinsii");
		data.put(DatasetColumn.of("http://purl.org/dc/terms/references"), "https://www.inaturalist.org/observations/5078823");
		ds1940.rowsProperty().add(new DatasetRow(ds1940, data));
		assertEquals(new HashSet<>(Arrays.asList(Name.get("Branta", "canadensis", "hutchinsii"))), ds1940.getNamesInAllRows());
		project.addDataset(ds1940);
		
		ds1950.explicitChangesProperty().addAll(
			Stream.concat(streamNamesToAdditions(ds1950,
				Name.get("Ornithorhynchus", "paradoxus")
			), Stream.of(
				new Change(ds1950, ChangeType.RENAME, 
					Stream.of(Name.get("Platypus", "anatinus")), 
					Stream.of(Name.get("Ornithorhynchus", "anatinus"))
				),
				new Change(ds1950, ChangeType.DELETION, Stream.of(Name.get("Branta", "canadensis", "hutchinsii")), Stream.empty()),
				new Change(ds1950, ChangeType.ERROR, Stream.of(Name.get("Branta", "canadensis")), Stream.of(
					Name.get("Branta", "canadensis"),
					Name.get("Branta", "hutchinsii")
				))
			)).collect(Collectors.toList())
		);
		project.addDataset(ds1950);
		
		ds1960.explicitChangesProperty().addAll(
			Stream.concat(streamNamesToAdditions(ds1960
			), Stream.of(
				new Change(ds1960, ChangeType.LUMP, 
					Stream.of(Name.get("Ornithorhynchus", "paradoxus"), Name.get("Ornithorhynchus", "anatinus")),
					Stream.of(Name.get("Ornithorhynchus", "anatinus"))
				),
				new Change(ds1960, ChangeType.SPLIT, Stream.of(Name.get("Branta", "canadensis")), Stream.of(
						Name.get("Branta", "canadensis"),
						Name.get("Branta", "hutchinsii")
					))
			)).collect(Collectors.toList())
		);
		project.addDataset(ds1960);
		
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
		Project project = getBuiltProject();
		
		assertEquals(5, project.getDatasets().size());
		
		Dataset last = project.getLastDataset().get();
		assertEquals(last, ds1960);

		LOGGER.fine("ds15: " + ds1935.getAllChanges().map(ch -> ch.toString()).collect(Collectors.joining("\n - ")));		
		assertEquals(1, ds1935.getAllChanges().count());
		assertEquals(setOfNames(
			Name.get("Branta", "canadensis"),
			Name.get("Branta", "canadensis", "hutchinsii"),
			Name.get("Platypus", "anatinus")
		), ds1935.getRecognizedNames(project).collect(Collectors.toSet()));
		
		LOGGER.fine("ds20: " + ds1940.getAllChanges().map(ch -> ch.toString()).collect(Collectors.joining("\n - ")));		
		assertEquals(0, ds1940.getAllChanges().count());
		assertEquals(setOfNames(
			Name.get("Branta", "canadensis"),
			Name.get("Branta", "canadensis", "hutchinsii"),
			Name.get("Platypus", "anatinus")
		), ds1940.getRecognizedNames(project).collect(Collectors.toSet()));
		
		LOGGER.fine("ds30: " + ds1950.getAllChanges().map(ch -> ch.toString()).collect(Collectors.joining("\n - ")));
		assertEquals(4, ds1950.getAllChanges().count());	
		assertEquals(3, ds1950.getChanges(project).count());	
		assertEquals(setOfNames(
			Name.get("Branta", "canadensis"),		
			Name.get("Ornithorhynchus", "anatinus"),
			Name.get("Ornithorhynchus", "paradoxus")
		), ds1950.getRecognizedNames(project).collect(Collectors.toSet()));
		
		LOGGER.fine("ds40: " + ds1960.getAllChanges().map(ch -> ch.toString()).collect(Collectors.joining("\n - ")));
		assertEquals(2, ds1960.getAllChanges().count());
		assertEquals(setOfNames(
			Name.get("Branta", "canadensis"),
			Name.get("Branta", "hutchinsii"),			
			Name.get("Ornithorhynchus", "anatinus")
		), ds1960.getRecognizedNames(project).collect(Collectors.toSet()));
		
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
		Project project = getBuiltProject();
		Dataset lastDataset = project.getLastDataset().get();
		
		assertEquals(
			setOfNames(
				Name.get("Branta", "canadensis"),
				Name.get("Branta", "hutchinsii"),
				Name.get("Ornithorhynchus", "anatinus")
			),
			lastDataset.getRecognizedNames(project).collect(Collectors.toSet())
		);
		
		ComprehensiveSetTest.test(
			"Taxon concepts for 'Branta canadensis'",
			project.getNameClusterManager().getSpeciesClusters()
				.filter(cl -> cl.contains(Name.get("Branta", "canadensis")))
				.flatMap(cl -> cl.getTaxonConcepts(project).stream())
				.collect(Collectors.toSet()),
			Arrays.asList(
				(Predicate<TaxonConcept>) tc -> 
					ComprehensiveSetTest.containsOnly(tc.getNames(), 
						Arrays.asList(
							Name.get("Branta", "canadensis"),
							Name.get("Branta", "canadensis", "hutchinsii")
						)
					) && ComprehensiveSetTest.containsOnly(tc.getFoundIn(), 
						Arrays.asList(
							ds1930, ds1935
						)							
					),
				(Predicate<TaxonConcept>) tc -> 
					ComprehensiveSetTest.containsOnly(tc.getNames(), 
						Arrays.asList(
							Name.get("Branta", "canadensis")
						)
					) && ComprehensiveSetTest.containsOnly(tc.getFoundIn(), 
						Arrays.asList(
							ds1960
						)							
					),
				(Predicate<TaxonConcept>) tc -> 
					ComprehensiveSetTest.containsOnly(tc.getNames(), 
						Arrays.asList(
							Name.get("Branta", "canadensis"),
							Name.get("Branta", "canadensis", "hutchinsii")
						)
					) && ComprehensiveSetTest.containsOnly(tc.getFoundIn(), 
						Arrays.asList(
							ds1935, ds1940, ds1950, ds1960
						)							
					)
			)
		);
		
		/*
		assertEquals(
			new HashSet<>(Arrays.asList(
				// TODO: how do we test this?
			)),
			project.getNameClusterManager().getClusters(lastDataset.getRecognizedNames(project).collect(Collectors.toList()))
		);*/	
	}
}
