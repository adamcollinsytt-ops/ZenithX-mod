package org.adam.zenithx.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.adam.zenithx.ui.notification.NotificationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(
            method = "extractGui",
            at = @At("TAIL")
    )
    private void onExtractGui(DeltaTracker deltaTracker, boolean shouldRenderLevel,
                              boolean resourcesLoaded, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        float deltaSeconds = deltaTracker.getGameTimeDeltaTicks() / 20f;
        NotificationManager.tick(deltaSeconds);

        int mouseX = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int mouseY = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());

        GameRenderState gameRenderState = ((GameRendererAccessor) (Object) this).getGameRenderState();
        GuiRenderState guiRenderState = gameRenderState.guiRenderState;

        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(mc, guiRenderState, mouseX, mouseY);

        NotificationManager.render(graphics, mouseX, mouseY);
    }
}