package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import a7.tweakception.utils.*;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
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
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        public boolean throwValuableRewards = false;
        public boolean autoGiftAndRefillFromInv = false;
        public String autoGiftTarget = "";
        public TreeSet<String> giftRewardDumpBlacklist = new TreeSet<>();
    }
    private final GiftTweaksConfig c;
    // Sb id, shit predicate
    private static final Map<String, Predicate<ItemStack>> GIFT_SHITS = new HashMap<>();
    private static final Map<String, Predicate<ItemStack>> GIFT_SHITS_VALUABLE = new HashMap<>();
    private static final String GIFT_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTBmNTM5ODUxMGIxYTA1YWZjNWIyMDFlYWQ4YmZjNTgzZTU3ZDcyMDJmNTE5M2IwYjc2MWZjYmQwYWUyIn19fQ==";
    private static final Set<String> ALL_GIFT_IDS = Utils.hashSet("WHITE_GIFT", "GREEN_GIFT", "RED_GIFT");
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
    private boolean autoGifting = false;
    
    private enum InvFeature
    {
        None, DropGiftShit, MoveGift, AutoDropGiftShit, DumpItems
    }
    
    static
    {
        Predicate<ItemStack> always = s -> true;
        GIFT_SHITS.put("BATTLE_DISC", always);
        GIFT_SHITS.put("WINTER_DISC", always);
        GIFT_SHITS.put("GLASS_BOTTLE", always);
        for (String pot : Arrays.asList("COMBAT", "FARMING", "FORAGING", "FISHING", "MINING", "ALCHEMY", "ENCHANTING"))
        {
            for (int i = 1; i <= 3; i++)
                GIFT_SHITS.put("POTION_"+pot+"_XP_BOOST_"+i, always);
        }
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
        GIFT_SHITS_VALUABLE.put("SNOW_GENERATOR_1", always);
        GIFT_SHITS_VALUABLE.put("GIFT_THE_FISH", always);
        GIFT_SHITS_VALUABLE.put("SNOW_SUIT_HELMET", always);
        GIFT_SHITS_VALUABLE.put("SNOW_SUIT_CHESTPLATE", always);
        GIFT_SHITS_VALUABLE.put("SNOW_SUIT_LEGGINGS", always);
        GIFT_SHITS_VALUABLE.put("SNOW_SUIT_BOOTS", always);
        GIFT_SHITS_VALUABLE.put("RUNE", s ->
        {
            AutoRunes.RuneType type = AutoRunes.getRuneType(s);
            return type != null && type.level == 1 && type.name.equals("ICE");
        });
        GIFT_SHITS_VALUABLE.put("SANTA_PERSONALITY", always);
        GIFT_SHITS_VALUABLE.put("GRINCH_PERSONALITY", always);
        GIFT_SHITS_VALUABLE.put("GINGERBREAD_PERSONALITY", always);
        GIFT_SHITS_VALUABLE.put("PRESENTS", always);
        GIFT_SHITS_VALUABLE.put("NUTCRACKER", always);
        GIFT_SHITS_VALUABLE.put("BIG_XTREE", always);
    }
    
    public GiftTweaks(Configuration configuration)
    {
        super(configuration, "Gift");
        c = configuration.config.giftTweaks;
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
            onTickStart(event);
        else
            onTickEnd(event);
    }
    
    private void onTickStart(TickEvent.ClientTickEvent event)
    {
        if (!c.isRecipient)
        {
            EntityOtherPlayerMP targetPlayer = getMc().objectMouseOver == null
                ? null
                : getMc().objectMouseOver.entityHit instanceof EntityOtherPlayerMP
                    ? (EntityOtherPlayerMP) getMc().objectMouseOver.entityHit
                    : null;
            
            if (isAutoGiftingOn() &&
                (getMc().currentScreen == null || getMc().currentScreen instanceof GuiChat) &&
                targetPlayer != null &&
                targetPlayer.getName().equalsIgnoreCase(c.autoGiftTarget) &&
                ALL_GIFT_IDS.contains(Utils.getSkyblockItemId(getPlayer().getCurrentEquippedItem())) &&
                (getMc().gameSettings.keyBindUseItem.isKeyDown() || autoGifting) &&
                !getMc().gameSettings.keyBindAttack.isKeyDown()
            )
            {
                if (!autoGifting)
                    sendChat("Auto gifting started");
                autoGifting = true; // TODO: Handle clicking while in chat gui
                KeyBinding.setKeyBindState(
                    getMc().gameSettings.keyBindUseItem.getKeyCode(),
                    true);
            }
            else if (autoGifting)
            {
                sendChat("Auto gifting ended");
                autoGifting = false;
                KeyBinding.setKeyBindState(
                    getMc().gameSettings.keyBindUseItem.getKeyCode(),
                    false);
            }
        }
        else
        {
            // TODO: Handle gifting on recipient
        }
    }
    
    private void onTickEnd(TickEvent.ClientTickEvent event)
    {
        if (!c.giftFeatures)
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
                        invRefillGiftToHotbar();
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
                Utils.findInHotbarById(ALL_GIFT_IDS) != -1)
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
                    autoGifting = false;
                    KeyBinding.setKeyBindState(
                        getMc().gameSettings.keyBindUseItem.getKeyCode(),
                        false);
                    sendChat("Released use keybind because " + closePlayer.getName() + " came");
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
                                !getWorld().getEntitiesWithinAABB(EntityArmorStand.class,
                                    entity.getEntityBoundingBox().expand(2, 5, 2),
                                    e -> e.getName().startsWith("§eFrom: ")).isEmpty()
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
            String id = Utils.getSkyblockItemOrPotionId(stack);
            if (id == null)
                continue;
            boolean throwShit = GIFT_SHITS.containsKey(id) && GIFT_SHITS.get(id).test(stack);
            boolean throwValuable = c.throwValuableRewards && GIFT_SHITS_VALUABLE.containsKey(id) && GIFT_SHITS_VALUABLE.get(id).test(stack);
            if (!c.giftRewardDumpBlacklist.contains(id) && (throwShit || throwValuable))
            {
                getMc().playerController.windowClick(0, invFeatureIndex,
                    WindowClickContants.Drop.BTN_CTRL_DROP,
                    WindowClickContants.Drop.MODE, getPlayer());
                invFeatureLastTicks = getTicks();
                invFeatureClickDelay = c.invFeaturesMinDelay + getWorld().rand.nextInt(3);
                invFeatureIndex++;
                return true;
            }
        }
        invFeatureIndex = 9;
        return false;
    }
    
    private void invRefillGiftToHotbar()
    {
        List<ItemStack> inv = ((GuiInventory) getMc().currentScreen).inventorySlots.getInventory();
        // Hotbar index
        for (; invFeatureIndex < 8; invFeatureIndex++)
        {
            {
                ItemStack stack = inv.get(36 + invFeatureIndex);
                String id = Utils.getSkyblockItemId(stack);
                if (stack != null && id != null && ALL_GIFT_IDS.contains(id))
                    continue;
            }
            
            int invGiftSlot = Utils.findInInvById(ALL_GIFT_IDS);
            
            if (invGiftSlot != -1)
            {
                getMc().playerController.windowClick(0, invGiftSlot + 9,
                    invFeatureIndex,
                    WindowClickContants.Number.MODE, getPlayer());
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
                    getMc().playerController.windowClick(
                        getPlayer().openContainer.windowId,
                        54 + 27 + invFeatureIndex,
                        WindowClickContants.ShiftLeftRight.BTN_SHIFT_LEFT,
                        WindowClickContants.ShiftLeftRight.MODE,
                        getPlayer());
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
    
    public boolean isAutoGiftingOn()
    {
        return c.giftFeatures && c.autoGiftAndRefillFromInv;
    }
    
    public List<String> getRewardDumpBlacklist()
    {
        return new ArrayList<>(c.giftRewardDumpBlacklist);
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
        sendChat("Toggled all gift features " + c.giftFeatures);
        if (c.autoSetTargeting)
        {
            if (c.isRecipient)
            {
                c.targetingDisableArmorStand = false;
                c.targetingOnlyOpenableGift = c.giftFeatures;
                sendChat("Only targeting gift is " + c.giftFeatures);
            }
            else
            {
                c.targetingDisableArmorStand = c.giftFeatures;
                c.targetingOnlyOpenableGift = false;
                sendChat("Disable armor stand targeting is " + c.giftFeatures);
            }
        }
    }
    
    public void toggleInvFeatures()
    {
        c.invFeatures = !c.invFeatures;
        sendChat("Toggled inv features " + c.invFeatures);
    }
    
    public void setInvFeaturesMinDelay(int ticks)
    {
        c.invFeaturesMinDelay =
            ticks == -1 ? new GiftTweaksConfig().invFeaturesMinDelay : Utils.clamp(ticks, 0, 20);
        sendChat("Set inv features min delay ticks to " + c.invFeaturesMinDelay);
    }
    
    public void toggleAutoSwitchGiftSlot()
    {
        c.autoSwitchGiftSlot = !c.autoSwitchGiftSlot;
        sendChat("Toggled auto switch gift slot " + c.autoSwitchGiftSlot);
    }
    
    public void toggleAutoReleaseRightClick()
    {
        c.autoReleaseRightClick = !c.autoReleaseRightClick;
        sendChat("Toggled auto release right click " + c.autoReleaseRightClick);
    }
    
    public void setAutoReleaseRightClickDistance(int blocks)
    {
        c.autoReleaseRightClickDistance = Utils.clamp(blocks, 5, 500);
        sendChat("Set the distance of a player needed to release right click to " + c.autoReleaseRightClickDistance);
    }
    
    public void toggleDisableArmorStandTargeting()
    {
        c.targetingDisableArmorStand = !c.targetingDisableArmorStand;
        sendChat("Toggled disable armor stand targeting " + c.targetingDisableArmorStand);
    }
    
    public void toggleTargetOnlyOpenableGift()
    {
        c.targetingOnlyOpenableGift = !c.targetingOnlyOpenableGift;
        sendChat("Toggled target only openable gifts " + c.targetingOnlyOpenableGift);
    }
    
    public void toggleRecipient()
    {
        c.isRecipient = !c.isRecipient;
        sendChat("Toggled recipient " + c.isRecipient);
    }
    
    public void toggleAutoSetTargeting()
    {
        c.autoSetTargeting = !c.autoSetTargeting;
        sendChat("Toggled auto set targeting " + c.autoSetTargeting);
        if (c.giftFeatures && c.autoSetTargeting)
        {
            if (c.isRecipient)
            {
                c.targetingDisableArmorStand = false;
                c.targetingOnlyOpenableGift = true;
                sendChat("Gift features are on, enabled only targeting gift");
            }
            else
            {
                c.targetingDisableArmorStand = true;
                c.targetingOnlyOpenableGift = false;
                sendChat("Gift features are on, disabled armor stand targeting");
            }
        }
    }
    
    public void setAutoReleaseRightClickWhitelist(@NotNull String name)
    {
        if (name.isEmpty())
        {
            sendChat("Printing list");
            int i = 0;
            for (String n : c.autoReleaseRightClickWhitelist)
                sendChat(++i + ": " + n);
            return;
        }
        
        name = name.toLowerCase(Locale.ROOT);
        if (c.autoReleaseRightClickWhitelist.contains(name))
        {
            c.autoReleaseRightClickWhitelist.remove(name);
            sendChat("Removed " + name + " from whitelist");
        }
        else
        {
            c.autoReleaseRightClickWhitelist.add(name);
            sendChat("Added " + name + " to whitelist");
        }
    }
    
    public void toggleWhiteGiftTracking()
    {
        trackWhiteGifts = !trackWhiteGifts;
        sendChat("Toggled white gift tracking " + trackWhiteGifts);
        whiteGifts.clear();
        whiteGiftsTemp.clear();
    }
    
    public void toggleThrowValuable()
    {
        c.throwValuableRewards = !c.throwValuableRewards;
        sendChat("Toggled throwing valuable rewards " + c.throwValuableRewards);
    }
    
    public void toggleRewardDumpBlacklist(String id)
    {
        if (id.isEmpty())
        {
            int i = 1;
            sendChat("Blacklist:");
            for (String id2 : c.giftRewardDumpBlacklist)
                sendChat(i++ + ": " + id2);
        }
        else
        {
            id = id.toUpperCase(Locale.ROOT);
            if (c.giftRewardDumpBlacklist.contains(id))
            {
                c.giftRewardDumpBlacklist.remove(id);
                sendChat("Removed " + id);
            }
            else
            {
                c.giftRewardDumpBlacklist.add(id);
                sendChat("Added " + id);
            }
        }
    }
    
    public void toggleAutoGiftAndRefillFromInv()
    {
        if (c.autoGiftTarget.isEmpty())
        {
            sendChat("Please set the auto gift target player first");
        }
        else
        {
            c.autoGiftAndRefillFromInv = !c.autoGiftAndRefillFromInv;
            sendChat("Toggled auto gifting " + c.autoGiftAndRefillFromInv + ", is recipient: " + c.isRecipient);
            sendChat("Right click on the target with a gift and the keybind will remain pressed -");
            sendChat(" while you have gifts in inv and screen is on game or chat");
            sendChat("If you're a recipient, hovering over gifts from the target player will hold down the keybind for you, for 5 secs");
        }
    }
    
    public void setAutoGiftTarget(@NotNull String player)
    {
        if (player.isEmpty())
        {
            c.autoGiftTarget = "";
            c.autoGiftAndRefillFromInv = false;
            sendChat("Removed auto gift target player");
        }
        else
        {
            c.autoGiftTarget = player.toLowerCase(Locale.ROOT);
            sendChat("Set auto gift target player to " + c.autoGiftTarget);
        }
    }
}
