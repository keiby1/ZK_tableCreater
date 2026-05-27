package com.example.demo.Services;

import com.example.demo.DTO.DaCsvDocument;
import com.example.demo.DTO.DaCsvParseException;
import com.example.demo.DTO.DaResourceRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Чтение первого листа XLSX в том же виде выгрузки ДА, что и CSV ({@link DaCsvDocument}).
 */
@Service
public class DaXlsxReaderService {

    public DaCsvDocument read(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.xlsx";
        if (name.toLowerCase(Locale.ROOT).endsWith(".xls") && !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new DaCsvParseException(name, 1,
                    "Формат .xls не поддерживается. Откройте файл в Excel и сохраните как «Книга Excel (*.xlsx)».");
        }
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new DaCsvParseException(name, 1, "В книге нет листов");
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new DaCsvParseException(name, 1, "Пустой лист");
            }
            int headerNum = sheet.getFirstRowNum() + 1;
            List<String> headerCells = rowToCellStrings(headerRow, formatter, evaluator, 0);
            Map<Integer, Integer> colIdx = DaExportTableParser.mapHeaderCells(headerCells, name, headerNum);
            int minWidth = DaExportTableParser.maxPhysicalColumnIndex(colIdx) + 1;

            List<DaResourceRow> rows = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            for (int r = sheet.getFirstRowNum() + 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                int lineNum = r + 1;
                List<String> cols = rowToCellStrings(row, formatter, evaluator, minWidth);
                if (DaExportTableParser.isEmptyDataRow(cols, colIdx)) {
                    continue;
                }
                rows.add(DaExportTableParser.parseDataRow(cols, colIdx, name, lineNum));
            }
            return new DaCsvDocument(name, rows);
        } catch (DaCsvParseException e) {
            throw e;
        } catch (org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException e) {
            throw new DaCsvParseException(name, 1,
                    "Файл не является корректной книгой .xlsx. Убедитесь, что выбран формат Excel 2007+.", e);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new DaCsvParseException(name, 1, "Не удалось прочитать XLSX: " + e.getMessage(), e);
        }
    }

    private static List<String> rowToCellStrings(
            Row row,
            DataFormatter formatter,
            FormulaEvaluator evaluator,
            int minColumns) {
        if (row == null) {
            return repeatEmpty(minColumns);
        }
        int last = row.getLastCellNum();
        if (last < 0) {
            last = 0;
        }
        int width = Math.max(minColumns, last);
        List<String> out = new ArrayList<>(Collections.nCopies(width, ""));
        for (int c = 0; c < width; c++) {
            Cell cell = row.getCell(c);
            if (cell == null) {
                continue;
            }
            String s = formatter.formatCellValue(cell, evaluator);
            if (s != null) {
                s = s.trim();
                if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
                    s = s.substring(1).trim();
                }
            } else {
                s = "";
            }
            out.set(c, s);
        }
        return out;
    }

    private static List<String> repeatEmpty(int n) {
        if (n <= 0) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Collections.nCopies(n, ""));
    }
}
