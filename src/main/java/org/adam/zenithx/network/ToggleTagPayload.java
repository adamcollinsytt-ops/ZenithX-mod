package org.adam.zenithx.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record ToggleTagPayload(UUID playerUuid, boolean visible) implements CustomPacketPayload {
    public static final Type<ToggleTagPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("zenithx", "toggle_tag"));

    public ToggleTagPayload(boolean visible) {
        this(new UUID(0L, 0L), visible);
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
        (buf, uuid) -> buf.writeUUID(uuid),
        buf -> buf.readUUID()
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleTagPayload> CODEC = StreamCodec.composite(
        UUID_CODEC, ToggleTagPayload::playerUuid,
        ByteBufCodecs.BOOL, ToggleTagPayload::visible,
        ToggleTagPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}