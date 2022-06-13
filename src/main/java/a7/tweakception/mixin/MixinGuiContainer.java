package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.SkyblockIsland;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static a7.tweakception.tweaks.GlobalTracker.getCurrentIsland;
import static a7.tweakception.utils.McUtils.*;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends GuiScreen
{
    @Shadow public Container inventorySlots;
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
        if (Tweakception.dungeonTweaks.isBlockingOpheliaShopClicks() &&
            slot != null && this.inventorySlots instanceof ContainerChest)
        {
            ContainerChest chest = (ContainerChest)this.inventorySlots;
            IInventory inv = chest.getLowerChestInventory();
            String name = inv.getName();
            if (inv.getSizeInventory() == 54 &&
                name.equals("Ophelia"))
            {
                int column = slotId % 9;
                int row = slotId / 9;
                if (column >= 2 - 1 && column <= 8 - 1 &&
                    row >= 2 - 1 && row <= 5 - 1)
                {
                    EntityPlayerSP p = McUtils.getPlayer();
                    ISound sound = new PositionedSoundRecord(new ResourceLocation("random.orb"),
                            1.0f, 0.943f, (float)p.posX, (float)p.posY, (float)p.posZ);

                    float oldLevel = getMc().gameSettings.getSoundLevel(SoundCategory.PLAYERS);
                    getMc().gameSettings.setSoundLevel(SoundCategory.PLAYERS, 1);
                    getMc().getSoundHandler().playSound(sound);
                    getMc().gameSettings.setSoundLevel(SoundCategory.PLAYERS, oldLevel);

                    ci.cancel();
                }
            }
        }
    }
}