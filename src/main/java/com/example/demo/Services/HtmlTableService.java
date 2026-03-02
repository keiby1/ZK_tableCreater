package com.example.demo.Services;

import com.example.demo.DTO.Container;
import com.example.demo.DTO.Deployment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HtmlTableService {

    private static final int MAX_CPU_PER_DEPLOYMENT = 4000;
    private static final int MAX_RAM_PER_DEPLOYMENT = 10000;

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
        html.append("        tr:hover {\n");
        html.append("            background-color: #f0f0f0;\n");
        html.append("        }\n");
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
        html.append("    <div class=\"legend\">\n");
        html.append("        <details>\n");
        html.append("            <summary>Расшифровка индикаторов</summary>\n");
        html.append("            <ul>\n");
        html.append("                <li><span class=\"swatch\" style=\"background:#c8e6c9\"></span> CPU/память: оптимальная утилизация (60–79%)</li>\n");
        html.append("                <li><span class=\"swatch\" style=\"background:#fff9c4\"></span> CPU/память: низкая утилизация (20–59%)</li>\n");
        html.append("                <li><span class=\"swatch\" style=\"background:#ffcdd2\"></span> CPU/память: очень низкая (0–19%)</li>\n");
        html.append("                <li><span class=\"swatch\" style=\"background:#ef9a9a\"></span> CPU/память: критическая утилизация (≥80%)</li>\n");
        html.append("                <li><span class=\"swatch\" style=\"background:#78909c\"></span> Превышен лимит деплоймента по столбцу (CPU/RAM)</li>\n");
        html.append("                <li><span class=\"swatch\" style=\"background:#c8e6c9\"></span> Время старта: до 1 мин</li>\n");
        html.append("                <li><span class=\"swatch\" style=\"background:#fff9c4\"></span> Время старта: 1–1.5 мин</li>\n");
        html.append("                <li><span class=\"swatch\" style=\"background:#ffe0b2\"></span> Время старта: 1.5–2 мин</li>\n");
        html.append("                <li><span class=\"swatch\" style=\"background:#ffcdd2\"></span> Время старта: более 2 мин</li>\n");
        html.append("            </ul>\n");
        html.append("        </details>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"interval-info\">\n");
        html.append("        Интервал выгрузки: ").append(formatInterval(from, to)).append("\n");
        html.append("    </div>\n");
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
        html.append("                <th>Время старта</th>\n");
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

            // Суммы по контейнерам деплоймента для проверки лимитов
            long deployCpuLim = 0;
            long deployCpuRq = 0;
            long deployMemLim = 0;
            long deployMemRq = 0;
            for (Container c : deployment.getContainers()) {
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
                
                // ЦПУ лимиты — закрашиваем только если сумма CpuLim по деплойменту > 4000
                appendTd(html, container.getCpuLim(), cpuLimClass);
                sumCpuLim += container.getCpuLim();
                
                // ЦПУ реквесты — закрашиваем только если сумма CpuRq по деплойменту > 4000
                appendTd(html, container.getCpuRq(), cpuRqClass);
                sumCpuRq += container.getCpuRq();
                
                // Память лимиты — закрашиваем только если сумма MemLim по деплойменту > 10000
                appendTd(html, container.getMemLim(), memLimClass);
                sumMemLim += container.getMemLim();
                
                // Память реквесты — закрашиваем только если сумма MemRq по деплойменту > 10000
                appendTd(html, container.getMemRq(), memRqClass);
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
        html.append("                <td>—</td>\n");
        html.append("            </tr>\n");
        
        html.append("        </tbody>\n");
        html.append("    </table>\n");
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
    
    private void appendTd(StringBuilder html, long value, String cssClass) {
        if (cssClass != null && !cssClass.isEmpty()) {
            html.append("                <td class=\"").append(cssClass).append("\">").append(value).append("</td>\n");
        } else {
            html.append("                <td>").append(value).append("</td>\n");
        }
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

