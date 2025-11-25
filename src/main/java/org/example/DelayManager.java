package org.example;

public class DelayManager {
    private static int delayMs = 0;

    public static void setDelay(int ms) {
        delayMs = ms;
    }

    public static int getDelay() {
        return delayMs;
    }

    public static void sleepIfNeeded() {
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
