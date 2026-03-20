package org.phonepe.enums;

public enum LoggingLevel {
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    FATAL(5);

    private final int severity;

    LoggingLevel(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }

    public boolean isAtLeast(LoggingLevel threshold) {
        return this.severity >= threshold.severity;
    }
}
