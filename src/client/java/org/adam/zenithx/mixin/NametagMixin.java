package org.adam.zenithx.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
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

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        String nameText = state.nameTag.getString();
        String[] parts = nameText.split("[^\\w]+");

        for (String part : parts) {
            PlayerInfo info = mc.getConnection().getPlayerInfoByName(part);
            if (info != null && info.getProfile() != null
                    && OnlineIndicator.ONLINE_MOD_PLAYERS.contains(info.getProfile().name())) {
                OnlineIndicator.currentlyDrawingPlayerEntityName.set(true);
                return;
            }
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