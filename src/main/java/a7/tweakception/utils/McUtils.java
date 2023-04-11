package a7.tweakception.utils;

import a7.tweakception.mixin.AccessorMinecraft;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.event.ClickEvent;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.*;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.util.Constants;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class McUtils
{
    private static Minecraft mc = Minecraft.getMinecraft();
    private static Timer mcTimer = ((AccessorMinecraft) mc).getTimer();
    private static final Matcher COLOR_MATCHER = Pattern.compile("§[0-9a-fk-orA-FK-OR]").matcher("");
    
    public static Minecraft getMc()
    {
        return mc;
    }
    
    public static EntityPlayerSP getPlayer()
    {
        return mc.thePlayer;
    }
    
    public static WorldClient getWorld()
    {
        return mc.theWorld;
    }
    
    public static float getPartialTicks()
    {
        return mcTimer.renderPartialTicks;
    }
    
    public static boolean isInGame()
    {
        return getWorld() != null && getPlayer() != null;
    }
    
    public static void executeCommand(String cmd)
    {
        if (ClientCommandHandler.instance.executeCommand(getPlayer(), cmd) == 0)
            getPlayer().sendChatMessage(cmd);
    }
    
    public static <T> T sendDebug(T v)
    {
        getPlayer().addChatMessage(new ChatComponentText(String.valueOf(v)));
        return v;
    }
    
    public static void sendChat(String s)
    {
        getPlayer().addChatMessage(new ChatComponentText(s));
    }
    
    public static void sendChatf(String s, Object... args)
    {
        getPlayer().addChatMessage(new ChatComponentText(String.format(s, args)));
    }
    
    public static boolean checkStackInInv(IInventory inv, int slot, Block block, String name)
    {
        return checkStackInInv(inv, slot, Item.getItemFromBlock(block), name);
    }
    
    public static boolean checkStackInInv(IInventory inv, int slot, Item item, String name)
    {
        ItemStack stack = inv.getStackInSlot(slot);
        return stack != null &&
            stack.getItem() == item &&
            (name == null || stack.getDisplayName().equals(name));
    }
    
    public static String[] getDisplayLore(ItemStack stack)
    {
        if (stack == null)
            return null;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null)
        {
            NBTTagCompound display = nbt.getCompoundTag("display");
            if (display != null)
            {
                NBTTagList lore = display.getTagList("Lore", Constants.NBT.TAG_STRING);
                if (lore.tagCount() > 0)
                {
                    String[] a = new String[lore.tagCount()];
                    for (int i = 0; i < lore.tagCount(); i++)
                    {
                        a[i] = lore.getStringTagAt(i);
                    }
                    return a;
                }
            }
        }
        return null;
    }
    
    public static NBTTagCompound getExtraAttributes(ItemStack item)
    {
        if (item == null)
            return null;
        
        NBTTagCompound tag = item.getTagCompound();
        if (tag != null)
        {
            return tag.getCompoundTag("ExtraAttributes");
        }
        return null;
    }
    
    // Gets the nbt from a path defined like "tag1.tag2[5].tag"
    public static NBTBase getNbt(ItemStack stack, String path)
    {
        return getNbt(stack.getTagCompound(), path);
    }
    
    // Gets the nbt from ExtraAttributes from a path defined like "tag1.tag2[5].tag"
    public static NBTBase getExtraAttributes(ItemStack stack, String path)
    {
        return getNbt(getExtraAttributes(stack), path);
    }
    
    // Gets the nbt from a path defined like "tag1.tag2[5].tag"
    public static NBTBase getNbt(NBTBase nbt, String path)
    {
        if (nbt == null)
            return null;
        NBTBase cur = nbt;
        String[] eles = path.split("\\.");
        for (String ele : eles)
        {
            if (cur.getId() != NbtType.COMPOUND)
                return null;
            
            NBTTagCompound tagCompound = (NBTTagCompound) cur;
            cur = tagCompound.getTag(ele);
            
            if (cur == null)
                return null;
            
            if (ele.endsWith("]"))
            {
                if (!(cur instanceof NBTTagList))
                    return null;
                NBTTagList list = (NBTTagList) cur;
                int index = Integer.parseInt(
                    ele.substring(
                        ele.indexOf('['),
                        ele.length() - 1
                    )
                ) - 1;
                if (index >= list.tagCount())
                    return null;
                cur = list.get(index);
            }
        }
        return cur;
    }
    
    public static String getArmorStandHeadTexture(EntityArmorStand armorStand)
    {
        return getSkullTexture(armorStand.getCurrentArmor(3));
    }
    
    public static String getSkullTexture(ItemStack stack)
    {
        if (stack == null)
            return null;
        if (stack.getItem() != Items.skull)
            return null;
        
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null)
            return null;
        if (!tag.hasKey("SkullOwner"))
            return null;
        tag = tag.getCompoundTag("SkullOwner");
        if (!tag.hasKey("Properties"))
            return null;
        tag = tag.getCompoundTag("Properties");
        if (!tag.hasKey("textures"))
            return null;
        NBTTagList list = tag.getTagList("textures", Constants.NBT.TAG_COMPOUND);
        if (list.tagCount() == 0)
            return null;
        return list.getCompoundTagAt(0).getString("Value");
    }
    
    public static Entity getNewestEntityInAABB(Entity entity, AxisAlignedBB bb, Predicate<? super Entity> predicate)
    {
        Entity newest = null;
        int newestId = -1;
        List<Entity> entities = getWorld().getEntitiesInAABBexcluding(entity, bb, predicate::test);
        for (Entity e : entities)
        {
            int id = e.getEntityId();
            if (id > newestId)
            {
                newestId = id;
                newest = e;
            }
        }
        return newest;
    }
    
    public static Entity getNearestEntityInAABB(Entity entity, AxisAlignedBB bb, Predicate<? super Entity> predicate)
    {
        Entity nearest = null;
        double nearestDis = Double.MAX_VALUE;
        List<Entity> entities = getWorld().getEntitiesInAABBexcluding(entity, bb, predicate::test);
        for (Entity e : entities)
        {
            double dis = e.getDistanceSqToEntity(entity);
            if (dis < nearestDis)
            {
                nearestDis = dis;
                nearest = e;
            }
        }
        return nearest;
    }
    
    public static Entity getNearestEntityInAABBFromCollection(Collection<Entity> collection,
                                                              Entity entity,
                                                              AxisAlignedBB bb,
                                                              Predicate<? super Entity> predicate)
    {
        Entity nearest = null;
        double nearestDis = Double.MAX_VALUE;
        for (Entity e : collection)
        {
            if (e.getEntityBoundingBox().intersectsWith(bb))
            {
                double dis = e.getDistanceSqToEntity(entity);
                if (dis < nearestDis)
                {
                    nearestDis = dis;
                    nearest = e;
                }
            }
        }
        return nearest;
    }
    
    public static int getChessboardDistance(ChunkCoordIntPair a, ChunkCoordIntPair b)
    {
        return Math.max(
            Math.abs(a.chunkXPos - b.chunkXPos),
            Math.abs(a.chunkZPos - b.chunkZPos));
    }
    
    public static String cleanColor(String s)
    {
        return COLOR_MATCHER.reset(s).replaceAll("");
    }
    
    public static String cleanDuplicateColorCodes(String line)
    {
        StringBuilder sb = new StringBuilder();
        char currentColourCode = 'r';
        boolean sectionSymbolLast = false;
        for (char c : line.toCharArray())
        {
            if ((int) c > 50000) continue;
            
            if (c == '§')
            {
                sectionSymbolLast = true;
            }
            else
            {
                if (sectionSymbolLast)
                {
                    if (currentColourCode != c)
                    {
                        sb.append('\u00a7');
                        sb.append(c);
                        currentColourCode = c;
                    }
                    sectionSymbolLast = false;
                }
                else
                {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }
    
    public static void playCoolDing()
    {
        playCoolDing(0.943f);
    }
    
    public static void playCoolDing(float pitch)
    {
        EntityPlayerSP p = McUtils.getPlayer();
        ISound sound = new PositionedSoundRecord(new ResourceLocation("random.orb"),
            1.0f, pitch, (float) p.posX, (float) p.posY, (float) p.posZ);
        
        float oldLevel = getMc().gameSettings.getSoundLevel(SoundCategory.PLAYERS);
        getMc().gameSettings.setSoundLevel(SoundCategory.PLAYERS, 1);
        getMc().getSoundHandler().playSound(sound);
        getMc().gameSettings.setSoundLevel(SoundCategory.PLAYERS, oldLevel);
    }
    
    public static IChatComponent makeFileLink(File file)
    {
        IChatComponent link = new ChatComponentText(file.getName());
        link.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));
        link.getChatStyle().setUnderlined(true);
        return link;
    }
}
