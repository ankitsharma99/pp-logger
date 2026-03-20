package org.phonepe.config;

import lombok.Data;
import org.phonepe.enums.LoggingLevel;
import org.phonepe.enums.SinkType;
import org.phonepe.enums.ThreadingModel;
import org.phonepe.enums.WriteMode;
import org.phonepe.sink.Sink;
import org.phonepe.sink.SinkFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class LoggerConfig {
    private final String timeStampFormat;
    private final LoggingLevel defaultLoggingLevel;
    private final ThreadingModel threadingModel;
    private final WriteMode writeMode;
    private final Map<LoggingLevel, Sink> levelToSinkMap;

    private LoggerConfig(Builder builder) {
        this.timeStampFormat = builder.timeStampFormat;
        this.defaultLoggingLevel = builder.defaultLoggingLevel;
        this.threadingModel = builder.threadingModel;
        this.writeMode = builder.writeMode;
        this.levelToSinkMap = Collections.unmodifiableMap(builder.levelToSinkMap);
    }

    public Sink getSinkForLevel(LoggingLevel level) {
        return this.levelToSinkMap.get(level);
    }

    public Map<LoggingLevel, Sink> getLoggingLevelToSinkMap() {
        return this.levelToSinkMap;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String timeStampFormat = "yyyy-MM-dd HH:mm:ss,SSS";
        private LoggingLevel defaultLoggingLevel = LoggingLevel.INFO;
        private ThreadingModel threadingModel = ThreadingModel.SINGLE_THREADED;
        private WriteMode writeMode = WriteMode.SYNC;
        private final Map<LoggingLevel, Sink> levelToSinkMap = new java.util.HashMap<>();

        public Builder timeStampFormat(String timeStampFormat) {
            this.timeStampFormat = timeStampFormat;
            return this;
        }

        public Builder defaultLoggingLevel(LoggingLevel defaultLoggingLevel) {
            this.defaultLoggingLevel = defaultLoggingLevel;
            return this;
        }

        public Builder threadingModel(ThreadingModel threadingModel) {
            this.threadingModel = threadingModel;
            return this;
        }

        public Builder writeMode(WriteMode writeMode) {
            this.writeMode = writeMode;
            return this;
        }

        public Builder addSinkForLevel(LoggingLevel level, Sink sink) {
            this.levelToSinkMap.put(level, sink);
            return this;
        }

        public Builder addSinkMapping(LoggingLevel level, SinkType sinkType, Map<String, String> properties) {
            Sink sink = SinkFactory.createSink(sinkType, properties);
            this.levelToSinkMap.put(level, sink);
            return this;
        }

        public Builder addSinkMapping(List<LoggingLevel> levels, SinkType sinkType, Map<String, String> properties) {
            Sink sink = SinkFactory.createSink(sinkType, properties);
            for (LoggingLevel level : levels) {
                this.levelToSinkMap.put(level, sink);
            }
            return this;
        }

        public Builder addSinkMapping(List<LoggingLevel> levels, Sink sink) {
            for (LoggingLevel level : levels) {
                this.levelToSinkMap.put(level, sink);
            }
            return this;
        }

        public LoggerConfig build() {
            if (this.levelToSinkMap.isEmpty()) {
                throw new IllegalStateException("At least one log level to sink mapping must be provided");
            }
            return new LoggerConfig(this);
        }
    }

}
