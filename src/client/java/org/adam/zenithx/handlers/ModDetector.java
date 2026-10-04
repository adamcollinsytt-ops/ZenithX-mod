package org.adam.zenithx.handlers;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? } else {
/*import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;*/
//? }

//? if >=26 {
import net.minecraft.resources.Identifier;
//? } else {
/*import net.minecraft.resources.ResourceLocation;*/
//? }

public class ModDetector {

    //? if >=26 {
    public static final Identifier CHANNEL_ID =
            Identifier.fromNamespaceAndPath("zenithx", "handshake");
    //? } else if >=1.20.5 {
    /*public static final ResourceLocation CHANNEL_ID =
            ResourceLocation.fromNamespaceAndPath("zenithx", "handshake");*/
    //? } else {
    /*public static final ResourceLocation CHANNEL_ID =
            new ResourceLocation("zenithx", "handshake");*/
    //? }

    public static void register() {

        //? if >=1.20.5 {
        PayloadTypeRegistry.serverboundPlay().register(
                HandshakePayload.TYPE,
                HandshakePayload.STREAM_CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                HandshakePayload.TYPE,
                HandshakePayload.STREAM_CODEC
        );

        ClientPlayNetworking.registerGlobalReceiver(
                HandshakePayload.TYPE,
                (payload, context) -> {

                    String playerName =
                            context.player().getGameProfile().name();

                    //OnlineIndicator.ONLINE_MOD_PLAYERS.add(playerName);
                }
        );
        //? } else {
        /*ClientPlayNetworking.registerGlobalReceiver(
                CHANNEL_ID,
                (client, handler, buf, responseSender) -> {

                    String playerName = client.getUser().getName();

                    client.execute(() -> {
                        //OnlineIndicator.ONLINE_MOD_PLAYERS.add(playerName);
                    });
                }
        );*/
        //? }

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {

            //? if >=1.20.5 {
            if (ClientPlayNetworking.canSend(HandshakePayload.TYPE)) {
                ClientPlayNetworking.send(new HandshakePayload());
            }
            //? } else {
            /*if (ClientPlayNetworking.canSend(CHANNEL_ID)) {
                ClientPlayNetworking.send(CHANNEL_ID, PacketByteBufs.empty());
            }*/
            //? }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            //OnlineIndicator.ONLINE_MOD_PLAYERS.clear();
        });
    }

    //? if >=1.20.5 {
    public record HandshakePayload() implements CustomPacketPayload {

        public static final Type<HandshakePayload> TYPE =
                new Type<>(CHANNEL_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, HandshakePayload> STREAM_CODEC =
                StreamCodec.unit(new HandshakePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    //? }
}
