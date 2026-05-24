package org.adam.zenithx.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.adam.zenithx.handlers.ZenithRPC;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftRPCMixin {

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void onSetLevel(ClientLevel level, CallbackInfo ci) {
        ZenithRPC.update();
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("TAIL"))
    private void onDisconnect(CallbackInfo ci) {
        ZenithRPC.update();
    }
}