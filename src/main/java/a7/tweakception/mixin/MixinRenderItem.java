package a7.tweakception.mixin;

import a7.tweakception.Tweakception;
import a7.tweakception.utils.Constants;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.StringBuilderCache;
import a7.tweakception.utils.Utils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static a7.tweakception.utils.McUtils.getMc;

@Mixin(RenderItem.class)
public class MixinRenderItem
{
    private static final Map<String, String> ENCHANT_DISPLAY_NAMES = new HashMap<>();
    private static final Map<String, String> SACK_DISPLAY_NAMES = new HashMap<>();
    
    @Inject(method = "renderItemOverlayIntoGUI", at = @At("TAIL"))
    public void renderItemOverlayIntoGUI(FontRenderer r, ItemStack stack, int x, int y, String str, CallbackInfo ci)
    {
        if (stack == null || stack.stackSize != 1)
            return;

        String tip = "";
        String stackCount = "";
        NBTTagCompound extra = McUtils.getExtraAttributes(stack);
        
        if (extra == null)
            return;
        
        if (Tweakception.globalTweaks.isRenderPotionTierOn() && extra.hasKey("potion_level"))
            stackCount = String.valueOf(extra.getInteger("potion_level"));
        else if (Tweakception.globalTweaks.isRenderEnchantedBooksTypeOn() &&
            stack.getItem() == Items.enchanted_book && extra.hasKey("enchantments"))
        {
            NBTTagCompound enchantments = extra.getCompoundTag("enchantments");
            Set<String> ids = enchantments.getKeySet();
            if (ids.size() == 1)
            {
                String id = ids.iterator().next();
                tip = getEnchantDisplayName(id);
                stackCount = String.valueOf(enchantments.getInteger(id));
            }
        }
        else if (Tweakception.globalTweaks.isRenderSacksTypeOn() &&
            stack.getItem() != Items.dye)
        {
            String id = Utils.getSkyblockItemId(stack);
            if (id != null && id.endsWith("_SACK"))
            {
                tip = getSackDisplayName(id);
            }
        }
        
        FontRenderer fr = getMc().fontRendererObj;
        
        if (!tip.isEmpty())
        {
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 1.0f);
            GlStateManager.scale(0.9f, 0.9f, 1.0f);
            fr.drawStringWithShadow(tip, 0.0f, 0.0f, 0xffffffff);
            GlStateManager.popMatrix();
            GlStateManager.enableLighting();
            GlStateManager.enableDepth();
        }
        
        if (!stackCount.isEmpty())
        {
            GlStateManager.disableLighting();
            GlStateManager.disableBlend();
            GlStateManager.disableDepth();
            GlStateManager.pushMatrix();
            int width = fr.getStringWidth(stackCount);
            GlStateManager.translate(x + 17.0f - width, y + 9.0f, 1.0f);
            fr.drawStringWithShadow(stackCount, 0.0f, 0.0f, 0xffffffff);
            GlStateManager.popMatrix();
            GlStateManager.enableLighting();
            GlStateManager.enableBlend();
            GlStateManager.enableDepth();
        }
    }
    
    private static String getEnchantDisplayName(String id)
    {
        String displayName = ENCHANT_DISPLAY_NAMES.get(id);
        if (displayName != null)
            return displayName;
        
        StringBuilder sb = StringBuilderCache.get();
        String enchantName = Constants.ENCHANTS.get(id);
        
        if (enchantName != null)
        {
            String[] parts = enchantName.split(" ");
            if (parts.length > 1)
                for (int i = 0; i < parts.length && i < 3; i++)
                    sb.append(Character.toUpperCase(parts[i].charAt(0)));
            else
                sb.append(parts[0], 0, Math.min(parts[0].length(), 3));
        }
        else
        {
            String[] parts = id.split("_");
            int start = 0;
            
            if (parts[0].equals("ultimate"))
                start = 1;
            
            if (parts.length > 1 + start)
                for (int i = start; i < parts.length && i < 3 + start; i++)
                    sb.append(Character.toUpperCase(parts[i].charAt(0)));
            else
                sb.append(Utils.capitalize(parts[start]), 0, Math.min(parts[start].length(), 3));
        }
        
        if (id.startsWith("ultimate"))
            sb.insert(0, "§d§l");
        
        displayName = sb.toString();
        ENCHANT_DISPLAY_NAMES.put(id, displayName);
        return displayName;
    }
    
    private static String getSackDisplayName(String id)
    {
        String displayName = SACK_DISPLAY_NAMES.get(id);
        if (displayName != null)
            return displayName;
        
        StringBuilder sb = StringBuilderCache.get();
        String[] split = id.split("_");
        
        int start = 0;
        if (split[0].equals("SMALL") || split[0].equals("MEDIUM") || split[0].equals("LARGE"))
            start = 1;
        
        if (split.length > start + 2) // Size? + name + SACK
            for (int i = start; i < split.length - 1 && i < 3 + start; i++)
                sb.append(split[i].charAt(0));
        else
            sb.append(Utils.capitalize(split[start]), 0, Math.min(split[start].length(), 3));
        
        displayName = sb.toString();
        SACK_DISPLAY_NAMES.put(id, displayName);
        return displayName;
    }
}
