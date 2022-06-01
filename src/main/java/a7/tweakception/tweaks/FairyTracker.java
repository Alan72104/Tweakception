package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.TreeSet;

import static a7.tweakception.tweaks.GlobalTracker.getTicks;
import static a7.tweakception.utils.McUtils.*;
import static a7.tweakception.utils.Utils.*;

public class FairyTracker extends Tweak
{
    private final FairyTrackerConfig c;
    public static class FairyTrackerConfig
    {
        public boolean enabled = false;
        public boolean autoTracking = false;
        public int autoTrackingDelayTicks = 20;
    }
    private final TreeSet<FairyPos> set = new TreeSet<FairyPos>();
    private long lastSneakTime = 0;
    private boolean wasSneaking = false;

    public FairyTracker(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.fairyTracker;
    }

    public void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
    }

    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (!c.autoTracking || !isInGame()) return;

        if (event.phase == TickEvent.Phase.END)
        {
            if (getTicks() % c.autoTrackingDelayTicks == 0)
            {
                trackOnce();
            }
        }
    }

    public void onRenderLast(RenderWorldLastEvent event)
    {
        if (!c.enabled) return;

        if (set.isEmpty()) return;

        for (FairyPos pos : set)
        {
            Color color;
            if (pos.isFound)
                color = new Color(84, 166, 102);
            else
                color = new Color(206, 57, 199);
            RenderUtils.drawBeaconBeamOrBoundingBox(new BlockPos(pos.x, pos.y, pos.z), color, event.partialTicks, 0);
        }
    }

    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        if (!c.enabled) return;

        if (FMLClientHandler.instance().getClient().gameSettings.keyBindSneak.isPressed())
        {
            if (!wasSneaking)
            {
                if (System.currentTimeMillis() - lastSneakTime <= 750)
                {
                    FairyPos pos = findClosest(getPlayer().posX, getPlayer().posY, getPlayer().posZ, 4.0);
                    if (pos != null)
                        pos.isFound = true;
                }
                lastSneakTime = System.currentTimeMillis();
            }
            wasSneaking = true;
        }
        else
            wasSneaking = false;
    }

    public FairyPos findClosest(double x, double y, double z, double range)
    {
        for (FairyPos pos : set)
        {
            double dX = pos.x - x;
            double dY = pos.y - y;
            double dZ = pos.z - z;
            double distanceSq = dX * dX + dY * dY + dZ * dZ;
            if (distanceSq <= range * range) {
                return pos;
            }
        }
        return null;
    }

    public void trackOnce()
    {
        if (!c.enabled)
            sendChat("Fairy: tracking isn't enabled");
        else
        {
            World world = Minecraft.getMinecraft().theWorld;

            int oldSize = set.size();
            for (Entity e : world.getLoadedEntityList()) {
                tryAddFairy(e);
            }
            if (set.size() != oldSize)
                sendChat("Fairy: new count: " + set.size());
        }
    }

    public void tryAddFairy(Entity e)
    {
        if (e instanceof EntityArmorStand)
        {
            EntityArmorStand armorStand = (EntityArmorStand) e;
            ItemStack head = armorStand.getCurrentArmor(3);
            try
            {
                String fairyTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjk2OTIzYWQyNDczMTAwMDdmNmFlNWQzMjZkODQ3YWQ1Mzg2NGNmMTZjMzU2NWExODFkYzhlNmIyMGJlMjM4NyJ9fX0=";
                if (head != null && head.getTagCompound().getCompoundTag("SkullOwner").
                        getCompoundTag("Properties").getTagList("textures", Constants.NBT.TAG_COMPOUND).
                        getCompoundTagAt(0).getString("Value").equals(fairyTexture)) {
                    FairyPos pos = new FairyPos(armorStand.posX - 1, armorStand.posY + 2, armorStand.posZ - 1);
                    if (!set.contains(pos)) {
                        sendChat("Fairy: found new " + pos.x + ", " + pos.y + ", " + pos.z);
                        set.add(pos);
                    }
                }
            }
            catch (NullPointerException ignored)
            {
            }
        }
    }

    public void toggle()
    {
        c.enabled = !c.enabled;
        sendChat("Fairy: toggled " + c.enabled + ", count: " + set.size());
    }

    public void toggleAutoTracking()
    {
        c.autoTracking = !c.autoTracking;
        sendChat("Fairy: auto tracking toggled " + c.enabled + ", count: " + set.size());
    }

    public void setDelay(int newDelay)
    {
        c.autoTrackingDelayTicks = newDelay;
        sendChat("Fairy: delay set to " + newDelay);
    }

    public void setNotFound()
    {
        FairyPos pos = findClosest(getPlayer().posX, getPlayer().posY, getPlayer().posZ, 4.0);
        if (pos != null)
            pos.isFound = false;
    }

    public void reset()
    {
        set.clear();
    }

    public void count()
    {
        sendChat("Fairy: count: " + set.size());
    }

    public void list()
    {
        sendChat("Fairy: printing list to console, count: " + set.size());
        Tweakception.logger.info("Fairy: list start, count: " + set.size());
        for (FairyTracker.PosI pos : set)
            Tweakception.logger.info("Fairy: " + pos);
        Tweakception.logger.info("Fairy: list end ====================");
        dump();
    }

    public void dump()
    {
        Tweakception.logger.info("Fairy: dumping");
        Object[] a = set.toArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++)
        {
            FairyPos p = (FairyPos)a[i];
            sb.append(p.isFound ? "1" : "0");
            sb.append("@" + p.x + "," + p.y + "," + p.z);
            if (i < a.length - 1)
                sb.append(";");
        }
        Tweakception.logger.info(sb.toString());
        setClipboard(sb.toString());
        sendChat("Fairy: copied dump to clipboard");
        Tweakception.logger.info("Fairy: dumping finished");
    }

    public void load() {
        String data = getClipboard();
        if (data == null)
        {
            sendChat("Fairy: clipboard is wrong or empty");
            return;
        }
        String[] items = data.split(";");
        if (items.length == 0)
        {
            sendChat("Fairy: clipboard is wrong or empty");
            return;
        }
        ArrayList<FairyPos> positions = new ArrayList<FairyPos>();
        for (String s : items)
        {
            String[] split = s.split("[@,]");
            if (split.length != 4)
            {
                sendChat("Fairy: clipboard content is wrong");
                return;
            }
            FairyPos pos = new FairyPos(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]));
            pos.isFound = split[0].equals("1");
            positions.add(pos);
        }
        set.clear();
        set.addAll(positions);
        sendChat("Fairy: imported " + set.size() + " locations from clipboard");
    }

    private static class FairyPos extends PosI
    {
        public boolean isFound = false;
        public FairyPos(int x, int y, int z)
        {
            super(x, y, z);
        }
        public FairyPos(double x, double y, double z)
        {
            super(x, y, z);
        }
    }

    private static class PosI implements Comparable<PosI>
    {
        public int x;
        public int y;
        public int z;
        public PosI(int x, int y, int z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        public PosI(float x, float y, float z)
        {
            this.x = (int)x;
            this.y = (int)y;
            this.z = (int)z;
        }
        public PosI(double x, double y, double z)
        {
            this.x = (int)x;
            this.y = (int)y;
            this.z = (int)z;
        }
        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof PosI))
                return false;
            PosI p = (PosI)o;
            return this.x == p.x && this.y == p.y && this.z == p.z;
        }
        @Override
        public int hashCode()
        {
            return x + y + z;
        }
        @Override
        public String toString()
        {
            return x + ", " + y + ", " + z;
        }
        @Override
        public int compareTo(PosI o)
        {
            if (x < o.x)
                return -1;
            else if (x == o.x)
            {
                if (y < o.y)
                    return -1;
                else if (y == o.y)
                {
                    if (z < o.z)
                        return -1;
                    else if (z == o.z)
                        return 0;
                    else
                        return 1;
                }
                else
                    return 1;
            }
            else
                return 1;
        }
    }
}
