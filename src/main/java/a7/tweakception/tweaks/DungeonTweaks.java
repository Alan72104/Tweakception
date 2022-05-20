package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Timer;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static a7.tweakception.tweaks.GlobalTracker.*;
import static a7.tweakception.utils.McUtils.*;

public class DungeonTweaks extends Tweak
{
    private final DungeonTweaksConfig c;
    public static class DungeonTweaksConfig
    {
        public boolean enableNoFog = false;
        public boolean enableNoFogAutoToggle = false;
        public boolean hideNonStarredMobsName = true;
        public boolean highlightStarredMobs = false;
        public boolean highlightBats = true;
        public boolean highlightSpiritBear = true;
        public boolean highlightShadowAssassins = true;
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
        public Set<String> autoJoinPartyOwners = new HashSet<>(Arrays.asList("alan72104"));
        public Map<String, Integer> fragDrops = FRAGS_AND_NAMES.keySet().stream().collect(Collectors.toMap(e -> e, e -> 0));
        public String fragBot = "";
        public long fastestFragrun = 0L;
        public int totalFragruns = 0;
        public boolean trackShootingSpeed = false;
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
    private static final String SHADOW_ASSASSIN_SKIN_PATH = "skins/3399e00f404411e465d74388df132d51fe868ecf86f1c073faffa1d9172ec0f3";

    private static final SimpleDateFormat DATE_MMSS = new SimpleDateFormat("mm:ss");
    private static final SimpleDateFormat DATE_MMSSSS = new SimpleDateFormat("mm:ss.SS");
    private static boolean isDamageFormattingExceptionNotified = false;
    private static boolean isGetFieldExceptionNotified = false;
    static
    {
        FRAGS_AND_NAMES.put("GIANT_FRAGMENT_DIAMOND", "Diamante's Handle");
        FRAGS_AND_NAMES.put("GIANT_FRAGMENT_LASER", "L.A.S.R.'s Eye");
        FRAGS_AND_NAMES.put("GIANT_FRAGMENT_BIGFOOT", "Bigfoot's Lasso");
        FRAGS_AND_NAMES.put("GIANT_FRAGMENT_BOULDER", "Jolly Pink Rock");
    }
    private boolean wasNoFogAutoToggled = false;
    private boolean isInF5Bossfight = false;
    private String realLividName;
    private Entity realLivid;
    private final Set<String> knownLivids = new HashSet<>();
    private boolean lividFound = false;
    private final List<Entity> bats = new LinkedList<>();
    private final List<Entity> shadowAssassins = new LinkedList<>();
    private final LinkedList<Pair<Integer, String>> damageTags = new LinkedList<>();
    private final LinkedList<Pair<Integer, Entity>> damageTagsTemp = new LinkedList<>();
    private final Matcher critTagMatcher = Pattern.compile("§f✧((?:§.\\d)+)§.✧(.*)").matcher(""); // §f✧§a6§b7§c8§a✧§d♥
    private final Matcher nonCritTagMatcher = Pattern.compile("§7(\\d+)(.*)").matcher(""); // §712345
    private final Matcher witherTagMatcher = Pattern.compile("§0(\\d+)$").matcher(""); // §012345
    private boolean secretChestOpened = false;
    private boolean blacksmithMenuOpened = false;
    private boolean salvageClickSent = false;
    private int salvageLastClickTick = 0;
    private String salvagingEssenceType = "";
    private int salvagingEssencegAmount = 0;
    private final Matcher essenceMatcher = Pattern.compile(" {2}§[\\da-f](\\w+) Essence §[\\da-f]x(\\d+)").matcher("");
    private final Matcher partyRequestMatcher = Pattern.compile(" (.*) has invited you to join (?:their|.*) party!").matcher("");
    private boolean fragGotten = false;
    private long fragrunStartTime = 0L;
    private int fragSessionRuns = 0;
    private long fragSessionTotalTime = 0L;
    private boolean fragPendingEndRunWarp = false;
    private int fragPendingEndRunStartTime = 0;
    private final Queue<Integer> arrowSpawnTimes = new ArrayDeque<>();
    public boolean t = false;

