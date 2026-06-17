package org.adam.zenithx.mixin;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class MixinIntegratedServer {

    @Inject(method = "publishServer", at = @At("RETURN"))
    private void disableAuthOnPublish(GameType gameMode, boolean allowCommands, int port, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            ((IntegratedServer)(Object)this).setUsesAuthentication(false);
        }
    }
}