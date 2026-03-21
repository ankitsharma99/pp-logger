package org.phonepe.sink.impl;

import org.phonepe.message.LogMessage;
import org.phonepe.sink.Sink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DatabaseSink implements Sink {
    private final List<String> inMemoryStore = Collections.synchronizedList(new ArrayList<>());


    @Override
    public void init(Map<String, String> properties) {
        String dbHost = properties.getOrDefault("db_host", "localhost");
        String dbPort = properties.getOrDefault("db_port", "5432");
        System.out.println("DatabaseSink initialized with host: " + dbHost + ", port: " + dbPort);
    }

    @Override
    public void write(String formattedMessage, LogMessage originalMessage) {
        inMemoryStore.add(formattedMessage);
    }

    @Override
    public void flush() {
        // No-op for in-memory simulation
    }

    @Override
    public void close() {
        System.out.println("DatabaseSink closed. Total records stored: " + inMemoryStore.size());
    }
}
