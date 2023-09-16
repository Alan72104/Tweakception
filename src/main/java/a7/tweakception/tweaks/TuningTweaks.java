package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.timers.Stopwatch;
import a7.tweakception.utils.Utils;
import a7.tweakception.utils.WindowClickContants;
import a7.tweakception.utils.timers.TicksStopwatch;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static a7.tweakception.utils.McUtils.*;
import static a7.tweakception.utils.Utils.f;

public class TuningTweaks extends Tweak
{
    private final TuningTweaksConfig c;
    
    public static class TuningTweaksConfig
    {
        public boolean enableTemplates = false;
        public Tuning[] tuningTemplates = new Tuning[4];
        public int tuningClickDelayTicks = 2;
    }
    
    private Tuning remainingToTune = null;
    private TicksStopwatch timer;
    
    private static class Tuning implements Cloneable
    {
        public static final String[] NAMES = {"Health", "Defense", "Speed", "Strength",
            "Crit", "Crit Chance", "Bonus ATS", "Intel"};
        public static final String[] COLORS = {"§c", "§a", "§f", "§c", "§9", "§9", "§e", "§b"};
        public static final float[] RATIOS = {5f, 1f, 1.5f, 1f, 1f, 0.2f, 0.3f, 2f};
        public int health = 0;
        public int defence = 0;
        public int speed = 0;
        public int strength = 0;
        public int critDmg = 0;
        public int critChance = 0;
        public int atkSpeed = 0;
        public int intelligence = 0;
        
        public int getByIndex(int i)
        {
            switch (i)
            {
                default:
                case 0: return health;
                case 1: return defence;
                case 2: return speed;
                case 3: return strength;
                case 4: return critDmg;
                case 5: return critChance;
                case 6: return atkSpeed;
                case 7: return intelligence;
            }
        }
        
        public void setByIndex(int i, int n)
        {
            switch (i)
            {
                default:
                case 0: health = n; break;
                case 1: defence = n; break;
                case 2: speed = n; break;
                case 3: strength = n; break;
                case 4: critDmg = n; break;
                case 5: critChance = n; break;
                case 6: atkSpeed = n; break;
                case 7: intelligence = n; break;
            }
        }
        
