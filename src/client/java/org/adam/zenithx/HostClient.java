package org.adam.zenithx;

import java.net.URI;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.level.storage.LevelSummary;
import org.adam.zenithx.ui.NoConnectionWarningScreen;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class HostClient extends WebSocketClient {

    private static final Gson   GSON = new Gson();
    private static HostClient   instance;

    private static String[] getServers() {
        return Servers.LIST;
    }

    private static final String UPDATE_DOWNLOAD_URL =
            "https://zenithx.42web.io/ZenithX.jar";
    private static final String VERSION_CHECK_URL =
            "https://gist.githubusercontent.com/adamcollinsytt-ops/33062d3149ffb8381b86f7b73935a605/raw/zenithx-version.json";

    public static final String MOD_VERSION = "1.0.2";
    public static String       SERVER_VERSION = "1.0.2";

    private static volatile int currentIndex  = 0;
    public  static             String   lastLoginUsername;
    private static volatile boolean retrying      = false;
    private static volatile boolean wasConnected = false;
    private final  Minecraft minecraft = Minecraft.getInstance();

    private boolean isLoggedIn = false; // added by adam for fixed duplicate logins
    private volatile boolean lowTraffic = false; // added by adam for fixed bypass timer (4 more bugs)

    private static final long BUSY_COOLDOWN_MS = 60_000;
    private static final Map<String, Long> BUSY_UNTIL = new ConcurrentHashMap<>();
    private volatile boolean busy = false;

    private static String lastFailReason = null;
    public static volatile boolean UI_BLOCKED    = false;
    public static volatile boolean FORCE_TITLE   = false;
    public static volatile boolean USER_DISMISSED = false;

    private volatile boolean manualClose = false;

    public static void setLastFailReason(String reason) {
        lastFailReason = reason;
    }

    public static String getLastFailReason() {
        return lastFailReason;
    }

    private static void showConnectionWarning(Runnable onContinue, Runnable onRetry) {
        Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();

            if (mc.player != null) {
                org.adam.zenithx.ui.notification.NotificationManager.show(
                        new org.adam.zenithx.ui.notification.Notification(
                                "ZenithX",
                                "Connection to ZenithX Network lost - retrying in the background...",
                                5f, false,
                                "connection_lost_ingame",
                                () -> true,
                                null, null, null, null
                        )
                );
                if (onRetry != null) onRetry.run();
                return;
            }

            if (!(mc.screen instanceof NoConnectionWarningScreen)) {
                mc.setScreen(new NoConnectionWarningScreen(onContinue, onRetry, getLastFailReason()));
            }
        });
    }

    private HostClient(URI uri) {
        super(uri);
        this.setTcpNoDelay(true);
        this.setConnectionLostTimeout(60);
    }

    private Timer pingTimer;

    public void startPingLoop() {
        if (pingTimer != null) pingTimer.cancel();
        long period = lowTraffic ? 45000 : 15000;
        pingTimer = new Timer(lowTraffic ? "zenithx-ping-slow" : "zenithx-ping", true);
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                if (isOpen()) sendPing();
            }
        }, 5000, period);
    }

    public void stopPingLoop() {
        if (pingTimer != null) { pingTimer.cancel(); pingTimer = null; }
    }

    Consumer<JsonObject> handler;
    Runnable onConnect;
    Runnable onDisconnect;

    public static synchronized HostClient getInstance() {
        String[] servers = getServers();
        String target = servers[Math.min(currentIndex, servers.length - 1)];
        if (instance == null || instance.isClosed()
                || (!instance.isOpen() && !instance.getURI().toString().equals(target))) {
            try {
                instance = new HostClient(new URI(target));
            } catch (Exception e) {
                System.err.println("[ZenithX] Init failed: " + e.getMessage());
            }
        }
        return instance;
    }

    public void hostWorld(LevelSummary world, String friendName) {
        if (world == null) return;
        try {
            minecraft.execute(() -> minecraft.setScreen(
                    new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(null)));
            if (friendName != null && !friendName.isEmpty()) {
                sendHostingInvite(friendName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendHostingInvite(String friendName) {
        System.out.println("The hosting invitation was sent to: " + friendName);
    }

    public void setMessageHandler(Consumer<JsonObject> h) { this.handler = h; }
    public void setOnConnect(Runnable r)               { this.onConnect = r; }
    public void setOnDisconnect(Runnable r)              { this.onDisconnect = r; }

    public static String getServerVersion() { return SERVER_VERSION; }

    private static Timer   updateTimer           = null;
    private static String lastNotifiedVersion   = null;

    public static void startUpdateChecker() {
        if (updateTimer != null) return;
        updateTimer = new Timer("zenithx-update-timer", true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { checkForUpdates(); }
        }, 0, 5 * 60 * 1000);
    }

    public static void checkForUpdates() {
        new Thread(() -> {
            try {
                String urlStr = VERSION_CHECK_URL + "?t=" + System.currentTimeMillis();
                java.net.URL url = new java.net.URL(urlStr);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent",    "ZenithX-Mod");
                conn.setRequestProperty("Cache-Control",  "no-cache");
                conn.setRequestProperty("Pragma",         "no-cache");

                String     json    = new String(conn.getInputStream().readAllBytes());
                JsonObject obj     = new Gson().fromJson(json, JsonObject.class);

                String     latest  = obj.get("version").getAsString().trim();

                if (obj.has("server_version")) {
                    SERVER_VERSION = obj.get("server_version").getAsString().trim();
                }

                String     current = MOD_VERSION.trim();

                System.out.println("[ZenithX] current=" + current + " | latest=" + latest + " | server_version=" + SERVER_VERSION);

                if (latest.equals(current))             return;
                if (latest.equals(lastNotifiedVersion))  return;
                lastNotifiedVersion = latest;

                final String finalUrl = UPDATE_DOWNLOAD_URL;

                new Thread(() -> {
                    try {
                        int tries = 0;
                        while (tries++ < 60) {
                            Thread.sleep(500);
                            if (Minecraft.getInstance().screen instanceof TitleScreen) break;
                        }
                    } catch (Exception ignored) {}

                    Minecraft.getInstance().execute(() ->
                            org.adam.zenithx.ui.notification.NotificationManager.show(
                                    new org.adam.zenithx.ui.notification.Notification(
                                            "ZenithX",
                                            "New update v" + latest + " available!",
                                            8f, false,
                                            "update_available_" + latest,
                                            () -> true,
                                            null, null, null, null,
                                            "UPDATE",
                                            () -> {
                                                try {
                                                    java.awt.Desktop.getDesktop().browse(new java.net.URI(finalUrl));
                                                } catch (Exception e) {
                                                    System.err.println("[ZenithX] Failed to open browser: " + e.getMessage());
                                                }
                                            }
                                    )
                            )
                    );
                }, "zenithx-update-show").start();

            } catch (Exception e) {
                System.err.println("[ZenithX] Update check failed: " + e.getMessage());
            }
        }, "zenithx-update-check").start();
    }

    private synchronized void sendLoginPacket(String username) {
        if (isLoggedIn) return;
        isLoggedIn = true;

        JsonObject o = new JsonObject();
        o.addProperty("type",     "LOGIN");
        o.addProperty("username", username);
        o.addProperty("version", MOD_VERSION);
        o.addProperty("server_version", SERVER_VERSION);
        send(GSON.toJson(o));
    }

    private static boolean isBusy(String uri) {
        Long until = BUSY_UNTIL.get(uri);
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            BUSY_UNTIL.remove(uri);
            return false;
        }
        return true;
    }

    private static boolean allBusy(String[] servers) {
        for (String s : servers) if (!isBusy(s)) return false;
        return true;
    }

    private static void copyCallbacks(HostClient from, HostClient to) {
        if (from == null) return;
        if (from.handler      != null) to.handler      = from.handler;
        if (from.onConnect    != null) to.onConnect    = from.onConnect;
        if (from.onDisconnect != null) to.onDisconnect = from.onDisconnect;
    }

    private void markBusyAndFailover() {
        if (busy) return;
        busy = true;

        BUSY_UNTIL.put(getURI().toString(), System.currentTimeMillis() + BUSY_COOLDOWN_MS);
        System.out.println("[ZenithX] Server busy, skipping for "
                + (BUSY_COOLDOWN_MS / 1000) + "s: " + getURI());

        setLastFailReason("server_busy");
        retrying     = false;
        wasConnected = false;
        manualClose  = true;
        try { if (!isClosed()) close(); } catch (Exception ignored) {}

        startBusyFailover();
    }

    private static void startBusyFailover() {
        new Thread(() -> {
            try {
                final String[] servers = getServers();
                final int firstBackup  = Servers.firstBackupIndex();
                final HostClient old;
                synchronized (HostClient.class) { old = instance; }

                for (int i = 0; i < servers.length; i++) {
                    if (isBusy(servers[i])) continue;

                    HostClient candidate = new HostClient(new URI(servers[i]));
                    candidate.manualClose = true;
                    copyCallbacks(old, candidate);

                    synchronized (HostClient.class) {
                        currentIndex = i;
                        instance = candidate;
                    }

                    System.out.println("[ZenithX] Busy-failover trying: " + servers[i]);

                    if (candidate.connectBlocking(6, TimeUnit.SECONDS)
                            && candidate.isOpen() && !candidate.busy) {
                        candidate.manualClose = false;
                        wasConnected   = true;
                        lastFailReason = null;
                        if (i >= firstBackup) candidate.applyLowTrafficMode();
                        return;
                    }

                    if (candidate.busy) return;

                    candidate.manualClose = true;
                    try { candidate.close(); } catch (Exception ignored) {}
                }

                setLastFailReason("server_busy");
                showConnectionWarning(
                    () -> {},
                    () -> {
                        BUSY_UNTIL.clear();
                        retrying = false;
                        startReconnectLoop();
                    }
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[ZenithX] Busy failover error: " + e);
            }
        }, "zenithx-busy-failover").start();
    }

    @Override
    public void onOpen(ServerHandshake h) {
        wasConnected = true;
        isLoggedIn = false;
        String playerName = getCurrentPlayerName();
        if (DevelopmentShared.isDev(playerName)) {
            System.out.println("[ZenithX] Connected to: " + getURI());
        } 

        if (lastLoginUsername != null) {
            sendLoginPacket(lastLoginUsername);
        }

        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof NoConnectionWarningScreen) {
                Minecraft.getInstance().setScreen(null);
            }
        });
        if (onConnect != null) onConnect.run();
    }

    @Override
    public void onMessage(String raw) {
        try {
            JsonObject json = GSON.fromJson(raw, JsonObject.class);

            if (json.has("type")) {
                String type = json.get("type").getAsString();

                if ("LOGIN_FAIL".equals(type)) {
                    String reason = json.has("reason") ? json.get("reason").getAsString() : "connection_lost";

                    if ("server_busy".equals(reason)) {
                        markBusyAndFailover();
                        return;
                    }

                    setLastFailReason(reason);

                    retrying = false;
                    wasConnected = false;
                    manualClose = true;

                    try {
                        close();
                    } catch (Exception ignored) {}

                    if ("outdated_version".equals(reason)) {
                        return;
                    }

                    showConnectionWarning(
                        () -> {},
                        () -> {
                            retrying = false;
                            startReconnectLoop();
                        }
                    );
                    return;
                }
            }

            if (json.has("type") && "FRIEND_REQUEST".equals(json.get("type").getAsString())) {
                if (json.has("from")) {
                    String fromPlayer = json.get("from").getAsString();

                    java.util.UUID playerUuid = java.util.UUID.randomUUID();

                    Minecraft.getInstance().execute(() ->
                        org.adam.zenithx.ui.FriendRequestNotification.show(
                            fromPlayer,
                            playerUuid,
                            () -> acceptFriend(fromPlayer),
                            () -> System.out.println("[ZenithX] Friend request from " + fromPlayer + " rejected")
                        )
                    );
                }
            }

            if (handler != null) handler.accept(json);
        } catch (Exception e) {
            System.err.println("[ZenithX] onMessage failed: " + e);
        }
    }

    @Override
    public void onMessage(java.nio.ByteBuffer bytes) {
        byte[] data = new byte[bytes.remaining()];
        bytes.get(data);
        GameData.getInstance().receiveData(data);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[ZenithX] WebSocket Error occurred! Connection failed or dropped.");
        System.err.println("[ZenithX] Error Type: " + ex.getClass().getName());
        System.err.println("[ZenithX] Error Message: " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        isLoggedIn = false;
        if (onDisconnect != null) onDisconnect.run();
        if (manualClose) return;

        if (code == 1013) {
            markBusyAndFailover();
            return;
        }

        if ("outdated_version".equals(lastFailReason)) {
            retrying = false;
            wasConnected = false;
            return;
        }

        if (retrying) return;
        if (wasConnected) {
            wasConnected = false;
            startReconnectLoop();
        } else {
            switchServer();
        }
    }

    private static void switchServer() {
        wasConnected = false;
        currentIndex++;
        if (currentIndex >= getServers().length) {
            currentIndex = 0;
            setLastFailReason("connection_lost");
            showConnectionWarning(
                () -> {},
                () -> startReconnectLoop()
            );
            return;
        }

        new Thread(() -> {
            try {
                Consumer<JsonObject> oldHandler     = instance != null ? instance.handler     : null;
                Runnable             oldOnConnect   = instance != null ? instance.onConnect   : null;
                Runnable             oldOnDisconnect= instance != null ? instance.onDisconnect: null;

                HostClient fresh = new HostClient(new URI(getServers()[currentIndex]));
                if (oldHandler      != null) fresh.handler      = oldHandler;
                if (oldOnConnect    != null) fresh.onConnect    = oldOnConnect;
                if (oldOnDisconnect != null) fresh.onDisconnect = oldOnDisconnect;

                synchronized (HostClient.class) { instance = fresh; }
                fresh.connect();
            } catch (Exception e) {
                System.err.println("[ZenithX] switchServer failed: " + e.getMessage());
            }
        }, "zenithx-switch").start();
    }

    public static synchronized boolean connectToAnyServer() {
        if (instance != null && instance.isOpen()) {
            return true;
        }

        Consumer<JsonObject> oldHandler      = instance != null ? instance.handler      : null;
        Runnable             oldOnConnect    = instance != null ? instance.onConnect    : null;
        Runnable             oldOnDisconnect = instance != null ? instance.onDisconnect : null;

        String[] servers = getServers();

        String playerName = getCurrentPlayerName();
        /*if (playerName != null) { // edit by xekek from make it like players default!
            if (DevelopmentShared.isDev(playerName)) {
                int backupStart = Servers.firstBackupIndex();
                if (backupStart < servers.length) {
                    servers = java.util.Arrays.copyOfRange(servers, backupStart, servers.length);
                }
            }
        }*/

        for (int i = 0; i < servers.length; i++) {
            if (isBusy(servers[i])) continue;

            HostClient test = null;
            try {
                test = new HostClient(new URI(servers[i]));
                test.manualClose = true;
                if (oldHandler      != null) test.handler      = oldHandler;
                if (oldOnConnect    != null) test.onConnect    = oldOnConnect;
                if (oldOnDisconnect != null) test.onDisconnect = oldOnDisconnect;

                currentIndex = i;
                instance = test;

                System.out.println("[ZenithX] Trying: " + servers[i]);

                if (test.connectBlocking(6, TimeUnit.SECONDS) && test.isOpen()) {
                    if (test.busy) return false;
                    test.manualClose = false;
                    wasConnected = true;
                    lastFailReason = null;
                    return true;
                }

                System.err.println("[ZenithX] Timeout/failed: " + servers[i]);
                test.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (test != null) test.close();
                break;
            } catch (Exception e) {
                System.err.println("[ZenithX] Failed: " + servers[i] + " -> " + e);
                if (test != null) test.close();
            }
        }

        setLastFailReason("connection_lost");
        wasConnected = false;
        return false;
    }

    private static String getCurrentPlayerName() {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.getUser() != null) {
                return mc.getUser().getName();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static void startReconnectLoop() {
        if ("outdated_version".equals(lastFailReason)) {
            return;
        }
        if (retrying) return;
        retrying = true;
        new Thread(() -> {
            Servers.fetchServers();
            final String[] servers = getServers();
            final int totalServers = servers.length;
            int serverCycle = 0;

            while (retrying) {
                try {
                    Thread.sleep(serverCycle == 0 ? 3000 : 10000);

                    if (instance != null && instance.isOpen()) {
                        retrying = false;
                        Minecraft.getInstance().execute(() -> {
                            if (Minecraft.getInstance().screen instanceof NoConnectionWarningScreen) {
                                Minecraft.getInstance().setScreen(null);
                            }
                        });
                        return;
                    }

                    if (serverCycle >= totalServers * 2) {
                        retrying = false;
                        setLastFailReason(lastFailReason != null ? lastFailReason : "connection_lost");
                        showConnectionWarning(
                            () -> {},
                            () -> { retrying = false; startReconnectLoop(); }
                        );
                        return;
                    }

                    boolean lowTrafficPass = (serverCycle >= totalServers);
                    if (lowTrafficPass) {
                        int backups = Math.max(1, Math.min(Servers.backupCount(), totalServers));
                        int firstBackup = totalServers - backups;
                        currentIndex = firstBackup + ((serverCycle - totalServers) % backups);

                        if (serverCycle == totalServers) {
                            Minecraft.getInstance().execute(() ->
                                org.adam.zenithx.ui.notification.NotificationManager.show(
                                    new org.adam.zenithx.ui.notification.Notification(
                                        "ZenithX - High Traffic",
                                        "Servers are busy. Switching to backup servers (Low-Traffic Mode).",
                                        8f, false,
                                        "high_traffic_warning",
                                        () -> true,
                                        null, null, null, null,
                                        null, null
                                    )
                                )
                            );
                        }
                    } else {
                        currentIndex = serverCycle % totalServers;
                    }

                    if (isBusy(servers[currentIndex]) && !allBusy(servers)) {
                        System.out.println("[ZenithX] Skipping busy server: " + servers[currentIndex]);
                        serverCycle++;
                        continue;
                    }

                    serverCycle++;

                    Consumer<JsonObject> oldHandler      = instance != null ? instance.handler      : null;
                    Runnable             oldOnConnect    = instance != null ? instance.onConnect    : null;
                    Runnable             oldOnDisconnect = instance != null ? instance.onDisconnect : null;

                    HostClient fresh = new HostClient(new URI(servers[currentIndex]));
                    if (oldHandler      != null) fresh.handler      = oldHandler;
                    if (oldOnConnect    != null) fresh.onConnect    = oldOnConnect;
                    if (oldOnDisconnect != null) fresh.onDisconnect = oldOnDisconnect;

                    synchronized (HostClient.class) { instance = fresh; }

                    System.out.println("[ZenithX] Reconnect trying: " + servers[currentIndex]);
                    fresh.connect();

                    int wait = 0;
                    while (wait++ < 30 && !fresh.isOpen() && !fresh.isClosed()) {
                        Thread.sleep(250);
                    }

                    if (fresh.isOpen()) {
                        wasConnected = true;
                        retrying     = false;

                        if (lowTrafficPass) {
                            fresh.applyLowTrafficMode();
                        }
                        return;
                    }

                    fresh.manualClose = true;
                    try { fresh.close(); } catch (Exception ignored) {}

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    retrying = false;
                    return;
                } catch (Exception e) {
                    System.err.println("[ZenithX] Reconnect loop error: " + e);
                }
            }
        }, "zenithx-reconnect").start();
    }

    public void applyLowTrafficMode() {
        System.out.println("[ZenithX] Low-Traffic / Slow-Mode activated to reduce server load.");
        lowTraffic = true;
        startPingLoop();
    }

    public static void tryReconnectNow() {
        startReconnectLoop();
    }

    public void sendJson(JsonObject obj) {
        if (isOpen()) send(GSON.toJson(obj));
    }

    public void sendRaw(byte[] data) {
        if (!isOpen() || data == null) return;
        send(data);
    }

    public void login(String username) {
        lastLoginUsername = username;
        if (isOpen()) {
            sendLoginPacket(username);
        }
    }

    public void sendFriendRequest(String target) {
        JsonObject o = new JsonObject();
        o.addProperty("type",   "FRIEND_REQUEST");
        o.addProperty("target", target);
        sendJson(o);
    }

    public void acceptFriend(String from) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "FRIEND_ACCEPT");
        o.addProperty("from", from);
        sendJson(o);
    }

    public void removeFriend(String username) {
        JsonObject o = new JsonObject();
        o.addProperty("type",    "FRIEND_REMOVE");
        o.addProperty("username", username);
        sendJson(o);
    }

    public void sendInvite(String target) {
        JsonObject o = new JsonObject();
        o.addProperty("type",   "INVITE");
        o.addProperty("target", target);
        sendJson(o);
    }

    public void acceptInvite(String from) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "INVITE_ACCEPT");
        o.addProperty("from", from);
        sendJson(o);
    }

    public void notifyHostReady() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "HOST_READY");
        sendJson(o);
    }

    public void sendSessionEnd() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "SESSION_END");
        sendJson(o);
    }

    public static void forceReturnToTitle() {
        Minecraft mc = Minecraft.getInstance();
        FORCE_TITLE  = true;
        UI_BLOCKED   = false;
        mc.execute(() -> mc.setScreen(new TitleScreen(false, null)));
    }
}