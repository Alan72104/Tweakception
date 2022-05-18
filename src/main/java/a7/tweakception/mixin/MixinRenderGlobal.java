package a7.tweakception.mixin;

import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal
{

//    @Inject(method = "playAuxSFX", at = @At(value = "CONSTANT", args = "stringValue=mob.zombie.woodbreak"))
//    public void playAuxSFX(EntityPlayer player, int sfxType, BlockPos blockPosIn, int data, CallbackInfo ci)
//    {
//        if (!isInGame()) return;
//        if (!isInSkyblock()) return;
//
//        if (Tweakception.dungeonTweaks.isTrackingDamages())
//        {
//            Tweakception.dungeonTweaks.mixinFerocityDetected();
//        }
//    }

//    @Inject(method = "playAuxSFX", at = @At("HEAD"))
//    public void playAuxSFX2(EntityPlayer player, int sfxType, BlockPos blockPosIn, int data, CallbackInfo ci)
//    {
//        if (!isInGame()) return;
//        if (!isInSkyblock()) return;
//        sendChat(String.valueOf(sfxType));
//    }
}
