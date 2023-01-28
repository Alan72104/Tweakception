package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import a7.tweakception.utils.Constants;
import a7.tweakception.utils.RayTraceUtils;
import a7.tweakception.utils.RenderUtils;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3i;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import static a7.tweakception.tweaks.GlobalTweaks.getTicks;
import static a7.tweakception.utils.McUtils.*;

public class ForagingTweaks extends Tweak
{
    public static class ForagingTweaksConfig
    {
    }
    
    private final ForagingTweaksConfig c;
    private boolean enableTreeIndicator = false;
    private final boolean[] treeGrowthStates = new boolean[Constants.PARK_DARK_TREES.length];
    
    public ForagingTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.foragingTweaks;
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END) return;
        
        if (enableTreeIndicator && getTicks() % 5 == 3)
        {
            for (int i = 0; i < Constants.PARK_DARK_TREES.length; i++)
            {
                BlockPos center = Constants.PARK_DARK_TREES[i];
                boolean grown = doTreeGrowthSearch(center, 15);
                treeGrowthStates[i] = grown;
            }
        }
    }
    
    public void onRenderLast(RenderWorldLastEvent event)
    {
        if (enableTreeIndicator)
        {
            for (int i = 0; i < Constants.PARK_DARK_TREES.length; i++)
            {
                BlockPos pos = Constants.PARK_DARK_TREES[i];
                boolean grown = treeGrowthStates[i];
                if (grown)
                    RenderUtils.drawBeaconBeamOrBoundingBox(pos, new Color(255, 175, 175, 96), event.partialTicks, -1);
            }
        }
    }
    
    private static boolean isDarkOak(IBlockState state)
    {
        return state.getBlock() == Blocks.log2 &&
            state.getValue(BlockNewLog.VARIANT) == BlockPlanks.EnumType.DARK_OAK;
    }
    
    private boolean doTreeGrowthSearch(BlockPos center, int targetConnectedCount)
    {
        Queue<BlockPos> nextPoses = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos pos : BlockPos.getAllInBox(center.add(new Vec3i(-1, 0, -1)),
            center.add(new Vec3i(1, 1, 1))))
        {
            IBlockState state = getWorld().getBlockState(pos);
            int connectedCount = 0;
            if (isDarkOak(state) && !visited.contains(pos))
            {
                nextPoses.offer(pos);
                visited.add(pos);
                connectedCount++;
                while (!nextPoses.isEmpty() && connectedCount < targetConnectedCount)
                {
                    pos = nextPoses.poll();
                    for (EnumFacing face : EnumFacing.VALUES)
                    {
                        BlockPos adjPos = pos.offset(face);
                        state = getWorld().getBlockState(adjPos);
                        if (isDarkOak(state) && !visited.contains(adjPos))
                        {
                            nextPoses.offer(adjPos);
                            visited.add(adjPos);
                            connectedCount++;
                        }
                    }
                }
            }
            if (connectedCount >= targetConnectedCount)
                return true;
        }
        return false;
    }
    
    public void toggleTreeIndicator()
    {
        enableTreeIndicator = !enableTreeIndicator;
        sendChat("FT-TreeIndicator: toggled " + enableTreeIndicator);
    }
    
    public void debugTreeIndicator(int connectedCount)
    {
        if (connectedCount < 0)
            connectedCount = 15;
        MovingObjectPosition[] traces = RayTraceUtils.getRayTraceFromEntity(getWorld(), getPlayer(), false, 5.0);
        for (MovingObjectPosition pos : traces)
        {
            if (pos.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
            {
                boolean grown = doTreeGrowthSearch(pos.getBlockPos(), connectedCount);
                sendChat("FT-TreeIndicator: result = " + grown);
                return;
            }
        }
        sendChat("FT-TreeIndicator: no block is looked at");
    }
}
