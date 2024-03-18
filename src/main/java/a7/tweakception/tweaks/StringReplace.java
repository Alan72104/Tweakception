package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.overlay.Anchor;
import a7.tweakception.overlay.TextOverlay;
import it.unimi.dsi.fastutil.objects.*;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static a7.tweakception.utils.McUtils.sendChat;
import static a7.tweakception.utils.Utils.f;

public class StringReplace
{
    private static final Object2ObjectArrayMap<String, String> replacementMap = new Object2ObjectArrayMap<>();
    private static final ReferenceQueue<String> refQueue = new ReferenceQueue<>();
    private static Object2ObjectMap<WeakStringReference, String> stringMap = null;
    private static int newCount = 0;
    private static long replacementNanos = 0L;
    
    public static boolean isOn()
    {
        return !replacementMap.isEmpty() && stringMap != null;
    }
    
    public static void add(String from, String to)
    {
        if (stringMap == null)
            changeMapType("array");
        replacementMap.put(from, to);
    }
    
    public static void remove(String s)
    {
        replacementMap.remove(s);
    }
    
    public static String replaceString(String s)
    {
        if (stringMap == null)
            return s;
        reap();
        long nanos = System.nanoTime();
        String replaced = stringMap.computeIfAbsent(new WeakStringReference(s, refQueue), ref ->
        {
            Tweakception.logger.debug(f("StrReplace (%d): %s", stringMap.size() + 1, s));
            newCount++;
            String res = s;
            ObjectIterator<Object2ObjectMap.Entry<String, String>> iterator = replacementMap.object2ObjectEntrySet().fastIterator();
            while (iterator.hasNext())
            {
                Map.Entry<String, String> entry = iterator.next();
                res = res.replace(entry.getKey(), entry.getValue());
            }
            return res;
        });
        replacementNanos += System.nanoTime() - nanos;
        return replaced;
    }
    
    public static void clear()
    {
        replacementMap.clear();
        stringMap = null;
    }
    
    public static void changeMapType(String type)
    {
        reap();
        switch (type)
        {
            case "array":
                stringMap = stringMap == null ? new Object2ObjectArrayMap<>() : new Object2ObjectArrayMap<>(stringMap);
                break;
            case "linked":
                stringMap = stringMap == null ? new Object2ObjectLinkedOpenHashMap<>() : new Object2ObjectLinkedOpenHashMap<>(stringMap);
                break;
        }
    }
    
    private static void reap()
    {
        if (stringMap == null)
            return;
        
        Reference<? extends String> zombie;
        while ((zombie = refQueue.poll()) != null)
        {
            //noinspection SuspiciousMethodCalls
            Tweakception.logger.debug(f("StrReplace (%d): removed %s", stringMap.size() - 1, stringMap.remove(zombie)));
        }
    }
    
    public static void toggleOverlay()
    {
        Tweakception.overlayManager.toggle(StringReplaceOverlay.NAME);
        sendChat("StringReplace: Toggled " + Tweakception.overlayManager.isEnabled(StringReplaceOverlay.NAME));
    }
    
    public static class StringReplaceOverlay extends TextOverlay
    {
        public static final String NAME = "StringReplaceOverlay";
        
        public StringReplaceOverlay()
        {
            super(NAME);
            setAnchor(Anchor.BottomRight);
            setOrigin(Anchor.BottomRight);
            setX(-300);
            setY(-300);
        }
        
        @Override
        public void update()
        {
            super.update();
            List<String> list = getContent();
            list.clear();
            if (!isOn())
                list.add("StringReplace not active");
            else
            {
                list.add(f("string map: %d", stringMap.size()));
                list.add(f("new count: %d", newCount));
                list.add(f("frame replacement time: %,d ns", replacementNanos));
            }
            newCount = 0;
            replacementNanos = 0L;
            setContent(list);
        }
    }
    
    private static class WeakStringReference extends WeakReference<String>
    {
        private final int hash;
        
        public WeakStringReference(String s, ReferenceQueue<String> queue)
        {
            super(s, queue);
            hash = s.hashCode();
        }
        
        @Override
        public int hashCode()
        {
            return hash;
        }
        
        @Override
        public boolean equals(Object other)
        {
            if (this == other)
                return true;
            if (other instanceof WeakStringReference)
            {
                String thisString = this.get();
                String otherString = ((WeakStringReference) other).get();
                return Objects.equals(thisString, otherString);
            }
            return false;
        }
    }
}
