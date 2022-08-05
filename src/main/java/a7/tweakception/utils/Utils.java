package a7.tweakception.utils;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.List;

import static a7.tweakception.utils.McUtils.getMc;

public class Utils
{
    public static int clamp(int n, int min, int max)
    {
        return Math.max(Math.min(n, max), min);
    }
    
    public static float clamp(float n, float min, float max)
    {
        return Math.max(Math.min(n, max), min);
    }
    
    public static double clamp(double n, double min, double max)
    {
        return Math.max(Math.min(n, max), min);
    }
    
    public static String stringRepeat(String s, int c)
    {
        StringBuilder sb = new StringBuilder();
        while (c-- > 0)
            sb.append(s);
        return sb.toString();
    }
    
    public static String formatCommas(long n)
    {
        String s = String.valueOf(n);
        StringBuilder sb = new StringBuilder();
        int l = s.length();
        char[] a = s.toCharArray();
        if (l > 0)
            sb.append(a[0]);
        for (int i = 1; i < l; i++)
        {
            if ((l - i) % 3 == 0)
                sb.append(',');
            sb.append(a[i]);
        }
        return sb.toString();
    }
    
    public static String formatCommas(float n)
    {
        String s = String.valueOf(n);
        return formatCommas(s);
    }
    
    // Input string could be "1234" or "1234.5"
    public static String formatCommas(String s)
    {
        String[] split = s.split("\\.", 2);
        StringBuilder sb = new StringBuilder();
        int l = split[0].length();
        char[] a = split[0].toCharArray();
        if (l > 0)
            sb.append(a[0]);
        for (int i = 1; i < l; i++)
        {
            if ((l - i) % 3 == 0)
                sb.append(',');
            sb.append(a[i]);
        }
        if (split.length > 1)
            sb.append('.').append(split[1]);
        return sb.toString();
    }
    
    public static String formatMetric(long n)
    {
        if (n >= 1_000_000_000)
        {
            return (n / 1_000_000_000) + "b";
        }
        else if (n >= 1_000_000)
        {
            return (n / 1_000_000) + "m";
        }
        else if (n >= 1_000)
        {
            return (n / 1_000) + "k";
        }
        return String.valueOf(n);
    }
    
    public static String msToHHMMSSmmm(long ms)
    {
        return String.format("%d:%02d:%02d.%03d",
            ms / 3_600_000,
            ms % 3_600_000 / 60_000,
            ms % 60_000 / 1_000,
            ms % 1_000);
    }
    
    public static String msToMMSSmmm(long ms)
    {
        return String.format("%02d:%02d.%03d",
            ms / 60_000,
            ms % 60_000 / 1_000,
            ms % 1_000);
    }
    
    public static <T> void removeWhile(Collection<T> queue, Predicate<T> predicate)
    {
        removeWhile(queue, predicate, null);
    }
    
    public static <T> void removeWhile(Collection<T> queue, Predicate<T> predicate, Consumer<T> beforeRemove)
    {
        Iterator<T> it2 = queue.iterator();
        while (it2.hasNext())
        {
            T ele = it2.next();
            if (predicate.test(ele))
            {
                if (beforeRemove != null)
                    beforeRemove.accept(ele);
                it2.remove();
            }
            else
                break;
        }
    }
    
    public static String f(String s, Object... args)
    {
        return String.format(s, args);
    }
    
    public static NBTTagCompound getExtraAttributes(ItemStack item)
    {
        if (item == null)
            return null;
        
        NBTTagCompound tag = item.getTagCompound();
        if (tag != null)
        {
            return tag.getCompoundTag("ExtraAttributes");
        }
        return null;
    }
    
    public static String getSkyblockItemId(ItemStack item)
    {
        if (item == null)
            return null;
    
        NBTTagCompound tag = item.getTagCompound();
        if (tag != null)
        {
            NBTTagCompound extra = tag.getCompoundTag("ExtraAttributes");
            if (extra != null)
                return extra.getString("id");
        }
        return null;
    }
    
    public static String getSkyblockItemUuid(ItemStack item)
    {
        if (item == null)
            return null;
    
        NBTTagCompound tag = item.getTagCompound();
        if (tag != null)
        {
            NBTTagCompound extra = tag.getCompoundTag("ExtraAttributes");
            if (extra != null)
                return extra.getString("uuid");
        }
        return null;
    }
    
    public static int[] makeColorArray(int r, int g, int b, int a)
    {
        int[] arr = new int[4];
        arr[0] = Utils.clamp(r, 0, 255);
        arr[1] = Utils.clamp(g, 0, 255);
        arr[2] = Utils.clamp(b, 0, 255);
        arr[3] = Utils.clamp(a, 0, 255);
        return arr;
    }
    
    public static Color colorArrayToColor(int[] a)
    {
        return new Color(a[0], a[1], a[2], a[3]);
    }
    
    public static int getMaxStringWidth(List<String> list)
    {
        int max = 0;
        for (String s : list)
            max = Math.max(max, getMc().fontRendererObj.getStringWidth(s));
        return max;
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
}
