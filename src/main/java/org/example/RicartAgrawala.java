package org.example;

import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class RicartAgrawala {

    private final Node node;

    private final Set<String> deferredReplies = ConcurrentHashMap.newKeySet();
    private volatile boolean requesting = false;
    private volatile int requestTs = 0;

    // латч текущего запроса
    private volatile CountDownLatch latch;

    public RicartAgrawala(Node node) {
        this.node = node;
    }

    // Запрос критической секции
    public void requestCS() {
        Set<String> peersSnapshot;

        synchronized (this) {
            if (requesting || node.isInCriticalSection()) {
                node.getLogger().log("requestCS() called but already requesting/in CS. requesting="
                        + requesting + ", inCS=" + node.isInCriticalSection());
                return;
            }

            node.getClock().tick();
            requestTs = node.getClock().getTime();
            requesting = true;

            deferredReplies.clear();

            // снимок пиров
            peersSnapshot = new HashSet<>();
            for (String p : node.getPeers()) {
                String clean = p.trim();
                if (!clean.isEmpty() && !clean.equals(node.getAddress())) {
                    peersSnapshot.add(clean);
                }
            }

            node.getLogger().log("Requesting critical section, ts=" + requestTs +
                    ", peers=" + peersSnapshot);

            if (peersSnapshot.isEmpty()) {
                node.setInCriticalSection(true);
                node.getLogger().log("Entered critical section (no peers)");
                return;
            }

            latch = new CountDownLatch(peersSnapshot.size());
            node.getLogger().log("Latch created with count=" + latch.getCount());
        }

        // Ожидание ответов — ВНЕ synchronized
        for (String peerUrl : peersSnapshot) {
            RestClient.post(peerUrl, "/request", String.valueOf(requestTs), node);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            node.getLogger().log("requestCS interrupted while waiting for replies");
            return;
        }

        synchronized (this) {
            node.setInCriticalSection(true);
            node.getLogger().log("Entered critical section");
        }
    }

    // Выход из КС
    public synchronized void releaseCS() {
        if (!node.isInCriticalSection()) {
            node.getLogger().log("releaseCS() called but not in critical section.");
            requesting = false;
            latch = null;
            return;
        }

        node.setInCriticalSection(false);
        requesting = false;
        node.getLogger().log("Leaving critical section, sending deferred replies to: " + deferredReplies);

        for (String peerUrl : deferredReplies) {
            new Thread(() -> RestClient.post(peerUrl, "/reply", "", node)).start();
        }
        deferredReplies.clear();
        latch = null;
    }

    // Входящий /request
    public synchronized void receiveRequest(String senderUrl, int ts) {
        senderUrl = senderUrl.trim();
        node.getClock().update(ts);

        boolean replyNow =
                !requesting ||
                        ts < requestTs ||
                        (ts == requestTs && senderUrl.compareTo(node.getAddress()) < 0);

        node.getLogger().log("Handling REQUEST from " + senderUrl +
                " ts=" + ts +
                ", myTs=" + requestTs +
                ", requesting=" + requesting +
                ", replyNow=" + replyNow);

        if (replyNow) {
            RestClient.post(senderUrl, "/reply", "", node);
        } else {
            deferredReplies.add(senderUrl);
            node.getLogger().log("Deferred reply to " + senderUrl +
                    ", deferred set=" + deferredReplies);
        }
    }

    // Входящий /reply
    public void receiveReply(String senderUrl) {
        senderUrl = senderUrl.trim();

        CountDownLatch localLatch;
        synchronized (this) {
            localLatch = this.latch;
            node.getLogger().log("receiveReply from " + senderUrl +
                    ", latch=" + (localLatch == null ? "null" : localLatch.getCount()));
        }

        if (localLatch != null) {
            localLatch.countDown();
            node.getLogger().log("Latch after countDown = " + localLatch.getCount());
        } else {
            node.getLogger().log("receiveReply: latch is null, probably not requesting CS now.");
        }
    }
}
