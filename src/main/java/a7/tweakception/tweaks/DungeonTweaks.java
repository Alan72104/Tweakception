package a7.tweakception.tweaks;

import a7.tweakception.DevSettings;
import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.overlay.Anchor;
import a7.tweakception.overlay.TextOverlay;
import a7.tweakception.utils.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static a7.tweakception.tweaks.GlobalTweaks.*;
import static a7.tweakception.utils.McUtils.*;
import static a7.tweakception.utils.Utils.f;
import static a7.tweakception.utils.Utils.removeWhile;

public class DungeonTweaks extends Tweak
{
    public static class DungeonTweaksConfig
    {
        public Map<String, Integer> fragDrops = FRAGS_AND_NAMES.keySet().stream().collect(Collectors.toMap(e -> e, e -> 0));
        public Map<String, Integer> salvagedEssences = ESSENCES.stream().collect(Collectors.toMap(e -> e, e -> 0));
        public int salvagedItemCount = 0;
        public boolean trackSalvage = true;
        public String fragBot = "";
        public boolean fragAutoReparty = true;
        public TreeMap<String, String> partyFinderPlayerBlacklist = new TreeMap<>();
        public TreeSet<String> autoJoinPartyOwners = new TreeSet<>(Collections.singletonList("alan72104"));
        public TreeSet<String> blockRightClickItemNames = new TreeSet<>();
        public boolean autoCloseSecretChest = false;
        public boolean autoJoinParty = false;
        public boolean autoJoinPartyWhitelistEnable = true;
        public boolean blockOpheliaShopClicks = true;
        public boolean displayTargetMobNameTag = false;
        public boolean enableNoFog = false;
        public boolean enableNoFogAutoToggle = false;
        public boolean gyroWandOverlay = false;
        public boolean hideDamageTags = false;
        public boolean hideNonStarredMobsName = true;
        public boolean highlightBats = true;
        public boolean highlightDoorKeys = true;
        public boolean highlightShadowAssassins = true;
        public boolean highlightSpiritBear = true;
        public boolean highlightStarredMobs = false;
        public boolean partyFinderDisplayQuickPlayerInfo = false;
        public boolean partyFinderQuickPlayerInfoShowSecretPerExp = false;
        public boolean partyFinderRefreshCooldown = true;
        public boolean trackDamageTags = false;
        public boolean trackDamageHistory = false;
        public boolean trackMaskUsage = true;
        public boolean trackNonCritDamageTags = false;
        public boolean trackShootingSpeed = false;
        public boolean trackWitherDamageTags = false;
        public boolean autoSwapSpiritSceptreAote = false;
        public boolean autoSwapHyperionAote = false;
        public int damageTagHistoryTimeoutTicks = 20 * 30;
        public int damageTagTrackingCount = 10;
        public int damageHistoryOverlayMaxLines = 15;
        public int shootingSpeedTrackingRange = 4;
        public int shootingSpeedTrackingSampleSecs = 2;
        public int totalFragruns = 0;
        public long fastestBloodRush = 0L;
        public long fastestFragrun = 0L;
        public boolean pickaxeMiddleClickRemoveBlock = false;
        public boolean pickaxeMiddleClickRemoveLine = false;
        public boolean blockFlowerPlacement = false;
    }
    
    private final DungeonTweaksConfig c;
    private static final Set<String> SECRET_CHEST_ITEMS = new HashSet<>();
    private static final Set<String> TRASH_ITEMS = new HashSet<>();
    private static final Set<String> ESSENCES = Utils.hashSet("wither", "spider", "undead", "dragon",
        "gold", "diamond", "ice", "crimson");
    private static final Map<String, String> FRAGS_AND_NAMES = new HashMap<>();
    private static final Set<String> DUNGEON_FLOOR_HEADS = new HashSet<>();
    private static final Set<String> MASKS = Utils.hashSet("BONZO_MASK", "STARRED_BONZO_MASK",
        "SPIRIT_MASK");
    private static boolean isDamageFormattingExceptionNotified = false;
    private boolean wasNoFogAutoToggled = false;
    private boolean isInF5Bossfight = false;
    private String realLividName;
    private Entity realLivid;
    private final Set<String> knownLivids = new HashSet<>();
    private boolean lividFound = false;
    private final List<Entity> bats = new LinkedList<>();
    private final List<Entity> shadowAssassins = new LinkedList<>();
    private final LinkedList<Pair<Integer, String>> damageTags = new LinkedList<>();
    private final LinkedList<Pair<Integer, Entity>> armorStandsTemp = new LinkedList<>();
    private final WeakHashMap<String, Boolean> armorStandNameFilterCache = new WeakHashMap<>();
    private final Set<Entity> starredMobs = new HashSet<>();
    private final Matcher anyDamageTagMatcher = Pattern.compile(
        "^(?:§[\\da-f][✧✯]?)?((?:(?:(?:§[\\da-f])?\\d){1,3}(?:§[\\da-f])?,?)+)(?:§[\\da-f][✧✯]?)?").matcher("");
    // §f✧§f1§e3§6,§e7§66§c9§c✧§d♥
    private final Matcher critTagMatcher = Pattern.compile(
        "^§f[✧✯]((?:(?:§[\\da-f]\\d){1,3}(?:§[\\da-f],)?)+)§[\\da-f][✧✯](.*)").matcher("");
    // §712,345
    private final Matcher nonCritTagMatcher = Pattern.compile("^§7((?:\\d{1,3},?)+)(.*)").matcher("");
    // §012,345
    private final Matcher witherTagMatcher = Pattern.compile("^§0((?:\\d{1,3},?)+)$").matcher("");
    private boolean secretChestOpened = false;
    private boolean salvagingMessageArrived = false;
    private final Matcher essenceMatcher = Pattern.compile(
        "^ {2}§[\\da-f](\\w+) Essence §[\\da-f]x(\\d+)").matcher("");
    private final Matcher partyRequestMatcher = Pattern.compile(
        " (.*) has invited you to join (?:their|.*) party!").matcher("");
    private boolean fragGotten = false;
    private boolean fragRunTracking = false;
    private long fragrunStartTime = 0L; // long = millis, int = ticks
    private long fragBloodRushTime = 0L;
    private String fragBloodRushRecord = "";
    private int fragLastRunDisplayStartTime = 0;
    private String fragLastRunTime = "";
    private String fragLastRecord = "";
    private String fragLastBloodRush = "";
    private int fragSessionRuns = 0;
    private long fragSessionTotalTime = 0L;
    private long fragSessionFastestBloodRush = 0L;
    private boolean fragPendingEndRunWarp = false;
    private final Queue<Integer> arrowSpawnTimes = new ArrayDeque<>();
    private final ConcurrentMap<Entity, ConcurrentLinkedQueue<Integer>> entityHurtTimes = new ConcurrentHashMap<>();
    private Entity hitDisplayTargetNameTag = null;
    private final Map<String, MaskUsage> maskUsages = new ConcurrentHashMap<>();
    private final Matcher maskCooldownMatcher = Pattern.compile(
        "^§8Cooldown: §a(\\d+)s").matcher("");
    private final Matcher dungeonItemStatMatcher = Pattern.compile(
        " §8\\(([-+])?(\\d+(?:,\\d+)*(?:\\.\\d+)?)(%?)\\)(.*)").matcher("");
    private final Map<String, DungeonStats> uuidToDungeonStatsMap = new HashMap<>();
    private final Matcher partyFinderTitleMatcher = Pattern.compile(
        "^§o§6(?:§[\\da-f])?([^']+)'s Party(.*)").matcher("");
    private final Matcher partyFinderPlayerMatcher = Pattern.compile(
        "^§5§o (?:§[\\da-f])?([\\w\\d]+)(§f: §e\\w+§b \\(§e\\d{1,2}§b\\))").matcher("");
    private long partyFinderLastRefreshMillis = 0L;
    private final HashMap<Long, Long> damageHistoriesMap = new HashMap<>();
    private final List<Map.Entry<Long, Long>> damageHistoriesSorted = new ArrayList<>();
    private HashMap<String, Integer> prevInventoryItemCounts = new HashMap<>(); // Items are differentiated by id
    
    private static class DungeonStats
    {
        public float cata = 0.0f;
        public float cataExp = 0.0f;
        public float secretPerRun = 0.0f;
        public long totalSecret = 0L;
        public int wBlade = 0;
        public int term = 0;
        public int bestF7RunSecretsFound = 0;
        public boolean apiDiabled = false;
        
        public DungeonStats() {}
        
        public DungeonStats(float c, float ce, float spr, long ts, int wb, int t, int b, boolean ad)
        {
            cata = c;
            cataExp = ce;
            secretPerRun = spr;
            totalSecret = ts;
            wBlade = wb;
            term = t;
            bestF7RunSecretsFound = b;
            apiDiabled = ad;
        }
        
        public static final DungeonStats NOT_AVAILABLE = new DungeonStats();
    }
    
    private static class MaskUsage
    {
        public int useTicks;
        public int cooldownTicks;
        
        public MaskUsage(int u, int c)
        {
            useTicks = u;
            cooldownTicks = c;
        }
    }
    
