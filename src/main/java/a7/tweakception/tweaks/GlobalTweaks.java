package a7.tweakception.tweaks;

import a7.tweakception.Scheduler;
import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.events.IslandChangedEvent;
import a7.tweakception.events.PacketReceiveEvent;
import a7.tweakception.events.PacketSendEvent;
import a7.tweakception.mixin.AccessorGuiPlayerTabOverlay;
import a7.tweakception.mixin.AccessorMinecraft;
import a7.tweakception.overlay.Anchor;
import a7.tweakception.overlay.TextOverlay;
import a7.tweakception.utils.*;
import com.google.common.collect.Ordering;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.*;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static a7.tweakception.utils.McUtils.*;
import static a7.tweakception.utils.Utils.f;

public class GlobalTweaks extends Tweak
{
    public static class GlobalTweaksConfig
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
        public boolean enablePingOverlay = false;
        public boolean enableChampionOverlay = false;
        public int championExpIncrementResetDuration = 30;
        public boolean disableTooltips = false;
        public boolean renderEnchantedBooksType = false;
        public boolean renderSacksType = false;
        public boolean renderPotionTier = false;
        public boolean tooltipDisplaySkyblockItemId = false;
        public TreeSet<String> minionAutoClaimWhitelist = new TreeSet<>();
        public int minionAutoclaimDelayTicksMin = 3;
        public boolean enableOnlineStatusOverlay = false;
        public String lastOnlineStatus = "online";
        public boolean showOnlineStatusAlreadyOn = false;
        public boolean trevorQuestAutoAccept = false;
        public boolean trevorQuestAutoStart = false;
        public boolean trevorHighlightAnimal = false;
        public boolean sendBitsMessage = false;
        public boolean targetingDisableArmorStand = false;
        public boolean targetingDisableBat = false;
        public boolean targetingDisableDeadMob = false;
        public boolean targetingDisablePlayer = false;
        public boolean afkMode = false;
        public boolean afkOnlyUnfocused = true;
        public boolean afkSkipWorldRendering = true;
        public int afkFpsLimit = 60;
        public boolean sendSkyblockLevelExpGainMessage = false;
        public int snipeWarpDelayTicks = 40;
        public TreeSet<String> strangerWhitelist = new TreeSet<>();
        public boolean ranchersBootsTooltipSpeedNote = false;
        public boolean displayPersonalCompactorItems = true;
        public boolean displayPersonalDeletorItems = true;
        public boolean chatLogForceFormatted = false;
        public boolean autoGuildWelcome = false;
    }
    
    private final GlobalTweaksConfig c;
    //private static final HashMap<String, SkyblockIsland> SUBPLACE_TO_ISLAND_MAP = new HashMap<>();
    private static final List<SkyblockIsland> ISLANDS_THAT_HAS_SUBAREAS = new ArrayList<>();
    // Below class loaded in tweakception
    private static final Ordering<NetworkPlayerInfo> TAB_LIST_ORDERING = AccessorGuiPlayerTabOverlay.getTabListOrdering();
    private static int ticks = 0;
    private static boolean islandUpdatedThisTick = false;
    private static boolean playerListUpdatedThisTick = false;
    private static boolean isInSkyblock = false;
    private static boolean isInHypixel = false;
    private static boolean overrideIslandDetection = false;
    private static SkyblockIsland prevIsland = null;
    private static SkyblockIsland currentIsland = null;
    private static String currentLocationRaw = "";
    private static String currentLocationRawCleaned = "";
    private static boolean useFallbackDetection = false;
    private static final Map<String, Runnable> chatActionMap = new HashMap<>();
    private static final HashSet<String> npcSkins = new HashSet<>();
    private static final PacketLogger packetLogger = new PacketLogger();
    private static final List<String> playerList = new ArrayList<>(40);
    public static boolean t = false;
    private int pendingCopyStartTicks = -1;
    private boolean editingAreas = false;
    private int selectedAreaPointIndex = 0;
    private BlockPos[] areaPoints = null;
    private final HashMap<String, SkyblockIsland.SubArea> playersInAreas = new HashMap<>();
    private long pingNanos = 0;
    private int ping = 0;
    private boolean pingingFromCommand = false;
    private long lastWorldJoin = 0;
    private int lastWorldJoinTicks = 0;
    private List<String> lastTooltip = null;
    private int tooltipUpdateTicks = 0;
    private int minionAutoClaimLastClickTicks = 0;
    private int minionAutoClaimClickDelay = 0;
    private boolean highlightPlayers = false;
    private final Set<String> playersToHighlight = new HashSet<>();
    private final Set<String> armorStandsToHighlight = new HashSet<>();
    private final Matcher trevorAnimalNametagMatcher = Pattern.compile(
        "\\[Lv[0-9]+] (?<rarity>[a-zA-Z]+) (?<animal>[a-zA-Z]+) .*❤").matcher("");
    private Entity trevorAnimalNametag = null;
    private boolean trevorQuestOngoing = false;
    private int trevorQuestStartTicks = 0;
    private boolean trevorQuestCooldownNoticed = false;
    private boolean trevorQuestPendingStart = false;
    private int trevorQuestPendingStartStartTicks = 0;
    private boolean highlightSkulls = false;
    private List<BlockPos> skulls = new ArrayList<>(25);
    private List<BlockPos> skullsTemp = new ArrayList<>(25);
    private Tweakception.BlockSearchTask skullsSearchThread;
    private int lastBitsMsgTicks = 0;
    private boolean minionAutoClaim = false;
    private final int[] minionAutoclaimPos = {-2, -2};
    private boolean minionAutoclaimWasInScreen = false;
    private boolean fakePowerScrolls = false;
    private boolean fakeStars = false;
    private int fakeStarsRed = 5;
    private int fakeStarsPurple = 5;
    private int fakeStarsAqua = 2;
    private final Matcher fakeStarsMatcher = Pattern.compile(
        "(?:(?:§.✪)+(?:§c[➊➋➌➍➎])?)?(?<id>§r \\(#\\d{4}(?:/\\d+)?\\))?$").matcher("");
    private final Matcher skyblockLevelExpGainMatcher = Pattern.compile(
        "§b\\+\\d+ SkyBlock XP §7\\(.*§7\\)§b \\(\\d+/100\\)").matcher("");
    private String lastSkyblockLevelExpGainMsg = "";
    private int lastSkyblockLevelExpGainTicks = 0;
    private String snipePlayerName = "";
    private String snipeWarpCmd = "";
    private int snipeLastTpTicks = 0;
    private boolean snipeWarping = false;
    private int snipeTimesWarped = 0;
    private boolean snipeWaitingAtHub = false;
    private boolean abiphoneRelayHint = false;
    private boolean abiphoneRelayInMenu = false;
    private final Queue<String> abiphoneRelaySoundStrings = new ArrayDeque<>();
    // §r         §r§a§lPlayers §r§f(21)§r
    private final Matcher tabListPlayerSectionNameMatcher = Pattern.compile(
        "§r *§r§a§lPlayers §r§f\\(([0-9]{1,2})\\)§r").matcher("");
    private final Matcher minecraftUsernameMatcher = Pattern.compile(
        "[A-Za-z0-9_]{1,16}").matcher("");
    private boolean hideFromStrangers = false;
    private int hideFromStrangersLastWarpTicks = 0;
    private final Matcher auctionPriceMatcher = Pattern.compile(
        "§7(?:Buy it now|Starting bid|Top bid): §6((?:\\d{1,3},?)+) coins$").matcher("");
    private final Matcher petItemJsonExpMatcher = Pattern.compile(
        "\\\"exp\\\":(\\d+.?\\d*E?\\d*)").matcher("");
    private boolean dojoDisciplineHelper = false;
    
    static
    {
        for (SkyblockIsland island : SkyblockIsland.values())
        {
            if (island.subAreas != null)
                ISLANDS_THAT_HAS_SUBAREAS.add(island);
        }
    }
    
    public GlobalTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.globalTweaks;
        Tweakception.overlayManager.addOverlay(new PlayersInAreasDisplayOverlay());
        Tweakception.overlayManager.addOverlay(new PingOverlay());
        Tweakception.overlayManager.addOverlay(new ChampionOverlay());
        Tweakception.overlayManager.addOverlay(new OnlineStatusOverlay());
        Tweakception.overlayManager.addOverlay(new TrevorOverlay());
        npcSkins.add("minecraft:skins/57a517865b820a4451cd3cc6765f370fd0522b6489c9c94fb345fdee2689451a"); // Shaman
        npcSkins.add("minecraft:skins/1642a06cd75ef307c1913ba7a224fb2082d8a2c5254fd1bf006125a087a9a868"); // Taurus
    }
    
    // region Events
    
    public void onPacketReceive(PacketReceiveEvent event)
    {
        packetLogger.logPacket("Receive", event.getPacket());
    }
    
    public void onPacketSend(PacketSendEvent event)
    {
        packetLogger.logPacket("Send", event.getPacket());
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (pingNanos != 0L && System.nanoTime() - pingNanos >= 1000_000_000L * 10)
        {
            pingNanos = 0L;
            if (pingingFromCommand && isInGame())
                sendChat("GT: ping exceeded 10 secs");
        }
        
        if (!isInGame())
            return;
        
        if (event.phase == TickEvent.Phase.START)
        {
            ticks++;
            islandUpdatedThisTick = false;
            playerListUpdatedThisTick = false;
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
        
        if (event.phase == TickEvent.Phase.END)
        {
            if (getTicks() % 5 == 4)
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
                                    String skin = ((AbstractClientPlayer) e).getLocationSkin().toString();
                                    return !npcSkins.contains(skin) && !e.isInvisible(); // Watchdog player is invisible
                                });
                            for (Entity p : players)
                                playersInAreas.put(p.getName(), area);
                        }
                    Tweakception.overlayManager.setEnable(PlayersInAreasDisplayOverlay.NAME, !playersInAreas.isEmpty());
                }
            }
            
            if (minionAutoClaim && getMc().currentScreen instanceof GuiChest)
            {
                GuiChest chest = (GuiChest) McUtils.getMc().currentScreen;
                ContainerChest container = (ContainerChest) chest.inventorySlots;
                IInventory inv = container.getLowerChestInventory();
                String[] words = inv.getName().split(" ");
                int[] pos1 = {-1, -1}; // Both 0 based
                int[] pos2 = {-1, -1};
                if (words.length == 3 && words[1].equals("Minion") &&
                    inv.getSizeInventory() == 54 &&
                    inv.getStackInSlot(54 - 1) != null &&
                    inv.getStackInSlot(54 - 1).getItem() == Item.getItemFromBlock(Blocks.bedrock))
                {
                    pos1[0] = 3;
                    pos1[1] = 2;
                    pos2[0] = 7;
                    pos2[1] = 4;
                }
                else if (words.length == 2 && words[0].equals("Minion") && words[1].equals("Chest") &&
                    inv.getSizeInventory() == 27)
                {
                    pos1[0] = 0;
                    pos1[1] = 0;
                    pos2[0] = 8;
                    pos2[1] = 2;
                }
                
                if (pos1[0] != -1)
                {
                    if (!minionAutoclaimWasInScreen)
                    {
                        minionAutoclaimPos[0] = pos2[0];
                        minionAutoclaimPos[1] = pos2[1];
                    }
                    
                    minionAutoclaimWasInScreen = true;
                    
                    if (getTicks() - minionAutoClaimLastClickTicks >= minionAutoClaimClickDelay)
                    {
                        minionAutoClaimLastClickTicks = getTicks();
                        minionAutoClaimClickDelay = c.minionAutoclaimDelayTicksMin + getWorld().rand.nextInt(3);
                        
                        findLoop:
                        for (; minionAutoclaimPos[1] >= pos1[1];
                             minionAutoclaimPos[1]--, minionAutoclaimPos[0] = pos2[0])
                        {
                            for (; minionAutoclaimPos[0] >= pos1[0]; minionAutoclaimPos[0]--)
                            {
                                int index = 9 * minionAutoclaimPos[1] + minionAutoclaimPos[0];
                                ItemStack stack = inv.getStackInSlot(index);
                                String id = Utils.getSkyblockItemId(stack);
                                if (stack != null && id != null &&
                                    c.minionAutoClaimWhitelist.contains(id))
                                {
                                    getMc().playerController.windowClick(container.windowId, index,
                                        2, 3, getPlayer());
                                    minionAutoclaimPos[0]--;
                                    if (minionAutoclaimPos[0] < pos1[0])
                                    {
                                        minionAutoclaimPos[1]--;
                                        minionAutoclaimPos[0] = pos2[0];
                                    }
                                    break findLoop;
                                }
                            }
                        }
                    }
                }
                else
                    minionAutoclaimWasInScreen = false;
            }
            else
                minionAutoclaimWasInScreen = false;
            
            if (lastTooltip != null && getTicks() - tooltipUpdateTicks > 10)
            {
                lastTooltip = null;
            }
            
            if (c.trevorHighlightAnimal || c.trevorQuestAutoAccept || c.trevorQuestAutoStart)
            {
                GuiScreen screen = getMc().currentScreen;
                if (trevorQuestPendingStart && screen instanceof GuiChest)
                {
                    GuiChest chest = (GuiChest) screen;
                    ContainerChest container = (ContainerChest) chest.inventorySlots;
                    IInventory inv = container.getLowerChestInventory();
                    String containerName = inv.getName();
                    if (containerName.startsWith("Abiphone "))
                    {
                        for (int i = 0; i < inv.getSizeInventory(); i++)
                        {
                            ItemStack stack = inv.getStackInSlot(i);
                            if (stack != null && stack.getDisplayName().equals("§fTrevor"))
                            {
                                trevorQuestPendingStart = false;
                                getMc().playerController.windowClick(container.windowId, i, 0, 0, getPlayer());
                                sendChat("GT-Trevor: quest started");
                                break;
                            }
                        }
                    }
                }
                
                if (trevorQuestStartTicks != 0)
                {
                    int elapsed = getTicks() - trevorQuestStartTicks;
                    
                    if (elapsed >= 20 * 60 * 10 && trevorQuestOngoing)
                    {
                        sendChat("GT-Trevor: quest timed out");
                        trevorQuestStartTicks = 0;
                        trevorAnimalNametag = null;
                        trevorQuestOngoing = false;
                    }
                    
                    if (elapsed >= 20 * 60)
                    {
                        if (!trevorQuestCooldownNoticed)
                        {
                            trevorQuestCooldownNoticed = true;
                            sendChat("GT-Trevor: quest cooldown elapsed");
                        }
                        
                        if (c.trevorQuestAutoStart && !trevorQuestOngoing)
                        {
                            // To prevent failing right after killing the animal
                            Tweakception.scheduler.addDelayed(this::trevorStartFromAbiphone, 40);
                            trevorQuestStartTicks = 0;
                        }
                    }
                    
                }
                
                if (getTicks() - trevorQuestPendingStartStartTicks >= 20 * 11)
                    trevorQuestPendingStart = false;
            }
            
            if (highlightSkulls && getTicks() % 5 == 0)
            {
                if (skullsSearchThread == null || skullsSearchThread.done)
                {
                    EntityPlayerSP p = getPlayer();
                    skulls = skullsTemp;
                    skullsTemp = new ArrayList<>(20);
                    skullsSearchThread = new Tweakception.BlockSearchTask((int) p.posX - 64, 40, (int) p.posZ - 64,
                        (int) p.posX + 64, 150, (int) p.posZ + 64, getWorld(), Blocks.skull, skullsTemp);
                    Tweakception.threadPool.execute(skullsSearchThread);
                }
            }
            
            if (!snipePlayerName.isEmpty())
            {
                if (getTicks() - snipeLastTpTicks >= 20 * 10)
                {
                    sendChat("GT-Snipe: timeout");
                    snipeStop();
                }
                else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_X))
                {
                    sendChat("GT-Snipe: cancelled with Lctrl+X");
                    snipeStop();
                }
                else if (!snipeWarping)
                {
                    if (!snipeWaitingAtHub)
                    {
                        List<String> playerList = getPlayerListFromTabList();
                        
                        if (playerList != null && playerList.size() <= 40 - 2)
                        {
                            boolean found = false;
                            for (String name : playerList)
                            {
                                if (name.equalsIgnoreCase(snipePlayerName))
                                {
                                    sendChat("GT-Snipe: sniped player: " + snipePlayerName + ", times warped: " + snipeTimesWarped);
                                    if (!playersToHighlight.contains(snipePlayerName))
                                    {
                                        sendChat("GT-Snipe: also highlighting them");
                                        setPlayerToHighlight(snipePlayerName);
                                    }
                                    found = true;
                                    snipeStop();
                                    break;
                                }
                            }
                            
                            if (!found && getTicks() - getWorldJoinTicks() >= c.snipeWarpDelayTicks)
                            {
                                String transferIsland = snipeWarpCmd.equals("hub") ? "dhub" : "hub";
                                sendChat("GT-Snipe: full player list detected and player not found, warping to " + transferIsland);
                                McUtils.executeCommand("/warp " + transferIsland);
                                snipeWarping = true;
                                snipeWaitingAtHub = true;
                                snipeLastTpTicks = getTicks();
                            }
                        }
                    }
                    else if (getTicks() - getWorldJoinTicks() >= c.snipeWarpDelayTicks) // Going to hub
                    {
                        snipeTimesWarped++;
                        sendChat("GT-Snipe: warping using: /" + snipeWarpCmd + ", times warped: " + snipeTimesWarped);
                        McUtils.executeCommand("/" + snipeWarpCmd);
                        snipeWarping = true;
                        snipeWaitingAtHub = false;
                        snipeLastTpTicks = getTicks();
                    }
                }
            }
            
            if (abiphoneRelayHint && getMc().currentScreen instanceof GuiChest)
            {
                GuiChest chest = (GuiChest) McUtils.getMc().currentScreen;
                ContainerChest container = (ContainerChest) chest.inventorySlots;
                IInventory inv = container.getLowerChestInventory();
                abiphoneRelayInMenu = inv.getName().equals("9f™ Network Relay") && inv.getSizeInventory() == 54;
            }
            else
                abiphoneRelayInMenu = false;
            if (!abiphoneRelayInMenu)
                abiphoneRelaySoundStrings.clear();
            
            
            if (hideFromStrangers && getTicks() - hideFromStrangersLastWarpTicks >= 20 * 5)
            {
                if (getPlayerListFromTabList()
                    .stream()
                    .map(String::toLowerCase)
                    .anyMatch(name ->
                        !(name.equalsIgnoreCase(getPlayer().getName()) || c.strangerWhitelist.contains(name)))
                )
                {
                    sendChat("GT-HideFromStrangers: evacuating to private island...");
                    getPlayer().sendChatMessage("/is");
                    hideFromStrangersLastWarpTicks = getTicks();
                }
            }
        }
    }
    
    public void onEntityUpdate(LivingEvent.LivingUpdateEvent event)
    {
        if (c.trevorHighlightAnimal &&
            trevorQuestOngoing &&
            event.entity instanceof EntityArmorStand &&
            event.entity.hasCustomName() &&
            event.entity.ticksExisted > 5)
        {
            String name = McUtils.cleanColor(event.entity.getName());
            if (trevorAnimalNametagMatcher.reset(name).matches())
            {
                trevorAnimalNametag = event.entity;
            }
        }
    }
    
    public void onGuiKeyInputPre(GuiScreenEvent.KeyboardInputEvent.Pre event)
    {
        if (c.devMode &&
            Keyboard.getEventKey() == Keyboard.KEY_RCONTROL &&
            Keyboard.getEventKeyState() && !Keyboard.isRepeatEvent())
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
        
        if (!playersToHighlight.isEmpty() || highlightPlayers)
        {
            for (EntityPlayer p : getWorld().playerEntities)
            {
                if (p.isEntityAlive() &&
                    ((highlightPlayers && getMc().getNetHandler().getPlayerInfo(p.getUniqueID()) != null) ||
                    playersToHighlight.contains(p.getName().toLowerCase())))
                {
                    RenderUtils.drawBeaconBeamOrBoundingBox(p, new Color(0, 255, 0, 64), event.partialTicks, 0, 15);
                }
            }
        }
        
        if (!armorStandsToHighlight.isEmpty())
        {
            for (EntityArmorStand armorStand : getWorld().getEntities(EntityArmorStand.class, e -> true))
            {
                for (String name : armorStandsToHighlight)
                {
                    if (armorStand.isEntityAlive() && armorStand.getName().toLowerCase().contains(name))
                    {
                        if (armorStand.hasMarker())
                            RenderUtils.drawBeaconBeamOrBoundingBoxWithBoxSize(
                                armorStand, new Color(0, 255, 0, 64), event.partialTicks, 0, 15, 0.3f, 1.2f);
                        else
                            RenderUtils.drawBeaconBeamOrBoundingBox(
                                armorStand, new Color(0, 255, 0, 64), event.partialTicks, 0, 15);
                        break;
                    }
                }
            }
        }
        
        if (c.trevorHighlightAnimal)
        {
            if (trevorAnimalNametag != null && !trevorAnimalNametag.isDead)
                RenderUtils.drawBeaconBeamAtEntity(trevorAnimalNametag, new Color(0, 255, 0, 80), getPartialTicks());
            else
                trevorAnimalNametag = null;
        }
        
        if (highlightSkulls)
        {
            for (BlockPos pos : skulls)
                RenderUtils.drawBeaconBeamOrBoundingBox(pos, new Color(168, 157, 50, 127), event.partialTicks, 0);
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
    
    public void onLivingRenderPre(RenderLivingEvent.Pre<?> event)
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
    
    public void onLivingSpecialRenderPre(RenderLivingEvent.Specials.Pre<?> event)
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
    
    public void onGuiDrawPost(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (!abiphoneRelayInMenu)
            return;
        
        try
        {
            int xSize = Utils.setAccessibleAndGetField(GuiContainer.class, event.gui, "xSize", "field_146999_f");
            int ySize = Utils.setAccessibleAndGetField(GuiContainer.class, event.gui, "ySize", "field_147000_g");
            int guiLeft = Utils.setAccessibleAndGetField(GuiContainer.class, event.gui, "guiLeft", "field_147003_i");
            int guiTop = Utils.setAccessibleAndGetField(GuiContainer.class, event.gui, "guiTop", "field_147009_r");
            
            FontRenderer fr = getMc().fontRendererObj;
            int y = guiTop + fr.FONT_HEIGHT;
            for (String soundString : abiphoneRelaySoundStrings)
            {
                getMc().fontRendererObj.drawString(soundString,
                    guiLeft + xSize + 20, y, 0xFFFFFFFF);
                y += fr.FONT_HEIGHT;
            }
        }
        catch (Exception ignored)
        {
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
                    Tweakception.globalTweaks.extendAreaPoint();
                    break;
                case Keyboard.KEY_DOWN:
                    Tweakception.globalTweaks.retractAreaPoint();
                    break;
                case Keyboard.KEY_LEFT:
                case Keyboard.KEY_RIGHT:
                    Tweakception.globalTweaks.switchAreaPoints();
                    break;
            }
        }
    }
    
    public void onItemTooltip(ItemTooltipEvent event)
    {
        List<String> tooltip = event.toolTip;
        ItemStack itemStack = event.itemStack;
        
        if (c.disableTooltips)
        {
            tooltip.clear();
            return;
        }
        
        if (c.tooltipDisplaySkyblockItemId && itemStack != null)
        {
            String id = Utils.getSkyblockItemId(itemStack);
            if (id != null && !id.isEmpty())
            {
                tooltip.add("ID: " + id);
            }
        }
        
        if (fakePowerScrolls)
        {
            for (int i = 0; i < tooltip.size(); i++)
            {
                if (tooltip.get(i).contains(" §e§lRIGHT CLICK"))
                {
                    tooltip.set(i, "§6§l⦾§5§l⦾§d§l⦾§c§l⦾§b§l⦾§f§l⦾ " +
                        tooltip.get(i).replaceAll("§.§l⦾ ", ""));
                }
            }
        }
        
        if (fakeStars)
        {
            for (int i = 0; i < tooltip.size(); i++)
            {
                if (fakeStarsMatcher.reset(tooltip.get(i)).find())
                {
                    String[] stars = new String[5];
                    for (int j = 0; j < fakeStarsPurple; j++)
                        stars[j] = "§d✪";
                    for (int j = 0; j < fakeStarsAqua; j++)
                        stars[j] = "§b✪";
                    StringBuilder sb = new StringBuilder();
                    for (String s : stars)
                        if (s != null)
                            sb.append(s);
                    if (fakeStarsRed > 0)
                        sb.append("§c").append("➊➋➌➍➎".charAt(fakeStarsRed - 1));
                    String idPart = fakeStarsMatcher.group("id");
                    tooltip.set(i, fakeStarsMatcher.replaceFirst(sb.toString()) + (idPart == null ? "" : idPart));
                    break;
                }
            }
        }
        
        if (c.ranchersBootsTooltipSpeedNote &&
            "RANCHERS_BOOTS".equals(Utils.getSkyblockItemId(itemStack)) &&
            Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
        {
            for (int i = 0; i < tooltip.size(); i++)
            {
                if (tooltip.get(i).contains("Current Speed Cap: "))
                {
                    int t = 1;
                    tooltip.add(i + t++, "§7 Base: 4.317 m/s");
                    tooltip.add(i + t++, "§7 5 blocks: 93%");
                    tooltip.add(i + t++, "§7 5 m/s: 116%");
                    tooltip.add(i + t++, "§7 3 blocks: 155%");
                    tooltip.add(i + t++, "§7 10 m/s: 232%");
                    tooltip.add(i + t++, "§7 20 m/s: 464%");
                    tooltip.add(i + t++, "§7 45° 10 m/s: 328%");
                    tooltip.add(i + t, "§7 Cocoa: 120%");
                    break;
                }
            }
        }
        
        if (getMc().currentScreen instanceof GuiChest &&
            ((ContainerChest) ((GuiChest) getMc().currentScreen).inventorySlots)
            .getLowerChestInventory().getName().startsWith("Auctions"))
        {
            for (int i = 0; i < tooltip.size(); i++)
            {
                if (auctionPriceMatcher.reset(tooltip.get(i)).find())
                {
                    NBTTagCompound extra = McUtils.getExtraAttributes(itemStack);
                    if (extra != null && petItemJsonExpMatcher.reset(extra.getString("petInfo")).find())
                    {
                        double price = Utils.parseDouble(auctionPriceMatcher.group(1));
                        double exp = Utils.parseDouble(petItemJsonExpMatcher.group(1));
                        double pricePer1mExp = exp == 0.0 ? 0.0 : price / exp * 1_000_000;
                        String string = Utils.formatCommas((long) pricePer1mExp);
                        tooltip.add(i + 1, "§6 " + string + " coins/1m exp");
                        break;
                    }
                }
            }
        }
    
        Consumer<String> addTheItems = keyStart ->
        {
            NBTTagCompound extra = getExtraAttributes(itemStack);
            if (extra == null)
                return;
            TreeMap<Integer, String> map = new TreeMap<>();
            for (String key : extra.getKeySet())
            {
                if (key.startsWith(keyStart))
                {
                    int index = Utils.parseInt(key.split(keyStart, 2)[1]);
                    map.put(index, extra.getString(key));
                }
            }
            for (Map.Entry<Integer, String> entry : map.entrySet())
                tooltip.add("Item " + (entry.getKey() + 1) + ": " + entry.getValue());
        };
        
        if (c.displayPersonalCompactorItems && itemStack != null &&
            Utils.getSkyblockItemId(itemStack) != null &&
            Utils.getSkyblockItemId(itemStack).startsWith("PERSONAL_COMPACTOR_"))
        {
            addTheItems.accept("personal_compact_");
        }
        else if (c.displayPersonalDeletorItems && itemStack != null &&
            Utils.getSkyblockItemId(itemStack) != null &&
            Utils.getSkyblockItemId(itemStack).startsWith("PERSONAL_DELETOR_"))
        {
            addTheItems.accept("personal_deletor_");
        }
    }
    
    public void onWorldLoad(WorldEvent.Load event)
    {
        lastWorldJoin = System.currentTimeMillis();
        lastWorldJoinTicks = getTicks();
        trevorQuestStartTicks = 0;
        trevorQuestCooldownNoticed = false;
        trevorQuestPendingStart = false;
        trevorAnimalNametag = null;
        trevorQuestOngoing = false;
        snipeWarping = false;
    }
    
    public void onPacketSoundEffect(S29PacketSoundEffect packet)
    {
        if (abiphoneRelayInMenu)
        {
            if (!(packet.getSoundName().equals("game.player.hurt") &&
                packet.getPitch() == 0.0f &&
                packet.getVolume() == 0.0f))
            {
                Tweakception.scheduler.add(() ->
                {
                    abiphoneRelaySoundStrings.offer(f("%s   %.3f   %.3f", packet.getSoundName(), packet.getPitch(), packet.getVolume()));
                    if (abiphoneRelaySoundStrings.size() > 15)
                        abiphoneRelaySoundStrings.remove();
                });
            }
        }
    }
    
    public void onChatReceivedGlobal(ClientChatReceivedEvent event)
    {
        String msg = event.message.getUnformattedText();
        if (event.type == 0 || event.type == 1)
        {
            if (msg.startsWith("Your online status has been set to "))
            {
                switch (msg)
                {
                    case "Your online status has been set to Online":
                        c.lastOnlineStatus = "online";
                        break;
                    case "Your online status has been set to Away":
                        c.lastOnlineStatus = "away";
                        break;
                    case "Your online status has been set to Busy":
                        c.lastOnlineStatus = "busy";
                        break;
                    case "Your online status has been set to Appear Offline":
                        c.lastOnlineStatus = "offline";
                        break;
                }
            }
            else if (msg.equals("REMINDER: Your Online Status is currently set to Appear Offline"))
            {
                c.lastOnlineStatus = "offline";
            }
            else if (c.autoGuildWelcome && ((msg = McUtils.cleanColor(msg)) != null) &&
                msg.startsWith("Guild > ") && msg.endsWith(" joined."))
            {
                String name = msg.substring(8, msg.length() - 8);
                McUtils.executeCommand("/gc welcome " + name);
            }
        }
        else if (event.type == 2)
        {
            if (msg.startsWith("You are currently "))
            {
                switch (msg)
                {
                    case "You are currently AWAY":
                        c.lastOnlineStatus = "away";
                        break;
                    case "You are currently BUSY":
                        c.lastOnlineStatus = "busy";
                        break;
                    case "You are currently APPEARING OFFLINE":
                        c.lastOnlineStatus = "offline";
                        break;
                }
            }
        }
    }
    
    public void onChatReceived(ClientChatReceivedEvent event)
    {
        String msg = event.message.getUnformattedText();
        if (event.type == 0 || event.type == 1)
        {
            if (c.trevorHighlightAnimal &&
                McUtils.cleanColor(msg).startsWith("[NPC] Trevor The Trapper: You can find your "))
            {
                trevorQuestStartTicks = getTicks();
                trevorQuestCooldownNoticed = false;
                trevorQuestOngoing = true;
            }
            else if (c.trevorHighlightAnimal &&
                (msg.startsWith("Your mob died randomly, you are rewarded ") ||
                    msg.startsWith("Killing the animal rewarded you ")))
            {
                trevorAnimalNametag = null;
                trevorQuestOngoing = false;
            }
            else if (c.trevorQuestAutoAccept &&
                msg.trim().startsWith("Accept the trapper's task to hunt the animal?"))
            {
                for (IChatComponent part : event.message.getSiblings())
                {
                    ClickEvent onClick = part.getChatStyle().getChatClickEvent();
                    if (onClick != null && onClick.getAction() == ClickEvent.Action.RUN_COMMAND)
                    {
                        String value = onClick.getValue();
                        if (value.startsWith("/chatprompt ") && value.endsWith(" YES"))
                        {
                            getPlayer().sendChatMessage(value);
                            break;
                        }
                    }
                }
            }
        }
        else if (event.type == 2)
        {
            if (msg.endsWith(" Bits from Cookie Buff!") && getTicks() - lastBitsMsgTicks >= 20 * 3)
            {
                lastBitsMsgTicks = getTicks();
                sendChat(msg);
            }
            else if (c.sendSkyblockLevelExpGainMessage &&
                skyblockLevelExpGainMatcher.reset(msg).find() &&
                (getTicks() - lastSkyblockLevelExpGainTicks >= 20 * 5 ||
                    !lastSkyblockLevelExpGainMsg.equals(skyblockLevelExpGainMatcher.group(0))))
            {
                lastSkyblockLevelExpGainMsg = skyblockLevelExpGainMatcher.group(0);
                lastSkyblockLevelExpGainTicks = getTicks();
                sendChat(lastSkyblockLevelExpGainMsg);
            }
        }
    }
    
    // endregion Events
    
    // region Misc
    
    public void printIsland()
    {
        sendChat("GT: " + (currentIsland != null ? currentIsland.name : "none"));
    }
    
    public void updateTooltipToCopy(List<String> list)
    {
        if (list.size() > 0 && !list.get(0).isEmpty())
        {
            lastTooltip = list;
            tooltipUpdateTicks = getTicks();
        }
    }
    
    private void detectSkyblock()
    {
        if (overrideIslandDetection)
            return;
        
        Minecraft mc = getMc();
        isInSkyblock = false;
        isInHypixel = false;
        currentIsland = null;
        currentLocationRaw = "";
        
        islandUpdatedThisTick = true;
        
        if (mc.isSingleplayer())
            return;
        
        String serverBrand = mc.thePlayer.getClientBrand(); // It's actually getServerBrand()
        if (mc.theWorld == null || mc.thePlayer == null || serverBrand == null ||
            !serverBrand.toLowerCase().contains("hypixel"))
            return;
        isInHypixel = true;
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
        ItemStack hoveredStack = null;
        
        GuiScreen screen = getMc().currentScreen;
        if (screen instanceof GuiContainer)
        {
            GuiContainer container = (GuiContainer) screen;
            Slot currentSlot = container.getSlotUnderMouse();
            
            if (currentSlot != null && currentSlot.getHasStack())
            {
                hoveredStack = currentSlot.getStack();
            }
        }
        
        switch (c.rightCtrlCopyType)
        {
            default:
            case "nbt":
                if (hoveredStack == null)
                    return;
                String nbt = hoveredStack.serializeNBT().toString();
                doRealCopy(copyToFile,
                    DumpUtils.prettifyJson(nbt),
                    "nbt",
                    McUtils.cleanColor(hoveredStack.getDisplayName()));
                break;
            case "tooltip":
                if (hoveredStack == null)
                    return;
                List<String> tooltip = hoveredStack.getTooltip(getPlayer(), true);
                if (tooltip.size() == 0)
                    sendChat("Tooltip has 0 line");
                else
                {
                    doRealCopy(copyToFile,
                        String.join(System.lineSeparator(), tooltip),
                        "tooltip",
                        tooltip.get(0));
                }
                break;
            case "tooltipfinal":
                if (lastTooltip != null)
                {
                    doRealCopy(copyToFile,
                        String.join(System.lineSeparator(), lastTooltip),
                        "tooltipfinal",
                        lastTooltip.get(0));
                }
                break;
        }
    }
    
    private void doRealCopy(boolean copyToFile, String string, String type, String fileSubName)
    {
        if (copyToFile)
        {
            File file = null;
            try
            {
                file = Tweakception.configuration.createWriteFileWithCurrentDateTime(
                    type + "_$_" + fileSubName.substring(0, Math.min(fileSubName.length(), 20)) + ".txt",
                    new ArrayList<>(Collections.singleton(string)));
                
                IChatComponent fileName = new ChatComponentText(file.getName());
                fileName.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));
                fileName.getChatStyle().setUnderlined(true);
                
                getPlayer().addChatMessage(new ChatComponentTranslation("GT: written %s to file %s", type, fileName));
            }
            catch (IOException e)
            {
                e.printStackTrace();
                sendChat("GT: exception occurred when creating file");
            }
            
            if (file != null)
            {
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
        }
        else
        {
            Utils.setClipboard(string);
            sendChat("GT: copied item " + type + " to clipboard");
        }
    }
    
    public static boolean isInSkyblock()
    {
        return isInSkyblock;
    }
    
    public static boolean isInHypixel()
    {
        return isInHypixel;
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
                    isInHypixel = true;
                    currentIsland = island;
                    islandUpdatedThisTick = true;
                    sendChat("GT: overridden current island with " + island.name);
                    return;
                }
            sendChat("GT: cannot find island in implemented island list");
        }
    }
    
    public long getWorldJoinMillis()
    {
        return lastWorldJoin;
    }
    
    public long getWorldJoinTicks()
    {
        return lastWorldJoinTicks;
    }
    
    // Returns a new list, or null if both list sections (count) are not in sync or no list
    public List<String> getPlayerListFromTabList()
    {
        if (playerListUpdatedThisTick)
            return new ArrayList<>(playerList);
        
        List<NetworkPlayerInfo> list = TAB_LIST_ORDERING
            .sortedCopy(getPlayer().sendQueue.getPlayerInfoMap());
        playerList.clear();
        playerListUpdatedThisTick = true;
        
        boolean foundSection = false;
        int playerCount = 0;
        if (list.size() >= 80)
        {
            list = list.subList(0, 80);
            GuiPlayerTabOverlay tabList = getMc().ingameGUI.getTabList();
            // 4 section columns, 20 per column
            for (int i = 0; i < 80; i += 20)
            {
                NetworkPlayerInfo sectionEle = list.get(i);
                if (tabListPlayerSectionNameMatcher.reset(tabList.getPlayerName(sectionEle)).matches())
                {
                    foundSection = true;
                    int playerCountNew = Integer.parseInt(tabListPlayerSectionNameMatcher.group(1));
                    if (i != 0 && playerCount != playerCountNew)
                    {
                        playerList.clear();
                        return null;
                    }
                    playerCount = playerCountNew;
                    
                    for (int j = i + 1; j <= i + 19; j += 1)
                    {
                        NetworkPlayerInfo playerEle = list.get(j);
                        if (playerEle.getDisplayName() != null)
                        {
                            List<IChatComponent> playerEleParts = playerEle.getDisplayName().getSiblings();
                            // Reverse because parts in crimson are:[,241,] ,Alan72104 ,⚒️
                            for (int k = playerEleParts.size() - 1; k >= 0; k--)
                            {
                                IChatComponent playerElePart = playerEleParts.get(k);
                                if (minecraftUsernameMatcher
                                    .reset(playerElePart.getUnformattedText().trim())
                                    .matches())
                                {
                                    playerList.add(minecraftUsernameMatcher.group());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (foundSection && playerList.size() == playerCount)
            return new ArrayList<>(playerList);
        else
            return null;
    }
    
    // endregion Misc
    
    // region Feature access
    
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
    
    public boolean isRenderEnchantedBooksTypeOn()
    {
        return c.renderEnchantedBooksType;
    }
    
    public boolean isRenderSacksTypeOn()
    {
        return c.renderSacksType;
    }
    
    public boolean isRenderPotionTierOn()
    {
        return c.renderPotionTier;
    }
    
    public boolean isAfkModeActive()
    {
        return c.afkMode && (!c.afkOnlyUnfocused || !Display.isActive());
    }
    
    public int getAfkFpsLimit()
    {
        return c.afkFpsLimit;
    }
    
    public boolean isAfkSkipWorldRenderingOn()
    {
        return c.afkSkipWorldRendering;
    }
    
    public boolean toggleQuickCraftWhitelist(String id)
    {
        if (c.quickCraftWhitelist.contains(id))
        {
            c.quickCraftWhitelist.remove(id);
            return false;
        }
        else
        {
            c.quickCraftWhitelist.add(id);
            return true;
        }
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
    
    public void pingSend()
    {
        if (pingNanos == 0L)
        {
            getMc().getNetHandler().addToSendQueue(
                new C16PacketClientStatus(C16PacketClientStatus.EnumState.REQUEST_STATS));
            pingNanos = System.nanoTime();
        }
    }
    
    public void pingReset()
    {
        pingNanos = 0L;
    }
    
    public void pingDone()
    {
        if (pingNanos != 0L)
        {
            ping = (int) ((System.nanoTime() - pingNanos) / 1_000_000L);
            pingNanos = 0L;
            if (isInGame() && pingingFromCommand)
            {
                pingingFromCommand = false;
                sendChat("GT: ping = " + ping + " ms");
            }
        }
    }
    
    
    public PacketLogger getPacketLogger()
    {
        return packetLogger;
    }
    
    public boolean isDisableDeadMobTargetingOn()
    {
        return c.targetingDisableDeadMob;
    }
    
    public boolean isDisableArmorStandTargetingOn()
    {
        return c.targetingDisableArmorStand;
    }
    
    public boolean isDisableBatTargetingOn()
    {
        return c.targetingDisableBat;
    }
    
    public boolean isDisablePlayerTargetingOn()
    {
        return c.targetingDisablePlayer;
    }
    
    public boolean isChatLogForceFormattedOn()
    {
        return c.chatLogForceFormatted;
    }
    
    public boolean isDojoDisciplineHelperOn()
    {
        return dojoDisciplineHelper;
    }
    
    private void trevorStartFromAbiphone()
    {
        int slot = Utils.findInHotbarById(id -> id.startsWith("ABIPHONE_"));
        if (slot != -1)
        {
            sendChat("GT-Trevor: starting from abiphone");
            getPlayer().inventory.currentItem = slot;
            ((AccessorMinecraft) getMc()).rightClickMouse();
            trevorQuestPendingStart = true;
            trevorQuestPendingStartStartTicks = getTicks();
        }
        else
        {
            sendChat("GT-Trevor: can't find abiphone in hotbar");
        }
    }
    
    private void snipeStart(String name, String warpCmd)
    {
        snipePlayerName = name;
        snipeWarpCmd = warpCmd;
        snipeLastTpTicks = getTicks();
        snipeTimesWarped = 0;
        updateIslandNow();
        snipeWarping = false;
        snipeWaitingAtHub = true;
        if (!snipeWarpCmd.equals("hub") && getCurrentIsland() != SkyblockIsland.HUB)
        {
            snipeWarping = true;
            Tweakception.scheduler.addDelayed(() -> McUtils.executeCommand("/hub"), 5);
        }
        else if (snipeWarpCmd.equals("hub") && getCurrentIsland() != SkyblockIsland.DUNGEON_HUB)
        {
            snipeWarping = true;
            Tweakception.scheduler.addDelayed(() -> McUtils.executeCommand("/warp dhub"), 5);
        }
    }
    
    private void snipeStop()
    {
        snipePlayerName = "";
        snipeWarpCmd = "";
    }
    
    // endregion Feature access
    
    // region Overlays
    
    private class PlayersInAreasDisplayOverlay extends TextOverlay
    {
        public static final String NAME = "PlayersInAreasDisplayOverlay";
        private final List<Map.Entry<String, SkyblockIsland.SubArea>> sorted = new ArrayList<>();
        
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
            
            sorted.clear();
            sorted.addAll(playersInAreas.entrySet());
            sorted.sort((a, b) ->
            {
                int r = a.getValue().shortName.compareTo(b.getValue().shortName);
                if (r == 0)
                    return a.getKey().compareTo(b.getKey());
                return r;
            });
            sorted.forEach(e -> list.add(e.getKey() + "-" + e.getValue().shortName));
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("player-area name");
            list.add("player2-area name");
            return list;
        }
    }
    
    private class PingOverlay extends TextOverlay
    {
        public static final String NAME = "PingOverlay";
        private long lastPingMillis = 0;
        
        public PingOverlay()
        {
            super(NAME);
            setAnchor(Anchor.BottomCenter);
            setOrigin(Anchor.BottomRight);
            setX(-200);
            setY(-10);
        }
        
        @Override
        public void update()
        {
            super.update();
            
            long millis = System.currentTimeMillis();
            if (millis - lastPingMillis >= 3000)
            {
                lastPingMillis = millis;
                pingSend();
            }
            
            List<String> list = new ArrayList<>();
            list.add("Ping: " + ping + " ms");
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("ping is lSo ms");
            return list;
        }
    }
    
    private class ChampionOverlay extends TextOverlay
    {
        public static final String NAME = "ChampionOverlay";
        private String lastItemUuid = "";
        private long lastExp = 0L;
        private long increment = 0L;
        private int lastIncrementTicks = 0;
        
        public ChampionOverlay()
        {
            super(NAME);
            setAnchor(Anchor.BottomCenter);
            setOrigin(Anchor.BottomRight);
            setX(-200);
            setY(-20);
        }
        
        @Override
        public void update()
        {
            super.update();
            List<String> list = new ArrayList<>();
            
            ItemStack stack = getPlayer().getHeldItem();
            if (stack != null)
            {
                NBTTagCompound extra = McUtils.getExtraAttributes(stack);
                String uuid = Utils.getSkyblockItemUuid(stack);
                
                // Increment is global, any exp gained on the held item will be added to it,
                // overlay only shows if a champion item is held
                if (uuid != null && extra != null && extra.hasKey("champion_combat_xp"))
                {
                    if (!uuid.equals(lastItemUuid))
                    {
                        lastItemUuid = uuid;
                        lastExp = 0L;
                    }
                    
                    StringBuilder sb = new StringBuilder();
                    double xpDouble = extra.getDouble("champion_combat_xp");
                    int level = -1;
                    for (int i = 0; i < Constants.CHAMPION_EXPS.length; i++)
                        if (xpDouble >= Constants.CHAMPION_EXPS[i])
                            level = i;
                        else
                            break;
                    
                    long xp = (long) xpDouble;
                    
                    if (lastExp == 0L) // Just switched to this item, set the last exp
                    {
                        lastExp = xp;
                    }
                    else if (xp > lastExp)
                    {
                        increment += xp - lastExp;
                        lastExp = xp;
                        lastIncrementTicks = getTicks();
                    }
                    else if (increment > 0 &&
                        getTicks() - lastIncrementTicks >= 20 * c.championExpIncrementResetDuration)
                    {
                        increment = 0;
                    }
                    
                    
                    String s = Utils.formatCommas(xp);
                    if (level + 1 < 10)
                    {
                        String ss = Utils.formatCommas(Constants.CHAMPION_EXPS[level + 1]);
                        sb.append("Champion ").append(Constants.ROMAN_NUMERALS[level]).append(": ")
                            .append(s).append("/").append(ss);
                    }
                    else
                    {
                        sb.append("Champion X: ").append(s);
                    }
                    
                    if (increment > 0)
                        sb.append(" §2+").append(Utils.formatCommas(increment));
                    
                    list.add(sb.toString());
                }
            }
            
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("ping is lSo ms");
            return list;
        }
    }
    
    private class OnlineStatusOverlay extends TextOverlay
    {
        public static final String NAME = "OnlineStatusOverlay";
        
        public OnlineStatusOverlay()
        {
            super(NAME);
            setAnchor(Anchor.BottomCenter);
            setOrigin(Anchor.BottomCenter);
            setX(200);
            setY(-20);
        }
        
        @Override
        public void update()
        {
            super.update();
            
            String text;
            switch (c.lastOnlineStatus)
            {
                case "online":
                    if (!c.showOnlineStatusAlreadyOn)
                    {
                        setContent(Collections.emptyList());
                        return;
                    }
                    text = "§aOnline";
                    break;
                case "away":
                    text = "§eAway";
                    break;
                case "busy":
                    text = "§5Busy";
                    break;
                case "offline":
                    text = "§8Offline";
                    break;
                default:
                    text = "Invalid cached status";
                    break;
            }
            
            setContent(Collections.singletonList(text));
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("§aOnline");
            return list;
        }
    }
    
    private class TrevorOverlay extends TextOverlay
    {
        public static final String NAME = "TrevorOverlay";
        
        public TrevorOverlay()
        {
            super(NAME);
            setAnchor(Anchor.BottomCenter);
            setOrigin(Anchor.BottomCenter);
            setX(-200);
            setY(-20);
        }
        
        @Override
        public void update()
        {
            super.update();
            
            List<String> list = new ArrayList<>();
            if (trevorQuestStartTicks != 0)
            {
                int elapsed = (getTicks() - trevorQuestStartTicks) * 50;
                if (trevorQuestOngoing)
                {
                    list.add("§aOngoing quest >>>");
                    list.add("Trevor quest time: " + Utils.msToMMSSmmm(elapsed));
                    
                    if (trevorAnimalNametag != null)
                    {
                        list.add("§aANIMAL IN RANGE");
                        list.add("Distance: §a" +
                            Utils.roundToDigits(getPlayer().getDistanceToEntity(trevorAnimalNametag), 1) +
                            " blocks");
                        list.add(f("Coords: §a%d§r, §a%d§r, §a%d",
                            (int) trevorAnimalNametag.posX,
                            (int) trevorAnimalNametag.posY,
                            (int) trevorAnimalNametag.posZ));
                    }
                }
                else
                {
                    list.add("Trevor quest cooldown: " + Utils.msToMMSSmmm(Math.max(60000 - elapsed, 0)));
                }
            }
            
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("Trevor quest cooldown: 0:0:0");
            return list;
        }
    }
    
    // endregion Overlays
    
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
                        String skin = ((AbstractClientPlayer) e).getLocationSkin().toString();
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
        c.selectedEntityOutlineWidth = w > 0.0f ? w : new GlobalTweaksConfig().selectedEntityOutlineWidth;
        sendChat("GT-DrawSelectedEntityOutline: set width to " + c.selectedEntityOutlineWidth);
    }
    
    public void setSelectedEntityOutlineColor(int r, int g, int b, int a)
    {
        c.selectedEntityOutlineColor = r < 0 ? new GlobalTweaksConfig().selectedEntityOutlineColor
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
    
    public void pingServer()
    {
        if (c.enablePingOverlay)
            sendChat("GT: you have overlay on!");
        else
        {
            if (pingNanos != 0L)
                sendChat("GT: still pinging");
            else
            {
                pingingFromCommand = true;
                pingSend();
                sendChat("GT: pinging");
            }
        }
    }
    
    public void pingOverlay()
    {
        c.enablePingOverlay = !c.enablePingOverlay;
        Tweakception.overlayManager.setEnable(PingOverlay.NAME, c.enablePingOverlay);
        sendChat("GT: toggled ping overlay " + c.enablePingOverlay);
    }
    
    public void toggleLogPacket()
    {
        packetLogger.toggle();
    }
    
    public void setPacketLogAllowedClass(String name)
    {
        packetLogger.toggleAllowedPacket(name);
    }
    
    public void toggleLogPacketLogAll()
    {
        packetLogger.toggleLogAll();
    }
    
    public void toggleChampionOverlay()
    {
        c.enableChampionOverlay = !c.enableChampionOverlay;
        Tweakception.overlayManager.setEnable(ChampionOverlay.NAME, c.enableChampionOverlay);
        sendChat("GT-ChampionOverlay: toggled " + c.enableChampionOverlay);
    }
    
    public void setChampionOverlayIncrementResetDuration(int d)
    {
        c.championExpIncrementResetDuration =
            d > 0 ? d : new GlobalTweaksConfig().championExpIncrementResetDuration;
        sendChat("FT-ChampionOverlay: set increment reset time to " + c.championExpIncrementResetDuration);
    }
    
    public void toggleDisableTooltips()
    {
        c.disableTooltips = !c.disableTooltips;
        sendChat("GT-DisableTooltips: toggled " + c.disableTooltips);
    }
    
    public void toggleRenderEnchantedBooksType()
    {
        c.renderEnchantedBooksType = !c.renderEnchantedBooksType;
        sendChat("GT-RenderEnchantedBooksType: toggled " + c.renderEnchantedBooksType);
    }
    
    public void toggleRenderSacksType()
    {
        c.renderSacksType = !c.renderSacksType;
        sendChat("GT-RenderSacksType: toggled " + c.renderSacksType);
    }
    
    public void toggleRenderPotionTier()
    {
        c.renderPotionTier = !c.renderPotionTier;
        sendChat("GT-RenderPotionTier: toggled " + c.renderPotionTier);
    }
    
    public void toggleMinionAutoClaim()
    {
        minionAutoClaim = !minionAutoClaim;
        sendChat("GT-MinionAutoClaim: toggled " + minionAutoClaim);
    }
    
    public void addMinionAutoClaimWhitelist(String id)
    {
        if (id.isEmpty())
            sendChat("GT-MinionAutoClaim: give id ");
        else
        {
            id = id.toUpperCase();
            c.minionAutoClaimWhitelist.add(id);
            sendChat("GT-MinionAutoClaim: added " + id);
        }
    }
    
    public void removeMinionAutoClaimWhitelist(int i)
    {
        if (i < 1)
        {
            sendChat("GT-MinionAutoClaim: there are " + c.minionAutoClaimWhitelist.size() + " whitelisted IDs");
            int ii = 1;
            for (String id : c.minionAutoClaimWhitelist)
                sendChat(ii++ + ": " + id);
        }
        else
        {
            if (i > c.minionAutoClaimWhitelist.size())
                sendChat("GT-MinionAutoClaim: index is out of bounds!");
            else
            {
                String id = c.minionAutoClaimWhitelist.toArray(new String[0])[i - 1];
                c.minionAutoClaimWhitelist.remove(id);
                sendChat("GT-MinionAutoClaim: removed " + id);
            }
        }
    }
    
    public void setMinionAutoClaimClickDelayMin(int i)
    {
        i = Utils.clamp(i, 1, 20);
        c.minionAutoclaimDelayTicksMin = i;
        sendChat("GT-MinionAutoClaim: set min delay ticks to " + i);
    }
    
    public void toggleTooltipDisplayId()
    {
        c.tooltipDisplaySkyblockItemId = !c.tooltipDisplaySkyblockItemId;
        sendChat("GT-TooltipDisplayItemId: toggled " + c.tooltipDisplaySkyblockItemId);
    }
    
    public void toggleHighlightPlayers()
    {
        highlightPlayers = !highlightPlayers;
        sendChat("GT-HighlightPlayers: toggled " + highlightPlayers);
    }
    
    public void setPlayerToHighlight(String name)
    {
        if (name.equals(""))
        {
            playersToHighlight.clear();
            sendChat("GT-HighlightPlayer: cleared list");
        }
        else
        {
            name = name.toLowerCase();
            if (playersToHighlight.contains(name))
            {
                playersToHighlight.remove(name);
                sendChat("GT-HighlightPlayer: removed " + name);
            }
            else
            {
                playersToHighlight.add(name);
                sendChat("GT-HighlightPlayer: added " + name);
            }
        }
    }
    
    public void setArmorStandToHighlight(String name)
    {
        if (name.equals(""))
        {
            armorStandsToHighlight.clear();
            sendChat("GT-HighlightArmorStand: cleared list");
        }
        else
        {
            name = name.toLowerCase();
            if (armorStandsToHighlight.contains(name))
            {
                armorStandsToHighlight.remove(name);
                sendChat("GT-HighlightArmorStand: removed " + name);
            }
            else
            {
                armorStandsToHighlight.add(name);
                sendChat("GT-HighlightArmorStand: added " + name);
            }
        }
    }
    
    public void toggleOnlineStatusOverlay()
    {
        c.enableOnlineStatusOverlay = !c.enableOnlineStatusOverlay;
        Tweakception.overlayManager.setEnable(OnlineStatusOverlay.NAME, c.enableOnlineStatusOverlay);
        sendChat("GT-OnlineStatusOverlay: toggled " + c.enableOnlineStatusOverlay);
    }
    
    public void toggleOnlineStatusOverlayShowAlreadyOn()
    {
        c.showOnlineStatusAlreadyOn = !c.showOnlineStatusAlreadyOn;
        sendChat("GT-OnlineStatusOverlay: toggled show already on " + c.showOnlineStatusAlreadyOn);
    }
    
    public void toggleTrevorAnimalHighlight()
    {
        c.trevorHighlightAnimal = !c.trevorHighlightAnimal;
        sendChat("GT-Trevor: toggled highlight " + c.trevorHighlightAnimal);
        trevorAnimalNametag = null;
        trevorQuestStartTicks = 0;
        Tweakception.overlayManager.setEnable(TrevorOverlay.NAME,
            c.trevorHighlightAnimal || c.trevorQuestAutoAccept || c.trevorQuestAutoStart);
    }
    
    public void toggleTrevorQuestAutoAccept()
    {
        c.trevorQuestAutoAccept = !c.trevorQuestAutoAccept;
        sendChat("GT-Trevor: toggled auto accept " + c.trevorQuestAutoAccept);
        Tweakception.overlayManager.setEnable(TrevorOverlay.NAME,
            c.trevorHighlightAnimal || c.trevorQuestAutoAccept || c.trevorQuestAutoStart);
    }
    
    public void toggleTrevorQuestAutoStart()
    {
        c.trevorQuestAutoStart = !c.trevorQuestAutoStart;
        sendChat("GT-Trevor: toggled auto start and auto accept " + c.trevorQuestAutoStart);
        c.trevorQuestAutoAccept = c.trevorQuestAutoStart;
        Tweakception.overlayManager.setEnable(TrevorOverlay.NAME,
            c.trevorHighlightAnimal || c.trevorQuestAutoAccept || c.trevorQuestAutoStart);
    }
    
    public void toggleHighlightSkulls()
    {
        highlightSkulls = !highlightSkulls;
        sendChat("GT-HighlightSkulls: toggled " + highlightSkulls);
        if (!highlightSkulls)
        {
            if (skullsSearchThread != null && !skullsSearchThread.done)
                skullsSearchThread.cancel = true;
            skullsSearchThread = null;
        }
    }
    
    public void toggleSendBitsMessage()
    {
        c.sendBitsMessage = !c.sendBitsMessage;
        sendChat("GT-SendBitsMessage: toggled " + c.sendBitsMessage);
    }
    
    public void toggleDisableArmorStandTargeting()
    {
        c.targetingDisableArmorStand = !c.targetingDisableArmorStand;
        sendChat("GT-DisableArmorStandTargeting: toggled " + c.targetingDisableArmorStand);
    }
    
    public void toggleDisableBatTargeting()
    {
        c.targetingDisableBat = !c.targetingDisableBat;
        sendChat("GT-DisableBatTargeting: toggled " + c.targetingDisableBat);
    }
    
    public void toggleDisableDeadMobTargeting()
    {
        c.targetingDisableDeadMob = !c.targetingDisableDeadMob;
        sendChat("GT-DisableDeadMobTargeting: toggled " + c.targetingDisableDeadMob);
    }
    
    public void toggleDisablePlayerTargeting()
    {
        c.targetingDisablePlayer = !c.targetingDisablePlayer;
        sendChat("GT-DisablePlayerTargeting: toggled " + c.targetingDisablePlayer);
    }
    
    public void resetTargeting()
    {
        GlobalTweaksConfig newConfig = new GlobalTweaksConfig();
        c.targetingDisableArmorStand = newConfig.targetingDisableArmorStand;
        c.targetingDisableBat = newConfig.targetingDisableBat;
        c.targetingDisableDeadMob = newConfig.targetingDisableDeadMob;
        c.targetingDisablePlayer = newConfig.targetingDisablePlayer;
        sendChat("GT: reset all targeting options");
    }
    
    public void toggleAfkMode()
    {
        c.afkMode = !c.afkMode;
        sendChat("GT-AfkMode: toggled " + c.afkMode);
    }
    
    public void setAfkFpsLimit(int i)
    {
        if (i == 0)
            i = new GlobalTweaksConfig().afkFpsLimit;
        c.afkFpsLimit = Utils.clamp(i, 5, 120);
        sendChat("GT-AfkMode: set fps limit to " + c.afkFpsLimit);
    }
    
    public void toggleAfkOnlyUnfocused()
    {
        c.afkOnlyUnfocused = !c.afkOnlyUnfocused;
        sendChat("GT-AfkMode: toggled only when unfocused " + c.afkOnlyUnfocused);
    }
    
    public void toggleAfkSkipWorldRendering()
    {
        c.afkSkipWorldRendering = !c.afkSkipWorldRendering;
        sendChat("GT-AfkMode: toggled skip world rendering " + c.afkSkipWorldRendering);
    }
    
    public void toggleFakePowerScrolls()
    {
        fakePowerScrolls = !fakePowerScrolls;
        sendChat("GT-FakePowerScrolls: toggled " + fakePowerScrolls);
    }
    
    public void toggleFakeStars()
    {
        fakeStars = !fakeStars;
        sendChat("GT-FakeStars: toggled " + fakeStars);
    }
    
    public void setFakeStarsRed(int i)
    {
        fakeStarsRed = Utils.clamp(i, 0, 5);
        sendChat("GT-FakeStars: set red count to " + fakeStarsRed);
    }
    
    public void setFakeStarsPurple(int i)
    {
        fakeStarsPurple = Utils.clamp(i, 0, 5);
        sendChat("GT-FakeStars: set purple count to " + fakeStarsPurple);
    }
    
    public void setFakeStarsAqua(int i)
    {
        fakeStarsAqua = Utils.clamp(i, 0, 5);
        sendChat("GT-FakeStars: set aqua count to " + fakeStarsAqua);
    }
    
    public void toggleSendSkyblockLevelExpGainMessage()
    {
        c.sendSkyblockLevelExpGainMessage = !c.sendSkyblockLevelExpGainMessage;
        sendChat("GT-SendSkyblockLevelExpGainMessage: toggled " + c.sendSkyblockLevelExpGainMessage);
    }
    
    public void startSnipe(String name, String warpCmd)
    {
        if (!snipePlayerName.isEmpty())
        {
            sendChat("GT-Snipe: snipe already running");
            return;
        }
        
        name = name.toLowerCase().trim();
        warpCmd = warpCmd.toLowerCase().trim();
        if (name.isEmpty() || warpCmd.isEmpty())
        {
            sendChat("GT-Snipe: snipe params empty");
            return;
        }
        snipeStart(name, warpCmd);
        sendChat("GT-Snipe: starting sniping of player: " + snipePlayerName + ", warp cmd: " + snipeWarpCmd);
    }
    
    public void stopSnipe()
    {
        if (snipePlayerName.isEmpty())
        {
            sendChat("GT-Snipe: snipe not active");
            return;
        }
        snipeStop();
        sendChat("GT-Snipe: stopping sniping");
    }
    
    public void setSnipeWarpDelay(int delay)
    {
        c.snipeWarpDelayTicks = Utils.clamp(delay, 0, 20 * 10);
        sendChat("GT-Snipe: set minimum warp delay to " + c.snipeWarpDelayTicks);
    }
    
    public void toggleAbiphoneRelayHint()
    {
        abiphoneRelayHint = !abiphoneRelayHint;
        sendChat("GT-AbiphoneRelayHint: toggled " + abiphoneRelayHint);
    }
    
    public void toggleHideFromStrangers()
    {
        hideFromStrangers = !hideFromStrangers;
        sendChat("GT-HideFromStrangers: toggled " + hideFromStrangers);
    }
    
    public void setHideFromStrangersWhitelist(String name)
    {
        if (name == null || name.isEmpty())
        {
            sendChat("GT-HideFromStrangers: printing list");
            int i = 0;
            for (String n : c.strangerWhitelist)
            {
                sendChat(++i + ": " + n);
            }
            return;
        }
        
        name = name.toLowerCase();
        if (c.strangerWhitelist.contains(name))
        {
            c.strangerWhitelist.remove(name);
            sendChat("GT-HideFromStrangers: removed " + name + " from whitelist");
        }
        else
        {
            c.strangerWhitelist.add(name);
            sendChat("GT-HideFromStrangers: added " + name + " to whitelist");
        }
    }
    
    public void toggleRanchersBootsTooltipSpeedNote()
    {
        c.ranchersBootsTooltipSpeedNote = !c.ranchersBootsTooltipSpeedNote;
        sendChat("GT-RanchersBootsTooltipSpeedNote: toggled " + c.ranchersBootsTooltipSpeedNote);
    }
    
    public void toggleDisplayPersonalCompactorItems()
    {
        c.displayPersonalCompactorItems = !c.displayPersonalCompactorItems;
        sendChat("GT-DisplayPersonalCompactorItems: toggled " + c.displayPersonalCompactorItems);
    }
    
    public void toggleDisplayPersonalDeletorItems()
    {
        c.displayPersonalDeletorItems = !c.displayPersonalDeletorItems;
        sendChat("GT-DisplayPersonalDeletorItems: toggled " + c.displayPersonalDeletorItems);
    }
    
    public void toggleChatLogForceFormatted()
    {
        c.chatLogForceFormatted = !c.chatLogForceFormatted;
        sendChat("GT-ChatLogForceFormatted: toggled " + c.chatLogForceFormatted);
    }
    
    public void toggleDojoDisciplineHelper()
    {
        dojoDisciplineHelper = !dojoDisciplineHelper;
        sendChat("GT-DojoDisciplineHelper: toggled " + dojoDisciplineHelper);
    }
    
    public void toggleAutoGuildWelcome()
    {
        c.autoGuildWelcome = !c.autoGuildWelcome;
        sendChat("GT-AutoGuildWelcome: toggled " + c.autoGuildWelcome);
    }
    
    public void setGlassesToStones()
    {
        int count = 0;
        Vec3i radius = new Vec3i(128, 1, 128);
        for (BlockPos pos : BlockPos.getAllInBox(
            new BlockPos(getPlayer()).subtract(radius),
            new BlockPos(getPlayer()).add(radius)))
        {
            Block theBlock = getWorld().getBlockState(pos).getBlock();
            if (theBlock == Blocks.stained_glass || theBlock == Blocks.glass)
            {
                count++;
                getWorld().setBlockState(pos, Blocks.stone.getDefaultState());
            }
        }
        sendChat("GT: set " + count + " blocks to stone");
    }
    
    // endregion Commands
}
