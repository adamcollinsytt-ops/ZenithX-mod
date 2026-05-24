package org.adam.zenithx;

import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameData {

    private static GameData instance;

    private ServerSocket relayServer;
    private Socket minecraftSocket;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private static int LOCAL_PORT = 25565;

    public static void setPort(int port) {
        LOCAL_PORT = port;
    }

    public static int getPort() {
        return LOCAL_PORT;
    }

    public static GameData getInstance() {
        if (instance == null) instance = new GameData();
        return instance;
    }

    public void startHostRelay() {

        if (!running.compareAndSet(false, true)) return;

        new Thread(() -> {

            int retries = 60;

            while (retries-- > 0 && running.get()) {

                try {

                    Thread.sleep(500);

                    Socket socket = new Socket();

                    socket.connect(
                            new InetSocketAddress("127.0.0.1", LOCAL_PORT),
                            3000
                    );

                    minecraftSocket = socket;

                    System.out.println("[ZenithX] Connected to LAN on " + LOCAL_PORT);

                    startReading(socket);

                    return;

                } catch (Exception e) {
                    System.out.println("[ZenithX] Waiting for LAN...");
                }
            }

            System.err.println("[ZenithX] Failed to connect to LAN");

        }, "zenithx-host-relay").start();
    }

    public void startGuest() {

        if (!running.compareAndSet(false, true)) return;

        new Thread(() -> {

            try {

                relayServer = new ServerSocket(24499);

                System.out.println("[ZenithX] Guest relay listening on 24499");

                minecraftSocket = relayServer.accept();

                System.out.println("[ZenithX] Minecraft connected");

                startReading(minecraftSocket);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }, "zenithx-guest-relay").start();
    }

    private void startReading(Socket socket) {

        new Thread(() -> {

            try (InputStream in = socket.getInputStream()) {

                byte[] buffer = new byte[8192];

                int read;

                while (running.get() && (read = in.read(buffer)) != -1) {

                    String encoded = Base64.getEncoder()
                            .encodeToString(Arrays.copyOf(buffer, read));

                    JsonObject obj = new JsonObject();

                    obj.addProperty("type", "TUNNEL");
                    obj.addProperty("data", encoded);

                    HostClient.getInstance().sendJson(obj);
                }

            } catch (Exception e) {

                if (running.get()) {
                    System.err.println("[ZenithX] Relay disconnected: " + e.getMessage());
                }
            }

        }, "zenithx-reader").start();
    }

    public void receiveData(String base64Data) {

        try {

            if (minecraftSocket == null || minecraftSocket.isClosed()) return;

            byte[] data = Base64.getDecoder().decode(base64Data);

            OutputStream out = minecraftSocket.getOutputStream();

            out.write(data);
            out.flush();

        } catch (Exception e) {
            System.err.println("[ZenithX] Receive error: " + e.getMessage());
        }
    }

    public void stop() {

        running.set(false);

        try {
            if (minecraftSocket != null)
                minecraftSocket.close();
        } catch (Exception ignored) {
        }

        try {
            if (relayServer != null)
                relayServer.close();
        } catch (Exception ignored) {
        }

        minecraftSocket = null;
        relayServer = null;

        System.out.println("[ZenithX] GameData stopped");
    }
}