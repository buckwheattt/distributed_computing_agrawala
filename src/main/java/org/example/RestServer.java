package org.example;

import static spark.Spark.*;

public class RestServer {

    public static void start(Node node) {
        threadPool(50);
        port(node.getPort());

        RicartAgrawala ra = node.getRicartAgrawala();
        System.out.println("DEBUG: ricart in RestServer.start = " + ra);


        post("/request", (req, res) -> {
            String raw = req.body();
            String[] parts = raw.split(";");
            String senderUrl = parts[0].trim();
            int ts = Integer.parseInt(parts[1].trim());

            node.getLogger().log("Received REQUEST from " + senderUrl + " ts=" + ts);
            ra.receiveRequest(senderUrl, ts);
            return "OK";
        });


        post("/reply", (req, res) -> {
            String senderUrl = req.body().trim();

            node.getLogger().log("Received REPLY from " + senderUrl);
            ra.receiveReply(senderUrl);

            return "OK";
        });


        post("/leave", (req, res) -> {
            String leavingUrl = req.body().trim();
            node.getLogger().log("Incoming LEAVE from " + leavingUrl);

            node.removePeer(leavingUrl);
            return "OK";
        });


        post("/join", (req, res) -> {
            String peerUrl = req.body().trim();
            node.getLogger().log("JOIN from " + peerUrl);

            node.addPeer(peerUrl);

            if (!node.getAddress().equals(peerUrl)) {
                RestClient.post(peerUrl, "/joinConfirm", node.getAddress(), node);
            }

            return "OK";
        });


        post("/joinConfirm", (req, res) -> {
            String peerUrl = req.body().trim();
            node.getLogger().log("JOIN CONFIRM from " + peerUrl);

            node.addPeer(peerUrl);
            return "OK";
        });


        post("/revive", (req, res) -> {
            String revivedUrl = req.body().trim();
            node.getLogger().log("REVIVE request from " + revivedUrl);

            node.addPeer(revivedUrl);

            StringBuilder sb = new StringBuilder();

            sb.append("VALUE=").append(node.getSharedVariable().read()).append("\n");

            sb.append(node.getAddress()).append("\n");

            for (String p : node.getPeers()) {
                sb.append(p).append("\n");
            }

            return sb.toString();
        });


        post("/syncSharedValue", (req, res) -> {
            String[] parts = req.body().split(";");

            String senderUrl = parts[0].trim();  // это добавил RestClient
            int ts = Integer.parseInt(parts[1].trim());
            String writerId = parts[2].trim();
            int value = Integer.parseInt(parts[3].trim());

            node.getClock().update(ts);

            boolean applied = node.getSharedVariable().applyIfNewer(value, ts, writerId);

            node.getLogger().log("SYNC from " + senderUrl +
                    " ts=" + ts + " writer=" + writerId + " val=" + value +
                    " applied=" + applied);

            return "OK";
        });



        get("/getSharedValue", (req, res) -> {
            return "" + node.getSharedVariable().read();
        });


        post("/enterCS", (req, res) -> {
            new Thread(() -> {
                ra.requestCS();

                try {
                    SharedVariable sv = node.getSharedVariable();
                    int old = sv.read();
                    int newVal = old + 1;
                    int writeTs = node.getClock().tick();
                    sv.writeLocal(newVal, writeTs, node.getId());


                    node.getLogger().log(
                            "CRITICAL SECTION (REST): " + old + " -> " + newVal
                    );

                    Thread.sleep(1000);

                } catch (Exception ignored) {
                } finally {
                    ra.releaseCS();  // sync
                }
            }).start();

            return "OK";
        });

        post("/leaveCS", (req, res) -> {
            new Thread(ra::releaseCS).start();
            return "Released";
        });

        // ping
        get("/ping", (req, res) -> "OK");
    }
}


