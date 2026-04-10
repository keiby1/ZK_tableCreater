package com.example.demo.Services;

import com.example.demo.DTO.PostgresQueryMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * HTML-страница выгрузки проверок БД (PostgreSQL).
 */
@Service
public class PostgresDbChecksHtmlService {

    // ТЗ: верхняя граница допустимых запросов задаётся в коде и отображается в UI.
    // Значение сравнивается с avg/max и влияет на подсветку.
    private static final double MAX_ALLOWED_QUERIES = 100.0;

    private static final DateTimeFormatter INTERVAL_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").withZone(ZoneId.of("UTC"));

    @Autowired
    private AppLayoutService appLayoutService;

    public String generatePage(List<PostgresQueryMetrics> rows, List<String> servers, String job, Long from, Long to) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"ru\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <link rel=\"icon\" href=\"/image/leaf.png\" type=\"image/png\">\n");
        html.append("    <title>Проверки БД (PostgreSQL)</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
        html.append("        table { border-collapse: collapse; width: 100%; background: #fff; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("        th { background-color: #2e7d32; color: #fff; padding: 12px; text-align: left; border: 1px solid #ddd; }\n");
        html.append("        td { padding: 10px; border: 1px solid #ddd; text-align: left; vertical-align: top; }\n");
        html.append("        tbody tr:hover { background-color: #f0f0f0; }\n");
        html.append("        .interval-info { margin-bottom: 16px; padding: 10px 16px; background: #e8f5e9; border: 1px solid #a5d6a7; border-radius: 6px; color: #1b5e20; }\n");
        html.append("        .hint { margin-bottom: 12px; padding: 10px 14px; background: #fff8e1; border: 1px solid #ffe082; border-radius: 6px; font-size: 0.95em; color: #5d4037; }\n");
        html.append("        .q-green { background-color: #c8e6c9; }\n");
        html.append("        .q-red { background-color: #ffcdd2; }\n");
        html.append("        .mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, \"Liberation Mono\", \"Courier New\", monospace; white-space: pre-wrap; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append(appLayoutService.buildAppHeader());
        html.append("    <h1 style=\"font-size:1.2em;color:#333;\">Проверки БД (PostgreSQL)</h1>\n");
        html.append("    <div class=\"interval-info\">Интервал выгрузки: ").append(formatInterval(from, to)).append("</div>\n");
        html.append("    <div class=\"hint\">Порог допустимых запросов: <b>").append(formatNumber(MAX_ALLOWED_QUERIES)).append("</b>. ");
        html.append("Если значение больше порога — ячейка красная, иначе зелёная. ");
        html.append("Фильтр: <code>var-server</code>/<code>servers</code>");
        if (job != null && !job.isBlank()) {
            html.append(", <code>job</code>=").append(escapeHtml(job.trim()));
        }
        if (servers != null && !servers.isEmpty()) {
            html.append(". Серверов в фильтре: ").append(servers.size());
        }
        html.append(".</div>\n");

        html.append("    <table>\n");
        html.append("        <thead><tr>\n");
        html.append("            <th>Сервер БД</th>\n");
        html.append("            <th>Запрос</th>\n");
        html.append("            <th>Среднее кол-во</th>\n");
        html.append("            <th>Макс. кол-во</th>\n");
        html.append("        </tr></thead>\n");
        html.append("        <tbody>\n");
        for (PostgresQueryMetrics r : rows) {
            String avgCls = r.getAvgCount() > MAX_ALLOWED_QUERIES ? "q-red" : "q-green";
            String maxCls = r.getMaxCount() > MAX_ALLOWED_QUERIES ? "q-red" : "q-green";
            html.append("            <tr>\n");
            html.append("                <td>").append(escapeHtml(nvl(r.getServer()))).append("</td>\n");
            html.append("                <td class=\"mono\">").append(escapeHtml(nvl(r.getQuery()))).append("</td>\n");
            html.append("                <td class=\"").append(avgCls).append("\">").append(formatNumber(r.getAvgCount())).append("</td>\n");
            html.append("                <td class=\"").append(maxCls).append("\">").append(formatNumber(r.getMaxCount())).append("</td>\n");
            html.append("            </tr>\n");
        }
        if (rows.isEmpty()) {
            html.append("            <tr><td colspan=\"4\" style=\"padding:24px;color:#666;\">Нет данных по запросу ");
            html.append("(проверьте интервал, фильтр серверов и наличие метрики pg_stat_database_tup_returned).</td></tr>\n");
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

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String formatNumber(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return "—";
        }
        if (Math.abs(v - Math.rint(v)) < 1e-9) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format("%.2f", v);
    }
}

