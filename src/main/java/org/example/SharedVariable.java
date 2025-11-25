package org.example;

public class SharedVariable {
    private int value = 0;

    public synchronized int read() {
        return value;
    }

    public synchronized void write(int newValue) {
        this.value = newValue;
    }
}
