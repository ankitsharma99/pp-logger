package org.phonepe.config;

import org.phonepe.enums.LoggingLevel;
import org.phonepe.enums.SinkType;
import org.phonepe.enums.ThreadingModel;
import org.phonepe.enums.WriteMode;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.file.Files.readAllBytes;
import static org.phonepe.utils.LoggingConstants.*;

public class LoggerConfigParser {

    public static LoggerConfig parseSimple(String configString) {
        Map<String, String> props = parseKeyValuePairs(configString);
        return buildFromFlatProperties(props);
    }

    public static LoggerConfig parseFromFile(String filePath) throws IOException {
        String content = new String(readAllBytes(Paths.get(filePath)));
        return parseMultiSection(content);
    }

    private static LoggerConfig parseMultiSection(String content) {
        String[] lines = content.split(LINE_DELIMITER_REGEX);

        Map<String, String> globalProps = new LinkedHashMap<>();
        Map<String, Map<String, String>> sectionProps = new LinkedHashMap<>();
        String currentSection = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith(COMMENT_PREFIX)) {
                continue;
            }

            if (line.startsWith(SECTION_PREFIX) && line.endsWith(SECTION_SUFFIX)) {
                currentSection = line.substring(1, line.length() - 1).trim().toUpperCase();
                sectionProps.putIfAbsent(currentSection, new LinkedHashMap<>());
                continue;
            }

            int colonIdx = line.indexOf(KEY_VALUE_DELIMITER);
            if (colonIdx > 0) {
                String key = line.substring(0, colonIdx).trim().toLowerCase();
                String value = line.substring(colonIdx + 1).trim();

                if (currentSection == null) {
                    globalProps.put(key, value);
                } else {
                    sectionProps.get(currentSection).put(key, value);
                }
            }
        }

        LoggerConfig.Builder builder = LoggerConfig.builder();
        if (globalProps.containsKey(KEY_TS_FORMAT)) {
            builder.timeStampFormat(globalProps.get(KEY_TS_FORMAT));
        }
        if (globalProps.containsKey(KEY_LOG_LEVEL)) {
            builder.defaultLoggingLevel(LoggingLevel.valueOf(globalProps.get(KEY_LOG_LEVEL).toUpperCase()));
        }
        if (globalProps.containsKey(KEY_THREAD_MODEL)) {
            builder.threadingModel(ThreadingModel.valueOf(globalProps.get(KEY_THREAD_MODEL).toUpperCase()));
        }
        if (globalProps.containsKey(KEY_WRITE_MODE)) {
            builder.writeMode(WriteMode.valueOf(globalProps.get(KEY_WRITE_MODE).toUpperCase()));
        }

        if (sectionProps.isEmpty()) {
            // No sections, treat global props as flat config
            return buildFromFlatProperties(globalProps);
        }

        for (Map.Entry<String, Map<String, String>> entry : sectionProps.entrySet()) {
            String levelStr = entry.getKey();
            Map<String, String> props = entry.getValue();
            String sinkTypeStr = props.get(KEY_SINK_TYPE);
            SinkType sinkType = SinkType.valueOf(sinkTypeStr.toUpperCase());
            LoggingLevel loggingLevel = LoggingLevel.valueOf(levelStr);
            builder.addSinkMapping(loggingLevel, sinkType, props);
        }

        return builder.build();
    }

    private static LoggerConfig buildFromFlatProperties(Map<String, String> props) {
        LoggerConfig.Builder builder = LoggerConfig.builder();
        if (props.containsKey(KEY_TS_FORMAT)) {
            builder.timeStampFormat(props.get(KEY_TS_FORMAT));
        }

        LoggingLevel defaultLoggingLevel = LoggingLevel.INFO;
        if (props.containsKey(KEY_LOG_LEVEL)) {
            try {
                defaultLoggingLevel = LoggingLevel.valueOf(props.get(KEY_LOG_LEVEL).toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid log level: " + props.get(KEY_LOG_LEVEL));
            }
        }
        builder.defaultLoggingLevel(defaultLoggingLevel);

        if (props.containsKey(KEY_THREAD_MODEL)) {
            builder.threadingModel(ThreadingModel.valueOf(props.get(KEY_THREAD_MODEL)));
        }

        if (props.containsKey(KEY_WRITE_MODE)) {
            builder.writeMode(WriteMode.valueOf(props.get(KEY_WRITE_MODE)));
        }

        String sinkTypeStr = props.get(KEY_SINK_TYPE);
        if (sinkTypeStr != null) {
            SinkType sinkType = SinkType.valueOf(sinkTypeStr.toUpperCase());
            builder.addSinkMapping(defaultLoggingLevel, sinkType, props);
        }

        return builder.build();
    }

    private static Map<String, String> parseKeyValuePairs(String configString) {
        Map<String, String> properties = new LinkedHashMap<>();
        String[] lines = configString.split(LINE_DELIMITER_REGEX);

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith(COMMENT_PREFIX) || line.startsWith(OPTIONAL_PREFIX)) {
                if (line.startsWith(OPTIONAL_PREFIX)) {
                    line = line.substring(OPTIONAL_PREFIX.length()).trim();
                } else {
                    continue;
                }
            }

            int colonIndex = line.indexOf(KEY_VALUE_DELIMITER);
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim().toLowerCase();
                String value = line.substring(colonIndex + 1).trim();
                properties.put(key, value);
            }
        }
        return properties;
    }
}
