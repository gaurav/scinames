package com.ggvaidya.scinames.util;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.ggvaidya.scinames.model.Dataset;
import com.ggvaidya.scinames.model.DatasetColumn;
import com.ggvaidya.scinames.model.DatasetRow;;

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
		DataFormatter formatter = new DataFormatter();
		
		return getCells(row).stream()
			.map(cell -> {
				if(cell == null) return "";
				return formatter.formatCellValue(cell);
			})
			.collect(Collectors.toList());
	}
	
	public Dataset asDataset(int sheetIndex) {
		List<Sheet> sheets = getWorksheets();
		if(sheets.size() < sheetIndex) return new Dataset();
		Dataset ds = ExcelImporter.asDataset(sheets.get(sheetIndex));
		ds.setName(file.getName());
		return ds;
	}
	
	public static Dataset asDataset(Sheet sheet) {
		Dataset ds = new Dataset();
		
		// Sheet range.
		int minRow = sheet.getFirstRowNum();
		int lastRow = sheet.getLastRowNum();
		
		if(lastRow - minRow == 0) return ds;
		
		// Step 1. Load sheet into dataset.
		Row headerRow = sheet.getRow(minRow);
		List<String> headers = getCellsAsValues(headerRow);
		
		// Fill in all headers.
		for(int colIndex = 0; colIndex < headers.size(); colIndex++) {
			String colName = headers.get(colIndex);
			if(colName == null || colName.equals("")) {
				colName = "column_" + (colIndex + 1);
				headers.set(colIndex, colName);
			}
			
			// What if the column name isn't unique?
			String baseName = colName;
			int uniq = 1;
			while(headers.indexOf(colName) < colIndex) {
				colName = baseName + "_" + uniq;
				headers.set(colIndex, colName);
				uniq++;
			}
			
			ds.getColumns().add(DatasetColumn.of(colName));
		}
		
		// Extract all the rows.
		for(int x = (minRow + 1); x <= lastRow; x++) {
			DatasetRow dsRow = new DatasetRow(ds);
			List<String> row = getCellsAsValues(sheet.getRow(x));
			
			for(int y = 0; y < row.size(); y++) {
				dsRow.put(headers.get(y), row.get(y));
			}
			
			ds.rowsProperty().add(dsRow);
		}
		
		return ds;
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
