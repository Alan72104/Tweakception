package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.events.IslandChangedEvent;
import a7.tweakception.utils.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.event.ClickEvent;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.ByteArrayInputStream;
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

import static a7.tweakception.tweaks.GlobalTracker.*;
import static a7.tweakception.utils.McUtils.*;
import static a7.tweakception.utils.Utils.f;
import static a7.tweakception.utils.Utils.removeWhile;

public class DungeonTweaks extends Tweak
{
    private final DungeonTweaksConfig c;
    public static class DungeonTweaksConfig
    {
        public boolean enableNoFog = false;
        public boolean enableNoFogAutoToggle = false;
        public boolean hideNonStarredMobsName = true;
        public boolean hideDamageTags = false;
        public boolean highlightStarredMobs = false;
        public boolean highlightBats = true;
        public boolean highlightSpiritBear = true;
        public boolean highlightShadowAssassins = true;
        public boolean highlightDoorKeys = true;
        public Set<String> blockRightClickItemNames = new HashSet<>();
        public boolean trackDamageTags = false;
        public int damageTagTrackingCount = 10;
        public boolean trackNonCritDamageTags = false;
        public boolean trackWitherDamageTags = false;
        public int damageTagHistoryTimeoutTicks = 20 * 30;
        public boolean autoCloseSecretChest = false;
        public boolean autoSalvage = false;
        public Map<String, Integer> salvagedEssences = ESSENCES.stream().collect(Collectors.toMap(e -> e, e -> 0));
        public boolean autoJoinParty = false;
        public Set<String> autoJoinPartyOwners = new HashSet<>(Collections.singletonList("alan72104"));
        public Map<String, Integer> fragDrops = FRAGS_AND_NAMES.keySet().stream().collect(Collectors.toMap(e -> e, e -> 0));
        public String fragBot = "";
        public long fastestFragrun = 0L;
        public int totalFragruns = 0;
        public long fastestBloodRush = 0L;
        public boolean trackShootingSpeed = false;
        public int shootingSpeedTrackingSampleSecs = 2;
        public int shootingSpeedTrackingRange = 4;
        public boolean displayTargetMobNameTag = false;
        public boolean trackMaskUsage = true;
        public boolean blockOpheliaShopClicks = true;
        public boolean partyFinderDisplayQuickPlayerInfo = false;
        public boolean partyFinderQuickPlayerInfoShowSecretPerExp = false;
        public Map<String, String> partyFinderPlayerBlacklist = new HashMap<>();
        public boolean gyroWandOverlay = false;
    }
    private static final String F5_BOSS_START = "Welcome, you arrive right on time. I am Livid, the Master of Shadows.";
    private static final String F5_BOSS_END = "Impossible! How did you figure out which one I was?";
    private static final Map<String, String> LIVID_PREFIX_TO_COLOR_MAP = new HashMap<>();
    private static final EnumChatFormatting[] KOOL_COLORS =
    {
        EnumChatFormatting.WHITE,
        EnumChatFormatting.YELLOW,
        EnumChatFormatting.GOLD,
        EnumChatFormatting.RED,
        EnumChatFormatting.RED,
        EnumChatFormatting.WHITE
    };
    private static final Set<String> SECRET_CHEST_ITEMS = new HashSet<>();
    private static final Set<String> TRASH_ITEMS = new HashSet<>();
    private static final Set<String> ESSENCES = new HashSet<>(Arrays.asList("wither", "spider", "undead", "dragon",
            "gold", "diamond", "ice", "crimson"));
    private static final Map<String, String> FRAGS_AND_NAMES = new HashMap<>();
    private static final Set<String> DUNGEON_FLOOR_HEADS = new HashSet<>();
    private static final Set<String> MASKS = new HashSet<>(Arrays.asList("BONZO_MASK",
            "STARRED_BONZO_MASK", "SPIRIT_MASK"));

    private static boolean isDamageFormattingExceptionNotified = false;
    private static boolean isGetFieldExceptionNotified = false;
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
    private final Set<Entity> starredMobs = new HashSet<>();
    private Entity spiritBear = null;
    private final Matcher anyDamageTagMatcher = Pattern.compile("^§.✧?(?:(?:§.)?\\d)+.*").matcher("");
    private final Matcher critTagMatcher = Pattern.compile("^§f✧((?:§.\\d)+)§.✧(.*)").matcher(""); // §f✧§a6§b7§c8§a✧§d♥
    private final Matcher nonCritTagMatcher = Pattern.compile("^§7(\\d+)(.*)").matcher(""); // §712345
    private final Matcher witherTagMatcher = Pattern.compile("^§0(\\d+)$").matcher(""); // §012345
    private boolean secretChestOpened = false;
    private boolean blacksmithMenuOpened = false;
    private boolean salvageClickSent = false;
    private int salvageLastClickTick = 0;
    private String salvagingEssenceType = "";
    private int salvagingEssencegAmount = 0;
    private final Matcher essenceMatcher = Pattern.compile("^ {2}§[\\da-f](\\w+) Essence §[\\da-f]x(\\d+)").matcher("");
    private final Matcher partyRequestMatcher = Pattern.compile(" (.*) has invited you to join (?:their|.*) party!").matcher("");
    private boolean fragGotten = false;
    private boolean fragRunTracking = false;
    private long fragrunStartTime = 0L; // long = millis, int = ticks
    private long fragBloodRushTime = 0L;
    private String fragBloodRushRecord = "";
    private int fragLastRunDisplayStartTime = 0;
    private String fragLastRunTime= "";
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
    private final Matcher maskCooldownMatcher = Pattern.compile("^§8Cooldown: §a(\\d+)s").matcher("");
    private final Matcher dungeonItemStatMatcher = Pattern.compile(
            " §8\\(([-+])?(\\d+(?:,\\d+)*(?:\\.\\d+)?)(%?)\\)(.*)").matcher("");
    private final Map<String, DungeonStats> uuidToDungeonStatsMap = new HashMap<>();
    private final Matcher partyFinderTitleMatcher = Pattern.compile("^§o§6(?:§[\\da-f])?([^']+)'s Party(.*)").matcher("");
    private final Matcher partyFinderPlayerMatcher = Pattern.compile(
            "^§5§o (?:§[\\da-f])?([\\w\\d]+)(§f: §e\\w+§b \\(§e\\d{1,2}§b\\))").matcher("");
    private static class DungeonStats
    {
        public float cata = 0.0f;
        public float cataExp = 0.0f;
        public float secretPerRun = 0.0f;
        public long totalSecret = 0L;
        public int wBlade = 0;
        public int term = 0;
        public boolean apiDiabled = false;
        public DungeonStats() { }
        public DungeonStats(float c, float ce, float spr, long ts, int wb, int t, boolean ad)
        { cata = c; cataExp = ce; secretPerRun = spr; totalSecret = ts; wBlade = wb; term = t; apiDiabled = ad; }
        public static final DungeonStats NOT_AVAILABLE = new DungeonStats();
    }
    private static class MaskUsage
    {
        public int useTicks = 0;
        public int cooldownTicks = 0;
        public MaskUsage(int u, int c) { useTicks = u; cooldownTicks = c; }
    }

