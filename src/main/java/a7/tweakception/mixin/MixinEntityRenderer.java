package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer implements IResourceManagerReloadListener
{
    @Shadow
    public void onResourceManagerReload(IResourceManager iResourceManager) {}
    
    @Inject(method = "renderWorld", at = @At("HEAD"), cancellable = true)
    private void onRenderWorld(CallbackInfo ci)
    {
        if ((Tweakception.globalTweaks.isAfkModeActive() && Tweakception.globalTweaks.isAfkSkipWorldRenderingOn()) ||
            Tweakception.globalTweaks.isSkipWorldRenderingOn())
        {
            ci.cancel();
        }
    }
}
