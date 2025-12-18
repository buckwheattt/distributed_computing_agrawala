package org.example;

public class SharedVariable {
    private int value = 0;

    // версия последней записи
    private int lastTs = 0;
    private String lastWriter = "";

    public synchronized int read() { return value; }

    public synchronized int getLastTs() { return lastTs; }
    public synchronized String getLastWriter() { return lastWriter; }

    public synchronized void writeLocal(int newValue, int ts, String writerId) {
        value = newValue;
        lastTs = ts;
        lastWriter = writerId;
    }

    public synchronized boolean applyIfNewer(int newValue, int ts, String writerId) {
        if (ts > lastTs) {
            value = newValue;
            lastTs = ts;
            lastWriter = writerId;
            return true;
        }
        if (ts == lastTs && writerId.compareTo(lastWriter) > 0) {
            value = newValue;
            lastTs = ts;
            lastWriter = writerId;
            return true;
        }
        return false;
    }
}
