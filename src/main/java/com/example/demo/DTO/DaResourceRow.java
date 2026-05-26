package com.example.demo.DTO;

import java.math.BigDecimal;

/**
 * Одна строка выгрузки ДА из CSV: контейнер в поде namespace с заявками/лимитами и использованием CPU и RAM.
 */
public class DaResourceRow {

    private String namespace;
    /** Полное имя пода из CSV */
    private String pod;
    /** Имя deployment, извлечённое из {@link #pod} */
    private String deployment;
    private String containerName;

    private BigDecimal cpuRequestCores;
    private BigDecimal cpuLimitCores;
    private BigDecimal cpuUsedCores;
    private Long ramRequestBytes;
    private Long ramLimitBytes;
    private Long ramUsedBytes;

    /** Исходные значения ячеек для отображения в таблице сравнения */
    private String cpuRequestDisplay;
    private String cpuLimitDisplay;
    private String cpuUsedDisplay;
    private String ramRequestDisplay;
    private String ramLimitDisplay;
    private String ramUsedDisplay;

    private int sourceLineNumber;

    public DaResourceRow() {
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

    public String getDeployment() {
        return deployment;
    }

    public void setDeployment(String deployment) {
        this.deployment = deployment;
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

    public Long getRamRequestBytes() {
        return ramRequestBytes;
    }

    public void setRamRequestBytes(Long ramRequestBytes) {
        this.ramRequestBytes = ramRequestBytes;
    }

    public Long getRamLimitBytes() {
        return ramLimitBytes;
    }

    public void setRamLimitBytes(Long ramLimitBytes) {
        this.ramLimitBytes = ramLimitBytes;
    }

    public Long getRamUsedBytes() {
        return ramUsedBytes;
    }

    public void setRamUsedBytes(Long ramUsedBytes) {
        this.ramUsedBytes = ramUsedBytes;
    }

    public String getCpuRequestDisplay() {
        return cpuRequestDisplay;
    }

    public void setCpuRequestDisplay(String cpuRequestDisplay) {
        this.cpuRequestDisplay = cpuRequestDisplay;
    }

    public String getCpuLimitDisplay() {
        return cpuLimitDisplay;
    }

    public void setCpuLimitDisplay(String cpuLimitDisplay) {
        this.cpuLimitDisplay = cpuLimitDisplay;
    }

    public String getCpuUsedDisplay() {
        return cpuUsedDisplay;
    }

    public void setCpuUsedDisplay(String cpuUsedDisplay) {
        this.cpuUsedDisplay = cpuUsedDisplay;
    }

    public String getRamRequestDisplay() {
        return ramRequestDisplay;
    }

    public void setRamRequestDisplay(String ramRequestDisplay) {
        this.ramRequestDisplay = ramRequestDisplay;
    }

    public String getRamLimitDisplay() {
        return ramLimitDisplay;
    }

    public void setRamLimitDisplay(String ramLimitDisplay) {
        this.ramLimitDisplay = ramLimitDisplay;
    }

    public String getRamUsedDisplay() {
        return ramUsedDisplay;
    }

    public void setRamUsedDisplay(String ramUsedDisplay) {
        this.ramUsedDisplay = ramUsedDisplay;
    }

    public int getSourceLineNumber() {
        return sourceLineNumber;
    }

    public void setSourceLineNumber(int sourceLineNumber) {
        this.sourceLineNumber = sourceLineNumber;
    }

    /** Ключ сопоставления: namespace, deployment, container. */
    public String joinKey() {
        return namespace + '\u001e' + deployment + '\u001e' + containerName;
    }
}
