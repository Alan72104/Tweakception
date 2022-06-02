package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.SkyblockIsland;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static a7.tweakception.tweaks.GlobalTracker.getCurrentIsland;
import static a7.tweakception.utils.McUtils.getSkyblockItemId;
import static a7.tweakception.utils.McUtils.getSkyblockItemUuid;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends GuiScreen
{
    private static final ResourceLocation ITEM_RED_X = new ResourceLocation("tweakception:item_red_x.png");

    @Inject(method = "drawSlot", at = @At("TAIL"))
    public void afterDrawSlot(Slot slot, CallbackInfo ci)
    {
        if (getCurrentIsland() == SkyblockIsland.DUNGEON &&
            Tweakception.dungeonTweaks.isTrackingBonzoMaskUsage() &&
            slot.getStack() != null)
        {
            ItemStack stack = slot.getStack();
            String id = getSkyblockItemId(stack);
            String uuid = getSkyblockItemUuid(stack);
            if (id != null && (id.equals("BONZO_MASK") || id.equals("STARRED_BONZO_MASK") && uuid != null &&
                Tweakception.dungeonTweaks.isBonzoMaskUsed(uuid)))
            {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0, itemRender.zLevel + 200);
                GlStateManager.depthMask(false);
                Minecraft.getMinecraft().getTextureManager().bindTexture(ITEM_RED_X);
                this.drawTexturedModalRect(slot.xDisplayPosition, slot.yDisplayPosition, 0, 0, 16, 16);
                GlStateManager.depthMask(true);
                GlStateManager.popMatrix();
            }
        }
    }
}