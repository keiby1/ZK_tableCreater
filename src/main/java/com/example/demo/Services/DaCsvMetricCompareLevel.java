package com.example.demo.Services;

/**
 * Уровень расхождения пары метрик при сравнении двух CSV ДА.
 */
public enum DaCsvMetricCompareLevel {
    MATCH("cmp-match"),
    BELOW_10("cmp-below-10"),
    BELOW_20("cmp-below-20"),
    ABOVE_20("cmp-above-20"),
    MISSING("cmp-missing");

    private final String cssClass;

    DaCsvMetricCompareLevel(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getCssClass() {
        return cssClass;
    }
}
