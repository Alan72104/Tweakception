package a7.tweakception.tweaks;

import a7.tweakception.DevSettings;
import a7.tweakception.Scheduler;
import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.events.IslandChangedEvent;
import a7.tweakception.mixin.AccessorGuiContainer;
import a7.tweakception.mixin.AccessorGuiPlayerTabOverlay;
import a7.tweakception.mixin.AccessorMinecraft;
import a7.tweakception.overlay.Anchor;
import a7.tweakception.overlay.TextOverlay;
import a7.tweakception.utils.*;
import a7.tweakception.utils.timers.StopwatchTimer;
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
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.*;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static a7.tweakception.utils.McUtils.*;
import static a7.tweakception.utils.Utils.f;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
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
        public boolean afkAutoRightClick = false;
        public int afkFpsLimit = 60;
        public boolean sendSkyblockLevelExpGainMessage = false;
        public int snipeWarpDelayTicks = 40;
        public TreeSet<String> strangerWhitelist = new TreeSet<>();
        public boolean ranchersBootsTooltipSpeedNote = false;
        public boolean displayPersonalCompactorItems = true;
        public boolean displayPersonalDeletorItems = true;
        public boolean chatLogForceFormatted = false;
        public boolean autoGuildWelcome = false;
        public int autoHarpClickDelayTicks = 10;
        public boolean autoHarpAutoClose = false;
        public boolean autoHarpReplayMode = false;
        public boolean buildersWandItemsTooltip = false;
        public boolean ignoreServerChunkUnloadDistance = false;
        public int clientChunkUnloadDistance = 8;
        public boolean armorColorSortingHelper = false;
        public boolean hideMinionStorageFull = false;
        public String fastCommand = "";
        public boolean autoConsumeBoosterCookie = false;
        public boolean minionAutoClaimHopper = false;
        public boolean centuryCakeCountInChat = true;
        public boolean autoHarp = false;
    }
    
    private final GlobalTweaksConfig c;
    private static final HashMap<String, SkyblockIsland> ISLAND_NAME_TO_ISLAND_MAP = new HashMap<>();
    private static final List<SkyblockIsland> ISLANDS_THAT_HAS_SUBAREAS = new ArrayList<>();
    // Below class loaded in tweakception
    private static final Ordering<NetworkPlayerInfo> TAB_LIST_ORDERING = AccessorGuiPlayerTabOverlay.getTabListOrdering();
    private static final Map<String, String> HARP_DATA = new HashMap<>();
    private static int ticks = 0;
    private static boolean islandUpdatedThisTick = false;
    private static boolean playerListUpdatedThisTick = false;
    private static boolean isInSkyblock = false;
    private static boolean isInHypixel = false;
    private static boolean overridenIslandDetection = false;
    @Nullable private static SkyblockIsland prevIsland = null;
    @Nullable private static SkyblockIsland currentIsland = null;
    @Nonnull private static String currentLocationRaw = "";
    @Nonnull private static String currentLocationRawCleaned = "";
    @Nonnull private static String currentServerType = "";
    private static final Matcher invalidLocationLetterMatcher = Pattern.compile("[^A-Za-z0-9() \\-']").matcher("");
    private static final Map<String, Runnable> chatActionMap = new HashMap<>();
    private static final HashSet<String> npcSkins = new HashSet<>();
    private static final PacketLogger packetLogger = new PacketLogger();
    private static final List<String> playerList = new ArrayList<>(40);
    public static boolean t = false;
    private int pendingCopyStartTicks = -1;
    private boolean editingAreas = false;
    private int selectedAreaPointIndex = 0;
    private BlockPos[] areaPoints = null;
    private final List<PlayerLocation> playerLocations = new ArrayList<>();
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
    private final Set<String> hidePlayersWhitelist = new HashSet<>();
    private final Set<String> playersToHighlight = new HashSet<>();
    private final Set<String> armorStandsToHighlight = new HashSet<>();
    // [Lv3] Undetected Sheep 2000/2000❤
    private final Matcher trevorAnimalNametagMatcher = Pattern.compile(
        "\\[Lv[0-9]+] (?<rarity>[a-zA-Z]+) (?<animal>[a-zA-Z]+) .*❤").matcher("");
    private final Set<Entity> trevorTempEntitySet = Collections.newSetFromMap(new WeakHashMap<>());
    private Entity trevorAnimal = null;
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
    private boolean hideFromStrangers = false;
    private int hideFromStrangersLastWarpTicks = 0;
    private final Matcher petItemJsonExpMatcher = Pattern.compile(
        "\"exp\":(\\d+.?\\d*E?\\d*)").matcher("");
    private boolean dojoDisciplineHelper = false;
    private final Item[] autoHarpLastChestItems = new Item[28];
    private int autoHarpReplayIndex = 0;
    private int autoHarpReplayLastClickTicks = 0;
    private String autoHarpReplayData = null;
    private long windowOpenMillis = 0;
    private final Set<ChunkCoordIntPair> pendingUnloadChunks = new HashSet<>();
    private ChunkCoordIntPair lastChunkUnloadPosition = new ChunkCoordIntPair(0, 0);
    private List<String> tooltipOverride = null;
    private final Set<BlockPos> blocksToHighlight = new HashSet<>();
    private final Set<String> entityTypesToHighlight = new HashSet<>();
    private final Map<String, Integer> recentlyClaimedMinionHoppers = new HashMap<>();
    private final StopwatchTimer centuryCakeTimer = new StopwatchTimer(1000 * 60 * 10);
    private int centuryCakeCount = 0;
    
    private static class PlayerLocation implements Comparable<PlayerLocation>
    {
        String name;
        SkyblockIsland.SubArea area;
        long joinMillis;
        long leaveMillis;
        
        @Override
        public int compareTo(PlayerLocation that)
        {
            int i = this.area.shortName.compareTo(that.area.shortName);
            if (i != 0)
                return i;
            return this.name.compareTo(that.name);
        }
    }
    
    static
    {
        for (SkyblockIsland island : SkyblockIsland.values())
        {
            ISLAND_NAME_TO_ISLAND_MAP.put(island.name, island);
            if (island.areas != null)
                ISLANDS_THAT_HAS_SUBAREAS.add(island);
        }
        HARP_DATA.put("Hymn to the Joy",
            "3.3.4.5.|5.4.3.2.|1.1.2.3.|3..22..|" +
            "3.3.4.5.|5.4.3.2.|1.1.2.3.|2..11..|");
        HARP_DATA.put("Frère Jacques",
            "2..3..4..2..|2..3..4..2...|" +
            "4..5..6....|4..5..6....|" +
            "6.7.6.5.4...2...|6.7.6.5.4..2..|" +
            "2..1..2...|2..1..2...|");
        HARP_DATA.put("Amazing Grace",
            "1..|3..3...5.4.3.|5...4..|3...2..|1...1..|" +
            "3..3...5.4.3.|5...4..|6..6....|" +
            "4..6..6...5.4.3.|5....4..|3...2.|1....1..|" +
            "3..3...5.4.3.|5...4..|3.....|");
        HARP_DATA.put("Brahm's Lullaby",
            "1.1.|3...1.1.|3...1.3.|6..5..4.|4..3...1.2.|" +
            "3..1..1.2.|3...1.3.|6.5.4..6..|7...|");
        HARP_DATA.put("Happy Birthday to You",
            "1.1|2..1..3..|2....1.1|2..1..4..|3....1..1.|" +
            "6..5..4..|3.2....5.5|4..2..3..|2........|");
        HARP_DATA.put("Greensleeves",
            "3.|4..5.|6..76.|5..4.|3..23.|" +
            "4..3.|3.23.|4..2.|1..3.|" +
            "4..5.|6..76.|5..4.|2..34.|" +
            "5..43.|2..12.|3.....|");
        HARP_DATA.put("Geothermy?",
            "3.|5.3.1.3.|5.3...3.|5.3.5.7|65432.2.|" +
            "5.3.1.3.|5.3..7.|65.43.2.|1.......|");
        HARP_DATA.put("Minuet",
            "6.2345|6.2.2.|7.3456|7.2.2.|" +
            "5.6543|4.5432|3.4321|2.....|");
        HARP_DATA.put("Joy to the World",
            "7.6.5|4..4.|3.2|1..4|" +
            "5..5|6..6|7..7|7654.4|" +
            "432135|7.....|");
        HARP_DATA.put("Godly Imagination",
            "3.7.2.|3.7.2.|3.7.2.|3.7.2..|" +
            "1.3.5..|1.2.5..|1.3.6.7|67676.5.|" +
            "2.3.4..|6.7.5..|4.3.2.3|23232.1|");
        HARP_DATA.put("La Vie en Rose",
            "6..5|4.3|2.6|5..4|3.2|1.5|4..3|2.1|2.5|4..|3...$|" +
            "7..6|5.4|3.6|5..4|3.2|1.5|4..3|2.1|2.5|4...|3...$|" +
            "6..5|4.3|2.6|5..4|3.2|1.5|4..3|2..$|" +
            "1.5|5...|6.6|..5|6.6|..5|6.6|..5$|" +
            "2...|6.6|..5|6.6|..5|6.6|..5|7.4|..6.|" +
            "1..5|654.3|2.6|5.1|..4|3.2|1.5|4.3|4.5|6..|");
        HARP_DATA.put("Through the Campfire",
            "23423453|64534543|23423453|64534543|" +
            "23423453|64534543|23423453|64534543|" +
            "23423453|64534543|23423453|64534576|" +
            "56545434|32321|" +
            "3.13.131|3313.131|6.36.363|6636.363|" +
            "5.25.252|5525.2|432321.|" +
            "3.13.131|3313.131|6.36.363|6736.363|" +
            "5.25.252|5525.2|432321.|27....|");
        HARP_DATA.put("Pachelbel",
            "6...4...|5...2...|3...1...|3...4...|" +
            "4.3.4.1.|1...3..|4..5..|6..6.7.|" +
            "6.5.4.7.|6.5.4.3.|2.1.3.3.|432..|" +
            "6.456.45|61234567|6.234.23|45434656|" +
            "3.654.32|32123456|4.656.32|345676.|" +
            "456.54|53456543|4.234.45|67656.545|" +
            "3.543.32|32123456|4.656.32|345674|");
    }
    
    public GlobalTweaks(Configuration configuration)
    {
        super(configuration, "GT");
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
    
    public void onPacketReceive(Packet<?> packet)
    {
        packetLogger.logPacket("Receive", packet);
    }
    
    public void onPacketSend(Packet<?> packet)
    {
        packetLogger.logPacket("Send", packet);
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (pingNanos != 0L && System.nanoTime() - pingNanos >= 1000_000_000L * 10)
        {
            pingNanos = 0L;
            if (pingingFromCommand && isInGame())
                sendChat("Ping exceeded 10 secs");
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
                    if (island != null && ISLANDS_THAT_HAS_SUBAREAS.contains(island))
                    {
                        List<PlayerLocation> toRemove = new ArrayList<>(playerLocations);
                        long millis = System.currentTimeMillis();
                        for (SkyblockIsland.SubArea area : island.areas)
                        {
                            List<EntityOtherPlayerMP> players = getWorld().getEntitiesWithinAABB(EntityOtherPlayerMP.class,
                                area.box,
                                e ->
                                {
                                    NetworkPlayerInfo networkPlayerInfo = getMc().getNetHandler().getPlayerInfo(e.getUniqueID());
                                    return !npcSkins.contains(e.getLocationSkin().toString()) &&
                                        networkPlayerInfo != null &&
                                        networkPlayerInfo.getResponseTime() == 1; // 0 for npc, 1 for real players
                                });
                            
                            for (EntityOtherPlayerMP player : players)
                            {
                                PlayerLocation oldEntry = null;
                                for (PlayerLocation e : toRemove)
                                    if (e.name.equals(player.getName()) && e.area == area)
                                    {
                                        oldEntry = e;
                                        break;
                                    }
                                if (oldEntry != null)
                                {
                                    if (oldEntry.leaveMillis != 0)
                                    {
                                        oldEntry.joinMillis = millis;
                                        oldEntry.leaveMillis = 0;
                                    }
                                    toRemove.remove(oldEntry);
                                }
                                else
                                {
                                    PlayerLocation newEntry = new PlayerLocation();
                                    newEntry.name = player.getName();
                                    newEntry.area = area;
                                    newEntry.joinMillis = millis;
                                    playerLocations.add(newEntry);
                                }
                            }
                        }
                        for (PlayerLocation ele : toRemove)
                        {
                            if (ele.leaveMillis == 0)
                                ele.leaveMillis = millis;
                            else if (millis - ele.leaveMillis >= 5000)
                                playerLocations.remove(ele);
                        }
                    }
                    else
                    {
                        playerLocations.clear();
                    }
                    Tweakception.overlayManager.setEnable(PlayersInAreasDisplayOverlay.NAME, !playerLocations.isEmpty());
                }
            }
            else if (getTicks() % 1200 == 0)
            {
                if (c.afkAutoRightClick && getMc().currentScreen == null)
                {
                    sendChat("AfkMode: Right clicking");
                    ((AccessorMinecraft) getMc()).rightClickMouse();
                }
            }
            
            recentlyClaimedMinionHoppers.entrySet().removeIf(entry -> getTicks() - entry.getValue() >= 20 * 60);
            if (c.minionAutoClaimHopper && McUtils.getOpenedChest() != null &&
                McUtils.getOpenedChest().getSizeInventory() == 54)
            {
                IInventory inv = McUtils.getOpenedChest();
                ItemStack hopper = inv.getStackInSlot(9 * 3 + 2 - 1);
                if (hopper != null &&
                    ("ENCHANTED_HOPPER".equals(Utils.getSkyblockItemId(hopper)) ||
                    "HOPPER".equals(Utils.getSkyblockItemId(hopper)))
                )
                {
                    String[] lore = McUtils.getDisplayLore(hopper);
                    for (String s : lore)
                    {
                        if (s.startsWith("§7Held Coins: "))
                        {
                            String uuid = Utils.getSkyblockItemUuid(hopper);
                            if (uuid != null && !s.equals("§7Held Coins: §b0") &&
                                !recentlyClaimedMinionHoppers.containsKey(uuid))
                            {
                                getMc().playerController.windowClick(getPlayer().openContainer.windowId,
                                    9 * 3 + 2 - 1,
                                    WindowClickContants.LeftRight.BTN_LEFT,
                                    WindowClickContants.LeftRight.MODE, getPlayer());
                                getPlayer().closeScreen();
                                recentlyClaimedMinionHoppers.put(uuid, getTicks());
                            }
                            break;
                        }
                    }
                }
                
            }
            else if (minionAutoClaim && McUtils.getOpenedChest() != null)
            {
                IInventory inv = McUtils.getOpenedChest();
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
                                    getMc().playerController.windowClick(getPlayer().openContainer.windowId, index,
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
                    IInventory inv = McUtils.getOpenedChest();
                    String containerName = inv.getName();
                    if (containerName.startsWith("Abiphone "))
                    {
                        for (int i = 0; i < inv.getSizeInventory(); i++)
                        {
                            ItemStack stack = inv.getStackInSlot(i);
                            if (stack != null && stack.getDisplayName().equals("§fTrevor"))
                            {
                                trevorQuestPendingStart = false;
                                getMc().playerController.windowClick(getPlayer().openContainer.windowId, i, 0, 0, getPlayer());
                                sendChat("Trevor: Quest started");
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
                        sendChat("Trevor: Quest timed out");
                        trevorQuestStartTicks = 0;
                        trevorQuestOngoing = false;
                    }
                    
                    if (elapsed >= 20 * 60)
                    {
                        if (!trevorQuestCooldownNoticed)
                        {
                            trevorQuestCooldownNoticed = true;
                            sendChat("Trevor: Quest cooldown elapsed");
                        }
                        
                        if (c.trevorQuestAutoStart && !trevorQuestOngoing)
                        {
                            // To prevent failing right after killing the animal
                            Tweakception.scheduler.addDelayed(this::trevorStartFromAbiphone, 20);
                            trevorQuestStartTicks = 0;
                        }
                    }
                    
                }
                
                if (getTicks() - trevorQuestPendingStartStartTicks >= 20 * 11)
                    trevorQuestPendingStart = false;
                
                if (trevorQuestOngoing)
                {
                    trevorAnimal = null;
                    trevorTempEntitySet.clear();
                    for (Entity e : getWorld().loadedEntityList)
                    {
                        if (!e.isDead &&
                            (e instanceof EntityCow ||
                            e instanceof EntityPig ||
                            e instanceof EntitySheep ||
                            e instanceof EntityChicken ||
                            e instanceof EntityRabbit ||
                            e instanceof EntityHorse))
                        {
                            float max = ((EntityLivingBase) e).getMaxHealth();
                            if (max == 100 ||
                                max == 500 ||
                                max == 1000 ||
                                max == 5000 ||
                                max == 10000 ||
                                max == 200 || // Derpy hps
                                max == 2000 ||
                                max == 20000)
                            {
                                trevorTempEntitySet.add(e);
                                List<Entity> nearNametags = getWorld().getEntitiesWithinAABB(EntityArmorStand.class,
                                    e.getEntityBoundingBox().expand(2, 3, 2),
                                    en -> en.hasCustomName() &&
                                        en.ticksExisted > 5 &&
                                        trevorAnimalNametagMatcher.reset(McUtils.cleanColor(en.getName())).matches()
                                );
                                if (!nearNametags.isEmpty())
                                {
                                    trevorAnimal = e;
                                    trevorTempEntitySet.clear();
                                    break;
                                }
                            }
                        }
                    }
                }
                else
                {
                    trevorTempEntitySet.clear();
                    trevorAnimal = null;
                }
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
                    sendChat("Snipe: Timeout");
                    snipeStop();
                }
                else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_X))
                {
                    sendChat("Snipe: Cancelled with Lctrl+X");
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
                                    sendChat("Snipe: Sniped player: " + snipePlayerName + ", times warped: " + snipeTimesWarped);
                                    if (!playersToHighlight.contains(snipePlayerName))
                                    {
                                        sendChat("Snipe: Also highlighting them");
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
                                sendChat("Snipe: Full player list detected and player not found, warping to " + transferIsland);
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
                        sendChat("Snipe: Warping using: /" + snipeWarpCmd + ", times warped: " + snipeTimesWarped);
                        McUtils.executeCommand("/" + snipeWarpCmd);
                        snipeWarping = true;
                        snipeWaitingAtHub = false;
                        snipeLastTpTicks = getTicks();
                    }
                }
            }
            
            if (abiphoneRelayHint && McUtils.getOpenedChest() != null)
            {
                IInventory inv = McUtils.getOpenedChest();
                abiphoneRelayInMenu = inv.getName().equals("9f™ Network Relay") && inv.getSizeInventory() == 54;
            }
            else
                abiphoneRelayInMenu = false;
            if (!abiphoneRelayInMenu)
                abiphoneRelaySoundStrings.clear();
            
            
            if (hideFromStrangers && getTicks() - hideFromStrangersLastWarpTicks >= 20 * 5)
            {
                List<String> list = McUtils.getRealPlayers();
                for (String s : list)
                {
                    String name = s.toLowerCase();
                    if (!(name.equalsIgnoreCase(getPlayer().getName()) || c.strangerWhitelist.contains(name)))
                    {
                        sendChat("HideFromStrangers: Evacuating to private island...");
                        getPlayer().sendChatMessage("/is");
                        hideFromStrangersLastWarpTicks = getTicks();
                        break;
                    }
                }
            }
            
            // Probably want to check after the whole container is updated
            if (c.autoHarp && McUtils.getOpenedChest() != null)
            {
                IInventory inv = McUtils.getOpenedChest();
                if (inv.getSizeInventory() == 54 && inv.getName().startsWith("Harp - "))
                {
                    boolean changed = false;
                    for (int j = 0; j < 4; j++)
                    {
                        for (int i = 1; i < 8; i++)
                        {
                            ItemStack stack = inv.getStackInSlot(j * 9 + i);
                            Item item = stack == null ? null : stack.getItem();
                            
                            if (item != null && autoHarpLastChestItems[j*7 + (i-1)] != item)
                                changed = true;
                            
                            autoHarpLastChestItems[j*7 + (i-1)] = item;
                        }
                    }
                    
                    if (c.autoHarpReplayMode)
                    {
                        if (autoHarpReplayData == null)
                        {
                            if (autoHarpReplayLastClickTicks == 0)
                            {
                                boolean noteAppeared = false;
                                for (int i = 0; i < 7; i++)
                                    if (autoHarpLastChestItems[3 * 7 + i] == Item.getItemFromBlock(Blocks.wool))
                                        noteAppeared = true;
                                if (noteAppeared)
                                {
                                    autoHarpReplayLastClickTicks = getTicks();
                                }
                            }
                            else if (getTicks() - autoHarpReplayLastClickTicks >= c.autoHarpClickDelayTicks)
                            {
                                String songName = inv.getName().substring(7);
                                autoHarpReplayData = HARP_DATA.get(songName);
                                if (autoHarpReplayData != null)
                                    autoHarpReplayData = autoHarpReplayData.replace("|", "");
                                else
                                {
                                    sendChat("AutoHarp: There is currently no replay data for " + songName + "!");
                                    autoHarpReplayData = "";
                                }
                                autoHarpReplayIndex = 0;
                                autoHarpReplayLastClickTicks = getTicks() - 5;
                            }
                        }
                        
                        if (autoHarpReplayData != null &&
                            !autoHarpReplayData.isEmpty() &&
                            getTicks() - autoHarpReplayLastClickTicks >= 5)
                        {
                            autoHarpReplayLastClickTicks = getTicks();
                            if (autoHarpReplayIndex < autoHarpReplayData.length())
                            {
                                char note = autoHarpReplayData.charAt(autoHarpReplayIndex);
                                if (note != '.')
                                {
                                    getMc().playerController.windowClick(getPlayer().openContainer.windowId,
                                        4 * 9 + 1 + (note - '1'),
                                        2, 3, getPlayer());
                                }
                            }
                            autoHarpReplayIndex++;
                        }
                    }
                    else
                    {
                        if (changed && System.currentTimeMillis() - getWindowOpenMillis() >= 500)
                        {
                            for (int i = 0; i < 7; i++)
                            {
                                if (autoHarpLastChestItems[2 * 7 + i] == Item.getItemFromBlock(Blocks.wool))
                                {
                                    int index = i;
                                    int windowId = getPlayer().openContainer.windowId;
                                    Tweakception.scheduler.addDelayed(() ->
                                    {
                                        if (getPlayer().openContainer.windowId == windowId)
                                        {
                                            getMc().playerController.windowClick(getPlayer().openContainer.windowId,
                                                4 * 9 + index + 1,
                                                WindowClickContants.Middle.BTN,
                                                WindowClickContants.Middle.MODE,
                                                getPlayer());
                                        }
                                    }, c.autoHarpClickDelayTicks);
                                }
                            }
                        }
                    }
                    
                    if (c.autoHarpAutoClose)
                    {
                        for (int i = 1; i < 8; i++)
                        {
                            ItemStack stack = inv.getStackInSlot(5 * 9 + 1 + i);
                            if (stack != null && stack.getItem() == Item.getItemFromBlock(Blocks.wool))
                            {
                                getPlayer().closeScreen();
                                sendChat("AutoHarp: Closed harp (non perfect)");
                                break;
                            }
                        }
                    }
                }
                else
                {
                    autoHarpReplayData = null;
                    autoHarpReplayIndex = 0;
                    autoHarpReplayLastClickTicks = 0;
                }
            }
            
            if (c.autoConsumeBoosterCookie && McUtils.getOpenedChest() != null &&
                McUtils.getOpenedChest().getName().equals("Consume Booster Cookie?"))
            {
                for (int i = 0; i < McUtils.getOpenedChest().getSizeInventory(); i++)
                {
                    ItemStack stack = McUtils.getOpenedChest().getStackInSlot(i);
                    if (stack != null && stack.hasDisplayName() && stack.getDisplayName().equals("§eConsume Cookie"))
                    {
                        getMc().playerController.windowClick(getPlayer().openContainer.windowId,
                            i, WindowClickContants.Middle.BTN, WindowClickContants.Middle.MODE, getPlayer());
                        getPlayer().closeScreen();
                        break;
                    }
                }
            }
        }
    }
    
    public void onEntityUpdate(LivingEvent.LivingUpdateEvent event)
    {
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
                    ((highlightPlayers && getMc().getNetHandler().getPlayerInfo(p.getUniqueID()) != null && !p.getName().equals(getPlayer().getName())) ||
                    playersToHighlight.contains(p.getName().toLowerCase(Locale.ROOT))))
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
                    if (armorStand.isEntityAlive() && armorStand.getName().toLowerCase(Locale.ROOT).contains(name))
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
            if (trevorAnimal != null)
                RenderUtils.drawBeaconBeamAtEntity(trevorAnimal, new Color(0, 255, 0, 80), getPartialTicks());
            else if (!trevorTempEntitySet.isEmpty())
                for (Entity e : trevorTempEntitySet)
                {
                    if (e.getDistanceSqToEntity(getPlayer()) <= 25 * 25)
                        RenderUtils.drawBeaconBeamAtEntity(e, new Color(0, 255, 0, 80), getPartialTicks());
                    else
                        RenderUtils.drawBeaconBeamAtEntity(e, new Color(0, 255, 0, 40), getPartialTicks());
                }
        }
        
        if (highlightSkulls)
        {
            for (BlockPos pos : skulls)
                RenderUtils.drawBeaconBeamOrBoundingBox(pos, new Color(168, 157, 50, 128), event.partialTicks, 0);
        }
        
        if (!blocksToHighlight.isEmpty())
        {
            for (BlockPos pos : blocksToHighlight)
            {
                double dist = Math.sqrt(getPlayer().getDistanceSqToCenter(pos));
                int alpha = (int) Utils.mapClamp(dist, 5, 30, 32, 128);
                RenderUtils.drawBeaconBeamOrBoundingBox(pos, new Color(0, 255, 0, alpha), event.partialTicks, 1);
            }
        }
        
        if (!entityTypesToHighlight.isEmpty())
        {
            for (Entity e : getWorld().loadedEntityList)
            {
                String name = e.getClass().getSimpleName().toLowerCase(Locale.ROOT);
                for (String type : entityTypesToHighlight)
                {
                    if (name.contains(type))
                    {
                        RenderUtils.drawBeaconBeamOrBoundingBox(e, new Color(0, 255, 0, 64), event.partialTicks, 15);
                        break;
                    }
                }
            }
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
            if (event.entity instanceof EntityOtherPlayerMP &&
                !hidePlayersWhitelist.contains(event.entity.getName().toLowerCase(Locale.ROOT)))
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
        Entity e = event.entity;
        if (c.highlightShinyPigs && getCurrentIsland() == SkyblockIsland.HUB &&
            e.hasCustomName() &&
            e instanceof EntityArmorStand &&
            McUtils.cleanColor(e.getName()).equalsIgnoreCase(c.shinyPigName))
        {
            List<Entity> pig = getWorld().getEntitiesWithinAABB(EntityPig.class,
                e.getEntityBoundingBox().expand(0.0, 2.5, 0.0));
            List<EntityArmorStand> armorStands = getWorld().getEntitiesWithinAABB(EntityArmorStand.class,
                e.getEntityBoundingBox().expand(0.2, 2.5, 0.2));
            
            if (pig.size() > 0)
                RenderUtils.drawDefaultHighlightBoxForEntity(pig.get(0), RenderUtils.DEFAULT_HIGHLIGHT_COLOR, false);
            
            for (EntityArmorStand armorStand : armorStands)
            {
                String shinyOrbTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODJjZGUwNjhlOTlhNGY5OGMzMWY4N2I0Y2MwNmJlMTRiMjI5YWNhNGY3MjgxYTQxNmM3ZTJmNTUzMjIzZGI3NCJ9fX0=";
                String tex = McUtils.getArmorStandHeadTexture(armorStand);
                if (tex != null && tex.equals(shinyOrbTexture))
                {
                    RenderUtils.drawDefaultHighlightBoxUnderEntity(e, -1, RenderUtils.DEFAULT_HIGHLIGHT_COLOR, false);
                    break;
                }
            }
        }
        else if (c.hideMinionStorageFull && getCurrentIsland() == SkyblockIsland.PRIVATE_ISLAND &&
            e.hasCustomName() &&
            e.getCustomNameTag().equals("§cMy storage is full! :("))
        {
            event.setCanceled(true);
        }
    }
    
    public void onGuiDrawPost(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (McUtils.getOpenedChest() == null)
            return;
        
        IInventory inv = McUtils.getOpenedChest();
        ContainerChest container = getOpenedChestContainer();
        if (abiphoneRelayInMenu)
        {
            AccessorGuiContainer accessor = (AccessorGuiContainer) event.gui;
            int xSize = accessor.getXSize();
            int guiLeft = accessor.getGuiLeft();
            int guiTop = accessor.getGuiTop();
            
            FontRenderer fr = getMc().fontRendererObj;
            int y = guiTop + fr.FONT_HEIGHT;
            for (String soundString : abiphoneRelaySoundStrings)
            {
                fr.drawString(soundString,
                    guiLeft + xSize + 20, y, 0xFFFFFFFF);
                y += fr.FONT_HEIGHT;
            }
        }
        else if (c.armorColorSortingHelper &&
            Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
        {
            List<Slot> slots = container.inventorySlots.subList(0, inv.getSizeInventory());
            List<Slot> sorted = slots.stream()
                .filter(slot -> slot.getHasStack() &&
                    slot.getStack().getItem() instanceof ItemArmor &&
                    ((ItemArmor) slot.getStack().getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER)
                .sorted(
                    Comparator
                        .comparingInt((Slot slot) -> ((ItemArmor) slot.getStack().getItem()).armorType)
                        .thenComparingDouble(slot -> Utils.rgbToHsv(Items.leather_helmet.getColor(slot.getStack()))[0])
                        .thenComparingDouble(slot -> Utils.rgbToHsv(Items.leather_helmet.getColor(slot.getStack()))[1])
                        .thenComparingDouble(slot -> Utils.rgbToHsv(Items.leather_helmet.getColor(slot.getStack()))[2])
                ).collect(Collectors.toList());
            
            AccessorGuiContainer accessor = (AccessorGuiContainer) event.gui;
            int guiLeft = accessor.getGuiLeft();
            int guiTop = accessor.getGuiTop();
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldRenderer = tessellator.getWorldRenderer();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.pushMatrix();
            GlStateManager.translate(guiLeft, guiTop, 100);
            GL11.glLineWidth(3);
            for (int i = 0; i < sorted.size(); i++)
            {
                Slot slot = sorted.get(i);
                int oldIndex = slot.getSlotIndex();
                if (i == oldIndex || !slot.getHasStack())
                    continue;
                Slot slotAtNewPos = slots.get(i);
                
                GlStateManager.color(119 / 255.0f, 0, 200 / 255.0f, 255);
                worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
                worldRenderer.pos(slotAtNewPos.xDisplayPosition + 8, slotAtNewPos.yDisplayPosition + 8, 0.0).endVertex();
                worldRenderer.pos(slot.xDisplayPosition + 8, slot.yDisplayPosition + 8, 0.0).endVertex();
                tessellator.draw();
                break;
            }
            GL11.glLineWidth(1);
            GlStateManager.popMatrix();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        }
    }
    
    public void onLeftClick(CallbackInfo ci)
    {
        if (editingAreas && getPlayer().inventory.getCurrentItem() != null &&
            getPlayer().inventory.getCurrentItem().getItem() == Items.stick)
        {
            extendAreaPoint();
            ci.cancel();
        }
    }
    
    public void onRightClick(CallbackInfo ci)
    {
        if (editingAreas && getPlayer().inventory.getCurrentItem() != null &&
            getPlayer().inventory.getCurrentItem().getItem() == Items.stick)
        {
            if (getMc().objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK &&
                getMc().objectMouseOver.getBlockPos() != null)
                areaPoints[selectedAreaPointIndex] = getMc().objectMouseOver.getBlockPos();
            else if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
                switchAreaPoints();
            else
                retractAreaPoint();
            ci.cancel();
        }
    }
    
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        if (getMc().currentScreen == null && Keyboard.getEventKeyState())
        {
            int key = Keyboard.getEventKey();
            if (key == Tweakception.keybindFastCommand.getKeyCode() &&
                !c.fastCommand.isEmpty())
            {
                sendChat("FastCommand: Executing " + c.fastCommand);
                McUtils.executeCommand("/" + c.fastCommand);
            }
        }
    }
    
    public void onItemTooltip(ItemTooltipEvent event)
    {
        final List<String> tooltip = event.toolTip;
        ItemStack stack = event.itemStack;
        
        if (c.disableTooltips)
        {
            tooltip.clear();
            return;
        }
        
        if (tooltipOverride != null)
        {
            tooltip.clear();
            tooltip.addAll(tooltipOverride);
            return;
        }
        
        if (stack == null)
            return;
        
        String id = Utils.getSkyblockItemOrPotionId(stack);
        
        if (c.tooltipDisplaySkyblockItemId)
            if (id != null && !id.isEmpty())
                tooltip.add("ID: " + id);
        
        if (c.armorColorSortingHelper &&
            stack.getItem() instanceof ItemArmor &&
            ((ItemArmor) stack.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER)
        {
            float hue = Utils.rgbToHsv(((ItemArmor) stack.getItem()).getColor(stack))[0];
            tooltip.add("Hue: " + hue * 255.0f);
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
                    StringBuilder sb = StringBuilderCache.get();
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
            "RANCHERS_BOOTS".equals(id) &&
            Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
        {
            for (int i = 0; i < tooltip.size(); i++)
            {
                if (tooltip.get(i).contains("Current Speed Cap: "))
                {
                    int t = 1;
                    tooltip.add(i + t++, "§7 Base: 4.317 m/s");
                    tooltip.add(i + t++, "§7 5 blocks 4 m/s:   93%");
                    tooltip.add(i + t++, "§7 4 blocks 5 m/s:   116%");
                    tooltip.add(i + t++, "§7 3 blocks 6.7 m/s: 155%");
                    tooltip.add(i + t++, "§7 2 blocks 10 m/s:  232%");
                    tooltip.add(i + t++, "§7 1 block  20 m/s:  464%");
                    tooltip.add(i + t++, "§7 45°      10 m/s:  328%");
                    tooltip.add(i + t, "§7 Cocoa: 120%");
                    break;
                }
            }
        }
        
        if (McUtils.getOpenedChest() != null &&
            McUtils.getOpenedChest().getName().startsWith("Auctions"))
        {
            for (int i = 0; i < tooltip.size(); i++)
            {
                if (Utils.auctionPriceMatcher.reset(tooltip.get(i)).find())
                {
                    NBTTagCompound extra = McUtils.getExtraAttributes(stack);
                    if (extra != null && petItemJsonExpMatcher.reset(extra.getString("petInfo")).find())
                    {
                        double price = Utils.parseDouble(Utils.auctionPriceMatcher.group("price"));
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
            NBTTagCompound extra = McUtils.getExtraAttributes(stack);
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
        
        if (c.displayPersonalCompactorItems &&
            id != null &&
            id.startsWith("PERSONAL_COMPACTOR_"))
        {
            addTheItems.accept("personal_compact_");
        }
        else if (c.displayPersonalDeletorItems &&
            id != null &&
            id.startsWith("PERSONAL_DELETOR_"))
        {
            addTheItems.accept("personal_deletor_");
        }
        
        if (c.buildersWandItemsTooltip &&
            id != null &&
            (id.equals("BUILDERS_WAND") || id.equals("BUILDERS_RULER")))
        {
            NBTTagCompound extra = McUtils.getExtraAttributes(stack);
            byte[] byteArray = extra.getByteArray("builder's_wand_data");
            if (byteArray.length == 0)
                byteArray = extra.getByteArray("builder's_ruler_data");
            try
            {
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(new ByteArrayInputStream(byteArray));
                NBTTagList items = nbt.getTagList("i", NbtType.COMPOUND);
                Map<String, Integer> map = new TreeMap<>();
                for (int i = 0; i < items.tagCount(); i++)
                {
                    NBTTagCompound item = items.getCompoundTagAt(i);
                    int count = item.getByte("Count");
                    NBTBase nameNbt = McUtils.getNbt(item, "tag.display.Name");
                    if (nameNbt != null)
                    {
                        map.merge(((NBTTagString) nameNbt).getString(), count, Integer::sum);
                    }
                }
                
                for (Map.Entry<String, Integer> entry : map.entrySet())
                {
                    tooltip.add(entry.getKey() + "§r: " + entry.getValue());
                }
            }
            catch (Exception ignored) { }
        }
    }
    
    public void onWorldLoad(WorldEvent.Load event)
    {
        lastWorldJoin = System.currentTimeMillis();
        lastWorldJoinTicks = getTicks();
    }
    
    public void onWorldUnload(WorldEvent.Unload event)
    {
        trevorQuestStartTicks = 0;
        trevorQuestCooldownNoticed = false;
        trevorQuestPendingStart = false;
        trevorQuestOngoing = false;
        snipeWarping = false;
        lastChunkUnloadPosition = new ChunkCoordIntPair(0, 0);
        pendingUnloadChunks.clear();
        playerLocations.clear();
        recentlyClaimedMinionHoppers.clear();
    }
    
    public void onChunkLoad(ChunkEvent.Load event)
    {
        // Inspired by https://github.com/dlee13/hold-that-chunk/
        if (c.ignoreServerChunkUnloadDistance)
        {
            Chunk chunk = event.getChunk();
            pendingUnloadChunks.remove(chunk.getChunkCoordIntPair());
            ChunkCoordIntPair current = new ChunkCoordIntPair(getPlayer().chunkCoordX, getPlayer().chunkCoordZ);
            if (McUtils.getChessboardDistance(current, lastChunkUnloadPosition) < 8 || getWorld() == null)
                return;
            lastChunkUnloadPosition = current;
            
            Iterator<ChunkCoordIntPair> iterator = pendingUnloadChunks.iterator();
            while (iterator.hasNext())
            {
                ChunkCoordIntPair pos = iterator.next();
                if (McUtils.getChessboardDistance(current, lastChunkUnloadPosition) > getMc().gameSettings.renderDistanceChunks)
                {
                    getWorld().doPreChunk(pos.chunkXPos, pos.chunkZPos, false);
                    iterator.remove();
                }
            }
        }
    }
    
    public void onPacketSoundEffect(S29PacketSoundEffect packet)
    {
        if (abiphoneRelayInMenu)
        {
            if (!(packet.getSoundName().equals("game.player.hurt") &&
                packet.getPitch() == 0.0f &&
                packet.getVolume() == 0.0f))
            {
                abiphoneRelaySoundStrings.offer(f("%s   %.3f   %.3f", packet.getSoundName(), packet.getPitch(), packet.getVolume()));
                if (abiphoneRelaySoundStrings.size() > 15)
                    abiphoneRelaySoundStrings.remove();
            }
        }
    }
    
    public void onPacketChunkUnload(S21PacketChunkData packet, CallbackInfo ci)
    {
        if (c.ignoreServerChunkUnloadDistance)
        {
            pendingUnloadChunks.add(new ChunkCoordIntPair(packet.getChunkX(), packet.getChunkZ()));
            ci.cancel();
        }
    }
    
    public void onGuiOpen(GuiOpenEvent event)
    {
        windowOpenMillis = System.currentTimeMillis();
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
                        return;
                    case "Your online status has been set to Away":
                        c.lastOnlineStatus = "away";
                        return;
                    case "Your online status has been set to Busy":
                        c.lastOnlineStatus = "busy";
                        return;
                    case "Your online status has been set to Appear Offline":
                        c.lastOnlineStatus = "offline";
                        return;
                }
            }
            if (msg.equals("REMINDER: Your Online Status is currently set to Appear Offline"))
            {
                c.lastOnlineStatus = "offline";
                return;
            }
            if (msg.startsWith("Your new API key is "))
            {
                Tweakception.apiManager.setApiKey(msg.split("Your new API key is ")[1]);
                return;
            }
            if (c.autoGuildWelcome && ((msg = McUtils.cleanColor(msg)) != null) &&
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
        String cleaned = McUtils.cleanColor(event.message.getUnformattedText());
        if (event.type == 0 || event.type == 1)
        {
            if (c.trevorHighlightAnimal &&
                cleaned.startsWith("[NPC] Trevor: You can find your "))
            {
                trevorQuestStartTicks = getTicks();
                trevorQuestCooldownNoticed = false;
                trevorQuestOngoing = true;
            }
            else if (c.trevorHighlightAnimal &&
                (msg.startsWith("Your mob died randomly, you are rewarded ") ||
                    msg.startsWith("Killing the animal rewarded you ")))
            {
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
            else if (cleaned.startsWith("Yum! You gain ") && cleaned.endsWith(" for 48 hours!"))
            {
                if (!centuryCakeTimer.isRunning())
                    centuryCakeCount = 0;
                centuryCakeCount++;
                centuryCakeTimer.start();
                if (c.centuryCakeCountInChat && centuryCakeCount < 14)
                {
                    IChatComponent comp = makeAddedByTweakceptionComponent(" §e(" + (14 - centuryCakeCount) + " left)");
                    event.message.appendSibling(comp);
                }
            }
        }
        else if (event.type == 2)
        {
            if (c.sendBitsMessage && msg.endsWith(" Bits from Cookie Buff!") && getTicks() - lastBitsMsgTicks >= 20 * 3)
            {
                lastBitsMsgTicks = getTicks();
                McUtils.sendChat(msg);
            }
            else if (c.sendSkyblockLevelExpGainMessage &&
                skyblockLevelExpGainMatcher.reset(msg).find() &&
                (getTicks() - lastSkyblockLevelExpGainTicks >= 20 * 5 ||
                    !lastSkyblockLevelExpGainMsg.equals(skyblockLevelExpGainMatcher.group(0))))
            {
                lastSkyblockLevelExpGainMsg = skyblockLevelExpGainMatcher.group(0);
                lastSkyblockLevelExpGainTicks = getTicks();
                McUtils.sendChat(lastSkyblockLevelExpGainMsg);
            }
        }
    }
    
    // endregion Events
    
    // region Misc
    
    public void printIsland()
    {
        sendChatf("Server: \"%s\", Location: \"%s\", §rArea: \"%s\"",
            currentServerType,
            currentLocationRaw,
            currentIsland == null ? "" : currentIsland.name);
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
        if (overridenIslandDetection)
            return;
        
        String prevServerType = currentServerType;
        String prevLocationRaw = currentLocationRaw;
        
        Minecraft mc = getMc();
        isInSkyblock = false;
        isInHypixel = false;
        currentIsland = null;
        currentLocationRaw = "";
        currentServerType = "";
        
        islandUpdatedThisTick = true;
        
        if (mc.isSingleplayer())
            return;
        
        String serverBrand = mc.thePlayer.getClientBrand(); // It's actually getServerBrand()
        if (mc.theWorld == null || mc.thePlayer == null || serverBrand == null ||
            !serverBrand.toLowerCase(Locale.ROOT).contains("hypixel"))
            return;
        isInHypixel = true;
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective sidebarObjective = scoreboard.getObjectiveInDisplaySlot(1);
        if (sidebarObjective == null)
            return;
        String objectiveName = McUtils.cleanColor(sidebarObjective.getDisplayName());
        if (!objectiveName.startsWith("SKYBLOCK"))
            return;
        
        isInSkyblock = true;
        
        // The server name in tab list
        for (NetworkPlayerInfo info : getMc().getNetHandler().getPlayerInfoMap())
        {
            if (info.getDisplayName() != null)
            {
                IChatComponent displayName = info.getDisplayName();
                if (displayName.getSiblings().size() >= 1)
                {
                    String name = displayName.getFormattedText();
                    if (name.startsWith("§r§b§lArea: ") || name.startsWith("§r§b§lDungeon: "))
                    {
                        String cleaned = McUtils.cleanColor(name);
                        currentServerType = cleaned.substring(cleaned.indexOf(' ') + 1);
                        currentIsland = ISLAND_NAME_TO_ISLAND_MAP.get(currentServerType);
                        if (DevSettings.printLocationChange && !prevServerType.equals(currentServerType))
                            sendChatf("Server type changed from \"%s\" to \"%s\"", prevServerType, currentServerType);
                        break;
                    }
                }
            }
        }
        
        // The location in scoreboard
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
            if (line.contains("⏣"))
            {
                currentLocationRaw = line;
                line = invalidLocationLetterMatcher.reset(McUtils.cleanColor(line)).replaceAll("").trim();
                currentLocationRawCleaned = line;
                
                if (DevSettings.printLocationChange && !prevLocationRaw.equals(currentLocationRaw))
                    sendChatf("Location changed from \"%s§r\" to \"%s§r\"", prevLocationRaw, currentLocationRaw);
                break;
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
                sendChat("Exception occurred when creating file");
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
                    sendChat("Exception occurred when opening file");
                }
            }
        }
        else
        {
            Utils.setClipboard(string);
            sendChat("Copied item " + type + " to clipboard");
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
    
    @Nullable
    public static SkyblockIsland getCurrentIsland()
    {
        return currentIsland;
    }
    
    public void updateIslandNow()
    {
        if (!islandUpdatedThisTick)
            detectSkyblock();
    }
    
    public void overrideIsland(String name)
    {
        if (name == null || name.equals("") || name.equals("disable") || name.equals("off"))
        {
            overridenIslandDetection = false;
            islandUpdatedThisTick = false;
            sendChat("Toggle island override off");
        }
        else
        {
            for (SkyblockIsland island : SkyblockIsland.values())
                if (island.name.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
                {
                    overridenIslandDetection = true;
                    isInSkyblock = true;
                    isInHypixel = true;
                    currentIsland = island;
                    currentServerType = island.name;
                    currentLocationRaw = " §7⏣ §cThe Waste⚽§cland";
                    currentLocationRawCleaned = " ⏣ The Wasteland";
                    islandUpdatedThisTick = true;
                    sendChat("Overwriting current island with " + island.name);
                    return;
                }
            sendChat("Cannot find island in implemented island list");
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
    
    public long getWindowOpenMillis()
    {
        return windowOpenMillis;
    }
    
    // Returns a new list, or null if both list sections (count) are not in sync or no list
    public List<String> getPlayerListFromTabList()
    {
        if (playerListUpdatedThisTick)
            return new ArrayList<>(playerList);
        
        List<NetworkPlayerInfo> list = TAB_LIST_ORDERING
            .sortedCopy(getMc().getNetHandler().getPlayerInfoMap());
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
                                if (Matchers.minecraftUsername
                                    .reset(playerElePart.getUnformattedText().trim())
                                    .matches())
                                {
                                    playerList.add(Matchers.minecraftUsername.group());
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
    
    public void setHideFromStrangers(boolean v)
    {
        hideFromStrangers = v;
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
    
    public AxisAlignedBB getAreaEditBlockSelection()
    {
        return new AxisAlignedBB(
            areaPoints[0].getX(),
            areaPoints[0].getY(),
            areaPoints[0].getZ(),
            areaPoints[1].getX(),
            areaPoints[1].getY(),
            areaPoints[1].getZ());
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
                sendChat("Ping = " + ping + " ms");
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
    
    public boolean isIgnoreServerChunkUnloadDistanceOn()
    {
        return c.ignoreServerChunkUnloadDistance;
    }
    
    public int getClientChunkUnloadDistance()
    {
        return c.clientChunkUnloadDistance;
    }
    
    private void trevorStartFromAbiphone()
    {
        int slot = Utils.findInHotbarById(id -> id.startsWith("ABIPHONE_"));
        if (slot != -1)
        {
            sendChat("Trevor: Starting from abiphone");
            getPlayer().inventory.currentItem = slot;
            ((AccessorMinecraft) getMc()).rightClickMouse();
            trevorQuestPendingStart = true;
            trevorQuestPendingStartStartTicks = getTicks();
        }
        else
        {
            sendChat("Trevor: Can't find abiphone in hotbar");
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
        private final List<PlayerLocation> sorted = new ArrayList<>();
        
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
            List<String> list = getContent();
            list.clear();
            sorted.clear();
            sorted.addAll(playerLocations);
            sorted.sort(null);
            long millis = System.currentTimeMillis();
            for (PlayerLocation p : sorted)
            {
                if (p.leaveMillis != 0)
                    list.add("§c" + p.name + "§f-" + p.area.shortName);
                else if (millis - p.joinMillis < 5000)
                    list.add("§a" + p.name + "§f-" + p.area.shortName);
                else
                    list.add(p.name + "-" + p.area.shortName);
            }
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
            
            List<String> list = getContent();
            list.clear();
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
            List<String> list = getContent();
            list.clear();
            
            ItemStack stack = getPlayer().getHeldItem();
            if (stack != null)
            {
                NBTTagCompound extra = McUtils.getExtraAttributes(stack);
                String uuid = Utils.getSkyblockItemUuid(stack);
                
                // Increment is global, any exp gained on the held item will be added to it,
                // overlay only shows when a champion item is held
                if (uuid != null && extra != null && extra.hasKey("champion_combat_xp"))
                {
                    if (!uuid.equals(lastItemUuid))
                    {
                        lastItemUuid = uuid;
                        lastExp = 0L;
                    }
                    
                    StringBuilder sb = StringBuilderCache.get();
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
            List<String> list = getContent();
            list.clear();
            
            String text;
            switch (c.lastOnlineStatus)
            {
                case "online":
                    if (!c.showOnlineStatusAlreadyOn)
                    {
                        setContent(list);
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
            
            list.add(text);
            setContent(list);
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
            
            List<String> list = getContent();
            list.clear();
            
            if (trevorQuestStartTicks != 0)
            {
                int elapsed = (getTicks() - trevorQuestStartTicks) * 50;
                if (trevorQuestOngoing)
                {
                    list.add("§aOngoing quest >>>");
                    list.add("Trevor quest time: " + Utils.msToMMSSm(elapsed));
                    if (trevorAnimal != null)
                    {
                        list.add("§aANIMAL DETECTED");
                        list.add("Distance: §a" +
                            Utils.roundToDigits(getPlayer().getDistanceToEntity(trevorAnimal), 1) +
                            " blocks");
                    }
                }
                else
                {
                    list.add("Trevor quest cooldown: " + Utils.msToMMSSm(Math.max(60000 - elapsed, 0)));
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
        sendChat("Copied raw location line to clipboard (" + currentLocationRaw + "§r)");
    }
    
    public void toggleDevMode()
    {
        c.devMode = !c.devMode;
        sendChat("Toggled dev mode " + c.devMode);
    }
    
    public void rightCtrlCopySet(String type)
    {
        c.rightCtrlCopyType = type;
        sendChat("RightCtrlCopy: Set to " + c.rightCtrlCopyType);
    }
    
    public void toggleHighlightShinyPigs()
    {
        c.highlightShinyPigs = !c.highlightShinyPigs;
        sendChat("HighlightShinyPigs: Toggled " + c.highlightShinyPigs);
    }
    
    public void setHighlightShinyPigsName(String name)
    {
        c.shinyPigName = name;
        if (name.equals(""))
            sendChat("HighlightShinyPigs: Removed name");
        else
            sendChat("HighlightShinyPigs: Ret name to " + name);
    }
    
    public void toggleHidePlayers()
    {
        c.hidePlayers = !c.hidePlayers;
        sendChat("HidePlayers: Toggled " + c.hidePlayers);
    }
    
    public void setHidePlayersWhitelist(String name)
    {
        name = name.toLowerCase(Locale.ROOT);
        if (name.isEmpty())
        {
            hidePlayersWhitelist.clear();
            sendChat("HidePlayers: Cleared whitelist");
        }
        else
        {
            if (hidePlayersWhitelist.contains(name))
            {
                hidePlayersWhitelist.remove(name);
                sendChat("HidePlayers: Removed " + name + " from whitelist");
            }
            else
            {
                hidePlayersWhitelist.add(name);
                sendChat("HidePlayers: Added " + name + " to whitelist");
            }
        }
    }
    
    public void toggleEnterToCloseNumberTypingSign()
    {
        c.enterToCloseNumberTypingSign = !c.enterToCloseNumberTypingSign;
        sendChat("EnterToCloseNumberTypingSign: Toggled " + c.enterToCloseNumberTypingSign);
    }
    
    public void toggleRenderInvisibleEntities()
    {
        c.renderInvisibleEntities = !c.renderInvisibleEntities;
        sendChat("RenderInvisibleEntities: Toggled " + c.renderInvisibleEntities);
    }
    
    public void toggleRenderInvisibleArmorStands()
    {
        c.renderInvisibleArmorStands = !c.renderInvisibleArmorStands;
        sendChat("RenderInvisibleArmorStands: Toggled " + c.renderInvisibleArmorStands);
    }
    
    public void setInvisibleEntityAlphaPercentage(int p)
    {
        if (p <= 0 || p > 100)
            c.invisibleEntityAlphaPercentage = 15;
        else
            c.invisibleEntityAlphaPercentage = p;
        sendChat("RenderInvisibleEntities: Set alpha percentage to " + c.invisibleEntityAlphaPercentage);
    }
    
    public void toggleSkipWorldRendering()
    {
        c.skipWorldRendering = !c.skipWorldRendering;
        sendChat("SkipWorldRendering: Toggled " + c.skipWorldRendering);
    }
    
    public void toggleBlockQuickCraft()
    {
        c.blockQuickCraft = !c.blockQuickCraft;
        sendChat("BlockQuickCraft: Toggled " + c.blockQuickCraft);
    }
    
    public void removeBlockQuickCraftWhitelist(int i)
    {
        if (i < 1)
        {
            sendChat("BlockQuickCraft: There are " + c.quickCraftWhitelist.size() + " whitelisted IDs");
            int ii = 1;
            for (String id : c.quickCraftWhitelist)
                sendChat(ii++ + ": " + id);
        }
        else
        {
            if (i > c.quickCraftWhitelist.size())
                sendChat("BlockQuickCraft: Index is out of bounds!");
            else
            {
                String id = c.quickCraftWhitelist.toArray(new String[0])[i - 1];
                c.quickCraftWhitelist.remove(id);
                sendChat("BlockQuickCraft: Removed " + id);
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
        
        sendChat("There are " + entities.size() + " players in the " + areaName + " area");
        for (int i = 0; i < entities.size(); i++)
            sendChat((i + 1) + ": " + entities.get(i).getDisplayName());
    }
    
    public void toggleAreaEdit()
    {
        editingAreas = !editingAreas;
        sendChat("AreaEdit: Toggled " + editingAreas);
        if (editingAreas)
        {
            sendChat("AreaEdit: Tool = stick");
            sendChat("AreaEdit: Left click to extend point");
            sendChat("AreaEdit: Right click to retract point");
            sendChat("AreaEdit: Right click block to set point");
            sendChat("AreaEdit: Ctrl right click to switch points");
            areaPoints = new BlockPos[2];
            areaPoints[0] = getPlayer().getPosition();
            areaPoints[1] = getPlayer().getPosition().add(1, 1, 1);
            selectedAreaPointIndex = 0;
        }
        else
        {
            sendChatf("AreaEdit: Last area: (%d,%d,%d),(%d,%d,%d)",
                areaPoints[0].getX(), areaPoints[0].getY(), areaPoints[0].getZ(),
                areaPoints[1].getX(), areaPoints[1].getY(), areaPoints[1].getZ());
            areaPoints = null;
        }
    }
    
    public void resetArea()
    {
        if (editingAreas)
        {
            sendChatf("AreaEdit: Reset area, last: (%d,%d,%d),(%d,%d,%d)",
                areaPoints[0].getX(), areaPoints[0].getY(), areaPoints[0].getZ(),
                areaPoints[1].getX(), areaPoints[1].getY(), areaPoints[1].getZ());
            areaPoints = new BlockPos[2];
            areaPoints[0] = getPlayer().getPosition();
            areaPoints[1] = getPlayer().getPosition().add(1, 1, 1);
        }
        else
            sendChat("AreaEdit: Feature is off");
    }
    
    public void setAreaPoint(int i, int x, int y, int z)
    {
        if (editingAreas)
        {
            if (i == 0)
            {
                areaPoints[0] = new BlockPos(x, y, z);
                sendChatf("AreaEdit: Set point 1 to %d, %d, %d", x, y, z);
            }
            else
            {
                areaPoints[1] = new BlockPos(x, y, z);
                sendChatf("AreaEdit: Set point 2 to %d, %d, %d", x, y, z);
            }
        }
        else
            sendChat("AreaEdit: Feature is off");
    }
    
    public void printArea()
    {
        if (editingAreas)
        {
            sendChatf("AreaEdit: Current area: (%d,%d,%d),(%d,%d,%d)",
                areaPoints[0].getX(), areaPoints[0].getY(), areaPoints[0].getZ(),
                areaPoints[1].getX(), areaPoints[1].getY(), areaPoints[1].getZ());
            sendChat("AreaEdit: Copied to clipboard");
            Utils.setClipboard(f("%d, %d, %d, %d, %d, %d",
                areaPoints[0].getX(), areaPoints[0].getY(), areaPoints[0].getZ(),
                areaPoints[1].getX(), areaPoints[1].getY(), areaPoints[1].getZ()));
        }
        else
            sendChat("AreaEdit: Feature is off");
    }
    
    public void toggleDrawSelectedEntityOutline()
    {
        c.drawSelectedEntityOutline = !c.drawSelectedEntityOutline;
        sendChat("DrawSelectedEntityOutline: Toggled " + c.drawSelectedEntityOutline);
    }
    
    public void setSelectedEntityOutlineWidth(float w)
    {
        c.selectedEntityOutlineWidth = w > 0.0f ? w : new GlobalTweaksConfig().selectedEntityOutlineWidth;
        sendChat("DrawSelectedEntityOutline: Set width to " + c.selectedEntityOutlineWidth);
    }
    
    public void setSelectedEntityOutlineColor(int r, int g, int b, int a)
    {
        c.selectedEntityOutlineColor = r < 0 ? new GlobalTweaksConfig().selectedEntityOutlineColor
            : Utils.makeColorArray(r, g, b, a);
        sendChat("DrawSelectedEntityOutline: Set color to " + Arrays.toString(c.selectedEntityOutlineColor));
    }
    
    public void togglePlayersInAreasDisplay()
    {
        c.displayPlayersInArea = !c.displayPlayersInArea;
        sendChat("PlayersInAreasDisplay: Toggled " + c.displayPlayersInArea);
        if (c.displayPlayersInArea)
            Tweakception.overlayManager.enable(PlayersInAreasDisplayOverlay.NAME);
        else
        {
            playerLocations.clear();
            Tweakception.overlayManager.disable(PlayersInAreasDisplayOverlay.NAME);
        }
    }
    
    public void pingServer()
    {
        if (c.enablePingOverlay)
            sendChat("You have overlay on!");
        else
        {
            if (pingNanos != 0L)
                sendChat("Still pinging");
            else
            {
                pingingFromCommand = true;
                pingSend();
                sendChat("Pinging");
            }
        }
    }
    
    public void pingOverlay()
    {
        c.enablePingOverlay = !c.enablePingOverlay;
        Tweakception.overlayManager.setEnable(PingOverlay.NAME, c.enablePingOverlay);
        sendChat("Toggled ping overlay " + c.enablePingOverlay);
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
        sendChat("ChampionOverlay: Toggled " + c.enableChampionOverlay);
    }
    
    public void setChampionOverlayIncrementResetDuration(int d)
    {
        c.championExpIncrementResetDuration =
            d > 0 ? d : new GlobalTweaksConfig().championExpIncrementResetDuration;
        sendChat("ChampionOverlay: Set increment reset time to " + c.championExpIncrementResetDuration);
    }
    
    public void toggleDisableTooltips()
    {
        c.disableTooltips = !c.disableTooltips;
        sendChat("DisableTooltips: Toggled " + c.disableTooltips);
    }
    
    public void toggleRenderEnchantedBooksType()
    {
        c.renderEnchantedBooksType = !c.renderEnchantedBooksType;
        sendChat("RenderEnchantedBooksType: Toggled " + c.renderEnchantedBooksType);
    }
    
    public void toggleRenderSacksType()
    {
        c.renderSacksType = !c.renderSacksType;
        sendChat("RenderSacksType: Toggled " + c.renderSacksType);
    }
    
    public void toggleRenderPotionTier()
    {
        c.renderPotionTier = !c.renderPotionTier;
        sendChat("RenderPotionTier: Toggled " + c.renderPotionTier);
    }
    
    public void toggleMinionAutoClaim()
    {
        minionAutoClaim = !minionAutoClaim;
        sendChat("MinionAutoClaim: Toggled " + minionAutoClaim);
    }
    
    public void addMinionAutoClaimWhitelist(String id)
    {
        if (id.isEmpty())
            sendChat("MinionAutoClaim: Give id ");
        else
        {
            id = id.toUpperCase();
            c.minionAutoClaimWhitelist.add(id);
            sendChat("MinionAutoClaim: Added " + id);
        }
    }
    
    public void removeMinionAutoClaimWhitelist(int i)
    {
        if (i < 1)
        {
            sendChat("MinionAutoClaim: There are " + c.minionAutoClaimWhitelist.size() + " whitelisted IDs");
            int ii = 1;
            for (String id : c.minionAutoClaimWhitelist)
                sendChat(ii++ + ": " + id);
        }
        else
        {
            if (i > c.minionAutoClaimWhitelist.size())
                sendChat("MinionAutoClaim: Index is out of bounds!");
            else
            {
                String id = c.minionAutoClaimWhitelist.toArray(new String[0])[i - 1];
                c.minionAutoClaimWhitelist.remove(id);
                sendChat("MinionAutoClaim: Removed " + id);
            }
        }
    }
    
    public void setMinionAutoClaimClickDelayMin(int i)
    {
        i = Utils.clamp(i, 1, 20);
        c.minionAutoclaimDelayTicksMin = i;
        sendChat("MinionAutoClaim: Set min delay ticks to " + i);
    }
    
    public void toggleTooltipDisplayId()
    {
        c.tooltipDisplaySkyblockItemId = !c.tooltipDisplaySkyblockItemId;
        sendChat("TooltipDisplayItemId: Toggled " + c.tooltipDisplaySkyblockItemId);
    }
    
    public void toggleHighlightPlayers()
    {
        highlightPlayers = !highlightPlayers;
        sendChat("HighlightPlayers: Toggled " + highlightPlayers);
    }
    
    public void setPlayerToHighlight(String name)
    {
        if (name.equals(""))
        {
            playersToHighlight.clear();
            sendChat("HighlightPlayer: Cleared list");
        }
        else
        {
            name = name.toLowerCase(Locale.ROOT);
            if (playersToHighlight.contains(name))
            {
                playersToHighlight.remove(name);
                sendChat("HighlightPlayer: Removed " + name);
            }
            else
            {
                playersToHighlight.add(name);
                sendChat("HighlightPlayer: Added " + name);
            }
        }
    }
    
    public void setArmorStandToHighlight(String name)
    {
        if (name.equals(""))
        {
            armorStandsToHighlight.clear();
            sendChat("HighlightArmorStand: Cleared list");
        }
        else
        {
            name = name.toLowerCase(Locale.ROOT);
            if (armorStandsToHighlight.contains(name))
            {
                armorStandsToHighlight.remove(name);
                sendChat("HighlightArmorStand: Removed " + name);
            }
            else
            {
                armorStandsToHighlight.add(name);
                sendChat("HighlightArmorStand: Added " + name);
            }
        }
    }
    
    public void toggleOnlineStatusOverlay()
    {
        c.enableOnlineStatusOverlay = !c.enableOnlineStatusOverlay;
        Tweakception.overlayManager.setEnable(OnlineStatusOverlay.NAME, c.enableOnlineStatusOverlay);
        sendChat("OnlineStatusOverlay: Toggled " + c.enableOnlineStatusOverlay);
    }
    
    public void toggleOnlineStatusOverlayShowAlreadyOn()
    {
        c.showOnlineStatusAlreadyOn = !c.showOnlineStatusAlreadyOn;
        sendChat("OnlineStatusOverlay: Toggled show already on " + c.showOnlineStatusAlreadyOn);
    }
    
    public void toggleTrevorAnimalHighlight()
    {
        c.trevorHighlightAnimal = !c.trevorHighlightAnimal;
        sendChat("Trevor: Toggled highlight " + c.trevorHighlightAnimal);
        trevorQuestStartTicks = 0;
        Tweakception.overlayManager.setEnable(TrevorOverlay.NAME,
            c.trevorHighlightAnimal || c.trevorQuestAutoAccept || c.trevorQuestAutoStart);
    }
    
    public void toggleTrevorQuestAutoAccept()
    {
        c.trevorQuestAutoAccept = !c.trevorQuestAutoAccept;
        sendChat("Trevor: Toggled auto accept " + c.trevorQuestAutoAccept);
        Tweakception.overlayManager.setEnable(TrevorOverlay.NAME,
            c.trevorHighlightAnimal || c.trevorQuestAutoAccept || c.trevorQuestAutoStart);
    }
    
    public void toggleTrevorQuestAutoStart()
    {
        c.trevorQuestAutoStart = !c.trevorQuestAutoStart;
        sendChat("Trevor: Toggled auto start and auto accept " + c.trevorQuestAutoStart);
        c.trevorQuestAutoAccept = c.trevorQuestAutoStart;
        Tweakception.overlayManager.setEnable(TrevorOverlay.NAME,
            c.trevorHighlightAnimal || c.trevorQuestAutoAccept);
    }
    
    public void toggleHighlightSkulls()
    {
        highlightSkulls = !highlightSkulls;
        sendChat("HighlightSkulls: Toggled " + highlightSkulls);
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
        sendChat("SendBitsMessage: Toggled " + c.sendBitsMessage);
    }
    
    public void toggleDisableArmorStandTargeting()
    {
        c.targetingDisableArmorStand = !c.targetingDisableArmorStand;
        sendChat("DisableArmorStandTargeting: Toggled " + c.targetingDisableArmorStand);
    }
    
    public void toggleDisableBatTargeting()
    {
        c.targetingDisableBat = !c.targetingDisableBat;
        sendChat("DisableBatTargeting: Toggled " + c.targetingDisableBat);
    }
    
    public void toggleDisableDeadMobTargeting()
    {
        c.targetingDisableDeadMob = !c.targetingDisableDeadMob;
        sendChat("DisableDeadMobTargeting: Toggled " + c.targetingDisableDeadMob);
    }
    
    public void toggleDisablePlayerTargeting()
    {
        c.targetingDisablePlayer = !c.targetingDisablePlayer;
        sendChat("DisablePlayerTargeting: Toggled " + c.targetingDisablePlayer);
    }
    
    public void resetTargeting()
    {
        GlobalTweaksConfig newConfig = new GlobalTweaksConfig();
        c.targetingDisableArmorStand = newConfig.targetingDisableArmorStand;
        c.targetingDisableBat = newConfig.targetingDisableBat;
        c.targetingDisableDeadMob = newConfig.targetingDisableDeadMob;
        c.targetingDisablePlayer = newConfig.targetingDisablePlayer;
        sendChat("Reset all targeting options");
    }
    
    public void toggleAfkMode()
    {
        c.afkMode = !c.afkMode;
        sendChat("AfkMode: Toggled " + c.afkMode);
    }
    
    public void setAfkFpsLimit(int i)
    {
        if (i == 0)
            i = new GlobalTweaksConfig().afkFpsLimit;
        c.afkFpsLimit = Utils.clamp(i, 5, 120);
        sendChat("AfkMode: Set fps limit to " + c.afkFpsLimit);
    }
    
    public void toggleAfkOnlyUnfocused()
    {
        c.afkOnlyUnfocused = !c.afkOnlyUnfocused;
        sendChat("AfkMode: Toggled only when unfocused " + c.afkOnlyUnfocused);
    }
    
    public void toggleAfkSkipWorldRendering()
    {
        c.afkSkipWorldRendering = !c.afkSkipWorldRendering;
        sendChat("AfkMode: Toggled skip world rendering " + c.afkSkipWorldRendering);
    }
    
    public void toggleAfkAutoRightClick()
    {
        c.afkAutoRightClick = !c.afkAutoRightClick;
        sendChat("AfkMode: Toggled auto right click every minute " + c.afkAutoRightClick);
    }
    
    public void toggleFakePowerScrolls()
    {
        fakePowerScrolls = !fakePowerScrolls;
        sendChat("FakePowerScrolls: Toggled " + fakePowerScrolls);
    }
    
    public void toggleFakeStars()
    {
        fakeStars = !fakeStars;
        sendChat("FakeStars: Toggled " + fakeStars);
    }
    
    public void setFakeStarsRed(int i)
    {
        fakeStarsRed = Utils.clamp(i, 0, 5);
        sendChat("FakeStars: Set red count to " + fakeStarsRed);
    }
    
    public void setFakeStarsPurple(int i)
    {
        fakeStarsPurple = Utils.clamp(i, 0, 5);
        sendChat("FakeStars: Set purple count to " + fakeStarsPurple);
    }
    
    public void setFakeStarsAqua(int i)
    {
        fakeStarsAqua = Utils.clamp(i, 0, 5);
        sendChat("FakeStars: Set aqua count to " + fakeStarsAqua);
    }
    
    public void toggleSendSkyblockLevelExpGainMessage()
    {
        c.sendSkyblockLevelExpGainMessage = !c.sendSkyblockLevelExpGainMessage;
        sendChat("SendSkyblockLevelExpGainMessage: Toggled " + c.sendSkyblockLevelExpGainMessage);
    }
    
    public void startSnipe(String name, String warpCmd)
    {
        if (!snipePlayerName.isEmpty())
        {
            sendChat("Snipe: Snipe already running");
            return;
        }
        
        name = name.toLowerCase(Locale.ROOT).trim();
        warpCmd = warpCmd.toLowerCase(Locale.ROOT).trim();
        if (name.isEmpty() || warpCmd.isEmpty())
        {
            sendChat("Snipe: Snipe params empty");
            return;
        }
        snipeStart(name, warpCmd);
        sendChat("Snipe: Starting sniping of player: " + snipePlayerName + ", warp cmd: " + snipeWarpCmd);
    }
    
    public void stopSnipe()
    {
        if (snipePlayerName.isEmpty())
        {
            sendChat("Snipe: Snipe not active");
            return;
        }
        snipeStop();
        sendChat("Snipe: Stopping");
    }
    
    public void setSnipeWarpDelay(int delay)
    {
        c.snipeWarpDelayTicks = Utils.clamp(delay, 0, 20 * 10);
        sendChat("Snipe: Set minimum warp delay to " + c.snipeWarpDelayTicks);
    }
    
    public void toggleAbiphoneRelayHint()
    {
        abiphoneRelayHint = !abiphoneRelayHint;
        sendChat("AbiphoneRelayHint: Toggled " + abiphoneRelayHint);
    }
    
    public void toggleHideFromStrangers()
    {
        hideFromStrangers = !hideFromStrangers;
        sendChat("HideFromStrangers: Toggled " + hideFromStrangers);
    }
    
    public void setHideFromStrangersWhitelist(String name)
    {
        if (name == null || name.isEmpty())
        {
            sendChat("HideFromStrangers: Printing list");
            int i = 0;
            for (String n : c.strangerWhitelist)
            {
                sendChat(++i + ": " + n);
            }
            return;
        }
        
        name = name.toLowerCase(Locale.ROOT);
        if (c.strangerWhitelist.contains(name))
        {
            c.strangerWhitelist.remove(name);
            sendChat("HideFromStrangers: Removed " + name + " from whitelist");
        }
        else
        {
            c.strangerWhitelist.add(name);
            sendChat("HideFromStrangers: Added " + name + " to whitelist");
        }
    }
    
    public void toggleRanchersBootsTooltipSpeedNote()
    {
        c.ranchersBootsTooltipSpeedNote = !c.ranchersBootsTooltipSpeedNote;
        sendChat("RanchersBootsTooltipSpeedNote: Toggled " + c.ranchersBootsTooltipSpeedNote);
    }
    
    public void toggleDisplayPersonalCompactorItems()
    {
        c.displayPersonalCompactorItems = !c.displayPersonalCompactorItems;
        sendChat("DisplayPersonalCompactorItems: Toggled " + c.displayPersonalCompactorItems);
    }
    
    public void toggleDisplayPersonalDeletorItems()
    {
        c.displayPersonalDeletorItems = !c.displayPersonalDeletorItems;
        sendChat("DisplayPersonalDeletorItems: Toggled " + c.displayPersonalDeletorItems);
    }
    
    public void toggleChatLogForceFormatted()
    {
        c.chatLogForceFormatted = !c.chatLogForceFormatted;
        sendChat("ChatLogForceFormatted: Toggled " + c.chatLogForceFormatted);
    }
    
    public void toggleDojoDisciplineHelper()
    {
        dojoDisciplineHelper = !dojoDisciplineHelper;
        sendChat("DojoDisciplineHelper: Toggled " + dojoDisciplineHelper);
    }
    
    public void toggleAutoGuildWelcome()
    {
        c.autoGuildWelcome = !c.autoGuildWelcome;
        sendChat("AutoGuildWelcome: Toggled " + c.autoGuildWelcome);
    }
    
    public void toggleAutoHarp()
    {
        c.autoHarp = !c.autoHarp;
        sendChat("AutoHarp: Toggled " + c.autoHarp);
        sendChat("AutoHarp: Remember to set your delay using setClickDelayTicks <ticks>");
    }
    
    public void setAutoHarpClickDelayTicks(int ticks)
    {
        c.autoHarpClickDelayTicks = ticks < 0 ? new GlobalTweaksConfig().autoHarpClickDelayTicks : Utils.clamp(ticks, 0, 30);
        sendChat("AutoHarp: Set click delay (time to wait after the the note appears on the 4th row) to " + c.autoHarpClickDelayTicks + " ticks");
    }
    
    public void toggleAutoHarpAutoClose()
    {
        c.autoHarpAutoClose = !c.autoHarpAutoClose;
        sendChat("AutoHarp: Toggled auto close on non perfect " + c.autoHarpAutoClose);
    }
    
    public void toggleAutoHarpReplayMode()
    {
        c.autoHarpReplayMode = !c.autoHarpReplayMode;
        sendChat("AutoHarp: Toggled replay mode " + c.autoHarpReplayMode);
    }
    
    public void toggleBuildersWandItemsTooltip()
    {
        c.buildersWandItemsTooltip = !c.buildersWandItemsTooltip;
        sendChat("BuildersWandItemsTooltip: Toggled " + c.buildersWandItemsTooltip);
    }
    
    public void toggleIgnoreServerRenderDistance()
    {
        c.ignoreServerChunkUnloadDistance = !c.ignoreServerChunkUnloadDistance;
        sendChat("IgnoreServerChunkUnloadDistance: Toggled " + c.ignoreServerChunkUnloadDistance);
        if (!c.ignoreServerChunkUnloadDistance)
        {
            lastChunkUnloadPosition = new ChunkCoordIntPair(0, 0);
            pendingUnloadChunks.clear();
        }
    }
    
    public void setTooltipOverride(boolean off)
    {
        String clip = Utils.getClipboard();
        if (clip == null || clip.isEmpty() || off)
        {
            tooltipOverride = null;
            sendChat("TooltipOverride: Toggled off");
        }
        else
        {
            tooltipOverride = Arrays.asList(clip.split("\\R"));
            sendChat("TooltipOverride: Overriding with " + tooltipOverride.size() + " lines");
        }
    }
    
    public void highlightBlocks(BlockPos[] poses)
    {
        int count = 0;
        for (BlockPos pos : poses)
            if (blocksToHighlight.add(pos))
                count++;
        sendChatf("HighlightBlock: Added %d new blocks", count);
    }
    
    public void highlightBlock(int x, int y, int z)
    {
        BlockPos pos = new BlockPos(x, y, z);
        if (blocksToHighlight.contains(pos))
        {
            blocksToHighlight.remove(pos);
            sendChatf("HighlightBlock: Removed block %d, %d, %d", x, y, z);
        }
        else
        {
            blocksToHighlight.add(pos);
            sendChatf("HighlightBlock: Added block %d, %d, %d", x, y, z);
        }
    }
    
    public void highlightBlockRemoveNearest()
    {
        double nearestDist = Double.MAX_VALUE;
        BlockPos nearest = null;
        for (BlockPos pos : blocksToHighlight)
        {
            double dist = getPlayer().getDistanceSqToCenter(pos);
            if (dist < nearestDist)
            {
                nearestDist = dist;
                nearest = pos;
            }
        }
        if (nearest != null && nearestDist <= 25.0)
        {
            blocksToHighlight.remove(nearest);
            sendChatf("HighlightBlock: Removed nearest %d, %d, %d", nearest.getX(), nearest.getY(), nearest.getZ());
        }
        else
        {
            sendChat("HighlightBlock: Nearest not found");
        }
    }
    
    public void highlightBlockClear()
    {
        blocksToHighlight.clear();
        sendChat("HighlightBlock: Cleared list");
    }
    
    public void highlightBlockList()
    {
        int i = 0;
        for (BlockPos pos : blocksToHighlight)
            sendChatf("HighlightBlock: %d: %d, %d, %d", ++i, pos.getX(), pos.getY(), pos.getZ());
    }
    
    public void highlightBlockCopy()
    {
        if (blocksToHighlight.isEmpty())
        {
            sendChat("HighlightBlock: List is empty");
            return;
        }
        StringBuilder sb = new StringBuilder();
        String nl = "";
        for (BlockPos pos : blocksToHighlight)
        {
            sb.append(f("%s%d, %d, %d", nl, pos.getX(), pos.getY(), pos.getZ()));
            nl = System.lineSeparator();
        }
        Utils.setClipboard(sb.toString());
        sendChatf("HighlightBlock: Copied %d poses", blocksToHighlight.size());
    }
    
    public void highlightBlockPaste()
    {
        String clip = Utils.getClipboard();
        if (clip == null)
        {
            sendChat("HighlightBlock: Clipboard is empty");
            return;
        }
        // -1,  23 ,4
        Matcher matcher = Pattern.compile("^\\s*(-?\\d+)[,\\s]*(-?\\d+)[,\\s]*(-?\\d+)[,\\s]*$").matcher("");
        int count = 0;
        for (String line : Utils.newlinePattern.split(clip))
        {
            if (matcher.reset(line).matches())
            {
                blocksToHighlight.add(new BlockPos(
                    Utils.parseInt(matcher.group(1)),
                    Utils.parseInt(matcher.group(2)),
                    Utils.parseInt(matcher.group(3))
                ));
                count++;
            }
            else if (!line.isEmpty())
                sendChatf("HighlightBlock: Invalid line: \"%s\"", line);
        }
        sendChatf("HighlightBlock: Added %d poses", count);
    }
    
    public void highlightEntityType(String type)
    {
        type = type.toLowerCase(Locale.ROOT);
        if (type.isEmpty())
        {
            highlightEntityTypeClear();
        }
        else if (entityTypesToHighlight.contains(type))
        {
            entityTypesToHighlight.remove(type);
            sendChat("HighlightEntityType: Removed " + type);
        }
        else
        {
            entityTypesToHighlight.add(type);
            sendChat("HighlightEntityType: Added " + type);
        }
    }
    
    public void highlightEntityTypeClear()
    {
        entityTypesToHighlight.clear();
        sendChat("HighlightEntityType: Cleared list");
    }
    
    public void highlightEntityTypeList()
    {
        int i = 0;
        for (String s : entityTypesToHighlight)
            sendChatf("HighlightEntityType: %d: %s", ++i, s);
    }
    
    public void toggleArmorColorSortingHelper()
    {
        c.armorColorSortingHelper = !c.armorColorSortingHelper;
        sendChat("ArmorColorSortingHelper: Toggled " + c.armorColorSortingHelper);
    }
    
    public void toggleHideMinionStorageFull()
    {
        c.hideMinionStorageFull = !c.hideMinionStorageFull;
        sendChat("HideMinionStorageFull: Toggled " + c.hideMinionStorageFull);
    }
    
    public void setFastCommand(String s)
    {
        if (s.isEmpty())
        {
            c.fastCommand = "";
            sendChat("FastCommand: Reset command");
        }
        else
        {
            while (s.startsWith("/"))
                s = s.substring(1);
            c.fastCommand = s;
            sendChat("FastCommand: Set command to " + c.fastCommand);
        }
    }
    
    public void toggleAutoConsumeBoosterCookie()
    {
        c.autoConsumeBoosterCookie = !c.autoConsumeBoosterCookie;
        sendChat("AutoConsumeBoosterCookie: Toggled " + c.autoConsumeBoosterCookie);
    }
    
    public void toggleMinionAutoClaimHopper()
    {
        c.minionAutoClaimHopper = !c.minionAutoClaimHopper;
        sendChat("MinionAutoClaimHopper: Toggled " + c.minionAutoClaimHopper);
    }
    
    public void toggleCenturyCakeCountInChat()
    {
        c.centuryCakeCountInChat = !c.centuryCakeCountInChat;
        sendChat("CenturyCakeCountInChat: Toggled " + c.centuryCakeCountInChat);
    }
    
    // endregion Commands
}
