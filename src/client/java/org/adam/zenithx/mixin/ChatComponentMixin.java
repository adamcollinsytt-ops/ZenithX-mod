package org.adam.zenithx.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import org.adam.zenithx.handlers.OnlineIndicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @Inject(
            method = "addPlayerMessage",
            at = @At("HEAD")
    )
    private void onAddPlayerMessage(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag tag, CallbackInfo ci) {
        OnlineIndicator.trackChatMessage(message);
    }
}