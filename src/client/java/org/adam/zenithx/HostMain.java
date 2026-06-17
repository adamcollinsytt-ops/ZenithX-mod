package org.adam.zenithx;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.gui.screens.ConnectScreen;
import org.adam.zenithx.ui.notification.Notification;
import org.adam.zenithx.ui.notification.NotificationManager;
import org.adam.zenithx.ui.FriendsScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HostMain implements ClientModInitializer {

    private static String  currentUsername;
    private static boolean connecting = false;

    @Override
    public void onInitializeClient() {
        System.out.println("[ZenithX] Init");
    }

    public static synchronized void connect(String username) {
        currentUsername = username;
        new Thread(() -> {
            HostClient client = HostClient.getInstance();
            client.setMessageHandler(HostMain::handleMessage);

            client.setOnConnect(() -> {
                connecting = false;
                System.out.println("[ZenithX] onConnect fired for: " + username);
                if (HostClient.lastLoginUsername == null) {
                    HostClient.getInstance().login(username);
                }
            });

            client.setOnDisconnect(() -> {
                connecting = false;
                HostClient.getInstance().stopPingLoop();
                FriendManager.getInstance().clear();
                GameData.getInstance().stop();
                System.out.println("[ZenithX] Disconnected");
            });
        }, "zenithx-connect").start();

        HostClient client = HostClient.getInstance();

        if (client != null && client.isOpen()) {
            client.login(username);
        }
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
                        String friend = arr.get(i).getAsString();
                        System.out.println("[ZenithX] FRIEND: " + friend);
                        FriendManager.getInstance().addFriend(friend);
                    }
                    System.out.println("[ZenithX] Friends loaded: " + arr.size());
                }
                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().screen instanceof FriendsScreen fs) {
                        fs.publicrefresh2026byadam();
                    }
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
            case "FRIEND_REQUEST" -> {
                String from = msg.get("from").getAsString();
                System.out.println("[ZenithX] Friend request from: " + from);
                FriendManager.getInstance().addPendingRequest(from);
            }
            case "FRIEND_ADDED" -> {
                String name = msg.get("username").getAsString();
                System.out.println("[ZenithX] FRIEND_ADDED: " + name);
                FriendManager.getInstance().addFriend(name);
            }
            case "FRIEND_REMOVED" -> {
                String name = msg.get("username").getAsString();
                System.out.println("[ZenithX] FRIEND_REMOVED: " + name);
                FriendManager.getInstance().removeFriend(name);
            }
            case "INVITE" -> {
                String from = msg.get("from").getAsString();
                FriendManager.getInstance().addPendingInvite(from);
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    NotificationManager.show(new Notification(
                            "ZenithX",
                            "You Received Invite From " + from,
                            3.5f,
                            false,
                            "invite_recv_" + from,
                            () -> true,
                            null, null, null, null
                    ));
                });
            }
            case "SESSION_START" -> {
                if (GameData.getInstance().isRunning()) {
                    GameData.getInstance().stop();
                }
                String role = msg.get("role").getAsString();
                Minecraft mc = Minecraft.getInstance();

                if (role.equals("HOST")) {
                    mc.execute(() -> {
                        new Thread(() -> {
                            try {
                                int tries = 0;
                                while (mc.getSingleplayerServer() == null && tries++ < 80)
                                    Thread.sleep(250);
                                var srv = mc.getSingleplayerServer();
                                if (srv == null) return;

                                Thread.sleep(500);
                                srv.setUsesAuthentication(false);
                                srv.setDefaultGameType(HostSessionConfig.getGameMode());
                                srv.setDifficulty(HostSessionConfig.getDifficulty(), true);
                                srv.publishServer(HostSessionConfig.getGameMode(), HostSessionConfig.hasCheats(), 25565);
                                System.out.println("AUTH=" + srv.usesAuthentication());

                                if (HostSessionConfig.hasCheats()) {
                                    var profile = mc.player.getGameProfile();
                                    net.minecraft.server.players.NameAndId nameAndId =
                                            new net.minecraft.server.players.NameAndId(profile.id(), profile.name());
                                    srv.getPlayerList().op(nameAndId);
                                    System.out.println("[ZenithX] OP granted to: " + profile.name());
                                }

                                GameData.setPort(25565);
                                GameData.getInstance().startHostRelay();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    });

                } else {
                    GameData.setPort(24499);
                    new Thread(() -> {
                        try {
                            GameData.getInstance().startGuest();
                            boolean ready = GameData.getInstance()
                                    .getGuestReadyLatch().await(15, TimeUnit.SECONDS);
                            if (!ready) {
                                System.err.println("[ZenithX] Guest relay timed out");
                                GameData.getInstance().stop();
                                return;
                            }
                        } catch (Exception e) {
                            System.err.println("[ZenithX] Guest start failed: " + e.getMessage());
                            return;
                        }
                        mc.execute(() -> {
                            try { Thread.sleep(200); } catch (Exception ignored) {}
                            String address = "127.0.0.1:24499";
                            ServerData serverData = new ServerData("ZenithX Session", address, ServerData.Type.OTHER);
                            ConnectScreen.startConnecting(
                                    mc.screen, mc,
                                    ServerAddress.parseString(address),
                                    serverData, false, null
                            );
                        });
                    }).start();
                }
            }
            case "HOST_READY" -> {
                GameData.getInstance().signalHostReady();
            }
            case "SESSION_END" -> {
                GameData.getInstance().stop();
                HostClient.getInstance().sendSessionEnd();
            }
            case "ERROR" -> System.err.println("[ZenithX] Server: " + msg.get("message").getAsString());
        }
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }
}