package a7.tweakception.mixin;

import a7.tweakception.LagSpikeWatcher;
import a7.tweakception.Tweakception;
import a7.tweakception.utils.McUtils;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft
{
    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void runGameLoop(CallbackInfo ci)
    {
        LagSpikeWatcher.newTick();
    }
}
