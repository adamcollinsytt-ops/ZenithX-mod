package org.adam.zenithx.mixin;

import net.minecraft.world.item.ItemStack;
import org.adam.zenithx.handlers.Settings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.GameRenderer")
public class NoTotemOverlayMixin {

    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void cancelTotem(ItemStack stack, CallbackInfo ci) {
        if (Settings.noTotemPop) {
            ci.cancel();
        }
    }
}