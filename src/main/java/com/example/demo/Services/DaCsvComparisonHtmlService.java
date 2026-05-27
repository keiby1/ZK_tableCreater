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

    private static final int KEY_COLUMN_COUNT = 4;

    private static final String[][] METRIC_COLUMNS = {
            {"cpu-requests", "CPU Requests"},
            {"cpu-limits", "CPU Limits"},
            {"cpu-used", "CPU Used"},
            {"ram-requests", "RAM Requests"},
            {"ram-limits", "RAM Limits"},
            {"ram-used", "RAM Used"},
    };

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
        html.append("    .filters { margin-bottom: 16px; padding: 12px 16px; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }\n");
        html.append("    .filters h3 { margin: 0 0 10px 0; font-size: 0.95em; color: #2e7d32; }\n");
        html.append("    .filters-row { display: flex; flex-wrap: wrap; gap: 12px 20px; align-items: flex-end; }\n");
        html.append("    .filter-field { display: flex; flex-direction: column; gap: 4px; min-width: 180px; flex: 1; }\n");
        html.append("    .filter-field label { font-size: 0.85em; color: #616161; font-weight: bold; }\n");
        html.append("    .filter-field input { padding: 8px 10px; border: 1px solid #bdbdbd; border-radius: 6px; font-size: 0.92em; }\n");
        html.append("    .filter-field input:focus { outline: none; border-color: #4CAF50; box-shadow: 0 0 0 2px rgba(76,175,80,0.2); }\n");
        html.append("    .filter-actions { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }\n");
        html.append("    .filter-btn { padding: 8px 16px; background: #4CAF50; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 0.9em; }\n");
        html.append("    .filter-btn:hover { background: #43A047; }\n");
        html.append("    .filter-btn-secondary { background: #fff; color: #4CAF50; border: 1px solid #4CAF50; }\n");
        html.append("    .filter-btn-secondary:hover { background: #e8f5e9; }\n");
        html.append("    .filter-count { font-size: 0.9em; color: #616161; }\n");
        html.append("    .col-picker { margin-bottom: 16px; padding: 12px 16px; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }\n");
        html.append("    .col-picker h3 { margin: 0 0 10px 0; font-size: 0.95em; color: #2e7d32; }\n");
        html.append("    .col-picker-list { display: flex; flex-wrap: wrap; gap: 10px 20px; align-items: center; }\n");
        html.append("    .col-picker-item { display: flex; align-items: center; gap: 6px; font-size: 0.92em; color: #424242; cursor: pointer; }\n");
        html.append("    .col-picker-item input { cursor: pointer; accent-color: #4CAF50; }\n");
        html.append("    .col-picker-actions { margin-top: 10px; display: flex; gap: 8px; flex-wrap: wrap; }\n");
        html.append("    .col-hidden { display: none !important; }\n");
        html.append("    .back-link { margin-top: 16px; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append(appLayoutService.buildAppHeader());
        html.append("  <div class=\"compare-header\">\n");
        html.append("    <h2>Сравнение: ").append(escapeHtml(name1)).append(" и ").append(escapeHtml(name2)).append("</h2>\n");
        html.append("    <p>Слева — метрики из <strong>").append(escapeHtml(name1)).append("</strong>, ");
        html.append("справа — из <strong>").append(escapeHtml(name2)).append("</strong>. ");
        html.append("Сопоставление по namespace, deployment (из Pod) и container. "
                + "Pod — исходное значение из файла.</p>\n");
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
        html.append("  <div class=\"filters\">\n");
        html.append("    <h3>Фильтр по ключевым полям</h3>\n");
        html.append("    <div class=\"filters-row\">\n");
        html.append("      <div class=\"filter-field\">\n");
        html.append("        <label for=\"filter-namespace\">Namespace</label>\n");
        html.append("        <input type=\"text\" id=\"filter-namespace\" placeholder=\"Часть имени…\" autocomplete=\"off\">\n");
        html.append("      </div>\n");
        html.append("      <div class=\"filter-field\">\n");
        html.append("        <label for=\"filter-deployment\">Deployment</label>\n");
        html.append("        <input type=\"text\" id=\"filter-deployment\" placeholder=\"Часть имени…\" autocomplete=\"off\">\n");
        html.append("      </div>\n");
        html.append("      <div class=\"filter-field\">\n");
        html.append("        <label for=\"filter-pod\">Pod</label>\n");
        html.append("        <input type=\"text\" id=\"filter-pod\" placeholder=\"Часть имени…\" autocomplete=\"off\">\n");
        html.append("      </div>\n");
        html.append("      <div class=\"filter-field\">\n");
        html.append("        <label for=\"filter-container\">Container</label>\n");
        html.append("        <input type=\"text\" id=\"filter-container\" placeholder=\"Часть имени…\" autocomplete=\"off\">\n");
        html.append("      </div>\n");
        html.append("      <div class=\"filter-actions\">\n");
        html.append("        <button type=\"button\" class=\"filter-btn filter-btn-secondary\" id=\"filter-clear\">Сбросить</button>\n");
        html.append("        <span class=\"filter-count\" id=\"filter-count\"></span>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");
        html.append("  <div class=\"col-picker\">\n");
        html.append("    <h3>Столбцы метрик</h3>\n");
        html.append("    <div class=\"col-picker-list\">\n");
        for (String[] col : METRIC_COLUMNS) {
            html.append("      <label class=\"col-picker-item\">")
                    .append("<input type=\"checkbox\" class=\"col-toggle\" id=\"col-")
                    .append(col[0]).append("\" data-col=\"").append(col[0]).append("\" checked> ")
                    .append(escapeHtml(col[1])).append("</label>\n");
        }
        html.append("    </div>\n");
        html.append("    <div class=\"col-picker-actions\">\n");
        html.append("      <button type=\"button\" class=\"filter-btn filter-btn-secondary\" id=\"col-show-all\">Показать все</button>\n");
        html.append("      <button type=\"button\" class=\"filter-btn filter-btn-secondary\" id=\"col-hide-all\">Скрыть все</button>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");
        html.append("  <div class=\"table-wrap\">\n");
        html.append("    <table id=\"da-compare-table\">\n");
        html.append("      <thead>\n");
        html.append("        <tr>\n");
        html.append("          <th colspan=\"").append(KEY_COLUMN_COUNT).append("\" class=\"key-col\">Ключ</th>\n");
        html.append("          <th colspan=\"6\" class=\"group-left\" id=\"header-group-left\">")
                .append(escapeHtml(name1)).append("</th>\n");
        html.append("          <th colspan=\"6\" class=\"group-right\" id=\"header-group-right\">")
                .append(escapeHtml(name2)).append("</th>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("          <th class=\"key-col\">Namespace</th>\n");
        html.append("          <th class=\"key-col\">Deployment</th>\n");
        html.append("          <th class=\"key-col\">Pod</th>\n");
        html.append("          <th class=\"key-col\">Container</th>\n");
        for (String[] col : METRIC_COLUMNS) {
            html.append("          <th class=\"group-left col-metric col-").append(col[0]).append("\">")
                    .append(escapeHtml(col[1])).append("</th>\n");
        }
        for (String[] col : METRIC_COLUMNS) {
            html.append("          <th class=\"group-right col-metric col-").append(col[0]).append("\">")
                    .append(escapeHtml(col[1])).append("</th>\n");
        }
        html.append("        </tr>\n");
        html.append("      </thead>\n");
        html.append("      <tbody id=\"da-compare-tbody\">\n");

        for (DaCompareRow row : rows) {
            DaResourceRow left = row.getLeft();
            DaResourceRow right = row.getRight();
            html.append("        <tr class=\"data-row\" data-namespace=\"")
                    .append(escapeHtmlAttr(row.getNamespace())).append("\" data-pod=\"")
                    .append(escapeHtmlAttr(row.getPod())).append("\" data-deployment=\"")
                    .append(escapeHtmlAttr(row.getDeployment())).append("\" data-container=\"")
                    .append(escapeHtmlAttr(row.getContainerName())).append("\">\n");
            html.append("          <td class=\"key-col\">").append(escapeHtml(row.getNamespace())).append("</td>\n");
            html.append("          <td class=\"key-col\">").append(escapeHtml(row.getDeployment())).append("</td>\n");
            html.append("          <td class=\"key-col\">").append(escapeHtml(row.getPod())).append("</td>\n");
            html.append("          <td class=\"key-col\">").append(escapeHtml(row.getContainerName())).append("</td>\n");
            appendComparedMetricsRow(html, left, right);
            html.append("        </tr>\n");
        }

        html.append("      </tbody>\n");
        html.append("    </table>\n");
        html.append("  </div>\n");
        html.append("  <p class=\"back-link\"><a href=\"/compareDa\">← Вернуться к выбору файлов</a></p>\n");
        html.append("  <script>\n");
        html.append("    (function() {\n");
        html.append("      var nsInput = document.getElementById('filter-namespace');\n");
        html.append("      var podInput = document.getElementById('filter-pod');\n");
        html.append("      var depInput = document.getElementById('filter-deployment');\n");
        html.append("      var contInput = document.getElementById('filter-container');\n");
        html.append("      var countEl = document.getElementById('filter-count');\n");
        html.append("      var clearBtn = document.getElementById('filter-clear');\n");
        html.append("      function rowMatches(tr, nsQ, podQ, depQ, contQ) {\n");
        html.append("        var ns = (tr.getAttribute('data-namespace') || '').toLowerCase();\n");
        html.append("        var pod = (tr.getAttribute('data-pod') || '').toLowerCase();\n");
        html.append("        var dep = (tr.getAttribute('data-deployment') || '').toLowerCase();\n");
        html.append("        var cont = (tr.getAttribute('data-container') || '').toLowerCase();\n");
        html.append("        if (nsQ && ns.indexOf(nsQ) < 0) return false;\n");
        html.append("        if (podQ && pod.indexOf(podQ) < 0) return false;\n");
        html.append("        if (depQ && dep.indexOf(depQ) < 0) return false;\n");
        html.append("        if (contQ && cont.indexOf(contQ) < 0) return false;\n");
        html.append("        return true;\n");
        html.append("      }\n");
        html.append("      function applyFilter() {\n");
        html.append("        var nsQ = nsInput.value.trim().toLowerCase();\n");
        html.append("        var podQ = podInput.value.trim().toLowerCase();\n");
        html.append("        var depQ = depInput.value.trim().toLowerCase();\n");
        html.append("        var contQ = contInput.value.trim().toLowerCase();\n");
        html.append("        var rows = document.querySelectorAll('#da-compare-tbody tr.data-row');\n");
        html.append("        var visible = 0;\n");
        html.append("        rows.forEach(function(tr) {\n");
        html.append("          var show = rowMatches(tr, nsQ, podQ, depQ, contQ);\n");
        html.append("          tr.style.display = show ? '' : 'none';\n");
        html.append("          if (show) visible++;\n");
        html.append("        });\n");
        html.append("        countEl.textContent = 'Показано: ' + visible + ' из ' + rows.length;\n");
        html.append("      }\n");
        html.append("      [nsInput, depInput, podInput, contInput].forEach(function(el) {\n");
        html.append("        el.addEventListener('input', applyFilter);\n");
        html.append("      });\n");
        html.append("      clearBtn.addEventListener('click', function() {\n");
        html.append("        nsInput.value = '';\n");
        html.append("        podInput.value = '';\n");
        html.append("        depInput.value = '';\n");
        html.append("        contInput.value = '';\n");
        html.append("        applyFilter();\n");
        html.append("      });\n");
        html.append("      applyFilter();\n");
        html.append("    })();\n");
        html.append("    (function() {\n");
        html.append("      var toggles = document.querySelectorAll('.col-toggle');\n");
        html.append("      var headerLeft = document.getElementById('header-group-left');\n");
        html.append("      var headerRight = document.getElementById('header-group-right');\n");
        html.append("      function applyColumnVisibility() {\n");
        html.append("        var visible = 0;\n");
        html.append("        toggles.forEach(function(cb) {\n");
        html.append("          var colId = cb.getAttribute('data-col');\n");
        html.append("          var show = cb.checked;\n");
        html.append("          if (show) visible++;\n");
        html.append("          document.querySelectorAll('.col-' + colId).forEach(function(el) {\n");
        html.append("            if (show) el.classList.remove('col-hidden'); else el.classList.add('col-hidden');\n");
        html.append("          });\n");
        html.append("        });\n");
        html.append("        if (headerLeft) headerLeft.colSpan = Math.max(visible, 1);\n");
        html.append("        if (headerRight) headerRight.colSpan = Math.max(visible, 1);\n");
        html.append("      }\n");
        html.append("      toggles.forEach(function(cb) { cb.addEventListener('change', applyColumnVisibility); });\n");
        html.append("      document.getElementById('col-show-all').addEventListener('click', function() {\n");
        html.append("        toggles.forEach(function(cb) { cb.checked = true; });\n");
        html.append("        applyColumnVisibility();\n");
        html.append("      });\n");
        html.append("      document.getElementById('col-hide-all').addEventListener('click', function() {\n");
        html.append("        toggles.forEach(function(cb) { cb.checked = false; });\n");
        html.append("        applyColumnVisibility();\n");
        html.append("      });\n");
        html.append("      applyColumnVisibility();\n");
        html.append("    })();\n");
        html.append("  </script>\n");
        html.append("</body>\n");
        html.append("</html>");
        return html.toString();
    }

    /**
     * Порядок ячеек как в шапке: сначала все 6 метрик файла 1, затем все 6 метрик файла 2.
     */
    private static void appendComparedMetricsRow(StringBuilder html, DaResourceRow left, DaResourceRow right) {
        MetricColumn[] columns = {
                new MetricColumn("cpu-requests", DaResourceRow::getCpuRequestDisplay,
                        DaResourceRow::getCpuRequestCores, true),
                new MetricColumn("cpu-limits", DaResourceRow::getCpuLimitDisplay,
                        DaResourceRow::getCpuLimitCores, true),
                new MetricColumn("cpu-used", DaResourceRow::getCpuUsedDisplay,
                        DaResourceRow::getCpuUsedCores, true),
                new MetricColumn("ram-requests", DaResourceRow::getRamRequestDisplay,
                        DaResourceRow::getRamRequestBytes, false),
                new MetricColumn("ram-limits", DaResourceRow::getRamLimitDisplay,
                        DaResourceRow::getRamLimitBytes, false),
                new MetricColumn("ram-used", DaResourceRow::getRamUsedDisplay,
                        DaResourceRow::getRamUsedBytes, false),
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
            appendMetricCell(html, leftDisplays[i], false, levels[i], columns[i].columnId);
        }
        for (int i = 0; i < columns.length; i++) {
            appendMetricCell(html, rightDisplays[i], true, levels[i], columns[i].columnId);
        }
    }

    private static final class MetricColumn {
        final String columnId;
        final Function<DaResourceRow, String> displayGetter;
        final Function<DaResourceRow, ?> numericGetter;
        final boolean cpu;

        MetricColumn(
                String columnId,
                Function<DaResourceRow, String> displayGetter,
                Function<DaResourceRow, ?> numericGetter,
                boolean cpu) {
            this.columnId = columnId;
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
            DaCsvMetricCompareLevel level,
            String columnId) {
        String group = rightGroup ? " group-right" : "";
        String cmp = " " + level.getCssClass();
        String col = " col-metric col-" + columnId;
        String display = value != null ? value : "";
        html.append("          <td class=\"").append((group + col + cmp).trim()).append("\">");
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

    private static String escapeHtmlAttr(String s) {
        return escapeHtml(s);
    }
}
