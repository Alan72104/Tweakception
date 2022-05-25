package a7.tweakception.utils;

import net.minecraft.entity.Entity;
import net.minecraft.util.*;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class RayTraceUtils
{
    @Nonnull
    public static MovingObjectPosition[] getRayTraceFromEntity(World worldIn, Entity entityIn, boolean useLiquids)
    {
        double reach = 5.0;
        return getRayTraceFromEntity(worldIn, entityIn, useLiquids, reach);
    }

    @Nonnull
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