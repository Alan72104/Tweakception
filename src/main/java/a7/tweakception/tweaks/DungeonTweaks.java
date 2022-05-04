package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.RenderUtils;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Timer;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import scala.Int;

import java.awt.*;
import java.util.*;
import java.util.List;

import static a7.tweakception.tweaks.GlobalTracker.*;
import static a7.tweakception.utils.McUtils.*;

public class DungeonTweaks extends Tweak
{
    private final DungeonTweaksConfig c;
    public static class DungeonTweaksConfig
    {
        public boolean hideNonStarredMobsName = true;
        public boolean highlightStarredMobs = false;
        public boolean highlightBats = true;
        public List<String> blockRightClickItemNames = new ArrayList<>();
        public boolean trackDamageTags = false;
        public int damageTagTrackingCount = 10;
    }
    private static final String F5_BOSS_START = "Welcome, you arrive right on time. I am Livid, the Master of Shadows.";
    private static final String F5_BOSS_END = "Impossible! How did you figure out which one I was?";
    private static final HashMap<String, String> LIVID_PREFIX_TO_COLOR_MAP = new HashMap<>();
    private static boolean hasGetFieldExceptionNotified = false;
    private boolean isInF5Bossfight = false;
    private String realLividName;
    private Entity realLivid;
    private final HashSet<String> knownLivids = new HashSet<>();
    private boolean lividFound = false;
    private final List<Entity> bats = new LinkedList<>();
    private final LinkedList<String> damageTags = new LinkedList<>();
    private final LinkedList<Pair<Integer, Entity>> damageTagsTemp = new LinkedList<>();
    public boolean t = false;

