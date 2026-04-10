package com.example.demo.Services;

import com.example.demo.Config.VictoriaMetricsConfig;
import com.example.demo.DTO.PostgresQueryMetrics;
import com.example.demo.DTO.PrometheusResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Проверки БД (PostgreSQL) по метрикам из VictoriaMetrics.
 *
 * Ожидается, что лейбл {@code server} содержит имя сервера БД (без порта).
 * Для "Запрос" берём лейбл {@code query} (если есть), иначе {@code queryid}, иначе {@code datname}.
 */
@Service
public class PostgresDbChecksMetricsService {

    private static final String LABEL_SERVER = "server";
    private static final String LABEL_JOB = "job";

    private final VictoriaMetricsClient client;
    private final VictoriaMetricsConfig config;

    public PostgresDbChecksMetricsService(VictoriaMetricsClient client, VictoriaMetricsConfig config) {
        this.client = client;
        this.config = config;
    }

    public List<PostgresQueryMetrics> fetchQueryMetrics(List<String> servers, String job, Long fromMs, Long toMs) {
        List<String> serverFilter = normalizeTokens(servers);
        String range = config.getTimeRange();
        Long evaluationTimeSec = null;
        if (fromMs != null && toMs != null && toMs > fromMs) {
            long rangeSec = (toMs - fromMs) / 1000;
            range = rangeSec + "s";
            evaluationTimeSec = toMs / 1000;
        }
        String step = config.getSubqueryStep();

        // NOTE: метрика из запроса пользователя; интерпретируем как "количество/скорость" и агрегируем avg/max по интервалу.
        String rateExpr = "rate(" + pgTupReturnedSelector(serverFilter, job) + "[5m])";
        String qAvg = "avg_over_time(" + rateExpr + "[" + range + ":" + step + "])";
        String qMax = "max_over_time(" + rateExpr + "[" + range + ":" + step + "])";

        Map<String, PostgresQueryMetrics> byKey = new HashMap<>();
        fill(byKey, qAvg, (row, v) -> row.setAvgCount(v), evaluationTimeSec);
        fill(byKey, qMax, (row, v) -> row.setMaxCount(v), evaluationTimeSec);

        List<PostgresQueryMetrics> out = new ArrayList<>(byKey.values());
        out.sort(Comparator
                .comparing(PostgresQueryMetrics::getServer, Comparator.nullsLast(String::compareTo))
                .thenComparing(PostgresQueryMetrics::getQuery, Comparator.nullsLast(String::compareTo)));
        return out;
    }

    private static String pgTupReturnedSelector(List<String> servers, String job) {
        StringBuilder sb = new StringBuilder("pg_stat_database_tup_returned{");
        boolean needComma = false;
        if (job != null && !job.isBlank()) {
            sb.append(LABEL_JOB).append("=~\"").append(Pattern.quote(job.trim())).append("\"");
            needComma = true;
        }
        if (!servers.isEmpty()) {
            if (needComma) sb.append(",");
            sb.append(LABEL_SERVER).append("=~\"").append(regexOr(servers)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private void fill(Map<String, PostgresQueryMetrics> byKey, String query, MetricSetter setter, Long evaluationTimeSec) {
        PrometheusResponse resp = client.query(query, evaluationTimeSec);
        if (resp == null || resp.getData() == null || resp.getData().getResult() == null) {
            return;
        }
        for (PrometheusResponse.Result r : resp.getData().getResult()) {
            Map<String, String> metric = r.getMetric();
            String server = VictoriaMetricsClient.getLabel(metric, LABEL_SERVER);
            if (server == null || server.isBlank()) {
                continue;
            }
            String queryLabel = pickQueryLabel(metric);
            if (queryLabel == null || queryLabel.isBlank()) {
                continue;
            }
            double v = VictoriaMetricsClient.parseValue(r.getValue());
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                continue;
            }
            String key = server + "\n" + queryLabel;
            PostgresQueryMetrics row = byKey.computeIfAbsent(key, k -> {
                PostgresQueryMetrics m = new PostgresQueryMetrics();
                m.setServer(server);
                m.setQuery(queryLabel);
                m.setAvgCount(0);
                m.setMaxCount(0);
                return m;
            });
            setter.set(row, v);
        }
    }

    private static String pickQueryLabel(Map<String, String> metric) {
        String q = VictoriaMetricsClient.getLabel(metric, "query");
        if (q != null && !q.isBlank()) return q;
        q = VictoriaMetricsClient.getLabel(metric, "queryid");
        if (q != null && !q.isBlank()) return q;
        q = VictoriaMetricsClient.getLabel(metric, "datname");
        if (q != null && !q.isBlank()) return q;
        return null;
    }

    private static List<String> normalizeTokens(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String s : raw) {
            if (s == null || s.isBlank()) continue;
            if (s.contains(",")) {
                for (String part : s.split(",")) {
                    String t = normalizeToken(part);
                    if (t != null) out.add(t);
                }
            } else {
                String t = normalizeToken(s);
                if (t != null) out.add(t);
            }
        }
        return out;
    }

    private static String normalizeToken(String token) {
        if (token == null) return null;
        String t = token.trim();
        if (t.isEmpty()) return null;
        // На всякий случай: отрежем URL-части и числовой порт (как в server=host:5432).
        if (t.startsWith("http://")) t = t.substring("http://".length());
        if (t.startsWith("https://")) t = t.substring("https://".length());
        int slash = t.indexOf('/');
        if (slash >= 0) t = t.substring(0, slash);
        t = t.trim();
        int firstColon = t.indexOf(':');
        int lastColon = t.lastIndexOf(':');
        if (firstColon >= 0 && firstColon == lastColon && lastColon < t.length() - 1) {
            String host = t.substring(0, lastColon).trim();
            String maybePort = t.substring(lastColon + 1).trim();
            if (!host.isEmpty() && maybePort.matches("\\d+")) {
                t = host;
            }
        }
        return t.isEmpty() ? null : t;
    }

    private static String regexOr(List<String> tokens) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) sb.append("|");
            sb.append(Pattern.quote(tokens.get(i)));
        }
        return sb.toString();
    }

    @FunctionalInterface
    private interface MetricSetter {
        void set(PostgresQueryMetrics m, double value);
    }
}