    static
    {
        SECRET_CHEST_ITEMS.add("DEFUSE_KIT");
        SECRET_CHEST_ITEMS.add("DUNGEON_DECOY");
        SECRET_CHEST_ITEMS.add("DUNGEON_TRAP");
        SECRET_CHEST_ITEMS.add("INFLATABLE_JERRY");
        SECRET_CHEST_ITEMS.add("POTION");
        SECRET_CHEST_ITEMS.add("REVIVE_STONE");
        SECRET_CHEST_ITEMS.add("SPIRIT_LEAP");
        SECRET_CHEST_ITEMS.add("TRAINING_WEIGHTS");
        TRASH_ITEMS.add("BOUNCY_BOOTS");
        TRASH_ITEMS.add("BOUNCY_CHESTPLATE");
        TRASH_ITEMS.add("BOUNCY_HELMET");
        TRASH_ITEMS.add("BOUNCY_LEGGINGS");
        TRASH_ITEMS.add("CONJURING_SWORD");
        TRASH_ITEMS.add("CRYPT_BOW");
        TRASH_ITEMS.add("CRYPT_DREADLORD_SWORD");
        TRASH_ITEMS.add("EARTH_SHARD");
        TRASH_ITEMS.add("HEAVY_BOOTS");
        TRASH_ITEMS.add("HEAVY_CHESTPLATE");
        TRASH_ITEMS.add("HEAVY_HELMET");
        TRASH_ITEMS.add("HEAVY_LEGGINGS");
        TRASH_ITEMS.add("MACHINE_GUN_BOW");
        TRASH_ITEMS.add("ROTTEN_BOOTS");
        TRASH_ITEMS.add("ROTTEN_CHESTPLATE");
        TRASH_ITEMS.add("ROTTEN_HELMET");
        TRASH_ITEMS.add("ROTTEN_LEGGINGS");
//        TRASH_ITEMS.add("SILENT_DEATH");
        TRASH_ITEMS.add("SKELETON_GRUNT_BOOTS");
        TRASH_ITEMS.add("SKELETON_GRUNT_CHESTPLATE");
        TRASH_ITEMS.add("SKELETON_GRUNT_HELMET");
        TRASH_ITEMS.add("SKELETON_GRUNT_LEGGINGS");
        TRASH_ITEMS.add("SKELETON_LORD_BOOTS");
        TRASH_ITEMS.add("SKELETON_LORD_CHESTPLATE");
        TRASH_ITEMS.add("SKELETON_LORD_HELMET");
        TRASH_ITEMS.add("SKELETON_LORD_LEGGINGS");
        TRASH_ITEMS.add("SKELETON_MASTER_BOOTS");
        TRASH_ITEMS.add("SKELETON_MASTER_CHESTPLATE");
        TRASH_ITEMS.add("SKELETON_MASTER_HELMET");
        TRASH_ITEMS.add("SKELETON_MASTER_LEGGINGS");
        TRASH_ITEMS.add("SKELETON_SOLDIER_BOOTS");
        TRASH_ITEMS.add("SKELETON_SOLDIER_CHESTPLATE");
        TRASH_ITEMS.add("SKELETON_SOLDIER_HELMET");
        TRASH_ITEMS.add("SKELETON_SOLDIER_LEGGINGS");
        TRASH_ITEMS.add("SKELETOR_BOOTS");
        TRASH_ITEMS.add("SKELETOR_CHESTPLATE");
        TRASH_ITEMS.add("SKELETOR_HELMET");
        TRASH_ITEMS.add("SKELETOR_LEGGINGS");
        TRASH_ITEMS.add("SNIPER_BOW");
        TRASH_ITEMS.add("SNIPER_HELMET");
        TRASH_ITEMS.add("STONE_CHESTPLATE");
        TRASH_ITEMS.add("SUPER_HEAVY_BOOTS");
        TRASH_ITEMS.add("SUPER_HEAVY_CHESTPLATE");
        TRASH_ITEMS.add("SUPER_HEAVY_HELMET");
        TRASH_ITEMS.add("SUPER_HEAVY_LEGGINGS");
        TRASH_ITEMS.add("SUPER_UNDEAD_BOW");
        TRASH_ITEMS.add("UNDEAD_BOW");
        TRASH_ITEMS.add("ZOMBIE_COMMANDER_BOOTS");
        TRASH_ITEMS.add("ZOMBIE_COMMANDER_CHESTPLATE");
        TRASH_ITEMS.add("ZOMBIE_COMMANDER_HELMET");
        TRASH_ITEMS.add("ZOMBIE_COMMANDER_LEGGINGS");
        TRASH_ITEMS.add("ZOMBIE_COMMANDER_WHIP");
        TRASH_ITEMS.add("ZOMBIE_KNIGHT_BOOTS");
        TRASH_ITEMS.add("ZOMBIE_KNIGHT_CHESTPLATE");
        TRASH_ITEMS.add("ZOMBIE_KNIGHT_HELMET");
        TRASH_ITEMS.add("ZOMBIE_KNIGHT_LEGGINGS");
        TRASH_ITEMS.add("ZOMBIE_KNIGHT_SWORD");
        TRASH_ITEMS.add("ZOMBIE_LORD_BOOTS");
        TRASH_ITEMS.add("ZOMBIE_LORD_CHESTPLATE");
        TRASH_ITEMS.add("ZOMBIE_LORD_HELMET");
        TRASH_ITEMS.add("ZOMBIE_LORD_LEGGINGS");
        TRASH_ITEMS.add("ZOMBIE_SOLDIER_BOOTS");
        TRASH_ITEMS.add("ZOMBIE_SOLDIER_CHESTPLATE");
        TRASH_ITEMS.add("ZOMBIE_SOLDIER_CUTLASS");
        TRASH_ITEMS.add("ZOMBIE_SOLDIER_HELMET");
        TRASH_ITEMS.add("ZOMBIE_SOLDIER_LEGGINGS");
        TRASH_ITEMS.add("RAMPART_BOOTS");
        TRASH_ITEMS.add("RAMPART_CHESTPLATE");
        TRASH_ITEMS.add("RAMPART_HELMET");
        TRASH_ITEMS.add("RAMPART_LEGGINGS");
        TRASH_ITEMS.add("BLADE_OF_THE_VOLCANO");
        TRASH_ITEMS.add("STAFF_OF_THE_VOLCANO");
        TRASH_ITEMS.add("SWORD_OF_BAD_HEALTH");
        TRASH_ITEMS.add("FLAMING_CHESTPLATE");
        TRASH_ITEMS.add("MOOGMA_LEGGINGS");
        TRASH_ITEMS.add("SLUG_BOOTS");
        TRASH_ITEMS.add("TAURUS_HELMET");
        FRAGS_AND_NAMES.put("GIANT_FRAGMENT_DIAMOND", "Diamante's Handle");
        FRAGS_AND_NAMES.put("GIANT_FRAGMENT_LASER", "L.A.S.R.'s Eye");
        FRAGS_AND_NAMES.put("GIANT_FRAGMENT_BIGFOOT", "Bigfoot's Lasso");
        FRAGS_AND_NAMES.put("GIANT_FRAGMENT_BOULDER", "Jolly Pink Rock");
        DUNGEON_FLOOR_HEADS.add("GOLD_BONZO_HEAD");
        DUNGEON_FLOOR_HEADS.add("GOLD_SCARF_HEAD");
        DUNGEON_FLOOR_HEADS.add("GOLD_PROFESSOR_HEAD");
        DUNGEON_FLOOR_HEADS.add("GOLD_THORN_HEAD");
        DUNGEON_FLOOR_HEADS.add("GOLD_LIVID_HEAD");
        DUNGEON_FLOOR_HEADS.add("GOLD_SADAN_HEAD");
        DUNGEON_FLOOR_HEADS.add("GOLD_NECRON_HEAD");
        DUNGEON_FLOOR_HEADS.add("DIAMOND_BONZO_HEAD");
        DUNGEON_FLOOR_HEADS.add("DIAMOND_SCARF_HEAD");
        DUNGEON_FLOOR_HEADS.add("DIAMOND_PROFESSOR_HEAD");
        DUNGEON_FLOOR_HEADS.add("DIAMOND_THORN_HEAD");
        DUNGEON_FLOOR_HEADS.add("DIAMOND_LIVID_HEAD");
        DUNGEON_FLOOR_HEADS.add("DIAMOND_SADAN_HEAD");
        DUNGEON_FLOOR_HEADS.add("DIAMOND_NECRON_HEAD");
    }
    
    public DungeonTweaks(Configuration configuration)
    {
        super(configuration, "DT");
        c = configuration.config.dungeonTweaks;
        Tweakception.overlayManager.addOverlay(new FragRunOverlay());
        Tweakception.overlayManager.addOverlay(new DamageTagTrackingOverlay());
        Tweakception.overlayManager.addOverlay(new ShootingSpeedOverlay());
        Tweakception.overlayManager.addOverlay(new TargetMobNametagOverlay());
        Tweakception.overlayManager.addOverlay(new DamageHistoryOverlay());
    }
    
    // region Events
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;
        
        if (getTicks() % 20 == 0)
        {
            if (c.enableNoFogAutoToggle)
            {
                boolean f5 = getCurrentLocationRaw().contains("(F5)") || getCurrentLocationRaw().contains("(M5)");
                boolean f7 = getCurrentLocationRaw().contains("(F7)") || getCurrentLocationRaw().contains("(M7)");
                if (getCurrentIsland() == SkyblockIsland.DUNGEON && (f5 || f7))
                {
                    if (!wasNoFogAutoToggled && !c.enableNoFog)
                    {
                        c.enableNoFog = true;
                        wasNoFogAutoToggled = true;
                        sendChat("NoFog: Dungeon floor 5/7 detected, auto toggled on");
                    }
                }
                else
                {
                    if (c.enableNoFog && wasNoFogAutoToggled)
                    {
                        c.enableNoFog = false;
                        wasNoFogAutoToggled = false;
                        
                        sendChat("NoFog: Auto toggled off");
                    }
                }
            }
        }
        
        if (getTicks() % 5 == 4 &&
            fragRunTracking &&
            getCurrentIsland() == SkyblockIsland.DUNGEON &&
            !fragGotten &&
            System.currentTimeMillis() - Tweakception.globalTweaks.getWorldJoinMillis() >= 5000)
        {
            HashMap<String, Integer> curInventoryItemCounts = new HashMap<>();
            HashSet<String> allItemIds = new HashSet<>(prevInventoryItemCounts.keySet());
            for (int i = 0; i < 36; i++)
            {
                if (i == 9 - 1) // The menu slot
                    continue;
                ItemStack stack = getPlayer().inventory.getStackInSlot(i);
                String id = Utils.getSkyblockItemId(stack);
                if (id != null)
                {
                    curInventoryItemCounts.merge(id, stack.stackSize, Integer::sum);
                    allItemIds.add(id);
                }
            }
            
            if (!prevInventoryItemCounts.isEmpty())
                for (String id : allItemIds)
                {
                    if (FRAGS_AND_NAMES.containsKey(id))
                    {
                        int oldAmount = 0;
                        if (prevInventoryItemCounts.containsKey(id))
                            oldAmount = prevInventoryItemCounts.get(id);
                        int newAmount = 0;
                        if (curInventoryItemCounts.containsKey(id))
                            newAmount = curInventoryItemCounts.get(id);
                        
                        if (newAmount - oldAmount == 1)
                        {
                            fragGotten = true;
                            c.fragDrops.merge(id, 1, Integer::sum);
                            sendChatf("Frag: Obtained %s, count: %d", FRAGS_AND_NAMES.get(id), c.fragDrops.get(id));
                            break;
                        }
                    }
                }
            
            prevInventoryItemCounts = curInventoryItemCounts;
        }
        
