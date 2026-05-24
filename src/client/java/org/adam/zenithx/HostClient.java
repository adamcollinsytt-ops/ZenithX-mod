package org.adam.zenithx;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import org.adam.zenithx.ui.NoConnectionWarningScreen;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class HostClient extends WebSocketClient {

    private static final Gson GSON = new Gson();
    private static HostClient instance;

    private static final String[] SERVERS = {
            "ws://93.115.101.182:9373",
            "ws://bot.cyerox.fun:3023"
    };

    private static int currentIndex = 0;
    static String lastLoginUsername;
    private static boolean retrying = false;
    private static volatile boolean wasConnected = false;

    public static volatile boolean UI_BLOCKED = false;
    public static volatile boolean FORCE_TITLE = false;
    public static volatile boolean USER_DISMISSED = false;

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

    private HostClient(URI uri) {
        super(uri);
    }

    public void setMessageHandler(Consumer<JsonObject> h) { this.handler = h; }
    public void setOnConnect(Runnable r) { this.onConnect = r; }
    public void setOnDisconnect(Runnable r) { this.onDisconnect = r; }

    @Override
    public void onOpen(ServerHandshake h) {
        wasConnected = true;
        System.out.println("[ZenithX] Connected to: " + SERVERS[currentIndex]);
        System.out.println("[ZenithX] lastLoginUsername = " + lastLoginUsername);

        if (lastLoginUsername != null) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "LOGIN");
            o.addProperty("username", lastLoginUsername);
            send(GSON.toJson(o));
            System.out.println("[ZenithX] LOGIN sent from onOpen: " + lastLoginUsername);
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
        currentIndex++;
        if (currentIndex >= SERVERS.length) {
            currentIndex = 0;
        }
        System.out.println("[ZenithX] Switching to: " + SERVERS[currentIndex]);
        try {
            Consumer<JsonObject> oldHandler = instance != null ? instance.handler : null;
            Runnable oldOnConnect = instance != null ? instance.onConnect : null;
            Runnable oldOnDisconnect = instance != null ? instance.onDisconnect : null;

            instance = new HostClient(new URI(SERVERS[currentIndex]));

            if (oldHandler != null) instance.handler = oldHandler;
            if (oldOnConnect != null) instance.onConnect = oldOnConnect;
            if (oldOnDisconnect != null) instance.onDisconnect = oldOnDisconnect;

            instance.connect();
        } catch (Exception e) {
            System.err.println("[ZenithX] Switch failed: " + e.getMessage());
        }
    }

    public static synchronized boolean connectToAnyServer() {

        for (int i = 0; i < SERVERS.length; i++) {

            try {

                currentIndex = i;

                HostClient test = new HostClient(new URI(SERVERS[i]));

                if (instance != null) {
                    test.handler = instance.handler;
                    test.onConnect = instance.onConnect;
                    test.onDisconnect = instance.onDisconnect;
                }

                System.out.println("[ZenithX] Trying: " + SERVERS[i]);

                test.connectBlocking();

                if (test.isOpen()) {

                    instance = test;

                    wasConnected = true;

                    System.out.println("[ZenithX] Connected to: " + SERVERS[i]);

                    return true;
                }

            } catch (Exception e) {
                System.err.println("[ZenithX] Failed: " + SERVERS[i]);
            }
        }

        return false;
    }

    public static void startReconnectLoop() {
        if (retrying) return;
        retrying = true;

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    HostClient client = getInstance();
                    if (!client.isOpen()) {
                        client.reconnectBlocking();
                        if (client.isOpen()) {
                            retrying = false;
                            timer.cancel();
                        } else {
                            switchServer();
                        }
                    }
                } catch (Exception ignored) {
                    switchServer();
                }
            }
        }, 0, 15000);
    }

    public static void tryReconnectNow() {
        startReconnectLoop();
    }

    public void sendJson(JsonObject obj) {
        if (isOpen()) send(GSON.toJson(obj));
    }

    public void login(String username) {
        lastLoginUsername = username;
        JsonObject o = new JsonObject();
        o.addProperty("type", "LOGIN");
        o.addProperty("username", username);
        sendJson(o);
    }

    public void sendFriendRequest(String target) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "FRIEND_REQUEST");
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
        o.addProperty("type", "FRIEND_REMOVE");
        o.addProperty("username", username);
        sendJson(o);
    }

    public void sendInvite(String target) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "INVITE");
        o.addProperty("target", target);
        sendJson(o);
    }

    public void acceptInvite(String from) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "INVITE_ACCEPT");
        o.addProperty("from", from);
        sendJson(o);
    }

    public void sendTunnel(String data) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "TUNNEL");
        o.addProperty("data", data);
        sendJson(o);
    }

    public static void forceReturnToTitle() {
        Minecraft mc = Minecraft.getInstance();
        FORCE_TITLE = true;
        UI_BLOCKED = false;
        mc.execute(() -> mc.setScreen(new TitleScreen(false, null)));
    }
}