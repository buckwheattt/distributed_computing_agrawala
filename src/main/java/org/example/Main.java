package org.example;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.out.println("Usage: java -jar node.jar <port> <comma_separated_peer_urls>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        String[] peerArray = args[1].split(",");
        Set<String> peers = new HashSet<>();
        for (String p : peerArray) {
            if (!p.isBlank()) peers.add(p.trim());
        }

        Node node = new Node("node-" + port, port, peers);

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
                            int old = shared.read();
                            int newVal = old + 1;
                            shared.write(newVal);

                            node.getLogger().log("CRITICAL SECTION: shared variable " + old + " -> " + newVal);

                            Thread.sleep(1000);

                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            ra.releaseCS();  // тут произойдёт SYNC broadcast
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
                        ra.requestCS();  // гарантируем, что пишем только в КС
                        try {
                            int old = shared.read();
                            shared.write(newVal);

                            node.getLogger().log(
                                    "CRITICAL SECTION (MANUAL WRITE): " + old + " -> " + newVal
                            );

                            Thread.sleep(1000); // имитация работы

                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            ra.releaseCS(); // тут произойдёт SYNC broadcast
                        }
                    }).start();
                    break;



                default:
                    System.out.println("Commands: enter, read, write");
            }

        }
    }
}
