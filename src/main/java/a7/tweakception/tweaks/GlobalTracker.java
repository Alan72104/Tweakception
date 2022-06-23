package a7.tweakception.tweaks;

import a7.tweakception.Scheduler;
import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.events.IslandChangedEvent;
import a7.tweakception.utils.DumpUtils;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.RenderUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.event.ClickEvent;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static a7.tweakception.utils.McUtils.*;

public class GlobalTracker extends Tweak
{
    private final GlobalTrackerConfig c;
    public static class GlobalTrackerConfig
    {
        public boolean devMode = false;
        public String rightCtrlCopyType = "nbt";
        public boolean highlightShinyPigs = false;
        public String shinyPigName = "";
        public boolean hidePlayers = false;
        public boolean enterToCloseNumberTypingSign = false;
        public boolean renderInvisibleEntities = false;
        public boolean renderInvisibleArmorStands = false;
        public int invisibleEntityAlphaPercentage = 15;
        public boolean skipWorldRendering = false;
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
    private static final Map<String, Runnable> chatActionMap = new HashMap<>();
    public static boolean t = false;
    private int pendingCopyStartTicks = -1;

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

            if (pendingCopyStartTicks != -1 && getTicks() - pendingCopyStartTicks >= 10)
            {
                pendingCopyStartTicks = -1;
                doCopy(false);
            }
        }
    }

    public void onGuiKeyInputPre(GuiScreenEvent.KeyboardInputEvent.Pre event)
    {
        if (!c.devMode) return;

        if (Keyboard.getEventKey() == Keyboard.KEY_RCONTROL && Keyboard.getEventKeyState() && !Keyboard.isRepeatEvent())
        {
            GuiScreen screen = event.gui;

            if (screen instanceof GuiContainer)
            {
                GuiContainer container = (GuiContainer)screen;
                Slot currentSlot = container.getSlotUnderMouse();

                if (currentSlot != null && currentSlot.getHasStack())
                {
                    if (pendingCopyStartTicks != -1)
                    {
                        pendingCopyStartTicks = -1;
                        doCopy(true);
                    }
                    else
                        pendingCopyStartTicks = getTicks();
                }
            }
        }
    }

    public void onLivingRenderPre(RenderLivingEvent.Pre event)
    {
        if (c.hidePlayers)
        {
            if (event.entity instanceof EntityOtherPlayerMP)
            {
                // Check if it's a real online player
                NetworkPlayerInfo info = getMc().getNetHandler().getPlayerInfo(event.entity.getUniqueID());
                if (info != null)
                    event.setCanceled(true);
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

    private void checkIslandChange()
    {
        if (currentIsland != prevIsland)
        {
            MinecraftForge.EVENT_BUS.post(new IslandChangedEvent(prevIsland, currentIsland));
            prevIsland = currentIsland;
        }
    }

    private void doCopy(boolean copyToFile)
    {
        GuiScreen screen = getMc().currentScreen;

        if (screen instanceof GuiContainer)
        {
            GuiContainer container = (GuiContainer)screen;
            Slot currentSlot = container.getSlotUnderMouse();

            if (currentSlot != null && currentSlot.getHasStack())
            {
                ItemStack stack = currentSlot.getStack();
                String string;
                String type;
                switch (c.rightCtrlCopyType)
                {
                    default:
                    case "nbt":
                        String nbt = stack.serializeNBT().toString();
                        string = DumpUtils.prettifyJson(nbt);
                        type = "nbt";
                        break;
                    case "tooltip":
                        List<String> tooltip = stack.getTooltip(getPlayer(), true);
                        string = String.join(System.lineSeparator(), tooltip);
                        type = "tooltip";
                        break;
                }

                if (copyToFile)
                {
                    String itemName = McUtils.cleanColor(stack.getDisplayName());
                    File file = null;
                    try
                    {
                        file = Tweakception.configuration.createWriteFileWithCurrentDateTime(
                                type + "_$_" + itemName.substring(0, Math.min(itemName.length(), 20)) + ".txt",
                                new ArrayList<>(Collections.singleton(string)));

                        IChatComponent fileName = new ChatComponentText(file.getName());
                        fileName.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));
                        fileName.getChatStyle().setUnderlined(true);

                        getPlayer().addChatMessage(new ChatComponentTranslation(
                                "GT: written item %s to file %s", type, fileName));
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        sendChat("GT: exception occurred when creating file");
                    }
                    if (file != null)
                        try
                        {
                            Desktop.getDesktop().open(file);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            sendChat("GT: exception occurred when opening file");
                        }
                }
                else
                {
                    Utils.setClipboard(string);
                    sendChat("GT: copied item " + type + " to clipboard");
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

    public boolean isEnterToCloseNumberTypingSignOn()
    {
        return c.enterToCloseNumberTypingSign;
    }

    public boolean isRenderInvisibleEntitiesOn()
    {
        return c.renderInvisibleEntities;
    }

    public boolean isRenderInvisibleArmorStandsOn()
    {
        return c.renderInvisibleArmorStands;
    }

    public float getInvisibleEntityAlpha()
    {
        return c.invisibleEntityAlphaPercentage / 100.0f;
    }

    public boolean isSkipWorldRenderingOn()
    {
        return c.skipWorldRendering;
    }

    public String registerChatAction(Runnable action, int timeoutTicks, Runnable timeoutAction)
    {
        String uuid = UUID.randomUUID().toString();

        Scheduler.ScheduledTask deletionTask = Tweakception.scheduler.addDelayed(() ->
        {
            chatActionMap.remove(uuid);
            if (timeoutAction != null)
                timeoutAction.run();
        }, Math.max(timeoutTicks, 20));

        chatActionMap.put(uuid, () ->
        {
            action.run();
            Tweakception.scheduler.remove(deletionTask);
        });

        return uuid;
    }

    public void doChatAction(String uuid)
    {
        if (chatActionMap.containsKey(uuid))
        {
            chatActionMap.get(uuid).run();
        }
    }

    public void copyLocation()
    {
        Utils.setClipboard(currentLocationRaw);
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
        sendChat("GT-HighlightShinyPigs: toggled " + c.highlightShinyPigs);
    }

    public void setHighlightShinyPigsName(String name)
    {
        c.shinyPigName = name;
        if (name.equals(""))
            sendChat("GT-HighlightShinyPigs: removed name");
        else
            sendChat("GT-HighlightShinyPigs: set name to " + name);
    }

    public void toggleHidePlayers()
    {
        c.hidePlayers = !c.hidePlayers;
        sendChat("GT-HidePlayers: toggled " + c.hidePlayers);
    }

    public void toggleEnterToCloseNumberTypingSign()
    {
        c.enterToCloseNumberTypingSign = !c.enterToCloseNumberTypingSign;
        sendChat("GT-EnterToCloseNumberTypingSign: toggled " + c.enterToCloseNumberTypingSign);
    }

    public void toggleRenderInvisibleEntities()
    {
        c.renderInvisibleEntities = !c.renderInvisibleEntities;
        sendChat("GT-RenderInvisibleEntities: toggled " + c.renderInvisibleEntities);
    }

    public void toggleRenderInvisibleArmorStands()
    {
        c.renderInvisibleArmorStands = !c.renderInvisibleArmorStands;
        sendChat("GT-RenderInvisibleArmorStands: toggled " + c.renderInvisibleArmorStands);
    }

    public void setInvisibleEntityAlphaPercentage(int p)
    {
        if (p <= 0 || p > 100)
            c.invisibleEntityAlphaPercentage = 15;
        else
            c.invisibleEntityAlphaPercentage = p;
        sendChat("GT-RenderInvisibleEntities: set alpha percentage to " + c.invisibleEntityAlphaPercentage);
    }

    public void toggleSkipWorldRendering()
    {
        c.skipWorldRendering = !c.skipWorldRendering;
        sendChat("GT-SkipWorldRendering: toggled " + c.skipWorldRendering);
    }
}
