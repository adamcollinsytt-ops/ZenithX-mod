package org.adam.zenithx.mixin;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class MixinIntegratedServer {

    @Inject(method = "publishServer", at = @At("HEAD"))
    private void disableAuthOnPublish(
            GameType gameMode,
            boolean allowCommands,
            int port,
            CallbackInfoReturnable<Boolean> cir) {

        IntegratedServer self = (IntegratedServer)(Object)this;
        self.setUsesAuthentication(false);
    }
}