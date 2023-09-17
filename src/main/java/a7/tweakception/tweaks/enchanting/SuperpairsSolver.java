package a7.tweakception.tweaks.enchanting;

import a7.tweakception.tweaks.EnchantingTweaks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SuperpairsSolver extends EnchantingTweaks.Solver
{
    
    public SuperpairsSolver(ContainerChest container)
    {
        super(container);
    }
    
    @Override
    public void update()
    {
    }
    
    @Override
    public void onSlotSet(Container container, Slot slot, ItemStack stack)
    {
    }
    
    @Override
    public void drawDebug(int xSize, int guiLeft, int guiTop)
    {
    }
}
