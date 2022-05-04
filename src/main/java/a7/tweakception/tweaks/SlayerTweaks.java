package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import a7.tweakception.utils.RenderUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.LinkedList;
import java.util.List;

import static a7.tweakception.tweaks.GlobalTracker.*;
import static a7.tweakception.utils.McUtils.*;

public class SlayerTweaks extends Tweak
{
    private final SlayerTweaksConfig c;
    public static class SlayerTweaksConfig
    {
        public boolean highlightGlyph = false;
    }
    private final List<BlockPos> glyphs = new LinkedList<>();

    public SlayerTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.slayerTweaks;
    }

    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (getTicks() % 5 == 0)
        {
            if (c.highlightGlyph)
            {
                glyphs.clear();
                EntityPlayerSP p = getPlayer();
                for (BlockPos pos : BlockPos.getAllInBox(new BlockPos(p.posX - 15, p.posY - 10, p.posZ - 15),
                        new BlockPos(p.posX + 15, p.posY + 10, p.posZ + 15)))
                {
                    if (getWorld().getBlockState(pos).getBlock() == Blocks.beacon)
                        glyphs.add(pos);
                }
            }
        }
    }

    public void onRenderLast(RenderWorldLastEvent event)
    {
        if (c.highlightGlyph)
            for (BlockPos p : glyphs)
                RenderUtils.renderBoundingBox(p.getX(), p.getY(), p.getZ(), 0xff00b7, 1.0f, 0);
    }

    public void toggleHighlightGlyph()
    {
        c.highlightGlyph = !c.highlightGlyph;
        sendChat("SlayerTweaks => Eman: toggled glyph highlighting " + c.highlightGlyph);
    }
}
