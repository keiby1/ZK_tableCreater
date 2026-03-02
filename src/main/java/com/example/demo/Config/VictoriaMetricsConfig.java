package com.example.demo.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class VictoriaMetricsConfig {

    @Value("${victoriametrics.base-url:http://localhost:8428}")
    private String baseUrl;

    @Value("${victoriametrics.time-range:1h}")
    private String timeRange;

    @Value("${victoriametrics.query-timeout-sec:30}")
    private int queryTimeoutSec;

    /** Шаг в подзапросах avg/max_over_time (например 1m), чтобы совпадать с Grafana. */
    @Value("${victoriametrics.subquery-step:1m}")
    private String subqueryStep;

    /** Окно для rate(container_cpu_...[X]), например 5m. */
    @Value("${victoriametrics.cpu-rate-window:5m}")
    private String cpuRateWindow;

    /** average_per_pod = среднее от процентов по подам; sum_then_percent = sum(usage)/sum(limit) как в Grafana. */
    @Value("${victoriametrics.aggregation-method:sum_then_percent}")
    private String aggregationMethod;

    @Bean
    public RestTemplate victoriaMetricsRestTemplate() {
        return new RestTemplate();
    }

    public String getBaseUrl() {
        return baseUrl.replaceAll("/$", "");
    }

    public String getTimeRange() {
        return timeRange;
    }

    public Duration getQueryTimeout() {
        return Duration.ofSeconds(queryTimeoutSec);
    }

    public String getSubqueryStep() {
        return subqueryStep;
    }

    public String getCpuRateWindow() {
        return cpuRateWindow;
    }

    public String getAggregationMethod() {
        return aggregationMethod;
    }
}
