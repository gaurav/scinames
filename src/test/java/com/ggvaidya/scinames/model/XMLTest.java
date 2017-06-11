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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for XML parsing.
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class XMLTest {
	private static final String BEFORE_DATASETS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><project name=\"Test\"><properties/><filters/><datasets>";
	private static final String AFTER_DATASETS = "</datasets></project>";
	
	/**
	 * Why can't the XML import code handle certain entities?
	 */
	@Test
	public void testEntities() throws IOException {
		File tempFile = File.createTempFile("xml_entities_test", ".xml.gz");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(tempFile))));
		
		writer.write(BEFORE_DATASETS +
			"<dataset name=\"test_1\" type=\"Checklist\" year=\"2017\">" +
			"<columns><column name=\"test\" /></columns>" +
			"<rows>" +
            	"<row><key name=\"test\">&#29;</key></row>" +
			"</rows>" +
            "</dataset>" + AFTER_DATASETS);
		writer.close();
		
		Project project;
		IOException thrown = null;
		
		try {
			project = Project.loadFromFile(tempFile);
		} catch(IOException ex) {
			thrown = ex;
		}
		
		assertNotNull(thrown, "Exception IOException not thrown -- is this working?");
		assertNotNull(thrown.getCause(), "Cause unknown for IOException '" + thrown + "'");
		assertTrue(
			thrown.getCause().getMessage().contains("Character reference \"&#"),
			"Unexpected exception error: '" + thrown.getCause().getMessage() + "'"
		);
		
		// TODO: Fix!
	}
}
