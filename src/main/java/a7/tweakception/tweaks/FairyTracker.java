package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.RenderUtils;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import static a7.tweakception.utils.McUtils.*;
import static a7.tweakception.utils.Utils.getClipboard;
import static a7.tweakception.utils.Utils.setClipboard;

public class FairyTracker extends Tweak
{
    public static class FairyTrackerConfig
    {
        public boolean enabled = false;
    }
    
    private final FairyTrackerConfig c;
    private boolean fairyTrackingEnabled = false;
    private final TreeSet<PosMark> fairySet = new TreeSet<>();
    private long lastSneakTime = 0;
    private boolean wasSneaking = false;
    
    public FairyTracker(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.fairyTracker;
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
    }
    
    public void onRenderLast(RenderWorldLastEvent event)
    {
        if (c.enabled)
            for (PosMark pos : fairySet)
            {
                Color color;
                if (pos.isFound)
                    color = new Color(84, 166, 102);
                else
                    color = new Color(206, 57, 199);
                RenderUtils.drawBeaconBeamOrBoundingBox(new BlockPos(pos.x, pos.y, pos.z), color, event.partialTicks, 0);
            }
    }
    
    public void onEntityUpdate(LivingEvent.LivingUpdateEvent event)
    {
        if (c.enabled && fairyTrackingEnabled && event.entity instanceof EntityArmorStand)
        {
            String fairyTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjk2OTIzYWQyNDczMTAwMDdmNmFlNWQzMjZkODQ3YWQ1Mzg2NGNmMTZjMzU2NWExODFkYzhlNmIyMGJlMjM4NyJ9fX0=";
            
            PosMark pos = tryAdd((EntityArmorStand) event.entity, fairyTexture, fairySet);
            if (pos != null)
            {
                sendChatf("Fairy: found new %d, %d, %d (count: %d)", pos.x, pos.y, pos.z, fairySet.size());
                return;
            }
        }
    }
    
    private PosMark tryAdd(EntityArmorStand armorStand, String texture, Set<PosMark> set)
    {
        String tex = McUtils.getArmorStandHeadTexture(armorStand);
        if (tex != null && tex.equals(texture))
        {
            PosMark pos = new PosMark(armorStand.posX - 1, armorStand.posY + 2, armorStand.posZ - 1);
            if (!set.contains(pos))
            {
                set.add(pos);
                return pos;
            }
        }
        
        return null;
    }
    
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        if (!c.enabled) return;
        
        if (getMc().gameSettings.keyBindSneak.isPressed())
        {
            if (!wasSneaking)
            {
                if (System.currentTimeMillis() - lastSneakTime <= 750)
                {
                    PosMark pos = findClosest(getPlayer().posX, getPlayer().posY, getPlayer().posZ, 4.0);
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
    
    public PosMark findClosest(double x, double y, double z, double range)
    {
        Set<PosMark> set = new TreeSet<>();
        if (c.enabled)
            set.addAll(fairySet);
        
        for (PosMark pos : set)
        {
            double dX = pos.x - x;
            double dY = pos.y - y;
            double dZ = pos.z - z;
            double distanceSq = dX * dX + dY * dY + dZ * dZ;
            if (distanceSq <= range * range)
            {
                return pos;
            }
        }
        
        return null;
    }
    
    public void toggle()
    {
        c.enabled = !c.enabled;
        sendChat("Fairy: toggled " + c.enabled + ", count: " + fairySet.size());
    }
    
    public void toggleTracking()
    {
        fairyTrackingEnabled = !fairyTrackingEnabled;
        sendChat("Fairy: toggled tracking " + fairyTrackingEnabled);
    }
    
    public void setNotFound()
    {
        PosMark pos = findClosest(getPlayer().posX, getPlayer().posY, getPlayer().posZ, 4.0);
        if (pos != null)
            pos.isFound = false;
    }
    
    public void reset()
    {
        fairySet.clear();
    }
    
    public void count()
    {
        sendChat("Fairy: count: " + fairySet.size());
    }
    
    public void list()
    {
        sendChat("Fairy: printing list to console, count: " + fairySet.size());
        Tweakception.logger.info("Fairy: list start, count: " + fairySet.size());
        for (FairyTracker.PosI pos : fairySet)
            Tweakception.logger.info("Fairy: " + pos);
        Tweakception.logger.info("Fairy: list end ====================");
        dump();
    }
    
    public void dump()
    {
        Tweakception.logger.info("Fairy: dumping");
        Object[] a = fairySet.toArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++)
        {
            PosMark p = (PosMark) a[i];
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
    
    public void load()
    {
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
        ArrayList<PosMark> positions = new ArrayList<PosMark>();
        for (String s : items)
        {
            String[] split = s.split("[@,]");
            if (split.length != 4)
            {
                sendChat("Fairy: clipboard content is wrong");
                return;
            }
            PosMark pos = new PosMark(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]));
            pos.isFound = split[0].equals("1");
            positions.add(pos);
        }
        fairySet.clear();
        fairySet.addAll(positions);
        sendChat("Fairy: imported " + fairySet.size() + " locations from clipboard");
    }
    
    private static class PosMark extends PosI
    {
        public boolean isFound = false;
        
        public PosMark(int x, int y, int z)
        {
            super(x, y, z);
        }
        
        public PosMark(double x, double y, double z)
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
        
        public PosI(double x, double y, double z)
        {
            this.x = (int) x;
            this.y = (int) y;
            this.z = (int) z;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof PosI))
                return false;
            PosI p = (PosI) o;
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
