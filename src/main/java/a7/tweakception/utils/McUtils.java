package a7.tweakception.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.ChatComponentText;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
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

    public static void sendChat(String s) {
        getPlayer().addChatMessage(new ChatComponentText(s));
    }

    private static final Pattern color = Pattern.compile("(?i)\\u00A7.");

    public static String cleanColor(String in) {
        return color.matcher(in).replaceAll("");
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
}
