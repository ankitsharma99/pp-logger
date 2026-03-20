package org.phonepe.sink.impl;

import org.phonepe.message.LogMessage;
import org.phonepe.sink.Sink;

import java.util.Map;

public class ConsoleSink implements Sink {
    @Override
    public void init(Map<String, String> properties) {

    }

    @Override
    public void write(String formattedMessage, LogMessage originalMessage) {
        System.out.println(formattedMessage);
    }

    @Override
    public void flush() {
        System.out.flush();
    }

    @Override
    public void close() {

    }
}
