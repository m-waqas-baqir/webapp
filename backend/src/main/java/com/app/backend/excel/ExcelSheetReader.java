package com.app.backend.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads the first (or "Data Entry") sheet of an .xlsx.
 * Auto-discovers the header row (first row containing all expected column headers).
 * Data rows follow the header row; empty rows are skipped.
 * Header keys are normalized (trim, lower case, spaces removed) for lookup.
 * {@code excelRowNumber} in {@link ParsedRow} is 1-based, including the header row.
 */
public final class ExcelSheetReader {

    private static final DataFormatter FORMATTER = new DataFormatter();
    public static final String DATA_ENTRY_SHEET = "Data Entry";
    private static final int MAX_HEADER_SCAN_ROWS = 50;

    private ExcelSheetReader() {
    }

    public static List<ParsedRow> readDataRows(InputStream in, Set<String> expectedNormalizedHeaders) throws IOException {
        try (Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = resolveDataSheet(wb);
            return readDataRows(sheet, expectedNormalizedHeaders);
        }
    }

    public static List<ParsedRow> readDataRows(Sheet sheet, Set<String> expectedNormalizedHeaders) {
        int headerRowIndex = findHeaderRowIndex(sheet, expectedNormalizedHeaders);
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            throw new ExcelParseException("Missing header row");
        }
        Map<Integer, String> colToNormalizedHeader = new LinkedHashMap<>();
        short lastCell = headerRow.getLastCellNum();
        for (int c = 0; c < lastCell; c++) {
            String raw = getCellString(headerRow.getCell(c));
            if (raw.isBlank()) {
                continue;
            }
            String norm = normalizeHeader(raw);
            colToNormalizedHeader.put(c, norm);
        }
        for (String expected : expectedNormalizedHeaders) {
            if (!colToNormalizedHeader.containsValue(expected)) {
                throw new ExcelParseException("Missing required column: " + expected);
            }
        }

        List<ParsedRow> out = new ArrayList<>();
        int lastRow = sheet.getLastRowNum();
        for (int r = headerRowIndex + 1; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null || rowIsEmpty(row, colToNormalizedHeader.keySet())) {
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            for (var e : colToNormalizedHeader.entrySet()) {
                String v = getCellString(row.getCell(e.getKey()));
                values.put(e.getValue(), v);
            }
            out.add(new ParsedRow(r + 1, values));
        }
        return out;
    }

    public static int findHeaderRowIndex(Sheet sheet, Set<String> expectedNormalizedHeaders) {
        int last = Math.min(sheet.getLastRowNum(), MAX_HEADER_SCAN_ROWS);
        for (int r = 0; r <= last; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            Map<Integer, String> colToNorm = new LinkedHashMap<>();
            short lastCell = row.getLastCellNum();
            for (int c = 0; c < lastCell; c++) {
                String raw = getCellString(row.getCell(c));
                if (raw.isBlank()) {
                    continue;
                }
                colToNorm.put(c, normalizeHeader(raw));
            }
            Collection<String> found = colToNorm.values();
            boolean allPresent = true;
            for (String expected : expectedNormalizedHeaders) {
                if (!found.contains(expected)) {
                    allPresent = false;
                    break;
                }
            }
            if (allPresent) {
                return r;
            }
        }
        throw new ExcelParseException("Missing required columns (could not find a header row containing: "
                + String.join(", ", expectedNormalizedHeaders) + ")");
    }

    private static Sheet resolveDataSheet(Workbook wb) {
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            if (DATA_ENTRY_SHEET.equalsIgnoreCase(wb.getSheetName(i))) {
                return wb.getSheetAt(i);
            }
        }
        return wb.getSheetAt(0);
    }

    private static boolean rowIsEmpty(Row row, Collection<Integer> cols) {
        for (int c : cols) {
            String s = getCellString(row.getCell(c));
            if (!s.isBlank()) {
                return false;
            }
        }
        return true;
    }

    public static String normalizeHeader(String h) {
        return h.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    public static String getCellString(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        if (cell.getCellType() == CellType.FORMULA) {
            return FORMATTER.formatCellValue(cell).trim();
        }
        return FORMATTER.formatCellValue(cell).trim();
    }

    public record ParsedRow(int excelRowNumber, Map<String, String> valuesByNormalizedHeader) {}
}
