package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import a7.tweakception.events.IslandChangedEvent;
import a7.tweakception.utils.DumpUtils;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.RenderUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.inventory.Slot;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.List;

import static a7.tweakception.utils.McUtils.*;
import static a7.tweakception.utils.Utils.setClipboard;

public class GlobalTracker extends Tweak
{
    private final GlobalTrackerConfig c;
    public static class GlobalTrackerConfig
    {
        public boolean devMode = false;
        public String rightCtrlCopyType = "nbt";
        public boolean highlightShinyPigs = false;
        public String shinyPigName = "";
    }
    private static final HashMap<String, SkyblockIsland> SUBPLACE_TO_ISLAND_MAP = new HashMap<>();
    private static int ticks = 0;
    private static boolean islandUpdatedThisTick = false;
    private static boolean isInSkyblock = false;
    private static boolean overrideIslandDetection = false;
    private static SkyblockIsland prevIsland = null;
    private static SkyblockIsland currentIsland = null;
    private static String currentLocationRaw = "";
    private static String currentLocationRawCleaned = "";
    private static boolean useFallbackDetection = false;
    public static boolean t = false;

    public GlobalTracker(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.globalTracker;
        for (SkyblockIsland island : SkyblockIsland.values())
            for (String subPlace : island.places)
                SUBPLACE_TO_ISLAND_MAP.put(subPlace, island);
    }

    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            ticks++;
            islandUpdatedThisTick = false;
            if (ticks % 10 == 8)
            {
                detectSkyblock();
                checkIslandChange();
            }
        }
    }

    public void onGuiKeyInputPre(GuiScreenEvent.KeyboardInputEvent.Pre event)
    {
        if (!c.devMode) return;

        if (Keyboard.getEventKey() == Keyboard.KEY_RCONTROL && Keyboard.getEventKeyState())
        {
            GuiScreen screen = event.gui;

            if (screen instanceof GuiContainer)
            {
                GuiContainer container = (GuiContainer)screen;
                Slot currentSlot = container.getSlotUnderMouse();

                if (currentSlot != null && currentSlot.getHasStack())
                {
                    switch (c.rightCtrlCopyType)
                    {
                        case "nbt":
                        {
                            String nbt = currentSlot.getStack().serializeNBT().toString();
                            nbt = DumpUtils.prettifyJson(nbt);
                            setClipboard(nbt);
                            sendChat("GT: copied item nbt to clipboard");
                            break;
                        }
                        case "tooltip":
                        {
                            List<String> tooltip = currentSlot.getStack().getTooltip(getPlayer(), true);
                            String s = String.join(System.lineSeparator(), tooltip);
                            setClipboard(s);
                            sendChat("GT: copied item tooltip to clipboard");
                            break;
                        }
                    }
                }
            }
        }
    }

    public void onLivingSpecialRenderPre(RenderLivingEvent.Specials.Pre event)
    {
        if (c.highlightShinyPigs && getCurrentIsland() == SkyblockIsland.HUB)
        {
            Entity entity = event.entity;
            if (entity instanceof EntityArmorStand &&
                McUtils.cleanColor(entity.getName()).equalsIgnoreCase(c.shinyPigName))
            {
                List<Entity> pig = getWorld().getEntitiesWithinAABB(EntityPig.class,
                        entity.getEntityBoundingBox().expand(0.0, 2.5, 0.0));
                List<EntityArmorStand> armorStands = getWorld().getEntitiesWithinAABB(EntityArmorStand.class,
                        entity.getEntityBoundingBox().expand(0.2, 2.5, 0.2));

                if (pig.size() > 0)
                    RenderUtils.drawDefaultHighlightBoxForEntity(pig.get(0), RenderUtils.DEFAULT_HIGHLIGHT_COLOR, false);

                for (EntityArmorStand armorStand : armorStands)
                {
                    String shinyOrbTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODJjZGUwNjhlOTlhNGY5OGMzMWY4N2I0Y2MwNmJlMTRiMjI5YWNhNGY3MjgxYTQxNmM3ZTJmNTUzMjIzZGI3NCJ9fX0=";
                    String tex = McUtils.getArmorStandHeadTexture(armorStand);
                    if (tex != null && tex.equals(shinyOrbTexture))
                    {
                        RenderUtils.drawDefaultHighlightBoxUnderEntity(entity, -1, RenderUtils.DEFAULT_HIGHLIGHT_COLOR, false);
                        break;
                    }
                }
            }
        }
    }

    public void printIsland()
    {
        sendChat("GT: " + (currentIsland != null ? currentIsland.name : "none"));
    }

    private void detectSkyblock()
    {
        if (overrideIslandDetection)
            return;

        Minecraft mc = getMc();
        isInSkyblock = false;
        currentIsland = null;
        currentLocationRaw = "";

        islandUpdatedThisTick = true;

        if (mc.isSingleplayer())
            return;

        String serverBrand = mc.thePlayer.getClientBrand(); // It's actually getServerBrand()
        if (mc.theWorld == null || mc.thePlayer == null || serverBrand == null ||
            !serverBrand.toLowerCase().contains("hypixel"))
            return;
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
        if (sidebarObjective == null)
            return;
        String objectiveName = sidebarObjective.getDisplayName().replaceAll("§.", "");
        if (!objectiveName.startsWith("SKYBLOCK"))
            return;

        isInSkyblock = true;
        for (Score score : scoreboard.getSortedScores(sidebarObjective))
        {
            ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName());

            // Need special detection for dungeon " ⏣ The Catacombs (F5)"
            // And wtf are these
            //  §7⏣ §bVillage👾
            //  §7⏣ §cDungeon H🌠§cub
            //  §7⏣ §aYour Isla🌠§and
            //  §7⏣ §6Bank🌠
            //  §7⏣ §cJerry's W🌠§corkshop
            //  §7⏣ §cThe Catac👾§combs §7(F7)
//            if (!useFallbackDetection)
            if (false)
            {
                if (line.startsWith(" §7⏣"))
                {
                    currentLocationRaw = line;
                    line = cleanColor(cleanDuplicateColorCodes(line)).replaceAll("[^A-Za-z0-9() \\-']", "").trim();
                    currentLocationRawCleaned = line;
                    currentIsland = SUBPLACE_TO_ISLAND_MAP.get(line);
                    break;
                }
            }
            else
            {
                if (line.contains("⏣"))
                {
                    currentLocationRaw = line;
                    line = cleanColor(cleanDuplicateColorCodes(line)).replaceAll("[^A-Za-z0-9() \\-']", "").trim();
                    currentLocationRawCleaned = line;

                    islandLoop:
                    for (SkyblockIsland island : SkyblockIsland.values())
                        for (String subPlace : island.places)
                            if (line.contains(subPlace))
                            {
                                currentIsland = island;
                                break islandLoop;
                            }
                    break;
                }
            }
        }
    }

    public void checkIslandChange()
    {
        if (currentIsland != prevIsland)
        {
            MinecraftForge.EVENT_BUS.post(new IslandChangedEvent(prevIsland, currentIsland));
            prevIsland = currentIsland;
        }
    }

    public static boolean isInSkyblock()
    {
        return isInSkyblock;
    }

    public static String getCurrentLocationRaw()
    {
        return currentLocationRawCleaned;
    }

    public static int getTicks()
    {
        return ticks;
    }

    public static SkyblockIsland getCurrentIsland()
    {
        return currentIsland;
    }

    public void updateIslandNow()
    {
        if (!islandUpdatedThisTick)
            detectSkyblock();
    }

    public void forceSetIsland(String name)
    {
        if (name == null || name.equals("") || name.equals("disable") || name.equals("off"))
        {
            overrideIslandDetection = false;
            sendChat("GT: toggle island override off");
            islandUpdatedThisTick = false;
        }
        else
        {
            for (SkyblockIsland island : SkyblockIsland.values())
                if (island.name.toLowerCase().contains(name.toLowerCase()))
                {
                    overrideIslandDetection = true;
                    isInSkyblock = true;
                    currentIsland = island;
                    islandUpdatedThisTick = true;
                    sendChat("GT: overridden current island with " + island.name);
                    return;
                }
            sendChat("GT: cannot find island in implemented island list");
        }
    }

    public boolean isInDevMode()
    {
        return c.devMode;
    }

    public void copyLocation()
    {
        setClipboard(currentLocationRaw);
        sendChat("GT: copied raw location line to clipboard (" + currentLocationRaw + "§r)");
    }

    public void toggleFallbackDetection()
    {
        useFallbackDetection = !useFallbackDetection;
        sendChat("GT: toggled location fallback detection " + useFallbackDetection);
    }

    public void toggleDevMode()
    {
        c.devMode = !c.devMode;
        sendChat("GT: toggled dev mode " + c.devMode);
    }

    public void rightCtrlCopySet(String type)
    {
        c.rightCtrlCopyType = type;
        sendChat("GT-RightCtrlCopy: set to " + c.rightCtrlCopyType);
    }

    public void toggleHighlightShinyPigs()
    {
        c.highlightShinyPigs = !c.highlightShinyPigs;
        sendChat("GT-HighlightShinyPigs: toggled" + c.highlightShinyPigs);
    }

    public void setHighlightShinyPigsName(String name)
    {
        c.shinyPigName = name;
        if (name.equals(""))
            sendChat("GT-HighlightShinyPigs: removed name");
        else
            sendChat("GT-HighlightShinyPigs: set name to " + name);
    }
}
