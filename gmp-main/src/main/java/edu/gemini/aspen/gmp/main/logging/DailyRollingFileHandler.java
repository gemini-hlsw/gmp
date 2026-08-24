package edu.gemini.aspen.gmp.main.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * JUL handler with the same behavior as log4j's DailyRollingFileAppender,
 * which pax-logging provided before the OSGi removal: log to a fixed file and,
 * on the first record of a new day, rename the previous day's file to
 * {@code <file>.yyyy-MM-dd}. No retention limit, matching the old setup.
 *
 * Configure via logging.properties:
 * <pre>
 * edu.gemini.aspen.gmp.main.logging.DailyRollingFileHandler.pattern = ../../logs/gmp.log
 * edu.gemini.aspen.gmp.main.logging.DailyRollingFileHandler.formatter = edu.gemini.aspen.gmp.main.logging.GmpLogFormatter
 * edu.gemini.aspen.gmp.main.logging.DailyRollingFileHandler.level = ALL
 * </pre>
 */
public class DailyRollingFileHandler extends Handler {

    private static final SimpleDateFormat SUFFIX = new SimpleDateFormat("yyyy-MM-dd");

    private final File file;
    private OutputStream out;
    private long nextRollover;

    public DailyRollingFileHandler() throws IOException {
        LogManager manager = LogManager.getLogManager();
        String cname   = getClass().getName();
        String pattern = value(manager, cname + ".pattern", "logs/gmp.log");
        String level   = value(manager, cname + ".level", "ALL");
        String fmt     = value(manager, cname + ".formatter", null);

        setLevel(Level.parse(level));
        if (fmt != null) {
            try {
                setFormatter((java.util.logging.Formatter)
                        Class.forName(fmt.trim()).getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                setFormatter(new SimpleFormatter());
            }
        } else {
            setFormatter(new SimpleFormatter());
        }

        file = new File(pattern);
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create log directory " + parent);
        }
        openStream();
        nextRollover = nextMidnight(file.exists() ? file.lastModified() : System.currentTimeMillis());
    }

    private static String value(LogManager manager, String key, String fallback) {
        String v = manager.getProperty(key);
        return v == null ? fallback : expand(v.trim());
    }

    // ${prop} expansion from system properties; LogManager does none itself and
    // the dev logging.properties refers to ${logs.dir}
    private static String expand(String value) {
        StringBuffer sb = new StringBuffer();
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("\\$\\{([^}]+)}").matcher(value);
        while (m.find()) {
            String replacement = System.getProperty(m.group(1), m.group(0));
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private void openStream() throws IOException {
        out = new FileOutputStream(file, true);
    }

    private static long nextMidnight(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }

    @Override
    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        try {
            long now = System.currentTimeMillis();
            if (now >= nextRollover) {
                rollover();
                nextRollover = nextMidnight(now);
            }
            out.write(getFormatter().format(record).getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            reportError(null, e, ErrorManager.WRITE_FAILURE);
        }
    }

    private void rollover() throws IOException {
        out.close();
        File rolled = new File(file.getPath() + "." + SUFFIX.format(new Date(nextRollover - 1)));
        if (!file.renameTo(rolled)) {
            reportError("Could not roll " + file + " to " + rolled, null, ErrorManager.GENERIC_FAILURE);
        }
        openStream();
    }

    @Override
    public synchronized void flush() {
        try {
            if (out != null) {
                out.flush();
            }
        } catch (IOException e) {
            reportError(null, e, ErrorManager.FLUSH_FAILURE);
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (out != null) {
                out.close();
                out = null;
            }
        } catch (IOException e) {
            reportError(null, e, ErrorManager.CLOSE_FAILURE);
        }
    }
}