    public DungeonTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.dungeonTweaks;
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
            if (c.trackDamageTags)
            {
                Iterator<Pair<Integer, Entity>> it = damageTagsTemp.iterator();
                while (it.hasNext())
                {
                    Pair<Integer, Entity> p = it.next();
                    int elapsed = getTicks() - p.a;
                    if (elapsed < 5)
                    {
                        break;
                    }
                    else
                    {
                        String s = p.b.getName();
                        if (s.startsWith("§f✧") && s.endsWith("§f✧"))
                        {
                            damageTags.addFirst(s);
                            if (damageTags.size() > c.damageTagTrackingCount)
                                damageTags.removeLast();
                        }
                        it.remove();
                    }
                }
            }
        }
    }

    public void onEntityUpdate(LivingEvent.LivingUpdateEvent event)
    {
        if (getCurrentIsland() != SkyblockIsland.DUNGEON) return;

        EntityLivingBase e = event.entityLiving;
        if (isInF5Bossfight)
        {
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
        if (getCurrentIsland() != SkyblockIsland.DUNGEON) return;

        if (isInF5Bossfight && lividFound)
            RenderUtils.drawHighlightBox(realLivid, AxisAlignedBB.fromBounds(-0.4, 0, -0.4, 0.4, 1.8, 0.4),
                    new Color(0, 255, 0, 192), event.partialTicks, false);
        // Have to do it here because entities don't get rendered if they are too far
        if (c.highlightBats)
        {
            Iterator<Entity> it = bats.iterator();
            while (it.hasNext())
            {
                Entity bat = it.next();
                // There is no EntityLeave event
                if (bat.isDead)
                    it.remove();
                else
                {
                    RenderUtils.drawHighlightBox(bat, AxisAlignedBB.fromBounds(-0.4, -0.6, -0.4, 0.4, 0.6, 0.4),
                            new Color(255, 76, 76, 160), event.partialTicks, false);
                }
            }
        }
    }

    public void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event)
    {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        if (c.trackDamageTags)
        {
            ScaledResolution res = new ScaledResolution(getMc());
            int x = res.getScaledWidth() / 2 - 10;
            int y = res.getScaledHeight() / 2;
            FontRenderer r = getMc().fontRendererObj;
            for (String s : damageTags)
            {
                r.drawString(s, x - r.getStringWidth(s), y, 0xfff0f0f0);
                y -= r.FONT_HEIGHT;
            }
        }
    }

    public void onLivingRenderPost(RenderLivingEvent.Post event)
    {
    }

    // Called on RenderLivingEntity.renderName()
    public void onLivingSpecialRenderPre(RenderLivingEvent.Specials.Pre event)
    {
        if (getCurrentIsland() != SkyblockIsland.DUNGEON) return;

        if (c.hideNonStarredMobsName || c.highlightStarredMobs)
        {
            if (event.entity instanceof EntityArmorStand)
            {
                String name = event.entity.getName();
                if (name.endsWith("§c❤"))
                {
                    if (name.contains("✯"))
                    {
                        if (c.highlightStarredMobs)
                        {
                            try
                            {
                                Timer timer = McUtils.setAccessibleAndGetField(getMc(), "field_71428_T" /* timer */);
                                RenderUtils.drawHighlightBox(event.entity, AxisAlignedBB.fromBounds(-0.4, 0.0, -0.4, 0.4, -2.0, 0.4),
                                        new Color(0, 255, 0, 85), timer.renderPartialTicks, false);
                            }
                            catch (Exception e)
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
                        if (c.hideNonStarredMobsName)
                            event.setCanceled(true);
                    }
                }
            }
        }
    }

    public void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
        if (c.highlightBats && getCurrentIsland() == SkyblockIsland.DUNGEON)
        {
            if (event.entity instanceof EntityBat)
            {
                bats.add(event.entity);
                return;
            }
        }

        if (c.trackDamageTags)
        {
            if (event.entity instanceof EntityArmorStand)
            {
                damageTagsTemp.add(new Pair<>(getTicks(), event.entity));
                return;
            }
        }
    }

    public void onInteract(PlayerInteractEvent event)
    {
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR ||
                event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
        {
            if (!c.blockRightClickItemNames.isEmpty())
            {
                ItemStack item = getPlayer().inventory.getCurrentItem();
                if (item != null)
                {
                    String name = item.getDisplayName();
                    if (c.blockRightClickItemNames.contains(name))
                    {
                        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU))
                        {
                            sendChat("DungeonTweaks => BlockRightClick: overrode block click for item (" + name + EnumChatFormatting.RESET + ")");
                        }
                        else
                        {
                            event.setCanceled(true);
                            sendChat("DungeonTweaks => BlockRightClick: blocked click for item (" + name + EnumChatFormatting.RESET + "), hold alt to override it");
                        }
                    }
                }
            }
        }
    }

    public void onChatReceived(ClientChatReceivedEvent event)
    {
        if (getCurrentIsland() != SkyblockIsland.DUNGEON) return;

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
        if (c.highlightBats)
            bats.clear();
    }

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

    public void toggleHideName()
    {
        c.hideNonStarredMobsName = !c.hideNonStarredMobsName;
        sendChat("DungeonTweaks => HideName: toggled hide name " + c.hideNonStarredMobsName);
    }

    public void toggleHighlightStarredMobs()
    {
        c.highlightStarredMobs = !c.highlightStarredMobs;
        sendChat("DungeonTweaks => HighlightStarredMobs: toggled " + c.highlightStarredMobs);
    }

    public void toggleHighlightBats()
    {
        c.highlightBats = !c.highlightBats;
        sendChat("DungeonTweaks => HighlightBats: toggled " + c.highlightBats);

        if (c.highlightBats)
        {
            bats.addAll(getWorld().getEntities(EntityBat.class, (EntityBat e) -> { return true;}));
        }
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
        if (c.blockRightClickItemNames.contains(name))
        {
            c.blockRightClickItemNames.remove(name);
            sendChat("DungeonTweaks => BlockRightClick: removed item (" + name + EnumChatFormatting.RESET + ") from block list");
        }
        else
        {
            c.blockRightClickItemNames.add(name);
            sendChat("DungeonTweaks => BlockRightClick: added item (" + name + EnumChatFormatting.RESET + ") to block list");
        }
    }

    public void blockRightClickList()
    {
        if (c.blockRightClickItemNames.isEmpty())
        {
            sendChat("DungeonTweaks => BlockRightClick: list is empty");
            return;
        }
        sendChat("DungeonTweaks => BlockRightClick: there are " + c.blockRightClickItemNames.size() + " items in the list");
        for (String s : c.blockRightClickItemNames)
            sendChat("DungeonTweaks => BlockRightClick: " + s);
    }

    public void toggleTrackDamageTags()
    {
        c.trackDamageTags = !c.trackDamageTags;
        sendChat("DungeonTweaks => TrackDamageTags: toggled " + c.trackDamageTags);
        if (c.trackDamageTags)
        {
            damageTags.clear();
            damageTagsTemp.clear();
        }
    }

    public void setDamageTagTrackingCount(int count)
    {
        c.damageTagTrackingCount = count;
        sendChat("DungeonTweaks => TrackDamageTags: set count to " + c.damageTagTrackingCount);
        damageTags.clear();
        damageTagsTemp.clear();
    }
}
