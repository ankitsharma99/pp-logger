package org.phonepe.rotation;

import java.io.*;
import java.util.zip.GZIPOutputStream;

public class LogRotationPolicy {
    private final long maxFileSize;
    private final int maxBackupFiles;

    public LogRotationPolicy(long maxFileSize, int maxBackupFiles) {
        if (maxFileSize <= 0) {
            throw new IllegalArgumentException("Max file size must be greater than 0");
        }
        if (maxBackupFiles < 0) {
            throw new IllegalArgumentException("Max backup files cannot be negative");
        }

        this.maxFileSize = maxFileSize;
        this.maxBackupFiles = maxBackupFiles;
    }

    public LogRotationPolicy() {
        this(10 * 1024 * 1024, 5);
    }

    public boolean shouldRotateFile(File file) {
        return file.exists() && file.length() >= maxFileSize;
    }

    public synchronized void rotateFiles(File activeFile) throws IOException {
        if (!activeFile.exists()) {
            return;
        }

        String basePath = activeFile.getAbsolutePath();

        // delete the oldest file if exists and exceeds the backup limit
        File oldestFileBackup = new File(basePath + "." + maxBackupFiles + ".gz");
        if (oldestFileBackup.exists()) {
            oldestFileBackup.delete();
        }

        // shift existing backups: N.gz -> (N+1).gz
        for (int i = maxBackupFiles - 1; i >= 1; i--) {
            File source = new File(basePath + "." + i + ".gz");
            File target = new File(basePath + "." + (i + 1) + ".gz");
            if (source.exists()) {
                source.renameTo(target);
            }
        }

        // compress current file to .1.gz
        File compressedFile = new File(basePath + ".1.gz");
        compressFile(activeFile, compressedFile);

        new FileOutputStream(activeFile).close();

    }

    private void compressFile(File source, File target) {
        try (FileInputStream fileInputStream = new FileInputStream(source);
             FileOutputStream fileOutputStream = new FileOutputStream(target);
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                gzipOutputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
