package com.example.demo.Services;

import com.example.demo.DTO.DaCsvParseException;
import com.example.demo.DTO.DaResourceRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Общее сопоставление заголовков и разбор строки выгрузки ДА для CSV и XLSX.
 */
public final class DaExportTableParser {

    static final Pattern MEMORY_PATTERN =
            Pattern.compile("^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(KiB|MiB|GiB|TiB|KB|MB|GB|TB|K|M|G|T|B)?\\s*$",
                    Pattern.CASE_INSENSITIVE);

    static final List<String> EXPECTED_HEADERS = List.of(
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

    private DaExportTableParser() {
    }

    static Map<Integer, Integer> mapHeaderCells(List<String> headerCells, String sourceName, int lineNum) {
        Map<Integer, Integer> map = new HashMap<>();
        Set<Integer> usedIndices = new HashSet<>();
        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            String wanted = EXPECTED_HEADERS.get(i);
            int idx = indexOfIgnoreCase(headerCells, wanted);
            if (idx < 0) {
                throw new DaCsvParseException(sourceName, lineNum,
                        "Столбец \"" + capitalizeHeader(wanted) + "\" не найден в заголовке");
            }
            if (!usedIndices.add(idx)) {
                throw new DaCsvParseException(sourceName, lineNum,
                        "Столбец \"" + capitalizeHeader(wanted) + "\" сопоставлен с уже используемым столбцом файла");
            }
            map.put(i, idx);
        }
        return map;
    }

    static int maxPhysicalColumnIndex(Map<Integer, Integer> colIdx) {
        return colIdx.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    static boolean isEmptyDataRow(List<String> cols, Map<Integer, Integer> colIdx) {
        boolean anyNonEmpty = false;
        for (String cell : cols) {
            if (cell != null && !cell.isBlank() && !isMissingToken(cell)) {
                anyNonEmpty = true;
                break;
            }
        }
        if (!anyNonEmpty) {
            return true;
        }
        String ns = getCol(cols, colIdx, 0);
        String pod = getCol(cols, colIdx, 1);
        String container = getCol(cols, colIdx, 2);
        return (ns == null || ns.isBlank())
                && (pod == null || pod.isBlank())
                && (container == null || container.isBlank());
    }

    static DaResourceRow parseDataRow(
            List<String> cols,
            Map<Integer, Integer> colIdx,
            String sourceName,
            int lineNum) {
        String ns = requireText(getCol(cols, colIdx, 0), sourceName, lineNum, "Namespace");
        String pod = requireText(getCol(cols, colIdx, 1), sourceName, lineNum, "Pod");
        String container = requireText(getCol(cols, colIdx, 2), sourceName, lineNum, "Container");
        String deployment = DaPodNameParser.extractDeployment(pod);

        String cpuRqRaw = getCol(cols, colIdx, 3);
        String cpuLimRaw = getCol(cols, colIdx, 4);
        String cpuUsedRaw = getCol(cols, colIdx, 5);
        String ramRqRaw = getCol(cols, colIdx, 6);
        String ramLimRaw = getCol(cols, colIdx, 7);
        String ramUsedRaw = getCol(cols, colIdx, 8);

        DaResourceRow row = new DaResourceRow();
        row.setNamespace(ns);
        row.setPod(pod);
        row.setDeployment(deployment);
        row.setContainerName(container);
        row.setSourceLineNumber(lineNum);
        row.setCpuRequestDisplay(displayOrEmpty(cpuRqRaw));
        row.setCpuLimitDisplay(displayOrEmpty(cpuLimRaw));
        row.setCpuUsedDisplay(displayOrEmpty(cpuUsedRaw));
        row.setRamRequestDisplay(displayOrEmpty(ramRqRaw));
        row.setRamLimitDisplay(displayOrEmpty(ramLimRaw));
        row.setRamUsedDisplay(displayOrEmpty(ramUsedRaw));
        row.setCpuRequestCores(parseCpuCoresOptional(cpuRqRaw, sourceName, lineNum, "CPU Requests"));
        row.setCpuLimitCores(parseCpuCoresOptional(cpuLimRaw, sourceName, lineNum, "CPU Limits"));
        row.setCpuUsedCores(parseCpuCoresOptional(cpuUsedRaw, sourceName, lineNum, "CPU Used"));
        row.setRamRequestBytes(parseMemoryOptional(ramRqRaw, sourceName, lineNum, "RAM Requests"));
        row.setRamLimitBytes(parseMemoryOptional(ramLimRaw, sourceName, lineNum, "RAM Limits"));
        row.setRamUsedBytes(parseMemoryOptional(ramUsedRaw, sourceName, lineNum, "RAM Used"));
        return row;
    }

    private static String displayOrEmpty(String raw) {
        if (raw == null || raw.isBlank() || isMissingToken(raw)) {
            return "";
        }
        return raw.trim();
    }

    private static boolean isMissingToken(String raw) {
        String s = raw.trim();
        return s.isEmpty() || "-".equals(s);
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

    private static BigDecimal parseCpuCoresOptional(String raw, String sourceName, int lineNum, String field) {
        if (raw == null || raw.isBlank() || isMissingToken(raw)) {
            return null;
        }
        String s = raw.trim().replace(',', '.');
        try {
            return new BigDecimal(s).stripTrailingZeros();
        } catch (NumberFormatException e) {
            throw new DaCsvParseException(sourceName, lineNum,
                    "Поле \"" + field + "\": не удалось распознать число \"" + raw + "\"", e);
        }
    }

    private static Long parseMemoryOptional(String raw, String sourceName, int lineNum, String field) {
        if (raw == null || raw.isBlank() || isMissingToken(raw)) {
            return null;
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

    private static int indexOfIgnoreCase(List<String> headers, String expectedName) {
        for (int i = 0; i < headers.size(); i++) {
            if (normalizeHeader(headers.get(i)).equalsIgnoreCase(expectedName)) {
                return i;
            }
        }
        return -1;
    }

    private static String normalizeHeader(String h) {
        if (h == null) {
            return "";
        }
        String s = h.trim();
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            s = s.substring(1).trim();
        }
        return s.toLowerCase(Locale.ROOT);
    }

    static String capitalizeHeader(String lowerUnderscore) {
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
