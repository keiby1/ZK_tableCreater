package com.example.demo.Services;

import com.example.demo.Config.VictoriaMetricsConfig;
import com.example.demo.DTO.PrometheusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Низкоуровневый клиент к VictoriaMetrics (Prometheus-совместимый API).
 */
@Component
public class VictoriaMetricsClient {

    private final RestTemplate restTemplate;
    private final VictoriaMetricsConfig config;

    public VictoriaMetricsClient(@Qualifier("victoriaMetricsRestTemplate") RestTemplate victoriaMetricsRestTemplate,
                                 VictoriaMetricsConfig config) {
        this.restTemplate = victoriaMetricsRestTemplate;
        this.config = config;
    }

    /**
     * Instant-запрос: одна точка во времени.
     *
     * @param query PromQL-выражение
     * @param time  Unix-секунды (null = now)
     * @return ответ с data.result[].metric и .value
     */
    public PrometheusResponse query(String query, Long time) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(config.getBaseUrl() + "/api/v1/query")
                .queryParam("query", query);
        if (time != null) {
            builder.queryParam("time", time);
        }
        URI uri = builder.build().toUri();
        ResponseEntity<PrometheusResponse> entity = restTemplate.getForEntity(uri, PrometheusResponse.class);
        return entity.getBody();
    }

    /**
     * Range-запрос: серия точек за период.
     *
     * @param query PromQL
     * @param start start timestamp (Unix sec)
     * @param end   end timestamp (Unix sec)
     * @param step  step в секундах (например 60)
     * @return ответ с data.result[].values
     */
    public PrometheusResponse queryRange(String query, long start, long end, long step) {
        URI uri = UriComponentsBuilder
                .fromUriString(config.getBaseUrl() + "/api/v1/query_range")
                .queryParam("query", query)
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("step", step)
                .build()
                .toUri();
        ResponseEntity<PrometheusResponse> entity = restTemplate.getForEntity(uri, PrometheusResponse.class);
        return entity.getBody();
    }

    /**
     * Извлечь числовое значение из одной точки (instant) result.
     * value = [timestamp, "value"] — берётся второй элемент как double.
     */
    public static double parseValue(List<Object> value) {
        if (value == null || value.size() < 2) return Double.NaN;
        Object v = value.get(1);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return Double.parseDouble(String.valueOf(v));
    }

    /**
     * Извлечь метку из result.metric.
     */
    public static String getLabel(Map<String, String> metric, String label) {
        return metric == null ? null : metric.get(label);
    }
}
