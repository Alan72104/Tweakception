package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AutoFish extends Tweak
{
    private final AutoFishConfig c;
    public static class AutoFishConfig
    {
        public boolean enable = false;
    }
    private boolean lastTickFishing = false;

    public AutoFish(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.autoFish;
    }

    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
//            if (!event.player.getUniqueID().equals(getPlayer().getUniqueID())) return;
//
//            EntityPlayer player = event.player;
//            lastTickFishing = player.fishEntity != null;

        }
    }
}
