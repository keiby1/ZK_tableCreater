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
    /** Логических ядер CPU (по числу рядов idle в {@code node_cpu_seconds_total}); null если нет в запросе. */
    private Integer cpuCores;
    /** Вся оперативная память, байты ({@code node_memory_MemTotal_bytes}); null если нет в запросе. */
    private Long memTotalBytes;
    /** Релиз ядра Linux (лейбл {@code release} метрики {@code node_uname_info}), например {@code 4.18.0-553.111.1.el9.x86_64}. */
    private String linuxRelease;
    /** Средняя утилизация CPU, % (0–100). */
    private int cpuAvgPercent;
    /** Максимальная утилизация CPU за интервал, % (0–100). */
    private int cpuMaxPercent;
    /** Средняя утилизация RAM, % (0–100). */
    private int memAvgPercent;
    /** Максимальная утилизация RAM за интервал, % (0–100). */
    private int memMaxPercent;
}
