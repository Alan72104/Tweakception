package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.SkyblockIsland;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityCreeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static a7.tweakception.tweaks.GlobalTweaks.getCurrentIsland;
import static a7.tweakception.tweaks.GlobalTweaks.isInSkyblock;

@Mixin(EntityLivingBase.class)
public class MixinEntityLivingBase
{
    // Bypasses net.minecraft.client.renderer.entity.RenderLiving.canRenderName
    @Inject(method = "getAlwaysRenderNameTagForRender", at = @At("HEAD"), cancellable = true)
    public void getAlwaysRenderNameTagForRender(CallbackInfoReturnable<Boolean> cir)
    {
        if (!tc$shouldAddGhostNameTag((Entity) (Object) this)) return;
        cir.setReturnValue(true);
        cir.cancel();
    }
    
    @Unique
    private static boolean tc$shouldAddGhostNameTag(Entity e)
    {
        return isInSkyblock() &&
            Tweakception.miningTweaks.isGhostNameTagOn() &&
            getCurrentIsland() == SkyblockIsland.DWARVEN_MINES &&
            e.isInvisible() &&
            e instanceof EntityCreeper &&
            ((EntityCreeper) e).getPowered() &&
            ((EntityCreeper) e).getEntityAttribute(SharedMonsterAttributes.maxHealth).getBaseValue() > 1024.0;
    }
}
