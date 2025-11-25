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

//        post("/enterCS", (req, res) -> {
//            new Thread(() -> {
//                ra.requestCS();
//                try {
//                    SharedVariable sv = node.getSharedVariable();
//                    int old = sv.read();
//                    int newVal = old + 1;
//                    sv.write(newVal);
//
//                    node.getLogger().log("CRITICAL SECTION (REST): " + old + " -> " + newVal);
//
//                    Thread.sleep(1000);
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                } finally {
//                    ra.releaseCS();
//                }
//            }).start();
//
//            return "Executed critical section";
//        });
        post("/enterCS", (req, res) -> {
            new Thread(() -> {
                ra.requestCS();  // вход по RA

                try {
                    SharedVariable sv = node.getSharedVariable();

                    int old = sv.read();
                    int newVal = old + 1;
                    sv.write(newVal);

                    node.getLogger().log(
                            "CRITICAL SECTION (REST): shared variable " + old + " -> " + newVal
                    );

                    Thread.sleep(1000);  // имитация работы

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    ra.releaseCS();  // <-- ЗДЕСЬ ВЫЗЫВАЕТСЯ SYNC BROADCAST
                }

            }).start();

            return "OK";
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

        post("/syncSharedValue", (req, res) -> {
            String body = req.body(); // формат: "<sender-url>;<value>"
            String[] parts = body.split(";");
            int value = Integer.parseInt(parts[1]);

            node.getSharedVariable().write(value);
            node.getLogger().log("SYNC: shared variable updated to " + value +
                    " from " + parts[0]);

            return "OK";
        });



        get("/ping", (req, res) -> "OK");
    }
}



