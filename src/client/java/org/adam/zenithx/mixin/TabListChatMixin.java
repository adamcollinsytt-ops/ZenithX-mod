package org.adam.zenithx.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.adam.zenithx.handlers.OnlineIndicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class TabListChatMixin {

    @Inject(
            method = "renderEntry",
            at = @At("TAIL")
    )
    private void onRenderEntry(Object graphics, PlayerInfo playerInfo, int x, int y, int entryWidth, CallbackInfo ci) {
        OnlineIndicator.drawTabIndicator(graphics, playerInfo, x, y);
    }
}