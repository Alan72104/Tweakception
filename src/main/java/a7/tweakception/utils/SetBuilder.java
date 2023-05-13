package a7.tweakception.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SetBuilder<K>
{
    private final Set<K> set;
    
    public SetBuilder(Set<K> set)
    {
        this.set = set;
    }
    
    public SetBuilder<K> add(K k)
    {
        set.add(k);
        return this;
    }
    
    public Set<K> set()
    {
        return set;
    }
    
    public static <K> SetBuilder<K> hashSet()
    {
        return new SetBuilder<>(new HashSet<>());
    }
}
