package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class Logger {
    private final String nodeId;
    private final LamportClock clock;
    private final PrintWriter writer;

    public Logger(String nodeId, LamportClock clock) throws IOException {
        this.nodeId = nodeId;
        this.clock = clock;
        this.writer = new PrintWriter(new FileWriter("logs_" + nodeId + ".txt", true), true);
    }

    public void log(String message) {
        String timestamp = LocalDateTime.now().toString();
        int logicalTime = clock.getTime();

        String fullMessage = String.format("[%s] [Lamport: %d] [%s] %s", timestamp, logicalTime, nodeId, message);

        System.out.println(fullMessage);
        writer.println(fullMessage);
    }

    public void close() {
        writer.close();
    }
}
