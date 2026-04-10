package com.example.demo.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Метрики по запросу на PostgreSQL (агрегация avg/max за интервал).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostgresQueryMetrics {
    /** Сервер БД (лейбл {@code server}). */
    private String server;
    /** Запрос (лейбл {@code query} / {@code queryid} / fallback). */
    private String query;
    /** Среднее количество запросов за интервал. */
    private double avgCount;
    /** Максимум количества запросов за интервал. */
    private double maxCount;
}

