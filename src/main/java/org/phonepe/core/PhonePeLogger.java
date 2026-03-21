package org.phonepe.core;

import org.phonepe.config.LoggerConfig;
import org.phonepe.enums.LoggingLevel;
import org.phonepe.enums.ThreadingModel;
import org.phonepe.enums.WriteMode;
import org.phonepe.formatter.LogFormatter;
import org.phonepe.formatter.impl.DefaultLogFormatter;
import org.phonepe.message.LogMessage;
import org.phonepe.sink.Sink;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.concurrent.*;

public class PhonePeLogger {
    private final LoggerConfig loggerConfig;
    private final LogFormatter logFormatter;
    private final String hostName;

    private ExecutorService executorService;
    private BlockingQueue<Runnable> taskQueue;

    public PhonePeLogger(LoggerConfig loggerConfig) {
        this(loggerConfig, new DefaultLogFormatter(loggerConfig.getTimeStampFormat()));
    }

    public PhonePeLogger(LoggerConfig loggerconfig, LogFormatter logFormatter) {
        this.loggerConfig = loggerconfig;
        this.logFormatter = logFormatter;
        this.hostName = resolveHostName();

        if (loggerConfig.getWriteMode() == WriteMode.ASYNC) {
            initAsyncWriter();
        }
    }

    public void debug(String namespace, String content) {
        log(LoggingLevel.DEBUG, namespace, content, null);
    }

    public void info(String namespace, String content) {
        log(LoggingLevel.INFO, namespace, content, null);
    }

    public void warn(String namespace, String content) {
        log(LoggingLevel.WARN, namespace, content, null);
    }

    public void error(String namespace, String content) {
        log(LoggingLevel.ERROR, namespace, content, null);
    }

    public void fatal(String namespace, String content) {
        log(LoggingLevel.FATAL, namespace, content, null);
    }

    public void debug(String namespace, String content, String trackingId) {
        log(LoggingLevel.DEBUG, namespace, content, trackingId);
    }

    public void info(String namespace, String content, String trackingId) {
        log(LoggingLevel.INFO, namespace, content, trackingId);
    }

    public void warn(String namespace, String content, String trackingId) {
        log(LoggingLevel.WARN, namespace, content, trackingId);
    }

    public void error(String namespace, String content, String trackingId) {
        log(LoggingLevel.ERROR, namespace, content, trackingId);
    }

    public void fatal(String namespace, String content, String trackingId) {
        log(LoggingLevel.FATAL, namespace, content, trackingId);
    }

    public void log(LoggingLevel loggingLevel, String namespace, String content, String trackingId) {
        if (!loggingLevel.isAtLeast(loggerConfig.getDefaultLoggingLevel())) return;

        Sink sink = loggerConfig.getSinkForLevel(loggingLevel);
        if (sink == null) {
            throw new RuntimeException("No sink configured for log level: " + loggingLevel);
        }

        LogMessage message = LogMessage.builder()
                .level(loggingLevel)
                .namespace(namespace)
                .content(content)
                .timestamp(LocalDateTime.now())
                .hostName(hostName)
                .trackingId(trackingId)
                .build();

        String formattedMessage = logFormatter.format(message);

        if (loggerConfig.getWriteMode() == WriteMode.SYNC) {
            writeSync(sink, formattedMessage, message);
        } else {
            writeAsync(sink, formattedMessage, message);
        }
    }

    private void writeAsync(Sink sink, String formattedMessage, LogMessage message) {
        try {
            executorService.submit(() -> {
                try {
                    sink.write(formattedMessage, message);
                } catch (Exception e) {
                    System.err.println("Logger: Async write failed: " + e.getMessage());
                }
            });
        } catch (RejectedExecutionException e) {
            System.err.println("Logger: Task submission rejected: " + e.getMessage());
            writeSync(sink, formattedMessage, message);
        }
    }

    private void writeSync(Sink sink, String formattedMessage, LogMessage message) {
        sink.write(formattedMessage, message);
    }

    private void initAsyncWriter() {
        int poolSize = loggerConfig.getThreadingModel().equals(ThreadingModel.MULTI_THREADED) ? Runtime.getRuntime().availableProcessors() : 1;
        this.taskQueue = new LinkedBlockingQueue<>(100);
        this.executorService = new ThreadPoolExecutor(
                poolSize, poolSize * 2,
                60L, TimeUnit.SECONDS,
                taskQueue,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        loggerConfig.getLevelToSinkMap().values().stream()
                .distinct()
                .forEach(sink -> {
                    try {
                        sink.flush();
                        sink.close();
                    } catch (Exception e) {
                        System.err.println("Logger: Error closing sink: " + e.getMessage());
                    }
                });
    }

    private String resolveHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public void flush() {
        loggerConfig.getLevelToSinkMap().values().stream()
                .distinct()
                .forEach(Sink::flush);
    }
}
