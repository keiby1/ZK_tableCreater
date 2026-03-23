package com.example.demo.Services;

import com.example.demo.DTO.Container;
import com.example.demo.DTO.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HtmlTableService {

    @Autowired
    private AppLayoutService appLayoutService;

    private static final int MAX_CPU_PER_DEPLOYMENT = 4000;
    private static final int MAX_RAM_PER_DEPLOYMENT = 8000; // 8 ГБ (в МБ)

    private static final DateTimeFormatter INTERVAL_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").withZone(ZoneId.of("UTC"));

    public String generateHtmlTable(List<Deployment> deployments) {
        return generateHtmlTable(deployments, null, null);
    }

    /**
     * Генерирует HTML таблицу с опциональным блоком интервала выгрузки (между расшифровкой и таблицей).
     * @param from начало интервала (UTC, мс) или null
     * @param to   конец интервала (UTC, мс) или null
     */
    public String generateHtmlTable(List<Deployment> deployments, Long from, Long to) {
        return generateHtmlTable(deployments, from, to, true);
    }

    /**
     * Генерирует HTML таблицу.
     * @param includeHeader false — не включать хедер/меню (для сохранения в файл)
     */
    public String generateHtmlTable(List<Deployment> deployments, Long from, Long to, boolean includeHeader) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"ru\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <link rel=\"icon\" href=\"/image/leaf.png\" type=\"image/png\">\n");
        html.append("    <title>Deployment Table</title>\n");
        html.append("    <style>\n");
        html.append("        body {\n");
        html.append("            font-family: Arial, sans-serif;\n");
        html.append("            margin: 20px;\n");
        html.append("            background-color: #f5f5f5;\n");
        html.append("        }\n");
        html.append("        table {\n");
        html.append("            border-collapse: collapse;\n");
        html.append("            width: 100%;\n");
        html.append("            background-color: white;\n");
        html.append("            box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n");
        html.append("        }\n");
        html.append("        th {\n");
        html.append("            background-color: #4CAF50;\n");
        html.append("            color: white;\n");
        html.append("            padding: 12px;\n");
        html.append("            text-align: left;\n");
        html.append("            border: 1px solid #ddd;\n");
        html.append("            font-weight: bold;\n");
        html.append("        }\n");
        html.append("        td {\n");
        html.append("            padding: 10px;\n");
        html.append("            border: 1px solid #ddd;\n");
        html.append("            text-align: center;\n");
        html.append("        }\n");
        html.append("        tbody tr:hover {\n");
        html.append("            background-color: #f0f0f0;\n");
        html.append("        }\n");
        html.append("        tbody tr.data-row { cursor: pointer; }\n");
        html.append("        tbody tr.data-row.row-selected {\n");
        html.append("            outline: 3px solid #1976d2;\n");
        html.append("            outline-offset: -3px;\n");
        html.append("            background-color: #e3f2fd;\n");
        html.append("        }\n");
        html.append("        tbody tr.data-row.row-selected:hover {\n");
        html.append("            background-color: #bbdefb;\n");
        html.append("            outline-color: #0d47a1;\n");
        html.append("        }\n");
        html.append("        th.col-draggable { cursor: grab; user-select: none; }\n");
        html.append("        th.col-draggable:active { cursor: grabbing; }\n");
        html.append("        th.col-drag-over { box-shadow: inset 0 -4px 0 #ffeb3b; }\n");
        html.append("        .cpu-green { background-color: #c8e6c9; }\n");
        html.append("        .cpu-yellow { background-color: #fff9c4; }\n");
        html.append("        .cpu-red-light { background-color: #ffcdd2; }\n");
        html.append("        .cpu-red-dark { background-color: #ef9a9a; }\n");
        html.append("        .mem-green { background-color: #c8e6c9; }\n");
        html.append("        .mem-yellow { background-color: #fff9c4; }\n");
        html.append("        .mem-red-light { background-color: #ffcdd2; }\n");
        html.append("        .mem-red-dark { background-color: #ef9a9a; }\n");
        html.append("        .over-limit { background-color: #78909c; color: #fff; }\n");
        html.append("        .start-green { background-color: #c8e6c9; }\n");
        html.append("        .start-yellow { background-color: #fff9c4; }\n");
        html.append("        .start-orange { background-color: #ffe0b2; }\n");
        html.append("        .start-red { background-color: #ffcdd2; }\n");
        html.append("        .legend { margin-bottom: 16px; padding: 12px 16px; background: #fafafa; border: 1px solid #e0e0e0; border-radius: 6px; font-size: 0.9em; }\n");
        html.append("        .legend h3 { margin: 0 0 8px 0; font-size: 1em; color: #424242; }\n");
        html.append("        .legend ul { margin: 4px 0 0 0; padding-left: 20px; }\n");
        html.append("        .legend li { margin: 2px 0; }\n");
        html.append("        .legend .swatch { display: inline-block; width: 14px; height: 14px; margin-right: 6px; vertical-align: middle; border: 1px solid #bdbdbd; border-radius: 2px; }\n");
        html.append("        .legend details summary { cursor: pointer; font-weight: bold; font-size: 1em; color: #424242; list-style: none; }\n");
        html.append("        .legend details summary::-webkit-details-marker { display: none; }\n");
        html.append("        .legend details summary::before { content: '▶ '; font-size: 0.75em; }\n");
        html.append("        .legend details[open] summary::before { content: '▼ '; }\n");
        html.append("        .interval-info { margin-bottom: 16px; padding: 10px 16px; background: #e3f2fd; border: 1px solid #90caf9; border-radius: 6px; font-size: 0.95em; color: #1565c0; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        if (includeHeader) {
            html.append(appLayoutService.buildAppHeader());
        }
        html.append("    <div class=\"interval-info\">\n");
        html.append("        Интервал выгрузки: ").append(formatInterval(from, to)).append("\n");
        html.append("    </div>\n");
        html.append("    <table id=\"deployment-table\">\n");
        
        // Заголовок: 2 фикс. столбца + 12 переставляемых (data-block-key) + время старта
        html.append("        <thead>\n");
        html.append("            <tr>\n");
        html.append("                <th data-fixed=\"left\">Количество подов</th>\n");
        html.append("                <th data-fixed=\"left\">Workload</th>\n");
        html.append("                <th data-block-key=\"container\" class=\"col-draggable\" title=\"Перетащите, чтобы поменять порядок столбцов\">Container</th>\n");
        html.append("                <th data-block-key=\"cpuRq\" class=\"col-draggable\">CpuRq</th>\n");
        html.append("                <th data-block-key=\"cpuLim\" class=\"col-draggable\">CpuLim</th>\n");
        html.append("                <th data-block-key=\"memRq\" class=\"col-draggable\">MemRq</th>\n");
        html.append("                <th data-block-key=\"memLim\" class=\"col-draggable\">MemLim</th>\n");
        html.append("                <th data-block-key=\"cpuMaxUse\" class=\"col-draggable\">CpuMaxUse</th>\n");
        html.append("                <th data-block-key=\"cpuAvgUse\" class=\"col-draggable\">CpuAvgUse</th>\n");
        html.append("                <th data-block-key=\"cpuAbsUse\" class=\"col-draggable\">CpuAbsUse</th>\n");
        html.append("                <th data-block-key=\"memMaxUse\" class=\"col-draggable\">MemMaxUse</th>\n");
        html.append("                <th data-block-key=\"memAvgUse\" class=\"col-draggable\">MemAvgUse</th>\n");
        html.append("                <th data-block-key=\"memAbsUse\" class=\"col-draggable\">MemAbsUse</th>\n");
        html.append("                <th data-block-key=\"throttling\" class=\"col-draggable\">Троттлинг</th>\n");
        html.append("                <th data-fixed=\"right\">Время старта</th>\n");
        html.append("            </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");
        
        // Переменные для подсчета итогов
        long sumCpuLim = 0;
        long sumCpuRq = 0;
        long sumMemLim = 0;
        long sumMemRq = 0;
        long sumCpuAbsUse = 0;
        long sumMemAbsUse = 0;
        long sumCpuMaxUse = 0;
        long sumCpuAvgUse = 0;
        long sumMemMaxUse = 0;
        long sumMemAvgUse = 0;
        long sumThrottlingPercent = 0;
        int totalContainers = 0;
        
        // Сортировка: 1) по Workload (A–Z), 2) по Container внутри workload (A–Z)
        List<Deployment> sortedDeployments = deployments.stream()
                .sorted(Comparator.comparing(Deployment::getName))
                .collect(Collectors.toList());

        // Заполнение таблицы данными
        for (Deployment deployment : sortedDeployments) {
            List<Container> sortedContainers = deployment.getContainers().stream()
                    .sorted(Comparator.comparing(Container::getName))
                    .collect(Collectors.toList());
            int containerCount = sortedContainers.size();
            boolean isFirstRow = true;

            // Суммы по контейнерам деплоймента для проверки лимитов
            long deployCpuLim = 0;
            long deployCpuRq = 0;
            long deployMemLim = 0;
            long deployMemRq = 0;
            for (Container c : sortedContainers) {
                deployCpuLim += c.getCpuLim();
                deployCpuRq += c.getCpuRq();
                deployMemLim += c.getMemLim();
                deployMemRq += c.getMemRq();
            }
            boolean cpuLimOver = deployCpuLim > MAX_CPU_PER_DEPLOYMENT;
            boolean cpuRqOver = deployCpuRq > MAX_CPU_PER_DEPLOYMENT;
            boolean memLimOver = deployMemLim > MAX_RAM_PER_DEPLOYMENT;
            boolean memRqOver = deployMemRq > MAX_RAM_PER_DEPLOYMENT;
            String cpuLimClass = cpuLimOver ? "over-limit" : "";
            String cpuRqClass = cpuRqOver ? "over-limit" : "";
            String memLimClass = memLimOver ? "over-limit" : "";
            String memRqClass = memRqOver ? "over-limit" : "";
            
            for (Container container : sortedContainers) {
                html.append("            <tr class=\"data-row ").append(isFirstRow ? "group-start" : "group-cont").append("\">\n");
                
                // Количество подов (rowspan только для первой строки)
                if (isFirstRow) {
                    html.append("                <td rowspan=\"").append(containerCount).append("\">")
                        .append(deployment.getPodCount()).append("</td>\n");
                }
                // Название workload (rowspan только для первой строки)
                if (isFirstRow) {
                    html.append("                <td rowspan=\"").append(containerCount).append("\">")
                        .append(deployment.getName()).append("</td>\n");
                }
                
                // Название контейнера (переставляемый блок)
                html.append("                <td data-block-key=\"container\">").append(container.getName()).append("</td>\n");
                
                // CpuRq, CpuLim, MemRq, MemLim (порядок столбцов)
                appendTd(html, container.getCpuRq(), cpuRqClass, "cpuRq");
                sumCpuRq += container.getCpuRq();
                appendTd(html, container.getCpuLim(), cpuLimClass, "cpuLim");
                sumCpuLim += container.getCpuLim();
                appendTd(html, container.getMemRq(), memRqClass, "memRq");
                sumMemRq += container.getMemRq();
                appendTd(html, container.getMemLim(), memLimClass, "memLim");
                sumMemLim += container.getMemLim();
                
                // ЦПУ утилизация макс с цветовой подсветкой
                String cpuMaxClass = getCpuColorClass(container.getCpuMaxPercent());
                appendTdPercent(html, container.getCpuMaxPercent(), cpuMaxClass, "cpuMaxUse");
                sumCpuMaxUse += container.getCpuMaxPercent();
                
                // ЦПУ утилизация сред с цветовой подсветкой
                String cpuAvgClass = getCpuColorClass(container.getCpuAvgPercent());
                appendTdPercent(html, container.getCpuAvgPercent(), cpuAvgClass, "cpuAvgUse");
                sumCpuAvgUse += container.getCpuAvgPercent();
                
                // ЦПУ утилизация абс
                appendTd(html, container.getCpuMaxAbs(), null, "cpuAbsUse");
                sumCpuAbsUse += container.getCpuMaxAbs();
                
                // Утилизация памяти макс с цветовой подсветкой
                String memMaxClass = getMemColorClass(container.getMemMaxPercent());
                appendTdPercent(html, container.getMemMaxPercent(), memMaxClass, "memMaxUse");
                sumMemMaxUse += container.getMemMaxPercent();
                
                // Утилизация памяти сред с цветовой подсветкой
                String memAvgClass = getMemColorClass(container.getMemAvgPercent());
                appendTdPercent(html, container.getMemAvgPercent(), memAvgClass, "memAvgUse");
                sumMemAvgUse += container.getMemAvgPercent();
                
                // Утилизация памяти абс
                appendTd(html, container.getMemMaxAbs(), null, "memAbsUse");
                sumMemAbsUse += container.getMemMaxAbs();
                
                // Троттлинг CPU
                int throttling = container.getThrottlingPercent();
                String throttlingClass = getThrottlingColorClass(throttling);
                appendTdPercent(html, throttling, throttlingClass, "throttling");
                sumThrottlingPercent += throttling;
                
                // Время старта (один столбец на деплоймент, rowspan)
                if (isFirstRow) {
                    long startSec = deployment.getStartTime();
                    String startClass = getStartTimeColorClass(startSec);
                    html.append("                <td rowspan=\"").append(containerCount).append("\" class=\"").append(startClass).append("\">")
                        .append(startSec).append(" с</td>\n");
                }
                
                html.append("            </tr>\n");
                isFirstRow = false;
                totalContainers++;
            }
        }
        
        // Итоговая строка: 2 фикс. ячейки + блок из 12 (как в строках данных) + время старта
        html.append("            <tr class=\"totals-row\" style=\"font-weight: bold; background-color: #e0e0e0;\">\n");
        html.append("                <td>—</td>\n");
        html.append("                <td>—</td>\n");
        html.append("                <td data-block-key=\"container\">Итого</td>\n");
        appendTd(html, sumCpuRq, null, "cpuRq");
        appendTd(html, sumCpuLim, null, "cpuLim");
        appendTd(html, sumMemRq, null, "memRq");
        appendTd(html, sumMemLim, null, "memLim");
        html.append("                <td data-block-key=\"cpuMaxUse\">").append(totalContainers > 0 ? Math.round((double) sumCpuMaxUse / totalContainers) + "%" : "—").append("</td>\n");
        html.append("                <td data-block-key=\"cpuAvgUse\">").append(totalContainers > 0 ? Math.round((double) sumCpuAvgUse / totalContainers) + "%" : "—").append("</td>\n");
        appendTd(html, sumCpuAbsUse, null, "cpuAbsUse");
        html.append("                <td data-block-key=\"memMaxUse\">").append(totalContainers > 0 ? Math.round((double) sumMemMaxUse / totalContainers) + "%" : "—").append("</td>\n");
        html.append("                <td data-block-key=\"memAvgUse\">").append(totalContainers > 0 ? Math.round((double) sumMemAvgUse / totalContainers) + "%" : "—").append("</td>\n");
        appendTd(html, sumMemAbsUse, null, "memAbsUse");
        html.append("                <td data-block-key=\"throttling\">").append(totalContainers > 0 ? Math.round((double) sumThrottlingPercent / totalContainers) + "%" : "—").append("</td>\n");
        html.append("                <td>—</td>\n");
        html.append("            </tr>\n");
        
        html.append("        </tbody>\n");
        html.append("    </table>\n");
        if (includeHeader) {
            html.append("    <script>\n");
            html.append("      (function() {\n");
            html.append("        var BLOCK_KEYS = ['container','cpuRq','cpuLim','memRq','memLim','cpuMaxUse','cpuAvgUse','cpuAbsUse','memMaxUse','memAvgUse','memAbsUse','throttling'];\n");
            html.append("        var LS_KEY = 'zkDeploymentTableBlockOrder';\n");
            html.append("        function getThBlock(table) {\n");
            html.append("          return Array.prototype.slice.call(table.querySelectorAll('thead th[data-block-key]'));\n");
            html.append("        }\n");
            html.append("        function reorderBlockCells(tr, keys) {\n");
            html.append("          var cells = [];\n");
            html.append("          keys.forEach(function(k) {\n");
            html.append("            var el = tr.querySelector('[data-block-key=\"' + k + '\"]');\n");
            html.append("            if (el) cells.push(el);\n");
            html.append("          });\n");
            html.append("          if (cells.length !== BLOCK_KEYS.length) return;\n");
            html.append("          var insertAfter = null;\n");
            html.append("          if (tr.classList.contains('group-start')) {\n");
            html.append("            insertAfter = tr.children[1];\n");
            html.append("          } else if (tr.classList.contains('group-cont')) {\n");
            html.append("            insertAfter = null;\n");
            html.append("          } else if (tr.classList.contains('totals-row')) {\n");
            html.append("            insertAfter = tr.children[1];\n");
            html.append("          } else return;\n");
            html.append("          cells.forEach(function(td) {\n");
            html.append("            if (insertAfter) { tr.insertBefore(td, insertAfter.nextSibling); insertAfter = td; }\n");
            html.append("            else { tr.insertBefore(td, tr.firstChild); insertAfter = td; }\n");
            html.append("          });\n");
            html.append("        }\n");
            html.append("        function applyOrder(keys) {\n");
            html.append("          var table = document.getElementById('deployment-table');\n");
            html.append("          if (!table) return;\n");
        html.append("          var theadTr = table.querySelector('thead tr');\n");
        html.append("          var thByKey = {};\n");
        html.append("          getThBlock(table).forEach(function(th) { thByKey[th.getAttribute('data-block-key')] = th; });\n");
        html.append("          var refTh = theadTr.children[1];\n");
        html.append("          keys.forEach(function(k) {\n");
        html.append("            var th = thByKey[k];\n");
        html.append("            if (th && refTh && th.parentNode === theadTr) {\n");
        html.append("              theadTr.insertBefore(th, refTh.nextSibling);\n");
        html.append("              refTh = th;\n");
        html.append("            }\n");
        html.append("          });\n");
            html.append("          table.querySelectorAll('tbody tr.group-start').forEach(function(tr) { reorderBlockCells(tr, keys); });\n");
            html.append("          table.querySelectorAll('tbody tr.group-cont').forEach(function(tr) { reorderBlockCells(tr, keys); });\n");
            html.append("          var tot = table.querySelector('tr.totals-row');\n");
            html.append("          if (tot) reorderBlockCells(tot, keys);\n");
            html.append("        }\n");
            html.append("        function loadOrder() {\n");
            html.append("          try {\n");
            html.append("            var raw = localStorage.getItem(LS_KEY);\n");
            html.append("            if (!raw) return BLOCK_KEYS.slice();\n");
            html.append("            var arr = JSON.parse(raw);\n");
            html.append("            if (!Array.isArray(arr) || arr.length !== BLOCK_KEYS.length) return BLOCK_KEYS.slice();\n");
            html.append("            var set = {};\n");
            html.append("            arr.forEach(function(k) { if (BLOCK_KEYS.indexOf(k) >= 0) set[k] = true; });\n");
            html.append("            if (Object.keys(set).length !== BLOCK_KEYS.length) return BLOCK_KEYS.slice();\n");
            html.append("            return arr;\n");
            html.append("          } catch (e) { return BLOCK_KEYS.slice(); }\n");
            html.append("        }\n");
            html.append("        function saveOrder(keys) {\n");
            html.append("          try { localStorage.setItem(LS_KEY, JSON.stringify(keys)); } catch (e) {}\n");
            html.append("        }\n");
            html.append("        function initColumnDrag() {\n");
            html.append("          var table = document.getElementById('deployment-table');\n");
            html.append("          if (!table) return;\n");
            html.append("          var dragKey = null;\n");
            html.append("          getThBlock(table).forEach(function(th) {\n");
            html.append("            th.setAttribute('draggable', 'true');\n");
            html.append("            th.addEventListener('dragstart', function(e) {\n");
            html.append("              dragKey = th.getAttribute('data-block-key');\n");
            html.append("              e.dataTransfer.effectAllowed = 'move';\n");
            html.append("              try { e.dataTransfer.setData('text/plain', dragKey); } catch (err) {}\n");
            html.append("              th.classList.add('col-drag-over');\n");
            html.append("            });\n");
            html.append("            th.addEventListener('dragend', function() {\n");
            html.append("              getThBlock(table).forEach(function(t) { t.classList.remove('col-drag-over'); });\n");
            html.append("              dragKey = null;\n");
            html.append("            });\n");
            html.append("            th.addEventListener('dragover', function(e) { e.preventDefault(); e.dataTransfer.dropEffect = 'move'; });\n");
            html.append("            th.addEventListener('dragenter', function() { th.classList.add('col-drag-over'); });\n");
            html.append("            th.addEventListener('dragleave', function() { th.classList.remove('col-drag-over'); });\n");
            html.append("            th.addEventListener('drop', function(e) {\n");
            html.append("              e.preventDefault();\n");
            html.append("              var targetKey = th.getAttribute('data-block-key');\n");
            html.append("              var srcKey = dragKey;\n");
            html.append("              try { srcKey = e.dataTransfer.getData('text/plain') || dragKey; } catch (err2) {}\n");
            html.append("              if (!srcKey || !targetKey || srcKey === targetKey) return;\n");
            html.append("              var order = loadOrder();\n");
            html.append("              var i = order.indexOf(srcKey), j = order.indexOf(targetKey);\n");
            html.append("              if (i < 0 || j < 0) return;\n");
            html.append("              order.splice(i, 1);\n");
            html.append("              order.splice(j, 0, srcKey);\n");
            html.append("              saveOrder(order);\n");
            html.append("              applyOrder(order);\n");
            html.append("            });\n");
            html.append("          });\n");
            html.append("        }\n");
            html.append("        applyOrder(loadOrder());\n");
            html.append("        initColumnDrag();\n");
            html.append("        var rows = document.querySelectorAll('tbody tr.data-row');\n");
            html.append("        rows.forEach(function(tr) {\n");
            html.append("          tr.addEventListener('click', function(e) {\n");
            html.append("            document.querySelectorAll('tbody tr.data-row.row-selected').forEach(function(r) { r.classList.remove('row-selected'); });\n");
            html.append("            tr.classList.add('row-selected');\n");
            html.append("          });\n");
            html.append("        });\n");
            html.append("      })();\n");
            html.append("    </script>\n");
        }
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }

    private String formatInterval(Long from, Long to) {
        if (from != null && to != null) {
            String fromStr = INTERVAL_FORMAT.format(Instant.ofEpochMilli(from));
            String toStr = INTERVAL_FORMAT.format(Instant.ofEpochMilli(to));
            return "с " + fromStr + " по " + toStr + " (UTC)";
        }
        return "не указан";
    }
    
    private void appendTd(StringBuilder html, long value, String cssClass, String dataBlockKey) {
        String dk = (dataBlockKey != null && !dataBlockKey.isEmpty())
                ? " data-block-key=\"" + dataBlockKey + "\"" : "";
        if (cssClass != null && !cssClass.isEmpty()) {
            html.append("                <td class=\"").append(cssClass).append("\"").append(dk).append(">").append(value).append("</td>\n");
        } else {
            html.append("                <td").append(dk).append(">").append(value).append("</td>\n");
        }
    }

    private void appendTdPercent(StringBuilder html, int percent, String cssClass, String dataBlockKey) {
        String dk = (dataBlockKey != null && !dataBlockKey.isEmpty())
                ? " data-block-key=\"" + dataBlockKey + "\"" : "";
        html.append("                <td class=\"").append(cssClass).append("\"").append(dk).append(">")
                .append(percent).append("%</td>\n");
    }

    private String getCpuColorClass(int percent) {
        if (percent >= 60 && percent <= 79) {
            return "cpu-green";
        } else if (percent >= 20 && percent <= 59) {
            return "cpu-yellow";
        } else if (percent >= 0 && percent <= 19) {
            return "cpu-red-light"; // 0-19% - очень низкая утилизация
        } else {
            return "cpu-red-dark"; // >= 80% - критически высокая утилизация (включая > 100%)
        }
    }
    
    private String getMemColorClass(int percent) {
        if (percent >= 60 && percent <= 79) {
            return "mem-green";
        } else if (percent >= 20 && percent <= 59) {
            return "mem-yellow";
        } else if (percent >= 0 && percent <= 19) {
            return "mem-red-light"; // 0-19% - очень низкая утилизация
        } else {
            return "mem-red-dark"; // >= 80% - критически высокая утилизация (включая > 100%)
        }
    }

    /** Цвет по троттлингу: ≤1% — зелёный, >1% и ≤3% — жёлтый, >3% и ≤5% — оранжевый, >5% — красный. */
    private String getThrottlingColorClass(int percent) {
        if (percent <= 1) return "cpu-green";
        if (percent <= 3) return "cpu-yellow";
        if (percent <= 5) return "start-orange";
        return "cpu-red-dark";
    }

    /** Цвет по времени старта (секунды): до 1 мин — зелёный, 1м1с–1.5 мин — желтоватый, 1.5–2 мин — оранжевый, >2 мин — красный (приглушённые цвета). */
    private String getStartTimeColorClass(long seconds) {
        if (seconds <= 60) {
            return "start-green";
        } else if (seconds <= 90) {
            return "start-yellow";
        } else if (seconds <= 120) {
            return "start-orange";
        } else {
            return "start-red";
        }
    }
}

