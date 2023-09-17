package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static a7.tweakception.tweaks.GlobalTweaks.isInSkyblock;

@Mixin(Container.class)
public abstract class MixinContainer
{
    @Shadow public abstract Slot getSlot(int slotId);
    
    @Inject(method = "putStackInSlot", at = @At("RETURN"))
    public void putStacksInSlot(int slotID, ItemStack stack, CallbackInfo ci)
    {
        if (!(isInSkyblock() && Tweakception.enchantingTweaks.isAutoSolveAndExperimentOn())) return;
        Tweakception.enchantingTweaks.onSlotSet((Container) (Object) this, this.getSlot(slotID), stack);
    }
    
    @Inject(method = "putStacksInSlots", at = @At("RETURN"))
    public void putStacksInSlots(ItemStack[] stacks, CallbackInfo ci)
    {
        if (!(isInSkyblock() && Tweakception.enchantingTweaks.isAutoSolveAndExperimentOn())) return;
        for (int i = 0; i < stacks.length; i++)
            Tweakception.enchantingTweaks.onSlotSet((Container) (Object) this, this.getSlot(i), stacks[i]);
    }
}
