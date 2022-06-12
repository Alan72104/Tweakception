package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Pair;
import a7.tweakception.utils.RenderUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static a7.tweakception.Tweakception.BlockSearchThread;
import static a7.tweakception.Tweakception.miningTweaks;
import static a7.tweakception.tweaks.GlobalTracker.getCurrentIsland;
import static a7.tweakception.tweaks.GlobalTracker.getTicks;
import static a7.tweakception.utils.McUtils.*;
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
    private final LinkedList<Pair<Integer, Entity>> nameTagsTemp = new LinkedList<>();
    private final Matcher healthMatcher = Pattern.compile(" §[0-9a-f](\\d+(?:,\\d+)*(?:\\.\\d+)?)([MmKk]?)§c❤").matcher("");
    private final Set<SlayerRecord> slayersCache = new HashSet<>(); // The nametags
    private final Set<SlayerRecord> slayerMinibossCache = new HashSet<>();
    private static class SlayerRecord
    {
        public Entity entity;
        public int type;
        public float maxHealth;
        public boolean fishingRodThrown = false;
        public SlayerRecord(Entity e, int t, float mh) { entity = e; type = t; maxHealth = mh; }
        @Override
        public int hashCode() { return entity.hashCode(); }
        @Override
        public boolean equals(Object o) { return o instanceof SlayerRecord && ((SlayerRecord)o).entity.equals(this.entity); }
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
            removeWhile(nameTagsTemp, ele -> getTicks() - ele.a > 5,
                ele ->
                {
                    Entity entity = ele.b;
                    String name = entity.getName();
                    float hp;

                    for (Pair<String, Integer> type : SLAYER_TYPES)
                        if (name.contains(type.a) && (hp = parseHealth(name)) != -1.0f)
                        {
                            SlayerRecord record = new SlayerRecord(entity, type.b, hp);
                            slayersCache.add(record);
                            return;
                        }
                    if (c.highlightSlayerMiniboss)
                        for (Pair<String, Integer> type : MINIBOSS_TYPES)
                            if (name.contains(type.a) && (hp = parseHealth(name)) != -1.0f)
                            {
                                SlayerRecord record = new SlayerRecord(entity, type.b, hp);
                                slayerMinibossCache.add(record);
                                return;
                            }

                });

            slayersCache.removeIf(ele -> ele.entity.isDead);
            slayerMinibossCache.removeIf(ele -> ele.entity.isDead);

            if (autoThrowFishingRod)
            {
                SlayerRecord currentSlayer = null;

                float nearestDis = Float.MAX_VALUE;
                for (SlayerRecord record : slayersCache)
                {
                    float dis = record.entity.getDistanceToEntity(getPlayer());
                    if (dis <= 15.0f && dis < nearestDis)
                    {
                        nearestDis = dis;
                        currentSlayer = record;
                    }
                }

                if (currentSlayer != null)
                {
                    float health;

                    if (!currentSlayer.entity.isDead && !currentSlayer.fishingRodThrown &&
                        (health = parseHealth(currentSlayer.entity.getName())) != -1.0f)
                    {
                        if (GlobalTracker.t)
                            sendChat("" + health);

                        if (health <= currentSlayer.maxHealth * c.autoThrowFishingRodThreshold / 100)
                        {
                            int slot = findFishingRodSlot();

                            if (slot == -1)
                                sendChat("ST-AutoThrowFishingRod: cannot find any fishing rod in your hotbar!");
                            else
                            {
                                int lastSlot = getPlayer().inventory.currentItem;
                                getPlayer().inventory.currentItem = slot;
                                Tweakception.scheduler.addDelayed(() -> getMc().rightClickMouse(), 6).
                                        thenDelayed(() -> getPlayer().inventory.currentItem = lastSlot, 8);
                                currentSlayer.fishingRodThrown = true;
                            }
                        }
                    }
                }
            }
        }
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
                RenderUtils.drawDefaultHighlightBoxUnderEntity(
                        record.entity, record.type, RenderUtils.DEFAULT_HIGHLIGHT_COLOR, false);
        if (c.highlightSlayerMiniboss)
            for(SlayerRecord record : slayerMinibossCache)
                RenderUtils.drawDefaultHighlightBoxUnderEntity(
                        record.entity, record.type, RenderUtils.DEFAULT_HIGHLIGHT_COLOR, false);
    }

    public void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
        if (c.highlightSlayers || c.highlightSlayerMiniboss || autoThrowFishingRod)
        {
            if (event.entity instanceof EntityArmorStand)
                nameTagsTemp.add(new Pair<>(getTicks(), event.entity));
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
                String id = McUtils.getSkyblockItemId(stack);
                if (id == null || !id.equals("GRAPPLING_HOOK"))
                    return i;
            }
        }
        return -1;
    }

    private float parseHealth(String s)
    {
        if (healthMatcher.reset(s).find())
        {
            String healthString = healthMatcher.group(1).replace(",", "");
            String unit = healthMatcher.group(2);

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
