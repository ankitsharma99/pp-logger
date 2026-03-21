package org.phonepe;


import org.phonepe.config.LoggerConfig;
import org.phonepe.config.LoggerConfigParser;
import org.phonepe.core.PhonePeLogger;
import org.phonepe.enums.LoggingLevel;
import org.phonepe.enums.SinkType;
import org.phonepe.enums.ThreadingModel;
import org.phonepe.enums.WriteMode;
import org.phonepe.sink.impl.DatabaseSink;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoggerDemo {
    public static void main(String[] args) {
        basicConsoleLogging();

        fileLoggingWithRotation();

        multipleSinkMappings();

        multiThreadedLogging();

        configParsingSimple();

        configParsingMultiple();

        logLevelFiltering();

        writeModeSyncAndMultiThreaded();

        writeModeAsyncAndSingleThreaded();

        customSinkExample();
    }

    private static void customSinkExample() {
        System.out.println("=== Custom Sink Example ===");

        DatabaseSink customDbSink = new DatabaseSink();
        customDbSink.init(Map.of());

        LoggerConfig loggerConfig = LoggerConfig.builder()
                .timeStampFormat("yyyy-MM-dd HH:mm:ss,SSS")
                .defaultLoggingLevel(LoggingLevel.DEBUG)
                .threadingModel(ThreadingModel.SINGLE_THREADED)
                .writeMode(WriteMode.SYNC)
                .addSinkMapping(List.of(LoggingLevel.DEBUG, LoggingLevel.INFO), customDbSink)
                .build();

        PhonePeLogger logger = new PhonePeLogger(loggerConfig);
        logger.debug("com.app.custom", "This is a debug message to custom DB sink");
        logger.info("com.app.custom", "This is an info message to custom DB sink");
        logger.shutdown();
    }

    private static void writeModeAsyncAndSingleThreaded() {
        System.out.println("=== Write Mode ASYNC with Single-threaded Logging Example ===");

        String logDir = "logger-demo/async-single-threaded-logs";
        cleanDirectory(logDir);

        LoggerConfig loggerConfig = LoggerConfig.builder()
                .timeStampFormat("yyyy-MM-dd HH:mm:ss,SSS")
                .defaultLoggingLevel(LoggingLevel.DEBUG)
                .threadingModel(ThreadingModel.SINGLE_THREADED)
                .writeMode(WriteMode.ASYNC)
                .addSinkMapping(List.of(LoggingLevel.DEBUG,
                                LoggingLevel.INFO,
                                LoggingLevel.WARN,
                                LoggingLevel.ERROR,
                                LoggingLevel.FATAL),
                        SinkType.CONSOLE_SINK, Map.of())
                .build();

        PhonePeLogger logger = new PhonePeLogger(loggerConfig);
        logger.info("com.app.main", "Starting single-threaded logging test with ASYNC write mode");

        for (int i = 0; i < 20; i++) {
            logger.debug("com.app.main", "Message " + i + " from main thread");
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        logger.shutdown();
        System.out.println("=== End of Write Mode ASYNC with Single-threaded Logging Example ===");
    }


    private static void writeModeSyncAndMultiThreaded() {
        System.out.println("=== Write Mode SYNC with Multi-threaded Logging Example ===");

        String logDir = "logger-demo/sync-multi-threaded-logs";
        cleanDirectory(logDir);

        LoggerConfig loggerConfig = LoggerConfig.builder()
                .timeStampFormat("yyyy-MM-dd HH:mm:ss,SSS")
                .defaultLoggingLevel(LoggingLevel.DEBUG)
                .threadingModel(ThreadingModel.MULTI_THREADED)
                .writeMode(WriteMode.SYNC)
                .addSinkMapping(List.of(LoggingLevel.DEBUG,
                                LoggingLevel.INFO,
                                LoggingLevel.WARN,
                                LoggingLevel.ERROR,
                                LoggingLevel.FATAL),
                        SinkType.CONSOLE_SINK, Map.of())
                .build();

        PhonePeLogger logger = new PhonePeLogger(loggerConfig);
        logger.info("com.app.main", "Starting multi-threaded logging test with SYNC write mode");

        int numThreads = 5;
        int messagesPerThread = 10;
        CountDownLatch latch = new CountDownLatch(numThreads);
        ExecutorService appThreadPool = Executors.newFixedThreadPool(numThreads);

        for (int thread = 0; thread < numThreads; thread++) {
            final int threadId = thread;
            appThreadPool.submit(() -> {
                for (int msg = 0; msg < messagesPerThread; msg++) {
                    logger.debug("com.app.thread " + threadId, "Message " + msg + " from thread " + threadId);
                }
                latch.countDown();
            });
        }

        try {
            latch.await();
            appThreadPool.shutdown();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        logger.shutdown();
        System.out.println("=== End of Write Mode SYNC with Multi-threaded Logging Example ===");
    }

    private static void logLevelFiltering() {
        System.out.println("=== Log Level Filtering Example ===");

        LoggerConfig loggerConfig = LoggerConfig.builder()
                .timeStampFormat("yyyy-MM-dd HH:mm:ss,SSS")
                .defaultLoggingLevel(LoggingLevel.WARN)
                .threadingModel(ThreadingModel.SINGLE_THREADED)
                .writeMode(WriteMode.SYNC)
                .addSinkMapping(List.of(LoggingLevel.DEBUG,
                                LoggingLevel.INFO,
                                LoggingLevel.WARN,
                                LoggingLevel.ERROR,
                                LoggingLevel.FATAL),
                        SinkType.CONSOLE_SINK, Map.of())
                .build();

        System.out.println("Logger configured with default level WARN. Only WARN, ERROR and FATAL messages should be logged.");
        PhonePeLogger logger = new PhonePeLogger(loggerConfig);
        logger.debug("com.app.filter", "This debug message should NOT be logged");
        logger.info("com.app.filter", "This info message should NOT be logged");

        logger.warn("com.app.filter", "This warn message should be logged");
        logger.error("com.app.filter", "This error message should be logged");
        logger.fatal("com.app.filter", "This fatal message should be logged");
        logger.shutdown();

        System.out.println("=== End of Log Level Filtering Example ===");
    }

    private static void configParsingMultiple() {
        System.out.println("=== Configuration Parsing with Multiple Sections Example ===");

        String multiSectionConfig = """
                # Global properties
                ts_format:yyyy-MM-dd HH:mm:ss,SSS
                log_level:DEBUG
                thread_model: SINGLE_THREADED
                write_mode: SYNC
                
                [DEBUG]
                sink_type:CONSOLE_SINK
                
                [INFO]
                sink_type:CONSOLE_SINK
                
                [FATAL]
                sink_type:FILE_SINK
                file_location:/logger-demo/multi-section-logs/fatal.log
                """;

        System.out.println("Parsing multi-section config:");
        LoggerConfig config2 = LoggerConfigParser.parseMultiSection(multiSectionConfig);
        PhonePeLogger logger = new PhonePeLogger(config2);
        logger.debug("com.app.config", "This is a debug message using multi-section config");
        logger.info("com.app.config", "This is an info message using multi-section config");
        logger.fatal("com.app.config", "This is a fatal message using multi-section config");
        logger.shutdown();
    }

    private static void configParsingSimple() {
        System.out.println("=== Configuration Parsing Example ===");

        String simpleConfig = """
                ts_format:yyyy-MM-dd HH:mm:ss,SSS
                log_level:DEBUG
                sink_type:CONSOLE_SINK
                """;

        System.out.println("Parsing simple config:");
        LoggerConfig config1 = LoggerConfigParser.parseSimple(simpleConfig);
        PhonePeLogger logger = new PhonePeLogger(config1);
        logger.debug("com.app.config", "This is a debug message using simple config");
        logger.shutdown();
    }

    private static void multiThreadedLogging() {
        System.out.println("=== Multi-threaded Logging Example ===");

        String logDir = "logger-demo/multi-threaded-logs";
        cleanDirectory(logDir);

        LoggerConfig loggerConfig = LoggerConfig.builder()
                .timeStampFormat("yyyy-MM-dd HH:mm:ss,SSS")
                .defaultLoggingLevel(LoggingLevel.DEBUG)
                .threadingModel(ThreadingModel.MULTI_THREADED)
                .writeMode(WriteMode.ASYNC)
                .addSinkMapping(List.of(LoggingLevel.DEBUG,
                                LoggingLevel.INFO,
                                LoggingLevel.WARN,
                                LoggingLevel.ERROR,
                                LoggingLevel.FATAL),
                        SinkType.CONSOLE_SINK, Map.of())
                .build();

        PhonePeLogger logger = new PhonePeLogger(loggerConfig);
        logger.info("com.app.main", "Starting multi-threaded logging test");

        // simulating multiple threads logging concurrently
        int numThreads = 5;
        int messagesPerThread = 10;
        CountDownLatch latch = new CountDownLatch(numThreads);
        ExecutorService appThreadPool = Executors.newFixedThreadPool(numThreads);

        for (int thread = 0; thread < numThreads; thread++) {
            final int threadId = thread;
            appThreadPool.submit(() -> {
                for (int msg = 0; msg < messagesPerThread; msg++) {
                    logger.debug("com.app.thread " + threadId, "Message " + msg + " from thread " + threadId);
                }
                latch.countDown();
            });
        }

        try {
            latch.await();
            appThreadPool.shutdown();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        logger.shutdown();
        System.out.println("=== End of Multi-threaded Logging Example ===");


    }

    private static void multipleSinkMappings() {
        System.out.println("=== Multiple Sink Mappings Example ===");

        String logDir = "/logger-demo/multi-sink-logs";
        cleanDirectory(logDir);
        LoggerConfig loggerConfig = LoggerConfig.builder()
                .timeStampFormat("yyyy-MM-dd HH:mm:ss,SSS")
                .defaultLoggingLevel(LoggingLevel.DEBUG)
                .addSinkMapping(List.of(LoggingLevel.INFO, LoggingLevel.WARN, LoggingLevel.FATAL), SinkType.FILE_SINK, Map.of("file_location", logDir + "/app.log"))
                .addSinkMapping(List.of(LoggingLevel.DEBUG, LoggingLevel.ERROR), SinkType.CONSOLE_SINK, Map.of())
                .build();

        PhonePeLogger logger = new PhonePeLogger(loggerConfig);
        logger.info("com.app.info", "This is a info message");
        logger.warn("com.app.warning", "This is a warning message");
        logger.fatal("com.app.fatal", "This is a fatal message");

        System.out.println("Info, warn and fatal messages should be in file, debug and error should be on console");

        logger.debug("com.app.debug", "This is another debug message");
        logger.error("com.app.error", "This is another error message");

        logger.shutdown();
        System.out.println("=== End of Multiple Sink Mappings Example ===");


    }

    private static void fileLoggingWithRotation() {
        System.out.println("=== File Logging with Rotation Example ===");

        String logDir = "/logger-demo/log-rotation-logs";
        String logFile = logDir + "/application.log";

        cleanDirectory(logDir);

        LoggerConfig loggerConfig = LoggerConfig.builder()
                .timeStampFormat("yyyy-MM-dd HH:mm:ss,SSS")
                .defaultLoggingLevel(LoggingLevel.INFO)
                .threadingModel(ThreadingModel.SINGLE_THREADED)
                .writeMode(WriteMode.SYNC)
                .addSinkMapping(List.of(LoggingLevel.DEBUG,
                                LoggingLevel.INFO,
                                LoggingLevel.WARN,
                                LoggingLevel.ERROR,
                                LoggingLevel.FATAL),
                        SinkType.FILE_SINK,
                        Map.of(
                                "file_location", logFile,
                                "max_file_size_bytes", "5",
                                "max_backup_files", "3"
                        ))
                .build();

        PhonePeLogger logger = new PhonePeLogger(loggerConfig);

        for (int i = 1; i <= 50; i++) {
            logger.info("com.app.service", "Processing transaction #" + i + " with payload data");
        }

        logger.flush();
        logger.shutdown();

        File dir = new File(logDir);
        System.out.println("Files in " + logDir + ":");
        if (dir.exists()) {
            for (File f : Objects.requireNonNull(dir.listFiles())) {
                System.out.println("  " + f.getName() + " (" + f.length() + " bytes)");
            }
        }
        System.out.println();
    }

    private static void basicConsoleLogging() {
        System.out.println("=== Basic Console Logging Example ===");
        LoggerConfig loggerConfig = LoggerConfig.builder()
                .timeStampFormat("yyyy-MM-dd HH:mm:ss,SSS")
                .defaultLoggingLevel(LoggingLevel.DEBUG)
                .threadingModel(ThreadingModel.SINGLE_THREADED).
                writeMode(WriteMode.SYNC).
                addSinkMapping(List.of(LoggingLevel.DEBUG,
                                LoggingLevel.INFO,
                                LoggingLevel.WARN,
                                LoggingLevel.ERROR,
                                LoggingLevel.FATAL),
                        SinkType.CONSOLE_SINK, Map.of())
                .build();

        PhonePeLogger logger = new PhonePeLogger(loggerConfig);

        logger.debug("com.app.startup", "Application is starting up");
        logger.info("com.app.startup", "Application started successfully");
        logger.warn("com.app.config", "Configuration file is missing, using defaults");
        logger.error("com.app.database", "Failed to connect to database");
        logger.fatal("com.app.crash", "Unhandled exception occurred, application will crash");
        logger.shutdown();

        System.out.println("=== End of Basic Console Logging Example ===");
    }

    private static void cleanDirectory(String logDir) {
        File dir = new File(logDir);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete();
                    }
                }
            }
        } else {
            dir.mkdirs();
        }
    }
}