package a7.tweakception.tweaks;

import a7.tweakception.DevSettings;
import a7.tweakception.Tweakception;
import a7.tweakception.overlay.Anchor;
import a7.tweakception.overlay.TextOverlay;
import it.unimi.dsi.fastutil.objects.*;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static a7.tweakception.utils.McUtils.sendChat;
import static a7.tweakception.utils.Utils.f;

public class StringReplace
{
    private static final String NO_REPLACEMENT = "";
    private static final Object2ObjectArrayMap<String, String> toReplaceMap = new Object2ObjectArrayMap<>();
    private static final ReferenceQueue<String> refQueue = new ReferenceQueue<>();
    private static Object2ObjectMap<WeakStringReference, String> replacementMap = null;
    private static MapType mapType = null;
    private static int newCount = 0;
    private static long replacementNanos = 0L;
    
    public static boolean isOn()
    {
        return !toReplaceMap.isEmpty() && replacementMap != null;
    }
    
    public static void add(String from, String to)
    {
        if (replacementMap == null)
            changeMapType(MapType.HASH);
        toReplaceMap.put(from, to);
        replacementMap.clear();
    }
    
    public static void remove(String s)
    {
        toReplaceMap.remove(s);
        replacementMap.clear();
    }
    
    public static String replaceString(String s)
    {
        if (replacementMap == null || s == null || s.isEmpty())
            return s;
        long nanos = System.nanoTime();
        reap();
        String replaced = replacementMap.computeIfAbsent(new WeakStringReference(s, refQueue), ref ->
        {
            if (DevSettings.printStringReplace)
                Tweakception.logger.debug(f("StrReplace (%d) %s: %s", replacementMap.size() + 1, ref, s));
            newCount++;
            String res = s;
            ObjectIterator<Object2ObjectMap.Entry<String, String>> iterator = toReplaceMap.object2ObjectEntrySet().fastIterator();
            while (iterator.hasNext())
            {
                Map.Entry<String, String> entry = iterator.next();
                res = res.replace(entry.getKey(), entry.getValue());
            }
            //noinspection StringEquality
            if (res == s) // If nothing is replaced, set the result to another instance to prevent circular references
                res = NO_REPLACEMENT;
            return res;
        });
        replacementNanos += System.nanoTime() - nanos;
        //noinspection StringEquality
        return replaced == NO_REPLACEMENT ? s : replaced;
    }
    
    public static void clear()
    {
        toReplaceMap.clear();
        replacementMap = null;
    }
    
    public enum MapType
    {
        ARRAY, HASH, LINKED_HASH, AVL_TREE, RB_TREE
    }
    
    public static void changeMapType(MapType type)
    {
        reap();
        Object2ObjectMap<WeakStringReference, String> newMap = null;
        Comparator<WeakStringReference> comp = (a, b) ->
        {
            String as = a.get();
            String bs = b.get();
            if (as == null && bs == null)
                return 0;
            else if (as == null)
                return -1;
            else if (bs == null)
                return 1;
            else
                return as.compareTo(bs);
        };
        switch (type)
        {
            case ARRAY:
                newMap = new Object2ObjectArrayMap<>();
                break;
            case HASH:
                newMap = new Object2ObjectOpenHashMap<>();
                break;
            case LINKED_HASH:
                newMap = new Object2ObjectLinkedOpenHashMap<>();
                break;
            case AVL_TREE:
                newMap = new Object2ObjectAVLTreeMap<>(comp);
                break;
            case RB_TREE:
                newMap = new Object2ObjectRBTreeMap<>(comp);
                break;
        }
        
        if (replacementMap != null)
            newMap.putAll(replacementMap);
        replacementMap = newMap;
        mapType = type;
    }
    
    private static void reap()
    {
        if (replacementMap == null)
            return;
        
        WeakStringReference zombie;
        while ((zombie = (WeakStringReference) refQueue.poll()) != null)
        {
            String val = replacementMap.remove(zombie);
            if (DevSettings.printStringReplace)
                Tweakception.logger.debug(f("StrReplace (%d): removed %s", replacementMap.size(), zombie));
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
                list.add(f("map type: %s", mapType));
                list.add(f("string map: %d", replacementMap.size()));
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
