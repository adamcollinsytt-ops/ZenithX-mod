package org.adam.zenithx.handlers;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class ModDetector {

    public static final Identifier CHANNEL_ID =
            Identifier.fromNamespaceAndPath("zenithx", "handshake");

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(HandshakePayload.TYPE, (payload, context) -> {
            String playerName = context.player().getGameProfile().name();
            OnlineIndicator.ONLINE_MOD_PLAYERS.add(playerName);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (ClientPlayNetworking.canSend(HandshakePayload.TYPE)) {
                ClientPlayNetworking.send(new HandshakePayload());
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            OnlineIndicator.ONLINE_MOD_PLAYERS.clear();
        });
    }

    public record HandshakePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HandshakePayload> TYPE =
                new CustomPacketPayload.Type<>(CHANNEL_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, HandshakePayload> STREAM_CODEC =
                StreamCodec.unit(new HandshakePayload());

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}