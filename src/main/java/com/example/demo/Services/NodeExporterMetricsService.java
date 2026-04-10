package com.example.demo.Services;

import com.example.demo.Config.VictoriaMetricsConfig;
import com.example.demo.DTO.LinuxServerMetrics;
import com.example.demo.DTO.PrometheusResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Средняя и максимальная утилизация CPU и RAM для Linux-хостов по метрикам node_exporter в VictoriaMetrics.
 * Фильтр по лейблу {@code instance} (hostname:port и т.п.).
 */
@Service
public class NodeExporterMetricsService {

    private static final String LABEL_INSTANCE = "instance";

    private final VictoriaMetricsClient client;
    private final VictoriaMetricsConfig config;

    public NodeExporterMetricsService(VictoriaMetricsClient client, VictoriaMetricsConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * @param instances фильтр: только эти значения {@code instance} (OR). Пусто/null — все хосты с метриками.
     * @param fromMs    начало интервала UTC, мс; вместе с toMs задаёт окно подзапроса
     * @param toMs      конец интервала UTC, мс
     */
    public List<LinuxServerMetrics> fetchLinuxServerMetrics(List<String> instances, Long fromMs, Long toMs) {
        List<String> filter = normalizeInstances(instances);
        String range = config.getTimeRange();
        Long evaluationTimeSec = null;
        if (fromMs != null && toMs != null && toMs > fromMs) {
            long rangeSec = (toMs - fromMs) / 1000;
            range = rangeSec + "s";
            evaluationTimeSec = toMs / 1000;
        }

        String step = config.getSubqueryStep();
        String cpuWindow = config.getCpuRateWindow();

        String cpuPct = "(1 - avg by (" + LABEL_INSTANCE + ") (rate(" + cpuIdleSelector(filter) + "[" + cpuWindow + "]))) * 100";
        String qCpuAvg = "avg_over_time(" + cpuPct + "[" + range + ":" + step + "])";
        String qCpuMax = "max_over_time(" + cpuPct + "[" + range + ":" + step + "])";

        String memPct = memoryUsedPercentExpr(filter);
        String qMemAvg = "avg_over_time(" + memPct + "[" + range + ":" + step + "])";
        String qMemMax = "max_over_time(" + memPct + "[" + range + ":" + step + "])";

        Map<String, LinuxServerMetrics> byInstance = new HashMap<>();

        fillByInstance(byInstance, qCpuAvg, LinuxServerMetrics::setCpuAvgPercent, evaluationTimeSec);
        fillByInstance(byInstance, qCpuMax, LinuxServerMetrics::setCpuMaxPercent, evaluationTimeSec);
        fillByInstance(byInstance, qMemAvg, LinuxServerMetrics::setMemAvgPercent, evaluationTimeSec);
        fillByInstance(byInstance, qMemMax, LinuxServerMetrics::setMemMaxPercent, evaluationTimeSec);

        String qCpuCores = "count without (cpu, mode) (" + cpuIdleSelector(filter) + ")";
        fillNumericByInstance(byInstance, qCpuCores, evaluationTimeSec,
                (row, v) -> row.setCpuCores((int) Math.round(v)));

        String qMemTotal = memTotalSelector(filter);
        fillNumericByInstance(byInstance, qMemTotal, evaluationTimeSec,
                (row, v) -> row.setMemTotalBytes(Math.round(v)));

        String qUname = unameSelector(filter);
        fillReleaseByInstance(byInstance, qUname, evaluationTimeSec);

        String qTcpTimeWait = tcpTimeWaitQuery(filter);
        fillNumericByInstance(byInstance, qTcpTimeWait, evaluationTimeSec,
                (row, v) -> row.setTcpTimeWaitCount(Math.round(v)));

        List<LinuxServerMetrics> rows = new ArrayList<>(byInstance.values());
        rows.sort(Comparator.comparing(LinuxServerMetrics::getInstance, Comparator.nullsLast(String::compareTo)));
        return rows;
    }

    /** Поддержка {@code ?instances=a&instances=b} и {@code ?instances=a,b,c}. */
    public static List<String> normalizeInstances(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            if (s.contains(",")) {
                for (String part : s.split(",")) {
                    String t = part.trim();
                    if (!t.isEmpty()) {
                        String normalized = normalizeInstanceToken(t);
                        if (normalized != null) {
                            out.add(normalized);
                        }
                    }
                }
            } else {
                String normalized = normalizeInstanceToken(s.trim());
                if (normalized != null) {
                    out.add(normalized);
                }
            }
        }
        return out;
    }

    /**
     * Нормализация значения фильтра по лейблу {@code instance}.
     * <p>
     * В этом проекте на стороне экспортеров настроено переопределение instance перед отправкой в VictoriaMetrics,
     * поэтому в метриках {@code instance} хранится без порта (например {@code host1}).
     * </p>
     */
    private static String normalizeInstanceToken(String token) {
        if (token == null) {
            return null;
        }
        String t = token.trim();
        if (t.isEmpty()) {
            return null;
        }
        // На всякий случай поддержим "http(s)://host:port/..." из копипаста.
        if (t.startsWith("http://")) {
            t = t.substring("http://".length());
        } else if (t.startsWith("https://")) {
            t = t.substring("https://".length());
        }
        int slash = t.indexOf('/');
        if (slash >= 0) {
            t = t.substring(0, slash);
        }
        t = t.trim();
        if (t.isEmpty()) {
            return null;
        }
        // Если прилетело host:port, а в VM instance хранится без порта — отрежем порт.
        // Ограничение: IPv6-адреса с ':' тут не нормализуем (оставляем как есть).
        int firstColon = t.indexOf(':');
        int lastColon = t.lastIndexOf(':');
        if (firstColon >= 0 && firstColon == lastColon && lastColon < t.length() - 1) {
            String host = t.substring(0, lastColon).trim();
            String maybePort = t.substring(lastColon + 1).trim();
            if (!host.isEmpty() && maybePort.matches("\\d+")) {
                t = host;
            }
        }
        return t;
    }

