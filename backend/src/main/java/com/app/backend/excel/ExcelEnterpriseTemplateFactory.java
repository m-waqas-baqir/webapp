package com.app.backend.excel;

import com.app.backend.entity.PlotStatus;
import com.app.backend.entity.RentalPropertyStatus;
import com.app.backend.entity.RentalPropertyType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Pre-formatted Excel templates with a separate {@code Reference Data} sheet and {@code Data Entry} sheet.
 */
public final class ExcelEnterpriseTemplateFactory {

    public static final String DATA_ENTRY = ExcelSheetReader.DATA_ENTRY_SHEET;
    public static final String REFERENCE_DATA = "Reference Data";
    private static final int TEMPLATE_MAX_ROW_0 = 10_000;
    private static final int DATA_START_0 = 2;

    private ExcelEnterpriseTemplateFactory() {
    }

    public static byte[] plotTemplate(
            List<String> phaseNames,
            List<String> khayabanNames,
            List<String> ownerNames,
            List<String> agentNames,
            String exampleKhayabanForFirstPhase
    ) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Font headerFont = wb.createFont();
            headerFont.setBold(true);

            CellStyle instructionStyle = wb.createCellStyle();
            instructionStyle.setWrapText(true);
            instructionStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            Font instFont = wb.createFont();
            instFont.setItalic(true);
            instructionStyle.setFont(instFont);

            CellStyle reqHeader = headerStyle(wb, headerFont, IndexedColorHeader.REQUIRED);
            CellStyle optHeader = headerStyle(wb, headerFont, IndexedColorHeader.OPTIONAL);

            DataFormat df = wb.createDataFormat();
            CellStyle num2 = wb.createCellStyle();
            num2.setDataFormat(df.getFormat("#,##0.00"));
            num2.setVerticalAlignment(VerticalAlignment.CENTER);

            Sheet data = wb.createSheet(DATA_ENTRY);
            int colCount = 8;
            data.addMergedRegion(new CellRangeAddress(0, 0, 0, colCount - 1));
            Row irow = data.createRow(0);
            Cell ins = irow.createCell(0);
            ins.setCellStyle(instructionStyle);
            ins.setCellValue(
                    "Do not change header labels or sheet names. Enter plot records from row 3 onward. "
                            + "The row with plotNumber EXAMPLE is a sample — remove it before uploading. "
                            + "Use dropdowns; values not in lists are rejected on import.");
            irow.setHeightInPoints(42);

            Row h = data.createRow(1);
            String[] headers = {
                    "plotNumber", "size", "price", "status", "phaseName", "khayabanName", "ownerName", "agentName"
            };
            boolean[] required = {true, true, true, true, true, true, false, false};
            for (int c = 0; c < headers.length; c++) {
                Cell hc = h.createCell(c);
                hc.setCellValue(headers[c]);
                hc.setCellStyle(required[c] ? reqHeader : optHeader);
            }

            Row sample = data.createRow(2);
            String exPhase = phaseNames.isEmpty() ? "" : phaseNames.get(0);
            String exK = exampleKhayabanForFirstPhase != null ? exampleKhayabanForFirstPhase : "";
            Object[] ex = {
                    "EXAMPLE",
                    500,
                    1_000_000,
                    PlotStatus.AVAILABLE.name(),
                    exPhase,
                    exK,
                    ownerNames.isEmpty() ? "" : ownerNames.get(0),
                    agentNames.isEmpty() ? "" : agentNames.get(0)
            };
            for (int c = 0; c < ex.length; c++) {
                Cell sc = sample.createCell(c);
                if (c == 1 || c == 2) {
                    if (ex[c] instanceof Number n) {
                        sc.setCellValue(n.doubleValue());
                    }
                    sc.setCellStyle(num2);
                } else {
                    sc.setCellValue(Objects.toString(ex[c], ""));
                }
            }

            data.setColumnWidth(0, 18 * 256);
            data.setColumnWidth(1, 12 * 256);
            data.setColumnWidth(2, 14 * 256);
            for (int c = 1; c <= 2; c++) {
                data.setDefaultColumnStyle(c, num2);
            }

            Sheet ref = wb.createSheet(REFERENCE_DATA);
            int pRows = writeRefGrid(wb, ref, phaseNames, khayabanNames, ownerNames, agentNames);

