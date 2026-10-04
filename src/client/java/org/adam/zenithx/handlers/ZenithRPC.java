package org.adam.zenithx.handlers;

import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRichPresence;
import net.minecraft.client.Minecraft;

public class ZenithRPC {

    private static final String CLIENT_ID = "1505258118635061368";
    private static boolean running = false;
    private static boolean rpcSupported = true;
    private static Thread thread;
    private static long SESSION_START = 0;

    public static void init() {
        if (running) return;

        try {
            SESSION_START = System.currentTimeMillis();

            DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().build();
            DiscordRPC.discordInitialize(CLIENT_ID, handlers, true);

            running = true;

            thread = new Thread(() -> {
                while (running && rpcSupported) {
                    try {
                        DiscordRPC.discordRunCallbacks();
                        update();
                    } catch (Throwable ignored) {}

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {}
                }
            }, "ZenithX-RPC");

            thread.setDaemon(true);
            thread.start();

        } catch (UnsatisfiedLinkError e) {
            rpcSupported = false;
            running = false;
            System.err.println("[ZenithX] Discord RPC disabled (Native library already loaded in another client instance).");
        } catch (Throwable t) {
            rpcSupported = false;
            running = false;
            System.err.println("[ZenithX] Discord RPC failed to initialize: " + t.getMessage());
        }
    }

    public static void update() {
        if (!running || !rpcSupported) return;

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getUser() == null) return;

            String username = mc.getUser().getName();
            String uuid = mc.getUser().getProfileId() != null 
                    ? mc.getUser().getProfileId().toString().replace("-", "") 
                    : "";
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
        } catch (Throwable ignored) {
            // Prevents thread crashes if the game state changes suddenly
        }
    }

    public static void shutdown() {
        if (!running || !rpcSupported) return;
        running = false;
        try {
            DiscordRPC.discordShutdown();
        } catch (Throwable ignored) {}
    }
}