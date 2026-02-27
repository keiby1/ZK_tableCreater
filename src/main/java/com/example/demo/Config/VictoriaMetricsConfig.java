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
}
