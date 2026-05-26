package com.example.demo.Services;

import com.example.demo.DTO.DaCsvDocument;
import com.example.demo.DTO.DaCsvParseException;
import com.example.demo.DTO.DaResourceRow;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Чтение CSV выгрузки ДА ({@link DaCsvDocument}) с нормализацией CPU (ядра) и RAM (байты).
 */
@Service
public class DaCsvReaderService {

    private static final Pattern MEMORY_PATTERN =
            Pattern.compile("^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(KiB|MiB|GiB|TiB|KB|MB|GB|TB|K|M|G|T|B)?\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /** Ожидаемые имена столбцов (без учёта регистра после trim). */
    private static final List<String> EXPECTED_HEADERS = List.of(
            "namespace",
            "pod",
            "container",
            "cpu requests",
            "cpu limits",
            "cpu used",
            "ram requests",
            "ram limits",
            "ram used"
    );

    public DaCsvDocument read(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            return read(reader, name);
        }
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
        Map<Integer, Integer> colIndexByExpected = parseHeader(first, sourceName, lineNum);
        List<DaResourceRow> rows = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            lineNum++;
            if (line.isBlank()) {
                continue;
            }
            List<String> cols = splitCsvLine(line);
            if (cols.size() < EXPECTED_HEADERS.size()) {
                throw new DaCsvParseException(sourceName, lineNum,
                        "Ожидалось не менее " + EXPECTED_HEADERS.size() + " столбцов, получено " + cols.size());
            }
            rows.add(parseDataRow(cols, colIndexByExpected, sourceName, lineNum));
        }

        return new DaCsvDocument(sourceName, rows);
    }

    private static Map<Integer, Integer> parseHeader(String headerLine, String sourceName, int lineNum) {
        List<String> headers = splitCsvLine(headerLine);
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            String wanted = EXPECTED_HEADERS.get(i);
            int idx = indexOfIgnoreCase(headers, wanted);
            if (idx < 0) {
                throw new DaCsvParseException(sourceName, lineNum,
                        "Столбец \"" + capitalizeHeader(wanted) + "\" не найден в заголовке");
            }
            map.put(i, idx);
        }
        return map;
    }

    private static DaResourceRow parseDataRow(
            List<String> cols,
            Map<Integer, Integer> colIdx,
            String sourceName,
            int lineNum) {
        String ns = requireText(getCol(cols, colIdx, 0), sourceName, lineNum, "Namespace");
        String pod = requireText(getCol(cols, colIdx, 1), sourceName, lineNum, "Pod");
        String container = requireText(getCol(cols, colIdx, 2), sourceName, lineNum, "Container");

        BigDecimal cpuRq = parseCpuCores(getCol(cols, colIdx, 3), sourceName, lineNum, "CPU Requests");
        BigDecimal cpuLim = parseCpuCores(getCol(cols, colIdx, 4), sourceName, lineNum, "CPU Limits");
        BigDecimal cpuUsed = parseCpuCores(getCol(cols, colIdx, 5), sourceName, lineNum, "CPU Used");

        long ramRq = parseMemory(getCol(cols, colIdx, 6), sourceName, lineNum, "RAM Requests");
        long ramLim = parseMemory(getCol(cols, colIdx, 7), sourceName, lineNum, "RAM Limits");
        long ramUsed = parseMemory(getCol(cols, colIdx, 8), sourceName, lineNum, "RAM Used");

        return new DaResourceRow(
                ns,
                pod,
                container,
                cpuRq,
                cpuLim,
                cpuUsed,
                ramRq,
                ramLim,
                ramUsed,
                lineNum);
    }

    private static String getCol(List<String> cols, Map<Integer, Integer> colIdx, int logicalIndex) {
        Integer ix = colIdx.get(logicalIndex);
        return ix != null && ix < cols.size() ? cols.get(ix) : "";
    }

    private static String requireText(String value, String sourceName, int lineNum, String field) {
        if (value == null || value.isBlank()) {
            throw new DaCsvParseException(sourceName, lineNum, "Пустое значение поля \"" + field + "\"");
        }
        return value.trim();
    }

    private static BigDecimal parseCpuCores(String raw, String sourceName, int lineNum, String field) {
        if (raw == null || raw.isBlank()) {
            throw new DaCsvParseException(sourceName, lineNum, "Пустое значение поля \"" + field + "\"");
        }
        String s = raw.trim().replace(',', '.');
        try {
            BigDecimal bd = new BigDecimal(s);
            return bd.stripTrailingZeros();
        } catch (NumberFormatException e) {
            throw new DaCsvParseException(sourceName, lineNum,
                    "Поле \"" + field + "\": не удалось распознать число \"" + raw + "\"", e);
        }
    }

    private static long parseMemory(String raw, String sourceName, int lineNum, String field) {
        if (raw == null || raw.isBlank()) {
            throw new DaCsvParseException(sourceName, lineNum, "Пустое значение поля \"" + field + "\"");
        }
        Matcher m = MEMORY_PATTERN.matcher(raw.trim());
        if (!m.matches()) {
            throw new DaCsvParseException(sourceName, lineNum,
                    "Поле \"" + field + "\": неверный формат памяти \"" + raw + "\"");
        }
        BigDecimal amount = new BigDecimal(m.group(1));
        String unit = m.group(2);
        BigDecimal multiplier = memoryUnitMultiplier(unit);
        BigDecimal bytes = amount.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
        try {
            return bytes.longValueExact();
        } catch (ArithmeticException ex) {
            throw new DaCsvParseException(sourceName, lineNum,
                    "Поле \"" + field + "\": значение вне допустимого диапазона для байт", ex);
        }
    }

    /**
     * Множитель «единица → байт». Для суффиксов без «i» (KB, MB, …) считаются те же множители ×1024, как у KiB/MiB,
     * т.к. в выгрузках ДА обычно используются двоичные единицы.
     */
    private static BigDecimal memoryUnitMultiplier(String unit) {
        if (unit == null || unit.isEmpty()) {
            return BigDecimal.ONE;
        }
        switch (unit.toUpperCase(Locale.ROOT)) {
            case "B":
                return BigDecimal.ONE;
            case "K":
            case "KB":
            case "KIB":
                return BigDecimal.valueOf(1024L);
            case "M":
            case "MB":
            case "MIB":
                return BigDecimal.valueOf(1024L).pow(2);
            case "G":
            case "GB":
            case "GIB":
                return BigDecimal.valueOf(1024L).pow(3);
            case "T":
            case "TB":
            case "TIB":
                return BigDecimal.valueOf(1024L).pow(4);
            default:
                return BigDecimal.ONE;
        }
    }

    private static List<String> splitCsvLine(String line) {
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
            String inner = s.substring(1, s.length() - 1).replace("\"\"", "\"");
            return inner;
        }
        return s;
    }

    private static String stripBom(String line) {
        if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }

    private static int indexOfIgnoreCase(List<String> headers, String expectedName) {
        for (int i = 0; i < headers.size(); i++) {
            if (normalizeHeader(headers.get(i)).equalsIgnoreCase(expectedName)) {
                return i;
            }
        }
        return -1;
    }

    private static String normalizeHeader(String h) {
        return h == null ? "" : h.trim().toLowerCase(Locale.ROOT);
    }

    private static String capitalizeHeader(String lowerUnderscore) {
        String[] tokens = lowerUnderscore.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            String t = tokens[i];
            if (!t.isEmpty()) {
                sb.append(Character.toUpperCase(t.charAt(0))).append(t.substring(1));
            }
        }
        return sb.toString();
    }
}
