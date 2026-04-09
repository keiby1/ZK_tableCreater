package com.example.demo.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Утилизация CPU и RAM по данным node_exporter (сервер Linux), за выбранный интервал.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LinuxServerMetrics {
    /** Значение лейбла instance (часто host:port). */
    private String instance;
    /** Средняя утилизация CPU, % (0–100). */
    private int cpuAvgPercent;
    /** Максимальная утилизация CPU за интервал, % (0–100). */
    private int cpuMaxPercent;
    /** Средняя утилизация RAM, % (0–100). */
    private int memAvgPercent;
    /** Максимальная утилизация RAM за интервал, % (0–100). */
    private int memMaxPercent;
}
