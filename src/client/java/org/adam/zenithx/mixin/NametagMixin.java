package org.adam.zenithx.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.adam.zenithx.handlers.SelfNametagRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class NametagMixin {
    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void zenithx$selfNametag(LivingEntity entity, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity == mc.player) {
            cir.setReturnValue(SelfNametagRenderer.shouldRenderOwnNametag());
        }
    }
}
