package com.example.demo.Services;

import com.example.demo.DTO.LinuxServerMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * HTML-страница с таблицей утилизации CPU/RAM по Linux-серверам (node_exporter).
 */
@Service
public class LinuxServersHtmlService {

    private static final DateTimeFormatter INTERVAL_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").withZone(ZoneId.of("UTC"));

    @Autowired
    private AppLayoutService appLayoutService;

    public String generatePage(List<LinuxServerMetrics> rows, Long from, Long to) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"ru\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <link rel=\"icon\" href=\"/image/leaf.png\" type=\"image/png\">\n");
        html.append("    <title>Linux-серверы — утилизация CPU и RAM</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
        html.append("        table { border-collapse: collapse; width: 100%; background: #fff; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("        th { background-color: #2e7d32; color: #fff; padding: 12px; text-align: left; border: 1px solid #ddd; }\n");
        html.append("        td { padding: 10px; border: 1px solid #ddd; text-align: center; }\n");
        html.append("        tbody tr:hover { background-color: #f0f0f0; }\n");
        html.append("        .interval-info { margin-bottom: 16px; padding: 10px 16px; background: #e8f5e9; border: 1px solid #a5d6a7; border-radius: 6px; color: #1b5e20; }\n");
        html.append("        .hint { margin-bottom: 12px; padding: 10px 14px; background: #fff8e1; border: 1px solid #ffe082; border-radius: 6px; font-size: 0.95em; color: #5d4037; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append(appLayoutService.buildAppHeader());
        html.append("    <h1 style=\"font-size:1.2em;color:#333;\">Утилизация CPU и RAM (node_exporter)</h1>\n");
        html.append("    <div class=\"interval-info\">Интервал выгрузки: ").append(formatInterval(from, to)).append("</div>\n");
        html.append("    <div class=\"hint\">Фильтр по серверам: параметр <code>instances</code> — список значений лейбла <code>instance</code> ");
        html.append("(несколько раз <code>?instances=host:9100&amp;instances=...</code> или через запятую <code>?instances=a,b,c</code>). ");
        html.append("Пустой список — все хосты с метриками в VictoriaMetrics.</div>\n");
        html.append("    <table>\n");
        html.append("        <thead><tr>\n");
        html.append("            <th>instance</th>\n");
        html.append("            <th>CPU cores</th>\n");
        html.append("            <th>RAM total</th>\n");
        html.append("            <th>Linux release</th>\n");
        html.append("            <th>CpuAvgUse %</th>\n");
        html.append("            <th>CpuMaxUse %</th>\n");
        html.append("            <th>MemAvgUse %</th>\n");
        html.append("            <th>MemMaxUse %</th>\n");
        html.append("        </tr></thead>\n");
        html.append("        <tbody>\n");
        for (LinuxServerMetrics r : rows) {
            html.append("            <tr>\n");
            html.append("                <td style=\"text-align:left\">").append(escapeHtml(r.getInstance())).append("</td>\n");
            html.append("                <td>").append(formatNullableInt(r.getCpuCores())).append("</td>\n");
            html.append("                <td style=\"text-align:left\">").append(escapeHtml(formatMemTotal(r.getMemTotalBytes()))).append("</td>\n");
            html.append("                <td style=\"text-align:left;font-size:0.92em\">")
                    .append(escapeHtml(r.getLinuxRelease() != null ? r.getLinuxRelease() : "—")).append("</td>\n");
            html.append("                <td>").append(r.getCpuAvgPercent()).append("</td>\n");
            html.append("                <td>").append(r.getCpuMaxPercent()).append("</td>\n");
            html.append("                <td>").append(r.getMemAvgPercent()).append("</td>\n");
            html.append("                <td>").append(r.getMemMaxPercent()).append("</td>\n");
            html.append("            </tr>\n");
        }
        if (rows.isEmpty()) {
            html.append("            <tr><td colspan=\"8\" style=\"padding:24px;color:#666;\">Нет данных по запросу ");
            html.append("(проверьте интервал, фильтр <code>instances</code> и наличие метрик node_exporter).</td></tr>\n");
        }
        html.append("        </tbody>\n");
        html.append("    </table>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        return html.toString();
    }

    private static String formatInterval(Long from, Long to) {
        if (from != null && to != null) {
            String fromStr = INTERVAL_FORMAT.format(Instant.ofEpochMilli(from));
            String toStr = INTERVAL_FORMAT.format(Instant.ofEpochMilli(to));
            return "с " + fromStr + " по " + toStr + " (UTC)";
        }
        return "не указан (используется окно из настроек VictoriaMetrics)";
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String formatNullableInt(Integer n) {
        return n == null ? "—" : String.valueOf(n);
    }

    /** Человекочитаемый объём RAM; в JSON остаётся {@code memTotalBytes}. */
    private static String formatMemTotal(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "—";
        }
        double gib = bytes / (1024.0 * 1024.0 * 1024.0);
        if (gib >= 100) {
            return String.format("%.0f GiB (%d B)", gib, bytes);
        }
        return String.format("%.1f GiB (%d B)", gib, bytes);
    }
}
