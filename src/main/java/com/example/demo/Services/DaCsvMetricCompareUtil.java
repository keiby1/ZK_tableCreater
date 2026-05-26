package com.example.demo.Services;

import java.math.BigDecimal;

public final class DaCsvMetricCompareUtil {

    private DaCsvMetricCompareUtil() {
    }

    public static DaCsvMetricCompareLevel compare(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return DaCsvMetricCompareLevel.MISSING;
        }
        if (left.compareTo(right) == 0) {
            return DaCsvMetricCompareLevel.MATCH;
        }
        return comparePercent(left.doubleValue(), right.doubleValue());
    }

    public static DaCsvMetricCompareLevel compare(Long left, Long right) {
        if (left == null || right == null) {
            return DaCsvMetricCompareLevel.MISSING;
        }
        if (left.equals(right)) {
            return DaCsvMetricCompareLevel.MATCH;
        }
        return comparePercent(left.doubleValue(), right.doubleValue());
    }

    public static boolean isMissingDisplay(String display) {
        if (display == null) {
            return true;
        }
        String s = display.trim();
        return s.isEmpty() || "-".equals(s);
    }

    public static DaCsvMetricCompareLevel compareDisplayPair(String leftDisplay, String rightDisplay) {
        if (isMissingDisplay(leftDisplay) || isMissingDisplay(rightDisplay)) {
            return DaCsvMetricCompareLevel.MISSING;
        }
        if (leftDisplay.trim().equals(rightDisplay.trim())) {
            return DaCsvMetricCompareLevel.MATCH;
        }
        return DaCsvMetricCompareLevel.ABOVE_20;
    }

    private static DaCsvMetricCompareLevel comparePercent(double a, double b) {
        double diff = Math.abs(a - b);
        double ref = Math.max(Math.abs(a), Math.abs(b));
        if (ref == 0.0) {
            return DaCsvMetricCompareLevel.MATCH;
        }
        double pct = diff / ref * 100.0;
        if (pct < 10.0) {
            return DaCsvMetricCompareLevel.BELOW_10;
        }
        if (pct < 20.0) {
            return DaCsvMetricCompareLevel.BELOW_20;
        }
        return DaCsvMetricCompareLevel.ABOVE_20;
    }
}
