package com.example.demo.Services;

import com.example.demo.DTO.DaCsvDocument;
import com.example.demo.DTO.DaCsvParseException;
import com.example.demo.DTO.DaResourceRow;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Чтение CSV выгрузки ДА ({@link DaCsvDocument}) с нормализацией CPU (ядра) и RAM (байты).
 */
@Service
public class DaCsvReaderService {

    public DaCsvDocument read(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        try (BufferedReader reader = openReader(file.getInputStream())) {
            return read(reader, name);
        }
    }

    private static BufferedReader openReader(InputStream inputStream) throws IOException {
        InputStream in = inputStream.markSupported() ? inputStream : new BufferedInputStream(inputStream);
        Charset charset = detectCharset(in);
        return new BufferedReader(new InputStreamReader(in, charset));
    }

    private static Charset detectCharset(InputStream in) throws IOException {
        in.mark(4);
        int b0 = in.read();
        int b1 = in.read();
        int b2 = in.read();
        in.reset();
        if (b0 == 0xFF && b1 == 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        if (b0 == 0xFE && b1 == 0xFF) {
            return StandardCharsets.UTF_16BE;
        }
        if (b0 == 0xEF && b1 == 0xBB && b2 == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        return StandardCharsets.UTF_8;
    }

    public DaCsvDocument read(BufferedReader reader, String sourceName) throws IOException {
        String first = reader.readLine();
        int lineNum = 1;
        if (first == null) {
            throw new DaCsvParseException(sourceName, lineNum, "Пустой файл");
        }
        first = stripBom(first.trim());
        if (first.isEmpty()) {
            throw new DaCsvParseException(sourceName, lineNum, "Первая строка файла пуста");
        }
        char delimiter = detectDelimiter(first, sourceName, lineNum);
        Map<Integer, Integer> colIndexByExpected = DaExportTableParser.mapHeaderCells(
                splitDelimitedLine(first, delimiter), sourceName, lineNum);
        List<DaResourceRow> rows = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            lineNum++;
            if (line.isBlank()) {
                continue;
            }
            List<String> cols = splitDelimitedLine(line, delimiter);
            if (DaExportTableParser.isEmptyDataRow(cols, colIndexByExpected)) {
                continue;
            }
            int minCols = DaExportTableParser.maxPhysicalColumnIndex(colIndexByExpected) + 1;
            if (cols.size() < minCols) {
                throw new DaCsvParseException(sourceName, lineNum,
                        "Ожидалось не менее " + minCols + " столбцов, получено " + cols.size());
            }
            rows.add(DaExportTableParser.parseDataRow(cols, colIndexByExpected, sourceName, lineNum));
        }

        return new DaCsvDocument(sourceName, rows);
    }

    /**
     * Разделитель: точка с запятой (Excel RU), табуляция или запятая (как в DA_File*.csv).
     */
    private static char detectDelimiter(String headerLine, String sourceName, int lineNum) {
        char[] candidates = {';', '\t', ','};
        for (char delimiter : candidates) {
            List<String> headers = splitDelimitedLine(headerLine, delimiter);
            if (headerContainsAllExpected(headers)) {
                return delimiter;
            }
        }
        throw new DaCsvParseException(sourceName, lineNum,
                "Не удалось определить разделитель CSV (ожидаются «;», табуляция или «,») "
                        + "или не найдены обязательные столбцы");
    }

    private static boolean headerContainsAllExpected(List<String> headers) {
        for (String wanted : DaExportTableParser.EXPECTED_HEADERS) {
            int idx = -1;
            for (int i = 0; i < headers.size(); i++) {
                String hn = normalizeHeaderForMatch(headers.get(i));
                if (hn.equalsIgnoreCase(wanted)) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeHeaderForMatch(String h) {
        if (h == null) {
            return "";
        }
        String s = h.trim();
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            s = s.substring(1).trim();
        }
        return s.toLowerCase(Locale.ROOT);
    }

    private static List<String> splitDelimitedLine(String line, char delimiter) {
        if (delimiter == ',') {
            return splitCsvLineComma(line);
        }
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == delimiter) {
                parts.add(unquoteField(cur.toString().trim()));
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(unquoteField(cur.toString().trim()));
        return parts;
    }

    private static List<String> splitCsvLineComma(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                parts.add(unquoteField(cur.toString().trim()));
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(unquoteField(cur.toString().trim()));
        return parts;
    }

    private static String unquoteField(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1).replace("\"\"", "\"");
        }
        return s;
    }

    private static String stripBom(String line) {
        if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }
}
