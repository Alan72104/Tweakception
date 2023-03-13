package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import a7.tweakception.utils.MapBuilder;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

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
    private static final HashMap<String, Predicate<ItemStack>> GIFT_SHITS = new HashMap<>();
    private InvFeature invFeature = InvFeature.None;
    private int invFeatureIndex = 0;
    private int invFeatureLastTicks = 0;
    private int invFeatureClickDelay = 0;
    private boolean stashEmptied = false;
    private int lastPickupStashTicks = 0;
    
    private enum InvFeature
    {
        None, DropGiftShit, MoveGift, AutoDropGiftShit, DumpItems
    }
    
    static
    {
        GIFT_SHITS.put("BATTLE_DISC", s -> true);
        GIFT_SHITS.put("WINTER_DISC", s -> true);
        GIFT_SHITS.put("POTION", s -> McUtils.getExtraAttributes(s).getString("potion").endsWith("_xp_boost"));
        Map<String, Integer> crap = MapBuilder.stringIntHashMap()
            .put("scavenger", 4)
            .put("looting", 4)
            .put("luck", 6).getMap();
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
                List<EntityOtherPlayerMP> closePlayers = getWorld().getEntities(EntityOtherPlayerMP.class, p ->
                {
                    return
                        p.getDistanceSqToEntity(getPlayer()) <= c.autoReleaseRightClickDistance * c.autoReleaseRightClickDistance &&
                        !c.autoReleaseRightClickWhitelist.contains(p.getName().toLowerCase()) &&
                        p.getDisplayName().getFormattedText().length() >= 4 &&
                        p.getDisplayName().getFormattedText().charAt(2) == '§';
                });
            
                if (closePlayers.size() > 0)
                {
                    KeyBinding.setKeyBindState(getMc().gameSettings.keyBindUseItem.getKeyCode(),
                        false);
                    sendChat("Gift: released use keybind because " + closePlayers.get(0).getName() + " came");
                }
            }
        }
    }
    
    private boolean doDropGiftShit()
    {
        List<ItemStack> inv = ((GuiInventory) getMc().currentScreen).inventorySlots.getInventory();
        for (; invFeatureIndex <= 44; invFeatureIndex++)
        {
            ItemStack stack = inv.get(invFeatureIndex);
            String id = Utils.getSkyblockItemId(stack);
            if (stack != null && id != null &&
                GIFT_SHITS.containsKey(id) && GIFT_SHITS.get(id).test(stack))
            {
                getMc().playerController.windowClick(0, invFeatureIndex,
                    0, 4, getPlayer());
                invFeatureLastTicks = getTicks();
                invFeatureClickDelay = c.invFeaturesMinDelay + getWorld().rand.nextInt(3);
                invFeatureIndex++;
                return true;
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
        GuiChest chest = (GuiChest) McUtils.getMc().currentScreen;
        ContainerChest container = (ContainerChest) chest.inventorySlots;
        IInventory inv = container.getLowerChestInventory();
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
            if (stack == null || stack.stackSize > 1 && stack.stackSize < 64)
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
                    getMc().playerController.windowClick(container.windowId, 54 + 27 + invFeatureIndex, 0, 1, getPlayer());
                else
                    getMc().playerController.windowClick(container.windowId, 54 + invFeatureIndex - 9, 0, 1, getPlayer());
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
        c.autoReleaseRightClickDistance = Utils.clamp(blocks, 5, 50);
        sendChat("Gift: set the distance of a player needed to release right click to " + c.autoSwitchGiftSlot);
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
        
        name = name.toLowerCase();
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
}