        if (getTicks() % 2 == 1)
        {
            if (c.displayTargetMobNameTag)
            {
                Entity targetMob = null;
                float nearestDistance = Float.MAX_VALUE;
                hitDisplayTargetNameTag = null;
                
                for (Map.Entry<Entity, ConcurrentLinkedQueue<Integer>> entry : entityHurtTimes.entrySet())
                {
                    Entity entity = entry.getKey();
                    
                    if (entity.isDead)
                        entityHurtTimes.remove(entity);
                    else
                    {
                        ConcurrentLinkedQueue<Integer> queue = entry.getValue();
                        
                        removeWhile(queue, ele -> getTicks() - ele > 20 * 3);
                        
                        int size = queue.size();
                        float dis = entry.getKey().getDistanceToEntity(McUtils.getPlayer());
                        
                        if (size == 0)
                            entityHurtTimes.remove(entity);
                        else if (dis < nearestDistance)
                        {
                            nearestDistance = dis;
                            targetMob = entity;
                        }
                    }
                }
                
                if (targetMob != null)
                {
                    AxisAlignedBB aabb = targetMob.getEntityBoundingBox().expand(0.5, 4.0, 0.5);
                    List<Entity> entities = McUtils.getWorld().getEntitiesWithinAABB(EntityArmorStand.class, aabb, e -> true);
                    nearestDistance = Float.MAX_VALUE;
                    for (Entity e : entities)
                    {
                        String name = e.getName();
                        if (!e.isDead && !anyDamageTagMatcher.reset(name).matches())
                        {
                            float dis = e.getDistanceToEntity(targetMob);
                            if (dis < nearestDistance)
                            {
                                nearestDistance = dis;
                                hitDisplayTargetNameTag = e;
                            }
                        }
                    }
                }
            }
        }
        
        bats.removeIf(e -> e.isDead);
        shadowAssassins.removeIf(e -> e.isDead);
        
        if (c.highlightStarredMobs || c.trackDamageTags)
        {
            removeWhile(damageTags, ele -> getTicks() - ele.a > c.damageTagHistoryTimeoutTicks);
            starredMobs.removeIf(ele -> ele.isDead);
            
            removeWhile(armorStandsTemp, ele -> getTicks() - ele.a >= 3,
                ele ->
                {
                    Entity stand = ele.b;
                    String name = stand.getName();
                    
                    if (name.endsWith("§c❤"))
                    {
                        if (c.highlightStarredMobs)
                        {
                            boolean isStarred = name.contains("✯");
                            if (isStarred)
                            {
                                Entity nearest = McUtils.getNearestEntityInAABB(stand,
                                    stand.getEntityBoundingBox().expand(0.5, 2.5, 0.5),
                                    e -> e instanceof EntityOtherPlayerMP ||
                                        e instanceof EntitySkeleton ||
                                        e instanceof EntityZombie ||
                                        e instanceof EntityEnderman);
                                if (nearest != null)
                                {
                                    starredMobs.add(nearest);
                                    return;
                                }
                            }
                        }
                        return;
                    }
                    
                    try
                    {
                        if ((c.trackDamageTags || c.trackDamageHistory) &&
                            name.startsWith("§f") && critTagMatcher.reset(name).matches())
                        {
                            long num = Long.parseLong(McUtils.cleanColor(critTagMatcher.group(1)).replace(",", ""));
                            if (c.trackDamageTags)
                                pushDamageInfo(ele.a, name);
                            if (c.trackDamageHistory)
                                damageHistoriesMap.merge(num, 1L, Long::sum);
                        }
                        if (c.trackDamageTags && c.trackWitherDamageTags && witherTagMatcher.reset(name).matches())
                        {
                            pushDamageInfo(ele.a, name);
                        }
                        else if (c.trackDamageTags && c.trackNonCritDamageTags &&
                            nonCritTagMatcher.reset(name).matches())
                        {
                            pushDamageInfo(ele.a, name);
                        }
                    }
                    catch (Exception e)
                    {
                        if (!isDamageFormattingExceptionNotified)
                        {
                            isDamageFormattingExceptionNotified = true;
                            sendChat("TrackDamageTags: Formatting failed");
                            sendChat(e.toString());
                            e.printStackTrace();
                        }
                    }
                });
        }
        
        if (c.trackDamageHistory)
        {
            damageHistoriesSorted.clear();
            damageHistoriesSorted.addAll(damageHistoriesMap.entrySet());
            damageHistoriesSorted.sort((a, b) ->
            {
                int r = b.getValue().compareTo(a.getValue());
                if (r == 0)
                    return b.getKey().compareTo(a.getKey());
                return r;
            });
        }
        
        if (McUtils.getOpenedChest() != null)
        {
            IInventory inv = McUtils.getOpenedChest();
            if (secretChestOpened)
            {
                secretChestOpened = false;
                for (int i = 9 + 5 - 1; i < inv.getSizeInventory(); i += 9 * 3)
                {
                    ItemStack center = inv.getStackInSlot(i);
                    String id = Utils.getSkyblockItemId(center);
                    if (center != null && id != null && SECRET_CHEST_ITEMS.contains(id))
                    {
                        McUtils.getPlayer().closeScreen();
                        break;
                    }
                }
            }
        }
        else
        {
            secretChestOpened = false;
        }
        
        if (fragRunTracking)
        {
            Tweakception.globalTweaks.updateIslandNow();
            if (getCurrentIsland() != SkyblockIsland.DUNGEON)
            {
                if (fragPendingEndRunWarp || fragGotten)
                {
                    fragPendingEndRunWarp = false;
                    fragGotten = false;
                    fragEnd();
                    if (c.fragAutoReparty)
                    {
                        if (!c.fragBot.equals(""))
                        {
                            sendChat("Frag: Repartying " + c.fragBot);
                            Tweakception.scheduler
                                .addDelayed(() -> McUtils.getPlayer().sendChatMessage("/p disband"), 10)
                                .thenDelayed(() -> McUtils.getPlayer().sendChatMessage("/p " + c.fragBot), 10);
                        }
                        else
                            sendChat("Frag: Cannot reparty, please set a frag bot using `setfragbot <name>`");
                    }
                }
                else
                {
                    if (fragrunStartTime != 0L && !isInF7())
                    {
                        fragrunStartTime = 0L;
                        sendChat("Frag: Not in f7, run is cancelled!");
                    }
                }
            }
        }
        
        if (c.trackShootingSpeed)
        {
            removeWhile(arrowSpawnTimes,
                ele -> getTicks() - ele > 20 * c.shootingSpeedTrackingSampleSecs);
        }
        
