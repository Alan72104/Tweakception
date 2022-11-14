package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> extends Render<T>
{
    protected MixinRendererLivingEntity(RenderManager p_i46179_1_)
    {
        super(p_i46179_1_);
    }
    
    @Redirect(method = "renderModel", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/entity/EntityLivingBase;isInvisibleToPlayer(Lnet/minecraft/entity/player/EntityPlayer;)Z"))
    private boolean redirectedIsInvisibleToPlayer(EntityLivingBase $this, EntityPlayer player)
    {
        if ($this instanceof EntityArmorStand)
        {
            if (Tweakception.globalTweaks.isRenderInvisibleArmorStandsOn())
                return false;
            else
                return $this.isInvisibleToPlayer(player);
        }
        else if (Tweakception.globalTweaks.isRenderInvisibleEntitiesOn())
            return false;
        else
            return $this.isInvisibleToPlayer(player);
    }
    
    @ModifyArg(method = "renderModel", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/renderer/GlStateManager;color(FFFF)V"), index = 3)
    private float modifyInvisibleEntityAlpha(float a)
    {
        return Tweakception.globalTweaks.getInvisibleEntityAlpha();
    }
}
