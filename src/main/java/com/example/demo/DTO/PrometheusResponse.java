package com.example.demo.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Ответ Prometheus / VictoriaMetrics API: /api/v1/query или /api/v1/query_range.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrometheusResponse {

    private String status;
    private Data data;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String resultType;
        private List<Result> result;

        public String getResultType() { return resultType; }
        public void setResultType(String resultType) { this.resultType = resultType; }
        public List<Result> getResult() { return result; }
        public void setResult(List<Result> result) { this.result = result; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private Map<String, String> metric;
        /** Одна точка для instant-запроса: [timestamp_sec, "value"] */
        private List<Object> value;
        /** Серия точек для range-запроса */
        private List<List<Object>> values;

        public Map<String, String> getMetric() { return metric; }
        public void setMetric(Map<String, String> metric) { this.metric = metric; }
        public List<Object> getValue() { return value; }
        public void setValue(List<Object> value) { this.value = value; }
        public List<List<Object>> getValues() { return values; }
        public void setValues(List<List<Object>> values) { this.values = values; }
    }
}
