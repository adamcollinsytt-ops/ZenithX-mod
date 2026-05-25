package org.adam.zenithx.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.network.chat.Component;
import org.adam.zenithx.handlers.OnlineIndicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatIndicatorMixin {

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD")
    )
    private void onAddMessage(Component message, CallbackInfo ci) {
        OnlineIndicator.trackChatMessage(message);
    }

    @Inject(
            method = "render",
            at = @At("HEAD")
    )
    private void onRender(Object graphics, int ticks, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        OnlineIndicator.onChatRender(graphics);
    }
}