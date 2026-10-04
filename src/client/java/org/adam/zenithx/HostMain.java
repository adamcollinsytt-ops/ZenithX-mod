package org.adam.zenithx;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.Game;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.gui.screens.ConnectScreen;
import org.adam.zenithx.compat.CompatibilityManager;
import org.adam.zenithx.compat.CompatibilityResult;
import org.adam.zenithx.compat.CompatibilitySnapshot;
import org.adam.zenithx.ui.CompatibilityScreen;
import org.adam.zenithx.ui.notification.Notification;
import org.adam.zenithx.ui.notification.NotificationManager;
import org.adam.zenithx.ui.FriendsScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HostMain implements ClientModInitializer {

    private static String currentUsername;
    private static boolean connecting = false;
    
    private static final AtomicBoolean isConnectingOrConnected = new AtomicBoolean(false);
    private static final AtomicBoolean isConnectingToGame = new AtomicBoolean(false);
    private static long lastSessionStartTimestamp = 0;

    @Override
    public void onInitializeClient() {
        System.out.println("[ZenithX] Init");
    }

    public static synchronized void connect(String username) {
        HostClient client = HostClient.getInstance();

        if (client.isOpen() || isConnectingOrConnected.get()) {
            System.out.println("[ZenithX] Connection already open or in progress, skipping duplicate connect request.");
            if (client.isOpen() && HostClient.lastLoginUsername == null) {
                currentUsername = username;
                client.login(username);
            }
            return;
        }

        isConnectingOrConnected.set(true);
        currentUsername = username;
        connecting = true;

        new Thread(() -> {
            client.setMessageHandler(HostMain::handleMessage);
            client.setOnConnect(() -> {
                connecting = false;
                System.out.println("[ZenithX] onConnect fired for: " + username);
                if (HostClient.lastLoginUsername == null) {
                    client.login(username);
                }
            });

            client.setOnDisconnect(() -> {
                connecting = false;
                isConnectingOrConnected.set(false);
                isConnectingToGame.set(false);
                client.stopPingLoop();
                FriendManager.getInstance().clear();
                GameData.getInstance().stop(false);
                System.out.println("[ZenithX] Disconnected");
            });

            try {
                client.connect();
            } catch (Exception e) {
                connecting = false;
                isConnectingOrConnected.set(false);
                System.err.println("[ZenithX] Connection failed: " + e.getMessage());
            }
        }, "zenithx-connect").start();
    }

    public static void handleMessage(JsonObject msg) {
        if (!msg.has("type")) return;
        String type = msg.get("type").getAsString();

        switch (type) {
            case "LOGIN_OK" -> {
                FriendManager.getInstance().clear();
                HostClient.getInstance().startPingLoop();
                HostClient.startUpdateChecker();
                System.out.println("[ZenithX] LOGIN_OK as " + currentUsername);
                if (msg.has("friends") && msg.get("friends").isJsonArray()) {
                    JsonArray arr = msg.getAsJsonArray("friends");
                    for (int i = 0; i < arr.size(); i++) {
                        FriendManager.getInstance().addFriend(arr.get(i).getAsString());
                    }
                }
                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().screen instanceof FriendsScreen fs) fs.publicrefresh2026byadam();
                });
            }
            case "ONLINE_LIST" -> {
                if (msg.has("players") && msg.get("players").isJsonArray()) {
                    JsonArray arr = msg.getAsJsonArray("players");
                    List<String> online = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) online.add(arr.get(i).getAsString());
                    FriendManager.getInstance().setOnlinePlayers(online);
                }
            }
            case "INVITE" -> {
                String from = firstNonEmpty(msg, "from", "sender", "username", "inviter");
                if (from == null) from = "a friend";
                FriendManager.getInstance().addPendingInvite(from);
                final String fromFinal = from;
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    NotificationManager.show(new Notification(
                            "ZenithX", "You Received Invite From " + fromFinal, 3.5f, false, "invite_recv_" + fromFinal, () -> true, null, null, null, null
                    ));
                });
            }
            case "SESSION_START" -> {
                long now = System.currentTimeMillis();
                if (isConnectingToGame.get() || (now - lastSessionStartTimestamp < 2000)) {
                    System.out.println("[ZenithX] Ignoring duplicate or rapid SESSION_START packet.");
                    return;
                }
                isConnectingToGame.set(true);
                lastSessionStartTimestamp = now;

                if (GameData.getInstance().isRunning()) {
                    System.out.println("[ZenithX] GameData is already running, skipping restart.");
                    isConnectingToGame.set(false);
                    return;
                }

                String role = msg.get("role").getAsString();
                Minecraft mc = Minecraft.getInstance();

                if (role.equals("HOST")) {
                    GameData.setPort(25565);
                    mc.execute(() -> {
                        new Thread(() -> {
                            try {
                                System.out.println("[ZenithX] HOST session starting, waiting for singleplayer server...");
                                int tries = 0;
                                while (mc.getSingleplayerServer() == null && tries++ < 120) {
                                    Thread.sleep(250);
                                }
                                var srv = mc.getSingleplayerServer();
                                if (srv == null) {
                                    System.err.println("[ZenithX] HOST FAILED: no singleplayer world was open after "
                                            + (tries * 250) + "ms. You must have a world open (via Host World) "
                                            + "before the relay sends SESSION_START.");
                                    isConnectingToGame.set(false);
                                    GameData.getInstance().stop(true);
                                    return;
                                }
                                System.out.println("[ZenithX] Singleplayer server found after " + (tries * 250) + "ms, publishing...");

                                Thread.sleep(500);
                                srv.setUsesAuthentication(false);
                                srv.setDefaultGameType(HostSessionConfig.getGameMode());
                                srv.setDifficulty(HostSessionConfig.getDifficulty(), true);
                                
                                try {
                                    boolean published = srv.publishServer(HostSessionConfig.getGameMode(), HostSessionConfig.hasCheats(), 25565);
                                    System.out.println("[ZenithX] publishServer() returned: " + published);
                                } catch (Exception ex) {
                                    System.err.println("[ZenithX] publishServer() threw: " + ex);
                                    ex.printStackTrace();
                                }
                                
                                GameData.setPort(25565);
                                GameData.getInstance().startHostRelay();
                                CompatibilityManager.publishLocalSnapshotAsHost();
                                System.out.println("[ZenithX] HOST relay started, connecting to local LAN port 25565...");

                            } catch (Exception e) {
                                System.err.println("[ZenithX] HOST session setup threw: " + e);
                                e.printStackTrace();
                                isConnectingToGame.set(false);
                                GameData.getInstance().stop(true);
                            }
                        }, "zenithx-host-setup").start();
                    });

                } else {
                    String hostAddress = msg.has("hostAddress") && !msg.get("hostAddress").isJsonNull()
                            ? msg.get("hostAddress").getAsString() : null;
                    int hostPort = msg.has("port") ? msg.get("port").getAsInt() : 25565;

                    new Thread(() -> {
                        // --- Attempt 1: direct P2P to the host's public address ---
                        // Succeeds only if the host's router forwards/UPnP-maps the
                        // port, or isn't behind NAT. CGNAT and most unconfigured home
                        // routers will fail this quickly (expected) and fall through
                        // to the relay below - this is not real NAT hole-punching.
                        if (hostAddress != null && !hostAddress.isBlank() && tryDirectConnect(hostAddress, hostPort)) {
                            System.out.println("[ZenithX] Direct P2P connection to " + hostAddress + ":" + hostPort + " succeeded, skipping relay.");
                            mc.execute(() -> {
                                try {
                                    String address = hostAddress + ":" + hostPort;
                                    ConnectScreen.startConnecting(
                                            mc.screen, mc, ServerAddress.parseString(address),
                                            new ServerData("ZenithX Session (Direct)", address, ServerData.Type.OTHER), false, null
                                    );
                                } finally {
                                    isConnectingToGame.set(false);
                                }
                            });
                            return;
                        }
                        System.out.println("[ZenithX] Direct P2P unavailable (no route, timeout, or NAT) - falling back to relay.");

                        // --- Attempt 2: relay fallback (unchanged from before) ---
                        GameData.setPort(24499);
                        try {
                            GameData.getInstance().startGuest();
                            boolean ready = GameData.getInstance().getGuestReadyLatch().await(25, TimeUnit.SECONDS);
                            if (!ready) {
                                isConnectingToGame.set(false);
                                GameData.getInstance().stop(true);
                                return;
                            }

                            System.out.println("[ZenithX] Giving Host a small head start...");
                            Thread.sleep(3000);

                        } catch (Exception e) {
                            isConnectingToGame.set(false);
                            GameData.getInstance().stop(true);
                            return;
                        }

                        CompatibilitySnapshot hostSnapshot = null;
                        try { hostSnapshot = CompatibilityManager.awaitHostSnapshot(3).get(10, TimeUnit.SECONDS); } 
                        catch (Exception ignored) {}

                        Runnable doConnect = () -> mc.execute(() -> {
                            try {
                                if (mc.getCurrentServer() == null || !mc.getCurrentServer().ip.contains("24499")) {
                                    String address = "127.0.0.1:24499";
                                    ConnectScreen.startConnecting(
                                            mc.screen, mc, ServerAddress.parseString(address),
                                            new ServerData("ZenithX Session", address, ServerData.Type.OTHER), false, null
                                    );
                                }
                            } finally {
                                isConnectingToGame.set(false);
                            }
                        });

                        CompatibilityResult result = CompatibilityManager.lastResult();
                        if (hostSnapshot == null || result == null || result.problemCount() == 0) {
                            doConnect.run();
                        } else {
                            mc.execute(() -> mc.setScreen(new CompatibilityScreen(result, doConnect, () -> {
                                isConnectingToGame.set(false);
                                GameData.getInstance().stop(true);
                            })));
                        }
                    }, "zenithx-guest-setup").start();
                }
            }
            case "SESSION_END" -> {
                isConnectingToGame.set(false);
                GameData.getInstance().stop(false);
                System.out.println("[ZenithX] Session ended by remote.");
            }
        }
    }

    public static String getCurrentUsername() { return currentUsername; }

    private static boolean tryDirectConnect(String address, int port) {
        try (java.net.Socket probe = new java.net.Socket()) {
            probe.connect(new java.net.InetSocketAddress(address, port), 2500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    private static String firstNonEmpty(JsonObject msg, String... keys) {
        for (String key : keys) {
            try {
                if (msg.has(key) && !msg.get(key).isJsonNull()) {
                    String v = msg.get(key).getAsString();
                    if (v != null && !v.isBlank()) return v;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}