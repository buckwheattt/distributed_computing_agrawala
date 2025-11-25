package org.example;

import static spark.Spark.*;

public class RestServer {

    public static void start(Node node) {
        threadPool(50);
        port(node.getPort());

        RicartAgrawala ra = node.getRicartAgrawala();
        System.out.println("DEBUG: ricart in RestServer.start = " + ra);

        // Incoming /request message
        post("/request", (req, res) -> {
            String raw = req.body();
            String[] parts = raw.split(";");
            String senderUrl = parts[0].trim();
            int ts = Integer.parseInt(parts[1].trim());

            node.getLogger().log("Received REQUEST from " + senderUrl + " ts=" + ts);

            ra.receiveRequest(senderUrl, ts);
            return "OK";
        });

        // Incoming /reply message
        post("/reply", (req, res) -> {
            String senderUrl = req.body().trim();
            node.getLogger().log("Received REPLY from " + senderUrl);

            ra.receiveReply(senderUrl);
            return "OK";
        });

        post("/enterCS", (req, res) -> {
            new Thread(ra::requestCS).start();
            return "Request for entering CS sent";
        });

        post("/leaveCS", (req, res) -> {
            new Thread(ra::releaseCS).start();
            return "Released";
        });

        post("/join", (req, res) -> {
            String peerUrl = req.body().trim();
            node.addPeer(peerUrl);
            return "Peer added";
        });

        get("/ping", (req, res) -> "OK");
    }
}



