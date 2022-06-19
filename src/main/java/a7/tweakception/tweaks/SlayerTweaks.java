package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Pair;
import a7.tweakception.utils.RenderUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static a7.tweakception.Tweakception.BlockSearchThread;
import static a7.tweakception.tweaks.GlobalTracker.getCurrentIsland;
import static a7.tweakception.tweaks.GlobalTracker.getTicks;
import static a7.tweakception.utils.McUtils.*;
import static a7.tweakception.utils.Utils.f;
import static a7.tweakception.utils.Utils.removeWhile;

public class SlayerTweaks extends Tweak
{
    private final SlayerTweaksConfig c;
    public static class SlayerTweaksConfig
    {
        public boolean highlightGlyph = false;
        public int autoThrowFishingRodThreshold = 20;
        public boolean highlightSlayers = false;
        public boolean highlightSlayerMiniboss = false;
    }
    private static final List<Pair<String, Integer>> SLAYER_TYPES = new ArrayList<>(); // Name, type
    private static final List<Pair<String, Integer>> MINIBOSS_TYPES = new ArrayList<>();
    private List<BlockPos> glyphs = new ArrayList<>();
    private List<BlockPos> glyphsTemp = new ArrayList<>();
    private BlockSearchThread searchThread;
    private boolean autoThrowFishingRod = false;
    private final LinkedList<Pair<Integer, Entity>> armorStandsTemp = new LinkedList<>();
    // Supports all these:
    // §5Voidling Devotee §a11M§c❤
    // §c☠ §bVoidgloom Seraph §e97M§c❤
    // §c☠ §bVoidgloom Seraph §f§l60 Hits
    private final Matcher slayerNameTagMatcher = Pattern.compile(
        "(?:§c☠ )?§[0-9a-f][^§]+(?:§[0-9a-f](\\d+(?:,\\d+)*(?:\\.\\d+)?)([MmKk]?)§c❤|§f§l(\\d+) Hits)").matcher("");
    private final Set<SlayerRecord> slayersCache = new HashSet<>(); // The nametags
    private final Set<SlayerRecord> slayerMinibossCache = new HashSet<>();
    private SlayerRecord currentSlayer = null;
    private static class SlayerRecord
    {
        public Entity nameTag;
        public Entity entity;
        public String type;
        public float health = 0.0f;
        public float maxHealth;
        public boolean voidgloomFirstHitPhase = false;
        public boolean fishingRodThrown = false;
        public SlayerRecord(Entity n, Entity e, String t, float mh) { nameTag = n; entity = e; type = t; maxHealth = mh; }
        @Override
        public int hashCode() { return nameTag.hashCode(); }
        @Override
        public boolean equals(Object o) { return o instanceof SlayerRecord && ((SlayerRecord)o).nameTag.equals(this.nameTag); }
    }

    static
    {
        SLAYER_TYPES.add(new Pair<>("Revenant Horror", 0));
        SLAYER_TYPES.add(new Pair<>("Atoned Horror", 0));
        SLAYER_TYPES.add(new Pair<>("Tarantula Broodfather", 1));
        SLAYER_TYPES.add(new Pair<>("Sven Packmaster", 2));
        SLAYER_TYPES.add(new Pair<>("Voidgloom Seraph", 3));
        SLAYER_TYPES.add(new Pair<>("Inferno Demonlord", 4));
        MINIBOSS_TYPES.add(new Pair<>("Revenant Sycophant", 0));
        MINIBOSS_TYPES.add(new Pair<>("Revenant Champion", 0));
        MINIBOSS_TYPES.add(new Pair<>("Deformed Revenant", 0));
        MINIBOSS_TYPES.add(new Pair<>("Atoned Champion", 0));
        MINIBOSS_TYPES.add(new Pair<>("Atoned Revenant", 0));
        MINIBOSS_TYPES.add(new Pair<>("Tarantula Vermin", 1));
        MINIBOSS_TYPES.add(new Pair<>("Tarantula Beast", 1));
        MINIBOSS_TYPES.add(new Pair<>("Mutant Tarantula", 1));
        MINIBOSS_TYPES.add(new Pair<>("Pack Enforcer", 2));
        MINIBOSS_TYPES.add(new Pair<>("Sven Follower", 2));
        MINIBOSS_TYPES.add(new Pair<>("Sven Alpha", 2));
        MINIBOSS_TYPES.add(new Pair<>("Voidling Devotee", 3));
        MINIBOSS_TYPES.add(new Pair<>("Voidling Radical", 3));
        MINIBOSS_TYPES.add(new Pair<>("Voidcrazed Maniac", 3));
        MINIBOSS_TYPES.add(new Pair<>("Flare Demon", 4));
        MINIBOSS_TYPES.add(new Pair<>("Kindleheart Demon", 4));
        MINIBOSS_TYPES.add(new Pair<>("Burningsoul Demon", 4));
    }

