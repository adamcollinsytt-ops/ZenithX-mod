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

public class HostMain implements ClientModInitializer {

    private static String currentUsername;
    private static boolean connecting = false;

    @Override
    public void onInitializeClient() {
        System.out.println("[ZenithX] Init");
    }

    public static synchronized void connect(String username) {
        currentUsername = username;

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
            FriendManager.getInstance().clear();
            GameData.getInstance().stop();
            System.out.println("[ZenithX] Disconnected");
        });

        if (client.isOpen()) {
            client.login(username);
            return;
        }
    }

    public static void handleMessage(JsonObject msg) {

        if (!msg.has("type")) return;

        String type = msg.get("type").getAsString();

        switch (type) {

            case "LOGIN_OK" -> {

                FriendManager.getInstance().clear();

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

                System.out.println("[ZenithX] ONLINE_LIST RECEIVED");

                if (msg.has("players") && msg.get("players").isJsonArray()) {

                    JsonArray arr = msg.getAsJsonArray("players");

                    List<String> online = new ArrayList<>();

                    for (int i = 0; i < arr.size(); i++) {

                        String name = arr.get(i).getAsString();

                        System.out.println("[ZenithX] ONLINE: " + name);

                        online.add(name);
                    }

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
                    NotificationManager.show(
                            new Notification(
                                    "ZenithX",
                                    "You Received Invite From " + from,
                                    3.5f,
                                    false,
                                    "invite_recv_" + from,
                                    () -> true,
                                    null,
                                    null,
                                    null,
                                    null
                            )
                    );
                });
            }

            case "SESSION_START" -> {
                String role = msg.get("role").getAsString();

                Minecraft mc = Minecraft.getInstance();

                if (role.equals("HOST")) {

                    mc.execute(() -> {
                        new Thread(() -> {

                            try {

                                int tries = 0;

                                while (Minecraft.getInstance().getSingleplayerServer() == null && tries++ < 80) {
                                    Thread.sleep(250);
                                }

                                var srv = Minecraft.getInstance().getSingleplayerServer();

                                if (srv == null) {
                                    System.err.println("[ZenithX] No singleplayer server found!");
                                    return;
                                }

                                Thread.sleep(500);

                                boolean published = srv.publishServer(
                                        HostSessionConfig.getGameMode(),
                                        HostSessionConfig.hasCheats(),
                                        25565
                                );

                                srv.setDifficulty(
                                        HostSessionConfig.getDifficulty(),
                                        true
                                );

                                System.out.println("[ZenithX] publish result: " + published);

                                Thread.sleep(500);

                                int port = srv.getPort();

                                if (port != 25565) {
                                    System.err.println("[ZenithX] Wrong port detected: " + port);
                                }

                                GameData.setPort(25565);

                                GameData.getInstance().startHostRelay();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }).start();
                    });

                } else {

                    int port = msg.has("port") ? msg.get("port").getAsInt() : 25565;

                    GameData.setPort(port);
                    GameData.getInstance().startGuest();

                    mc.execute(() -> {
                        new Thread(() -> {
                            try {
                                Thread.sleep(600);
                            } catch (Exception ignored) {}

                            mc.execute(() -> {

                                String address = "127.0.0.1:" + GameData.getPort();

                                ServerData serverData = new ServerData(
                                        "ZenithX Session",
                                        address,
                                        ServerData.Type.OTHER
                                );

                                ServerAddress addr = ServerAddress.parseString(address);

                                ConnectScreen.startConnecting(
                                        mc.screen,
                                        mc,
                                        addr,
                                        serverData,
                                        false,
                                        null
                                );
                            });
                        }).start();
                    });
                }
            }

            case "TUNNEL" ->
                    GameData.getInstance().receiveData(msg.get("data").getAsString());

            case "SESSION_END" ->
                    GameData.getInstance().stop();

            case "ERROR" ->
                    System.err.println("[ZenithX] Server: " + msg.get("message").getAsString());
        }
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }
}