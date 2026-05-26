package com.example.demo.DTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Результат чтения одного CSV-файла ДА: имя источника и строки ресурсов.
 */
public class DaCsvDocument {

    private String sourceName;
    private List<DaResourceRow> rows = new ArrayList<>();

    public DaCsvDocument() {
    }

    public DaCsvDocument(String sourceName, List<DaResourceRow> rows) {
        this.sourceName = sourceName;
        this.rows = rows != null ? new ArrayList<>(rows) : new ArrayList<>();
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public List<DaResourceRow> getRows() {
        return rows;
    }

    public void setRows(List<DaResourceRow> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }
}
