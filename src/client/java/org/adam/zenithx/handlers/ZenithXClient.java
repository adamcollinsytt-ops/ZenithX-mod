package org.adam.zenithx.handlers;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import org.adam.zenithx.HostClient;
import org.adam.zenithx.Servers;
import org.adam.zenithx.network.ToggleTagPayload;
import org.adam.zenithx.ui.NoConnectionWarningScreen;
import org.adam.zenithx.util.AuthUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ZenithXClient implements ClientModInitializer {

    public static volatile boolean connectionChecked = false;
    private static volatile boolean isConnecting = false;

    private static boolean showMyTag = true;
    private static final Map<UUID, Boolean> PLAYERS_TAG_VISIBILITY = new ConcurrentHashMap<>();

    public static boolean toggleTag() {
        showMyTag = !showMyTag;
        return showMyTag;
    }

    public static boolean isTagVisible() {
        return showMyTag;
    }

    public static boolean isTagVisibleForPlayer(UUID uuid) {
        return PLAYERS_TAG_VISIBILITY.getOrDefault(uuid, true);
    }

    public static void setTagVisibleForPlayer(UUID uuid, boolean visible) {
        PLAYERS_TAG_VISIBILITY.put(uuid, visible);
    }

    @Override
    public void onInitializeClient() {
        System.out.println("[ZenithX] Initializing ZenithX Client...");

        ZenithRPC.init();
        Settings.init();
        ModDetector.register();
        HostSettingsApplier.register();

        try {
            PayloadTypeRegistry.serverboundPlay().register(ToggleTagPayload.TYPE, ToggleTagPayload.CODEC);
            PayloadTypeRegistry.clientboundPlay().register(ToggleTagPayload.TYPE, ToggleTagPayload.CODEC);
        } catch (NoSuchMethodError | Exception e) {
            System.out.println("[ZenithX] PayloadTypeRegistry bypass active or handled automatically.");
        }

        registerGameEvents();
        registerCommands();
        registerNetworkListeners();

        System.out.println("[ZenithX] ZenithX Client initialized successfully!");
    }

    private void registerNetworkListeners() {
        ClientPlayNetworking.registerGlobalReceiver(ToggleTagPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                setTagVisibleForPlayer(payload.playerUuid(), payload.visible());
            });
        });
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var client = net.minecraft.client.Minecraft.getInstance();
            
            if (client.getUser() == null || !client.getUser().getName().equals("Adam_CollinsYT")) {
                return; 
            }

            dispatcher.register(
                LiteralArgumentBuilder.<FabricClientCommandSource>literal("toggletagzenithxmod")
                    .executes(context -> {
                        if (client.player == null) return 0;

                        UUID myUuid = client.player.getUUID();
                        
                        boolean currentVisibility = isTagVisibleForPlayer(myUuid);
                        boolean newVisibility = !currentVisibility;

                        setTagVisibleForPlayer(myUuid, newVisibility);

                        try {
                            ClientPlayNetworking.send(new ToggleTagPayload(myUuid, newVisibility));
                        } catch (Exception e) {
                            System.err.println("[ZenithX] Failed to send tag packet: " + e.getMessage());
                        }

                        if (newVisibility) {
                            context.getSource().sendFeedback(
                                Component.literal("[ZenithX] Custom Role Tag is now ENABLED.")
                                    .withStyle(ChatFormatting.GREEN)
                            );
                        } else {
                            context.getSource().sendFeedback(
                                Component.literal("[ZenithX] Custom Role Tag is now DISABLED.")
                                    .withStyle(ChatFormatting.RED)
                            );
                        }

                        return 1;
                    })
            );
        });
    }

    private void registerGameEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Settings.tick();

            if (!connectionChecked && !isConnecting) {
                synchronized (ZenithXClient.class) {
                    if (!connectionChecked && !isConnecting) {
                        
                        if (client.getUser() == null) {
                            return;
                        }

                        if (!AuthUtils.isOfficialAccount()) {
                            connectionChecked = true;

                            throw new RuntimeException("ZenithX - Authentication Required: Please log in with an official Microsoft account!");
                        }

                        isConnecting = true;

                        Thread connectThread = new Thread(() -> {
                            try {
                                Servers.fetchServers();

                                boolean success = HostClient.connectToAnyServer();

                                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

                                HostClient currentInst = HostClient.getInstance();
                                boolean isReallyOpen = (currentInst != null && currentInst.isOpen());

                                if (!success || !isReallyOpen) {
                                    if (HostClient.getLastFailReason() == null) {
                                        HostClient.setLastFailReason("connection_lost");
                                    }

                                    client.execute(() -> {
                                        client.setScreen(new NoConnectionWarningScreen(
                                            () -> client.setScreen(new TitleScreen()),
                                            () -> {
                                                HostClient.setLastFailReason(null);
                                                connectionChecked = false;
                                                isConnecting = false;
                                            },
                                            HostClient.getLastFailReason()
                                        ));
                                    });
                                } else {
                                    connectionChecked = true;
                                }
                            } finally {
                                isConnecting = false;
                            }
                        }, "zenithx-connect");

                        connectThread.setDaemon(true);
                        connectThread.start();
                    }
                }
            }
        });
    }

    public static void cleanup() {
        try {
            System.out.println("[ZenithX] Cleaning up resources...");
            connectionChecked = false;
            isConnecting = false;
            System.out.println("[ZenithX] Cleanup complete!");
        } catch (Exception e) {
            System.err.println("[ZenithX] Error during cleanup: " + e.getMessage());
        }
    }
}