    static
    {
        LIVID_PREFIX_TO_COLOR_MAP.put("Arcade", "§e");
        LIVID_PREFIX_TO_COLOR_MAP.put("Crossed", "§d");
        LIVID_PREFIX_TO_COLOR_MAP.put("Doctor", "§7");
        LIVID_PREFIX_TO_COLOR_MAP.put("Frog", "§2");
        LIVID_PREFIX_TO_COLOR_MAP.put("Hockey", "§c");
        LIVID_PREFIX_TO_COLOR_MAP.put("Purple", "§5");
        LIVID_PREFIX_TO_COLOR_MAP.put("Scream", "§1");
        LIVID_PREFIX_TO_COLOR_MAP.put("Smile", "§a");
        LIVID_PREFIX_TO_COLOR_MAP.put("Vendetta", "§f");
        SECRET_CHEST_ITEMS.add("§5Health Potion VIII Splash Potion");
        SECRET_CHEST_ITEMS.add("§9Spirit Leap");
        SECRET_CHEST_ITEMS.add("§aDecoy");
        SECRET_CHEST_ITEMS.add("§aDefuse Kit");
        SECRET_CHEST_ITEMS.add("§aTraining Weights");
        SECRET_CHEST_ITEMS.add("§aTrap");
        SECRET_CHEST_ITEMS.add("§fInflatable Jerry");
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
        TRASH_ITEMS.add("SILENT_DEATH");
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
        super(configuration);
        c = configuration.config.dungeonTweaks;
    }

    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END) return;

        if (getTicks() % 20 == 0)
        {
            if (c.enableNoFogAutoToggle)
            {
                if (getCurrentIsland() == SkyblockIsland.DUNGEON &&
                        (getCurrentLocationRaw().contains("(F5)") || getCurrentLocationRaw().contains("(M5)")))
                {
                    if (!wasNoFogAutoToggled)
                        if (!c.enableNoFog)
                        {
                            c.enableNoFog = true;
                            wasNoFogAutoToggled = true;
                            sendChat("DT-NoFog: dungeon floor 5 detected, auto toggled on");
                        }
                        else
                            wasNoFogAutoToggled = false;
                }
                else
                {
                    if (c.enableNoFog && wasNoFogAutoToggled)
                    {
                        c.enableNoFog = false;
                        wasNoFogAutoToggled = false;
                        sendChat("DT-NoFog: auto toggled off");
                    }
                }
            }
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
                        float dis = entry.getKey().getDistanceToEntity(getPlayer());

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
                    AxisAlignedBB aabb = targetMob.getEntityBoundingBox().addCoord(0.5, 4.0, 0.5);
                    List<Entity> entities = getWorld().getEntitiesWithinAABB(EntityArmorStand.class, aabb, e -> true);
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

            removeWhile(armorStandsTemp, ele -> getTicks() - ele.a >= 5,
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

                    if (c.trackDamageTags)
                    {
                        try
                        {
                            if (name.startsWith("§f✧") && critTagMatcher.reset(name).matches())
                            {
                                int num = Integer.parseInt(cleanColor(critTagMatcher.group(1)));
                                name = Utils.formatCommas(num);
                                StringBuilder sb = new StringBuilder(35);
                                sb.append("§f✧");
                                int i = 0;
                                for (char c : name.toCharArray())
                                {
                                    if (c == ',')
                                        sb.append(EnumChatFormatting.GRAY);
                                    else
                                        sb.append(KOOL_COLORS[i++ % KOOL_COLORS.length]);
                                    sb.append(c);
                                }
                                sb.append("§f✧");
                                sb.append(critTagMatcher.group(2));
                                addDamageInfo(ele.a, sb.toString());
                            }
                            else if (c.trackWitherDamageTags && witherTagMatcher.reset(name).matches())
                            {
                                int num = Integer.parseInt(cleanColor(witherTagMatcher.group(1)));
                                name = "§0" + Utils.formatCommas(num);
                                addDamageInfo(ele.a, name);
                            }
                            else if (c.trackNonCritDamageTags && nonCritTagMatcher.reset(name).matches())
                            {
                                int num = Integer.parseInt(cleanColor(nonCritTagMatcher.group(1)));
                                name = "§7" + Utils.formatCommas(num) + nonCritTagMatcher.group(2);
                                addDamageInfo(ele.a, name);
                            }
                        }
                        catch (Exception e)
                        {
                            if (!isDamageFormattingExceptionNotified)
                            {
                                isDamageFormattingExceptionNotified = true;
                                sendChat("DT-TrackDamageTags: formatting failed");
                                sendChat(e.toString());
                                e.printStackTrace();
                            }
                        }
                    }
                });
        }

        if (getMc().currentScreen instanceof GuiChest)
        {
            GuiChest chest = (GuiChest) getMc().currentScreen;
            ContainerChest container = (ContainerChest)chest.inventorySlots;
            if (secretChestOpened)
            {
                IInventory inv = container.getLowerChestInventory();
                // Also double chest secret shits
                if (inv.getSizeInventory() == 27)
                {
                    ItemStack center = inv.getStackInSlot(9 + 5 - 1);
                    if (center != null && SECRET_CHEST_ITEMS.contains(center.getDisplayName()))
                    {
                        getPlayer().closeScreen();
                        secretChestOpened = false;
                    }
                }
                else
                    secretChestOpened = false;
            }
            else if (blacksmithMenuOpened && container.getLowerChestInventory().getName().equals("Salvage Item"))
            {
                IInventory inv = container.getLowerChestInventory();
                if (inv.getSizeInventory() == 54)
                {
                    ItemStack item = inv.getStackInSlot(9 * 2 + 5 - 1);
                    if (item != null && !salvageClickSent && getTicks() - salvageLastClickTick >= 15)
                    {
                        String id = Utils.getSkyblockItemId(item);
                        Item firstPane = inv.getStackInSlot(0).getItem();
                        ItemStack salvageBtn = inv.getStackInSlot(9 * 3 + 5 - 1);
                        if (id != null && TRASH_ITEMS.contains(id) &&
                            firstPane != null && Block.getBlockFromItem(firstPane) == Blocks.stained_glass_pane &&
                            salvageBtn != null && salvageBtn.getDisplayName().equals("§aSalvage Item"))
                        {
                            getMc().playerController.windowClick(container.windowId, 9 * 3 + 5 - 1,
                                    0, 0, getPlayer());
                            salvageClickSent = true;
                            salvageLastClickTick = getTicks();
                            String[] lore = getDisplayLore(salvageBtn);
                            if (lore != null)
                                for (String line : lore)
                                {
                                    if (essenceMatcher.reset(line).matches())
                                    {
                                        String ess = essenceMatcher.group(1).toLowerCase();
                                        if (ESSENCES.contains(ess))
                                        {
                                            salvagingEssenceType = ess;
                                            salvagingEssencegAmount = Integer.parseInt(essenceMatcher.group(2));
                                        }
                                        else
                                            salvagingEssenceType = "";
                                        break;
                                    }
                                }
                        }
                    }
                    else
                    {
                        if (salvageClickSent && !salvagingEssenceType.equals(""))
                        {
                            c.salvagedEssences.merge(salvagingEssenceType, salvagingEssencegAmount, Integer::sum);
                            sendChatf("DT-AutoSalvage: salvaged %d %s essences, total: %d", salvagingEssencegAmount,
                                    salvagingEssenceType, c.salvagedEssences.get(salvagingEssenceType));
                        }
                        salvageClickSent = false;
                    }
                }
                else
                    blacksmithMenuOpened = false;
            }
        }
        else
        {
            secretChestOpened = false;
            blacksmithMenuOpened = false;
        }

        if (fragRunTracking)
        {
            Tweakception.globalTracker.updateIslandNow();
            if (getCurrentIsland() != SkyblockIsland.DUNGEON)
            {
                if (fragPendingEndRunWarp || fragGotten)
                {
                    fragPendingEndRunWarp = false;
                    fragGotten = false;
                    fragEnd();
                    if (!c.fragBot.equals(""))
                    {
                        sendChat("DT-Frag: repartying " + c.fragBot);
                        Tweakception.scheduler.addDelayed(() -> getPlayer().sendChatMessage("/p disband"), 20).
                                thenDelayed(() -> getPlayer().sendChatMessage("/p " + c.fragBot), 20);
                    }
                    else
                        sendChat("DT-Frag: cannot reparty, please set a frag bot using `setfragbot <name>`");
                }
                else
                {
                    if (fragrunStartTime != 0L && !isInF7())
                    {
                        fragrunStartTime = 0L;
                        sendChat("DT-Frag: not in f7, run is cancelled!");
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
                    sendChatf("DT-TrackMaskUsage: §ayour §e%s §ais now available!",
                            String.join(" ", ele.getKey().toLowerCase().split("_")));
                    maskUsages.remove(ele.getKey());
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
            RenderUtils.drawDefaultHighlightBoxForEntity(realLivid, new Color(0, 255, 0, 192), false);

        if (c.highlightBats)
            for (Entity bat : bats)
                RenderUtils.drawDefaultHighlightBoxForEntity(bat, new Color(255, 76, 76, 85), false);

        if (c.highlightShadowAssassins)
            for (Entity sa : shadowAssassins)
                RenderUtils.drawDefaultHighlightBoxForEntity(sa, new Color(255, 76, 76, 85), false);

        if (c.highlightSpiritBear && spiritBear != null)
            RenderUtils.drawDefaultHighlightBox(spiritBear, 7, new Color(0, 255, 0, 192), false);

        if (c.highlightStarredMobs)
            for (Entity e : starredMobs)
                RenderUtils.drawDefaultHighlightBoxForEntity(e, RenderUtils.DEFAULT_HIGHLIGHT_COLOR, false);
    }

    public void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event)
    {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        ScaledResolution res = new ScaledResolution(getMc());
        FontRenderer r = getMc().fontRendererObj;
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        if (c.trackDamageTags)
        {
            int x = width - 10;
            int y = height - 20;
            Iterator<Pair<Integer, String>> it = damageTags.descendingIterator();
            while (it.hasNext())
            {
                Pair<Integer, String> ele = it.next();
                r.drawString(ele.b, x - r.getStringWidth(ele.b), y, 0xfff0f0f0);
                y -= r.FONT_HEIGHT;
            }
        }

        if (fragRunTracking)
        {
            int x = width - 10;
            int y = 10;

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

            String runs = "Session total runs: " + fragSessionRuns;
            String sFastest = "Session fastest blood rush: " + Utils.msToMMSSmmm(fragSessionFastestBloodRush);
            String avg = "Session avg run time: " + Utils.msToMMSSmmm(fragSessionTotalTime / Math.max(fragSessionRuns, 1));
            String total = "Session total time: " + Utils.msToHHMMSSmmm(fragSessionTotalTime);
            String total2 = "Total runs: " + c.totalFragruns;
            String fastest = "Fastest run: " + Utils.msToMMSSmmm(c.fastestFragrun);
            String fastestBr = "Fastest blood rush: " + Utils.msToMMSSmmm(c.fastestBloodRush);

            r.drawString(status, x - r.getStringWidth(status), y, 0xffffffff); y += r.FONT_HEIGHT;
            r.drawString(br, x - r.getStringWidth(br), y, 0xffffffff); y += r.FONT_HEIGHT;
            r.drawString(runs, x - r.getStringWidth(runs), y, 0xffffffff); y += r.FONT_HEIGHT;
            r.drawString(sFastest, x - r.getStringWidth(sFastest), y, 0xffffffff); y += r.FONT_HEIGHT;
            r.drawString(avg, x - r.getStringWidth(avg), y, 0xffffffff); y += r.FONT_HEIGHT;
            r.drawString(total, x - r.getStringWidth(total), y, 0xffffffff); y += r.FONT_HEIGHT;
            r.drawString(total2, x - r.getStringWidth(total2), y, 0xffffffff); y += r.FONT_HEIGHT;
            r.drawString(fastest, x - r.getStringWidth(fastest), y, 0xffffffff); y += r.FONT_HEIGHT;
            r.drawString(fastestBr, x - r.getStringWidth(fastestBr), y, 0xffffffff); y += r.FONT_HEIGHT;

            if (fragLastRunDisplayStartTime != 0 && getTicks() - fragLastRunDisplayStartTime <= 20 * 30)
            {
                String lastRun = "§bLast run time: " + fragLastRunTime + "§r" + fragLastRecord;
                String lastBr = "§bLast blood rush: " + fragLastBloodRush;
                r.drawString(lastRun, x - r.getStringWidth(lastRun), y, 0xffffffff); y += r.FONT_HEIGHT;
                r.drawString(lastBr, x - r.getStringWidth(lastBr), y, 0xffffffff); y += r.FONT_HEIGHT;
            }
        }

        if (c.trackShootingSpeed)
        {
            float count = (float)arrowSpawnTimes.size() / c.shootingSpeedTrackingSampleSecs;
            String s = f("Arrows/s: %.3f", count);
            r.drawString(s, width - 60 - r.getStringWidth(s), 10, 0xffffffff);
        }

        if (c.displayTargetMobNameTag)
        {
            if (hitDisplayTargetNameTag != null)
            {
                String name = hitDisplayTargetNameTag.getName();
                r.drawString(name, (width - r.getStringWidth(name)) / 2, 30, 0xffffffff);
            }
        }
    }

    public void onLivingRenderPre(RenderLivingEvent.Pre event)
    {
        if (c.highlightDoorKeys &&
            getCurrentIsland() == SkyblockIsland.DUNGEON &&
            event.entity instanceof EntityArmorStand)
        {
            String tex = McUtils.getArmorStandHeadTexture((EntityArmorStand)event.entity);
            if (tex != null)
            {
                String witherKeyTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzRkYjRhZGZhOWJmNDhmZjVkNDE3MDdhZTM0ZWE3OGJkMjM3MTY1OWZjZDhjZDg5MzQ3NDlhZjRjY2U5YiJ9fX0=";
                String bloodKeyTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjU2MTU5NWQ5Yzc0NTc3OTZjNzE5ZmFlNDYzYTIyMjcxY2JjMDFjZjEwODA5ZjVhNjRjY2IzZDZhZTdmOGY2In19fQ==";

                if (tex.equals(witherKeyTexture))
                    RenderUtils.drawBeaconBeamAtEntity(event.entity, new Color(84, 166, 102, 128));
                else if (tex.equals(bloodKeyTexture))
                    RenderUtils.drawBeaconBeamAtEntity(event.entity, new Color(84, 166, 102, 128));
            }
        }
    }

    public void onLivingSpecialRenderPre(RenderLivingEvent.Specials.Pre event)
    {
        if (event.entity instanceof EntityArmorStand)
        {
            String name = event.entity.getName();

            if (c.hideDamageTags && anyDamageTagMatcher.reset(name).matches())
            {
                event.setCanceled(true);
                return;
            }

            if (getCurrentIsland() == SkyblockIsland.DUNGEON && c.hideNonStarredMobsName)
            {
                if (name.endsWith("§c❤"))
                {
                    boolean isStarred = name.substring(0, Math.min(name.length(), 5)).contains("✯");

                    if (!isStarred)
                        event.setCanceled(true);
                }
            }
        }
    }

    public void onRenderBlockOverlay(DrawBlockHighlightEvent event)
    {
        if (c.gyroWandOverlay)
        {
            ItemStack stack = getPlayer().inventory.getCurrentItem();
            if (stack != null)
            {
                String id = Utils.getSkyblockItemId(stack);
                if (id != null && id.equals("GYROKINETIC_WAND"))
                {
                    RayTraceUtils.RayTraceResult res = RayTraceUtils.rayTraceBlock(
                        getPlayer(), event.partialTicks, 26.0f, 0.1f);
                    if (res != null)
                        RenderUtils.drawBeaconBeamOrBoundingBox(res.pos, new Color(0, 70, 156, 96), event.partialTicks, 1);
                }
            }
        }
    }

    public void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
        if (c.trackDamageTags || c.highlightStarredMobs && getCurrentIsland() == SkyblockIsland.DUNGEON)
        {
            // The custom name doesn't come with the first update
            // So check the name 5 ticks later
            if (event.entity instanceof EntityArmorStand)
            {
                armorStandsTemp.offer(new Pair<>(getTicks(), event.entity));
                return;
            }
        }

        if (c.highlightBats && getCurrentIsland() == SkyblockIsland.DUNGEON)
        {
            if (event.entity instanceof EntityBat)
            {
                bats.add(event.entity);
                return;
            }
        }

        if (getCurrentIsland() == SkyblockIsland.DUNGEON &&
            event.entity instanceof EntityOtherPlayerMP)
        {
            if (c.highlightShadowAssassins &&
                event.entity.getName().equals("Shadow Assassin"))
            {
                shadowAssassins.add(event.entity);
                return;
            }
            if (c.highlightSpiritBear &&
                event.entity.getName().equals("Spirit Bear"))
            {
                spiritBear = event.entity;
                return;
            }
        }

        if (c.trackShootingSpeed)
        {
            if (event.entity instanceof EntityArrow)
            {
                if (event.entity.getDistanceToEntity(getPlayer()) <= c.shootingSpeedTrackingRange)
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
                ItemStack item = getPlayer().inventory.getCurrentItem();
                if (item != null)
                {
                    String name = item.getDisplayName();
                    if (c.blockRightClickItemNames.contains(name))
                    {
                        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU))
                        {
                            sendChat("DT-BlockRightClick: overrode block click for item (" + name + "§r)");
                        }
                        else
                        {
                            event.setCanceled(true);
                            sendChat("DT-BlockRightClick: blocked click for item (" + name + "§r), hold alt to override it");
                        }
                    }
                }
            }
        }
    }

    public void onPacketCollectItem(S0DPacketCollectItem packet)
    {
        if (getCurrentIsland() != SkyblockIsland.DUNGEON) return;

        Entity player = getWorld().getEntityByID(packet.getEntityID());
        Entity entity = getWorld().getEntityByID(packet.getCollectedItemEntityID());

        if (fragRunTracking && (player == null || player == getPlayer()))
        {
            if (entity instanceof EntityItem)
            {
                EntityItem itemEntity = (EntityItem)entity;
                String id = Utils.getSkyblockItemId(itemEntity.getEntityItem());
                if (id != null && FRAGS_AND_NAMES.containsKey(id) && !fragGotten)
                {
                    fragGotten = true;
                    c.fragDrops.merge(id, 1, Integer::sum);
                    sendChatf("DT-Frag: %s obtained, count: %d", FRAGS_AND_NAMES.get(id), c.fragDrops.get(id));
                }
            }
        }
    }

    public void onPacketEntityStatus(S19PacketEntityStatus packet)
    {
        if (c.displayTargetMobNameTag && packet.getOpCode() == 2)
        {
            Entity e = packet.getEntity(getWorld());
            if (e != null)
            {
                if (entityHurtTimes.containsKey(e))
                    entityHurtTimes.get(e).offer(getTicks());
                else
                {
                    ConcurrentLinkedQueue<Integer> q = new ConcurrentLinkedQueue<>();
                    q.offer(getTicks());
                    entityHurtTimes.put(e, q);
                }
            }
        }
    }

    public void onGuiOpen(GuiOpenEvent event)
    {
        if (event.gui instanceof GuiChest)
        {
            GuiChest chest = (GuiChest)event.gui;
            ContainerChest container = (ContainerChest)chest.inventorySlots;
            String containerName = container.getLowerChestInventory().getName();
            if (getCurrentIsland() == SkyblockIsland.DUNGEON && c.autoCloseSecretChest && containerName.equals("Chest"))
                secretChestOpened = true;
            else if (c.autoSalvage && containerName.equals("Dungeon Blacksmith"))
            {
                blacksmithMenuOpened = true;
                salvageClickSent = false;
                salvagingEssenceType = "";
            }
        }
    }

    public void onChatReceived(ClientChatReceivedEvent event)
    {
        if (event.type != 0) return;

        String msg = event.message.getUnformattedText();
        if (getCurrentIsland() == SkyblockIsland.DUNGEON && msg.startsWith("[BOSS]"))
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
        else if (getCurrentIsland() == SkyblockIsland.DUNGEON && msg.equals("Dungeon starts in 1 second."))
        {
            Tweakception.globalTracker.updateIslandNow();
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
        else if (getCurrentIsland() == SkyblockIsland.DUNGEON && msg.startsWith("Your ") && msg.endsWith(" saved your life!"))
        {
            ItemStack head = getPlayer().getCurrentArmor(3);
            if (head != null)
            {
                String id = Utils.getSkyblockItemId(head);
//                String uuid = getSkyblockItemUuid(head);
                if (id != null && MASKS.contains(id))
                {
                    String[] lore = getDisplayLore(head);
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
                        sendChat("DT-TrackMaskUsage: cannot retrieve the lore of your mask, this will not be tracked!");
                    }
                }
                else
                {
                    sendChat("DT-TrackMaskUsage: cannot retrieve the id of your mask (or not registered), this will not be tracked!");
                }
            }
        }
        else if (c.autoJoinParty &&
                msg.startsWith("-----------------------------------------------------"))
        {
            if (partyRequestMatcher.reset(msg).find())
            {
                String name = partyRequestMatcher.group(1)/*.replaceAll("[.*]", "").trim()*/.toLowerCase();
                if (c.autoJoinPartyOwners.contains(name))
                {
                    sendChat("DT-AutoJoinParty: joining " + name + "'s party");
                    getPlayer().sendChatMessage("/p " + name);
                }
            }
        }
    }
    public void onItemTooltip(ItemTooltipEvent event)
    {
        if (event.itemStack == null || event.toolTip == null) return;

        String id = Utils.getSkyblockItemId(event.itemStack);
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
        else if (getMc().currentScreen instanceof GuiChest)
        {
            GuiChest chest = (GuiChest) getMc().currentScreen;
            ContainerChest container = (ContainerChest)chest.inventorySlots;
            String containerName = container.getLowerChestInventory().getName();
            if (containerName.equals("Party Finder"))
            {
                boolean ctrlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL);

                if (c.partyFinderPlayerBlacklist.size() > 0 &&
                    event.toolTip.size() > 0 && partyFinderTitleMatcher.reset(event.toolTip.get(0)).find())
                {
                    String name = partyFinderTitleMatcher.group(1);
                    if (c.partyFinderPlayerBlacklist.containsKey(name.toLowerCase()))
                    {
                        String reason = c.partyFinderPlayerBlacklist.get(name.toLowerCase());
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
                        StringBuilder sb = new StringBuilder();
                        boolean blacklisted = c.partyFinderPlayerBlacklist.containsKey(name.toLowerCase());

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
                            if (c.partyFinderQuickPlayerInfoShowSecretPerExp)
                                sb.append(f("/%.1f", stats.totalSecret / (stats.cataExp / 50000.0f)));

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
                            String reason = c.partyFinderPlayerBlacklist.get(name.toLowerCase());
                            if (ctrlDown && !reason.equals(""))
                                sb.append(" §8(").append(reason).append(")");
                        }

                        event.toolTip.set(i, sb.toString());
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
                addDamageInfo(getTicks(), "§f✧§4STRIKE§f✧");
        }
    }

    public void onFogDensitySet(EntityViewRenderEvent.FogDensity event)
    {
        if (c.enableNoFog)
        {
            event.density = 0.0f;
            event.setCanceled(true);
        }
    }

    public void onWorldUnload(WorldEvent.Unload event)
    {
        if (isInF5Bossfight)
        {
            resetLivid();
            isInF5Bossfight = false;
        }
        if (c.highlightStarredMobs)
            starredMobs.clear();
        if (c.highlightSpiritBear)
            spiritBear = null;
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
    }

    public void onIslandChanged(IslandChangedEvent event)
    {
    }

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

    private void addDamageInfo(int spawnTicks, String s)
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
                if (GlobalTracker.t)
                    Utils.setClipboard(DumpUtils.prettifyJson(items.toString()));
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
                            if (id.equals("HYPERION") || id.equals("SCYLLA") || id.equals("VALKYRIE") || id.equals("ASTRAEA"))
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

        DungeonStats stats = new DungeonStats(cata, cataExp, secretsPerRun, (long)secrets, wBlade, term, apiDisabled);
        uuidToDungeonStatsMap.put(uuid, stats);
        return stats;
    }

    public void toggleNoFog()
    {
        c.enableNoFog = !c.enableNoFog;
        sendChat("DT-NoFog: toggled " + c.enableNoFog);
    }

    public void toggleNoFogAutoToggle()
    {
        c.enableNoFogAutoToggle = !c.enableNoFogAutoToggle;
        sendChat("DT-NoFog: toggled auto toggle " + c.enableNoFogAutoToggle);
    }

    public void toggleHideName()
    {
        c.hideNonStarredMobsName = !c.hideNonStarredMobsName;
        sendChat("DT-HideName: toggled hide name " + c.hideNonStarredMobsName);
    }

    public void toggleHideDamageTags()
    {
        c.hideDamageTags = !c.hideDamageTags;
        sendChatf("DT-HideDamageTags: toggled " + c.hideDamageTags);
    }

    public void toggleHighlightStarredMobs()
    {
        c.highlightStarredMobs = !c.highlightStarredMobs;
        sendChat("DT-HighlightStarredMobs: toggled " + c.highlightStarredMobs);
    }

    public void toggleHighlightSpiritBear()
    {
        c.highlightSpiritBear = !c.highlightSpiritBear;
        sendChat("DT-HighlightSpiritBear: toggled " + c.highlightSpiritBear);
    }

    public void toggleHighlightShadowAssassin()
    {
        c.highlightShadowAssassins = !c.highlightShadowAssassins;
        sendChat("DT-HighlightShadowAssassin: toggled " + c.highlightShadowAssassins);
    }

    public void toggleHighlightBats()
    {
        c.highlightBats = !c.highlightBats;
        sendChat("DT-HighlightBats: toggled " + c.highlightBats);

        if (c.highlightBats)
        {
            bats.addAll(getWorld().getEntities(EntityBat.class, e -> true));
        }
    }

    public void toggleHighlightDoorKeys()
    {
        c.highlightDoorKeys = !c.highlightDoorKeys;
        sendChat("DT-HighlightDoorKeys: toggled " + c.highlightDoorKeys);
    }

    public void blockRightClickSet()
    {
        ItemStack item = getPlayer().inventory.getCurrentItem();
        if (item == null)
        {
            sendChat("DT-BlockRightClick: current selected item is empty");
            return;
        }
        String name = item.getDisplayName();
        if (c.blockRightClickItemNames.contains(name))
        {
            c.blockRightClickItemNames.remove(name);
            sendChat("DT-BlockRightClick: removed item \"" + name  + "§r\" from block list");
        }
        else
        {
            c.blockRightClickItemNames.add(name);
            sendChat("DT-BlockRightClick: added item \"" + name  + "§r\" to block list");
        }
    }

    public void blockRightClickRemove(int i)
    {
        if (i < 1 || i > c.blockRightClickItemNames.size())
        {
            sendChat("DT-BlockRightClick: index out of bounds");
            return;
        }
        String ele = c.blockRightClickItemNames.toArray(new String[0])[i - 1];
        String chatActionUuid = Tweakception.globalTracker.registerChatAction(() ->
        {
            if (c.blockRightClickItemNames.contains(ele))
            {
                c.blockRightClickItemNames.remove(ele);
                sendChat("DT-BlockRightClick: removed \"" + ele + "§r\" from the list");
            }
        }, 20 * 5, null);

        IChatComponent nice = new ChatComponentText(
            "DT-BlockRightClick: you really wanna remove \"" + ele + "§r\" from the list? Click here in 5 seconds to continue");
        nice.getChatStyle().setChatClickEvent(
            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tc action " + chatActionUuid));
        getPlayer().addChatMessage(nice);
    }

    public void blockRightClickList()
    {
        if (c.blockRightClickItemNames.isEmpty())
        {
            sendChat("DT-BlockRightClick: list is empty");
            return;
        }
        sendChat("DT-BlockRightClick: there are " + c.blockRightClickItemNames.size() + " items in the list");
        int i = 1;
        for (String s : c.blockRightClickItemNames)
            sendChat(i++ + ": " + s);
    }

    public void toggleTrackDamageTags()
    {
        c.trackDamageTags = !c.trackDamageTags;
        sendChatf("DT-TrackDamageTags: toggled %b, track non crit %b", c.trackDamageTags, c.trackNonCritDamageTags);
        if (c.trackDamageTags)
        {
            damageTags.clear();
            armorStandsTemp.clear();
        }
    }

    public void toggleTrackNonCritDamageTags()
    {
        c.trackNonCritDamageTags = !c.trackNonCritDamageTags;
        sendChat("DT-TrackDamageTags: toggled track non crit " + c.trackNonCritDamageTags);
        if (c.trackNonCritDamageTags)
            sendChat("DT-TrackDamageTags: (notice: this also tracks damages to you)");
        if (!c.trackDamageTags)
            sendChat("DT-TrackDamageTags: (notice: overall tracking isn't on!)");
    }

    public void toggleTrackWitherDamageTags()
    {
        c.trackWitherDamageTags = !c.trackWitherDamageTags;
        sendChat("DT-TrackDamageTags: toggled track wither damage " + c.trackWitherDamageTags);
        if (!c.trackDamageTags)
            sendChat("DT-TrackDamageTags: (notice: overall tracking isn't on!)");
    }

    public void setDamageTagTrackingCount(int count)
    {
        c.damageTagTrackingCount = count > 0 ? count : new DungeonTweaksConfig().damageTagTrackingCount;
        sendChat("DT-TrackDamageTags: set count to " + c.damageTagTrackingCount);
        damageTags.clear();
        armorStandsTemp.clear();
    }

    public void setDamageTagHistoryTimeoutTicks(int ticks)
    {
        c.damageTagHistoryTimeoutTicks = ticks > 0 ? ticks : new DungeonTweaksConfig().damageTagHistoryTimeoutTicks;
        sendChat("DT-TrackDamageTags: set history timeout to " + c.damageTagHistoryTimeoutTicks + " ticks");
    }

    public void toggleAutoCloseSecretChest()
    {
        c.autoCloseSecretChest = !c.autoCloseSecretChest;
        sendChat("DT-AutoCloseSecretChest: toggled " + c.autoCloseSecretChest);
    }

    public void toggleAutoSalvage()
    {
        c.autoSalvage = !c.autoSalvage;
        sendChat("DT-AutoSalvage: toggled " + c.autoSalvage);
    }

    public void toggleAutoJoinParty()
    {
        c.autoJoinParty = !c.autoJoinParty;
        sendChat("DT-AutoJoinParty: toggled " + c.autoJoinParty);
    }

    public void autoJoinPartyList()
    {
        if (c.autoJoinPartyOwners.isEmpty())
        {
            sendChat("DT-AutoJoinParty: trusted player list is empty");
            return;
        }
        sendChat("DT-AutoJoinParty: there are " + c.autoJoinPartyOwners.size() + " players in the list");
        int i = 1;
        for (String name : c.autoJoinPartyOwners)
            sendChat(i++ + ": " + name);
    }

    public void autoJoinPartyAdd(String name)
    {
        if (name == null || name.equals(""))
        {
            sendChat("DT-AutoJoinParty: give me a player name");
            return;
        }
        name = name.toLowerCase();
        if (c.autoJoinPartyOwners.contains(name))
        {
            sendChat("DT-AutoJoinParty: player " + name + " is already in the list");
        }
        else
        {
            c.autoJoinPartyOwners.add(name);
            sendChat("DT-AutoJoinParty: added " + name);
        }
    }

    public void autoJoinPartyRemove(String name)
    {
        if (name == null || name.equals(""))
        {
            sendChat("DT-AutoJoinParty: give me a player name");
            return;
        }
        name = name.toLowerCase();
        if (c.autoJoinPartyOwners.contains(name))
        {
            c.autoJoinPartyOwners.remove(name);
            sendChat("DT-AutoJoinParty: removed " + name);
        }
        else
        {
            sendChat("DT-AutoJoinParty: player " + name + " is not in the list");
        }
    }

    public void listFragCounts()
    {
        for (Map.Entry<String, Integer> e : c.fragDrops.entrySet())
        {
            sendChatf("§5%s§f: §a%d", FRAGS_AND_NAMES.get(e.getKey()), e.getValue());
        }
    }

    public void fragStartSession()
    {
        if (fragRunTracking)
        {
            sendChat("DT-Frag: you're already in a session, use `endsession` to end");
            return;
        }
        if (c.fragBot.equals(""))
        {
            sendChat("DT-Frag: please set a frag bot first using `setfragbot <name>`");
            return;
        }

        sendChat("DT-Frag: starting session, timer will start when you start f7");

        fragRunTracking = true;
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
            sendChat("DT-Frag: you've not started a session");
            return;
        }

        if (isInF7())
        {
            fragEnd();
        }

        fragRunTracking = false;

        sendChat("DT-Frag: ending session, life total runs: " + c.totalFragruns);

        if (fragSessionRuns == 0)
            return;

        sendChat("DT-Frag: session runs: " + fragSessionRuns);
        sendChat("DT-Frag: session fastest blood rush: " + Utils.msToMMSSmmm(fragSessionFastestBloodRush));
        sendChat("DT-Frag: session total time: " + Utils.msToHHMMSSmmm(fragSessionTotalTime));
        sendChat("DT-Frag: session average run time: " + Utils.msToMMSSmmm(fragSessionTotalTime / fragSessionRuns));
    }

    public void fragNext()
    {
        if (getCurrentIsland() != SkyblockIsland.DUNGEON &&
            !(getCurrentLocationRaw().contains("(F7)") || getCurrentLocationRaw().contains("(M7)")))
        {
            sendChat("DT-Frag: floor 7 not detected");
            return;
        }
        if (!fragRunTracking)
        {
            sendChat("DT-Frag: please start a session first using `startsession`, and the timer will start");
            return;
        }

        sendChat("DT-Frag: warping back to dhub for next run");

        fragPendingEndRunWarp = true;
        getPlayer().sendChatMessage("/warp dhub");
        // Continued at `if (fragPendingEndRunWarp)` in onTick()
    }

    private void fragStart()
    {
        fragrunStartTime = System.currentTimeMillis();
        sendChat("DT-Frag: started the next run");

        // To prevent active run being cancelled when starting before the island is updated
        Tweakception.globalTracker.updateIslandNow();
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

        sendChatf("DT-Frag: run took %s§r%s", fragLastRunTime, fragLastRecord);
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
                sendChat("DT-Frag: removed frag bot");
            else
                sendChat("DT-Frag: set a frag bot using `setfragbot <name>`, or remove the existing frag bot using `setfragbot`");
        }
        else
        {
            c.fragBot = name;
            sendChat("DT-Frag: set frag bot to " + name);
        }
    }

    public void toggleTrackShootingSpeed()
    {
        c.trackShootingSpeed = !c.trackShootingSpeed;
        sendChat("DT-TrackShootingSpeed: toggled " + c.trackShootingSpeed);
    }

    public void setShootingSpeedTrackingSampleSecs(int secs)
    {
        c.shootingSpeedTrackingSampleSecs = secs > 0 ? secs : new DungeonTweaksConfig().shootingSpeedTrackingSampleSecs;
        sendChat("DT-TrackShootingSpeed: set sample secs to " + c.shootingSpeedTrackingSampleSecs);
    }

    public void setShootingSpeedTrackingRange(int blocks)
    {
        c.shootingSpeedTrackingRange = blocks > 0 ? blocks : new DungeonTweaksConfig().shootingSpeedTrackingRange;
        sendChat("DT-TrackShootingSpeed: set spawn range to " + c.shootingSpeedTrackingRange);
    }

    public void toggleDisplayMobNameTag()
    {
        c.displayTargetMobNameTag = !c.displayTargetMobNameTag;
        sendChat("DT-DisplayMobNameTag: toggled " + c.displayTargetMobNameTag);
    }

    public void toggleTrackMaskUsage()
    {
        c.trackMaskUsage = !c.trackMaskUsage;
        sendChat("DT-TrackMaskUsage: toggled " + c.trackMaskUsage);
    }

    public void toggleBlockOpheliaShopClicks()
    {
        c.blockOpheliaShopClicks = !c.blockOpheliaShopClicks;
        sendChat("DT-BlockOpheliaShopClicks: toggled " + c.blockOpheliaShopClicks);
    }

    public void partyFinderQuickPlayerInfoToggle()
    {
        c.partyFinderDisplayQuickPlayerInfo = !c.partyFinderDisplayQuickPlayerInfo;
        sendChat("DT-PartyFinderDisplayQuickPlayerInfo: toggled " + c.partyFinderDisplayQuickPlayerInfo);
    }

    public void partyFinderQuickPlayerInfoToggleShowSecretPerExp()
    {
        c.partyFinderQuickPlayerInfoShowSecretPerExp = !c.partyFinderQuickPlayerInfoShowSecretPerExp;
        sendChat("DT-PartyFinderDisplayQuickPlayerInfo: toggled secrets per 50k exp " +
                c.partyFinderQuickPlayerInfoShowSecretPerExp);
    }

    public void partyFinderPlayerBlacklistSet(String name, String reason)
    {
        if (name.equals(""))
        {
            sendChatf("DT-PartyFinderPlayerBlacklist: there are %d players in the list",
                    c.partyFinderPlayerBlacklist.size());
            int i = 1;
            for (Map.Entry<String, String> s : c.partyFinderPlayerBlacklist.entrySet())
                sendChatf("%d: %s%s", i++, s.getKey(), s.getValue().equals("") ? "" : ", reason: " + s.getValue());
            return;
        }

        name = name.toLowerCase();
        if (c.partyFinderPlayerBlacklist.containsKey(name))
        {
            c.partyFinderPlayerBlacklist.remove(name);
            sendChat("DT-PartyFinderPlayerBlacklist: removed " + name);
        }
        else
        {
            c.partyFinderPlayerBlacklist.put(name, reason);
            sendChat("DT-PartyFinderPlayerBlacklist: added " + name + (reason.equals("") ? "" : ", reason: " + reason));
        }
    }

    public void freeCaches()
    {
        uuidToDungeonStatsMap.clear();
        sendChat("DT: cleared caches");
    }

    public void toggleGyroWandOverlay()
    {
        c.gyroWandOverlay = !c.gyroWandOverlay;
        sendChat("DT-GyroWandOverlay: toggled " + c.gyroWandOverlay);
    }
}
