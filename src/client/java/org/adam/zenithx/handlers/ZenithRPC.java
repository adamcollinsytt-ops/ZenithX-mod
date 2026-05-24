package org.adam.zenithx.handlers;

import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRichPresence;
import net.minecraft.client.Minecraft;

public class ZenithRPC {

    private static final String CLIENT_ID = "1505258118635061368";
    private static boolean running = false;
    private static Thread thread;
    private static long SESSION_START = 0;

    public static void init() {
        try {
            if (running) return;
            running = true;
            SESSION_START = System.currentTimeMillis();

            DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().build();
            DiscordRPC.discordInitialize(CLIENT_ID, handlers, true);

            thread = new Thread(() -> {
                while (running) {
                    try {
                        DiscordRPC.discordRunCallbacks();
                        update();
                    } catch (Exception ignored) {}
                    try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                }
            }, "ZenithX-RPC");
            thread.setDaemon(true);
            thread.start();
        } catch (Exception e) {
            System.err.println("[ZenithX] Discord RPC init failed: " + e.getMessage());
        }
    }

    public static void update() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        String username = mc.getUser().getName();
        String uuid = mc.getUser().getProfileId().toString().replace("-", "");
        String faceUrl = "https://mc-heads.net/avatar/" + uuid + "/64";

        String state;
        if (mc.level != null) {
            if (mc.getSingleplayerServer() != null) {
                state = "Singleplayer";
            } else if (mc.getCurrentServer() != null) {
                state = "Playing on " + mc.getCurrentServer().name;
            } else {
                state = "Multiplayer";
            }
        } else {
            state = "Main Menu";
        }

        DiscordRichPresence presence = new DiscordRichPresence.Builder(state)
                .setDetails("ZenithX - Minecraft 26.1.2")
                .setStartTimestamps(SESSION_START)
                .setBigImage("zenithx", "ZenithX Client")
                .setSmallImage(faceUrl, username)
                .build();

        DiscordRPC.discordUpdatePresence(presence);
    }

    public static void shutdown() {
        running = false;
        DiscordRPC.discordShutdown();
    }
}