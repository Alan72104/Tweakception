package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.GlobalTweaks;
import a7.tweakception.tweaks.SkyblockIsland;
import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class MixinBlock
{
    @Inject(method = "getBlockHardness", at = @At("HEAD"))
    public void getBlockHardness(World world, BlockPos pos, CallbackInfoReturnable<Float> cir)
    {
        if (Tweakception.miningTweaks.isSimulateBlockHardnessOn() &&
            GlobalTweaks.getCurrentIsland() == SkyblockIsland.DWARVEN_MINES)
        {
        }
    }
}
