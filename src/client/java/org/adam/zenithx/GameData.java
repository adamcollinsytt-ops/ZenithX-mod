package org.adam.zenithx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.adam.zenithx.handlers.PacketCompressor;

public class GameData {

    private static GameData instance;

    private ServerSocket relayServer;
    private Socket minecraftSocket;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private CountDownLatch guestReady = new CountDownLatch(1);

    private final Object socketLock = new Object();
    private final java.util.List<byte[]> pendingChunks = new java.util.ArrayList<>();

    private static int LOCAL_PORT = 25565;

    public static void setPort(int port) {
        LOCAL_PORT = port;
    }

    public static int getPort() {
        return LOCAL_PORT;
    }

    public static synchronized GameData getInstance() {
        if (instance == null) {
            instance = new GameData();
        }
        return instance;
    }

    public boolean isRunning() {
        return running.get();
    }

    public CountDownLatch getGuestReadyLatch() {
        return guestReady;
    }

    public synchronized void startHostRelay() {
        cleanSockets();
        running.set(true);

        new Thread(() -> {
            int retries = 120;
            while (retries-- > 0 && running.get()) {
                try {
                    Thread.sleep(300);
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress("127.0.0.1", LOCAL_PORT), 3000);
                    socket.setTcpNoDelay(true);
                    socket.setKeepAlive(true);
                    
                    minecraftSocket = socket;
                    System.out.println("[ZenithX] Connected to local LAN server on port " + LOCAL_PORT);

                    flushPending(socket);
                    startReading(socket);
                    return;
                } catch (Exception e) {
                    // waiting for local server start...
                }
            }
            System.err.println("[ZenithX] Failed to connect to LAN port " + LOCAL_PORT);
            stop(false);
        }, "zenithx-host-relay").start();
    }

    private void startReading(Socket socket) {
        new Thread(() -> {
            try {
                socket.setSoTimeout(30);
                InputStream in = socket.getInputStream();
                byte[] readBuf = new byte[16000];
                ByteArrayOutputStream acc = new ByteArrayOutputStream();
                long lastFlush = System.currentTimeMillis();
                long totalSent = 0;
                int sentMsgs = 0;

                final long FLUSH_INTERVAL_MS = 15;
                final int MAX_CHUNK_BYTES = 32000;
                final int MAX_QUEUED_BYTES = 250_000;

                while (running.get() && !socket.isClosed()) {
                    int read;
                    if (acc.size() >= MAX_QUEUED_BYTES) {
                        read = -2;
                        try { Thread.sleep(5); } catch (InterruptedException ignored) {}
                    } else {
                        try {
                            read = in.read(readBuf);
                        } catch (java.net.SocketTimeoutException timeout) {
                            read = -2;
                        }
                    }

                    if (read == -1) break;
                    if (read > 0) acc.write(readBuf, 0, read);

                    long now = System.currentTimeMillis();
                    boolean sizeReady = acc.size() >= MAX_CHUNK_BYTES;
                    boolean timeReady = acc.size() > 0 && (now - lastFlush) >= FLUSH_INTERVAL_MS;

                    if (sizeReady || timeReady) {
                        byte[] full = acc.toByteArray();
                        int sendLen = Math.min(full.length, MAX_CHUNK_BYTES);
                        byte[] chunk = java.util.Arrays.copyOfRange(full, 0, sendLen);

                        acc.reset();
                        if (sendLen < full.length) {
                            acc.write(full, sendLen, full.length - sendLen);
                        }

                        byte[] compressedData = PacketCompressor.compress(chunk);
                        HostClient.getInstance().sendRaw(compressedData);
                        totalSent += chunk.length;
                        sentMsgs++;
                        lastFlush = now;
                    }
                }

                while (acc.size() > 0) {
                    byte[] full = acc.toByteArray();
                    int sendLen = Math.min(full.length, MAX_CHUNK_BYTES);
                    byte[] chunk = java.util.Arrays.copyOfRange(full, 0, sendLen);
                    acc.reset();
                    if (sendLen < full.length) acc.write(full, sendLen, full.length - sendLen);
                    HostClient.getInstance().sendRaw(PacketCompressor.compress(chunk));
                }

                System.out.println("[ZenithX] Local read loop ended (running=" + running.get() + ", closed=" + socket.isClosed()
                        + ", sent " + sentMsgs + " msgs / " + totalSent + " bytes)");
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("[ZenithX] Local socket connection closed: " + e.getMessage());
                    stop(true);
                }
            }
        }, "zenithx-reader").start();
    }

    public synchronized void startGuest() {
        cleanSockets();
        running.set(true);
        guestReady = new CountDownLatch(1);

        new Thread(() -> {
            try {
                relayServer = new ServerSocket();
                relayServer.setReuseAddress(true);
                relayServer.bind(new InetSocketAddress("127.0.0.1", 24499));
                relayServer.setSoTimeout(0);

                System.out.println("[ZenithX] Guest relay listening on 24499");
                guestReady.countDown();

                minecraftSocket = relayServer.accept();
                minecraftSocket.setTcpNoDelay(true);
                minecraftSocket.setKeepAlive(true);

                System.out.println("[ZenithX] Minecraft connected to guest relay");
                flushPending(minecraftSocket);
                startReading(minecraftSocket);

            } catch (Exception e) {
                System.err.println("[ZenithX] Guest relay error: " + e.getMessage());
                guestReady.countDown();
                stop(false);
            }
        }, "zenithx-guest-relay").start();
    }

    public synchronized void receiveData(byte[] incomingData) {
        try {
            if (incomingData == null || incomingData.length < 5) {
                System.out.println("[ZenithX] << receiveData got " + (incomingData == null ? "null" : incomingData.length + " bytes") + " (too short/empty, ignoring)");
                return;
            }
            System.out.println("[ZenithX] << receiveData got " + incomingData.length + " bytes from peer");

            byte flag = incomingData[4];
            byte[] framedPayload = new byte[incomingData.length - 5];
            System.arraycopy(incomingData, 5, framedPayload, 0, framedPayload.length);

            byte[] payload = PacketCompressor.decompressPayload(flag, framedPayload);

            synchronized (socketLock) {
                if (minecraftSocket == null || minecraftSocket.isClosed()) {
                    pendingChunks.add(payload);
                    return;
                }
            }

            OutputStream out = minecraftSocket.getOutputStream();
            out.write(payload);
            out.flush();

        } catch (Exception e) {
            System.err.println("[ZenithX] Error processing stream frame: " + e.getMessage());
        }
    }

    private void flushPending(Socket socket) {
        synchronized (socketLock) {
            if (pendingChunks.isEmpty()) return;
            try {
                OutputStream out = socket.getOutputStream();
                for (byte[] chunk : pendingChunks) {
                    out.write(chunk);
                }
                out.flush();
                System.out.println("[ZenithX] Flushed " + pendingChunks.size() + " buffered chunk(s) to local socket");
            } catch (Exception e) {
                System.err.println("[ZenithX] Failed to flush buffered data: " + e.getMessage());
            } finally {
                pendingChunks.clear();
            }
        }
    }

    private void cleanSockets() {
        try { if (minecraftSocket != null && !minecraftSocket.isClosed()) minecraftSocket.close(); } catch (Exception ignored) {}
        try { if (relayServer != null && !relayServer.isClosed()) relayServer.close(); } catch (Exception ignored) {}
        minecraftSocket = null;
        relayServer = null;
        synchronized (socketLock) { pendingChunks.clear(); }
    }

    public void stop() {
        stop(true);
    }

    public synchronized void stop(boolean notifyServer) {
        if (!running.compareAndSet(true, false)) return;

        if (notifyServer) {
            try {
                HostClient client = HostClient.getInstance();
                if (client != null && client.isOpen()) {
                    client.sendSessionEnd();
                }
            } catch (Exception ignored) {}
        }

        cleanSockets();
        instance = null;
        System.out.println("[ZenithX] GameData stopped gracefully");
    }
}