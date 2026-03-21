package org.phonepe.message;

import lombok.*;
import org.phonepe.enums.LoggingLevel;

import java.time.LocalDateTime;

@Getter
public class LogMessage {
    private final LoggingLevel level;
    private final String namespace;
    private final String content;
    private final String trackingId;
    private final String hostName;
    private final LocalDateTime timestamp;

    private LogMessage(Builder builder) {
        this.level = builder.level;
        this.namespace = builder.namespace;
        this.content = builder.content;
        this.trackingId = builder.trackingId;
        this.hostName = builder.hostName;
        this.timestamp = builder.timestamp;
    }

    @Override
    public String toString() {
        return String.format("%s [%s] [%s] %s", level, timestamp, namespace, content);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LoggingLevel level;
        private String namespace;
        private String content;
        private LocalDateTime timestamp;
        private String trackingId;
        private String hostName;

        public Builder level(LoggingLevel level) {
            this.level = level;
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder trackingId(String trackingId) {
            this.trackingId = trackingId;
            return this;
        }

        public Builder hostName(String hostName) {
            this.hostName = hostName;
            return this;
        }

        public LogMessage build() {
            if (level == null) throw new IllegalArgumentException("Log level is required");
            if (content == null || content.isBlank()) throw new IllegalArgumentException("Content cannot be empty");
            if (namespace == null || namespace.isBlank()) throw new IllegalArgumentException("Namespace is required");
            if (timestamp == null) {
                timestamp = LocalDateTime.now();
            }
            return new LogMessage(this);
        }
    }
}