            XSSFDataValidationHelper dv = new XSSFDataValidationHelper((XSSFSheet) data);
            int lastDataRow0 = TEMPLATE_MAX_ROW_0;
            addListValidation(dv, data, DATA_START_0, lastDataRow0, 3, 3, statusList(PlotStatus.class));
            if (!phaseNames.isEmpty()) {
                String pRange = refRange1Based(ref.getSheetName(), 0, pRows);
                addFormulaListValidation(dv, data, DATA_START_0, lastDataRow0, 4, 4, pRange);
            }
            if (!khayabanNames.isEmpty()) {
                String kRange = refRange1Based(ref.getSheetName(), 1, pRows);
                addFormulaListValidation(dv, data, DATA_START_0, lastDataRow0, 5, 5, kRange);
            }
            String oRange = refRange1Based(ref.getSheetName(), 2, pRows);
            if (!ownerNames.isEmpty()) {
                addFormulaListValidation(dv, data, DATA_START_0, lastDataRow0, 6, 6, oRange);
            }
            String aRange = refRange1Based(ref.getSheetName(), 3, pRows);
            if (!agentNames.isEmpty()) {
                addFormulaListValidation(dv, data, DATA_START_0, lastDataRow0, 7, 7, aRange);
            }

            wb.write(bos);
            return bos.toByteArray();
        }
    }

    public static byte[] khayabanTemplate(List<String> phaseNames) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            CellStyle reqHeader = headerStyle(wb, headerFont, IndexedColorHeader.REQUIRED);
            CellStyle instStyle = wb.createCellStyle();
            instStyle.setWrapText(true);
            instStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            List<String> empty = List.of();
            Sheet data = wb.createSheet(DATA_ENTRY);
            data.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));
            Row r0 = data.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellStyle(instStyle);
            c0.setCellValue("Do not change headers. Remove the EXAMPLE row before upload. Use phaseName from the list.");
            r0.setHeightInPoints(36);

            Row h = data.createRow(1);
            h.createCell(0).setCellValue("name");
            h.getCell(0).setCellStyle(reqHeader);
            h.createCell(1).setCellValue("phaseName");
            h.getCell(1).setCellStyle(reqHeader);

            Row s = data.createRow(2);
            s.createCell(0).setCellValue("EXAMPLE");
            s.createCell(1).setCellValue(phaseNames.isEmpty() ? "" : phaseNames.get(0));

            Sheet ref = wb.createSheet(REFERENCE_DATA);
            int pRows = writeRefGrid(wb, ref, phaseNames, empty, empty, empty);

            if (!phaseNames.isEmpty()) {
                XSSFDataValidationHelper dv = new XSSFDataValidationHelper((XSSFSheet) data);
                String pRange = refRange1Based(ref.getSheetName(), 0, pRows);
                addFormulaListValidation(dv, data, DATA_START_0, TEMPLATE_MAX_ROW_0, 1, 1, pRange);
            }

            for (int c = 0; c < 2; c++) {
                data.autoSizeColumn(c);
            }
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    public static byte[] rentalTemplate(
            List<String> ownerNames,
            List<String> agentNames
    ) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            CellStyle reqHeader = headerStyle(wb, headerFont, IndexedColorHeader.REQUIRED);
            CellStyle optHeader = headerStyle(wb, headerFont, IndexedColorHeader.OPTIONAL);
            CellStyle instStyle = wb.createCellStyle();
            instStyle.setWrapText(true);
            DataFormat df = wb.createDataFormat();
            CellStyle rentStyle = wb.createCellStyle();
            rentStyle.setDataFormat(df.getFormat("#,##0.00"));
            rentStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            List<String> empty = List.of();
            Sheet data = wb.createSheet(DATA_ENTRY);
            data.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
            Row r0 = data.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellStyle(instStyle);
            c0.setCellValue("Do not change headers. Delete the EXAMPLE row before production import. Use dropdowns for type, status, owner, and agent.");
            r0.setHeightInPoints(36);

            String[] headers = {"title", "type", "address", "rentAmount", "status", "ownerName", "agentName"};
            boolean[] req = {true, true, true, true, true, false, false};
            Row h = data.createRow(1);
            for (int c = 0; c < headers.length; c++) {
                Cell hc = h.createCell(c);
                hc.setCellValue(headers[c]);
                hc.setCellStyle(req[c] ? reqHeader : optHeader);
            }
            Row s = data.createRow(2);
            s.createCell(0).setCellValue("EXAMPLE");
            s.createCell(1).setCellValue(RentalPropertyType.RESIDENTIAL.name());
            s.createCell(2).setCellValue("123 Main St");
            Cell rent = s.createCell(3);
            rent.setCellValue(50_000);
            rent.setCellStyle(rentStyle);
            s.createCell(4).setCellValue(RentalPropertyStatus.AVAILABLE.name());
            s.createCell(5).setCellValue(ownerNames.isEmpty() ? "" : ownerNames.get(0));
            s.createCell(6).setCellValue(agentNames.isEmpty() ? "" : agentNames.get(0));

            Sheet ref = wb.createSheet(REFERENCE_DATA);
            int pRows = writeRefGrid(wb, ref, empty, empty, ownerNames, agentNames);

            XSSFDataValidationHelper dv = new XSSFDataValidationHelper((XSSFSheet) data);
            addListValidation(dv, data, DATA_START_0, TEMPLATE_MAX_ROW_0, 1, 1, rentalTypeList());
            addListValidation(dv, data, DATA_START_0, TEMPLATE_MAX_ROW_0, 4, 4, rentalStatusList());
            String oRange = refRange1Based(ref.getSheetName(), 2, pRows);
            if (!ownerNames.isEmpty()) {
                addFormulaListValidation(dv, data, DATA_START_0, TEMPLATE_MAX_ROW_0, 5, 5, oRange);
            }
            String aRange = refRange1Based(ref.getSheetName(), 3, pRows);
            if (!agentNames.isEmpty()) {
                addFormulaListValidation(dv, data, DATA_START_0, TEMPLATE_MAX_ROW_0, 6, 6, aRange);
            }
            for (int c = 0; c < 7; c++) {
                data.autoSizeColumn(c);
            }
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private enum IndexedColorHeader {
        REQUIRED,
        OPTIONAL
    }

    private static CellStyle headerStyle(Workbook wb, Font headerFont, IndexedColorHeader kind) {
        CellStyle s = wb.createCellStyle();
        s.setFont(headerFont);
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setFillForegroundColor(kind == IndexedColorHeader.REQUIRED
                ? org.apache.poi.ss.usermodel.IndexedColors.LIGHT_GREEN.getIndex()
                : org.apache.poi.ss.usermodel.IndexedColors.PALE_BLUE.getIndex());
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    /**
     * @return number of data rows written in each column (same for all columns, max of list sizes)
     */
    private static int writeRefGrid(
            Workbook wb,
            Sheet ref,
            List<String> phases,
            List<String> khayabans,
            List<String> owners,
            List<String> agents) {
        Row t = ref.createRow(0);
        t.createCell(0).setCellValue("Phases (for phaseName / validation)");
        t.createCell(1).setCellValue("Khayabans (valid names; must match selected phase on import)");
        t.createCell(2).setCellValue("Owners (for ownerName)");
        t.createCell(3).setCellValue("Agents (for agentName)");
        int n = Math.max(1, Math.max(phases.size(), Math.max(khayabans.size(), Math.max(owners.size(), agents.size()))));
        for (int i = 0; i < n; i++) {
            Row r = ref.createRow(i + 1);
            r.createCell(0).setCellValue(i < phases.size() ? phases.get(i) : "");
            r.createCell(1).setCellValue(i < khayabans.size() ? khayabans.get(i) : "");
            r.createCell(2).setCellValue(i < owners.size() ? owners.get(i) : "");
            r.createCell(3).setCellValue(i < agents.size() ? agents.get(i) : "");
        }
        for (int c = 0; c < 4; c++) {
            ref.autoSizeColumn(c);
        }
        return n;
    }

    private static String refRange1Based(String sheetName, int col0, int rowCount) {
        String col = CellReference.convertNumToColString(col0);
        int startRow = 2;
        int endRow = 1 + rowCount;
        return "'" + sheetName.replace("'", "''") + "'!" + "$" + col + "$" + startRow + ":$" + col + "$" + endRow;
    }

    private static void addFormulaListValidation(
            XSSFDataValidationHelper dv,
            Sheet sheet,
            int firstRow0,
            int lastRow0,
            int col0,
            int col1,
            String excelFormula) {
        DataValidationConstraint c = dv.createFormulaListConstraint(excelFormula);
        CellRangeAddressList r = new CellRangeAddressList(firstRow0, lastRow0, col0, col1);
        addValidation(sheet, dv, c, r);
    }

    private static void addListValidation(
            XSSFDataValidationHelper dv,
            Sheet sheet,
            int firstRow0,
            int lastRow0,
            int col0,
            int col1,
            String[] values) {
        DataValidationConstraint c = dv.createExplicitListConstraint(values);
        CellRangeAddressList r = new CellRangeAddressList(firstRow0, lastRow0, col0, col1);
        addValidation(sheet, dv, c, r);
    }

    private static void addValidation(
            Sheet sheet,
            XSSFDataValidationHelper dv,
            DataValidationConstraint c,
            CellRangeAddressList r) {
        DataValidation v = dv.createValidation(c, r);
        v.setShowErrorBox(true);
        v.setErrorStyle(DataValidation.ErrorStyle.STOP);
        v.createErrorBox("Invalid value", "Choose a value from the list.");
        v.setSuppressDropDownArrow(false);
        sheet.addValidationData(v);
    }

    private static String[] statusList(Class<? extends Enum<?>> en) {
        Object[] c = en.getEnumConstants();
        String[] o = new String[c.length];
        for (int i = 0; i < c.length; i++) {
            o[i] = ((Enum<?>) c[i]).name();
        }
        return o;
    }

    private static String[] rentalTypeList() {
        return statusList(RentalPropertyType.class);
    }

    private static String[] rentalStatusList() {
        return statusList(RentalPropertyStatus.class);
    }
}
