package a7.tweakception.utils;

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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class McUtils
{
    private static final Matcher COLOR_MATCHER = Pattern.compile("§[0-9a-fk-or]").matcher("");
    
    public static Minecraft getMc()
    {
        return Minecraft.getMinecraft();
    }
    
    public static EntityPlayerSP getPlayer()
    {
        return getMc().thePlayer;
    }
    
    public static WorldClient getWorld()
    {
        return getMc().theWorld;
    }
    
    public static boolean isInGame()
    {
        return getWorld() != null && getPlayer() != null;
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
            
            if (c == '\u00a7')
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
        EntityPlayerSP p = McUtils.getPlayer();
        ISound sound = new PositionedSoundRecord(new ResourceLocation("random.orb"),
            1.0f, 0.943f, (float) p.posX, (float) p.posY, (float) p.posZ);
        
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