        if (c.trackMaskUsage && getCurrentIsland() == SkyblockIsland.DUNGEON)
        {
            for (Map.Entry<String, MaskUsage> ele : maskUsages.entrySet())
            {
                MaskUsage usage = ele.getValue();
                
                if (getTicks() - usage.useTicks > usage.cooldownTicks + 20)
                {
                    sendChatf("TrackMaskUsage: §aYour §e%s §ais now available!",
                        String.join(" ", ele.getKey().toLowerCase(Locale.ROOT).split("_")));
                    maskUsages.remove(ele.getKey());
                }
            }
        }
    }
    
    public void onEntityUpdate(LivingEvent.LivingUpdateEvent event)
    {
        EntityLivingBase e = event.entityLiving;
        if (isInF5Bossfight && getCurrentIsland() == SkyblockIsland.DUNGEON)
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
    
    public void onRenderLast(RenderWorldLastEvent ignoredEvent)
    {
        if (getCurrentIsland() != SkyblockIsland.DUNGEON) return;
        
        if (isInF5Bossfight && lividFound)
            RenderUtils.drawDefaultHighlightBoxForEntity(realLivid, new Color(0, 255, 0, 192), false);
        
        if (c.highlightBats)
            for (Entity bat : bats)
                RenderUtils.drawDefaultHighlightBoxForEntity(bat, new Color(255, 76, 76, 85), false);
        
        if (c.highlightShadowAssassins)
            for (Entity sa : shadowAssassins)
                RenderUtils.drawDefaultHighlightBoxForEntity(sa, new Color(255, 76, 76, 85), false);
        
        if (c.highlightStarredMobs)
            for (Entity e : starredMobs)
                RenderUtils.drawDefaultHighlightBoxForEntity(e, RenderUtils.DEFAULT_HIGHLIGHT_COLOR, false);
    }
    
    public void onLivingRenderPre(RenderLivingEvent.Pre<?> event)
    {
        Entity entity = event.entity;
        
        if (entity instanceof EntityArmorStand)
        {
            String tex = McUtils.getArmorStandHeadTexture((EntityArmorStand) entity);
            if (tex != null)
            {
                String witherKeyTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzRkYjRhZGZhOWJmNDhmZjVkNDE3MDdhZTM0ZWE3OGJkMjM3MTY1OWZjZDhjZDg5MzQ3NDlhZjRjY2U5YiJ9fX0=";
                String bloodKeyTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjU2MTU5NWQ5Yzc0NTc3OTZjNzE5ZmFlNDYzYTIyMjcxY2JjMDFjZjEwODA5ZjVhNjRjY2IzZDZhZTdmOGY2In19fQ==";
                
                if (c.highlightDoorKeys && getCurrentIsland() == SkyblockIsland.DUNGEON)
                {
                    if (tex.equals(witherKeyTexture))
                        RenderUtils.drawBeaconBeamAtEntity(entity, new Color(84, 166, 102, 128), getPartialTicks());
                    else if (tex.equals(bloodKeyTexture))
                        RenderUtils.drawBeaconBeamAtEntity(entity, new Color(84, 166, 102, 128), getPartialTicks());
                }
            }
        }
    }
    
    public void onLivingRenderPost(RenderLivingEvent.Post<?> event)
    {
        if (c.highlightSpiritBear && event.entity instanceof EntityOtherPlayerMP &&
            event.entity.getName().equals("Spirit Bear"))
        {
            RenderUtils.drawDefaultHighlightBoxForEntity(event.entity, new Color(0, 255, 0, 192), false);
        }
    }
    
    public void onLivingSpecialRenderPre(RenderLivingEvent.Specials.Pre<?> event)
    {
        if ((c.hideDamageTags || c.hideNonStarredMobsName) && event.entity instanceof EntityArmorStand)
        {
            String name = event.entity.getName();
            
            Boolean bool = armorStandNameFilterCache.get(name);
            if (bool == null)
            {
                if (c.hideDamageTags && anyDamageTagMatcher.reset(name).matches())
                    bool = true;
                else if (getCurrentIsland() == SkyblockIsland.DUNGEON && c.hideNonStarredMobsName &&
                    name.endsWith("§c❤") &&
                    !name.substring(0, Math.min(name.length(), 5)).contains("✯"))
                    bool = true;
                else
                    bool = false;
                armorStandNameFilterCache.put(name, bool);
                if (DevSettings.printArmorStandNameFilterCache)
                    sendChatf("Cached \"%s§r\" as %s%s§r (%d in cache)", name, bool ? "§a" : "§c",
                        bool, armorStandNameFilterCache.size());
            }
            
            if (bool)
            {
                event.setCanceled(true);
            }
        }
    }
    
    public void onRenderBlockOverlay(DrawBlockHighlightEvent event)
    {
        if (c.gyroWandOverlay)
        {
            ItemStack stack = McUtils.getPlayer().inventory.getCurrentItem();
            if (stack != null)
            {
                String id = Utils.getSkyblockItemId(stack);
                if (id != null && id.equals("GYROKINETIC_WAND"))
                {
                    RayTraceUtils.RayTraceResult res = RayTraceUtils.rayTraceBlock(
                        McUtils.getPlayer(), event.partialTicks, 26.0f, 0.1f);
                    if (res != null)
                        RenderUtils.drawFilledBoundingBox(res.pos, new Color(0, 70, 156, 96), event.partialTicks);
                }
            }
        }
    }
    
    public void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
        Entity entity = event.entity;
        if (c.trackDamageTags ||
//            c.displaySoulName ||
            c.highlightStarredMobs && getCurrentIsland() == SkyblockIsland.DUNGEON)
        {
            // The custom name doesn't come with the first update
            // So check the name 3 ticks later
            if (entity instanceof EntityArmorStand)
            {
                armorStandsTemp.offer(new Pair<>(getTicks(), entity));
            }
        }
        
        if (c.highlightBats && getCurrentIsland() == SkyblockIsland.DUNGEON)
        {
            if (entity instanceof EntityBat)
            {
                bats.add(entity);
                return;
            }
        }
        
        if (getCurrentIsland() == SkyblockIsland.DUNGEON &&
            entity instanceof EntityOtherPlayerMP)
        {
            if (c.highlightShadowAssassins &&
                entity.getName().equals("Shadow Assassin"))
            {
                shadowAssassins.add(entity);
                return;
            }
        }
        
        if (c.trackShootingSpeed)
        {
            if (entity instanceof EntityArrow)
            {
                if (entity.getDistanceToEntity(McUtils.getPlayer()) <= c.shootingSpeedTrackingRange)
                {
                    arrowSpawnTimes.offer(getTicks());
                }
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
                ItemStack item = McUtils.getPlayer().inventory.getCurrentItem();
                if (item != null)
                {
                    String name = McUtils.cleanColor(item.getDisplayName());
                    if (c.blockRightClickItemNames.contains(name))
                    {
                        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU))
                        {
                            sendChat("BlockRightClick: Overriding block click for item (" + name + "§r)");
                        }
                        else
                        {
                            sendChat("BlockRightClick: Blocked click for item (" + name + "§r), hold alt to override it");
                            event.setCanceled(true);
                            return;
                        }
                    }
                }
            }
            
            if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK && c.blockFlowerPlacement)
            {
                String id = Utils.getSkyblockItemId(event.entityPlayer.getCurrentEquippedItem());
                if (id != null && (id.equals("BAT_WAND") || id.equals("FLOWER_OF_TRUTH")))
                {
                    Block block = event.world.getBlockState(event.pos).getBlock();
                    if (block == Blocks.dirt || block == Blocks.grass || block == Blocks.tallgrass)
                        event.setCanceled(true);
                }
            }
        }
    }
    
    public void onPacketCollectItem(S0DPacketCollectItem ignoredPacket)
    {
    }
    
    public void onPacketEntityStatus(S19PacketEntityStatus packet)
    {
        if (c.displayTargetMobNameTag && packet.getOpCode() == 2)
        {
            Entity e = packet.getEntity(McUtils.getWorld());
            if (e != null)
            {
                entityHurtTimes.computeIfAbsent(e, en -> new ConcurrentLinkedQueue<>())
                    .offer(getTicks());
            }
        }
    }
    
    public void onPacketEntityEquipment(S04PacketEntityEquipment ignoredPacket)
    {
    }
    
    public void onGuiOpen(GuiOpenEvent event)
    {
        if (event.gui instanceof GuiChest)
        {
            GuiChest chest = (GuiChest) event.gui;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            String containerName = container.getLowerChestInventory().getName();
            if (getCurrentIsland() == SkyblockIsland.DUNGEON &&
                c.autoCloseSecretChest && (containerName.equals("Chest") || containerName.equals("Large Chest"))
            )
                secretChestOpened = true;
        }
    }
    
    public void onChatReceivedGlobal(ClientChatReceivedEvent event)
    {
        if (event.type != 0) return;
    
        String msg = event.message.getUnformattedText();
        if (c.autoJoinParty &&
            msg.startsWith("-----------------------------------------------------"))
        {
            if (partyRequestMatcher.reset(msg).find())
            {
                String name = partyRequestMatcher.group(1)/*.replaceAll("[.*]", "").trim()*/.toLowerCase(Locale.ROOT);
                if (!c.autoJoinPartyWhitelistEnable || c.autoJoinPartyOwners.contains(name))
                {
                    sendChat("AutoJoinParty: Joining " + name + "'s party" +
                        (!c.autoJoinPartyWhitelistEnable ? " (whitelist disabled)" : ""));
                    getPlayer().sendChatMessage("/p " + name);
                }
            }
        }
    }
    
    public void onChatReceived(ClientChatReceivedEvent event)
    {
        if (event.type != 0) return;
        
        String msg = event.message.getUnformattedText();
        if (getCurrentIsland() == SkyblockIsland.DUNGEON && msg.startsWith("[BOSS]"))
        {
            String f5BossStart = "Welcome, you arrive right on time. I am Livid, the Master of Shadows.";
            String f5BossEnd = "Impossible! How did you figure out which one I was?";
            if (msg.contains(f5BossStart))
            {
                isInF5Bossfight = true;
            }
            else if (msg.contains(f5BossEnd))
            {
                isInF5Bossfight = false;
                resetLivid();
            }
        }
        else if (getCurrentIsland() == SkyblockIsland.DUNGEON && msg.equals("Dungeon starts in 1 second."))
        {
            Tweakception.globalTweaks.updateIslandNow();
            if (isInF7())
            {
                fragGotten = false;
                if (fragRunTracking)
                    fragStart();
            }
        }
        else if (getCurrentIsland() == SkyblockIsland.DUNGEON && msg.equals("The BLOOD DOOR has been opened!"))
        {
            if (isInF7())
            {
                if (fragRunTracking)
                    fragSetBloodRush();
            }
        }
        else if (getCurrentIsland() == SkyblockIsland.DUNGEON &&
            msg.startsWith("Your ") &&
            msg.endsWith(" saved your life!"))
        {
            ItemStack head = McUtils.getPlayer().getCurrentArmor(3);
            if (head != null)
            {
                String id = Utils.getSkyblockItemId(head);
                if (id != null && MASKS.contains(id))
                {
                    String[] lore = McUtils.getDisplayLore(head);
                    if (lore != null)
                    {
                        int cooldown = 360;
                        for (int i = lore.length - 1; i >= 0; i--)
                        {
                            if (maskCooldownMatcher.reset(lore[i]).matches())
                            {
                                cooldown = Integer.parseInt(maskCooldownMatcher.group(1));
                                break;
                            }
                        }
                        MaskUsage usage = new MaskUsage(getTicks(), cooldown * 20);
                        maskUsages.put(id, usage);
                    }
                    else
                    {
                        sendChat("TrackMaskUsage: Cannot retrieve the lore of your mask, this will not be tracked!");
                    }
                }
                else
                {
                    sendChat("TrackMaskUsage: Cannot retrieve the id of your mask (or not registered), this will not be tracked!");
                }
            }
        }
        
        detectSalvage(event);
    }
    
    private void detectSalvage(ClientChatReceivedEvent event)
    {
        if (!c.trackSalvage)
            return;
        String msg = event.message.getUnformattedText();
        // Things come in like this colored with ChatStyle
        //§r§5§lEXTRA! §r§a§lDOUBLED SALVAGED ESSENCE:
        //§r   §r§f30x §r§dUndead Essence§r§f!
        //§r§aYou salvaged §r§b9 §r§aitems for:
        //§r   §r§f120x §r§dUndead Essence§r§f!
        //§r   §r§f5x §r§dWither Essence§r§f!
        if (msg.startsWith("You salvaged ")) //You salvaged 2 items for:
        {
            Matcher p = Pattern.compile("^You salvaged (\\d+) items? for:$").matcher(msg);
            if (p.matches())
            {
                int count = Integer.parseInt(p.group(1));
                c.salvagedItemCount += count;
                IChatComponent comp = McUtils.makeAddedByTweakceptionComponent(" §a(total: §b" + c.salvagedItemCount + "§a)");
                event.message.appendSibling(comp);
                salvagingMessageArrived = true;
            }
        }
        else if (salvagingMessageArrived && msg.endsWith(" Essence!")) //   30x Undead Essence!
        {
            Matcher p = Pattern.compile("^(\\d+)x (\\w+) Essence!$").matcher(msg.trim());
            if (p.matches())
            {
                int count = Integer.parseInt(p.group(1));
                String type = p.group(2).toLowerCase(Locale.ROOT);
                if (ESSENCES.contains(type))
                {
                    int countTotal = c.salvagedEssences.merge(type, count, Integer::sum);
                    IChatComponent comp = McUtils.makeAddedByTweakceptionComponent(" §f(total: " + countTotal + ")");
                    event.message.appendSibling(comp);
                }
            }
        }
        else
            salvagingMessageArrived = false;
    }
    
    public void onItemTooltip(ItemTooltipEvent event)
    {
        if (event.itemStack == null || event.toolTip == null) return;
        
        String id = Utils.getSkyblockItemId(event.itemStack);
        Item item = event.itemStack.getItem();
        
        if (id != null && id.endsWith("HEAD") && DUNGEON_FLOOR_HEADS.contains(id))
        {
            for (int i = 0; i < event.toolTip.size(); i++)
            {
                String line = event.toolTip.get(i);
                if (line.equals("§5§o")) break;
                
                if (line.startsWith("§5§o§7") && !line.startsWith("§5§o§7Gear Score") &&
                    dungeonItemStatMatcher.reset(line).find())
                {
                    String sign = dungeonItemStatMatcher.group(1);
                    DecimalFormat format = new DecimalFormat("###,###.##");
                    String num = format.format(Float.parseFloat(dungeonItemStatMatcher.group(2).replace(",", "")) * 2.0f);
                    String percent = dungeonItemStatMatcher.group(3);
                    String extra = dungeonItemStatMatcher.group(4);
                    
                    event.toolTip.set(i, line + " §e(" + sign + num + percent + ")" + extra);
                }
            }
        }
        else if (McUtils.getOpenedChest() != null)
        {
            String containerName = McUtils.getOpenedChest().getName();
            if (containerName.equals("Party Finder"))
            {
                if (item == Items.skull)
                {
                    boolean ctrlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL);
                    
                    if (c.partyFinderPlayerBlacklist.size() > 0 &&
                        event.toolTip.size() > 0 && partyFinderTitleMatcher.reset(event.toolTip.get(0)).find())
                    {
                        String name = partyFinderTitleMatcher.group(1);
                        if (c.partyFinderPlayerBlacklist.containsKey(name.toLowerCase(Locale.ROOT)))
                        {
                            String reason = c.partyFinderPlayerBlacklist.get(name.toLowerCase(Locale.ROOT));
                            event.toolTip.set(0, "§o§6§4§m" + name + "§a's Party" + partyFinderTitleMatcher.group(2) +
                                (ctrlDown && !reason.equals("") ? " §8(" + reason + ")" : ""));
                        }
                    }
                    
                    if (c.partyFinderDisplayQuickPlayerInfo || c.partyFinderPlayerBlacklist.size() > 0)
                    {
                        final int spaceWidth = getMc().fontRendererObj.getStringWidth(" ");
                        int maxWidth = 0;
                        // Index, result, width
                        List<TriPair<Integer, MatchResult, Integer>> playerLines = new ArrayList<>(5);
                        
                        for (int i = 1; i < event.toolTip.size(); i++)
                        {
                            String line = event.toolTip.get(i);
                            if (partyFinderPlayerMatcher.reset(line).matches())
                            {
                                int width = getMc().fontRendererObj.getStringWidth(line);
                                maxWidth = Math.max(maxWidth, width);
                                playerLines.add(new TriPair<>(i, partyFinderPlayerMatcher.toMatchResult(), width));
                                if (playerLines.size() == 5)
                                    break;
                            }
                        }
                        
                        for (TriPair<Integer, MatchResult, Integer> ele : playerLines)
                        {
                            int i = ele.a;
                            String line = event.toolTip.get(i);
                            String name = ele.b.group(1);
                            String padding = Utils.stringRepeat(" ", (maxWidth - ele.c) / spaceWidth);
                            StringBuilder sb = StringBuilderCache.get();
                            boolean blacklisted = c.partyFinderPlayerBlacklist.containsKey(name.toLowerCase(Locale.ROOT));
                            
                            if (blacklisted)
                                sb.append("§5§o §4§m").append(name).append(ele.b.group(2));
                            else
                                sb.append(line);
                            
                            quickPlayerInfo:
                            if (c.partyFinderDisplayQuickPlayerInfo)
                            {
                                if (!Tweakception.apiManager.hasApiKey())
                                {
                                    sb.append(padding).append(" §fNo API key");
                                    break quickPlayerInfo;
                                }
                                
                                DungeonStats stats = getPlayerDungeonStats(name);
                                
                                if (stats == null)
                                {
                                    sb.append(padding).append(" §fGetting info...");
                                    break quickPlayerInfo;
                                }
                                
                                if (stats == DungeonStats.NOT_AVAILABLE)
                                {
                                    sb.append(padding).append(" §cFailed resolving user or not available");
                                    break quickPlayerInfo;
                                }
                                
                                sb.append(padding);
                                sb.append(f(" §f[%.2f | %.1f/%s",
                                    stats.cata, stats.secretPerRun, Utils.formatMetric(stats.totalSecret)));
                                sb.append('/');
                                if (c.partyFinderQuickPlayerInfoShowSecretPerExp)
                                    sb.append(f("%.1f", stats.totalSecret / (stats.cataExp / 50000.0f)));
                                else
                                    sb.append(stats.bestF7RunSecretsFound);
                                
                                if (stats.apiDiabled)
                                    sb.append(" §cAPI disabled");
                                else
                                {
                                    if (stats.wBlade > 0)
                                    {
                                        sb.append(" §eWBlade");
                                        if (stats.wBlade > 1)
                                            sb.append("§cx").append(stats.wBlade);
                                    }
                                    if (stats.term > 0)
                                    {
                                        sb.append(" §eTerm");
                                        if (stats.term > 1)
                                            sb.append("§cx").append(stats.term);
                                    }
                                }
                                
                                sb.append("§r]");
                            }
                            
                            if (blacklisted)
                            {
                                String reason = c.partyFinderPlayerBlacklist.get(name.toLowerCase(Locale.ROOT));
                                if (ctrlDown && !reason.equals(""))
                                    sb.append(" §8(").append(reason).append(")");
                            }
                            
                            event.toolTip.set(i, sb.toString());
                        }
                    }
                }
                else if (item == Blocks.emerald_block.getItem(null, null) &&
                    event.itemStack.getDisplayName().equals("§aRefresh"))
                {
                    for (int i = 0; i < event.toolTip.size(); i++)
                    {
                        if (event.toolTip.get(i).endsWith("§eClick to refresh!"))
                        {
                            long cd = getPartyFinderRefreshCooldown();
                            if (cd > 0L)
                                event.toolTip.set(i, "§cOn cooldown! (" + cd + " ms)");
                            break;
                        }
                    }
                }
            }
        }
    }
    
    public void onPlaySound(PlaySoundEvent event)
    {
        if (c.trackDamageTags)
        {
            if (event.name.equals("mob.zombie.woodbreak"))
                pushDamageInfo(getTicks(), "§f✧§4STRIKE§f✧");
        }
    }
    
    public void onFogDensitySet(EntityViewRenderEvent.FogDensity event)
    {
        if (c.enableNoFog)
        {
            event.density = 0.0f;
            event.setCanceled(true);
            GlStateManager.setFogStart(1023.0f);
            GlStateManager.setFogEnd(1024.0f);
        }
    }
    
    public void onWorldLoad(WorldEvent.Load ignoredEvent)
    {
        if (fragRunTracking)
            prevInventoryItemCounts.clear();
    }
    
    public void onWorldUnload(WorldEvent.Unload ignoredEvent)
    {
        if (isInF5Bossfight)
        {
            resetLivid();
            isInF5Bossfight = false;
        }
        if (c.highlightStarredMobs)
            starredMobs.clear();
        if (c.highlightBats)
            bats.clear();
        if (c.highlightShadowAssassins)
            shadowAssassins.clear();
        if (c.displayTargetMobNameTag)
            entityHurtTimes.clear();
        if (c.trackMaskUsage)
            maskUsages.clear();
        if (fragRunTracking)
            fragGotten = false;
        if (fragRunTracking)
            prevInventoryItemCounts.clear();
        armorStandNameFilterCache.clear();
    }
    
    // endregion Events
    
    // region Misc
    
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
    
    private void pushDamageInfo(int spawnTicks, String s)
    {
        damageTags.offer(new Pair<>(spawnTicks, s));
        if (damageTags.size() > c.damageTagTrackingCount)
            damageTags.remove();
    }
    
    private boolean isInF7()
    {
        return getCurrentIsland() == SkyblockIsland.DUNGEON &&
            (getCurrentLocationRaw().contains("(F7)") || getCurrentLocationRaw().contains("(M7)"));
    }
    
    private DungeonStats getPlayerDungeonStats(String name)
    {
        String uuid = Tweakception.apiManager.getPlayerUUID(name);
        if (uuid == null)
            return null;
        if (uuid.equals(APIManager.UUID_NOT_AVAILABLE))
            return DungeonStats.NOT_AVAILABLE;
        if (uuidToDungeonStatsMap.containsKey(uuid))
            return uuidToDungeonStatsMap.get(uuid);
        
        JsonObject sbInfo = Tweakception.apiManager.getSkyblockPlayerInfo(name);
        JsonObject hyInfo = Tweakception.apiManager.getHypixelPlayerInfo(name);
        
        if (sbInfo == null || hyInfo == null)
            return null;
        
        if (sbInfo == APIManager.INFO_NOT_AVAILABLE || hyInfo == APIManager.INFO_NOT_AVAILABLE)
        {
            uuidToDungeonStatsMap.put(uuid, DungeonStats.NOT_AVAILABLE);
            return DungeonStats.NOT_AVAILABLE;
        }
        
        float cataExp = 0.0f;
        int cataLevel = 0;
        float toNext = 0.0f;
        float secrets = 0.0f;
        long comps = 0;
        int bestF7SecretsFound = 0;
        
        if (sbInfo.has("dungeons"))
        {
            JsonObject dungeons = sbInfo.get("dungeons").getAsJsonObject();
            JsonObject dungeon_types = dungeons.get("dungeon_types").getAsJsonObject();
            JsonObject catacombs = dungeon_types.get("catacombs").getAsJsonObject();
            if (catacombs.has("experience"))
            {
                float experience = catacombs.get("experience").getAsFloat();
                cataExp = experience;
                for (int i = 0; i < Constants.CATACOMBS_LEVEL_EXPS.length; i++)
                    if (experience >= Constants.CATACOMBS_LEVEL_EXPS[i])
                    {
                        experience -= Constants.CATACOMBS_LEVEL_EXPS[i];
                        cataLevel = i + 1;
                    }
                    else
                        break;
                if (cataLevel < 50)
                    toNext = experience / Constants.CATACOMBS_LEVEL_EXPS[cataLevel];
            }
            
            if (catacombs.has("best_runs"))
            {
                JsonObject bestRuns = catacombs.get("best_runs").getAsJsonObject();
                if (bestRuns.has("7"))
                {
                    JsonArray f7 = bestRuns.get("7").getAsJsonArray();
                    if (f7.size() > 0)
                    {
                        JsonObject best = f7.get(f7.size() - 1).getAsJsonObject();
                        bestF7SecretsFound = best.get("secrets_found").getAsInt();
                    }
                }
            }
            
            if (hyInfo.has("achievements"))
            {
                JsonObject achievements = hyInfo.get("achievements").getAsJsonObject();
                if (achievements.has("skyblock_treasure_hunter"))
                {
                    secrets = achievements.get("skyblock_treasure_hunter").getAsFloat();
                    if (catacombs.has("tier_completions"))
                    {
                        JsonObject tierCompletions = catacombs.get("tier_completions").getAsJsonObject();
                        for (Map.Entry<String, JsonElement> entry : tierCompletions.entrySet())
                            comps += entry.getValue().getAsFloat();
                    }
                    if (dungeon_types.has("master_catacombs"))
                    {
                        JsonObject masterCatacombs = dungeon_types.get("master_catacombs").getAsJsonObject();
                        if (masterCatacombs.has("tier_completions"))
                        {
                            JsonObject tierCompletionsM = masterCatacombs.get("tier_completions").getAsJsonObject();
                            for (Map.Entry<String, JsonElement> entry : tierCompletionsM.entrySet())
                                comps += entry.getValue().getAsFloat();
                        }
                    }
                }
            }
        }
        
        final float cata = cataLevel + toNext;
        final float secretsPerRun = secrets / Math.max(comps, 1);
        
        boolean apiDisabled = false;
        int wBlade = 0;
        int term = 0;
        if (sbInfo.has("inv_contents"))
        {
            String invContents = sbInfo.get("inv_contents").getAsJsonObject().get("data").getAsString();
            NBTTagCompound invNbt = null;
            try
            {
                invNbt = CompressedStreamTools.readCompressed(
                    new ByteArrayInputStream(Base64.getDecoder().decode(invContents)));
            }
            catch (IOException ignored)
            {
            }
            
            if (invNbt != null)
            {
                final int TAG_COMPOUND = net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND;
                NBTTagList items = invNbt.getTagList("i", TAG_COMPOUND);
                Set<String> witherBladeIds = Utils.hashSet("HYPERION", "SCYLLA", "VALKYRIE", "ASTRAEA");
                for (int i = 0; i < items.tagCount(); i++)
                {
                    NBTTagCompound item = items.getCompoundTagAt(i);
                    if (item == null || item.getKeySet().size() == 0)
                        continue;
                    
                    NBTTagCompound itemTag = item.getCompoundTag("tag");
                    
                    if (itemTag != null && itemTag.hasKey("ExtraAttributes", TAG_COMPOUND))
                    {
                        NBTTagCompound extra = itemTag.getCompoundTag("ExtraAttributes");
                        String id = extra.getString("id");
                        if (id != null)
                        {
                            if (witherBladeIds.contains(id))
                                wBlade++;
                            else if (id.equals("TERMINATOR"))
                                term++;
                        }
                    }
                }
            }
        }
        else
            apiDisabled = true;
        
        DungeonStats stats = new DungeonStats(cata, cataExp, secretsPerRun, (long) secrets,
            wBlade, term, bestF7SecretsFound, apiDisabled);
        uuidToDungeonStatsMap.put(uuid, stats);
        return stats;
    }
    
    // endregion Misc
    
    // region Feature access
    
    public boolean isTrackingMaskUsage()
    {
        return c.trackMaskUsage;
    }
    
    public boolean isMaskUsed(String uuid)
    {
        return maskUsages.containsKey(uuid);
    }
    
    public boolean isBlockingOpheliaShopClicks()
    {
        return c.blockOpheliaShopClicks;
    }
    
    public boolean isPartyFinderRefreshCooldownEnbaled()
    {
        return c.partyFinderRefreshCooldown;
    }
    
    public long getPartyFinderRefreshCooldown()
    {
        long elapsed = System.currentTimeMillis() - partyFinderLastRefreshMillis;
        return Math.max(3100L - elapsed, 0L);
    }
    
    public void setPartyFinderRefreshCooldown()
    {
        partyFinderLastRefreshMillis = System.currentTimeMillis();
    }
    
    public boolean isAutoSwapSpiritSceptreAoteOn()
    {
        return c.autoSwapSpiritSceptreAote;
    }
    
    public boolean isAutoSwapHyperionAoteOn()
    {
        return c.autoSwapHyperionAote;
    }
    
    public boolean isPickaxeMiddleClickRemoveBlockOn()
    {
        return c.pickaxeMiddleClickRemoveBlock;
    }
    
    public boolean isPickaxeMiddleClickRemoveLineOn()
    {
        return c.pickaxeMiddleClickRemoveLine;
    }
    
    public boolean isBlockFlowerPlacementOn()
    {
        return c.blockFlowerPlacement;
    }
    
    // endregion Feature access
    
    // region Overlays
    
    private class FragRunOverlay extends TextOverlay
    {
        public static final String NAME = "FragRunOverlay";
        
        public FragRunOverlay()
        {
            super(NAME);
            setAnchor(Anchor.TopRight);
            setOrigin(Anchor.TopRight);
            setX(-10);
            setY(10);
            setTextAlignment(1);
        }
        
        @Override
        public void update()
        {
            super.update();
            List<String> list = new ArrayList<>();
            
            String status;
            if (fragPendingEndRunWarp)
                status = "§cPending warp";
            else if (fragrunStartTime != 0L)
                status = "§aRun time: " + Utils.msToMMSSmmm(System.currentTimeMillis() - fragrunStartTime);
            else
                status = "§aIdle";
            
            String br;
            if (fragBloodRushTime != 0L)
                br = "§aBlood rush: " + Utils.msToMMSSmmm(fragBloodRushTime) + "§r" + fragBloodRushRecord;
            else
                br = "§aBlood rush: §cnot yet";
            
            list.add(status);
            list.add(br);
            list.add("Session total runs: " + fragSessionRuns);
            list.add("Session fastest blood rush: " + Utils.msToMMSSmmm(fragSessionFastestBloodRush));
            list.add("Session avg run time: " + Utils.msToMMSSmmm(fragSessionTotalTime / Math.max(fragSessionRuns, 1)));
            list.add("Session total time: " + Utils.msToHHMMSSmmm(fragSessionTotalTime));
            list.add("Total runs: " + c.totalFragruns);
            list.add("Fastest run: " + Utils.msToMMSSmmm(c.fastestFragrun));
            list.add("Fastest blood rush: " + Utils.msToMMSSmmm(c.fastestBloodRush));
            
            if (fragLastRunDisplayStartTime != 0 && getTicks() - fragLastRunDisplayStartTime <= 20 * 30)
            {
                String lastRun = "§bLast run time: " + fragLastRunTime + "§r" + fragLastRecord;
                String lastBr = "§bLast blood rush: " + fragLastBloodRush;
                list.add(lastRun);
                list.add(lastBr);
            }
            
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("frag run");
            list.add("overlay");
            return list;
        }
    }
    
    private class DamageTagTrackingOverlay extends TextOverlay
    {
        public static final String NAME = "DamageTagTrackingOverlay";
        
        public DamageTagTrackingOverlay()
        {
            super(NAME);
            setAnchor(Anchor.BottomRight);
            setOrigin(Anchor.BottomRight);
            setX(-10);
            setY(-10);
            setTextAlignment(1);
        }
        
        @Override
        public void update()
        {
            super.update();
            List<String> list = new ArrayList<>();
            // Old first, top down
            for (Pair<Integer, String> p : damageTags)
                list.add(p.b);
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("69420");
            list.add("damage tag 2");
            list.add("damage tag 1");
            return list;
        }
    }
    
    private class DamageHistoryOverlay extends TextOverlay
    {
        public static final String NAME = "DamageHistoryOverlay";
        
        public DamageHistoryOverlay()
        {
            super(NAME);
            setAnchor(Anchor.BottomRight);
            setOrigin(Anchor.BottomRight);
            setX(-100);
            setY(-10);
            setTextAlignment(1);
        }
        
        @Override
        public void update()
        {
            super.update();
            List<String> list = new ArrayList<>();
            int count = 0;
            for (Map.Entry<Long, Long> entry : damageHistoriesSorted)
            {
                list.add(Utils.formatCommas(entry.getKey()) + " - " + entry.getValue());
                if (++count >= c.damageHistoryOverlayMaxLines)
                    break;
            }
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("6,766 - 6");
            list.add("65 - 3");
            list.add("12 - 1");
            return list;
        }
    }
    
    private class ShootingSpeedOverlay extends TextOverlay
    {
        public static final String NAME = "ShootingSpeedOverlay";
        
        public ShootingSpeedOverlay()
        {
            super(NAME);
            setAnchor(Anchor.TopRight);
            setOrigin(Anchor.TopCenter);
            setX(-30);
            setY(10);
        }
        
        @Override
        public void update()
        {
            super.update();
            List<String> list = new ArrayList<>();
            float count = (float) arrowSpawnTimes.size() / c.shootingSpeedTrackingSampleSecs;
            String s = f("Arrows/s: %.3f", count);
            list.add(s);
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("Arrows/s: 69");
            return list;
        }
    }
    
    private class TargetMobNametagOverlay extends TextOverlay
    {
        public static final String NAME = "TargetMobNametagOverlay";
        
        public TargetMobNametagOverlay()
        {
            super(NAME);
            setAnchor(Anchor.TopCenter);
            setOrigin(Anchor.TopCenter);
            setY(10);
        }
        
        @Override
        public void update()
        {
            super.update();
            List<String> list = new ArrayList<>();
            if (hitDisplayTargetNameTag != null)
            {
                String name = hitDisplayTargetNameTag.getName();
                list.add(name);
            }
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("TargetMobNametag 69<3");
            return list;
        }
    }
    
    // endregion Overlay
    
    // region Commands
    
    public void toggleNoFog()
    {
        c.enableNoFog = !c.enableNoFog;
        sendChat("NoFog: Toggled " + c.enableNoFog);
    }
    
    public void toggleNoFogAutoToggle()
    {
        c.enableNoFogAutoToggle = !c.enableNoFogAutoToggle;
        sendChat("NoFog: Toggled auto toggle " + c.enableNoFogAutoToggle);
    }
    
    public void toggleHideName()
    {
        c.hideNonStarredMobsName = !c.hideNonStarredMobsName;
        sendChat("HideName: Toggled hide name " + c.hideNonStarredMobsName);
        if (!c.hideNonStarredMobsName)
            armorStandNameFilterCache.clear();
    }
    
    public void toggleHideDamageTags()
    {
        c.hideDamageTags = !c.hideDamageTags;
        sendChatf("HideDamageTags: Toggled " + c.hideDamageTags);
        if (!c.hideDamageTags)
            armorStandNameFilterCache.clear();
    }
    
    public void toggleHighlightStarredMobs()
    {
        c.highlightStarredMobs = !c.highlightStarredMobs;
        sendChat("HighlightStarredMobs: Toggled " + c.highlightStarredMobs);
    }
    
    public void toggleHighlightSpiritBear()
    {
        c.highlightSpiritBear = !c.highlightSpiritBear;
        sendChat("HighlightSpiritBear: Toggled " + c.highlightSpiritBear);
    }
    
    public void toggleHighlightShadowAssassin()
    {
        c.highlightShadowAssassins = !c.highlightShadowAssassins;
        sendChat("HighlightShadowAssassin: Toggled " + c.highlightShadowAssassins);
    }
    
    public void toggleHighlightBats()
    {
        c.highlightBats = !c.highlightBats;
        sendChat("HighlightBats: Toggled " + c.highlightBats);
        
        if (c.highlightBats)
        {
            bats.addAll(McUtils.getWorld().getEntities(EntityBat.class, e -> true));
        }
    }
    
    public void toggleHighlightDoorKeys()
    {
        c.highlightDoorKeys = !c.highlightDoorKeys;
        sendChat("HighlightDoorKeys: Toggled " + c.highlightDoorKeys);
    }
    
    public void blockRightClickSet()
    {
        ItemStack item = McUtils.getPlayer().inventory.getCurrentItem();
        if (item == null)
        {
            sendChat("BlockRightClick: Current selected item is empty");
            return;
        }
        String name = McUtils.cleanColor(item.getDisplayName());
        if (c.blockRightClickItemNames.contains(name))
        {
            c.blockRightClickItemNames.remove(name);
            sendChat("BlockRightClick: Removed item \"" + name + "§r\" from block list");
        }
        else
        {
            c.blockRightClickItemNames.add(name);
            sendChat("BlockRightClick: Added item \"" + name + "§r\" to block list");
        }
    }
    
    public void blockRightClickRemove(int i)
    {
        if (i < 1 || i > c.blockRightClickItemNames.size())
        {
            sendChat("BlockRightClick: Index out of bounds");
            return;
        }
        String ele = c.blockRightClickItemNames.toArray(new String[0])[i - 1];
        String chatActionUuid = Tweakception.globalTweaks.registerChatAction(() ->
        {
            if (c.blockRightClickItemNames.contains(ele))
            {
                c.blockRightClickItemNames.remove(ele);
                sendChat("BlockRightClick: Removed \"" + ele + "§r\" from the list");
            }
        }, 20 * 5, null);
        
        IChatComponent nice = new ChatComponentText(
            "DT-BlockRightClick: You really wanna remove \"" + ele + "§r\" from the list? Click here in 5 seconds to continue");
        nice.getChatStyle().setChatClickEvent(
            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tc action " + chatActionUuid));
        sendChat(nice);
    }
    
    public void blockRightClickList()
    {
        if (c.blockRightClickItemNames.isEmpty())
        {
            sendChat("BlockRightClick: List is empty");
            return;
        }
        sendChat("BlockRightClick: There are " + c.blockRightClickItemNames.size() + " items in the list");
        int i = 1;
        for (String s : c.blockRightClickItemNames)
            sendChat(i++ + ": " + s);
    }
    
    public void toggleTrackDamageTags()
    {
        c.trackDamageTags = !c.trackDamageTags;
        Tweakception.overlayManager.setEnable(DamageTagTrackingOverlay.NAME, c.trackDamageTags);
        sendChatf("TrackDamageTags: Toggled %b, track non crit %b", c.trackDamageTags, c.trackNonCritDamageTags);
        if (c.trackDamageTags)
        {
            damageTags.clear();
            armorStandsTemp.clear();
        }
    }
    
    public void toggleTrackNonCritDamageTags()
    {
        c.trackNonCritDamageTags = !c.trackNonCritDamageTags;
        sendChat("TrackDamageTags: Toggled track non crit " + c.trackNonCritDamageTags);
        if (c.trackNonCritDamageTags)
            sendChat("TrackDamageTags: (notice: this also tracks damages to you)");
        if (!c.trackDamageTags)
            sendChat("TrackDamageTags: (notice: overall tracking isn't on!)");
    }
    
    public void toggleTrackWitherDamageTags()
    {
        c.trackWitherDamageTags = !c.trackWitherDamageTags;
        sendChat("TrackDamageTags: Toggled track wither damage " + c.trackWitherDamageTags);
        if (!c.trackDamageTags)
            sendChat("TrackDamageTags: (notice: overall tracking isn't on!)");
    }
    
    public void setDamageTagTrackingCount(int count)
    {
        c.damageTagTrackingCount = count > 0 ? count : new DungeonTweaksConfig().damageTagTrackingCount;
        sendChat("TrackDamageTags: Set count to " + c.damageTagTrackingCount);
        damageTags.clear();
        armorStandsTemp.clear();
    }
    
    public void setDamageTagHistoryTimeoutTicks(int ticks)
    {
        c.damageTagHistoryTimeoutTicks = ticks > 0 ? ticks : new DungeonTweaksConfig().damageTagHistoryTimeoutTicks;
        sendChat("TrackDamageTags: Set history timeout to " + c.damageTagHistoryTimeoutTicks + " ticks");
    }
    
    public void toggleAutoCloseSecretChest()
    {
        c.autoCloseSecretChest = !c.autoCloseSecretChest;
        sendChat("AutoCloseSecretChest: Toggled " + c.autoCloseSecretChest);
    }
    
    public void toggleTrackSalvage()
    {
        c.trackSalvage = !c.trackSalvage;
        sendChat("TrackSalvage: Toggled " + c.trackSalvage);
    }
    
    public void printTrackSalvage()
    {
        if (!c.trackSalvage)
            sendChat("TrackSalvage: Feature isn't on!");
        for (Map.Entry<String, Integer> e : c.salvagedEssences.entrySet())
            sendChatf("TrackSalvage: §f%dx §s Essence", e.getValue(), StringUtils.capitalize(e.getKey()));
    }
    
    public void toggleAutoJoinParty()
    {
        c.autoJoinParty = !c.autoJoinParty;
        sendChat("AutoJoinParty: Toggled " + c.autoJoinParty);
    }
    
    public void autoJoinPartyList()
    {
        if (c.autoJoinPartyOwners.isEmpty())
        {
            sendChat("AutoJoinParty: Trusted player list is empty");
            return;
        }
        sendChat("AutoJoinParty: There are " + c.autoJoinPartyOwners.size() + " players in the list");
        int i = 1;
        for (String name : c.autoJoinPartyOwners)
            sendChat(i++ + ": " + name);
    }
    
    public void autoJoinPartyAdd(String name)
    {
        if (name == null || name.equals(""))
        {
            sendChat("AutoJoinParty: Give me a player name");
            return;
        }
        name = name.toLowerCase(Locale.ROOT);
        if (c.autoJoinPartyOwners.contains(name))
        {
            sendChat("AutoJoinParty: Player " + name + " is already in the list");
        }
        else
        {
            c.autoJoinPartyOwners.add(name);
            sendChat("AutoJoinParty: Added " + name);
        }
    }
    
    public void autoJoinPartyRemove(String name)
    {
        if (name == null || name.equals(""))
        {
            sendChat("AutoJoinParty: Give me a player name");
            return;
        }
        name = name.toLowerCase(Locale.ROOT);
        if (c.autoJoinPartyOwners.contains(name))
        {
            c.autoJoinPartyOwners.remove(name);
            sendChat("AutoJoinParty: Removed " + name);
        }
        else
        {
            sendChat("AutoJoinParty: Player " + name + " is not in the list");
        }
    }
    
    public void autoJoinPartyToggleWhitelist()
    {
        c.autoJoinPartyWhitelistEnable = !c.autoJoinPartyWhitelistEnable;
        sendChat("AutoJoinParty: Whitelist " + (c.autoJoinPartyWhitelistEnable ? "enabled" : "disabled"));
    }
    
    public void listFragCounts()
    {
        String sep = System.lineSeparator();
        StringBuilder sb = StringBuilderCache.get();
        
        sendChat("Total runs: " + c.totalFragruns);
        for (Map.Entry<String, Integer> e : c.fragDrops.entrySet())
            sendChatf("§5%s§f: §a%d", FRAGS_AND_NAMES.get(e.getKey()), e.getValue());
        
        // Stupid java crap dog shit ass fuck
        List<Map.Entry<String, Integer>> sorted = c.fragDrops.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .collect(Collectors.toList());
        
        sb.append("Total runs: ").append(c.totalFragruns).append(sep);
        for (Map.Entry<String, Integer> e : sorted)
            sb.append(FRAGS_AND_NAMES.get(e.getKey())).append(": ").append(e.getValue()).append(sep);
        
        Utils.setClipboard(sb.toString());
        sendChat("Also copied to clipboard");
    }
    
    public void fragStartSession()
    {
        if (fragRunTracking)
        {
            sendChat("Frag: You're already in a session, use `endsession` to end");
            return;
        }
        if (c.fragBot.equals(""))
        {
            sendChat("Frag: Please set a frag bot first using `setfragbot <name>`");
            return;
        }
        
        sendChat("Frag: Starting session, timer will start when you start f7");
        
        fragRunTracking = true;
        Tweakception.overlayManager.enable(FragRunOverlay.NAME);
        fragrunStartTime = 0L;
        fragBloodRushTime = 0L;
        fragBloodRushRecord = "";
        fragPendingEndRunWarp = false;
        
        fragSessionFastestBloodRush = 0L;
        fragSessionRuns = 0;
        fragSessionTotalTime = 0L;
        
        fragLastRunTime = "";
        fragLastRecord = "";
        fragLastRunDisplayStartTime = 0;
    }
    
    public void fragEndSession()
    {
        if (!fragRunTracking)
        {
            sendChat("Frag: You've not started a session");
            return;
        }
        
        if (isInF7())
        {
            fragEnd();
        }
        
        fragRunTracking = false;
        Tweakception.overlayManager.disable(FragRunOverlay.NAME);
        
        sendChat("Frag: Ending session, life total runs: " + c.totalFragruns);
        
        if (fragSessionRuns == 0)
            return;
        
        sendChat("Frag: Session runs: " + fragSessionRuns);
        sendChat("Frag: Session fastest blood rush: " + Utils.msToMMSSmmm(fragSessionFastestBloodRush));
        sendChat("Frag: Session total time: " + Utils.msToHHMMSSmmm(fragSessionTotalTime));
        sendChat("Frag: Session average run time: " + Utils.msToMMSSmmm(fragSessionTotalTime / fragSessionRuns));
    }
    
    public void fragNext()
    {
        if (getCurrentIsland() != SkyblockIsland.DUNGEON &&
            !(getCurrentLocationRaw().contains("(F7)") || getCurrentLocationRaw().contains("(M7)")))
        {
            sendChat("Frag: Floor 7 not detected");
            return;
        }
        if (!fragRunTracking)
        {
            sendChat("Frag: Please start a session first using `startsession`, and the timer will start");
            return;
        }
        
        sendChat("Frag: Warping back to dhub for next run");
        
        fragPendingEndRunWarp = true;
        McUtils.getPlayer().sendChatMessage("/warp dhub");
        // Continued at `if (fragPendingEndRunWarp)` in onTick()
    }
    
    private void fragStart()
    {
        fragrunStartTime = System.currentTimeMillis();
        sendChat("Frag: started the next run");
        
        // To prevent active run being cancelled when starting before the island is updated
        Tweakception.globalTweaks.updateIslandNow();
    }
    
    private void fragEnd()
    {
        long elapsed = System.currentTimeMillis() - fragrunStartTime;
        
        fragLastRunDisplayStartTime = getTicks();
        fragLastRunTime = Utils.msToMMSSmmm(elapsed);
        fragLastBloodRush = Utils.msToMMSSmmm(fragBloodRushTime);
        
        fragrunStartTime = 0L;
        fragBloodRushTime = 0L;
        fragBloodRushRecord = "";
        
        fragSessionRuns++;
        fragSessionTotalTime += elapsed;
        
        c.totalFragruns++;
        
        if (c.fastestFragrun == 0L || elapsed < c.fastestFragrun)
        {
            fragLastRecord = "(§eNEW RECORD!§r)";
            c.fastestFragrun = elapsed;
        }
        else if (elapsed == c.fastestFragrun)
            fragLastRecord = "(§aFASTEST TIME!§r)";
        else
            fragLastRecord = "";
        
        sendChatf("Frag: Run took %s§r%s", fragLastRunTime, fragLastRecord);
    }
    
    private void fragSetBloodRush()
    {
        fragBloodRushTime = System.currentTimeMillis() - fragrunStartTime;
        fragBloodRushRecord = "";
        
        if (fragSessionFastestBloodRush == 0L || fragBloodRushTime < fragSessionFastestBloodRush)
        {
            fragSessionFastestBloodRush = fragBloodRushTime;
            fragBloodRushRecord = "(§eSESSION NEW RECORD!§r)";
        }
        else if (fragBloodRushTime == fragSessionFastestBloodRush)
            fragBloodRushRecord = "(§aSESSION FASTEST TIME!§r)";
        
        if (c.fastestBloodRush == 0L || fragBloodRushTime < c.fastestBloodRush)
        {
            c.fastestBloodRush = fragBloodRushTime;
            fragBloodRushRecord = "(§eNEW RECORD!§r)";
        }
        else if (fragBloodRushTime == c.fastestBloodRush)
            fragBloodRushRecord = "(§aFASTEST TIME!§r)";
    }
    
    public void setFragBot(String name)
    {
        if (name == null || name.equals(""))
        {
            if (!c.fragBot.equals(""))
                sendChat("Frag: Removed frag bot");
            else
                sendChat("Frag: Set a frag bot using `setfragbot <name>`, or remove the existing frag bot using `setfragbot`");
        }
        else
        {
            c.fragBot = name;
            sendChat("Frag: Set frag bot to " + name);
        }
    }
    
    public void toggleFragAutoReparty()
    {
        c.fragAutoReparty = !c.fragAutoReparty;
        sendChat("Frag: Toggled auto reparty " + c.fragAutoReparty);
    }
    
    public void toggleTrackShootingSpeed()
    {
        c.trackShootingSpeed = !c.trackShootingSpeed;
        Tweakception.overlayManager.setEnable(ShootingSpeedOverlay.NAME, c.trackShootingSpeed);
        sendChat("TrackShootingSpeed: Toggled " + c.trackShootingSpeed);
    }
    
    public void setShootingSpeedTrackingSampleSecs(int secs)
    {
        c.shootingSpeedTrackingSampleSecs = secs > 0 ? secs : new DungeonTweaksConfig().shootingSpeedTrackingSampleSecs;
        sendChat("TrackShootingSpeed: Set sample secs to " + c.shootingSpeedTrackingSampleSecs);
    }
    
    public void setShootingSpeedTrackingRange(int blocks)
    {
        c.shootingSpeedTrackingRange = blocks > 0 ? blocks : new DungeonTweaksConfig().shootingSpeedTrackingRange;
        sendChat("TrackShootingSpeed: Set spawn range to " + c.shootingSpeedTrackingRange);
    }
    
    public void toggleDisplayMobNameTag()
    {
        c.displayTargetMobNameTag = !c.displayTargetMobNameTag;
        Tweakception.overlayManager.setEnable(TargetMobNametagOverlay.NAME, c.displayTargetMobNameTag);
        sendChat("DisplayMobNameTag: Toggled " + c.displayTargetMobNameTag);
    }
    
    public void toggleTrackMaskUsage()
    {
        c.trackMaskUsage = !c.trackMaskUsage;
        sendChat("TrackMaskUsage: Toggled " + c.trackMaskUsage);
    }
    
    public void toggleBlockOpheliaShopClicks()
    {
        c.blockOpheliaShopClicks = !c.blockOpheliaShopClicks;
        sendChat("BlockOpheliaShopClicks: Toggled " + c.blockOpheliaShopClicks);
    }
    
    public void partyFinderQuickPlayerInfoToggle()
    {
        c.partyFinderDisplayQuickPlayerInfo = !c.partyFinderDisplayQuickPlayerInfo;
        sendChat("PartyFinderDisplayQuickPlayerInfo: Toggled " + c.partyFinderDisplayQuickPlayerInfo);
    }
    
    public void partyFinderQuickPlayerInfoToggleShowSecretPerExp()
    {
        c.partyFinderQuickPlayerInfoShowSecretPerExp = !c.partyFinderQuickPlayerInfoShowSecretPerExp;
        sendChat("PartyFinderDisplayQuickPlayerInfo: Toggled secrets per 50k exp " +
            c.partyFinderQuickPlayerInfoShowSecretPerExp);
    }
    
    public void partyFinderRefreshCooldownToggle()
    {
        c.partyFinderRefreshCooldown = !c.partyFinderRefreshCooldown;
        sendChat("PartyFinderRefreshCooldown: Toggled " + c.partyFinderRefreshCooldown);
    }
    
    public void partyFinderPlayerBlacklistSet(String name, String reason)
    {
        if (name.equals(""))
        {
            sendChatf("PartyFinderPlayerBlacklist: There are %d players in the list",
                c.partyFinderPlayerBlacklist.size());
            int i = 1;
            for (Map.Entry<String, String> s : c.partyFinderPlayerBlacklist.entrySet())
                sendChatf("%d: %s%s", i++, s.getKey(), s.getValue().equals("") ? "" : ", reason: " + s.getValue());
            return;
        }
        
        name = name.toLowerCase(Locale.ROOT);
        if (c.partyFinderPlayerBlacklist.containsKey(name))
        {
            c.partyFinderPlayerBlacklist.remove(name);
            sendChat("PartyFinderPlayerBlacklist: Removed " + name);
        }
        else
        {
            c.partyFinderPlayerBlacklist.put(name, reason);
            sendChat("PartyFinderPlayerBlacklist: Added " + name + (reason.equals("") ? "" : ", reason: " + reason));
        }
    }
    
    public void freeCaches()
    {
        uuidToDungeonStatsMap.clear();
        sendChat("Cleared caches");
    }
    
    public void toggleGyroWandOverlay()
    {
        c.gyroWandOverlay = !c.gyroWandOverlay;
        sendChat("GyroWandOverlay: Toggled " + c.gyroWandOverlay);
    }
    
    public void getDailyRuns(String name)
    {
        if (name.equals(""))
            name = McUtils.getPlayer().getName();
        String finalName = name;
        
        Tweakception.apiManager.removeCache(name);
        JsonObject sbInfo = Tweakception.apiManager.getSkyblockPlayerInfo(name,
            res -> getDailyRunsInternal(finalName, res));
        
        if (sbInfo == APIManager.INFO_NOT_AVAILABLE)
            sendChat("Info is not available");
        else if (sbInfo == null)
            sendChat("Getting data");
        else
            getDailyRunsInternal(name, sbInfo); // Should never run
    }
    
    private void getDailyRunsInternal(String name, JsonObject sbInfo)
    {
        JsonObject dungeons = sbInfo.get("dungeons").getAsJsonObject();
        if (dungeons.has("daily_runs"))
        {
            JsonObject daily = dungeons.get("daily_runs").getAsJsonObject();
            int count = daily.get("completed_runs_count").getAsInt();
            sendChat("Daily runs count of " + name + " is " + count);
        }
    }
    
    public void toggleDisplaySoulName()
    {
        // TODO
        sendChat("no");
    }
    
    public void toggleTrackDamageHistory()
    {
        c.trackDamageHistory = !c.trackDamageHistory;
        if (!c.trackDamageHistory)
        {
            damageHistoriesMap.clear();
            damageHistoriesSorted.clear();
        }
        Tweakception.overlayManager.setEnable(DamageHistoryOverlay.NAME, c.trackDamageHistory);
        sendChat("TrackDamageHistory: Toggled " + c.trackDamageHistory);
    }
    
    public void resetDamageHistories()
    {
        damageHistoriesMap.clear();
        damageHistoriesSorted.clear();
    }
    
    public void setDamageHistoryOverlayMaxLines(int l)
    {
        c.damageHistoryOverlayMaxLines = l > 0 ? l : new DungeonTweaksConfig().damageHistoryOverlayMaxLines;
        sendChat("TrackDamageHistory: Set overlay max lines to " + c.damageHistoryOverlayMaxLines);
    }
    
    public void dumpDamageHistories()
    {
        if (!c.trackDamageHistory)
        {
            sendChat("TrackDamageHistory: Feature is off");
            return;
        }
        
        List<String> list = new ArrayList<>();
        list.add("With commas");
        list.add("");
        for (Map.Entry<Long, Long> entry : damageHistoriesSorted)
            list.add(Utils.formatCommas(entry.getKey()) + " - " + entry.getValue());
        list.add("");
        list.add("No commas");
        list.add("");
        for (Map.Entry<Long, Long> entry : damageHistoriesSorted)
            list.add(entry.getKey() + " - " + entry.getValue());
        
        try
        {
            File file = Tweakception.configuration.createWriteFileWithCurrentDateTime("damagehistories_$.txt", list);
            sendChat("Dumped damage histories");
            getPlayer().addChatMessage(new ChatComponentTranslation("Output written to file %s",
                McUtils.makeFileLink(file)));
            Desktop.getDesktop().open(file);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            sendChat("Exception occurred while making and opening file");
        }
    }
    
    public void toggleAutoSwapSpiritSceptreAote()
    {
        c.autoSwapSpiritSceptreAote = !c.autoSwapSpiritSceptreAote;
        sendChat("AutoSwapSpiritSceptreAote: Toggled " + c.autoSwapSpiritSceptreAote);
    }
    
    public void toggleAutoSwapHyperionAote()
    {
        c.autoSwapHyperionAote = !c.autoSwapHyperionAote;
        sendChat("AutoSwapHyperionAote: Toggled " + c.autoSwapHyperionAote);
    }
    
    public void togglePickaxeMiddleClickRemoveBlock()
    {
        c.pickaxeMiddleClickRemoveBlock = !c.pickaxeMiddleClickRemoveBlock;
        sendChat("PickaxeMiddleClickRemoveBlock: Toggled " + c.pickaxeMiddleClickRemoveBlock);
    }
    
    public void togglePickaxeMiddleClickRemoveLine()
    {
        c.pickaxeMiddleClickRemoveLine = !c.pickaxeMiddleClickRemoveLine;
        sendChat("PickaxeMiddleClickRemoveLine: Toggled " + c.pickaxeMiddleClickRemoveLine);
    }
    
    public void toggleBlockFlowerPlacement()
    {
        c.blockFlowerPlacement = !c.blockFlowerPlacement;
        sendChat("BlockFlowerPlacement: Toggled " + c.blockFlowerPlacement);
    }
    
    // endregion Commands
}
