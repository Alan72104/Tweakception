package a7.tweakception.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class McUtils
{
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

    private static final Matcher colorMatcher = Pattern.compile("(?i)\\u00A7.").matcher("");

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

    public static String f(String s, Object... args)
    {
        return String.format(s, args);
    }

    public static <T> T setAccessibleAndGetField(Object o, String name) throws NoSuchFieldException, IllegalAccessException
    {
        Field field = o.getClass().getDeclaredField(name);
        field.setAccessible(true);
        //noinspection unchecked
        return (T)field.get(o);
    }

    public static void setClipboard(String s)
    {
        StringSelection ss = new StringSelection(s);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
    }

    public static String getClipboard()
    {
        String s;
        try
        {
            s = (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        }
        catch (Exception e)
        {
            return null;
        }
        return s;
    }

    public static class Pair<A, B>
    {
        public final A a;
        public final B b;

        public Pair(A a, B b)
        {
            this.a = a;
            this.b = b;
        }
    }
}
