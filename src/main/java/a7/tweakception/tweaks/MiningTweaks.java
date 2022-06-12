package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import a7.tweakception.utils.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S24PacketBlockAction;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import static a7.tweakception.tweaks.GlobalTracker.getCurrentIsland;
import static a7.tweakception.tweaks.GlobalTracker.getTicks;
import static a7.tweakception.utils.McUtils.getWorld;
import static a7.tweakception.utils.McUtils.sendChat;

public class MiningTweaks extends Tweak
{
    private final MiningTweaksConfig c;
    public static class MiningTweaksConfig
    {
        public boolean highlightChests = false;
    }
    private final Set<TreasureChest> treasureChests = new ConcurrentSkipListSet<>(Comparator.comparing(a -> a.pos));

    public MiningTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.miningTweaks;
    }

    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END) return;

        if (getCurrentIsland() == SkyblockIsland.CRYSTAL_HOLLOWS)
        {
            if (!treasureChests.isEmpty())
            {
                Iterator<TreasureChest> it = treasureChests.iterator();
                while (it.hasNext())
                {
                    TreasureChest chest = it.next();
                    BlockPos bp = chest.pos;
                    TileEntity te = getWorld().getTileEntity(bp);
                    if (te instanceof TileEntityChest)
                    {
                        TileEntityChest teChest = (TileEntityChest)te;
                        if (!chest.opened && teChest.numPlayersUsing > 0)
                            chest.opened = true;
                    }
                    else
                        it.remove();
                }
            }
        }
    }

    private static final Color CHEST_COLOR_OPENED = new Color(0, 255, 0, 255 / 6);
    private static final Color CHEST_COLOR_CLOSED = new Color(255, 0, 0, 255 / 6);
    private static final Color CHEST_COLOR_WARNING = new Color(255, 255, 0, 255 / 6);

    public void onRenderLast(RenderWorldLastEvent event)
    {
        if (getCurrentIsland() == SkyblockIsland.CRYSTAL_HOLLOWS)
        {
            if (!treasureChests.isEmpty())
            {
                RenderUtils.Vector3d pos = RenderUtils.getInterpolatedViewingPos(event.partialTicks);

                for (TreasureChest chest : treasureChests)
                {
                    BlockPos bp = chest.pos;
                    Color color;
                    if (chest.opened)
                        color = CHEST_COLOR_OPENED;
                    else
                    {
                        int elapsed = getTicks() - chest.spawnTicks;
                        if (elapsed >= 20 * 50)
                        {
                            if (elapsed % 10 < 5)
                                color = CHEST_COLOR_WARNING;
                            else
                                continue;
                        }
                        else if (elapsed >= 20 * 30)
                            color = CHEST_COLOR_WARNING;
                        else
                            color = CHEST_COLOR_CLOSED;
                    }
                    RenderUtils.renderBoundingBoxChestSize(bp.getX() - pos.x, bp.getY() - pos.y, bp.getZ() - pos.z, color);
                }
            }
        }
    }

    public void onPacketMultiBlockChange(S22PacketMultiBlockChange packet)
    {
        if (getCurrentIsland() != SkyblockIsland.CRYSTAL_HOLLOWS) return;

        for (S22PacketMultiBlockChange.BlockUpdateData change : packet.getChangedBlocks())
        {
            Block block = change.getBlockState().getBlock();
            BlockPos pos = change.getPos();

            if (block == Blocks.chest)
                treasureChests.add(new TreasureChest(pos, getTicks()));
            else if (block == Blocks.air)
                treasureChests.remove(new TreasureChest(pos));
        }
    }

    public void onPacketBlockChange(S23PacketBlockChange packet)
    {
        if (getCurrentIsland() != SkyblockIsland.CRYSTAL_HOLLOWS) return;

        Block block = packet.getBlockState().getBlock();
        BlockPos pos = packet.getBlockPosition();

        if (block == Blocks.chest)
            treasureChests.add(new TreasureChest(pos, getTicks()));
        else if (block == Blocks.air)
            treasureChests.remove(new TreasureChest(pos));
    }

    public void onPacketBlockAction(S24PacketBlockAction packet)
    {
//        if (getCurrentIsland() != SkyblockIsland.CRYSTAL_HOLLOWS) return;
//
//        if (packet.getBlockType() == Blocks.chest)
//        {
//            BlockPos pos = packet.getBlockPosition();
//            if (packet.getData1() >= 1)
//                treasureChests.remove(pos);
//        }
    }

    public void onWorldUnload(WorldEvent.Unload event)
    {
        treasureChests.clear();
    }

    public void toggleHighlightChests()
    {
        c.highlightChests = !c.highlightChests;
        sendChat("MT-HighlightChests: toggled " + c.highlightChests);
    }

    private static class TreasureChest
    {
        public BlockPos pos;
        public int spawnTicks = 0;
        public boolean opened = false;
        public TreasureChest(BlockPos pos)
        {
            this.pos = pos;
        }
        public TreasureChest(BlockPos pos, int spawnTicks)
        {
            this.pos = pos;
            this.spawnTicks = spawnTicks;
        }
    }
}
