package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.overlay.Anchor;
import a7.tweakception.overlay.TextOverlay;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Pair;
import a7.tweakception.utils.RenderUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static a7.tweakception.Tweakception.BlockSearchTask;
import static a7.tweakception.tweaks.GlobalTweaks.getCurrentIsland;
import static a7.tweakception.tweaks.GlobalTweaks.getTicks;
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
        public int autoHealWandHealthThreshold = 50;
    }
    
    private static final List<Pair<String, Integer>> SLAYER_TYPES = new ArrayList<>(); // Name, type
    private static final List<Pair<String, Integer>> MINIBOSS_TYPES = new ArrayList<>();
    private boolean autoThrowFishingRod = false;
    private boolean autoHealWand = false;
    private List<BlockPos> glyphs = new ArrayList<>();
    private List<BlockPos> glyphsTemp = new ArrayList<>();
    private BlockSearchTask searchThread;
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
    // §64,232/3,932❤     §a1,042§a? Defense     §b943/943? §3400?
    private final Matcher healthMatcher = Pattern.compile(
        "^§[0-9a-f](?<health>(?:[0-9]{1,3},?)+)\\/(?<maxHealth>(?:[0-9]{1,3},?)+)❤").matcher("");
    private int currentHealth = 0;
    private int maxHealth = 0;
    private int lastHealWandTicks = 0;
    private int healWandRandomDelay = 0;
    private boolean switchingSlot = false;
    
    private static class SlayerRecord
    {
        public Entity nameTag;
        public Entity entity;
        public String type;
        public float health = 0.0f;
        public float maxHealth;
        public boolean voidgloomFirstHitPhase = false;
        public boolean fishingRodThrown = false;
        
        public SlayerRecord(Entity n, Entity e, String t, float mh)
        {
            nameTag = n;
            entity = e;
            type = t;
            maxHealth = mh;
        }
        
        @Override
        public int hashCode() {return nameTag.hashCode();}
        
        @Override
        public boolean equals(Object o) {return o instanceof SlayerRecord && ((SlayerRecord) o).nameTag.equals(this.nameTag);}
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
        Tweakception.overlayManager.addOverlay(new SlayerOverlay());
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
                        searchThread = new BlockSearchTask((int) p.posX, (int) p.posY, (int) p.posZ, 30, 15, getWorld(), Blocks.beacon, glyphsTemp);
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
                Tweakception.overlayManager.enable(SlayerOverlay.NAME);
                float health = parseHealth(currentSlayer.nameTag.getName());
                if (health != -1.0f && health != 0.0f)
                {
                    currentSlayer.health = health;
                    if (currentSlayer.voidgloomFirstHitPhase)
                    {
                        currentSlayer.voidgloomFirstHitPhase = false;
                        currentSlayer.maxHealth = health;
                    }
                    else if (autoThrowFishingRod &&
                        !currentSlayer.fishingRodThrown &&
                        currentSlayer.health <= currentSlayer.maxHealth * c.autoThrowFishingRodThreshold / 100 &&
                        !switchingSlot)
                    {
                        currentSlayer.fishingRodThrown = true;
                        int slot = Utils.findInHotbarBy(stack ->
                            stack != null &&
                                stack.getItem() == Items.fishing_rod &&
                                Utils.getSkyblockItemId(stack) != null &&
                                !Utils.getSkyblockItemId(stack).equals("GRAPPLING_HOOK"));
                        
                        if (slot == -1)
                            sendChat("ST-AutoThrowFishingRod: cannot find any fishing rod in your hotbar!");
                        else
                        {
                            switchingSlot = true;
                            int lastSlot = getPlayer().inventory.currentItem;
                            getPlayer().inventory.currentItem = slot;
                            Tweakception.scheduler.addDelayed(() -> getMc().rightClickMouse(), 4)
                                .thenDelayed(() ->
                                {
                                    getPlayer().inventory.currentItem = lastSlot;
                                    switchingSlot = false;
                                }, 6);
                        }
                    }
                }
            }
            else
                Tweakception.overlayManager.disable(SlayerOverlay.NAME);
        }
        
        if (autoHealWand)
        {
            if (currentHealth > 100 && maxHealth > 100 && getTicks() > 600 &&
                currentHealth <= maxHealth * c.autoHealWandHealthThreshold / 100 &&
                getTicks() - lastHealWandTicks >= 20 * 7 + 5 + healWandRandomDelay &&
                System.currentTimeMillis() - Tweakception.globalTweaks.getWorldJoinMillis() >= 2000 &&
                !switchingSlot)
            {
                lastHealWandTicks = getTicks();
                healWandRandomDelay = getWorld().rand.nextInt(5);
                int wandSlot = Utils.findInHotbarById("WAND_OF_HEALING", "WAND_OF_MENDING", "WAND_OF_RESTORATION",
                    "WAND_OF_ATONEMENT");
                if (wandSlot == -1)
                    sendChat("ST-AutoHealWand: cannot find any healing wands in your hotbar!");
                else
                {
                    switchingSlot = true;
                    int lastSlot = getPlayer().inventory.currentItem;
                    getPlayer().inventory.currentItem = wandSlot;
                    Tweakception.scheduler.addDelayed(() -> getMc().rightClickMouse(), 3)
                        .thenDelayed(() ->
                        {
                            getPlayer().inventory.currentItem = lastSlot;
                            switchingSlot = false;
                        }, 5);
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
                    
                    Entity nearest = McUtils.getNewestEntityInAABB(stand,
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
                    RenderUtils.drawFilledBoundingBox(p, new Color(255, 0, 106, (int) (255 * 0.9f)), event.partialTicks);
        }
        
        if (c.highlightSlayers)
            for (SlayerRecord record : slayersCache)
                RenderUtils.drawDefaultHighlightBoxForEntity(record.entity, RenderUtils.DEFAULT_HIGHLIGHT_COLOR, false);
        if (c.highlightSlayerMiniboss)
            for (SlayerRecord record : slayerMinibossCache)
                RenderUtils.drawDefaultHighlightBoxForEntity(record.entity, RenderUtils.DEFAULT_HIGHLIGHT_COLOR, false);
    }
    
    public void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
        if (c.highlightSlayers || c.highlightSlayerMiniboss || autoThrowFishingRod)
        {
            if (event.entity instanceof EntityArmorStand)
                armorStandsTemp.add(new Pair<>(getTicks(), event.entity));
        }
    }
    
    public void onChatReceived(ClientChatReceivedEvent event)
    {
        if (autoHealWand && event.type == 2)
        {
            String[] sections = event.message.getFormattedText().split(" {3,}");
            for (String sec : sections)
            {
                if (healthMatcher.reset(sec).matches())
                {
                    currentHealth = Utils.parseInt(healthMatcher.group("health"));
                    maxHealth = Utils.parseInt(healthMatcher.group("maxHealth"));
//                    sendChatf("hp: %d max: %d", currentHealth, maxHealth);
                    break;
                }
            }
        }
    }
    
    public void onWorldUnload(WorldEvent.Unload event)
    {
        if (searchThread != null)
            searchThread.cancel = true;
        slayersCache.clear();
        slayerMinibossCache.clear();
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
    
    private class SlayerOverlay extends TextOverlay
    {
        public static final String NAME = "SlayerOverlay";
        
        public SlayerOverlay()
        {
            super(NAME);
            setAnchor(Anchor.TopCenter);
            setOrigin(Anchor.TopCenter);
            setY(40);
        }
        
        @Override
        public void update()
        {
            super.update();
            List<String> list = new ArrayList<>();
            
            list.add("Current slayer:");
            String s;
            if (currentSlayer.voidgloomFirstHitPhase)
                s = f("Slayer: %s, health: -", currentSlayer.type);
            else
                s = f("Slayer: %s, health: %s (%d%%), threshold: %s%d%%",
                    currentSlayer.type,
                    Utils.formatMetric((long) currentSlayer.health),
                    (int) (currentSlayer.health / currentSlayer.maxHealth * 100.0f),
                    currentSlayer.fishingRodThrown ? "§6" : "",
                    c.autoThrowFishingRodThreshold);
            list.add(s);
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("slayer");
            list.add("overlay");
            return list;
        }
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
        c.autoThrowFishingRodThreshold = percent > 0 ? Utils.clamp(percent, 1, 100) :
            new SlayerTweaksConfig().autoThrowFishingRodThreshold;
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
    
    public void toggleAutoHealWand()
    {
        autoHealWand = !autoHealWand;
        sendChat("ST-AutoHealWand: toggled " + autoHealWand);
    }
    
    public void setAutoHealWandHealthThreshold(int percent)
    {
        c.autoHealWandHealthThreshold = percent > 0 ? Utils.clamp(percent, 1, 99) :
            new SlayerTweaksConfig().autoHealWandHealthThreshold;
        sendChat("ST-AutoHealWand: set health threshold to " + c.autoHealWandHealthThreshold);
    }
}
