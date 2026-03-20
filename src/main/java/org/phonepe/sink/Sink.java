package org.phonepe.sink;

import org.phonepe.message.LogMessage;

import java.util.Map;

public interface Sink {
    void init(Map<String, String> properties);

    void write(String formattedMessage, LogMessage originalMessage);

    void flush();

    void close();
}
