package org.adam.zenithx.mixin;

//? if >=26 {
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Accessor("gameRenderState")
    GameRenderState getGameRenderState();
}
//? }
