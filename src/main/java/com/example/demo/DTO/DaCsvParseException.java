package com.example.demo.DTO;

/**
 * Ошибка разбора CSV ДА (неверный заголовок, число столбцов, формат числа памяти и т.д.).
 */
public class DaCsvParseException extends RuntimeException {

    private final String sourceName;
    private final int lineNumber;

    public DaCsvParseException(String sourceName, int lineNumber, String message) {
        super(formatMessage(sourceName, lineNumber, message));
        this.sourceName = sourceName;
        this.lineNumber = lineNumber;
    }

    public DaCsvParseException(String sourceName, int lineNumber, String message, Throwable cause) {
        super(formatMessage(sourceName, lineNumber, message), cause);
        this.sourceName = sourceName;
        this.lineNumber = lineNumber;
    }

    public String getSourceName() {
        return sourceName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    private static String formatMessage(String sourceName, int lineNumber, String message) {
        String who = sourceName != null && !sourceName.isBlank() ? sourceName : "файл";
        return who + ", строка " + lineNumber + ": " + message;
    }
}
