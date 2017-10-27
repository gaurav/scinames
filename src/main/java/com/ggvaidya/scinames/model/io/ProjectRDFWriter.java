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
package com.ggvaidya.scinames.model.io;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

import com.ggvaidya.scinames.model.Change;
import com.ggvaidya.scinames.model.ChangeType;
import com.ggvaidya.scinames.model.Checklist;
import com.ggvaidya.scinames.model.Citation;
import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;
import com.ggvaidya.scinames.model.Name;
import com.ggvaidya.scinames.model.Project;
import com.ggvaidya.scinames.model.Tag;
import com.ggvaidya.scinames.model.filters.ChangeFilterFactory;
import com.ggvaidya.scinames.model.rowextractors.NameExtractorParseException;
import com.ggvaidya.scinames.util.SimplifiedDate;

/**
 * Write out an entire project as RDF. We use TaxMeOn (http://schema.onki.fi/taxmeon/) 
 * as our key vocabulary.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class ProjectRDFWriter {
	private static final Logger LOGGER = Logger.getLogger(ProjectRDFWriter.class.getSimpleName());
	
	private IRI baseIRI;
	public IRI getBaseIRI() { return baseIRI; }
	
	private Project project;
	public Project getProject() { return project; }
	
	public ProjectRDFWriter(Project proj) {
		
	}
	
	public ProjectRDFWriter(Project proj, IRI baseIRI) {
		this.baseIRI = baseIRI; 
	}
	
	private Map<Dataset, IRI> datasetIRIs = new HashMap<>();
	private void generateIRIsForChecklists() {
		datasetIRIs.clear();
		
		int checklist_index = 0;
		for(Dataset ch: project.getDatasets()) {
			checklist_index++;
			
			// Option 1. Try to convert the name into a fragment.
			String nameAsFragment = 
		}
		
		return map;
	}
	
	public void addChecklistsToOntology(OWLOntology ontology, Project project) {
		
	}
	
	public void addRecognizedNamesToOntology(OWLOntology ontology, Project project) {
		
	}
	
	public void addChangesToOntology(OWLOntology ontology, Project project) {
		
	}
}
