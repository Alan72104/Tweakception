package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.GlobalTweaks;
import a7.tweakception.tweaks.SkyblockIsland;
import a7.tweakception.utils.McUtils;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal
{
    @Final
    @Shadow
    private Map<Integer, DestroyBlockProgress> damagedBlocks;
    
    @Inject(method = "sendBlockBreakProgress", at = @At("HEAD"), cancellable = true)
    public void sendBlockBreakProgress(int entityId, BlockPos pos, int stage, CallbackInfo ci)
    {
        if (Tweakception.miningTweaks.isSimulateBlockHardnessOn() &&
            (GlobalTweaks.getCurrentIsland() == SkyblockIsland.DWARVEN_MINES ||
                GlobalTweaks.getCurrentIsland() == SkyblockIsland.CRYSTAL_HOLLOWS))
        {
            float blockHardness = Tweakception.miningTweaks.getSpecialBlockHardness(McUtils.getWorld(), pos);
            if (blockHardness != 0.0f)
            {
                DestroyBlockProgress currentProgress = this.damagedBlocks.get(entityId);
                if (currentProgress != null && currentProgress.getPartialBlockDamage() > stage)
                {
                    ci.cancel();
                }
            }
        }
    }
}
