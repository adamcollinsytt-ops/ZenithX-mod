package org.adam.zenithx.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import org.adam.zenithx.handlers.OnlineIndicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class NametagMixin {

    @Inject(
            method = "renderNameTag",
            at = @At("HEAD")
    )
    private void onRenderNameTag(Object context, Entity entity, Component displayName, Object matrixStack, Object bufferSource, int light, float partialTick, CallbackInfo ci) {
        OnlineIndicator.currentlyDrawingPlayerEntityName.set(true);
    }

    @Inject(
            method = "renderNameTag",
            at = @At("TAIL")
    )
    private void onRenderNameTagEnd(Object context, Entity entity, Component displayName, Object matrixStack, Object bufferSource, int light, float partialTick, CallbackInfo ci) {
        OnlineIndicator.currentlyDrawingPlayerEntityName.set(false);
    }
}