    public DungeonTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.dungeonTweaks;
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
    }

    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            if (getTicks() % 20 == 0)
            {
                if (c.enableNoFogAutoToggle)
                {
                    if (getCurrentIsland() == SkyblockIsland.DUNGEON &&
                            (getCurrentLocationRawCleaned().contains("(F5)") || getCurrentLocationRawCleaned().contains("(M5)")))
                    {
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

            bats.removeIf(e -> e.isDead);
            shadowAssassins.removeIf(e -> e.isDead);

            if (c.trackDamageTags)
            {
                Iterator<Pair<Integer, Entity>> it = damageTagsTemp.iterator();
                while (it.hasNext())
                {
                    Pair<Integer, Entity> p = it.next();
                    int elapsed = getTicks() - p.a;
                    if (elapsed < 5)
                        break;
                    else
                    {
                        String s = p.b.getName();
                        try
                        {
                            if (s.startsWith("§f✧") && critTagMatcher.reset(s).matches())
                            {
                                int num = Integer.parseInt(cleanColor(critTagMatcher.group(1)));
                                s = NumberFormat.getIntegerInstance().format(num);
                                StringBuilder sb = new StringBuilder(35);
                                sb.append("§f✧");
                                int i = 0;
                                for (char c : s.toCharArray())
                                {
                                    if (c == ',')
                                        sb.append(EnumChatFormatting.GRAY);
                                    else
                                        sb.append(KOOL_COLORS[i++ % KOOL_COLORS.length]);
                                    sb.append(c);
                                }
                                sb.append("§f✧");
                                sb.append(critTagMatcher.group(2));
                                addDamageInfo(p.a, sb.toString());
                            }
                            else if (c.trackWitherDamageTags && witherTagMatcher.reset(s).matches())
                            {
                                int num = Integer.parseInt(cleanColor(witherTagMatcher.group(1)));
                                s = "§0" + NumberFormat.getIntegerInstance().format(num);
                                addDamageInfo(p.a, s);
                            }
                            else if (c.trackNonCritDamageTags && nonCritTagMatcher.reset(s).matches())
                            {
                                int num = Integer.parseInt(cleanColor(nonCritTagMatcher.group(1)));
                                s = "§7" + NumberFormat.getIntegerInstance().format(num) + nonCritTagMatcher.group(2);
                                addDamageInfo(p.a, s);
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
                        it.remove();
                    }
                }

                Iterator<Pair<Integer, String>> it2 = damageTags.descendingIterator();
                while (it2.hasNext())
                {
                    int elapsed = getTicks() - it2.next().a;
                    if (elapsed > c.damageTagHistoryTimeoutTicks)
                        it2.remove();
                    else
                        break;
                }
            }

            if (getMc().currentScreen instanceof GuiChest)
            {
                GuiChest chest = (GuiChest)getMc().currentScreen;
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
                            String id = getSkyblockItemId(item);
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
//                                        sendChat("Salvaged click sent");
                                NBTTagCompound nbt = salvageBtn.getTagCompound();
                                if (nbt != null)
                                {
                                    NBTTagCompound display = nbt.getCompoundTag("display");
                                    if (display != null)
                                    {
                                        NBTTagList lore = display.getTagList("Lore", Constants.NBT.TAG_STRING);
                                        if (lore != null)
                                        {
                                            for (int i = 0; i < lore.tagCount(); i++)
                                            {
                                                String line = lore.getStringTagAt(i);
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
                                                }
                                            }
                                        }
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

            if (fragPendingEndRunWarp)
            {
                if (getCurrentIsland() == SkyblockIsland.DUNGEON_HUB)
                {
                    fragPendingEndRunWarp = false;
                    if (!c.fragBot.equals(""))
                    {
                        sendChat("DT-Frag: repartying " + c.fragBot);
                        Tweakception.scheduler.addDelayed(() -> getPlayer().sendChatMessage("/p disband"), 20).
                                thenDelayed(() -> getPlayer().sendChatMessage("/p " + c.fragBot), 10);
                    }
                    else
                        sendChat("DT-Frag: please set a frag bot using `setfragbot <name>`");
                    fragEnd();
                    fragrunStartTime = System.currentTimeMillis();
                }
                else if (getTicks() - fragPendingEndRunStartTime >= 20 * 10)
                {
                    fragPendingEndRunWarp = false;
                    sendChat("DT-Frag: still not warped back to dhub, try doing it again");
                }
            }

            if (c.trackShootingSpeed)
            {
                while (arrowSpawnTimes.size() > 0)
                {
                    int cur = arrowSpawnTimes.peek();
                    if (getTicks() - cur > 20 * 2)
                        arrowSpawnTimes.remove();
                    else
                        break;
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

        if (c.highlightBats)
        {
            for (Entity bat : bats)
            {
                RenderUtils.drawHighlightBox(bat, AxisAlignedBB.fromBounds(-0.3, -0.5, -0.3, 0.3, 0.5, 0.3),
                        new Color(255, 76, 76, 85), event.partialTicks, false);
            }
        }

        if (c.highlightShadowAssassins)
        {
            for (Entity sa : shadowAssassins)
            {
                RenderUtils.drawHighlightBox(sa, AxisAlignedBB.fromBounds(-0.4, 0, -0.4, 0.4, 1.8, 0.4),
                        new Color(255, 76, 76, 85), event.partialTicks, false);
            }
        }
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
            for (Pair<Integer, String> s : damageTags)
            {
                r.drawString(s.b, x - r.getStringWidth(s.b), y, 0xfff0f0f0);
                y -= r.FONT_HEIGHT;
            }
        }

        if (fragrunStartTime != 0L)
        {
            long elapsed = System.currentTimeMillis() - fragrunStartTime;
            String time = "run time: " + DATE_MMSS.format(new Date(elapsed));
            String runs = "session total runs: " + fragSessionRuns;
            String total = "session total time: " + DATE_MMSS.format(new Date(fragSessionTotalTime));
            String total2 = "total runs: " + c.totalFragruns;
            r.drawString(time, width - 10 - r.getStringWidth(time), 10, 0xffffffff);
            r.drawString(runs, width - 10 - r.getStringWidth(runs), 10 + r.FONT_HEIGHT, 0xffffffff);
            r.drawString(total, width - 10 - r.getStringWidth(total), 10 + r.FONT_HEIGHT * 2, 0xffffffff);
            r.drawString(total2, width - 10 - r.getStringWidth(total2), 10 + r.FONT_HEIGHT * 3, 0xffffffff);
        }

        if (c.trackShootingSpeed)
        {
            float count = arrowSpawnTimes.size() / 2.0f;
            String s = f("Arrows/s: %.3f", count);
            r.drawString(s, width - 60 - r.getStringWidth(s), 10, 0xffffffff);
        }
    }

    public void onLivingRenderPost(RenderLivingEvent.Post event)
    {
    }

    // Called on RenderLivingEntity.renderName()
    public void onLivingSpecialRenderPre(RenderLivingEvent.Specials.Pre event)
    {
        if (getCurrentIsland() != SkyblockIsland.DUNGEON) return;

        if (c.hideNonStarredMobsName || c.highlightStarredMobs || c.highlightSpiritBear)
        {
            if (event.entity instanceof EntityArmorStand)
            {
                String name = event.entity.getName();
                if (name.endsWith("§c❤"))
                {
                    boolean isStarred = name.contains("✯");
                    boolean isSpiritBear = name.startsWith("§c§d§lSpirit Bear");
                    boolean isShadowAssassin = name.startsWith("§c§d§lShadow Assassin");

                    if (c.highlightStarredMobs && isStarred || c.highlightSpiritBear && isSpiritBear ||
                        c.highlightShadowAssassins && isShadowAssassin)
                        highLightMobFromNametag(event.entity);

                    if (c.hideNonStarredMobsName && !isStarred && !isShadowAssassin)
                        event.setCanceled(true);
                }
            }
        }
    }

    private void highLightMobFromNametag(EntityLivingBase entity)
    {
        try
        {
            Timer timer = McUtils.setAccessibleAndGetField(getMc(), "field_71428_T" /* timer */);
            RenderUtils.drawHighlightBox(entity, AxisAlignedBB.fromBounds(-0.4, 0.0, -0.4, 0.4, -2.0, 0.4),
                    new Color(0, 255, 0, 85), timer.renderPartialTicks, false);
        }
        catch (Exception e)
        {
            if (!isGetFieldExceptionNotified)
            {
                isGetFieldExceptionNotified = true;
                sendChat("DT-HighlightStarredMobs: getField failed");
                sendChat(e.toString());
                e.printStackTrace();
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
            // The custom name doesn't come with the first update
            // So detect the name 5 ticks later
            if (event.entity instanceof EntityArmorStand)
            {
                damageTagsTemp.add(new Pair<>(getTicks(), event.entity));
                return;
            }
        }

        if (c.highlightShadowAssassins)
        {
            if (event.entity instanceof EntityOtherPlayerMP)
            {
//                EntityOtherPlayerMP player = (EntityOtherPlayerMP)event.entity;
                if (event.entity.getName().equals("Shadow Assassin"))
                {
                    shadowAssassins.add(event.entity);
                    return;
                }
            }
        }

        if (c.trackShootingSpeed)
        {
            if (event.entity instanceof EntityArrow)
            {
                if (event.entity.getDistanceToEntity(getPlayer()) <= 4.0f)
                {
                    arrowSpawnTimes.add(getTicks());
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
                            sendChat("DT-BlockRightClick: overrode block click for item (" + name + EnumChatFormatting.RESET + ")");
                        }
                        else
                        {
                            event.setCanceled(true);
                            sendChat("DT-BlockRightClick: blocked click for item (" + name + EnumChatFormatting.RESET + "), hold alt to override it");
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

        if (player == null || player == getPlayer())
        {
            if (entity instanceof EntityItem)
            {
                EntityItem itemEntity = (EntityItem)entity;
                String id = getSkyblockItemId(itemEntity.getEntityItem());
                if (id != null && FRAGS_AND_NAMES.containsKey(id) && !fragGotten)
                {
                    fragGotten = true;
                    c.fragDrops.merge(id, 1, Integer::sum);
                    sendChatf("DT-Frag: %s obtained, count: %d", FRAGS_AND_NAMES.get(id), c.fragDrops.get(id));
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
            if (c.autoCloseSecretChest && containerName.equals("Chest") && getCurrentIsland() == SkyblockIsland.DUNGEON)
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
        if (event.type == 0)
        {
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
            else if (msg.equals("[NPC] Mort: Good luck."))
            {
                fragGotten = false;
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
        if (c.highlightBats)
            bats.clear();
        if (c.highlightShadowAssassins)
            shadowAssassins.clear();
        fragGotten = false;
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

    private void addDamageInfo(int tick,String s)
    {
        damageTags.addFirst(new Pair<>(tick, s));
        if (damageTags.size() > c.damageTagTrackingCount)
            damageTags.removeLast();
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
            sendChat("DT-BlockRightClick: removed item (" + name + EnumChatFormatting.RESET + ") from block list");
        }
        else
        {
            c.blockRightClickItemNames.add(name);
            sendChat("DT-BlockRightClick: added item (" + name + EnumChatFormatting.RESET + ") to block list");
        }
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
            damageTagsTemp.clear();
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
        c.damageTagTrackingCount = count;
        sendChat("DT-TrackDamageTags: set count to " + c.damageTagTrackingCount);
        damageTags.clear();
        damageTagsTemp.clear();
    }

    public void setDamageTagHistoryTimeoutTicks(int ticks)
    {
        c.damageTagHistoryTimeoutTicks = ticks;
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
        if (fragrunStartTime != 0L)
        {
            sendChat("DT-Frag: you're already in a session, use `endsession` to end");
            return;
        }
        if (c.fragBot.equals(""))
        {
            sendChat("DT-Frag: please set a frag bot first using `setfragbot <name>`");
            return;
        }

        sendChat("DT-Frag: starting session, timer started");
        fragrunStartTime = System.currentTimeMillis();
        fragSessionRuns = 0;
        fragSessionTotalTime = 0L;
    }

    public void fragEndSession()
    {
        if (fragrunStartTime == 0L)
        {
            sendChat("DT-Frag: you've not started a session");
            return;
        }

        if (getCurrentIsland() == SkyblockIsland.DUNGEON &&
            (getCurrentLocationRawCleaned().contains("(F7)") || getCurrentLocationRawCleaned().contains("(M7)")))
        {
            fragEnd();
        }

        fragrunStartTime = 0L;

        sendChat("DT-Frag: ending session, total runs: " + c.totalFragruns);

        if (fragSessionRuns == 0)
            return;
        String totalFormatted = new SimpleDateFormat("HH:mm:ss.SS").format(new Date(fragSessionTotalTime));
        String avgFormatted = new SimpleDateFormat("mm:ss.SS").format(new Date(fragSessionTotalTime / fragSessionRuns));

        sendChat("DT-Frag: session runs: " + fragSessionRuns);
        sendChat("DT-Frag: session total time: " + totalFormatted);
        sendChat("DT-Frag: session average time: " + avgFormatted);
    }

    public void fragNext()
    {
        if (getCurrentIsland() != SkyblockIsland.DUNGEON &&
            !(getCurrentLocationRawCleaned().contains("(F7)") || getCurrentLocationRawCleaned().contains("(M7)")))
        {
            sendChat("DT-Frag: floor 7 not detected");
            return;
        }
        if (fragrunStartTime == 0L)
        {
            sendChat("DT-Frag: please start a session first using `startsession`, and the timer will start");
            return;
        }

        sendChat("DT-Frag: warping back to dhub for next run");

        fragPendingEndRunWarp = true;
        fragPendingEndRunStartTime = getTicks();
        getPlayer().sendChatMessage("/warp dhub");
    }

    private void fragEnd()
    {
        long elapsed = System.currentTimeMillis() - fragrunStartTime;
        fragSessionRuns++;
        c.totalFragruns++;
        fragSessionTotalTime += elapsed;
        String formatted = new SimpleDateFormat("mm:ss.SS").format(new Date(elapsed));
        String avgFormatted = new SimpleDateFormat("mm:ss.SS").format(new Date(fragSessionTotalTime / fragSessionRuns));
        String fastestFormatted = new SimpleDateFormat("mm:ss.SS").format(new Date(c.fastestFragrun));
        String record;
        if (c.fastestFragrun == 0L || elapsed < c.fastestFragrun)
        {
            record = "(§eNEW RECORD!§r)";
            c.fastestFragrun = elapsed;
        }
        else if (elapsed == c.fastestFragrun)
            record = "(§aFASTEST TIME!§r)";
        else
            record = "";
        sendChatf("DT-Frag: run took %s%s, session runs: %d, session average: %s, fastest time %s",
                formatted, record, fragSessionRuns, avgFormatted, fastestFormatted);
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
}
