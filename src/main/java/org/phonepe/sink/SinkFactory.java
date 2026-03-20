package org.phonepe.sink;

import org.phonepe.enums.SinkType;
import org.phonepe.sink.impl.ConsoleSink;
import org.phonepe.sink.impl.DatabaseConsoleSink;
import org.phonepe.sink.impl.FileSink;

import java.util.Map;

public class SinkFactory {
    public static Sink createSink(SinkType sinkType, Map<String, String> properties) {
        Sink sink = switch (sinkType) {
            case FILE_SINK -> new FileSink();
            case CONSOLE_SINK -> new ConsoleSink();
            case DATABASE_SINK -> new DatabaseConsoleSink();
        };
        sink.init(properties);
        return sink;

    }
}
