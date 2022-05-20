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

import java.awt.*;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import static a7.tweakception.tweaks.GlobalTracker.getCurrentIsland;
import static a7.tweakception.utils.McUtils.getWorld;
import static a7.tweakception.utils.McUtils.sendChat;

public class MiningTweaks extends Tweak
{
    private final MiningTweaksConfig c;
    public static class MiningTweaksConfig
    {
        public boolean highlightChests = false;
    }
    private final Set<BlockPos> treasureChests = new ConcurrentSkipListSet<>();

    public MiningTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.miningTweaks;
    }

    public void onRenderLast(RenderWorldLastEvent event)
    {
        if (getCurrentIsland() == SkyblockIsland.CRYSTAL_HOLLOWS)
        {
            for (BlockPos pos : treasureChests)
            {
                RenderUtils.Vector3d p = RenderUtils.getInterpolatedViewingPos(event.partialTicks);
                TileEntity te = getWorld().getTileEntity(pos);
                if (te instanceof TileEntityChest)
                {
                    TileEntityChest chest = (TileEntityChest)te;
                    Color color = chest.numPlayersUsing > 0 ? new Color(0, 255, 0, 255 / 6) : new Color(255, 0, 0, 255 / 6);
                    RenderUtils.renderBoundingBoxChestSize(pos.getX() - p.x, pos.getY() - p.y, pos.getZ() - p.z, color);
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
                treasureChests.add(pos);
            else if (block == Blocks.air)
                treasureChests.remove(pos);
        }
    }

    public void onPacketBlockChange(S23PacketBlockChange packet)
    {
        if (getCurrentIsland() != SkyblockIsland.CRYSTAL_HOLLOWS) return;

        Block block = packet.getBlockState().getBlock();
        BlockPos pos = packet.getBlockPosition();

        if (block == Blocks.chest)
            treasureChests.add(pos);
        else if (block == Blocks.air)
            treasureChests.remove(pos);
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
}
