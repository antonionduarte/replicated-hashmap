package asd.slog;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.time.Clock;

public class SLog {
    public static interface Invokable {
        public void invoke();
    }

    private static final Clock clock = Clock.systemUTC();
    private static OutputStream outputStream = null;
    private static String prefix;

    public static void init(String filename, Object... kv) {
        if (SLog.outputStream != null)
            throw new IllegalStateException("Cannot initialize twice");
        try {
            var sb = new StringBuilder();
            SLog.appendKv(sb, kv);
            SLog.prefix = sb.toString();

            SLog.outputStream = new BufferedOutputStream(new java.io.FileOutputStream(filename));
        } catch (java.io.FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void log(String event, Object... kv) {
        if (!SLog.shoudLog())
            return;
        var sb = new StringBuilder();
        sb.append(event);
        SLog.appendKv(sb, kv);
        SLog.write(sb.toString());
    }

    public static <T> SLogger logger(Class<T> clazz, Object... kv) {
        return new SLogger(clazz.getSimpleName(), kv);
    }

    public static SLogger logger(String clazz, Object... kv) {
        return new SLogger(clazz, kv);
    }

    static void loggerLog(SLogger logger, String event, Object... kv) {
        if (!SLog.shoudLog())
            return;
        var sb = new StringBuilder();
        sb.append(logger.clazz);
        SLog.appendKv(sb, logger.kv);
        sb.append(" ");
        sb.append(event);
        SLog.appendKv(sb, kv);
        SLog.write(sb.toString());
    }

    private static synchronized void write(String s) {
        try {
            var now = clock.instant();
            var nano_now = now.getEpochSecond() * 1_000_000_000 + ((long) now.getNano());
            SLog.outputStream.write(String.valueOf(nano_now).getBytes());
            SLog.outputStream.write(" ".getBytes());
            SLog.outputStream.write(SLog.prefix.getBytes());
            SLog.outputStream.write(" ".getBytes());
            SLog.outputStream.write(s.getBytes());
            SLog.outputStream.write("\n".getBytes());
            SLog.outputStream.flush();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void appendKv(StringBuilder sb, Object... kv) {
        for (var i = 0; i < kv.length; i += 2) {
            var k = kv[i];
            var v = kv[i + 1];
            sb.append(" ");
            sb.append(k);
            sb.append("=");
            sb.append(v);
        }
    }

    private static boolean shoudLog() {
        if (SLog.outputStream == null)
            return false;
        return true;
    }
}
