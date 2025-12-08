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
        html.append("        .cpu-red { background-color: #FFB6C1; }\n");
        html.append("        .mem-green { background-color: #90EE90; }\n");
        html.append("        .mem-yellow { background-color: #FFD700; }\n");
        html.append("        .mem-red { background-color: #FFB6C1; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <table>\n");
        
        // Заголовок таблицы
        html.append("        <thead>\n");
        html.append("            <tr>\n");
        html.append("                <th>Количество подов</th>\n");
        html.append("                <th>Название пода</th>\n");
        html.append("                <th>Название контейнера</th>\n");
        html.append("                <th>ЦПУ лимиты</th>\n");
        html.append("                <th>ЦПУ реквесты</th>\n");
        html.append("                <th>Память лимиты</th>\n");
        html.append("                <th>Память реквесты</th>\n");
        html.append("                <th>ЦПУ утилизация макс</th>\n");
        html.append("                <th>ЦПУ утилизация сред</th>\n");
        html.append("                <th>ЦПУ утилизация абс</th>\n");
        html.append("                <th>Утилизация памяти макс</th>\n");
        html.append("                <th>Утилизация памяти сред</th>\n");
        html.append("                <th>Утилизация памяти абс</th>\n");
        html.append("            </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");
        
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
                
                // ЦПУ реквесты
                html.append("                <td>").append(container.getCpuRq()).append("</td>\n");
                
                // Память лимиты
                html.append("                <td>").append(container.getMemLim()).append("</td>\n");
                
                // Память реквесты
                html.append("                <td>").append(container.getMemRq()).append("</td>\n");
                
                // ЦПУ утилизация макс с цветовой подсветкой
                String cpuMaxClass = getCpuColorClass(container.getCpuMaxPercent());
                html.append("                <td class=\"").append(cpuMaxClass).append("\">")
                    .append(container.getCpuMaxPercent()).append("</td>\n");
                
                // ЦПУ утилизация сред с цветовой подсветкой
                String cpuAvgClass = getCpuColorClass(container.getCpuAvgPercent());
                html.append("                <td class=\"").append(cpuAvgClass).append("\">")
                    .append(container.getCpuAvgPercent()).append("</td>\n");
                
                // ЦПУ утилизация абс
                html.append("                <td>").append(container.getCpuMaxAbs()).append("</td>\n");
                
                // Утилизация памяти макс с цветовой подсветкой
                String memMaxClass = getMemColorClass(container.getMemMaxPercent());
                html.append("                <td class=\"").append(memMaxClass).append("\">")
                    .append(container.getMemMaxPercent()).append("</td>\n");
                
                // Утилизация памяти сред с цветовой подсветкой
                String memAvgClass = getMemColorClass(container.getMemAvgPercent());
                html.append("                <td class=\"").append(memAvgClass).append("\">")
                    .append(container.getMemAvgPercent()).append("</td>\n");
                
                // Утилизация памяти абс
                html.append("                <td>").append(container.getMemMaxAbs()).append("</td>\n");
                
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
    
    private String getCpuColorClass(int percent) {
        if (percent >= 60 && percent <= 79) {
            return "cpu-green";
        } else if (percent >= 20 && percent <= 59) {
            return "cpu-yellow";
        } else {
            return "cpu-red"; // 0-19 и 80-100
        }
    }
    
    private String getMemColorClass(int percent) {
        if (percent >= 60 && percent <= 79) {
            return "mem-green";
        } else if (percent >= 20 && percent <= 59) {
            return "mem-yellow";
        } else {
            return "mem-red"; // 0-19 и 80-100
        }
    }
}

