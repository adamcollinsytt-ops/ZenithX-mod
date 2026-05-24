package org.adam.zenithx.mixin;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.adam.zenithx.handlers.Settings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ScreenEffectRenderer.class)
public class LowFireMixin {

    @ModifyArg(
            method = "renderFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"
            ),
            index = 1
    )
    private static float modifyFire(float y) {
        return Settings.lowFire ? y - 0.5F : y;
    }
}