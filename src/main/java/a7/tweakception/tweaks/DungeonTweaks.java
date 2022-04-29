package a7.tweakception.tweaks;

import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Timer;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

import static a7.tweakception.tweaks.GlobalTracker.*;
import static a7.tweakception.tweaks.GlobalTracker.getCurrentLocationRaw;
import static a7.tweakception.utils.McUtils.*;

public class DungeonTweaks
{
    private static final String F5_BOSS_START = "Welcome, you arrive right on time. I am Livid, the Master of Shadows.";
    private static final String F5_BOSS_END = "Impossible! How did you figure out which one I was?";
    private static final HashMap<String, String> LIVID_PREFIX_TO_COLOR_MAP = new HashMap<String, String>();
    private boolean isNoFogOn = false;
    private boolean isNoFogAutoToggleOn = true;
    private boolean wasNoFogAutoToggled = false;
    private boolean isInF5Bossfight = false;
    private String realLividName;
    private Entity realLivid;
    private final HashSet<String> knownLivids = new HashSet<String>();
    private boolean lividFound = false;
    private boolean hideName = true;
    private boolean highlightStarredMobs = false;
    private final ArrayList<String> blockRightClickItems = new ArrayList<String>();
    public boolean t = false;

    static
    {
        LIVID_PREFIX_TO_COLOR_MAP.put("Vendetta", "§f");
        LIVID_PREFIX_TO_COLOR_MAP.put("Crossed", "§d");
        LIVID_PREFIX_TO_COLOR_MAP.put("Hockey", "§c");
        LIVID_PREFIX_TO_COLOR_MAP.put("Doctor", "§7");
        LIVID_PREFIX_TO_COLOR_MAP.put("Frog", "§2");
        LIVID_PREFIX_TO_COLOR_MAP.put("Smile", "§a");
        LIVID_PREFIX_TO_COLOR_MAP.put("Scream", "§1");
        LIVID_PREFIX_TO_COLOR_MAP.put("Purple", "§5");
        LIVID_PREFIX_TO_COLOR_MAP.put("Arcade", "§e");
    }

    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            if (getTicks() % 20 == 0)
            {
                if (isNoFogAutoToggleOn)
                {
                    if (getCurrentIsland() == SkyblockIsland.DUNGEON &&
                            (getCurrentLocationRaw().contains("(F5)") || getCurrentLocationRaw().contains("(M5)")))
                    {
                        if (!isNoFogOn)
                        {
                            isNoFogOn = true;
                            wasNoFogAutoToggled = true;
                            sendChat("DungeonTweaks => NoFog: dungeon floor 5 detected, auto toggled on");
                        }
                        else
                            wasNoFogAutoToggled = false;
                    }
                    else
                    {
                        if (isNoFogOn && wasNoFogAutoToggled)
                        {
                            isNoFogOn = false;
                            wasNoFogAutoToggled = false;
                            sendChat("DungeonTweaks => NoFog: auto toggled off");
                        }
                    }
                }
            }
        }
    }

    public void onEntityUpdate(LivingEvent.LivingUpdateEvent event)
    {
        if (isInF5Bossfight)
        {
            EntityLivingBase e = event.entityLiving;
            if (e instanceof EntityOtherPlayerMP)
            {
                String n = e.getName();
                if (n.endsWith("Livid"))
                {
                    if (!knownLivids.contains(n))
                    {
                        knownLivids.add(n);
                        realLividName = n;
                        realLivid = e;
                        lividFound = true;
                        // prefix = LIVID_PREFIX_TO_COLOR_MAP.get(realLividName.split(" ")[0]);
                    }
                    else if (n.equalsIgnoreCase(realLividName))
                    {
                        realLivid = e;
                        lividFound = true;
                    }
                }
            }
        }
    }

    public void onRenderLast(RenderWorldLastEvent event)
    {
        if (isInF5Bossfight && lividFound)
            RenderUtils.drawHighlightBox(realLivid, AxisAlignedBB.fromBounds(-0.4, 0, -0.4, 0.4, 1.8, 0.4),
                    new Color(0, 255, 0, 192), event.partialTicks, false);
    }

    private static final Pattern nums = Pattern.compile("\\d+");
    private static boolean hasGetFieldExceptionNotified = false;

    // Called on RenderLivingEntity.renderName()
    public void onLivingSpecialRenderPre(RenderLivingEvent.Specials.Pre event)
    {
        if (getCurrentIsland() != SkyblockIsland.DUNGEON) return;

        if (hideName || highlightStarredMobs)
        {
            if (event.entity instanceof EntityArmorStand)
            {
                String name = event.entity.getName();
                if (name.endsWith("§c❤"))
                {
                    if (name.contains("✯"))
                    {
                        if (highlightStarredMobs)
                        {
                            try
                            {
                                Timer timer = McUtils.setAccessibleAndGetField(getMc(), "field_71428_T" /* timer */);
                                RenderUtils.drawHighlightBox(event.entity, AxisAlignedBB.fromBounds(-0.4, 0.0, -0.4, 0.4, -2.0, 0.4),
                                        new Color(0, 255, 0, 85), timer.renderPartialTicks, false);
                            }
                            catch (NoSuchFieldException | IllegalAccessException e)
                            {
                                if (!hasGetFieldExceptionNotified)
                                {
                                    hasGetFieldExceptionNotified = true;
                                    sendChat("DungeonTweaks => HighlightStarredMobs: getField failed (" + e + ")");
                                }
                            }
                        }
                    }
                    else
                    {
                        if (hideName)
                            event.setCanceled(true);
                    }
                }
            }
        }
    }

    public void onInteract(PlayerInteractEvent event)
    {
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR ||
                event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
        {
            if (!blockRightClickItems.isEmpty())
            {
                ItemStack item = getPlayer().inventory.getCurrentItem();
                if (item != null)
                {
                    String name = item.getDisplayName();
                    if (blockRightClickItems.contains(name))
                    {
                        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU))
                        {
                            sendChat("DungeonTweaks => BlockRightClick: overrode block click for item (" + name + ")");
                        }
                        else
                        {
                            event.setCanceled(true);
                            sendChat("DungeonTweaks => BlockRightClick: blocked click for item (" + name + "), hold alt to override it");
                        }
                    }
                }
            }
        }
    }

    public void onChatReceived(ClientChatReceivedEvent event)
    {
        if (event.type == 0)
        {
            String msg = event.message.getUnformattedText();
            if (msg.startsWith("[BOSS]"))
            {
                if (msg.contains(F5_BOSS_START))
                {
                    isInF5Bossfight = true;
                }
                else if (msg.contains(F5_BOSS_END))
                {
                    isInF5Bossfight = false;
                    resetLivid();
                }
            }
        }
    }

    public void onWorldUnload(WorldEvent.Unload event)
    {
        if (isInF5Bossfight)
        {
            resetLivid();
            isInF5Bossfight = false;
        }
    }

    public void onFogDensitySet(EntityViewRenderEvent.FogDensity event)
    {
        if (isNoFogOn)
        {
            event.density = 0.0f;
            event.setCanceled(true);
        }
    }

    // Anti minecraft mods feature - memory leak
    private void resetLivid()
    {
        if (lividFound)
        {
            lividFound = false;
            realLividName = "";
            realLivid = null;
            knownLivids.clear();
        }
    }

    public void toggleNoFog()
    {
        isNoFogOn = !isNoFogOn;
        sendChat("DungeonTweaks => NoFog: toggled " + isNoFogOn);
    }

    public void toggleNoFogAutoToggle()
    {
        isNoFogAutoToggleOn = !isNoFogAutoToggleOn;
        sendChat("DungeonTweaks => NoFog: toggled auto toggle " + isNoFogAutoToggleOn);
    }

    public void toggleHideName()
    {
        hideName = !hideName;
        sendChat("DungeonTweaks => HideName: toggled hide name " + hideName);
    }

    public void toggleHighlightStarredMobs()
    {
        highlightStarredMobs = !highlightStarredMobs;
        sendChat("DungeonTweaks => HighlightStarredMobs: toggled " + highlightStarredMobs);
    }

    public void blockRightClickSet()
    {
        ItemStack item = getPlayer().inventory.getCurrentItem();
        if (item == null)
        {
            sendChat("DungeonTweaks => BlockRightClick: current selected item is empty");
            return;
        }
        String name = item.getDisplayName();
        if (blockRightClickItems.contains(name))
        {
            blockRightClickItems.remove(name);
            sendChat("DungeonTweaks => BlockRightClick: removed item (" + name + ") from block list");
        }
        else
        {
            blockRightClickItems.add(name);
            sendChat("DungeonTweaks => BlockRightClick: added item (" + name + ") to block list");
        }
    }

    public void blockRightClickList()
    {
        if (blockRightClickItems.isEmpty())
        {
            sendChat("DungeonTweaks => BlockRightClick: list is empty");
            return;
        }
        sendChat("DungeonTweaks => BlockRightClick: there are " + blockRightClickItems.size() + " items in the list");
        for (String s : blockRightClickItems)
            sendChat("DungeonTweaks => BlockRightClick: " + s);
    }
}
