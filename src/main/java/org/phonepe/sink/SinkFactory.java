package org.phonepe.sink;

import org.phonepe.enums.SinkType;
import org.phonepe.sink.impl.ConsoleSink;
import org.phonepe.sink.impl.DatabaseSink;
import org.phonepe.sink.impl.FileSink;

import java.util.Map;

public class SinkFactory {
    public static Sink createSink(SinkType sinkType, Map<String, String> properties) {
        Sink sink = switch (sinkType) {
            case FILE_SINK -> new FileSink();
            case CONSOLE_SINK -> new ConsoleSink();
            case DATABASE_SINK -> new DatabaseSink();
        };
        sink.init(properties);
        return sink;
    }

    public static Sink createCustomSink(String className, Map<String, String> properties) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!Sink.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException("Class " + className + " does not implement Sink interface");
            }
            Sink sink = (Sink) clazz.getDeclaredConstructor().newInstance();
            sink.init(properties);
            return sink;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create custom sink: " + className, e);
        }
    }
}
