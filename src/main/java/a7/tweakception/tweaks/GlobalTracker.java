package a7.tweakception.tweaks;

import a7.tweakception.Scheduler;
import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.events.IslandChangedEvent;
import a7.tweakception.overlay.Anchor;
import a7.tweakception.overlay.TextOverlay;
import a7.tweakception.utils.DumpUtils;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.RenderUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.event.ClickEvent;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.*;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static a7.tweakception.utils.McUtils.*;
import static a7.tweakception.utils.Utils.f;

public class GlobalTracker extends Tweak
{
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
        public boolean blockQuickCraft = false;
        public TreeSet<String> quickCraftWhitelist = new TreeSet<>();
        public boolean drawSelectedEntityOutline = false;
        public float selectedEntityOutlineWidth = 3.0f;
        public int[] selectedEntityOutlineColor = {0, 0, 255, 128};
        public boolean displayPlayersInArea = false;
    }
    private final GlobalTrackerConfig c;
//    private static final HashMap<String, SkyblockIsland> SUBPLACE_TO_ISLAND_MAP = new HashMap<>();
    private static final List<SkyblockIsland> ISLANDS_THAT_HAS_SUBAREAS = new ArrayList<>();
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
    private static final HashSet<String> npcSkins = new HashSet<>();
    public static boolean t = false;
    private int pendingCopyStartTicks = -1;
    private boolean editingAreas = false;
    private int selectedAreaPointIndex = 0;
    private BlockPos[] areaPoints = null;
    private final HashMap<String, SkyblockIsland.SubArea> playersInAreas = new HashMap<>();
    
    public GlobalTracker(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.globalTracker;
        for (SkyblockIsland island : SkyblockIsland.values())
        {
            if (island.subAreas != null)
                ISLANDS_THAT_HAS_SUBAREAS.add(island);
//            for (String subPlace : island.areas)
//                SUBPLACE_TO_ISLAND_MAP.put(subPlace, island);
        }
        Tweakception.overlayManager.addOverlay(new PlayersInAreasDisplayOverlay());
        npcSkins.add("minecraft:skins/57a517865b820a4451cd3cc6765f370fd0522b6489c9c94fb345fdee2689451a"); // Shaman
        npcSkins.add("minecraft:skins/1642a06cd75ef307c1913ba7a224fb2082d8a2c5254fd1bf006125a087a9a868"); // Taurus
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
        
        if (event.phase == TickEvent.Phase.END && getTicks() % 5 == 4)
        {
            SkyblockIsland island = getCurrentIsland();
            if (c.displayPlayersInArea)
            {
                playersInAreas.clear();
                if (ISLANDS_THAT_HAS_SUBAREAS.contains(island))
                    for (SkyblockIsland.SubArea area : island.subAreas)
                    {
                        List<Entity> players = getWorld().getEntitiesWithinAABB(EntityOtherPlayerMP.class,
                            area.box,
                            e ->
                            {
                                String skin = ((AbstractClientPlayer)e).getLocationSkin().toString();
                                return !npcSkins.contains(skin) && !e.isInvisible(); // Watchdog player is invisible
                            });
                        for (Entity p : players)
                            playersInAreas.put(p.getName(), area);
                    }
                Tweakception.overlayManager.setEnable(PlayersInAreasDisplayOverlay.NAME, !playersInAreas.isEmpty());
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
    
    public void onRenderLast(RenderWorldLastEvent event)
    {
        if (editingAreas)
        {
            float t = event.partialTicks;
            int minX = Math.min(areaPoints[0].getX(), areaPoints[1].getX());
            int minY = Math.min(areaPoints[0].getY(), areaPoints[1].getY());
            int minZ = Math.min(areaPoints[0].getZ(), areaPoints[1].getZ());
            int maxX = Math.max(areaPoints[0].getX(), areaPoints[1].getX()) + 1;
            int maxY = Math.max(areaPoints[0].getY(), areaPoints[1].getY()) + 1;
            int maxZ = Math.max(areaPoints[0].getZ(), areaPoints[1].getZ()) + 1;
            AxisAlignedBB bb = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
            RenderUtils.drawFilledBoundingBox(bb, new Color(0, 255, 0, 32), t, true);
            RenderUtils.drawFilledBoundingBox(areaPoints[selectedAreaPointIndex], new Color(255, 0, 0, 64), t);
            RenderUtils.drawFilledBoundingBox(areaPoints[1 - selectedAreaPointIndex], new Color(0, 255, 0, 64), t);
        }
    }
    
    public void onRenderBlockOverlay(DrawBlockHighlightEvent event)
    {
        if (c.drawSelectedEntityOutline && event.target.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY)
        {
            RenderUtils.drawOutlineForEntity(event.target.entityHit,
                Utils.colorArrayToColor(c.selectedEntityOutlineColor),
                event.partialTicks, false, c.selectedEntityOutlineWidth);
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
    
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        if (editingAreas &&
            !Keyboard.isRepeatEvent() && Keyboard.getEventKeyState())
        {
            switch (Keyboard.getEventKey())
            {
                case Keyboard.KEY_UP:
                    Tweakception.globalTracker.extendAreaPoint();
                    break;
                case Keyboard.KEY_DOWN:
                    Tweakception.globalTracker.retractAreaPoint();
                    break;
                case Keyboard.KEY_LEFT:
                case Keyboard.KEY_RIGHT:
                    Tweakception.globalTracker.switchAreaPoints();
                    break;
            }
        }
    }
    
    // region Misc
    
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
//            if (false)
//            {
//                if (line.startsWith(" §7⏣"))
//                {
//                    currentLocationRaw = line;
//                    line = cleanColor(cleanDuplicateColorCodes(line)).replaceAll("[^A-Za-z0-9() \\-']", "").trim();
//                    currentLocationRawCleaned = line;
//                    currentIsland = SUBPLACE_TO_ISLAND_MAP.get(line);
//                    break;
//                }
//            }
//            else
//            {
                if (line.contains("⏣"))
                {
                    currentLocationRaw = line;
                    line = cleanColor(cleanDuplicateColorCodes(line)).replaceAll("[^A-Za-z0-9() \\-']", "").trim();
                    currentLocationRawCleaned = line;

                    islandLoop:
                    for (SkyblockIsland island : SkyblockIsland.values())
                        for (String subPlace : island.areas)
                            if (line.contains(subPlace))
                            {
                                currentIsland = island;
                                break islandLoop;
                            }
                    break;
                }
//            }
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
    
    public boolean isBlockingQuickCraft()
    {
        return c.blockQuickCraft;
    }
    
    public boolean isIdInQuickCraftWhitelist(String id)
    {
        return c.quickCraftWhitelist.contains(id);
    }
    
    public void toggleQuickCraftWhitelist(String id)
    {
        if (c.quickCraftWhitelist.contains(id))
            c.quickCraftWhitelist.remove(id);
        else
            c.quickCraftWhitelist.add(id);
        McUtils.playCoolDing();
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
    
    public boolean isInAreaEditMode()
    {
        return editingAreas;
    }
    
    public void switchAreaPoints()
    {
        selectedAreaPointIndex = selectedAreaPointIndex == 0 ? 1 : 0;
    }
    
    public void extendAreaPoint()
    {
        int i = selectedAreaPointIndex;
        if (getPlayer().rotationPitch > 45.0f)
            areaPoints[i] = areaPoints[i].down();
        else if (getPlayer().rotationPitch < -45.0f)
            areaPoints[i] = areaPoints[i].up();
        else
        {
            EnumFacing facing = getPlayer().getHorizontalFacing();
            areaPoints[i] = areaPoints[i].offset(facing);
        }
    }
    
    public void retractAreaPoint()
    {
        int i = selectedAreaPointIndex;
        if (getPlayer().rotationPitch > 45.0f)
            areaPoints[i] = areaPoints[i].up();
        else if (getPlayer().rotationPitch < -45.0f)
            areaPoints[i] = areaPoints[i].down();
        else
        {
            EnumFacing facing = getPlayer().getHorizontalFacing();
            areaPoints[i] = areaPoints[i].offset(facing.getOpposite());
        }
    }
    
    private class PlayersInAreasDisplayOverlay extends TextOverlay
    {
        public static final String NAME = "PlayersInAreasDisplayOverlay";
        
        public PlayersInAreasDisplayOverlay()
        {
            super(NAME);
            setAnchor(Anchor.CenterRight);
            setOrigin(Anchor.CenterRight);
            setX(-100);
            setTextAlignment(1);
        }
        
        @Override
        public void update()
        {
            super.update();
            List<String> list = new ArrayList<>();
            for (Map.Entry<String, SkyblockIsland.SubArea> entry : playersInAreas.entrySet())
            {
                list.add(entry.getKey() + "-" + entry.getValue().shortName);
            }
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("player: area name");
            list.add("player2: area name");
            return list;
        }
    }
    
    // endregion
    
    // region Commands
    
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
    
    public void toggleBlockQuickCraft()
    {
        c.blockQuickCraft = !c.blockQuickCraft;
        sendChat("GT-BlockQuickCraft: toggled " + c.blockQuickCraft);
    }
    
    public void removeBlockQuickCraftWhitelist(int i)
    {
        if (i < 1)
        {
            sendChat("GT-BlockQuickCraft: there are " + c.quickCraftWhitelist.size() + " whitelisted IDs");
            int ii = 1;
            for (String id : c.quickCraftWhitelist)
                sendChat(ii++ + ": " + id);
        }
        else
        {
            if (i > c.quickCraftWhitelist.size())
                sendChat("GT-BlockQuickCraft: index is out of bounds!");
            else
            {
                String id = c.quickCraftWhitelist.toArray(new String[0])[i - 1];
                c.quickCraftWhitelist.remove(id);
                sendChat("GT-BlockQuickCraft: removed " + id);
            }
        }
    }
    
    public void getPlayerCountInArea(int type)
    {
        String areaName;
        List<Entity> entities;
        
        switch (type)
        {
            default:
            case 0:
                areaName = "park";
                String shamanSkin = "minecraft:skins/57a517865b820a4451cd3cc6765f370fd0522b6489c9c94fb345fdee2689451a";
                entities = getWorld().getEntitiesWithinAABB(EntityOtherPlayerMP.class,
                    new AxisAlignedBB(-351, 78, -102, -399, 49, 36),
                    e ->
                    {
                        String skin = ((AbstractClientPlayer)e).getLocationSkin().toString();
                        return !skin.equals(shamanSkin) && !e.isInvisible(); // Watchdog player is invisible
                    });
                break;
            case 1:
                areaName = "crimson stronghold back top right";
                entities = getWorld().getEntitiesWithinAABB(EntityOtherPlayerMP.class,
                    new AxisAlignedBB(-370, 170, -485, -338, 132, -462),
                    e -> !e.isInvisible());
                break;
            case 2:
                areaName = "crimson stronghold front top right";
                entities = getWorld().getEntitiesWithinAABB(EntityOtherPlayerMP.class,
                    new AxisAlignedBB(-314, 135, -539, -267, 145, -580),
                    e -> !e.isInvisible());
                break;
        }
        
        sendChat("ST: there are " + entities.size() + " players in the " + areaName + " area");
        for (int i = 0; i < entities.size(); i++)
            sendChat((i + 1) + ": " + entities.get(i).getDisplayName());
    }
    
    public void toggleAreaEdit()
    {
        editingAreas = !editingAreas;
        if (editingAreas)
        {
            areaPoints = new BlockPos[2];
            areaPoints[0] = getPlayer().getPosition();
            areaPoints[1] = getPlayer().getPosition().add(1, 1, 1);
            selectedAreaPointIndex = 0;
        }
        else
        {
            sendChatf("GT-AreaEdit: last area: (%d,%d,%d),(%d,%d,%d)",
                areaPoints[0].getX(), areaPoints[0].getY(), areaPoints[0].getZ(),
                areaPoints[1].getX(), areaPoints[1].getY(), areaPoints[1].getZ());
            areaPoints = null;
        }
        sendChat("GT-AreaEdit: toggled " + editingAreas);
    }
    
    public void resetArea()
    {
        if (editingAreas)
        {
            sendChatf("GT-AreaEdit: reset area, last: (%d,%d,%d),(%d,%d,%d)",
                areaPoints[0].getX(), areaPoints[0].getY(), areaPoints[0].getZ(),
                areaPoints[1].getX(), areaPoints[1].getY(), areaPoints[1].getZ());
            areaPoints = new BlockPos[2];
            areaPoints[0] = getPlayer().getPosition();
            areaPoints[1] = getPlayer().getPosition().add(1, 1, 1);
        }
        else
            sendChat("GT-AreaEdit: feature is off");
    }
    
    public void setAreaPoint(int i, int x, int y, int z)
    {
        if (editingAreas)
        {
            if (i == 0)
            {
                areaPoints[0] = new BlockPos(x, y, z);
                sendChatf("GT-AreaEdit: set point 1 to %d, %d, %d", x, y, z);
            }
            else
            {
                areaPoints[1] = new BlockPos(x, y, z);
                sendChatf("GT-AreaEdit: set point 2 to %d, %d, %d", x, y, z);
            }
        }
        else
            sendChat("GT-AreaEdit: feature is off");
    }
    
    public void printArea()
    {
        if (editingAreas)
        {
            sendChatf("GT-AreaEdit: current area: (%d,%d,%d),(%d,%d,%d)",
                areaPoints[0].getX(), areaPoints[0].getY(), areaPoints[0].getZ(),
                areaPoints[1].getX(), areaPoints[1].getY(), areaPoints[1].getZ());
            sendChat("GT-AreaEdit: copied to clipboard");
            Utils.setClipboard(f("%d, %d, %d, %d, %d, %d",
                areaPoints[0].getX(), areaPoints[0].getY(), areaPoints[0].getZ(),
                areaPoints[1].getX(), areaPoints[1].getY(), areaPoints[1].getZ()));
        }
        else
            sendChat("GT-AreaEdit: feature is off");
    }
    
    public void toggleDrawSelectedEntityOutline()
    {
        c.drawSelectedEntityOutline = !c.drawSelectedEntityOutline;
        sendChat("GT-DrawSelectedEntityOutline: toggled " + c.drawSelectedEntityOutline);
    }
    
    public void setSelectedEntityOutlineWidth(float w)
    {
        c.selectedEntityOutlineWidth = w > 0.0f ? w : new GlobalTrackerConfig().selectedEntityOutlineWidth;
        sendChat("GT-DrawSelectedEntityOutline: set width to " + c.selectedEntityOutlineWidth);
    }
    
    public void setSelectedEntityOutlineColor(int r, int g, int b, int a)
    {
        c.selectedEntityOutlineColor = r < 0 ? new GlobalTrackerConfig().selectedEntityOutlineColor
            : Utils.makeColorArray(r, g, b, a);
        sendChat("GT-DrawSelectedEntityOutline: set color to " + Arrays.toString(c.selectedEntityOutlineColor));
    }
    
    public void togglePlayersInAreasDisplay()
    {
        c.displayPlayersInArea = !c.displayPlayersInArea;
        sendChat("GT-PlayersInAreasDisplay: toggled " + c.displayPlayersInArea);
        if (c.displayPlayersInArea)
            Tweakception.overlayManager.enable(PlayersInAreasDisplayOverlay.NAME);
        else
        {
            playersInAreas.clear();
            Tweakception.overlayManager.disable(PlayersInAreasDisplayOverlay.NAME);
        }
    }
    
    // endregion
}
