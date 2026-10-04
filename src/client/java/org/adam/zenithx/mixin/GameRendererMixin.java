package org.adam.zenithx.mixin;

//? if >=26 {
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
//? } else if >=1.20.5 {
/*import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.adam.zenithx.ui.notification.NotificationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GameRendererMixin {

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void onRenderHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        float deltaSeconds = deltaTracker.getGameTimeDeltaTicks() / 20f;
        NotificationManager.tick(deltaSeconds);

        int mouseX = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int mouseY = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());

        NotificationManager.render(guiGraphics, mouseX, mouseY);
    }
}*/
//? } else {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.adam.zenithx.ui.notification.NotificationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GameRendererMixin {

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void onRenderHud(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        float deltaSeconds = partialTick / 20f;
        NotificationManager.tick(deltaSeconds);

        int mouseX = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int mouseY = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());

        NotificationManager.render(guiGraphics, mouseX, mouseY);
    }
}*/
//? }
