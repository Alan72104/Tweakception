package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import a7.tweakception.utils.DumpUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.List;

import static a7.tweakception.utils.McUtils.*;

public class GlobalTracker extends Tweak
{
    private final GlobalTrackerConfig c;
    public static class GlobalTrackerConfig
    {
        public boolean devMode = false;
    }
    private static final HashMap<String, SkyblockIsland> SUBPLACE_TO_ISLAND_MAP = new HashMap<>();
    private static int ticks = 0;
    private static boolean isInSkyblock = false;
    private static boolean overrideIslandDetection = false;
    private static SkyblockIsland currentIsland = null;
    private static String currentLocationRaw = "";
    private static String currentLocationRawCleaned = "";
    private static boolean useFallbackDetection = false;

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
        ticks++;
        if (ticks % 20 == 10)
            detectSkyblock();
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
                    String nbt = currentSlot.getStack().serializeNBT().toString();
                    nbt = DumpUtils.prettifyJson(nbt);
                    setClipboard(nbt);
                    sendChat("GT: copied item data to clipboard");
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

        if (mc.isSingleplayer())
            return;

        if (mc.theWorld != null && mc.thePlayer != null &&
                mc.thePlayer.getClientBrand().toLowerCase().contains("hypixel")) // It's actually getServerBrand()
        {
            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
            if (sidebarObjective != null)
            {
                String objectiveName = sidebarObjective.getDisplayName().replaceAll("(?i)\\u00A7.", "");
                if (objectiveName.startsWith("SKYBLOCK"))
                {
                    isInSkyblock = true;
                    List<Score> scores = (List<Score>)scoreboard.getSortedScores(sidebarObjective);
                    for (int i = scores.size() - 1; i >= 0; i--)
                    {
                        Score score = scores.get(i);
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
//                        if (!useFallbackDetection)
                        if (false)
                        {
                            if (line.startsWith(" §7⏣"))
                            {
                                currentLocationRaw = line;
                                line = cleanColor(cleanDuplicateColorCodes(line)).replaceAll("[^A-Za-z0-9() ]", "").trim();
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
                                line = cleanColor(cleanDuplicateColorCodes(line)).replaceAll("[^A-Za-z0-9() ]", "").trim();
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
            }
        }
    }

    public static boolean isInSkyblock()
    {
        return isInSkyblock;
    }

    public static String getCurrentLocationRawCleaned()
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

    public boolean isInDevMode()
    {
        return c.devMode;
    }

    public void forceSetIsland(String name)
    {
        if (name == null || name.equals("") || name.equals("disable") || name.equals("off"))
        {
            overrideIslandDetection = false;
            sendChat("GT: toggle island override " + overrideIslandDetection);
        }
        else
        {
            for (SkyblockIsland island : SkyblockIsland.values())
                if (island.name.toLowerCase().contains(name.toLowerCase()))
                {
                    overrideIslandDetection = true;
                    isInSkyblock = true;
                    currentIsland = island;
                    sendChat("GT: overridden current island with " + island.name);
                    return;
                }
            sendChat("GT: cannot find specified island in implemented island list");
        }
    }

    public void copyLocation()
    {
        setClipboard(currentLocationRaw);
        sendChat("GT: coipied raw location line to clipboard (" + currentLocationRaw + EnumChatFormatting.RESET + ")");
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
}
