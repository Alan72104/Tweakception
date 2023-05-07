package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import a7.tweakception.mixin.AccessorGuiContainer;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static a7.tweakception.tweaks.GlobalTweaks.getTicks;
import static a7.tweakception.utils.McUtils.*;

public class AutoRunes extends Tweak
{
    private final AutoRunesConfig c;
    
    public static class AutoRunesConfig
    {
        long totalFused = 0;
    }
    
    private boolean enabled = false;
    private boolean inMenu = false;
    private State state = State.NONE;
    private RuneType typeToPut = null;
    private int nextClickTicks = 0;
    private int count = 0;
    
    private enum State
    {
        NONE, PUTTING, CLICKING, SAVING
    }
    
    private static class RuneType
    {
        public final String name;
        public final int level;
        
        public RuneType(String name, int level)
        {
            this.name = name;
            this.level = level;
        }
        
        @Override
        public int hashCode()
        {
            return name.hashCode() * level * level * level;
        }
        
        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            else if (o instanceof RuneType)
            {
                RuneType other = (RuneType) o;
                return this.name.equals(other.name) && this.level == other.level;
            }
            return false;
        }
    }
    
    public AutoRunes(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.autoRunes;
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;
        
        if (McUtils.getOpenedChest() != null)
        {
            IInventory inv = McUtils.getOpenedChest();
            if (inv.getName().equals("Runic Pedestal") &&
                inv.getSizeInventory() == 54 &&
                McUtils.checkStackInInv(inv, 9 + 9 * 4 - 1, Items.cauldron, "§eRune Removal"))
            {
                if (!inMenu || !enabled)
                {
                    enabled = false;
                    typeToPut = null;
                    state = State.NONE;
                }
                
                if (enabled)
                {
                    int[] slots = {2 + 9 * 2 - 1, 8 + 9 * 2 - 1};
                    switch (state)
                    {
                        case NONE:
                        {
                            for (int slot : slots)
                            {
                                ItemStack stack = inv.getStackInSlot(slot);
                                if (stack != null && getTicks() >= nextClickTicks)
                                {
                                    nextClickTicks = getTicks() + 15 + getWorld().rand.nextInt(5);
                                    getMc().playerController.windowClick(getPlayer().openContainer.windowId, slot,
                                        0, 1, getPlayer());
                                    break;
                                }
                            }
                            
                            Map<RuneType, Integer> runesWeHave = new HashMap<>();
                            for (ItemStack stack : getPlayer().inventory.mainInventory)
                            {
                                RuneType type = getRuneType(stack);
                                if (type != null && type.level < 3)
                                {
                                    if (runesWeHave.merge(type, stack.stackSize, Integer::sum) >= 2)
                                    {
                                        typeToPut = type;
                                        state = State.PUTTING;
                                        nextClickTicks = getTicks() + 15 + getWorld().rand.nextInt(5);
                                        count = 0;
                                        break;
                                    }
                                }
                            }
                            break;
                        }
                        case PUTTING:
                        {
                            if (getTicks() >= nextClickTicks)
                            {
                                boolean clicked = false;
                                for (int i = 0; i < 36; i++)
                                {
                                    ItemStack stack = getPlayer().inventory.mainInventory[i];
                                    RuneType type = getRuneType(stack);
                                    if (type != null && type.level < 3)
                                    {
                                        if (type.equals(typeToPut))
                                        {
                                            count++;
                                            nextClickTicks = getTicks() + 8 + getWorld().rand.nextInt(8);
                                            getMc().playerController.windowClick(getPlayer().openContainer.windowId,
                                                i < 9 ? i + 54 + 9 * 3 : i + 54 - 9,
                                                0, 1, getPlayer());
                                            
                                            if (count == 2)
                                                state = State.CLICKING;
                                            
                                            clicked = true;
                                            break;
                                        }
                                    }
                                }
                                
                                if (!clicked)
                                {
                                    state = State.NONE;
                                    typeToPut = null;
                                }
                            }
                            break;
                            
                        }
                        case CLICKING:
                        {
                            if (getRuneType(inv.getStackInSlot(slots[0])) != null &&
                                getRuneType(inv.getStackInSlot(slots[0])) != null)
                            {
                                nextClickTicks = getTicks() + 4 + getWorld().rand.nextInt(4);
                                getMc().playerController.windowClick(getPlayer().openContainer.windowId, 5 + 9 - 1,
                                    0, 0, getPlayer());
                                state = State.SAVING;
                            }
                            else if (getTicks() >= nextClickTicks + 20 * 5)
                            {
                                state = State.NONE;
                                typeToPut = null;
                            }
                            break;
                        }
                        case SAVING:
                        {
                            ItemStack result = inv.getStackInSlot(5 + 9 * 3 - 1);
                            if (result != null && getRuneType(result) != null)
                            {
                                String[] lore = McUtils.getDisplayLore(result);
                                if (!lore[lore.length - 1].equals("§cABOVE§a to combine."))
                                {
                                    nextClickTicks = getTicks() + 4 + getWorld().rand.nextInt(4);
                                    getMc().playerController.windowClick(getPlayer().openContainer.windowId, 5 + 9 * 3 - 1,
                                        0, 1, getPlayer());
                                    state = State.NONE;
                                    typeToPut = null;
                                    c.totalFused++;
                                }
                            }
                            
                            if (getTicks() >= nextClickTicks + 20 * 4)
                            {
                                getMc().playerController.windowClick(getPlayer().openContainer.windowId, 5 + 9 * 3 - 1,
                                    0, 1, getPlayer());
                                state = State.NONE;
                                typeToPut = null;
                                
                                if (getRuneType(result) != null)
                                    c.totalFused++;
                            }
                            break;
                        }
                    }
                }
                
                inMenu = true;
                return;
            }
        }
        
        inMenu = false;
    }
    
    public void onGuiDrawPost(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (!inMenu)
            return;
        
        AccessorGuiContainer accessor = (AccessorGuiContainer) event.gui;
        int xSize = accessor.getXSize();
        int guiLeft = accessor.getGuiLeft();
        int guiTop = accessor.getGuiTop();
        
        Color color;
        if (enabled)
            color = new Color(50, 50, 50);
        else
            color = new Color(127, 127, 127);
        GuiScreen.drawRect(guiLeft + xSize + 20, guiTop,
            guiLeft + xSize + 20 + 60, guiTop + 10,
            color.getRGB());
        int width = getMc().fontRendererObj.getStringWidth("AutoRunes");
        getMc().fontRendererObj.drawString("AutoRunes",
            guiLeft + xSize + 20 + 30 - width / 2, guiTop + 1, 0xFFFFFFFF);
        getMc().fontRendererObj.drawString("State: " + state.toString(),
            guiLeft + xSize + 20, guiTop + 30, 0xFFFFFFFF);
        getMc().fontRendererObj.drawString("Total fused: " + c.totalFused,
            guiLeft + xSize + 20, guiTop + 30 + 9, 0xFFFFFFFF);
    }
    
    public void onGuiMouseInput(GuiScreenEvent.MouseInputEvent.Pre event, int x, int y)
    {
        if (!inMenu)
            return;
        
        AccessorGuiContainer accessor = (AccessorGuiContainer) event.gui;
        int xSize = accessor.getXSize();
        int guiLeft = accessor.getGuiLeft();
        int guiTop = accessor.getGuiTop();
        
        if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0 &&
            x >= guiLeft + xSize + 20 && x <= guiLeft + xSize + 20 + 60 &&
            y >= guiTop && y <= guiTop + 10)
        {
            enabled = !enabled;
            event.setCanceled(true);
        }
    }
    
    private RuneType getRuneType(ItemStack stack)
    {
        if (stack == null)
            return null;
        String id = Utils.getSkyblockItemId(stack);
        if (id == null)
            return null;
        NBTTagCompound extra = McUtils.getExtraAttributes(stack);
        if (extra == null)
            return null;
        NBTTagCompound runes = extra.getCompoundTag("runes");
        if (runes == null)
            return null;
        Set<String> keys = runes.getKeySet();
        if (keys.size() != 1)
            return null;
        String type = keys.toArray(new String[0])[0];
        int level = runes.getInteger(type);
        return new RuneType(type, level);
    }
}