        public Tuning clone()
        {
            try
            {
                return (Tuning) super.clone();
            }
            catch (CloneNotSupportedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
    
    public TuningTweaks(Configuration configuration)
    {
        super(configuration, "TT");
        c = configuration.config.tuningTweaks;
        timer = new TicksStopwatch(c.tuningClickDelayTicks);
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END) return;
        
        if (McUtils.getOpenedChest() != null)
        {
            String containerName = McUtils.getOpenedChest().getName();
            if (containerName.equals("Stats Tuning") && remainingToTune != null)
            {
                if (timer.checkAndResetIfElapsed())
                {
                    for (int i = 0; i < 8; i++)
                    {
                        int num = (2 + i % 4) + 9 * (2 + i / 4) - 1;
                        int remaining = remainingToTune.getByIndex(i);
                        if (remaining == 0 && i == 7)
                            remainingToTune = null;
                        else if (remaining == 0)
                            continue;
                        else if (remaining >= 10)
                        {
                            getMc().playerController.windowClick(getPlayer().openContainer.windowId, num,
                                WindowClickContants.ShiftLeftRight.BTN_SHIFT_RIGHT,
                                WindowClickContants.ShiftLeftRight.MODE, getPlayer());
                            remainingToTune.setByIndex(i, remaining - 10);
                        }
                        else
                        {
                            getMc().playerController.windowClick(getPlayer().openContainer.windowId, num,
                                WindowClickContants.LeftRight.BTN_RIGHT,
                                WindowClickContants.LeftRight.MODE, getPlayer());
                            remainingToTune.setByIndex(i, remaining - 1);
                        }
                        break;
                    }
                }
            }
            else
                remainingToTune = null;
        }
    }
    
    public void onItemTooltip(ItemTooltipEvent event)
    {
        if (event.itemStack == null || event.toolTip == null) return;
        if (!c.enableTemplates) return;
        
        if (McUtils.getOpenedChest() == null)
            return;
        
        String containerName = McUtils.getOpenedChest().getName();
        if (!containerName.equals("Stats Tuning"))
            return;
        
        ItemStack stack = event.itemStack;
        List<String> tooltip = event.toolTip;
        int index = getTemplateSlotFromStack(stack);
        if (index == -1)
            return;
        
        int replaceStart = -1;
        for (int i = 0; i < tooltip.size(); i++)
            if (tooltip.get(i).endsWith("§7Save and load your stats tuning"))
            {
                replaceStart = i;
                break;
            }
        if (replaceStart == -1)
            return;
        
        tooltip.subList(replaceStart, tooltip.size()).clear();
        
        Tuning template = c.tuningTemplates[index];
        if (template == null)
        {
            tooltip.add("Template is empty!");
            tooltip.add("");
            tooltip.add("Shift + right click to set the template");
        }
        else
        {
            DecimalFormat df = new DecimalFormat("#.#");
            int spaceWidth = getMc().fontRendererObj.getStringWidth(" ");
            int maxWidth = getMc().fontRendererObj.getStringWidth("§9Crit Chance§f: ");
            for (int i = 0; i < 8; i++)
            {
                String name = Tuning.COLORS[i] + Tuning.NAMES[i] + "§f: ";
                
                String nums = f("%s+%s§f - %d",
                    Tuning.COLORS[i], df.format(template.getByIndex(i) * Tuning.RATIOS[i]), template.getByIndex(i));
                
                int width = getMc().fontRendererObj.getStringWidth(name);
                String padding = Utils.stringRepeat(" ", (maxWidth - width) / spaceWidth);
                
                tooltip.add(name + padding + nums);
            }
            tooltip.add("");
            tooltip.add("Shift + left click to use the template");
            tooltip.add("Shift + right click to set the template");
            tooltip.add("Ctrl + left click to remove the template");
        }
    }
    
    public boolean isTemplateEnabled()
    {
        return c.enableTemplates;
    }
    
    // Returns -1 if the stack isn't a locked template slot, else returns the index
    public int getTemplateSlotFromStack(ItemStack stack)
    {
        String tex = McUtils.getSkullTexture(stack);
        String lockedSlotTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTlkZWVjM2NlODcwZWI5OGE4MDkzN2VhOTcxMzRjMGUzMzdjZDU0NWQ1MWM0ZWI4MDkyOWVmNGJiYTU0MzhmZSJ9fX0=";
        String[] lore = McUtils.getDisplayLore(stack);
        if (tex != null && tex.equals(lockedSlotTexture) && stack.getDisplayName().equals("§cLocked Slot") &&
            lore != null && lore[0].startsWith("§8Tuning Template #"))
        {
            char lastChar = lore[0].charAt(lore[0].length() - 1);
            if (!(lastChar >= '1' && lastChar <= '4'))
                return -1;
            return lastChar - '1';
        }
        return -1;
    }
    
    public void useTemplate(int index)
    {
        if (c.tuningTemplates[index] == null)
            return;
        IInventory inv = McUtils.getOpenedChest();
        ItemStack stack = inv.getStackInSlot(4 + 9 * 5 - 1);
        if (stack != null && stack.getDisplayName().equals("§cClear Points"))
            getMc().playerController.windowClick(getPlayer().openContainer.windowId, 4 + 9 * 5 - 1,
                WindowClickContants.LeftRight.MODE, WindowClickContants.LeftRight.BTN_LEFT, getPlayer());
        remainingToTune = c.tuningTemplates[index].clone();
        timer.reset();
    }
    
    public void setTemplate(int index)
    {
        Matcher tuningMatcher = Pattern.compile("^§7Stat has: §[0-9a-f](\\d+) points$").matcher("");
        IInventory inv = McUtils.getOpenedChest();
        Tuning tuning = new Tuning();
        for (int j = 0; j < 2; j++)
            for (int i = 0; i < 4; i++)
            {
                ItemStack stack = inv.getStackInSlot(2 + i + 9 * (2 + j) - 1);
                if (stack == null)
                    return;
                String[] lore = McUtils.getDisplayLore(stack);
                for (String line : lore)
                {
                    if (tuningMatcher.reset(line).matches())
                    {
                        tuning.setByIndex(i + 4 * j, Integer.parseInt(tuningMatcher.group(1)));
                        break;
                    }
                }
            }
        c.tuningTemplates[index] = tuning;
    }
    
    public void removeTemplate(int index)
    {
        c.tuningTemplates[index] = null;
    }
    
    public void toggleTemplate()
    {
        c.enableTemplates = !c.enableTemplates;
        sendChat("Toggled templates " + c.enableTemplates);
    }
    
    public void setTuningClickDelay(int ticks)
    {
        c.tuningClickDelayTicks = ticks >= 0 ? Utils.clamp(ticks, 0, 10) :
            new TuningTweaksConfig().tuningClickDelayTicks;
        sendChat("Set tuning click delay to " + c.tuningClickDelayTicks + " ticks");
        timer.interval = c.tuningClickDelayTicks;
    }
}
