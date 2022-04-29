package a7.tweakception.utils;

import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.entity.Entity;
import net.minecraft.util.*;
import net.minecraft.world.World;

public class RayTraceUtils
{
    @Nonnull
    public static MovingObjectPosition getRayTraceFromEntity(World worldIn, Entity entityIn, boolean useLiquids)
    {
        double reach = 5.0;
        return getRayTraceFromEntity(worldIn, entityIn, useLiquids, reach);
    }

    @Nonnull
    public static MovingObjectPosition getRayTraceFromEntity(World worldIn, Entity entityIn, boolean useLiquids, double range)
    {
        Vec3 eyesVec = new Vec3(entityIn.posX, entityIn.posY + entityIn.getEyeHeight(), entityIn.posZ);
        Vec3 look = entityIn.getLook(1f);
        Vec3 rangedLookRot = new Vec3(look.xCoord * range, look.yCoord * range, look.zCoord * range);
        Vec3 lookVec = eyesVec.add(rangedLookRot);

        MovingObjectPosition result = worldIn.rayTraceBlocks(eyesVec, lookVec, useLiquids, false, false);

        if (result == null)
        {
            result = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, new Vec3(0, 0, 0), EnumFacing.UP, BlockPos.ORIGIN);
        }

        AxisAlignedBB bb = entityIn.getEntityBoundingBox().expand(rangedLookRot.xCoord, rangedLookRot.yCoord, rangedLookRot.zCoord).expand(1d, 1d, 1d);
        List<Entity> list = worldIn.getEntitiesWithinAABBExcludingEntity(entityIn, bb);

        double closest = result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ? eyesVec.distanceTo(result.hitVec) : Double.MAX_VALUE;
        MovingObjectPosition entityTrace = null;
        Entity targetEntity = null;

        for (Entity entity : list)
        {
            bb = entity.getEntityBoundingBox();
            MovingObjectPosition traceTmp = bb.calculateIntercept(lookVec, eyesVec);

            if (traceTmp != null)
            {
                double distance = eyesVec.distanceTo(traceTmp.hitVec);

                if (distance <= closest)
                {
                    targetEntity = entity;
                    entityTrace = traceTmp;
                    closest = distance;
                }
            }
        }

        if (targetEntity != null)
        {
            result = new MovingObjectPosition(targetEntity, entityTrace.hitVec);
        }

        return result;
    }
}