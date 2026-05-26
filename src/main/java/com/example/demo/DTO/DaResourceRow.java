package com.example.demo.DTO;

import java.math.BigDecimal;

/**
 * Одна строка выгрузки ДА из CSV: контейнер в поде namespace с заявками/лимитами и использованием CPU и RAM.
 * <ul>
 *   <li>CPU — значения из файла в <strong>ядрах</strong> (cores).</li>
 *   <li>RAM — байты после разбора строк вида {@code 1.11 GiB}, {@code 432.123 MiB} (×1024, KiB/MiB/GiB/TiB).</li>
 * </ul>
 */
public class DaResourceRow {

    private String namespace;
    private String pod;
    private String containerName;
    private BigDecimal cpuRequestCores;
    private BigDecimal cpuLimitCores;
    private BigDecimal cpuUsedCores;
    private long ramRequestBytes;
    private long ramLimitBytes;
    private long ramUsedBytes;
    /** Номер строки в CSV (первая строка файла — заголовок имеет номер 1). */
    private int sourceLineNumber;

    public DaResourceRow() {
    }

    public DaResourceRow(
            String namespace,
            String pod,
            String containerName,
            BigDecimal cpuRequestCores,
            BigDecimal cpuLimitCores,
            BigDecimal cpuUsedCores,
            long ramRequestBytes,
            long ramLimitBytes,
            long ramUsedBytes,
            int sourceLineNumber) {
        this.namespace = namespace;
        this.pod = pod;
        this.containerName = containerName;
        this.cpuRequestCores = cpuRequestCores;
        this.cpuLimitCores = cpuLimitCores;
        this.cpuUsedCores = cpuUsedCores;
        this.ramRequestBytes = ramRequestBytes;
        this.ramLimitBytes = ramLimitBytes;
        this.ramUsedBytes = ramUsedBytes;
        this.sourceLineNumber = sourceLineNumber;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getPod() {
        return pod;
    }

    public void setPod(String pod) {
        this.pod = pod;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public BigDecimal getCpuRequestCores() {
        return cpuRequestCores;
    }

    public void setCpuRequestCores(BigDecimal cpuRequestCores) {
        this.cpuRequestCores = cpuRequestCores;
    }

    public BigDecimal getCpuLimitCores() {
        return cpuLimitCores;
    }

    public void setCpuLimitCores(BigDecimal cpuLimitCores) {
        this.cpuLimitCores = cpuLimitCores;
    }

    public BigDecimal getCpuUsedCores() {
        return cpuUsedCores;
    }

    public void setCpuUsedCores(BigDecimal cpuUsedCores) {
        this.cpuUsedCores = cpuUsedCores;
    }

    public long getRamRequestBytes() {
        return ramRequestBytes;
    }

    public void setRamRequestBytes(long ramRequestBytes) {
        this.ramRequestBytes = ramRequestBytes;
    }

    public long getRamLimitBytes() {
        return ramLimitBytes;
    }

    public void setRamLimitBytes(long ramLimitBytes) {
        this.ramLimitBytes = ramLimitBytes;
    }

    public long getRamUsedBytes() {
        return ramUsedBytes;
    }

    public void setRamUsedBytes(long ramUsedBytes) {
        this.ramUsedBytes = ramUsedBytes;
    }

    public int getSourceLineNumber() {
        return sourceLineNumber;
    }

    public void setSourceLineNumber(int sourceLineNumber) {
        this.sourceLineNumber = sourceLineNumber;
    }

    /** Ключ для сопоставления одинаковых записей между двумя файлами: namespace → pod → container. */
    public String resourceKey() {
        return namespace + '\u001e' + pod + '\u001e' + containerName;
    }
}