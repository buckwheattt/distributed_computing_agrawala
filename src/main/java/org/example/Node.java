package org.example;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Node {
    private final String id;
    private final int port;
    private final String address;
    private final LamportClock clock;
    private final Logger logger;
    private final Set<String> peers;     // store full URLs

    private boolean inCriticalSection = false;
    private RicartAgrawala ricart;
    private SharedVariable shared;

    public Node(String id, int port, Set<String> peerUrls) throws IOException {
        this.id = id;
        this.port = port;
        this.address = "http://localhost:" + port;
        this.clock = new LamportClock();
        this.logger = new Logger(id, clock);

        this.peers = ConcurrentHashMap.newKeySet();
        this.peers.addAll(peerUrls);

        logger.log("Node created at " + address);
    }

    public String getId() { return id; }

    public String getAddress() { return address; }

    public int getPort() { return port; }

    public LamportClock getClock() { return clock; }

    public Logger getLogger() { return logger; }

    public Set<String> getPeers() { return peers; }

    public boolean isInCriticalSection() { return inCriticalSection; }

    public void setInCriticalSection(boolean v) { inCriticalSection = v; }

    private Set<String> previousPeers = ConcurrentHashMap.newKeySet();

    public Set<String> getPreviousPeers() {
        return previousPeers;
    }

    public void addPeer(String url) {
        if (url == null) return;
        url = url.trim();
        if (url.isEmpty()) return;
        if (url.equals(this.address)) {
            logger.log("Ignoring self peer: " + url);
            return;
        }

        if (peers.add(url)) {
            logger.log("New peer added: " + url);
        } else {
            logger.log("Peer already known: " + url);
        }
    }
    public void broadcastSharedVariable() {
        int val = shared.read();
        for (String peer : peers) {
            RestClient.post(peer, "/syncSharedValue", String.valueOf(val), this);
        }
        logger.log("SYNC broadcast (manual or RA): value=" + val);
    }




    public void removePeer(String url) {
        peers.remove(url);
        logger.log("Peer removed: " + url);

        if (ricart != null) {
            ricart.peerRemoved(url);
        }
    }


    public void shutdown() {
        logger.log("Node shutting down...");
        logger.close();
    }

    public void setRicartAgrawala(RicartAgrawala ra) {
        this.ricart = ra;
    }

    public RicartAgrawala getRicartAgrawala() { return ricart; }

    public void setSharedVariable(SharedVariable sv) {
        this.shared = sv;
    }

    public SharedVariable getSharedVariable() { return shared; }
}
