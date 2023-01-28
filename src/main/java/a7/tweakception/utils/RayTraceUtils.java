package a7.tweakception.utils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class RayTraceUtils
{
    public static class RayTraceResult
    {
        public IBlockState state;
        public BlockPos pos;
        
        public RayTraceResult(IBlockState state, BlockPos pos)
        {
            this.state = state;
            this.pos = pos;
        }
    }
    
    public static RayTraceResult rayTraceBlock(EntityPlayerSP player, float partialTicks, float dist, float step)
    {
        Vector3f pos = new Vector3f((float) player.posX, (float) player.posY + player.getEyeHeight(), (float) player.posZ);
        
        Vec3 lookVec3 = player.getLook(partialTicks);
        
        Vector3f look = new Vector3f((float) lookVec3.xCoord, (float) lookVec3.yCoord, (float) lookVec3.zCoord);
        look.scale(step / look.length());
        
        int stepCount = (int) Math.ceil(dist / step);
        
        for (int i = 0; i < stepCount; i++)
        {
            Vector3f.add(pos, look, pos);
            
            WorldClient world = Minecraft.getMinecraft().theWorld;
            BlockPos position = new BlockPos(pos.x, pos.y, pos.z);
            IBlockState state = world.getBlockState(position);
            
            if (state.getBlock() != Blocks.air)
            {
                Vector3f.sub(pos, look, pos);
                look.scale(0.1f);
                
                for (int j = 0; j < 10; j++)
                {
                    Vector3f.add(pos, look, pos);
                    
                    BlockPos position2 = new BlockPos(pos.x, pos.y, pos.z);
                    IBlockState state2 = world.getBlockState(position2);
                    
                    if (state2.getBlock() != Blocks.air)
                    {
                        return new RayTraceResult(state2, position2);
                    }
                }
                
                return new RayTraceResult(state, position);
            }
        }
        
        return null;
    }
    
    public static MovingObjectPosition[] getRayTraceFromEntity(World worldIn, Entity entityIn, boolean useLiquids)
    {
        double reach = 5.0;
        return getRayTraceFromEntity(worldIn, entityIn, useLiquids, reach);
    }
    
    public static MovingObjectPosition[] getRayTraceFromEntity(World worldIn, Entity entityIn, boolean useLiquids, double range)
    {
        Vec3 eyePos = new Vec3(entityIn.posX, entityIn.posY + entityIn.getEyeHeight(), entityIn.posZ);
        Vec3 look = entityIn.getLook(1f);
        Vec3 rangedLookRot = new Vec3(look.xCoord * range, look.yCoord * range, look.zCoord * range);
        Vec3 lookVec = eyePos.add(rangedLookRot);
        
        MovingObjectPosition result = worldIn.rayTraceBlocks(eyePos, lookVec, useLiquids, false, false);
        
        if (result == null)
        {
            result = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, new Vec3(0, 0, 0), EnumFacing.UP, BlockPos.ORIGIN);
        }
        
        AxisAlignedBB bb = entityIn.getEntityBoundingBox().expand(rangedLookRot.xCoord, rangedLookRot.yCoord, rangedLookRot.zCoord).expand(1d, 1d, 1d);
        List<Entity> list = worldIn.getEntitiesWithinAABBExcludingEntity(entityIn, bb);

//        double closest = result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ? eyePos.distanceTo(result.hitVec) : Double.MAX_VALUE;
        MovingObjectPosition entityTrace = null;
        Entity targetEntity = null;
        
        Map<Double, Pair<Entity, MovingObjectPosition>> hitEntities = new TreeMap<>();
        
        for (Entity entity : list)
        {
            bb = entity.getEntityBoundingBox();
            MovingObjectPosition traceTmp = bb.calculateIntercept(lookVec, eyePos);
            
            if (traceTmp != null)
            {
                double distance = eyePos.distanceTo(traceTmp.hitVec);
                hitEntities.put(distance, new Pair<>(entity, traceTmp));
            }
        }
        
        List<MovingObjectPosition> finalList = new ArrayList<>(10);
        
        if (hitEntities.size() > 0)
        {
            for (Pair<Entity, MovingObjectPosition> t : hitEntities.values())
                finalList.add(new MovingObjectPosition(t.a, t.b.hitVec));
        }
        else
        {
            finalList.add(result);
        }
        
        
        return finalList.toArray(new MovingObjectPosition[0]);
    }
}