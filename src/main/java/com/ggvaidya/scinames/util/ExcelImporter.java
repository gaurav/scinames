package com.ggvaidya.scinames.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;;

/**
 * A helper class to import data from Excel files, either XLS or XLSX.
 * Uses the Apache POI libraries: http://poi.apache.org/spreadsheet/
 * 
 * @author Gaurav Vaidya <gaurav@ggvaidya.com>
 */
public class ExcelImporter {
	private File file;
	public File getFile() { return file; }
	
	private Workbook wb;
	public Workbook getWorkbook() { return wb; }
	
	public List<Sheet> getWorksheets() {
		int nsheets = wb.getNumberOfSheets();
		
		ArrayList<Sheet> sheets = new ArrayList<>(nsheets);
		for(int x = 0; x < nsheets; x++) {
			sheets.add(wb.getSheetAt(x));
		}
		
		return sheets;
	}
	
	public static List<Cell> getCells(Row row) {
		int ncells = row.getPhysicalNumberOfCells();
		
		ArrayList<Cell> cells = new ArrayList<>(ncells);
		for(int x = row.getFirstCellNum(); x <= row.getLastCellNum(); x++) {
			cells.add(row.getCell(x));
		}
		
		return cells;
	}
	
	public static List<String> getCellsAsValues(Row row) {
		return getCells(row).stream()
			.map(cell -> {
				if(cell == null) return "";
				
				// cell.getCellTypeEnum();
				
				String str = cell.toString();
				if(str != null) return str;
				else return "";
			})
			.collect(Collectors.toList());
	}
	
	public ExcelImporter(File f) throws IOException {
		file = f;
		try {
			wb = WorkbookFactory.create(f);
		} catch(EncryptedDocumentException ex) {
			throw new IOException("File '" + f + "' is encrypted and could not be read: " + ex);
		} catch(InvalidFormatException ex) {
			throw new IOException("Invalid format exception opening '" + f + "': " + ex);
		}
	}
}
