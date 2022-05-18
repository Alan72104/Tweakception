package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.utils.RenderUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static a7.tweakception.Tweakception.BlockSearchThread;
import static a7.tweakception.tweaks.GlobalTracker.getCurrentIsland;
import static a7.tweakception.tweaks.GlobalTracker.getTicks;
import static a7.tweakception.utils.McUtils.*;

public class SlayerTweaks extends Tweak
{
    private final SlayerTweaksConfig c;
    public static class SlayerTweaksConfig
    {
        public boolean highlightGlyph = false;
    }
    private List<BlockPos> glyphs = new ArrayList<>(20);
    private List<BlockPos> glyphsTemp = new ArrayList<>(20);
    private BlockSearchThread searchThread;

    public SlayerTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.slayerTweaks;
    }

    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (getTicks() % 5 == 4)
        {
            if (getCurrentIsland() == SkyblockIsland.THE_END)
            {
                if (c.highlightGlyph)
                {
                    if (searchThread == null || searchThread.done)
                    {
                        EntityPlayerSP p = getPlayer();
                        glyphs = glyphsTemp;
                        glyphsTemp = new ArrayList<>(20);
                        searchThread = new BlockSearchThread((int)p.posX, (int)p.posY, (int)p.posZ, 15, 10, getWorld(), Blocks.beacon, glyphsTemp);
                        Tweakception.threadPool.execute(searchThread);
                    }
                }
            }
        }
    }

    public void onRenderLast(RenderWorldLastEvent event)
    {
        if (getCurrentIsland() == SkyblockIsland.THE_END)
        {
            if (c.highlightGlyph)
                for (BlockPos p : glyphs)
                    RenderUtils.drawBeaconBeamOrBoundingBox(p, new Color(255, 0, 106, (int)(255 * 0.9f)), event.partialTicks, 1);
        }
    }

    public void onWorldUnload(WorldEvent.Unload event)
    {
        if (searchThread != null)
            searchThread.cancel = true;
    }

    public void toggleHighlightGlyph()
    {
        c.highlightGlyph = !c.highlightGlyph;
        sendChat("SlayerTweaks-Eman: toggled glyph highlighting " + c.highlightGlyph);
        if (!c.highlightGlyph)
        {
            if (searchThread != null && !searchThread.done)
                searchThread.cancel = true;
            searchThread = null;
        }
    }
}
