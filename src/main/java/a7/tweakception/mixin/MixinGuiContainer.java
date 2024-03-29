package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.SkyblockIsland;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Utils;
import a7.tweakception.utils.WindowClickContants;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static a7.tweakception.tweaks.GlobalTweaks.getCurrentIsland;
import static a7.tweakception.utils.McUtils.getMc;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends GuiScreen
{
    @Shadow
    public Container inventorySlots;
    private static final ResourceLocation ITEM_RED_X = new ResourceLocation("tweakception:item_red_x.png");
    
    @Inject(method = "drawSlot", at = @At("TAIL"))
    public void slotDrawn(Slot slot, CallbackInfo ci)
    {
        if (Tweakception.dungeonTweaks.isTrackingMaskUsage() &&
            getCurrentIsland() == SkyblockIsland.DUNGEON &&
            slot.getStack() != null)
        {
            ItemStack stack = slot.getStack();
            String id = Utils.getSkyblockItemId(stack);
            if (id != null && Tweakception.dungeonTweaks.isMaskUsed(id))
            {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0, itemRender.zLevel + 200);
                GlStateManager.depthMask(false);
                getMc().getTextureManager().bindTexture(ITEM_RED_X);
                this.drawTexturedModalRect(slot.xDisplayPosition, slot.yDisplayPosition, 0, 0, 16, 16);
                GlStateManager.depthMask(true);
                GlStateManager.popMatrix();
            }
        }
    }
    
    @Inject(method = "handleMouseClick", at = @At("HEAD"), cancellable = true)
    public void handleMouseClick(Slot slot, int slotId, int button, int mode, CallbackInfo ci)
    {
        if (!(slot != null && this.inventorySlots instanceof ContainerChest))
            return;
        
        IInventory inv = ((ContainerChest) this.inventorySlots).getLowerChestInventory();
        String name = inv.getName();
        
        if (Tweakception.dungeonTweaks.isPartyFinderRefreshCooldownEnbaled() &&
            inv.getSizeInventory() == 54 &&
            name.equals("Party Finder") &&
            slot.getHasStack() && slot.getStack().getItem() == Item.getItemFromBlock(Blocks.emerald_block))
        {
            long cd = Tweakception.dungeonTweaks.getPartyFinderRefreshCooldown();
            if (cd > 0L)
            {
                McUtils.playCoolDing();
                ci.cancel();
            }
            else
                Tweakception.dungeonTweaks.setPartyFinderRefreshCooldown();
        }
        else if (Tweakception.dungeonTweaks.isBlockingOpheliaShopClicks() &&
            inv.getSizeInventory() == 54 &&
            name.equals("Ophelia"))
        {
            int column = slotId % 9;
            int row = slotId / 9;
            if (column >= 2 - 1 && column <= 8 - 1 &&
                row >= 2 - 1 && row <= 5 - 1)
            {
                McUtils.playCoolDing();
                ci.cancel();
            }
        }
        else if (Tweakception.tuningTweaks.isTemplateEnabled() &&
            inv.getSizeInventory() == 54 &&
            name.equals("Stats Tuning") &&
            slot.getStack() != null)
        {
            int index = Tweakception.tuningTweaks.getTemplateSlotFromStack(slot.getStack());
            if (index != -1)
            {
                if (mode == WindowClickContants.ShiftLeftRight.MODE)
                {
                    if (button == WindowClickContants.ShiftLeftRight.BTN_SHIFT_LEFT)
                        Tweakception.tuningTweaks.useTemplate(index);
                    else if (button == WindowClickContants.ShiftLeftRight.BTN_SHIFT_RIGHT)
                        Tweakception.tuningTweaks.setTemplate(index);
                }
                else if (mode == WindowClickContants.LeftRight.MODE &&
                    button == WindowClickContants.LeftRight.BTN_LEFT &&
                    Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
                {
                    Tweakception.tuningTweaks.removeTemplate(index);
                }
                ci.cancel();
            }
        }
        else if (Tweakception.globalTweaks.isBlockingQuickCraft() &&
            inv.getSizeInventory() == 54 &&
            name.equals("Craft Item") &&
            slot.getStack() != null && slot.slotNumber % 9 == 7 &&
            slot.slotNumber / 9 >= 1 && slot.slotNumber / 9 <= 3)
        {
            String id = Utils.getSkyblockItemId(slot.getStack());
            if (id != null && !Tweakception.globalTweaks.isIdInQuickCraftWhitelist(id))
            {
                McUtils.playCoolDing();
                ci.cancel();
            }
        }
    }
    
    @Inject(method = "keyTyped", at = @At("TAIL"))
    protected void keyTyped(char chr, int key, CallbackInfo ci)
    {
        if (!(this.inventorySlots instanceof ContainerChest))
            return;
        
        if (Keyboard.isRepeatEvent())
            return;
        
        IInventory inv = ((ContainerChest) this.inventorySlots).getLowerChestInventory();
        String name = inv.getName();
        
        if (Tweakception.globalTweaks.isBlockingQuickCraft() &&
            inv.getSizeInventory() == 54 &&
            name.equals("Craft Item"))
        {
            Slot slot = ((GuiContainer) (Object) this).getSlotUnderMouse();
            if (slot != null && slot.getHasStack() && slot.slotNumber % 9 == 7 &&
                slot.slotNumber / 9 >= 1 && slot.slotNumber / 9 <= 3 &&
                key == Keyboard.KEY_LMENU)
            {
                String id = Utils.getSkyblockItemId(slot.getStack());
                if (id != null)
                {
                    if (Tweakception.globalTweaks.toggleQuickCraftWhitelist(id))
                        McUtils.playCoolDing();
                    else
                        McUtils.playCoolDing(0.3f);
                }
            }
            
        }
    }
}