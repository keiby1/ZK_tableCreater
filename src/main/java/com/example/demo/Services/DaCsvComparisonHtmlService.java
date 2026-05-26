package com.example.demo.Services;

import com.example.demo.DTO.DaCompareRow;
import com.example.demo.DTO.DaResourceRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

/**
 * HTML-таблица сравнения двух CSV ДА: ключевые столбцы слева, метрики файла 1, метрики файла 2.
 */
@Service
public class DaCsvComparisonHtmlService {

    @Autowired
    private AppLayoutService appLayoutService;

    public String generateComparisonPage(
            List<DaCompareRow> rows,
            String fileName1,
            String fileName2) {
        String name1 = blankToDefault(fileName1, "Файл 1");
        String name2 = blankToDefault(fileName2, "Файл 2");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"ru\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>Сравнение CSV (DA)</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }\n");
        html.append("    .compare-header { margin-bottom: 16px; padding: 12px; background: #e8f5e9; border: 1px solid #c8e6c9; border-radius: 6px; }\n");
        html.append("    .compare-header h2 { margin: 0 0 8px 0; font-size: 1.1em; color: #2e7d32; }\n");
        html.append("    .compare-header p { margin: 4px 0; font-size: 0.95em; color: #333; }\n");
        html.append("    .legend { margin-bottom: 16px; padding: 12px 16px; background: #fafafa; border: 1px solid #e0e0e0; border-radius: 6px; font-size: 0.9em; }\n");
        html.append("    .legend details summary { cursor: pointer; font-weight: bold; color: #424242; list-style: none; }\n");
        html.append("    .legend details summary::-webkit-details-marker { display: none; }\n");
        html.append("    .legend details summary::before { content: '▶ '; font-size: 0.75em; }\n");
        html.append("    .legend details[open] summary::before { content: '▼ '; }\n");
        html.append("    .legend ul { margin: 6px 0 0 0; padding-left: 20px; }\n");
        html.append("    .legend li { margin: 2px 0; }\n");
        html.append("    .legend .swatch { display: inline-block; width: 14px; height: 14px; margin-right: 6px; vertical-align: middle; border: 1px solid #bdbdbd; border-radius: 2px; }\n");
        html.append("    .table-wrap { overflow-x: auto; }\n");
        html.append("    table { border-collapse: collapse; width: 100%; background: #fff; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("    th, td { border: 1px solid #ddd; padding: 8px 10px; text-align: center; font-size: 0.92em; }\n");
        html.append("    thead th { background: #4CAF50; color: #fff; font-weight: bold; }\n");
        html.append("    thead th.key-col { background: #388E3C; color: #fff; text-align: left; }\n");
        html.append("    thead th.group-left { background: #388E3C; }\n");
        html.append("    thead th.group-right { background: #2E7D32; border-left: 3px solid #1B5E20; }\n");
        html.append("    td.key-col { text-align: left; background: #fafafa; }\n");
        html.append("    td.group-right { border-left: 3px solid #c8e6c9; }\n");
        html.append("    td.cmp-match { background: #c8e6c9; }\n");
        html.append("    td.cmp-below-10 { background: #fff9c4; }\n");
        html.append("    td.cmp-below-20 { background: #ffe0b2; }\n");
        html.append("    td.cmp-above-20 { background: #ef9a9a; }\n");
        html.append("    td.cmp-missing { background: #cfd8dc; color: #546e7a; }\n");
        html.append("    tr:hover td.key-col { background: #ececec; }\n");
        html.append("    tr:hover td.cmp-match { background: #a5d6a7; }\n");
        html.append("    tr:hover td.cmp-below-10 { background: #fff59d; }\n");
        html.append("    tr:hover td.cmp-below-20 { background: #ffcc80; }\n");
        html.append("    tr:hover td.cmp-above-20 { background: #e57373; }\n");
        html.append("    tr:hover td.cmp-missing { background: #b0bec5; }\n");
        html.append("    .back-link { margin-top: 16px; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append(appLayoutService.buildAppHeader());
        html.append("  <div class=\"compare-header\">\n");
        html.append("    <h2>Сравнение: ").append(escapeHtml(name1)).append(" и ").append(escapeHtml(name2)).append("</h2>\n");
        html.append("    <p>Слева — метрики из <strong>").append(escapeHtml(name1)).append("</strong>, ");
        html.append("справа — из <strong>").append(escapeHtml(name2)).append("</strong>. ");
        html.append("Сопоставление по namespace, deployment и container.</p>\n");
        html.append("  </div>\n");
        html.append("  <div class=\"legend\">\n");
        html.append("    <details>\n");
        html.append("      <summary>Расшифровка индикаторов сравнения</summary>\n");
        html.append("      <ul>\n");
        html.append("        <li><span class=\"swatch\" style=\"background:#c8e6c9\"></span> Полное соответствие значений</li>\n");
        html.append("        <li><span class=\"swatch\" style=\"background:#fff9c4\"></span> Расхождение менее 10%</li>\n");
        html.append("        <li><span class=\"swatch\" style=\"background:#ffe0b2\"></span> Расхождение от 10% до 20%</li>\n");
        html.append("        <li><span class=\"swatch\" style=\"background:#ef9a9a\"></span> Расхождение 20% и более</li>\n");
        html.append("        <li><span class=\"swatch\" style=\"background:#cfd8dc\"></span> Одно из значений отсутствует (пусто или «-»)</li>\n");
        html.append("      </ul>\n");
        html.append("    </details>\n");
        html.append("  </div>\n");
        html.append("  <div class=\"table-wrap\">\n");
        html.append("    <table>\n");
        html.append("      <thead>\n");
        html.append("        <tr>\n");
        html.append("          <th colspan=\"3\" class=\"key-col\">Ключ</th>\n");
        html.append("          <th colspan=\"6\" class=\"group-left\">").append(escapeHtml(name1)).append("</th>\n");
        html.append("          <th colspan=\"6\" class=\"group-right\">").append(escapeHtml(name2)).append("</th>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("          <th class=\"key-col\">Namespace</th>\n");
        html.append("          <th class=\"key-col\">Deployment</th>\n");
        html.append("          <th class=\"key-col\">Container</th>\n");
        html.append("          <th class=\"group-left\">CPU Requests</th>\n");
        html.append("          <th class=\"group-left\">CPU Limits</th>\n");
        html.append("          <th class=\"group-left\">CPU Used</th>\n");
        html.append("          <th class=\"group-left\">RAM Requests</th>\n");
        html.append("          <th class=\"group-left\">RAM Limits</th>\n");
        html.append("          <th class=\"group-left\">RAM Used</th>\n");
        html.append("          <th class=\"group-right\">CPU Requests</th>\n");
        html.append("          <th class=\"group-right\">CPU Limits</th>\n");
        html.append("          <th class=\"group-right\">CPU Used</th>\n");
        html.append("          <th class=\"group-right\">RAM Requests</th>\n");
        html.append("          <th class=\"group-right\">RAM Limits</th>\n");
        html.append("          <th class=\"group-right\">RAM Used</th>\n");
        html.append("        </tr>\n");
        html.append("      </thead>\n");
        html.append("      <tbody>\n");

        for (DaCompareRow row : rows) {
            DaResourceRow left = row.getLeft();
            DaResourceRow right = row.getRight();
            html.append("        <tr>\n");
            html.append("          <td class=\"key-col\">").append(escapeHtml(row.getNamespace())).append("</td>\n");
            html.append("          <td class=\"key-col\">").append(escapeHtml(row.getDeployment())).append("</td>\n");
            html.append("          <td class=\"key-col\">").append(escapeHtml(row.getContainerName())).append("</td>\n");
            appendComparedMetricsRow(html, left, right);
            html.append("        </tr>\n");
        }

        html.append("      </tbody>\n");
        html.append("    </table>\n");
        html.append("  </div>\n");
        html.append("  <p class=\"back-link\"><a href=\"/compareDa\">← Вернуться к выбору файлов</a></p>\n");
        html.append("</body>\n");
        html.append("</html>");
        return html.toString();
    }

    /**
     * Порядок ячеек как в шапке: сначала все 6 метрик файла 1, затем все 6 метрик файла 2.
     */
    private static void appendComparedMetricsRow(StringBuilder html, DaResourceRow left, DaResourceRow right) {
        MetricColumn[] columns = {
                new MetricColumn(DaResourceRow::getCpuRequestDisplay, DaResourceRow::getCpuRequestCores, true),
                new MetricColumn(DaResourceRow::getCpuLimitDisplay, DaResourceRow::getCpuLimitCores, true),
                new MetricColumn(DaResourceRow::getCpuUsedDisplay, DaResourceRow::getCpuUsedCores, true),
                new MetricColumn(DaResourceRow::getRamRequestDisplay, DaResourceRow::getRamRequestBytes, false),
                new MetricColumn(DaResourceRow::getRamLimitDisplay, DaResourceRow::getRamLimitBytes, false),
                new MetricColumn(DaResourceRow::getRamUsedDisplay, DaResourceRow::getRamUsedBytes, false),
        };
        String[] leftDisplays = new String[columns.length];
        String[] rightDisplays = new String[columns.length];
        DaCsvMetricCompareLevel[] levels = new DaCsvMetricCompareLevel[columns.length];

        for (int i = 0; i < columns.length; i++) {
            MetricColumn col = columns[i];
            leftDisplays[i] = left != null ? col.displayGetter.apply(left) : "";
            rightDisplays[i] = right != null ? col.displayGetter.apply(right) : "";
            levels[i] = resolveLevel(left, right, leftDisplays[i], rightDisplays[i], col.numericGetter, col.cpu);
        }

        for (int i = 0; i < columns.length; i++) {
            appendMetricCell(html, leftDisplays[i], false, levels[i]);
        }
        for (int i = 0; i < columns.length; i++) {
            appendMetricCell(html, rightDisplays[i], true, levels[i]);
        }
    }

    private static final class MetricColumn {
        final Function<DaResourceRow, String> displayGetter;
        final Function<DaResourceRow, ?> numericGetter;
        final boolean cpu;

        MetricColumn(
                Function<DaResourceRow, String> displayGetter,
                Function<DaResourceRow, ?> numericGetter,
                boolean cpu) {
            this.displayGetter = displayGetter;
            this.numericGetter = numericGetter;
            this.cpu = cpu;
        }
    }

    private static DaCsvMetricCompareLevel resolveLevel(
            DaResourceRow left,
            DaResourceRow right,
            String leftDisplay,
            String rightDisplay,
            Function<DaResourceRow, ?> numericGetter,
            boolean cpu) {
        if (DaCsvMetricCompareUtil.isMissingDisplay(leftDisplay)
                || DaCsvMetricCompareUtil.isMissingDisplay(rightDisplay)) {
            return DaCsvMetricCompareLevel.MISSING;
        }
        Object leftNum = left != null ? numericGetter.apply(left) : null;
        Object rightNum = right != null ? numericGetter.apply(right) : null;
        if (cpu) {
            BigDecimal l = leftNum instanceof BigDecimal ? (BigDecimal) leftNum : null;
            BigDecimal r = rightNum instanceof BigDecimal ? (BigDecimal) rightNum : null;
            if (l != null && r != null) {
                return DaCsvMetricCompareUtil.compare(l, r);
            }
        } else {
            Long l = leftNum instanceof Long ? (Long) leftNum : null;
            Long r = rightNum instanceof Long ? (Long) rightNum : null;
            if (l != null && r != null) {
                return DaCsvMetricCompareUtil.compare(l, r);
            }
        }
        return DaCsvMetricCompareUtil.compareDisplayPair(leftDisplay, rightDisplay);
    }

    private static void appendMetricCell(
            StringBuilder html,
            String value,
            boolean rightGroup,
            DaCsvMetricCompareLevel level) {
        String group = rightGroup ? " group-right" : "";
        String cmp = " " + level.getCssClass();
        String display = value != null ? value : "";
        html.append("          <td class=\"").append((group + cmp).trim()).append("\">");
        if (!display.isEmpty()) {
            html.append(escapeHtml(display));
        }
        html.append("</td>\n");
    }

    private static String blankToDefault(String s, String def) {
        return s != null && !s.isBlank() ? s : def;
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
