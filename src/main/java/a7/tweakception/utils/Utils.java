package a7.tweakception.utils;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static a7.tweakception.utils.McUtils.getMc;
import static a7.tweakception.utils.McUtils.getPlayer;

public class Utils
{
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    public static final Matcher auctionPriceMatcher = Pattern.compile(
        "§7(?<type>Buy it now|Starting bid|Top bid): §6(?<price>(?:\\d{1,3},?)+) coins$").matcher("");
    
    @SafeVarargs
    public static <T> HashSet<T> hashSet(T... array)
    {
        return new HashSet<T>(Arrays.asList(array));
    }
    
    // Supports comma
    public static int parseInt(String s)
    {
        try
        {
            return NUMBER_FORMAT.parse(s).intValue();
        }
        catch (ParseException e)
        {
            return 0;
        }
    }
    
    // Supports comma
    public static long parseLong(String s)
    {
        try
        {
            return NUMBER_FORMAT.parse(s).longValue();
        }
        catch (ParseException e)
        {
            return 0;
        }
    }
    
    // Supports comma
    public static float parseFloat(String s)
    {
        try
        {
            return NUMBER_FORMAT.parse(s).floatValue();
        }
        catch (ParseException e)
        {
            return 0.0f;
        }
    }
    
    // Supports comma
    public static double parseDouble(String s)
    {
        try
        {
            return NUMBER_FORMAT.parse(s).doubleValue();
        }
        catch (ParseException e)
        {
            return 0.0;
        }
    }
    
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
    
    public static double roundToDigits(double d, int di)
    {
        double t = Math.pow(10, di);
        return Math.round(d * t) / t;
    }
    
    public static float roundToDigits(float f, int d)
    {
        float t = (float) Math.pow(10, d);
        return Math.round(f * t) / t;
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
    
    public static String formatCommas(double n)
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
    
    public static <T> void removeWhile(Iterable<T> queue, Predicate<T> predicate)
    {
        removeWhile(queue, predicate, null);
    }
    
    public static <T> void removeWhile(Iterable<T> queue, Predicate<T> predicate, Consumer<T> beforeRemove)
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
    
    public static int findInHotbarById(String... ids)
    {
        return findInHotbarById(id ->
        {
            for (String target : ids)
                if (id.equals(target))
                    return true;
            return false;
        });
    }
    
    public static int findInHotbarById(Collection<String> ids)
    {
        return findInHotbarById(id ->
        {
            if (ids.contains(id))
                return true;
            return false;
        });
    }
    
    public static int findInHotbarById(Predicate<String> predicate)
    {
        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = getPlayer().inventory.getStackInSlot(i);
            String id = Utils.getSkyblockItemId(stack);
            if (stack == null || id == null)
                continue;
            if (predicate.test(id))
                return i;
        }
        return -1;
    }
    
    public static int findInHotbarBy(Predicate<ItemStack> predicate)
    {
        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = getPlayer().inventory.getStackInSlot(i);
            if (predicate.test(stack))
                return i;
        }
        return -1;
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
    
    public static String captilize(String s)
    {
        if (s.length() == 0)
            return s;
        
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(s.charAt(0)));
        char[] a = s.toCharArray();
        for (int i = 1; i < a.length; i++)
            sb.append(Character.toLowerCase(a[i]));
        
        return sb.toString();
    }
    
    public static <T> T setAccessibleAndGetField(Object o, String... names) throws Exception
    {
        return setAccessibleAndGetField(o.getClass(), names);
    }
    
    public static <T> T setAccessibleAndGetField(Class<?> clazz, Object o, String... names) throws Exception
    {
        Exception lastException = null;
        for (String name : names)
        {
            try
            {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                //noinspection unchecked
                return (T) field.get(o);
            }
            catch (Exception e)
            {
                lastException = e;
            }
        }
        throw lastException;
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
            s = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        }
        catch (Exception e)
        {
            return null;
        }
        return s;
    }
    
    public static void fileCopy(File source, File dest) throws IOException
    {
        try (InputStream is = Files.newInputStream(source.toPath());
             OutputStream os = Files.newOutputStream(dest.toPath()))
        {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0)
                os.write(buffer, 0, length);
        }
    }
}
