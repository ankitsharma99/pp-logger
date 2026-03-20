package org.phonepe;


import org.phonepe.config.LoggerConfig;
import org.phonepe.core.PhonePeLogger;
import org.phonepe.enums.LoggingLevel;
import org.phonepe.enums.SinkType;
import org.phonepe.enums.ThreadingModel;
import org.phonepe.enums.WriteMode;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Main {
    public static void main(String[] args) {
        basicConsoleLogging();

        fileLoggingWithRotation();
    }

    private static void fileLoggingWithRotation() {
        System.out.println("=== File Logging with Rotation Example ===");

        String logDir = "/tmp/logger-demo/logs";
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
}