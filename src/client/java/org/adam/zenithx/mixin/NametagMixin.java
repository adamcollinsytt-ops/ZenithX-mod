package org.adam.zenithx.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import org.adam.zenithx.handlers.OnlineIndicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class NametagMixin {

    @Inject(
            method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD")
    )
    private void onSubmitNameDisplay(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        if (state.nameTag == null) return;
        String[] parts = state.nameTag.getString().split("[^\\w]+");
        for (String part : parts) {
            OnlineIndicator.currentlyDrawingPlayerEntityName.set(true);
            return;
        }
        OnlineIndicator.currentlyDrawingPlayerEntityName.set(false);
    }

    @Inject(
            method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("TAIL")
    )
    private void onSubmitNameDisplayEnd(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        OnlineIndicator.currentlyDrawingPlayerEntityName.set(false);
    }
}