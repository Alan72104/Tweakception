package a7.tweakception.utils;

import a7.tweakception.Tweakception;
import com.google.common.base.Optional;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.common.registry.GameData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;

import static a7.tweakception.utils.McUtils.getPlayer;
import static a7.tweakception.utils.McUtils.sendChat;

public class DataDumps
{
    public static void doLookTrace(World world, EntityPlayer entity, double range, boolean adjacent, boolean useLiquids)
    {
        MovingObjectPosition trace = RayTraceUtils.getRayTraceFromEntity(world, entity, useLiquids, range);

        ArrayList<String> lines = new ArrayList<String>();

        if (trace.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
        {
            BlockPos pos = adjacent ? trace.getBlockPos().offset(trace.sideHit) : trace.getBlockPos();

            IBlockState blockState = world.getBlockState(pos);
            IBlockState actualState = blockState.getBlock().getActualState(blockState, world, pos);
            Block block = actualState.getBlock();

            int id = Block.getIdFromBlock(block);
            int meta = block.getMetaFromState(actualState);
            ItemStack stack = block.getPickBlock(RayTraceUtils.getRayTraceFromEntity(world, entity, true), world, pos, entity);
            String registryName = block.getRegistryName();
            if (registryName == null)
                registryName = "<null>";
            String displayName;

            if (stack == null)
                // Blocks that are not obtainable/don't have an ItemBlock
                displayName = registryName;
            else
                displayName = stack.getDisplayName();


            String teInfo = "";
            boolean teInWorld = world.getTileEntity(pos) != null;
            boolean shouldHaveTE = block.hasTileEntity(actualState);

            if (teInWorld == shouldHaveTE)
            {
                teInfo = teInWorld ? "has a TileEntity" : "no TileEntity";
            }
            else
            {
                teInfo = teInWorld ? "!! is not supposed to have a TileEntity, but there is one in the world !!" :
                        "!! is supposed to have a TileEntity, but there isn't one in the world !!";
            }

            lines.add(String.format("%s (%s - %d:%d) %s", displayName, registryName, id, meta, teInfo));

            lines.add(String.format("Full block state: %s", actualState));
            lines.add(String.format("Hardness: %.4f, Explosion resistance: %.4f, Material: %s",
                    block.getBlockHardness(world, pos),
                    block.getExplosionResistance(world, pos, null, new Explosion(world, null, pos.getX(), pos.getY(), pos.getZ(), 2, true, true)),
                    block.getMaterial().getClass().getName()));
            lines.add("Block class: " + block.getClass().getName());

            if (actualState.getProperties().size() > 0)
            {
                lines.add("IBlockState properties, including getActualState():");

                for (Map.Entry<IProperty, Comparable> entry : actualState.getProperties().entrySet())
                    lines.add(entry.getKey().toString() + ": " + entry.getValue().toString());
            }
            else
            {
                lines.add("IBlockState properties: <none>");
            }

            IBlockState anotherState = actualState;
            try
            {
                anotherState = block.getExtendedState(anotherState, world, pos);
            }
            catch (Exception e)
            {
                Tweakception.logger.error("getFullBlockInfo(): Exception while calling getExtendedState() on the block");
            }

            if (anotherState instanceof IExtendedBlockState)
            {
                IExtendedBlockState extendedState = (IExtendedBlockState)anotherState;

                if (extendedState.getUnlistedProperties().size() > 0)
                {
                    lines.add("IExtendedBlockState properties:");

                    for (Map.Entry<IUnlistedProperty<?>, Optional<?>> entry : extendedState.getUnlistedProperties().entrySet())
                        lines.add(new StringJoiner(", ", entry.getKey() + "[", "]")
                                .add("name=" + entry.getKey().getName())
                                .add("clazz=" + entry.getKey().getType())
                                .add("value=" + entry.getValue().toString()).toString());
                }
            }

            TileEntity te = world.getTileEntity(pos);

            if (te != null)
            {
                NBTTagCompound nbt = new NBTTagCompound();
                te.writeToNBT(nbt);
                lines.add("TileEntity class: " + te.getClass().getName());
                lines.add("");
                lines.add("TileEntity NBT (from TileEntity#writeToNBT()):");
                lines.add(nbt.toString());
            }

            try
            {
                File file = Tweakception.configuration.createWriteFileWithCurrentDateTimeSuffix("block_and_tileentity_data.txt", lines);

                IChatComponent name = new ChatComponentText(file.getName());
                name.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));
                name.getChatStyle().setUnderlined(true);

                getPlayer().addChatMessage(new ChatComponentTranslation("Output written to file %s", name));
            }
            catch (IOException e)
            {
                sendChat("doLookTrace(): exception occurred when writing block dump file");
                Tweakception.logger.error("doLookTrace(): exception occurred when writing block dump file", e);
            }
        }
        else if (trace.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY)
        {
            Entity target = trace.entityHit;

            String regName = EntityList.getEntityString(target);
            if (regName == null)
                regName = "null";

            lines.add(String.format("Entity: %s [registry name: %s] (entityId: %d)", target.getName(), regName, target.getEntityId()));

            NBTTagCompound nbt = new NBTTagCompound();

            if (!target.writeToNBTOptional(nbt))
            {
                target.writeToNBT(nbt);
            }

            lines.add("Entity class: " + target.getClass().getName());
            lines.add("");

            if (target instanceof EntityLivingBase)
            {
                Collection<PotionEffect> effects = entity.getActivePotionEffects();

                if (!effects.isEmpty())
                {
                    lines.add("Potion effects of current entity:");
                    lines.add("Effect, Amplifier, Duration, Ambient");
                    for (PotionEffect effect : effects)
                    {
                        ResourceLocation rl = GameData.getPotionRegistry().getNameForObject(Potion.potionTypes[effect.getPotionID()]);

                        lines.add(
                                (rl != null ? rl.toString() : effect.getClass().getName()) + ", " +
                                        effect.getAmplifier() + ", " +
                                        effect.getDuration() + ", " +
                                        effect.getIsAmbient());
                    }
                }
                lines.add("");
            }

            lines.add(nbt.toString());

            try
            {
                File file = Tweakception.configuration.createWriteFileWithCurrentDateTimeSuffix("entity_data.txt", lines);

                IChatComponent name = new ChatComponentText(file.getName());
                name.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));
                name.getChatStyle().setUnderlined(true);

                getPlayer().addChatMessage(new ChatComponentTranslation("Output written to file %s", name));
            }
            catch (IOException e)
            {
                sendChat("doLookTrace(): exception occurred when writing block dump file");
                Tweakception.logger.error("doLookTrace(): exception occurred when writing block dump file", e);
            }
        }
        else
        {
            sendChat("Tweakception: not currently looking at anything within range");
        }
    }
}
