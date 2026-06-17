package org.adam.zenithx;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameData {

    private static GameData instance;

    private ServerSocket relayServer;
    private Socket minecraftSocket;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private CountDownLatch guestReady = new CountDownLatch(1);
    private CountDownLatch hostReadyLatch = new CountDownLatch(1); // NEW

    private static int LOCAL_PORT = 25565;

    public static void setPort(int port) {
        LOCAL_PORT = port;
    }

    public static int getPort() {
        return LOCAL_PORT;
    }

    public static synchronized GameData getInstance() {
        if (instance == null) instance = new GameData();
        return instance;
    }

    public boolean isRunning() {
        return running.get();
    }

    public CountDownLatch getGuestReadyLatch() {
        return guestReady;
    }

    // NEW — GUEST waits on this until HOST relay is connected to LAN
    public CountDownLatch getHostReadyLatch() {
        return hostReadyLatch;
    }

    // NEW — called in handleMessage when "HOST_READY" arrives
    public void signalHostReady() {
        hostReadyLatch.countDown();
    }

    public void startHostRelay() {
        if (!running.compareAndSet(false, true)) return;

        new Thread(() -> {
            int retries = 60;
            while (retries-- > 0 && running.get()) {
                try {
                    Thread.sleep(500);
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress("127.0.0.1", LOCAL_PORT), 3000);
                    socket.setTcpNoDelay(true);
                    socket.setKeepAlive(true);
                    minecraftSocket = socket;
                    System.out.println("[ZenithX] Connected to LAN on " + LOCAL_PORT);

                    // NEW — tell the GUEST that HOST relay is fully ready
                    HostClient.getInstance().notifyHostReady();

                    startReading(socket);
                    return;
                } catch (Exception e) {
                    System.out.println("[ZenithX] Waiting for LAN...");
                }
            }
            System.err.println("[ZenithX] Failed to connect to LAN");
            running.set(false);
        }, "zenithx-host-relay").start();
    }

    private void startReading(Socket socket) {
        new Thread(() -> {
            try {
                InputStream in = socket.getInputStream();
                byte[] buffer = new byte[8192];
                int read;

                while (running.get() && (read = in.read(buffer)) != -1) {
                    byte[] copy = new byte[read];
                    System.arraycopy(buffer, 0, copy, 0, read);
                    HostClient.getInstance().sendRaw(copy);
                }
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("[ZenithX] Relay disconnected: " + e.getMessage());
                    stop();
                }
            }
        }, "zenithx-reader").start();
    }

    public void startGuest() {
        if (!running.compareAndSet(false, true)) return;
        guestReady = new CountDownLatch(1);
        hostReadyLatch = new CountDownLatch(1); // reset for every new session

        new Thread(() -> {
            try {
                relayServer = new ServerSocket();
                relayServer.setReuseAddress(true);
                relayServer.bind(new InetSocketAddress("127.0.0.1", 24499));
                relayServer.setSoTimeout(60000); // was 30 000 — give extra time for slow hosts

                System.out.println("[ZenithX] Guest relay listening on 24499");
                guestReady.countDown();

                minecraftSocket = relayServer.accept();
                minecraftSocket.setTcpNoDelay(true);
                minecraftSocket.setKeepAlive(true);
                minecraftSocket.setSoTimeout(0);

                System.out.println("[ZenithX] Minecraft connected to guest relay");
                startReading(minecraftSocket);

            } catch (Exception e) {
                System.err.println("[ZenithX] Guest relay error: " + e.getMessage());
                running.set(false);
                guestReady.countDown();
            }
        }, "zenithx-guest-relay").start();
    }

    public void receiveData(byte[] data) {
        try {
            if (minecraftSocket == null || minecraftSocket.isClosed()) return;
            java.io.OutputStream out = minecraftSocket.getOutputStream();
            out.write(data);
            out.flush();
        } catch (Exception e) {}
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) return;
        HostClient.getInstance().sendSessionEnd();
        try { if (minecraftSocket != null) minecraftSocket.close(); } catch (Exception ignored) {}
        try { if (relayServer != null) relayServer.close(); } catch (Exception ignored) {}
        minecraftSocket = null;
        relayServer = null;
        instance = null;
        System.out.println("[ZenithX] GameData stopped");
    }
}