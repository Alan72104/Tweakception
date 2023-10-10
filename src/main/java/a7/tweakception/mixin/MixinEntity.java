package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.SkyblockIsland;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.StringBuilderCache;
import a7.tweakception.utils.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static a7.tweakception.tweaks.GlobalTweaks.getCurrentIsland;
import static a7.tweakception.tweaks.GlobalTweaks.isInSkyblock;

@Mixin(Entity.class)
public class MixinEntity
{
    // Bypasses net.minecraft.client.renderer.entity.RendererLivingEntity.canRenderName
    @Inject(method = "isInvisibleToPlayer", at = @At("HEAD"), cancellable = true)
    public void isInvisibleToPlayer(EntityPlayer player, CallbackInfoReturnable<Boolean> cir)
    {
        if (player != McUtils.getPlayer()) return;
        if (!tc$shouldAddGhostNameTag((Entity) (Object) this)) return;
        cir.setReturnValue(false);
        cir.cancel();
    }
    
    @Inject(method = "hasCustomName", at = @At("HEAD"), cancellable = true)
    public void hasCustomName(CallbackInfoReturnable<Boolean> cir)
    {
        if (!tc$shouldAddGhostNameTag((Entity) (Object) this)) return;
        cir.setReturnValue(true);
        cir.cancel();
    }
    
    @Inject(method = "getCustomNameTag", at = @At("HEAD"), cancellable = true)
    public void getCustomNameTag(CallbackInfoReturnable<String> cir)
    {
        Entity $this = (Entity) (Object) this;
        if (!tc$shouldAddGhostNameTag($this))
            return;
        EntityCreeper creeper = (EntityCreeper) $this;
        // getMaxHealth() calls getAttributeValue() which clamps the value to 1024, so we use the base value
        long maxHp = (long) creeper.getEntityAttribute(SharedMonsterAttributes.maxHealth).getBaseValue();
        long hp = (long) creeper.getHealth();
        String hpFormatted = Utils.formatNameTagHp(hp);
        String maxHpFormatted = Utils.formatNameTagHp(maxHp);
        StringBuilder sb = StringBuilderCache.get();
        if (maxHp == 4_000_000 || maxHp == 8_000_000)
            sb.append("§5[§dLv250§5] §5Runic Ghost §d").append(hpFormatted).append("§f/§5").append(maxHpFormatted).append("§c❤");
        else
            sb.append("§8[§7Lv250§8] §cGhost §a").append(hpFormatted).append("§f/§a").append(maxHpFormatted).append("§c❤");
        cir.setReturnValue(sb.toString());
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
