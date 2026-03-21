package org.phonepe.sink.impl;

import org.phonepe.message.LogMessage;
import org.phonepe.rotation.LogRotationPolicy;
import org.phonepe.sink.Sink;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static org.phonepe.utils.LoggingConstants.*;

public class FileSink implements Sink {
    private String filePath;
    private BufferedWriter writer;
    private File logFile;
    private LogRotationPolicy rotationPolicy;
    private final Object writeLock = new Object();

    @Override
    public void init(Map<String, String> properties) {
        this.filePath = properties.get(FILE_LOCATION);
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("file_location is required for FileSink");
        }

        long maxFileSizeBytes = properties.containsKey(MAX_FILE_SIZE_BYTES) ? parseLong(properties.get(MAX_FILE_SIZE_BYTES)) : 10 * 1024 * 1024;
        int maxBackupFiles = properties.containsKey(MAX_BACKUP_FILES) ? parseInt(properties.get(MAX_BACKUP_FILES)) : 5;

        this.rotationPolicy = new LogRotationPolicy(maxFileSizeBytes, maxBackupFiles);

        this.logFile = new File(filePath);
        checkParentDirectoryExistence();
        openWriter(true);
    }

    @Override
    public void write(String formattedMessage, LogMessage originalMessage) {
        synchronized (writeLock) {
            try {
                writer.write(formattedMessage);
                writer.newLine();
                writer.flush();

                if (rotationPolicy.shouldRotateFile(logFile)) {
                    closeWriter();
                    rotationPolicy.rotateFiles(logFile);
                    openWriter(false);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void flush() {
        synchronized (writeLock) {
            try {
                if (writer != null) {
                    writer.flush();
                }
            } catch (IOException e) {
                System.err.println("Failed to flush log file: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        synchronized (writeLock) {
            closeWriter();
        }
    }

    private void openWriter(boolean append) {
        try {
            this.writer = new BufferedWriter(new FileWriter(logFile, append));
        } catch (IOException e) {
            throw new RuntimeException("Failed to open log file for writing: " + logFile.getAbsolutePath(), e);
        }
    }

    private void checkParentDirectoryExistence() {
        File parentDir = logFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new RuntimeException("Failed to create parent directory for log file: " + parentDir.getAbsolutePath());
            }
        }
    }

    private void closeWriter() {
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
                writer = null;
            }
        } catch (IOException e) {
            System.err.println("Failed to close log file writer: " + e.getMessage());
        }
    }
}
