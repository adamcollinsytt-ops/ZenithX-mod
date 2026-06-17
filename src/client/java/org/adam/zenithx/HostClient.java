package org.adam.zenithx;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
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

    private static final String[] SERVERS = {
            "ws://93.115.101.182:9373"
    };

    private static final String UPDATE_DOWNLOAD_URL =
            "https://www.curseforge.com/minecraft/mc-mods/zenithx";
    private static final String VERSION_CHECK_URL =
            "https://gist.githubusercontent.com/adamcollinsytt-ops/33062d3149ffb8381b86f7b73935a605/raw/zenithx-version.json";

    public static final String MOD_VERSION = "1.0.1";

    private static int     currentIndex  = 0;
    static         String  lastLoginUsername;
    private static boolean retrying      = false;
    private static volatile boolean wasConnected = false;
    private final  Minecraft minecraft = Minecraft.getInstance();

    public static String         SERVER_VERSION  = "1.0.1";
    public static UpdateCallback updateCallback  = null;
    public static volatile boolean UI_BLOCKED    = false;
    public static volatile boolean FORCE_TITLE   = false;
    public static volatile boolean USER_DISMISSED = false;

    private HostClient(URI uri) {
        super(uri);
        this.setTcpNoDelay(true);
        this.setConnectionLostTimeout(60);
    }

    private Timer pingTimer;

    public void startPingLoop() {
        if (pingTimer != null) pingTimer.cancel();
        pingTimer = new Timer("zenithx-ping", true);
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                if (isOpen()) sendPing();
            }
        }, 5000, 15000);
    }

    public void stopPingLoop() {
        if (pingTimer != null) { pingTimer.cancel(); pingTimer = null; }
    }

    Consumer<JsonObject> handler;
    Runnable onConnect;
    Runnable onDisconnect;

    public static synchronized HostClient getInstance() {
        if (instance == null || instance.isClosed()) {
            try {
                instance = new HostClient(new URI(SERVERS[currentIndex]));
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
    public void setOnConnect(Runnable r)                  { this.onConnect = r; }
    public void setOnDisconnect(Runnable r)               { this.onDisconnect = r; }

    public static String getServerVersion() { return SERVER_VERSION; }

    private static Timer  updateTimer           = null;
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
                conn.setRequestProperty("User-Agent",     "ZenithX-Mod");
                conn.setRequestProperty("Cache-Control",  "no-cache");
                conn.setRequestProperty("Pragma",         "no-cache");

                String     json    = new String(conn.getInputStream().readAllBytes());
                JsonObject obj     = new Gson().fromJson(json, JsonObject.class);
                String     latest  = obj.get("version").getAsString().trim();
                String     current = MOD_VERSION.trim();

                System.out.println("[ZenithX] current=" + current + " | latest=" + latest);

                if (latest.equals(current))              return;
                if (latest.equals(lastNotifiedVersion))  return;
                lastNotifiedVersion = latest;

                final String finalUrl = obj.has("download")
                        ? obj.get("download").getAsString().trim()
                        : UPDATE_DOWNLOAD_URL;

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

    @Override
    public void onOpen(ServerHandshake h) {
        wasConnected = true;
        System.out.println("[ZenithX] Connected to: " + SERVERS[currentIndex]);
        if (lastLoginUsername != null) {
            JsonObject o = new JsonObject();
            o.addProperty("type",     "LOGIN");
            o.addProperty("username", lastLoginUsername);
            send(GSON.toJson(o));
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
            if (handler != null) handler.accept(json);
        } catch (Exception ignored) {}
    }

    @Override
    public void onMessage(java.nio.ByteBuffer bytes) {
        byte[] data = new byte[bytes.remaining()];
        bytes.get(data);
        GameData.getInstance().receiveData(data);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[ZenithX] WS error: " + ex.getMessage());
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (onDisconnect != null) onDisconnect.run();
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
        currentIndex = (currentIndex + 1) % SERVERS.length;
        new Thread(() -> {
            try {
                Consumer<JsonObject> oldHandler     = instance != null ? instance.handler     : null;
                Runnable             oldOnConnect   = instance != null ? instance.onConnect   : null;
                Runnable             oldOnDisconnect= instance != null ? instance.onDisconnect: null;

                HostClient fresh = new HostClient(new URI(SERVERS[currentIndex]));
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
        Consumer<JsonObject> oldHandler      = instance != null ? instance.handler      : null;
        Runnable             oldOnConnect    = instance != null ? instance.onConnect    : null;
        Runnable             oldOnDisconnect = instance != null ? instance.onDisconnect : null;

        for (int i = 0; i < SERVERS.length; i++) {
            currentIndex = i;
            try {
                HostClient test = new HostClient(new URI(SERVERS[i]));
                if (oldHandler      != null) test.handler      = oldHandler;
                if (oldOnConnect    != null) test.onConnect    = oldOnConnect;
                if (oldOnDisconnect != null) test.onDisconnect = oldOnDisconnect;

                System.out.println("[ZenithX] Trying: " + SERVERS[i]);
                test.connect();
                instance     = test;
                wasConnected = false;
                return true;
            } catch (Exception e) {
                System.err.println("[ZenithX] Failed: " + SERVERS[i]);
            }
        }
        return false;
    }

    public static void startReconnectLoop() {
        if (retrying) return;
        retrying = true;
        new Thread(() -> {
            int serverCycle = 0;
            while (retrying) {
                try {
                    Thread.sleep(serverCycle == 0 ? 3000 : 10000);

                    if (instance != null && instance.isOpen()) {
                        retrying = false;
                        return;
                    }

                    currentIndex = serverCycle % SERVERS.length;
                    serverCycle++;

                    Consumer<JsonObject> oldHandler      = instance != null ? instance.handler      : null;
                    Runnable             oldOnConnect    = instance != null ? instance.onConnect    : null;
                    Runnable             oldOnDisconnect = instance != null ? instance.onDisconnect : null;

                    HostClient fresh = new HostClient(new URI(SERVERS[currentIndex]));
                    if (oldHandler      != null) fresh.handler      = oldHandler;
                    if (oldOnConnect    != null) fresh.onConnect    = oldOnConnect;
                    if (oldOnDisconnect != null) fresh.onDisconnect = oldOnDisconnect;

                    synchronized (HostClient.class) { instance = fresh; }

                    fresh.connect();

                    int wait = 0;
                    while (wait++ < 20 && !fresh.isOpen() && !fresh.isClosed()) {
                        Thread.sleep(250);
                    }

                    if (fresh.isOpen()) {
                        wasConnected = true;
                        retrying     = false;
                        return;
                    }

                } catch (Exception ignored) {}
            }
        }, "zenithx-reconnect").start();
    }

    public static void tryReconnectNow() {
        startReconnectLoop();
    }

    public void sendJson(JsonObject obj) {
        if (isOpen()) send(GSON.toJson(obj));
    }

    public void sendRaw(byte[] data) {
        if (isOpen()) this.send(data);
    }

    public void login(String username) {
        lastLoginUsername = username;
        if (isOpen()) {
            JsonObject o = new JsonObject();
            o.addProperty("type",     "LOGIN");
            o.addProperty("username", username);
            sendJson(o);
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
        o.addProperty("type",     "FRIEND_REMOVE");
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