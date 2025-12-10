package com.example.demo.Services;

import com.example.demo.DTO.Container;
import com.example.demo.DTO.Deployment;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HtmlComparisonService {

    public String generateComparisonTable(List<Deployment> deployments1, List<Deployment> deployments2) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"ru\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Deployment Comparison Table</title>\n");
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
        html.append("        tr:hover {\n");
        html.append("            background-color: #f0f0f0;\n");
        html.append("        }\n");
        html.append("        .diff-green { background-color: #90EE90; }\n");
        html.append("        .diff-yellow { background-color: #FFD700; }\n");
        html.append("        .diff-red { background-color: #FFB6C1; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <table>\n");
        
        // Заголовок таблицы
        html.append("        <thead>\n");
        html.append("            <tr>\n");
        html.append("                <th>Количество подов</th>\n");
        html.append("                <th>Deployment</th>\n");
        html.append("                <th>Container</th>\n");
        html.append("                <th>CpuLim</th>\n");
        html.append("                <th>CpuRq</th>\n");
        html.append("                <th>MemLim</th>\n");
        html.append("                <th>MemRq</th>\n");
        html.append("                <th>CpuMaxUse</th>\n");
        html.append("                <th>CpuAvgUse</th>\n");
        html.append("                <th>MemMaxUse</th>\n");
        html.append("                <th>MemAvgUse</th>\n");
        html.append("            </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");
        
        // Создаем карту для быстрого поиска деплойментов из второго списка
        Map<String, Deployment> deploymentMap2 = new HashMap<>();
        for (Deployment dep : deployments2) {
            deploymentMap2.put(dep.getName(), dep);
        }
        
        // Заполнение таблицы данными
        for (Deployment deployment1 : deployments1) {
            Deployment deployment2 = deploymentMap2.get(deployment1.getName());
            
            int containerCount = deployment1.getContainers().size();
            boolean isFirstRow = true;
            
            // Создаем карту контейнеров из второго деплоймента
            Map<String, Container> containerMap2 = new HashMap<>();
            if (deployment2 != null) {
                for (Container cont : deployment2.getContainers()) {
                    containerMap2.put(cont.getName(), cont);
                }
            }
            
            for (Container container1 : deployment1.getContainers()) {
                Container container2 = containerMap2.get(container1.getName());
                
                html.append("            <tr>\n");
                
                // Количество подов (rowspan только для первой строки)
                if (isFirstRow) {
                    html.append("                <td rowspan=\"").append(containerCount).append("\">")
                        .append(deployment1.getPodCount()).append("</td>\n");
                }
                
                // Название пода (rowspan только для первой строки)
                if (isFirstRow) {
                    html.append("                <td rowspan=\"").append(containerCount).append("\">")
                        .append(deployment1.getName()).append("</td>\n");
                }
                
                // Название контейнера
                html.append("                <td>").append(container1.getName()).append("</td>\n");
                
                // ЦПУ лимиты с разницей
                int diffCpuLim = container2 != null ? container1.getCpuLim() - container2.getCpuLim() : 0;
                String diffCpuLimClass = getDiffColorClass(diffCpuLim);
                html.append("                <td class=\"").append(diffCpuLimClass).append("\">")
                    .append(container1.getCpuLim())
                    .append(" (").append(formatDiff(diffCpuLim)).append(")")
                    .append("</td>\n");
                
                // ЦПУ реквесты с разницей
                int diffCpuRq = container2 != null ? container1.getCpuRq() - container2.getCpuRq() : 0;
                String diffCpuRqClass = getDiffColorClass(diffCpuRq);
                html.append("                <td class=\"").append(diffCpuRqClass).append("\">")
                    .append(container1.getCpuRq())
                    .append(" (").append(formatDiff(diffCpuRq)).append(")")
                    .append("</td>\n");
                
                // Память лимиты с разницей
                int diffMemLim = container2 != null ? container1.getMemLim() - container2.getMemLim() : 0;
                String diffMemLimClass = getDiffColorClass(diffMemLim);
                html.append("                <td class=\"").append(diffMemLimClass).append("\">")
                    .append(container1.getMemLim())
                    .append(" (").append(formatDiff(diffMemLim)).append(")")
                    .append("</td>\n");
                
                // Память реквесты с разницей
                int diffMemRq = container2 != null ? container1.getMemRq() - container2.getMemRq() : 0;
                String diffMemRqClass = getDiffColorClass(diffMemRq);
                html.append("                <td class=\"").append(diffMemRqClass).append("\">")
                    .append(container1.getMemRq())
                    .append(" (").append(formatDiff(diffMemRq)).append(")")
                    .append("</td>\n");
                
                // ЦПУ утилизация макс с разницей
                int diffCpuMax = container2 != null ? container1.getCpuMaxPercent() - container2.getCpuMaxPercent() : 0;
                String diffCpuMaxClass = getDiffColorClass(diffCpuMax);
                html.append("                <td class=\"").append(diffCpuMaxClass).append("\">")
                    .append(container1.getCpuMaxPercent()).append("%")
                    .append(" (").append(formatDiff(diffCpuMax)).append(")")
                    .append("</td>\n");
                
                // ЦПУ утилизация сред с разницей
                int diffCpuAvg = container2 != null ? container1.getCpuAvgPercent() - container2.getCpuAvgPercent() : 0;
                String diffCpuAvgClass = getDiffColorClass(diffCpuAvg);
                html.append("                <td class=\"").append(diffCpuAvgClass).append("\">")
                    .append(container1.getCpuAvgPercent()).append("%")
                    .append(" (").append(formatDiff(diffCpuAvg)).append(")")
                    .append("</td>\n");
                
                // Утилизация памяти макс с разницей
                int diffMemMax = container2 != null ? container1.getMemMaxPercent() - container2.getMemMaxPercent() : 0;
                String diffMemMaxClass = getDiffColorClass(diffMemMax);
                html.append("                <td class=\"").append(diffMemMaxClass).append("\">")
                    .append(container1.getMemMaxPercent()).append("%")
                    .append(" (").append(formatDiff(diffMemMax)).append(")")
                    .append("</td>\n");
                
                // Утилизация памяти сред с разницей
                int diffMemAvg = container2 != null ? container1.getMemAvgPercent() - container2.getMemAvgPercent() : 0;
                String diffMemAvgClass = getDiffColorClass(diffMemAvg);
                html.append("                <td class=\"").append(diffMemAvgClass).append("\">")
                    .append(container1.getMemAvgPercent()).append("%")
                    .append(" (").append(formatDiff(diffMemAvg)).append(")")
                    .append("</td>\n");
                
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
    
    private String getDiffColorClass(int diff) {
        if (diff < 0) {
            return "diff-green"; // Отрицательная разница - зеленый
        } else if (diff > 0) {
            return "diff-red"; // Положительная разница - красный
        } else {
            return "diff-yellow"; // Ноль - желтый
        }
    }
    
    private String formatDiff(int diff) {
        if (diff > 0) {
            return "+" + diff;
        } else {
            return String.valueOf(diff);
        }
    }
}

