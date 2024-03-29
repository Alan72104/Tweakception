package a7.tweakception.utils;

import a7.tweakception.Tweakception;
import a7.tweakception.mixin.AccessorGuiPlayerTabOverlay;
import com.google.common.base.Optional;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static a7.tweakception.utils.McUtils.*;

public class DumpUtils
{
    public static void doLookTrace(World world, EntityPlayer entity, double range, boolean adjacent, boolean useLiquids)
    {
        MovingObjectPosition[] traces = RayTraceUtils.getRayTraceFromEntity(world, entity, useLiquids, range);
        
        if (traces[0].typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
        {
            BlockPos pos = adjacent ? traces[0].getBlockPos().offset(traces[0].sideHit) : traces[0].getBlockPos();
            dumpBlock(world, entity, pos);
        }
        else if (traces[0].typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY)
        {
            for (MovingObjectPosition trace : traces)
                dumpEntity(trace.entityHit);
        }
        else
        {
            sendChat("Not currently looking at anything within range");
        }
    }
    
    public static void dumpPlayerInfoMap()
    {
        try
        {
            NetworkPlayerInfo[] infoMap =
                AccessorGuiPlayerTabOverlay.getTabListOrdering()
                .sortedCopy(getMc().getNetHandler().getPlayerInfoMap())
                .toArray(new NetworkPlayerInfo[0]);
            
            String[][] data = new String[infoMap.length + 1][7];
            
            GuiPlayerTabOverlay tabList = getMc().ingameGUI.getTabList();
            
            data[0][0] = "display name";
            data[0][1] = "tab list name";
            data[0][2] = "profile name";
            data[0][3] = "profile id";
            data[0][4] = "response time";
            data[0][5] = "location skin";
            data[0][6] = "location cape";
            for (int i = 0; i < infoMap.length; i++)
            {
                NetworkPlayerInfo info = infoMap[i];
                data[i+1][0] = info.getDisplayName() == null ? "null" : info.getDisplayName().getFormattedText();
                data[i+1][1] = tabList.getPlayerName(info);
                data[i+1][2] = info.getGameProfile() == null ? "null" : info.getGameProfile().getName();
                data[i+1][3] = info.getGameProfile() == null ? "null" : info.getGameProfile().getId() == null ? "null" : info.getGameProfile().getId().toString();
                data[i+1][4] = String.valueOf(info.getResponseTime());
                data[i+1][5] = info.getLocationSkin() == null ? "null" : info.getLocationSkin().toString();
                data[i+1][6] = info.getLocationCape() == null ? "null" : info.getLocationCape().toString();
            }
            
            List<String> lines = get2dArrayMatrixString(data);
            
            File file = Tweakception.configuration.createWriteFileWithCurrentDateTime("playerinfomap_$.txt", lines);
            
            sendChat("Dumped player info map");
            getPlayer().addChatMessage(new ChatComponentTranslation("Output written to file %s",
                McUtils.makeFileLink(file)));
        }
        catch (Exception e)
        {
            sendChat("Exception occurred while dumping");
            sendChat(e.toString());
            e.printStackTrace();
        }
    }
    
    public static List<String> get2dArrayMatrixString(String[][] data)
    {
        List<String> lines = new ArrayList<>();
        
        int[] widths = new int[data[0].length];
        for (String[] ele : data)
            for (int i = 0; i < widths.length; i++)
                widths[i] = Math.max(widths[i], ele[i].length());
        
        List<String> formats = new ArrayList<>();
        for (int i : widths)
            formats.add("%-" + i + "s");
        String format = String.join("|", formats);
        
        for (String[] ele : data)
            lines.add(String.format(format, (Object[]) ele));
        
        return lines;
    }
    
    public static void dumpEntitiesInRange(World world, EntityPlayer entity, double range)
    {
        try
        {
            AxisAlignedBB bb = entity.getEntityBoundingBox().expand(range, range, range);
            List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(entity, bb);
            list.sort((a, b) -> Float.compare(a.getDistanceToEntity(entity), b.getDistanceToEntity(entity)));
            for (Entity e : list)
                dumpEntity(e);
        }
        catch (Exception e)
        {
            sendChat("Exception occurred while dumping entities");
            sendChat(e.toString());
            e.printStackTrace();
        }
    }
    
    public static void dumpBlock(World world, EntityPlayer entity, BlockPos pos)
    {
        ArrayList<String> lines = new ArrayList<>();
        
        IBlockState blockState = world.getBlockState(pos);
        IBlockState actualState = blockState.getBlock().getActualState(blockState, world, pos);
        Block block = actualState.getBlock();
        
        int id = Block.getIdFromBlock(block);
        int meta = block.getMetaFromState(actualState);
        ItemStack stack = block.getPickBlock(RayTraceUtils.getRayTraceFromEntity(world, entity, true)[0], world, pos, entity);
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
            IExtendedBlockState extendedState = (IExtendedBlockState) anotherState;
            
            if (extendedState.getUnlistedProperties().size() > 0)
            {
                lines.add("IExtendedBlockState properties:");
                
                for (Map.Entry<IUnlistedProperty<?>, Optional<?>> entry : extendedState.getUnlistedProperties().entrySet())
                    lines.add(entry.getKey() + "[" +
                        "name=" + entry.getKey().getName() + "," +
                        "clazz=" + entry.getKey().getType() + "," +
                        "value=" + entry.getValue().toString() + "]");
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
            lines.add(prettifyJson(nbt.toString()));
        }
        
        try
        {
            File file = Tweakception.configuration.createWriteFileWithCurrentDateTime("block_$_" +
                displayName.substring(0, Math.min(displayName.length(), 20)) + ".txt", lines);
            
            sendChat("Dumped block (" + displayName + "§r)");
            getPlayer().addChatMessage(new ChatComponentTranslation("Output written to file %s",
                McUtils.makeFileLink(file)));
        }
        catch (IOException e)
        {
            sendChat("Exception occurred when writing block dump file");
            Tweakception.logger.error("Exception occurred when writing block dump file", e);
        }
    }
    
    public static void dumpEntity(Entity entity)
    {
        ArrayList<String> lines = new ArrayList<String>();
        
        String regName = EntityList.getEntityString(entity);
        if (regName == null)
            regName = "null";
        
        lines.add(String.format("Entity: %s [registry name: %s] (entityId: %d)", entity.getName(), regName, entity.getEntityId()));
        
        NBTTagCompound nbt = new NBTTagCompound();
        
        if (!entity.writeToNBTOptional(nbt))
        {
            entity.writeToNBT(nbt);
        }
        
        lines.add("Entity class: " + entity.getClass().getName());
        lines.add("");
        
        if (entity instanceof EntityLivingBase)
        {
            Collection<PotionEffect> effects = ((EntityLivingBase) entity).getActivePotionEffects();
            
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
        
        lines.add(prettifyJson(nbt.toString()));
        lines.add("");
        
        if (entity instanceof AbstractClientPlayer)
        {
            lines.add("Entity is of AbstractClientPlayer");
            AbstractClientPlayer player = (AbstractClientPlayer) entity;
            ResourceLocation skinLocation = player.getLocationSkin();
            lines.add("Skin location: " + skinLocation.toString());
            
            NetworkPlayerInfo info = getMc().getNetHandler().getPlayerInfo(player.getUniqueID());
            if (info != null)
            {
                GameProfile profile = info.getGameProfile();
                lines.add("Entity has NetworkPlayerInfo");
                lines.add("Name: " + profile.getName());
                lines.add("Id: " + profile.getId());
                lines.add("Properties:");
                lines.add(prettifyJson(
                    new PropertyMap.Serializer().serialize(profile.getProperties(), null, null).toString()));
            }
            else
                lines.add("AbstractClientPlayer doesn't have NetworkPlayerInfo");
            lines.add("");
        }
        
        try
        {
            String name = McUtils.cleanColor(entity.getName());
            File file = Tweakception.configuration.createWriteFileWithCurrentDateTime("entity_$_" +
                name.substring(0, Math.min(name.length(), 20)) + ".txt", lines);
            
            sendChat("Dumped entity (" + entity.getName() + "§r)");
            getPlayer().addChatMessage(new ChatComponentTranslation("Output written to file %s",
                McUtils.makeFileLink(file)));
        }
        catch (IOException e)
        {
            sendChat("Exception occurred when writing block dump file");
            Tweakception.logger.error("Exception occurred when writing block dump file", e);
        }
    }
    
    public static String prettifyJson(String s)
    {
        StringBuilder sb = new StringBuilder(s);
        int indentCount = 0;
        String indent = "    ";
        boolean escape = false;
        int escapeCount = 0;
        boolean inQuote = false;
        String sep = System.lineSeparator();
        int lastProcessedIndex = 0;
        int stringQuoteType = 0; // 1 = single quote
        
        try
        {
            for (int i = 0; i < sb.length(); )
            {
                switch (sb.charAt(i))
                {
                    case '{':
                    case '[':
                        if (!inQuote)
                            indentCount++;
                    case ',':
                        if (!inQuote)
                        {
                            sb.insert(i + 1, sep + Utils.stringRepeat(indent, indentCount));
                            i += indent.length() * indentCount + sep.length() + 1;
                        }
                        else
                            i++;
                        break;
                    case '}':
                    case ']':
                        if (!inQuote)
                        {
                            indentCount--;
                            sb.insert(i, System.lineSeparator() + Utils.stringRepeat(indent, indentCount));
                            i += indent.length() * indentCount + sep.length();
                        }
                        i++;
                        break;
                    case '\\':
                        escape = !escape;
                        if (escape)
                            escapeCount = 0;
                        i++;
                        break;
                    case '\'':
                        if (!escape)
                        {
                            if (inQuote)
                            {
                                if (stringQuoteType == 1)
                                    inQuote = false;
                            }
                            else
                            {
                                stringQuoteType = 1;
                                inQuote = true;
                            }
                        }
                        i++;
                        break;
                    case '"':
                        if (!escape)
                        {
                            if (inQuote)
                            {
                                if (stringQuoteType == 0)
                                    inQuote = false;
                            }
                            else
                            {
                                stringQuoteType = 0;
                                inQuote = true;
                            }
                        }
                        i++;
                        break;
                    default:
                        i++;
                        break;
                }
                if (escape)
                {
                    escapeCount++;
                    if (escapeCount >= 2)
                        escape = false;
                }
                lastProcessedIndex = i;
            }
        }
        catch (Exception e)
        {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            String trace = writer.toString();
            return "Failed to prettify json, failed at pos " + lastProcessedIndex + sep +
                "==========" + sep +
                trace +
                "==========" + sep +
                "Original string and failed pos:" + sep +
                s + sep +
                Utils.stringRepeat(" ", lastProcessedIndex) + "^";
        }
        
        return sb.toString();
    }
}
