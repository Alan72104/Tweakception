package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.passive.EntityBat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net/minecraft/client/renderer/EntityRenderer$1")
public abstract class MixinEntityRenderer_Predicate
{
    @Inject(method = "apply(Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void targetEntityPredicate(Entity entity, CallbackInfoReturnable<Boolean> cir)
    {
        boolean cancel = false;
        if (Tweakception.globalTweaks.isDisableDeadMobTargetingOn() &&
            entity instanceof EntityLivingBase &&
            ((EntityLivingBase) entity).getHealth() <= 0f)
        {
            cancel = true;
        }
        else if ((Tweakception.globalTweaks.isDisableArmorStandTargetingOn() ||
                Tweakception.giftTweaks.isDisableArmorStandTargetingOn()) &&
            entity instanceof EntityArmorStand)
        {
            cancel = true;
        }
        else if (Tweakception.globalTweaks.isDisableBatTargetingOn() &&
            entity instanceof EntityBat)
        {
            cancel = true;
        }
        else if (Tweakception.globalTweaks.isDisablePlayerTargetingOn() &&
            entity instanceof EntityOtherPlayerMP)
        {
            cancel = true;
        }
        else if (Tweakception.giftTweaks.isOnlyTargetOpenableGiftOn() &&
            (!(entity instanceof EntityArmorStand) || !(entity.getName().equals("§e§lCLICK TO OPEN")))) // §eFrom: §a[VIP] someone§f
        {
            cancel = true;
        }
        
        if (cancel)
        {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
