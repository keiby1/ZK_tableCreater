package com.example.demo.Services;

import com.example.demo.DTO.Container;
import com.example.demo.DTO.Deployment;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HtmlComparisonService {

    public String generateComparisonTable(List<Deployment> deployments1, List<Deployment> deployments2) {
        return generateComparisonTable(deployments1, deployments2, null, null);
    }

    public String generateComparisonTable(List<Deployment> deployments1, List<Deployment> deployments2,
                                         String fileName1, String fileName2) {
        StringBuilder html = new StringBuilder();
        String name1 = fileName1 != null && !fileName1.isEmpty() ? fileName1 : "Файл 1";
        String name2 = fileName2 != null && !fileName2.isEmpty() ? fileName2 : "Файл 2";

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"ru\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <link rel=\"icon\" href=\"/image/leaf.png\" type=\"image/png\">\n");
        html.append("    <title>Deployment Comparison Table</title>\n");
        html.append("    <style>\n");
        html.append("        body {\n");
        html.append("            font-family: Arial, sans-serif;\n");
        html.append("            margin: 20px;\n");
        html.append("            background-color: #f5f5f5;\n");
        html.append("        }\n");
        html.append("        .compare-header { margin-bottom: 16px; padding: 12px; background: #e8f5e9; border: 1px solid #c8e6c9; border-radius: 6px; }\n");
        html.append("        .compare-header h2 { margin: 0 0 8px 0; font-size: 1.1em; color: #2e7d32; }\n");
        html.append("        .compare-header p { margin: 4px 0; font-size: 0.95em; color: #333; }\n");
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
        html.append("        tr:hover {\n");
        html.append("            background-color: #f0f0f0;\n");
        html.append("        }\n");
        html.append("        .diff-increased { background-color: #c8e6c9; }\n");
        html.append("        .diff-decreased { background-color: #ffcdd2; }\n");
        html.append("        .legend { margin-bottom: 16px; padding: 12px 16px; background: #fafafa; border: 1px solid #e0e0e0; border-radius: 6px; font-size: 0.9em; }\n");
        html.append("        .legend h3 { margin: 0 0 8px 0; font-size: 1em; color: #424242; }\n");
        html.append("        .legend ul { margin: 4px 0 0 0; padding-left: 20px; }\n");
        html.append("        .legend li { margin: 2px 0; }\n");
        html.append("        .legend .swatch { display: inline-block; width: 14px; height: 14px; margin-right: 6px; vertical-align: middle; border: 1px solid #bdbdbd; border-radius: 2px; }\n");
        html.append("        .legend details summary { cursor: pointer; font-weight: bold; font-size: 1em; color: #424242; list-style: none; }\n");
        html.append("        .legend details summary::-webkit-details-marker { display: none; }\n");
        html.append("        .legend details summary::before { content: '▶ '; font-size: 0.75em; }\n");
        html.append("        .legend details[open] summary::before { content: '▼ '; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"compare-header\">\n");
        html.append("        <h2>Сравнение: ").append(escapeHtml(name1)).append(" и ").append(escapeHtml(name2)).append("</h2>\n");
        html.append("        <p>В ячейке: <strong>слева</strong> — значение из первого файла (<em>").append(escapeHtml(name1)).append("</em>), <strong>в скобках</strong> — из второго (<em>").append(escapeHtml(name2)).append("</em>).</p>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"legend\">\n");
        html.append("        <details>\n");
        html.append("            <summary>Расшифровка индикаторов</summary>\n");
        html.append("            <ul>\n");
        html.append("                <li><span class=\"swatch\" style=\"background:#c8e6c9\"></span> Значение увеличилось (во 2-м файле больше)</li>\n");
        html.append("                <li><span class=\"swatch\" style=\"background:#ffcdd2\"></span> Значение уменьшилось (во 2-м файле меньше)</li>\n");
        html.append("                <li>Без заливки — не изменилось</li>\n");
        html.append("            </ul>\n");
        html.append("        </details>\n");
        html.append("    </div>\n");
        html.append("    <table>\n");
        
        // Заголовок таблицы
        html.append("        <thead>\n");
        html.append("            <tr>\n");
        html.append("                <th>Количество подов</th>\n");
        html.append("                <th>Workload</th>\n");
        html.append("                <th>Container</th>\n");
        html.append("                <th>CpuRq</th>\n");
        html.append("                <th>CpuLim</th>\n");
        html.append("                <th>MemRq</th>\n");
        html.append("                <th>MemLim</th>\n");
        html.append("                <th>CpuMaxUse</th>\n");
        html.append("                <th>CpuAvgUse</th>\n");
        html.append("                <th>CpuAbsUse</th>\n");
        html.append("                <th>MemMaxUse</th>\n");
        html.append("                <th>MemAvgUse</th>\n");
        html.append("                <th>MemAbsUse</th>\n");
        html.append("                <th>Время старта</th>\n");
        html.append("            </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");
        
        // Создаем карту для быстрого поиска деплойментов из второго списка
        Map<String, Deployment> deploymentMap2 = new HashMap<>();
        for (Deployment dep : deployments2) {
            deploymentMap2.put(dep.getName(), dep);
        }

        // Сортировка: по Workload (A–Z), по Container внутри (A–Z)
        List<Deployment> sortedDeployments1 = deployments1.stream()
                .sorted(Comparator.comparing(Deployment::getName))
                .collect(Collectors.toList());
        
        // Заполнение таблицы данными
        for (Deployment deployment1 : sortedDeployments1) {
            Deployment deployment2 = deploymentMap2.get(deployment1.getName());
            List<Container> sortedContainers1 = deployment1.getContainers().stream()
                    .sorted(Comparator.comparing(Container::getName))
                    .collect(Collectors.toList());
            int containerCount = sortedContainers1.size();
            boolean isFirstRow = true;
            
            Map<String, Container> containerMap2 = new HashMap<>();
            if (deployment2 != null) {
                for (Container cont : deployment2.getContainers()) {
                    containerMap2.put(cont.getName(), cont);
                }
            }
            
            for (Container container1 : sortedContainers1) {
                Container container2 = containerMap2.get(container1.getName());
                
                html.append("            <tr>\n");
                
                if (isFirstRow) {
                    html.append("                <td rowspan=\"").append(containerCount).append("\">")
                        .append(deployment1.getPodCount()).append("</td>\n");
                }
                if (isFirstRow) {
                    html.append("                <td rowspan=\"").append(containerCount).append("\">")
                        .append(deployment1.getName()).append("</td>\n");
                }
                
                html.append("                <td>").append(container1.getName()).append("</td>\n");
                
                // Формат: значение1 (значение2). Цвет: зелёный если значение2 > значение1, красный если меньше, без заливки если равно
                appendCompareCell(html, container1.getCpuRq(), container2 != null ? container2.getCpuRq() : null, "");
                appendCompareCell(html, container1.getCpuLim(), container2 != null ? container2.getCpuLim() : null, "");
                appendCompareCell(html, container1.getMemRq(), container2 != null ? container2.getMemRq() : null, "");
                appendCompareCell(html, container1.getMemLim(), container2 != null ? container2.getMemLim() : null, "");
                appendCompareCell(html, container1.getCpuMaxPercent(), container2 != null ? container2.getCpuMaxPercent() : null, "%");
                appendCompareCell(html, container1.getCpuAvgPercent(), container2 != null ? container2.getCpuAvgPercent() : null, "%");
                appendCompareCell(html, container1.getCpuMaxAbs(), container2 != null ? container2.getCpuMaxAbs() : null, "");
                appendCompareCell(html, container1.getMemMaxPercent(), container2 != null ? container2.getMemMaxPercent() : null, "%");
                appendCompareCell(html, container1.getMemAvgPercent(), container2 != null ? container2.getMemAvgPercent() : null, "%");
                appendCompareCell(html, container1.getMemMaxAbs(), container2 != null ? container2.getMemMaxAbs() : null, "");
                
                if (isFirstRow) {
                    long start1 = deployment1.getStartTime();
                    Long start2 = deployment2 != null ? deployment2.getStartTime() : null;
                    String startSuffix = " с";
                    html.append("                <td rowspan=\"").append(containerCount).append("\" class=\"")
                        .append(getCompareColorClass(start1, start2)).append("\">");
                    html.append(start1).append(startSuffix);
                    if (start2 != null) {
                        html.append(" (").append(start2).append(startSuffix).append(")");
                    } else {
                        html.append(" (—)");
                    }
                    html.append("</td>\n");
                }
                
                html.append("            </tr>\n");
                isFirstRow = false;
            }
        }
        
        html.append("        </tbody>\n");
        html.append("    </table>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }
    
    /** Зелёный — значение увеличилось (v2 > v1), красный — уменьшилось (v2 < v1), без заливки — не изменилось. */
    private String getCompareColorClass(long v1, Long v2) {
        if (v2 == null) return "";
        if (v2 > v1) return "diff-increased";
        if (v2 < v1) return "diff-decreased";
        return "";
    }

    private void appendCompareCell(StringBuilder html, int v1, Integer v2, String suffix) {
        Long v2Long = (v2 != null) ? Long.valueOf(v2) : null;
        String cls = getCompareColorClass((long) v1, v2Long);
        html.append("                <td");
        if (!cls.isEmpty()) html.append(" class=\"").append(cls).append("\"");
        html.append(">").append(v1).append(suffix);
        if (v2 != null) {
            html.append(" (").append(v2).append(suffix).append(")");
        } else {
            html.append(" (—)");
        }
        html.append("</td>\n");
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

