package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import a7.tweakception.utils.*;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

import static a7.tweakception.tweaks.GlobalTweaks.getCurrentIsland;
import static a7.tweakception.tweaks.GlobalTweaks.getTicks;
import static a7.tweakception.utils.McUtils.*;

public class GiftTweaks extends Tweak
{
    public static class GiftTweaksConfig
    {
        public boolean giftFeatures = false;
        public boolean invFeatures = false;
        public int invFeaturesMinDelay = 0;
        public boolean autoReleaseRightClick = false;
        public int autoReleaseRightClickDistance = 30;
        public TreeSet<String> autoReleaseRightClickWhitelist = new TreeSet<>();
        public boolean autoSwitchGiftSlot = false;
        public boolean isRecipient = false;
        public boolean autoSetTargeting = false;
        public boolean targetingDisableArmorStand = false;
        public boolean targetingOnlyOpenableGift = false;
    }
    private final GiftTweaksConfig c;
    private static final Map<String, Predicate<ItemStack>> GIFT_SHITS = new HashMap<>();
    private static final String GIFT_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTBmNTM5ODUxMGIxYTA1YWZjNWIyMDFlYWQ4YmZjNTgzZTU3ZDcyMDJmNTE5M2IwYjc2MWZjYmQwYWUyIn19fQ==";
    private InvFeature invFeature = InvFeature.None;
    private int invFeatureIndex = 0;
    private int invFeatureLastTicks = 0;
    private int invFeatureClickDelay = 0;
    private boolean stashEmptied = false;
    private int lastPickupStashTicks = 0;
    private boolean trackWhiteGifts = false;
    private Map<Entity, PosMark> whiteGifts = new HashMap<>();
    // Entity, detection start tick, start pos
    private Map<Entity, Pair<Integer, Vec3>> whiteGiftsTemp = new HashMap<>();
    
    private enum InvFeature
    {
        None, DropGiftShit, MoveGift, AutoDropGiftShit, DumpItems
    }
    
    static
    {
        GIFT_SHITS.put("BATTLE_DISC", null);
        GIFT_SHITS.put("WINTER_DISC", null);
        GIFT_SHITS.put("GLASS_BOTTLE", null);
        GIFT_SHITS.put("POTION", s -> McUtils.getExtraAttributes(s).getString("potion").endsWith("_xp_boost"));
        Map<String, Integer> crap = MapBuilder.stringIntHashMap()
            .put("scavenger", 4)
            .put("looting", 4)
            .put("luck", 6).map();
        GIFT_SHITS.put("ENCHANTED_BOOK", s ->
        {
            NBTTagCompound enchs = McUtils.getExtraAttributes(s).getCompoundTag("enchantments");
            Set<String> ids = enchs.getKeySet();
            if (ids.size() == 1)
            {
                String next = ids.iterator().next();
                return crap.containsKey(next) && crap.get(next) == enchs.getInteger(next);
            }
            return false;
        });
    }
    
