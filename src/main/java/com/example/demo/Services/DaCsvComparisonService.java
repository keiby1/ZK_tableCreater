package com.example.demo.Services;

import com.example.demo.DTO.DaCompareRow;
import com.example.demo.DTO.DaCsvDocument;
import com.example.demo.DTO.DaResourceRow;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Сопоставление двух выгрузок ДА: left join по namespace, deployment, container
 * с добавлением строк, присутствующих только во втором файле.
 */
@Service
public class DaCsvComparisonService {

    public List<DaCompareRow> compare(DaCsvDocument left, DaCsvDocument right) {
        Map<String, DaResourceRow> leftByKey = indexByJoinKey(left.getRows());
        Map<String, DaResourceRow> rightByKey = indexByJoinKey(right.getRows());

        Set<String> orderedKeys = new LinkedHashSet<>();
        for (DaResourceRow row : left.getRows()) {
            orderedKeys.add(row.joinKey());
        }
        for (DaResourceRow row : right.getRows()) {
            orderedKeys.add(row.joinKey());
        }

        List<String> sortedKeys = new ArrayList<>(orderedKeys);
        sortedKeys.sort(Comparator
                .comparing((String k) -> part(k, 0), Comparator.nullsLast(String::compareTo))
                .thenComparing(k -> part(k, 1), Comparator.nullsLast(String::compareTo))
                .thenComparing(k -> part(k, 2), Comparator.nullsLast(String::compareTo)));

        List<DaCompareRow> result = new ArrayList<>();
        for (String key : sortedKeys) {
            DaResourceRow l = leftByKey.get(key);
            DaResourceRow r = rightByKey.get(key);
            DaCompareRow row = new DaCompareRow();
            row.setLeft(l);
            row.setRight(r);
            row.setNamespace(displayNamespace(l, r));
            row.setDeployment(firstNonBlank(l, r, DaResourceRow::getDeployment));
            row.setContainerName(firstNonBlank(l, r, DaResourceRow::getContainerName));
            result.add(row);
        }
        return result;
    }

    private static Map<String, DaResourceRow> indexByJoinKey(List<DaResourceRow> rows) {
        Map<String, DaResourceRow> map = new LinkedHashMap<>();
        for (DaResourceRow row : rows) {
            map.put(row.joinKey(), row);
        }
        return map;
    }

    private static String part(String key, int index) {
        String[] p = key.split("\u001e", -1);
        return index < p.length ? p[index] : "";
    }

    /**
     * Namespace из данных; если строка есть только во 2-м файле — в колонку Namespace выводится полное имя Pod.
     */
    private static String displayNamespace(DaResourceRow left, DaResourceRow right) {
        if (left != null) {
            return left.getNamespace();
        }
        if (right != null) {
            return right.getPod();
        }
        return "";
    }

    private static String firstNonBlank(
            DaResourceRow left,
            DaResourceRow right,
            java.util.function.Function<DaResourceRow, String> getter) {
        if (left != null) {
            String v = getter.apply(left);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        if (right != null) {
            String v = getter.apply(right);
            if (v != null) {
                return v;
            }
        }
        return "";
    }
}
