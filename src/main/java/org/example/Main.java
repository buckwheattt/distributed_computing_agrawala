package org.example;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws Exception {

        if (args.length < 3) {
            System.out.println("Usage: java -jar node.jar <port> <myHostOrIp> <comma_separated_peer_urls>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String myHost = args[1];
        String peersArg = args[2];

        String[] peerArray = args[1].split(",");
        Set<String> peers = new HashSet<>();
        for (String p : peerArray) {
            if (!p.isBlank()) peers.add(p.trim());
        }

        Node node = new Node("node-" + port, myHost, port, peers);

        SharedVariable shared = new SharedVariable();
        RicartAgrawala ra = new RicartAgrawala(node);

        node.setRicartAgrawala(ra);
        node.setSharedVariable(shared);

        RestServer.start(node);

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("> ");

            String cmd = scanner.nextLine().trim();

            switch (cmd) {

                case "enter":
                    new Thread(() -> {
                        ra.requestCS();
                        try {
                            SharedVariable sv = node.getSharedVariable();

                            int old = sv.read();
                            int newVal = old + 1;            // или manual newVal

                            int writeTs = node.getClock().tick();
                            sv.writeLocal(newVal, writeTs, node.getId());

                            // 🔥 сразу рассылаем новое значение
                            node.broadcastSharedVariable();  // только сделай чтобы он слал ts;writer;val

                            node.getLogger().log("CS: " + old + " -> " + newVal + " ts=" + writeTs);

                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } finally {
                            ra.releaseCS();
                        }
                    }).start();
                    break;

                case "read":
                    System.out.println("Value = " + shared.read());
                    break;

                case "write":
                    System.out.print("New value: ");
                    int newVal = Integer.parseInt(scanner.nextLine());

                    new Thread(() -> {
                        ra.requestCS();
                        try {
                            SharedVariable sv = node.getSharedVariable();

                            int old = sv.read();
                            int newwVal = old + 1;            // или manual newVal

                            int writeTs = node.getClock().tick();
                            sv.writeLocal(newwVal, writeTs, node.getId());

                            // 🔥 сразу рассылаем новое значение
                            node.broadcastSharedVariable();  // только сделай чтобы он слал ts;writer;val

                            node.getLogger().log("CS: " + old + " -> " + newwVal + " ts=" + writeTs);

                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } finally {
                            ra.releaseCS();
                        }
                    }).start();
                    break;

                case "join":
                    System.out.print("Peer URL: ");
                    String peerUrl = scanner.nextLine().trim();

                    if (peerUrl.isEmpty()) {
                        System.out.println("Empty URL.");
                        break;
                    }
                    if (peerUrl.equals(node.getAddress())) {
                        System.out.println("Cannot join self.");
                        break;
                    }

                    new Thread(() -> {
                        node.getLogger().log("JOIN from console: contacting " + peerUrl);

                        node.addPeer(peerUrl);

                        RestClient.post(peerUrl, "/join", node.getAddress(), node);

                        if (!RestClient.ping(peerUrl)) {
                            node.getLogger().log("JOIN: peer " + peerUrl + " is DEAD, cannot sync variable");
                            return;
                        }

                        String sv = RestClient.get(peerUrl + "/getSharedValue", node);
                        if (sv != null) {
                            int value = Integer.parseInt(sv.trim());
                            int writeTs = node.getClock().tick();
                            shared.writeLocal(value, writeTs, node.getId());

                            node.getLogger().log("JOIN: synchronized shared variable = " + value);
                        } else {
                            node.getLogger().log("JOIN: failed to get shared variable from " + peerUrl);
                        }

                    }).start();

                    break;

                case "leave":
                    node.getLogger().log("LEAVE requested from console");

                    node.getPreviousPeers().clear();
                    node.getPreviousPeers().addAll(node.getPeers());

                    String me = node.getAddress();

                    for (String p : node.getPeers()) {
                        RestClient.post(p, "/leave", me, node);
                    }

                    node.getPeers().clear();
                    node.getLogger().log("Graceful leave done. Node is isolated but alive.");
                    break;

                case "revive":
                    new Thread(() -> {
                        node.getLogger().log("REVIVE started...");

                        Set<String> oldPeers = node.getPreviousPeers();
                        if (oldPeers.isEmpty()) {
                            node.getLogger().log("REVIVE: no recorded peers, cannot revive.");
                            return;
                        }

                        String bootstrap = null;
                        for (String p : oldPeers) {
                            if (RestClient.ping(p)) {
                                bootstrap = p;
                                break;
                            }
                        }

                        if (bootstrap == null) {
                            node.getLogger().log("REVIVE: no alive bootstrap found.");
                            return;
                        }

                        node.getLogger().log("REVIVE: contacting bootstrap " + bootstrap);

                        String response = RestClient.postAndGet(bootstrap, "/revive", node.getAddress(), node);
                        if (response == null) {
                            node.getLogger().log("REVIVE failed: no response");
                            return;
                        }

                        String[] lines = response.split("\n");
                        Set<String> alivePeers = new HashSet<>();

                        for (String line : lines) {
                            if (line.startsWith("VALUE=")) {
                                int val = Integer.parseInt(line.substring(6));
                                int writeTs = node.getClock().tick();
                                shared.writeLocal(val, writeTs, node.getId());

                                node.getLogger().log("REVIVE: shared variable updated = " + val);
                            } else {
                                if (RestClient.ping(line)) {
                                    node.addPeer(line);
                                    alivePeers.add(line);
                                }
                            }
                        }

                        node.getLogger().log("REVIVE complete. Alive peers: " + alivePeers);

                    }).start();
                    break;

                case "kill":
                    node.getLogger().log("!!! KILL invoked — terminating process immediately !!!");
                    System.exit(1);
                    break;

                case "enterCS":
                    new Thread(() -> {
                        ra.requestCS();
                        try {
                            int old = shared.read();
                            int newwVal = old + 1;
                            int writeTs = node.getClock().tick();
                            shared.writeLocal(newwVal, writeTs, node.getId());
                            node.broadcastSharedVariable();
                            node.getLogger().log("CRITICAL SECTION: shared variable " +
                                    old + " -> " + newwVal);

                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                    break;

                case "leaveCS":
                    new Thread(() -> {
                        ra.releaseCS();
                    }).start();
                    break;

                default:
                    System.out.println("Commands: enter, read, write, enterCS, leaveCS");
                    System.out.println("Node commands: join, leave, revive, kill");
            }
        }
    }
}