    public GiftTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.giftTweaks;
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !c.giftFeatures)
            return;
        
        boolean isInv = getMc().currentScreen instanceof GuiInventory;
        boolean isChest = getMc().currentScreen instanceof GuiChest;
    
        if (c.invFeatures && (isInv || isChest))
        {
            if (invFeature == InvFeature.None)
            {
                if (isInv && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_D))
                {
                    invFeatureIndex = 9;
                    invFeature = InvFeature.AutoDropGiftShit;
                    stashEmptied = false;
                }
                else if (isInv && Keyboard.isKeyDown(Keyboard.KEY_D))
                {
                    invFeatureIndex = 9;
                    invFeature = InvFeature.DropGiftShit;
                }
                else if (isInv && Keyboard.isKeyDown(Keyboard.KEY_M))
                {
                    invFeatureIndex = 0;
                    invFeature = InvFeature.MoveGift;
                }
                else if (isChest && Keyboard.isKeyDown(Keyboard.KEY_D) && getCurrentIsland() == SkyblockIsland.PRIVATE_ISLAND)
                {
                    invFeatureIndex = 0;
                    invFeature = InvFeature.DumpItems;
                }
            }
        
            if (getTicks() - invFeatureLastTicks >= invFeatureClickDelay)
            {
                switch (invFeature)
                {
                    case DropGiftShit:
                        if (!doDropGiftShit())
                            invFeature = InvFeature.None;
                        break;
                    case MoveGift:
                        invMoveGift();
                        break;
                    case AutoDropGiftShit:
                        invAutoDropGiftShit();
                        break;
                    case DumpItems:
                        dumpItemsToChest();
                        break;
                }
            }
        }
        else
        {
            invFeature = InvFeature.None;
        }
    
        if (c.autoReleaseRightClick && getCurrentIsland() == SkyblockIsland.HUB)
        {
            if (getMc().gameSettings.keyBindUseItem.isKeyDown() &&
                Utils.findInHotbarById("WHITE_GIFT", "GREEN_GIFT", "RED_GIFT") != -1)
            {
                List<EntityPlayer> players = getWorld().playerEntities;
                EntityPlayer closePlayer = null;
                for (EntityPlayer p : players)
                {
                    if (p instanceof EntityOtherPlayerMP &&
                        getMc().getNetHandler().getPlayerInfo(p.getUniqueID()) != null &&
                        p.getDistanceSqToEntity(getPlayer()) <= c.autoReleaseRightClickDistance * c.autoReleaseRightClickDistance &&
                        !c.autoReleaseRightClickWhitelist.contains(p.getName().toLowerCase(Locale.ROOT)) &&
                        p.getDisplayName().getFormattedText().length() >= 4 &&
                        p.getDisplayName().getFormattedText().charAt(2) == '§')
                    {
                        closePlayer = p;
                        break;
                    }
                }
            
                if (closePlayer != null)
                {
                    KeyBinding.setKeyBindState(getMc().gameSettings.keyBindUseItem.getKeyCode(),
                        false);
                    sendChat("Gift: released use keybind because " + closePlayer.getName() + " came");
                }
            }
        }
    }
    
    public void onRenderLast(RenderWorldLastEvent event)
    {
        if (trackWhiteGifts)
        {
            for (PosMark pos : whiteGifts.values())
            {
                Color color;
                if (pos.isFound)
                    color = new Color(84, 166, 102, 128);
                else
                    color = new Color(206, 57, 199, 128);
                RenderUtils.drawBeaconBeamOrBoundingBox(new BlockPos(pos.getX(), pos.getY(), pos.getZ()),
                    color, event.partialTicks, 0);
            }
        }
    }
    
    public void onEntityUpdate(LivingEvent.LivingUpdateEvent event)
    {
        if (trackWhiteGifts && event.entity instanceof EntityArmorStand)
        {
            Entity entity = event.entity;
            EntityArmorStand stand = (EntityArmorStand) entity;
            String tex = McUtils.getArmorStandHeadTexture(stand);
            if (tex != null && tex.equals(GIFT_TEXTURE))
            {
                if (!whiteGifts.containsKey(entity))
                {
                    Pair<Integer, Vec3> pair = whiteGiftsTemp.computeIfAbsent(entity,
                        e -> new Pair<>(getTicks(), new Vec3(e.posX, e.posY, e.posZ)));
                    // 0 means failed
                    if (pair.a != 0)
                    {
                        if (getTicks() - pair.a >= 20)
                        {
                            whiteGiftsTemp.remove(entity);
                            whiteGifts.put(entity, new PosMark(entity.posX, entity.posY + 2, entity.posZ));
                        }
                        else
                        {
                            if ((entity.posX != pair.b.xCoord ||
                                entity.posY != pair.b.yCoord ||
                                entity.posZ != pair.b.zCoord) ||
                                getWorld().getEntitiesWithinAABB(EntityArmorStand.class,
                                    entity.getEntityBoundingBox().expand(2, 5, 2),
                                    e -> e.getName().startsWith("§eFrom: ")).size() > 0
                            )
                            {
                                // This entity failed the detection
                                whiteGiftsTemp.put(entity, new Pair<>(0, null));
                            }
                        }
                    }
                }
            }
        }
    }
    
    public void onWorldUnload(WorldEvent.Unload event)
    {
        trackWhiteGifts = false;
        whiteGifts.clear();
        whiteGiftsTemp.clear();
    }
    
    
    
    private boolean doDropGiftShit()
    {
        List<ItemStack> inv = ((GuiInventory) getMc().currentScreen).inventorySlots.getInventory();
        for (; invFeatureIndex <= 44; invFeatureIndex++)
        {
            ItemStack stack = inv.get(invFeatureIndex);
            String id = Utils.getSkyblockItemId(stack);
            if (stack != null && id != null && GIFT_SHITS.containsKey(id))
            {
                Predicate<ItemStack> itemPredicate = GIFT_SHITS.get(id);
                if (itemPredicate == null || itemPredicate.test(stack))
                {
                    getMc().playerController.windowClick(0, invFeatureIndex,
                        0, 4, getPlayer());
                    invFeatureLastTicks = getTicks();
                    invFeatureClickDelay = c.invFeaturesMinDelay + getWorld().rand.nextInt(3);
                    invFeatureIndex++;
                    return true;
                }
            }
        }
        invFeatureIndex = 9;
        return false;
    }
    
    private void invMoveGift()
    {
        List<ItemStack> inv = ((GuiInventory) getMc().currentScreen).inventorySlots.getInventory();
        HashSet<String> gifts = Utils.hashSet("WHITE_GIFT", "GREEN_GIFT", "RED_GIFT");
        // Hotbar index
        for (; invFeatureIndex <= 7; invFeatureIndex++)
        {
            {
                ItemStack stack = inv.get(36 + invFeatureIndex);
                String id = Utils.getSkyblockItemId(stack);
                if (stack != null && id != null && gifts.contains(id))
                    continue;
            }
            
            int invGiftSlot = 0;
            for (int backpack = 9; backpack <= 35; backpack++)
            {
                ItemStack stack = inv.get(backpack);
                String id = Utils.getSkyblockItemId(stack);
                if (stack != null && id != null && gifts.contains(id))
                {
                    invGiftSlot = backpack;
                    break;
                }
            }
            
            if (invGiftSlot != 0)
            {
                getMc().playerController.windowClick(0, invGiftSlot,
                    invFeatureIndex, 2, getPlayer());
                invFeatureLastTicks = getTicks();
                invFeatureClickDelay = c.invFeaturesMinDelay + getWorld().rand.nextInt(3);
                invFeatureIndex++;
                return;
            }
        }
        invFeature = InvFeature.None;
    }
    
    private void invAutoDropGiftShit()
    {
        boolean dropped = doDropGiftShit();
        if (!dropped)
        {
            if (getTicks() - lastPickupStashTicks >= 20 && stashEmptied)
            {
                invFeature = InvFeature.None;
                return;
            }
            
            if (getTicks() - lastPickupStashTicks >= 60)
            {
                lastPickupStashTicks = getTicks();
                getPlayer().sendChatMessage("/pickupstash");
            }
        }
    }
    
    private void dumpItemsToChest()
    {
        IInventory inv = McUtils.getOpenedChest();
        if (inv.getSizeInventory() != 54)
        {
            invFeature = InvFeature.None;
            return;
        }
        
        ItemStack firstStack = inv.getStackInSlot(0);
        if (firstStack == null)
        {
            invFeature = InvFeature.None;
            return;
        }
        
        boolean hasEmptySlot = false;
        for (int i = 0; i < 54; i++)
        {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack == null || stack.stackSize > 0 && stack.stackSize < 64)
            {
                hasEmptySlot = true;
                break;
            }
        }
        
        if (!hasEmptySlot)
        {
            invFeature = InvFeature.None;
            return;
        }
        
        String targetId = Utils.getSkyblockItemId(firstStack);
        if (targetId == null)
        {
            invFeature = InvFeature.None;
            return;
        }
        
        for (; invFeatureIndex < 36; invFeatureIndex++)
        {
            if (invFeatureIndex == 8)
                continue;
            
            ItemStack stack = getPlayer().inventory.getStackInSlot(invFeatureIndex);
            String id = Utils.getSkyblockItemId(stack);
            if (stack != null && id != null && id.equals(targetId))
            {
                if (invFeatureIndex < 9)
                    getMc().playerController.windowClick(getPlayer().openContainer.windowId, 54 + 27 + invFeatureIndex, 0, 1, getPlayer());
                else
                    getMc().playerController.windowClick(getPlayer().openContainer.windowId, 54 + invFeatureIndex - 9, 0, 1, getPlayer());
                invFeatureLastTicks = getTicks();
                invFeatureClickDelay = c.invFeaturesMinDelay + getWorld().rand.nextInt(3);
                invFeatureIndex++;
                return;
            }
        }
        
        invFeature = InvFeature.None;
    }
    
    public void onChatReceived(ClientChatReceivedEvent event)
    {
        String msg = event.message.getUnformattedText();
        if (event.type == 0 || event.type == 1)
        {
            if (invFeature == InvFeature.AutoDropGiftShit &&
                (msg.equals("Your stash isn't holding any item!") ||
                    msg.equals("You picked up all items from your item stash!") ||
                    msg.equals("Couldn't unstash your items! Your inventory is full!")))
            {
                stashEmptied = true;
            }
            else if (trackWhiteGifts &&
                (msg.startsWith("GIFT! You found a White Gift! ") ||
                    msg.equals("You have already found this Gift this year!")))
            {
                markNearestFound();
            }
        }
    }
    
    private void markNearestFound()
    {
        EntityPlayerSP player = getPlayer();
        double nearestDis = Double.MAX_VALUE;
        PosMark mark = null;
        for (Map.Entry<Entity, PosMark> entry : whiteGifts.entrySet())
        {
            double dX = player.posX - entry.getKey().posX;
            double dY = player.posY - entry.getKey().posY;
            double dZ = player.posZ - entry.getKey().posZ;
            double distanceSq = dX * dX + dY * dY + dZ * dZ;
            if (distanceSq < nearestDis)
            {
                nearestDis = distanceSq;
                mark = entry.getValue();
            }
        }
        if (mark != null && nearestDis <= 10 * 10)
        {
            mark.isFound = true;
        }
    }
    
    public boolean isGiftFeaturesOn()
    {
        return c.giftFeatures;
    }
    
    public boolean isAutoSwitchGiftSlotOn()
    {
        return c.giftFeatures && c.autoSwitchGiftSlot;
    }
    
    public boolean isDisableArmorStandTargetingOn()
    {
        return c.giftFeatures && c.targetingDisableArmorStand;
    }
    
    public boolean isOnlyTargetOpenableGiftOn()
    {
        return c.giftFeatures && c.targetingOnlyOpenableGift;
    }
    
    private static class PosMark extends Vec3i
    {
        public boolean isFound = false;
        
        public PosMark(int x, int y, int z)
        {
            super(x, y, z);
        }
        
        public PosMark(double x, double y, double z)
        {
            super(x, y, z);
        }
    }
    
    public void toggleGiftFeatures()
    {
        c.giftFeatures = !c.giftFeatures;
        sendChat("Gift: toggled all gift features " + c.giftFeatures);
        if (c.autoSetTargeting)
        {
            if (c.isRecipient)
            {
                c.targetingDisableArmorStand = false;
                c.targetingOnlyOpenableGift = c.giftFeatures;
                sendChat("Gift: only targeting gift is " + c.giftFeatures);
            }
            else
            {
                c.targetingDisableArmorStand = c.giftFeatures;
                c.targetingOnlyOpenableGift = false;
                sendChat("Gift: disable armor stand targeting is " + c.giftFeatures);
            }
        }
    }
    
    public void toggleInvFeatures()
    {
        c.invFeatures = !c.invFeatures;
        sendChat("Gift: toggled inv features " + c.invFeatures);
    }
    
    public void setInvFeaturesMinDelay(int ticks)
    {
        c.invFeaturesMinDelay =
            ticks == -1 ? new GiftTweaksConfig().invFeaturesMinDelay : Utils.clamp(ticks, 0, 20);
        sendChat("Gift: set inv features min delay ticks to " + c.invFeaturesMinDelay);
    }
    
    public void toggleAutoSwitchGiftSlot()
    {
        c.autoSwitchGiftSlot = !c.autoSwitchGiftSlot;
        sendChat("Gift: toggled auto switch gift slot " + c.autoSwitchGiftSlot);
    }
    
    public void toggleAutoReleaseRightClick()
    {
        c.autoReleaseRightClick = !c.autoReleaseRightClick;
        sendChat("Gift: toggled auto release right click " + c.autoReleaseRightClick);
    }
    
    public void setAutoReleaseRightClickDistance(int blocks)
    {
        c.autoReleaseRightClickDistance = Utils.clamp(blocks, 5, 500);
        sendChat("Gift: set the distance of a player needed to release right click to " + c.autoReleaseRightClickDistance);
    }
    
    public void toggleDisableArmorStandTargeting()
    {
        c.targetingDisableArmorStand = !c.targetingDisableArmorStand;
        sendChat("Gift: toggled disable armor stand targeting " + c.targetingDisableArmorStand);
    }
    
    public void toggleTargetOnlyOpenableGift()
    {
        c.targetingOnlyOpenableGift = !c.targetingOnlyOpenableGift;
        sendChat("Gift: toggled target only openable gifts " + c.targetingOnlyOpenableGift);
    }
    
    public void toggleRecipient()
    {
        c.isRecipient = !c.isRecipient;
        sendChat("Gift: toggled recipient " + c.isRecipient);
    }
    
    public void toggleAutoSetTargeting()
    {
        c.autoSetTargeting = !c.autoSetTargeting;
        sendChat("Gift: toggled auto set targeting " + c.autoSetTargeting);
        if (c.giftFeatures && c.autoSetTargeting)
        {
            if (c.isRecipient)
            {
                c.targetingDisableArmorStand = false;
                c.targetingOnlyOpenableGift = true;
                sendChat("Gift: gift features are on, enabled only targeting gift");
            }
            else
            {
                c.targetingDisableArmorStand = true;
                c.targetingOnlyOpenableGift = false;
                sendChat("Gift: gift features are on, disabled armor stand targeting");
            }
        }
    }
    
    public void setAutoReleaseRightClickWhitelist(String name)
    {
        if (name == null || name.isEmpty())
        {
            sendChat("Gift: printing list");
            int i = 0;
            for (String n : c.autoReleaseRightClickWhitelist)
                sendChat(++i + ": " + n);
            return;
        }
        
        name = name.toLowerCase(Locale.ROOT);
        if (c.autoReleaseRightClickWhitelist.contains(name))
        {
            c.autoReleaseRightClickWhitelist.remove(name);
            sendChat("Gift: removed " + name + " from whitelist");
        }
        else
        {
            c.autoReleaseRightClickWhitelist.add(name);
            sendChat("Gift: added " + name + " to whitelist");
        }
    }
    
    public void toggleWhiteGiftTracking()
    {
        trackWhiteGifts = !trackWhiteGifts;
        sendChat("Gift: toggled white gift tracking " + trackWhiteGifts);
        whiteGifts.clear();
        whiteGiftsTemp.clear();
    }
}
