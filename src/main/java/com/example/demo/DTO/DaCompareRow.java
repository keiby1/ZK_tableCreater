package com.example.demo.DTO;

/**
 * Строка результата left join двух выгрузок ДА.
 */
public class DaCompareRow {

    private String namespace;
    /** Оригинальное имя Pod из CSV */
    private String pod;
    /** Имя deployment, извлечённое из Pod (без суффикса ReplicaSet) */
    private String deployment;
    private String containerName;
    private DaResourceRow left;
    private DaResourceRow right;

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

    public DaResourceRow getLeft() {
        return left;
    }

    public void setLeft(DaResourceRow left) {
        this.left = left;
    }

    public DaResourceRow getRight() {
        return right;
    }

    public void setRight(DaResourceRow right) {
        this.right = right;
    }
}
