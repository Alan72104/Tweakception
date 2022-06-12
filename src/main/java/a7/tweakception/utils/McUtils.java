package a7.tweakception.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.util.Constants;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class McUtils
{
    private static final Matcher colorMatcher = Pattern.compile("§.").matcher("");

    public static Minecraft getMc()
    {
        return Minecraft.getMinecraft();
    }

    public static EntityPlayerSP getPlayer()
    {
        return getMc().thePlayer;
    }

    public static WorldClient getWorld()
    {
        return getMc().theWorld;
    }

    public static boolean isInGame()
    {
        return getWorld() != null && getPlayer() != null;
    }

    public static void sendChat(String s)
    {
        getPlayer().addChatMessage(new ChatComponentText(s));
    }

    public static void sendChatf(String s, Object... args)
    {
        getPlayer().addChatMessage(new ChatComponentText(String.format(s, args)));
    }

    public static String[] getDisplayLore(ItemStack stack)
    {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null)
        {
            NBTTagCompound display = nbt.getCompoundTag("display");
            if (display != null)
            {
                NBTTagList lore = display.getTagList("Lore", Constants.NBT.TAG_STRING);
                if (lore.tagCount() > 0)
                {
                    String[] a = new String[lore.tagCount()];
                    for (int i = 0; i < lore.tagCount(); i++)
                    {
                        a[i] = lore.getStringTagAt(i);
                    }
                    return a;
                }
            }
        }
        return null;
    }

    public static String getSkyblockItemId(ItemStack item)
    {
        NBTTagCompound tag = item.getTagCompound();
        if (tag != null)
        {
            NBTTagCompound extra = tag.getCompoundTag("ExtraAttributes");
            if (extra != null)
            {
                return extra.getString("id");
            }
        }
        return null;
    }

    public static String getSkyblockItemUuid(ItemStack item)
    {
        NBTTagCompound tag = item.getTagCompound();
        if (tag != null)
        {
            NBTTagCompound extra = tag.getCompoundTag("ExtraAttributes");
            if (extra != null)
            {
                return extra.getString("uuid");
            }
        }
        return null;
    }

    public static String getArmorStandHeadTexture(EntityArmorStand armorStand)
    {
        ItemStack head = armorStand.getCurrentArmor(3);

        if (head == null)
            return null;
        if (head.getItem() != Items.skull)
            return null;

        NBTTagCompound tag = head.getTagCompound();
        if (tag == null)
            return null;
        if (!tag.hasKey("SkullOwner"))
            return null;
        tag = tag.getCompoundTag("SkullOwner");
        if (!tag.hasKey("Properties"))
            return null;
        tag = tag.getCompoundTag("Properties");
        if (!tag.hasKey("textures"))
            return null;
        NBTTagList list = tag.getTagList("textures", Constants.NBT.TAG_COMPOUND);
        if (list.tagCount() == 0)
            return null;
        return list.getCompoundTagAt(0).getString("Value");
    }

    public static String cleanColor(String s)
    {
        return colorMatcher.reset(s).replaceAll("");
    }

    public static String cleanDuplicateColorCodes(String line) {
        StringBuilder sb = new StringBuilder();
        char currentColourCode = 'r';
        boolean sectionSymbolLast = false;
        for (char c : line.toCharArray()) {
            if ((int) c > 50000) continue;

            if (c == '\u00a7') {
                sectionSymbolLast = true;
            } else {
                if (sectionSymbolLast) {
                    if (currentColourCode != c) {
                        sb.append('\u00a7');
                        sb.append(c);
                        currentColourCode = c;
                    }
                    sectionSymbolLast = false;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