    public SlayerTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.slayerTweaks;
    }

    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        if (getTicks() % 5 == 4)
        {
            if (getCurrentIsland() == SkyblockIsland.THE_END)
            {
                if (c.highlightGlyph)
                {
                    if (searchThread == null || searchThread.done)
                    {
                        EntityPlayerSP p = getPlayer();
                        glyphs = glyphsTemp;
                        glyphsTemp = new ArrayList<>(20);
                        searchThread = new BlockSearchThread((int)p.posX, (int)p.posY, (int)p.posZ, 15, 10, getWorld(), Blocks.beacon, glyphsTemp);
                        Tweakception.threadPool.execute(searchThread);
                    }
                }
            }
        }

        if (c.highlightSlayers || c.highlightSlayerMiniboss || autoThrowFishingRod)
        {
            removeWhile(armorStandsTemp, ele -> getTicks() - ele.a > 5,
                ele ->
                {
                    Entity stand = ele.b;
                    if (!tryDetectAndAddSlayerFromNameTag(SLAYER_TYPES, stand, slayersCache))
                        if (c.highlightSlayerMiniboss)
                            tryDetectAndAddSlayerFromNameTag(MINIBOSS_TYPES, stand, slayerMinibossCache);
                });

            slayersCache.removeIf(ele -> ele.nameTag.isDead);
            slayerMinibossCache.removeIf(ele -> ele.nameTag.isDead);
            currentSlayer = null;

            if (autoThrowFishingRod)
            {
                double nearestDis = Double.MAX_VALUE;
                for (SlayerRecord record : slayersCache)
                {
                    double dis = record.nameTag.getDistanceSqToEntity(getPlayer());
                    if (dis <= 64.0 && dis < nearestDis)
                    {
                        nearestDis = dis;
                        currentSlayer = record;
                    }
                }

                if (currentSlayer != null)
                {
                    float health = parseHealth(currentSlayer.nameTag.getName());
                    if (health != -1.0f && health != 0.0f)
                    {
                        currentSlayer.health = health;
                        if (currentSlayer.voidgloomFirstHitPhase)
                        {
                            currentSlayer.voidgloomFirstHitPhase = false;
                            currentSlayer.maxHealth = health;
                        }
                        else if (!currentSlayer.fishingRodThrown &&
                                currentSlayer.health <= currentSlayer.maxHealth * c.autoThrowFishingRodThreshold / 100)
                        {
                            currentSlayer.fishingRodThrown = true;
                            int slot = findFishingRodSlot();

                            if (slot == -1)
                                sendChat("ST-AutoThrowFishingRod: cannot find any fishing rod in your hotbar!");
                            else
                            {
                                int lastSlot = getPlayer().inventory.currentItem;
                                getPlayer().inventory.currentItem = slot;
                                Tweakception.scheduler.addDelayed(() -> getMc().rightClickMouse(), 4).
                                        thenDelayed(() -> getPlayer().inventory.currentItem = lastSlot, 6);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean tryDetectAndAddSlayerFromNameTag(List<Pair<String, Integer>> targets, Entity stand, Set<SlayerRecord> targetSet)
    {
        String name = stand.getName();
        float hp;
        for (Pair<String, Integer> type : targets)
            if (name.contains(type.a))
            {
                hp = parseHealth(name);
                if (hp != -1.0f)
                {
                    boolean hitPhase = slayerNameTagMatcher.group(3) != null;

                    Entity nearest = McUtils.getNearestEntityInAABB(stand,
                            stand.getEntityBoundingBox().expand(0.5, 2.5, 0.5),
                            e -> (e instanceof EntityZombie ||
                                    e instanceof EntitySpider ||
                                    e instanceof EntityWolf ||
                                    e instanceof EntityEnderman ||
                                    e instanceof EntityBlaze) &&
                                    !e.isDead);

                    if (nearest != null)
                    {
                        SlayerRecord record = new SlayerRecord(stand, nearest, type.a, hp);
                        record.voidgloomFirstHitPhase = hitPhase;
                        targetSet.add(record);
                        return true;
                    }
                    else
                        return false;
                }
            }
        return false;
    }

    public void onRenderLast(RenderWorldLastEvent event)
    {
        if (getCurrentIsland() == SkyblockIsland.THE_END)
        {
            if (c.highlightGlyph)
                for (BlockPos p : glyphs)
                    RenderUtils.drawBeaconBeamOrBoundingBox(p, new Color(255, 0, 106, (int)(255 * 0.9f)), event.partialTicks, 1);
        }

        if (c.highlightSlayers)
            for(SlayerRecord record : slayersCache)
                RenderUtils.drawDefaultHighlightBoxForEntity(record.entity, RenderUtils.DEFAULT_HIGHLIGHT_COLOR, false);
        if (c.highlightSlayerMiniboss)
            for(SlayerRecord record : slayerMinibossCache)
                RenderUtils.drawDefaultHighlightBoxForEntity(record.entity, RenderUtils.DEFAULT_HIGHLIGHT_COLOR, false);
    }

    public void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event)
    {
        if ((c.highlightSlayers || autoThrowFishingRod) && currentSlayer != null)
        {
            ScaledResolution res = new ScaledResolution(getMc());
            FontRenderer r = getMc().fontRendererObj;
            int width = res.getScaledWidth();

            String s;
            if (currentSlayer.voidgloomFirstHitPhase)
                s = f("Slayer: %s, health: -", currentSlayer.type);
            else
                s = f("Slayer: %s, health: %s (%d%%), threshold: %s%d%%",
                    currentSlayer.type,
                    Utils.formatMetric((long)currentSlayer.health),
                    (int)(currentSlayer.health / currentSlayer.maxHealth * 100.0f),
                    currentSlayer.fishingRodThrown ? "§6" : "",
                    c.autoThrowFishingRodThreshold);

            r.drawString(s, (width - r.getStringWidth(s)) / 2, 30 + r.FONT_HEIGHT, 0xffffffff);
        }
    }

    public void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
        if (c.highlightSlayers || c.highlightSlayerMiniboss || autoThrowFishingRod)
        {
            if (event.entity instanceof EntityArmorStand)
                armorStandsTemp.add(new Pair<>(getTicks(), event.entity));
        }
    }

    public void onWorldUnload(WorldEvent.Unload event)
    {
        if (searchThread != null)
            searchThread.cancel = true;
    }

    private int findFishingRodSlot()
    {
        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = getPlayer().inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.fishing_rod)
            {
                String id = Utils.getSkyblockItemId(stack);
                if (id == null || !id.equals("GRAPPLING_HOOK"))
                    return i;
            }
        }
        return -1;
    }

    // If the name has health, returns the health
    // If the name has no health, returns 0.0f
    // If the name isn't a slayer name tag, returns -1.0f
    private float parseHealth(String s)
    {
        if (slayerNameTagMatcher.reset(s).find())
        {
            // If the group doesn't match then the result will be null
            // eg. given the pattern (health)|(50 hits) and a string "50 hits", the health group will be null,
            // but with ((?:health)?)|(50 hits), then it will be an empty string
            if (slayerNameTagMatcher.group(1) == null)
                return 0.0f;

            String healthString = slayerNameTagMatcher.group(1).replace(",", "");
            String unit = slayerNameTagMatcher.group(2);

            float health = Float.parseFloat(healthString);
            switch (unit)
            {
                case "k":
                case "K":
                    health *= 1000;
                    break;
                case "m":
                case "M":
                    health *= 1000000;
                    break;
            }

            return health;
        }
        return -1.0f;
    }

    public void toggleHighlightGlyph()
    {
        c.highlightGlyph = !c.highlightGlyph;
        sendChat("ST-Eman: toggled glyph highlighting " + c.highlightGlyph);
        if (!c.highlightGlyph)
        {
            if (searchThread != null && !searchThread.done)
                searchThread.cancel = true;
            searchThread = null;
        }
    }

    public void toggleAutoThrowFishingRod()
    {
        autoThrowFishingRod = !autoThrowFishingRod;
        sendChat("ST-AutoThrowFishingRod: toggled " + autoThrowFishingRod);
    }

    public void setAutoThrowFishingRodThreshold(int percent)
    {
        if (percent == -1)
            c.autoThrowFishingRodThreshold = new SlayerTweaksConfig().autoThrowFishingRodThreshold;
        else
            c.autoThrowFishingRodThreshold = Utils.clamp(percent, 0, 100);
        sendChat("ST-AutoThrowFishingRod: set threshold to " + c.autoThrowFishingRodThreshold);
    }

    public void toggleHighlightSlayers()
    {
        c.highlightSlayers = !c.highlightSlayers;
        sendChat("ST-HighlightSlayers: toggled " + c.highlightSlayers);
    }

    public void toggleHighlightSlayerMiniboss()
    {
        c.highlightSlayerMiniboss = !c.highlightSlayerMiniboss;
        sendChat("ST-HighlightSlayerMiniboss: toggled " + c.highlightSlayerMiniboss);
    }
}
