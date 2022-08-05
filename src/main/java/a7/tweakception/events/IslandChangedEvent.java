package a7.tweakception.events;

import a7.tweakception.tweaks.SkyblockIsland;
import net.minecraftforge.fml.common.eventhandler.Event;

public class IslandChangedEvent extends Event
{
    private final SkyblockIsland prevIsland;
    private final SkyblockIsland newIsland;
    
    public IslandChangedEvent(SkyblockIsland prev, SkyblockIsland $new)
    {
        prevIsland = prev;
        newIsland = $new;
    }
    
    public SkyblockIsland getPrevIsland()
    {
        return prevIsland;
    }
    
    public SkyblockIsland getNewIsland()
    {
        return newIsland;
    }
}
