package org.adam.zenithx.handlers;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import org.adam.zenithx.network.ToggleTagPayload;

public class ZenithXServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        System.out.println("[ZenithX] Initializing ZenithX Server...");

        PayloadTypeRegistry.serverboundPlay().register(ToggleTagPayload.TYPE, ToggleTagPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ToggleTagPayload.TYPE, ToggleTagPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ToggleTagPayload.TYPE, (payload, context) -> {
            ServerPlayer sender = context.player();
            
            context.server().execute(() -> {
                boolean isVisible = payload.visible();
                System.out.println("[ZenithX] Received Tag Update from: " + sender.getName().getString() + " -> " + isVisible);

                ToggleTagPayload broadcastPayload = new ToggleTagPayload(sender.getUUID(), isVisible);

                for (ServerPlayer targetPlayer : context.server().getPlayerList().getPlayers()) {
                    if (ServerPlayNetworking.canSend(targetPlayer, ToggleTagPayload.TYPE)) {
                        ServerPlayNetworking.send(targetPlayer, broadcastPayload);
                    }
                }
            });
        });

        System.out.println("[ZenithX] ZenithX Server initialized successfully!");
    }
}