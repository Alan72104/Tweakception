package a7.tweakception.mixin;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderManager.class)
public class MixinRenderManager
{
    @Inject(method = "renderDebugBoundingBox", at = @At("HEAD"))
    public void renderDebugBoundingBox(Entity e, double x, double y, double z, float yaw, float partialTicks, CallbackInfo ci)
    {
        // Maybe fixes stupid fuck sbe silverfish solver not resetting line width,
        // which causes entity outline to be thicc
        GL11.glLineWidth(1.0f);
    }
}
