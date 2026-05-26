package com.example.demo.Services;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Имя пода в выгрузке ДА: {@code <deployment>-<10 символов>-<5 символов>}.
 * Суффикс ReplicaSet/пода отбрасывается, в поле deployment сохраняется префикс.
 */
public final class DaPodNameParser {

    /** Суффикс: дефис, 10 букв/цифр, дефис, 5 букв/цифр в конце строки. */
    private static final Pattern POD_WITH_REPLICA_SUFFIX =
            Pattern.compile("^(.+)-([A-Za-z0-9]{10})-([A-Za-z0-9]{5})$");

    private DaPodNameParser() {
    }

    /**
     * @param pod полное имя пода из CSV
     * @return имя deployment; если суффикс не распознан — возвращается {@code pod} целиком
     */
    public static String extractDeployment(String pod) {
        if (pod == null || pod.isBlank()) {
            return pod;
        }
        Matcher m = POD_WITH_REPLICA_SUFFIX.matcher(pod.trim());
        if (m.matches()) {
            return m.group(1);
        }
        return pod.trim();
    }
}
