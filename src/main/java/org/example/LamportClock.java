package org.example;

import java.util.concurrent.atomic.AtomicInteger;

public class LamportClock {
    private final AtomicInteger time = new AtomicInteger(0);

    public int getTime() {
        return time.get();
    }

    public int tick() {
        return time.incrementAndGet();
    }

    public int update(int receivedTime) {
        int newTime = Math.max(receivedTime, time.get()) + 1;
        time.set(newTime);
        return newTime;
    }
}
