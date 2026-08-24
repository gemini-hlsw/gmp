package edu.gemini.aspen.gmp.main.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Reproduces the log4j pattern used under pax-logging:
 * {@code %d %-5p [%t] %40.40c - %x %m%n}
 * so existing tooling that parses gmp.log keeps working.
 */
public class GmpLogFormatter extends Formatter {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

    @Override
    public synchronized String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date(record.getMillis())));
        sb.append(' ');
        sb.append(padLevel(levelName(record)));
        sb.append(" [").append(Thread.currentThread().getName()).append("] ");
        sb.append(pad40(record.getLoggerName()));
        sb.append(" -  ");
        sb.append(formatMessage(record));
        sb.append(System.lineSeparator());
        if (record.getThrown() != null) {
            java.io.StringWriter sw = new java.io.StringWriter();
            record.getThrown().printStackTrace(new java.io.PrintWriter(sw));
            sb.append(sw);
        }
        return sb.toString();
    }

    // JUL level names mapped to their log4j equivalents
    private static String levelName(LogRecord record) {
        switch (record.getLevel().getName()) {
            case "SEVERE":  return "ERROR";
            case "WARNING": return "WARN";
            case "CONFIG":
            case "INFO":    return "INFO";
            case "FINE":    return "DEBUG";
            case "FINER":
            case "FINEST":  return "TRACE";
            default:         return record.getLevel().getName();
        }
    }

    private static String padLevel(String level) {
        StringBuilder sb = new StringBuilder(level);
        while (sb.length() < 5) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String pad40(String name) {
        String c = name == null ? "" : name;
        if (c.length() > 40) {
            return c.substring(c.length() - 40);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = c.length(); i < 40; i++) {
            sb.append(' ');
        }
        return sb.append(c).toString();
    }
}
