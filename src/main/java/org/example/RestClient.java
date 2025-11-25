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
                // REQUEST: url;timestamp
                fullBody = node.getAddress() + ";" + body;

            } else if ("/reply".equals(endpoint)) {
                // REPLY: только url
                fullBody = node.getAddress();

            } else if ("/syncSharedValue".equals(endpoint)) {
                // 🔥 FIX: SYNC MUST BE url;value
                fullBody = node.getAddress() + ";" + body;

            } else {
                // все остальные эндпоинты (join, kill, revive, setDelay)
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
        }
    }

}

