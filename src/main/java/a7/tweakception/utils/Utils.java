package a7.tweakception.utils;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Utils
{
    public static String formatIntCommas(int in)
    {
        String s = String.valueOf(in);
        StringBuilder sb = new StringBuilder();
        int l = s.length();
        char[] a = s.toCharArray();
        if (l > 0)
            sb.append(a[0]);
        for (int i = 1; i < l; i++)
        {
            if ((l-i) % 3 == 0)
                sb.append(',');
            sb.append(a[i]);
        }
        return sb.toString();
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
