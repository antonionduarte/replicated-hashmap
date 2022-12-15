package asd.slog;

public class SLogger {
    final String clazz;
    final Object[] kv;

    SLogger(String clazz, Object... kv) {
        this.clazz = clazz;
        this.kv = kv;
    }

    public void log(String event, Object... kv) {
        SLog.loggerLog(this, event, kv);
    }
}
