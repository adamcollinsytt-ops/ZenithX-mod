package org.adam.zenithx.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.adam.zenithx.handlers.OnlineIndicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {

    @Inject(
            method = "extractRenderState",
            at = @At("TAIL")
    )
    private void onExtractRenderState(GuiGraphicsExtractor graphics, int screenWidth, Scoreboard scoreboard, @Nullable Objective displayObjective, CallbackInfo ci) {
        OnlineIndicator.drawTabListOverlay(graphics, (PlayerTabOverlay)(Object)this, screenWidth);
    }
}