package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.List;

import static a7.tweakception.utils.McUtils.*;

public class GlobalTracker
{
    private static int ticks = 0;
    private static boolean isInSkyblock = false;
    private static boolean isIslandDetectionOverridden = false;
    private static HashMap<String, SkyblockIsland> subplaceToIslandMap = new HashMap<String, SkyblockIsland>();
    private static SkyblockIsland currentIsland = null;
    private static String currentLocationRaw = "";

    static
    {
        for (SkyblockIsland island : SkyblockIsland.values())
            for (String subPlace : island.places)
                subplaceToIslandMap.put(subPlace, island);
    }

    public void onTick(TickEvent.ClientTickEvent event)
    {
        ticks++;
        if (ticks % 20 == 10)
            detectSkyblock();
    }

    public void printIsland()
    {
        sendChat("GlobalTracker: " + (currentIsland != null ? currentIsland.name : "none"));
    }

    private void detectSkyblock()
    {
        if (isIslandDetectionOverridden)
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
                    currentIsland = null;
                    List<Score> scores = (List<Score>)scoreboard.getSortedScores(sidebarObjective);
                    for (int i = scores.size() - 1; i >= 0; i--)
                    {
                        Score score = scores.get(i);
                        ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
                        String line = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName());
                        if (line.contains("\u23e3")) // Location prefix (some circle symbol)
                        {
                            line = cleanColor(cleanDuplicateColorCodes(line)).replaceAll("[^A-Za-z0-9() ]", "").trim();
                            currentLocationRaw = line;
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

    public static boolean isInSkyblock()
    {
        return isInSkyblock;
    }

    public static String getCurrentLocationRaw()
    {
        return currentLocationRaw;
    }

    public static int getTicks()
    {
        return ticks;
    }

    public static SkyblockIsland getCurrentIsland()
    {
        return currentIsland;
    }

    public void forceSetIsland(String name)
    {
        if (name.equals("") || name.equals("disable") || name.equals("off"))
        {
            isIslandDetectionOverridden = false;
        }
        else
        {
            for (SkyblockIsland island : SkyblockIsland.values())
                if (island.name.toLowerCase().contains(name.toLowerCase()))
                {
                    isIslandDetectionOverridden = true;
                    currentIsland = island;
                    sendChat("GlobalTracker: overridden current island with " + island.name);
                    return;
                }
        }
    }

    public void copyLocation()
    {
        StringSelection s = new StringSelection(currentLocationRaw);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, s);
        sendChat("GlobalTracker: coipied raw current location to clipboard (" + currentLocationRaw + EnumChatFormatting.RESET + ")");
    }
}
