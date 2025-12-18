package com.example.demo.Services;

import com.example.demo.DTO.Container;
import com.example.demo.DTO.Deployment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HtmlTableService {

    public String generateHtmlTable(List<Deployment> deployments) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"ru\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
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
        html.append("        tr:hover {\n");
        html.append("            background-color: #f0f0f0;\n");
        html.append("        }\n");
        html.append("        .cpu-green { background-color: #90EE90; }\n");
        html.append("        .cpu-yellow { background-color: #FFD700; }\n");
        html.append("        .cpu-red-light { background-color: #FFB6C1; }\n");
        html.append("        .cpu-red-dark { background-color: #DC143C; }\n");
        html.append("        .mem-green { background-color: #90EE90; }\n");
        html.append("        .mem-yellow { background-color: #FFD700; }\n");
        html.append("        .mem-red-light { background-color: #FFB6C1; }\n");
        html.append("        .mem-red-dark { background-color: #DC143C; }\n");
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
        html.append("                <th>CpuAbsUse</th>\n");
        html.append("                <th>MemMaxUse</th>\n");
        html.append("                <th>MemAvgUse</th>\n");
        html.append("                <th>MemAbsUse</th>\n");
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
        int totalContainers = 0;
        
        // Заполнение таблицы данными
        for (Deployment deployment : deployments) {
            int containerCount = deployment.getContainers().size();
            boolean isFirstRow = true;
            
            for (Container container : deployment.getContainers()) {
                html.append("            <tr>\n");
                
                // Количество подов (rowspan только для первой строки)
                if (isFirstRow) {
                    html.append("                <td rowspan=\"").append(containerCount).append("\">")
                        .append(deployment.getPodCount()).append("</td>\n");
                }
                
                // Название пода (rowspan только для первой строки)
                if (isFirstRow) {
                    html.append("                <td rowspan=\"").append(containerCount).append("\">")
                        .append(deployment.getName()).append("</td>\n");
                }
                
                // Название контейнера
                html.append("                <td>").append(container.getName()).append("</td>\n");
                
                // ЦПУ лимиты
                html.append("                <td>").append(container.getCpuLim()).append("</td>\n");
                sumCpuLim += container.getCpuLim();
                
                // ЦПУ реквесты
                html.append("                <td>").append(container.getCpuRq()).append("</td>\n");
                sumCpuRq += container.getCpuRq();
                
                // Память лимиты
                html.append("                <td>").append(container.getMemLim()).append("</td>\n");
                sumMemLim += container.getMemLim();
                
                // Память реквесты
                html.append("                <td>").append(container.getMemRq()).append("</td>\n");
                sumMemRq += container.getMemRq();
                
                // ЦПУ утилизация макс с цветовой подсветкой
                String cpuMaxClass = getCpuColorClass(container.getCpuMaxPercent());
                html.append("                <td class=\"").append(cpuMaxClass).append("\">")
                    .append(container.getCpuMaxPercent()).append("%</td>\n");
                sumCpuMaxUse += container.getCpuMaxPercent();
                
                // ЦПУ утилизация сред с цветовой подсветкой
                String cpuAvgClass = getCpuColorClass(container.getCpuAvgPercent());
                html.append("                <td class=\"").append(cpuAvgClass).append("\">")
                    .append(container.getCpuAvgPercent()).append("%</td>\n");
                sumCpuAvgUse += container.getCpuAvgPercent();
                
                // ЦПУ утилизация абс
                html.append("                <td>").append(container.getCpuMaxAbs()).append("</td>\n");
                sumCpuAbsUse += container.getCpuMaxAbs();
                
                // Утилизация памяти макс с цветовой подсветкой
                String memMaxClass = getMemColorClass(container.getMemMaxPercent());
                html.append("                <td class=\"").append(memMaxClass).append("\">")
                    .append(container.getMemMaxPercent()).append("%</td>\n");
                sumMemMaxUse += container.getMemMaxPercent();
                
                // Утилизация памяти сред с цветовой подсветкой
                String memAvgClass = getMemColorClass(container.getMemAvgPercent());
                html.append("                <td class=\"").append(memAvgClass).append("\">")
                    .append(container.getMemAvgPercent()).append("%</td>\n");
                sumMemAvgUse += container.getMemAvgPercent();
                
                // Утилизация памяти абс
                html.append("                <td>").append(container.getMemMaxAbs()).append("</td>\n");
                sumMemAbsUse += container.getMemMaxAbs();
                
                html.append("            </tr>\n");
                isFirstRow = false;
                totalContainers++;
            }
        }
        
        // Итоговая строка
        html.append("            <tr style=\"font-weight: bold; background-color: #e0e0e0;\">\n");
        html.append("                <td colspan=\"3\">Итого</td>\n");
        html.append("                <td>").append(sumCpuLim).append("</td>\n");
        html.append("                <td>").append(sumCpuRq).append("</td>\n");
        html.append("                <td>").append(sumMemLim).append("</td>\n");
        html.append("                <td>").append(sumMemRq).append("</td>\n");
        html.append("                <td>").append(Math.round((double)sumCpuMaxUse / totalContainers)).append("%</td>\n");
        html.append("                <td>").append(Math.round((double)sumCpuAvgUse / totalContainers)).append("%</td>\n");
        html.append("                <td>").append(sumCpuAbsUse).append("</td>\n");
        html.append("                <td>").append(Math.round((double)sumMemMaxUse / totalContainers)).append("%</td>\n");
        html.append("                <td>").append(Math.round((double)sumMemAvgUse / totalContainers)).append("%</td>\n");
        html.append("                <td>").append(sumMemAbsUse).append("</td>\n");
        html.append("            </tr>\n");
        
        html.append("        </tbody>\n");
        html.append("    </table>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
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
}

