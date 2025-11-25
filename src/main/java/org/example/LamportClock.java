package org.example;

import java.util.concurrent.atomic.AtomicInteger;

public class LamportClock {
    private final AtomicInteger time = new AtomicInteger(0);

    // Получить текущее логическое время
    public int getTime() {
        return time.get();
    }

    // Увеличить время на 1 (внутреннее событие)
    public int tick() {
        return time.incrementAndGet();
    }

    // Обновить время по полученному сообщению
    public int update(int receivedTime) {
        int newTime = Math.max(receivedTime, time.get()) + 1;
        time.set(newTime);
        return newTime;
    }
}
