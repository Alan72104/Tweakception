package a7.tweakception.utils;

import java.util.HashMap;
import java.util.Map;

public class MapBuilder<K, V>
{
    private final Map<K, V> map;
    
    public MapBuilder(Map<K, V> map)
    {
        this.map = map;
    }
    
    public MapBuilder<K, V> put(K k, V v)
    {
        map.put(k, v);
        return this;
    }
    
    public Map<K, V> map()
    {
        return map;
    }
    
    public static <K, V> MapBuilder<K, V> hashMap()
    {
        return new MapBuilder<>(new HashMap<>());
    }
    
    public static MapBuilder<String, String> stringHashMap()
    {
        return hashMap();
    }
    
    public static MapBuilder<String, Integer> stringIntHashMap()
    {
        return hashMap();
    }
}
