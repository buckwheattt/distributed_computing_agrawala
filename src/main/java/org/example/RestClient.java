package org.example;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RestClient {

    public static void post(String peerUrl, String endpoint, String body, Node node) {
        try {
            DelayManager.sleepIfNeeded();

            String fullUrl = peerUrl + endpoint;
            URL url = new URL(fullUrl);

            node.getLogger().log("Sending " + endpoint +
                    " to " + peerUrl +
                    " url=" + fullUrl);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/plain");

            String fullBody;

            if ("/request".equals(endpoint)) {
                fullBody = node.getAddress() + ";" + body;

            } else if ("/reply".equals(endpoint)) {
                fullBody = node.getAddress();

            } else if ("/syncSharedValue".equals(endpoint)) {
                fullBody = node.getAddress() + ";" + body;

            } else {
                fullBody = body == null ? "" : body;
            }

            try (OutputStream os = conn.getOutputStream()) {
                os.write(fullBody.getBytes());
            }

            int code = conn.getResponseCode();
            node.getLogger().log("POST " + endpoint + " -> " + peerUrl + " (code " + code + ")");

            conn.disconnect();
        } catch (Exception e) {
            node.getLogger().log("POST error to " + peerUrl + endpoint + ": " + e.getMessage());

            node.removePeer(peerUrl);
        }
        
    }
    public static String postAndGet(String peerUrl, String endpoint, String body, Node node) {
        try {
            DelayManager.sleepIfNeeded();
            URL url = new URL(peerUrl + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/plain");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                return null;
            }

            return new String(conn.getInputStream().readAllBytes());

        } catch (Exception e) {
            return null;
        }
    }

    public static String get(String fullUrl, Node node) {
        try {
            DelayManager.sleepIfNeeded();

            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int code = conn.getResponseCode();
            node.getLogger().log("GET " + fullUrl + " (code " + code + ")");

            if (code != 200) return null;

            return new String(conn.getInputStream().readAllBytes());

        } catch (Exception e) {
            node.getLogger().log("GET error for " + fullUrl + ": " + e.getMessage());
            return null;
        }
    }


    public static boolean ping(String url) {
        try {
            URL u = new URL(url + "/ping");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(500);
            c.setReadTimeout(500);
            return c.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }


}

