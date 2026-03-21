package org.phonepe.formatter.impl;

import org.phonepe.formatter.LogFormatter;
import org.phonepe.message.LogMessage;

import java.time.format.DateTimeFormatter;

public class DefaultLogFormatter implements LogFormatter {
    private final DateTimeFormatter dateTimeFormatter;

    public DefaultLogFormatter(String dateTimeFormatter) {
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormatter);
    }

    public DefaultLogFormatter() {
        this("yyyy-MM-dd HH:mm:ss,SSS");
    }

    @Override
    public String format(LogMessage logMessage) {
        if (logMessage == null) {
            throw new IllegalArgumentException("LogMessage cannot be null");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(logMessage.getLevel().name());

        sb.append(" [").append(logMessage.getTimestamp().format(dateTimeFormatter)).append("] ");

        if (logMessage.getHostName() != null && !logMessage.getHostName().isBlank()) {
            sb.append("[").append(logMessage.getHostName()).append("] ");
        }

        if (logMessage.getTrackingId() != null && !logMessage.getTrackingId().isBlank()) {
            sb.append("[").append(logMessage.getTrackingId()).append("] ");
        }

        if (logMessage.getNamespace() != null && !logMessage.getNamespace().isBlank()) {
            sb.append("[").append(logMessage.getNamespace()).append("] ");
        }

        sb.append(logMessage.getContent());

        return sb.toString();
    }
}