    private static String cpuIdleSelector(List<String> instances) {
        StringBuilder sb = new StringBuilder("node_cpu_seconds_total{mode=\"idle\"");
        if (!instances.isEmpty()) {
            sb.append(",").append(LABEL_INSTANCE).append("=~\"").append(instanceRegexOr(instances)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String memoryUsedPercentExpr(List<String> instances) {
        if (instances.isEmpty()) {
            return "(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100";
        }
        String br = "{" + LABEL_INSTANCE + "=~\"" + instanceRegexOr(instances) + "\"}";
        return "(1 - (node_memory_MemAvailable_bytes" + br + " / node_memory_MemTotal_bytes" + br + ")) * 100";
    }

    private static String memTotalSelector(List<String> instances) {
        if (instances.isEmpty()) {
            return "node_memory_MemTotal_bytes";
        }
        String br = "{" + LABEL_INSTANCE + "=~\"" + instanceRegexOr(instances) + "\"}";
        return "node_memory_MemTotal_bytes" + br;
    }

    private static String unameSelector(List<String> instances) {
        if (instances.isEmpty()) {
            return "node_uname_info";
        }
        return "node_uname_info{" + LABEL_INSTANCE + "=~\"" + instanceRegexOr(instances) + "\"}";
    }

    /**
     * Количество сокетов в TIME_WAIT; {@code sum by (instance)} на случай нескольких рядов (например разные наборы лейблов).
     */
    private static String tcpTimeWaitQuery(List<String> instances) {
        StringBuilder inner = new StringBuilder("node_tcp_connection_states{state=\"time_wait\"");
        if (!instances.isEmpty()) {
            inner.append(",").append(LABEL_INSTANCE).append("=~\"").append(instanceRegexOr(instances)).append("\"");
        }
        inner.append("}");
        return "sum by (" + LABEL_INSTANCE + ") (" + inner + ")";
    }

    private static String instanceRegexOr(List<String> instances) {
        return instances.stream().map(Pattern::quote).collect(Collectors.joining("|"));
    }

    private void fillByInstance(Map<String, LinuxServerMetrics> byInstance, String query,
                                MetricSetter setter, Long evaluationTimeSec) {
        PrometheusResponse resp = client.query(query, evaluationTimeSec);
        if (resp == null || resp.getData() == null || resp.getData().getResult() == null) {
            return;
        }
        for (PrometheusResponse.Result r : resp.getData().getResult()) {
            String inst = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_INSTANCE);
            if (inst == null || inst.isEmpty()) {
                continue;
            }
            double v = VictoriaMetricsClient.parseValue(r.getValue());
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                continue;
            }
            int pct = (int) Math.round(Math.max(0, Math.min(100, v)));
            LinuxServerMetrics row = byInstance.computeIfAbsent(inst, i -> {
                LinuxServerMetrics m = new LinuxServerMetrics();
                m.setInstance(i);
                return m;
            });
            setter.set(row, pct);
        }
    }

    private void fillNumericByInstance(Map<String, LinuxServerMetrics> byInstance, String query, Long evaluationTimeSec,
                                       BiConsumer<LinuxServerMetrics, Double> setter) {
        PrometheusResponse resp = client.query(query, evaluationTimeSec);
        if (resp == null || resp.getData() == null || resp.getData().getResult() == null) {
            return;
        }
        for (PrometheusResponse.Result r : resp.getData().getResult()) {
            String inst = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_INSTANCE);
            if (inst == null || inst.isEmpty()) {
                continue;
            }
            double v = VictoriaMetricsClient.parseValue(r.getValue());
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                continue;
            }
            LinuxServerMetrics row = byInstance.computeIfAbsent(inst, i -> {
                LinuxServerMetrics m = new LinuxServerMetrics();
                m.setInstance(i);
                return m;
            });
            setter.accept(row, v);
        }
    }

    private void fillReleaseByInstance(Map<String, LinuxServerMetrics> byInstance, String query, Long evaluationTimeSec) {
        PrometheusResponse resp = client.query(query, evaluationTimeSec);
        if (resp == null || resp.getData() == null || resp.getData().getResult() == null) {
            return;
        }
        for (PrometheusResponse.Result r : resp.getData().getResult()) {
            String inst = VictoriaMetricsClient.getLabel(r.getMetric(), LABEL_INSTANCE);
            if (inst == null || inst.isEmpty()) {
                continue;
            }
            String release = VictoriaMetricsClient.getLabel(r.getMetric(), "release");
            if (release == null || release.isEmpty()) {
                continue;
            }
            LinuxServerMetrics row = byInstance.computeIfAbsent(inst, i -> {
                LinuxServerMetrics m = new LinuxServerMetrics();
                m.setInstance(i);
                return m;
            });
            row.setLinuxRelease(release);
        }
    }

    @FunctionalInterface
    private interface MetricSetter {
        void set(LinuxServerMetrics m, int percent);
    }
}
