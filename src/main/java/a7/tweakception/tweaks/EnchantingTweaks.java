package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayDeque;
import java.util.Queue;

import static a7.tweakception.utils.McUtils.*;

public class EnchantingTweaks extends Tweak
{
    public static class EnchantingTweaksConfig
    {
        public boolean autoSolve = false;
    }
    private final EnchantingTweaksConfig c;
    private SolverType currentType = SolverType.NONE;
    private boolean chronoToAdd = false;
    private final Queue<String> chronoOrderNames = new ArrayDeque<>();
    private int chronoLastSize = 0;
    private boolean chronoReplaying = false;
    private int chronoClickDelayTicks = 0;
    
    private enum SolverType
    {
        NONE,
        CHRONOMATRON,
        ULTRASEQUENCER,
        SUPERPAIRS
    }
    
    public EnchantingTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.enchantingTweaks;
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (currentType != SolverType.NONE && !(getMc().currentScreen instanceof GuiChest))
            currentType = SolverType.NONE;
        
        if (event.phase != TickEvent.Phase.END)
            return;
    
        if (!c.autoSolve)
            return;
    
        if (currentType != SolverType.NONE)
        {
            updateContents(false);
            updateClicks();
        }
    }
    
    public void onGuiOpen(GuiOpenEvent event)
    {
        currentType = SolverType.NONE;
        
        if (!c.autoSolve)
            return;
        
        if (event.gui instanceof GuiChest)
        {
            GuiChest chest = (GuiChest)event.gui;
            ContainerChest container = (ContainerChest)chest.inventorySlots;
            String name = container.getLowerChestInventory().getDisplayName().getUnformattedText();
            
            if (container.getLowerChestInventory().getSizeInventory() == 54 &&
                !name.toLowerCase().contains("stakes"))
            {
                if (name.startsWith("Chronomatron"))
                {
                    chronoToAdd = true;
                    chronoReplaying = false;
                    chronoLastSize = 0;
                    chronoClickDelayTicks = 10;
                    chronoOrderNames.clear();
                    currentType = SolverType.CHRONOMATRON;
                }
                else if (name.startsWith("Ultrasequencer"))
                    currentType = SolverType.ULTRASEQUENCER;
                else if (name.startsWith("Superpairs"))
                    currentType = SolverType.SUPERPAIRS;
            }
        }
    }
    
    public void onGuiDrawPost(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (currentType == SolverType.NONE)
            return;
        
        GuiChest chest = (GuiChest)event.gui;
        FontRenderer r = getMc().fontRendererObj;
        int x = chest.guiLeft + 176 + 20;
        int y = chest.guiTop;
        GlStateManager.disableLighting();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 500);
        
        switch (currentType)
        {
            case CHRONOMATRON:
                r.drawStringWithShadow("chrono", x, y, 0xffffffff); y += r.FONT_HEIGHT;
                r.drawStringWithShadow("ToAdd: " + chronoToAdd, x, y, 0xffffffff); y += r.FONT_HEIGHT;
                r.drawStringWithShadow("LastSize: " + chronoLastSize, x, y, 0xffffffff); y += r.FONT_HEIGHT;
                r.drawStringWithShadow("Replaying: " + chronoReplaying, x, y, 0xffffffff); y += r.FONT_HEIGHT;
                r.drawStringWithShadow("ClickDelayTicks: " + chronoClickDelayTicks, x, y, 0xffffffff); y += r.FONT_HEIGHT;
                r.drawStringWithShadow("OrderNames: ", x, y, 0xffffffff); y += r.FONT_HEIGHT;
                for (String name : chronoOrderNames)
                {
                    r.drawStringWithShadow("    " + name, x, y, 0xffffffff); y += r.FONT_HEIGHT;
                }
                break;
        }
        
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
    }
    
    public void updateContents(boolean fromPacket)
    {
        if (!c.autoSolve)
            return;
    
        if (currentType == SolverType.NONE)
            return;
        
        GuiChest chest = (GuiChest)getMc().currentScreen;
        ContainerChest container = (ContainerChest)chest.inventorySlots;
        IInventory inv = container.getLowerChestInventory();
        
        switch (currentType)
        {
            case CHRONOMATRON:
                if (!chronoReplaying)
                {
                    String clayName = null;
                    for (int i = 0; i < inv.getSizeInventory(); i++)
                    {
                        ItemStack stack = inv.getStackInSlot(i);
                        if (stack != null && stack.getItem() == Item.getItemFromBlock(Blocks.stained_hardened_clay))
                        {
                            if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("ench"))
                            {
                                if (clayName != null &&
                                    !stack.getDisplayName().equals(clayName))
                                {
                                    return;
                                }
                                clayName = stack.getDisplayName();
                            }
                        }
                    }
                    ItemStack timerStack = inv.getStackInSlot(5 + 9 * 5 - 1);
                    if (timerStack == null)
                        return;
                    boolean isClock = timerStack.getItem() == Items.clock;
                    boolean isGlowstone = timerStack.getItem() == Item.getItemFromBlock(Blocks.glowstone);
                    boolean filled = chronoOrderNames.size() == chronoLastSize + 1;
                    if (isGlowstone || (isClock && !filled))
                    {
                        if (clayName != null && chronoToAdd) // Clay came, wait until clay go
                        {
                            chronoToAdd = false;
                            chronoOrderNames.add(clayName);
                        }
                        else // Clay gone, start adding
                        {
                            chronoToAdd = true;
                        }
                    }
                    else if (filled)
                    {
                        chronoReplaying = true;
                        chronoToAdd = true;
                        chronoLastSize = chronoOrderNames.size();
                        chronoClickDelayTicks = getWorld().rand.nextInt(15) + 10;
                    }
                }
                break;
            case ULTRASEQUENCER:
                if (fromPacket)
                    return;
                break;
            case SUPERPAIRS:
                if (fromPacket)
                    return;
                break;
        }
    }
    
    private void updateClicks()
    {
        GuiChest chest = (GuiChest)getMc().currentScreen;
        ContainerChest container = (ContainerChest)chest.inventorySlots;
        IInventory inv = container.getLowerChestInventory();
        
        switch (currentType)
        {
            case CHRONOMATRON:
                if (chronoReplaying)
                {
                    if (chronoClickDelayTicks == 0)
                    {
                        chronoClickDelayTicks = getWorld().rand.nextInt(10) + 10;
                    
                        String nameToClick = chronoOrderNames.poll();
                        for (int i = 0; i < inv.getSizeInventory(); i++)
                        {
                            ItemStack stack = inv.getStackInSlot(i);
                            if (stack != null && stack.getItem() == Item.getItemFromBlock(Blocks.stained_glass) &&
                                stack.getDisplayName().equals(nameToClick))
                            {
                                getMc().playerController.windowClick(container.windowId, i, 0, 0, getPlayer());
                                break;
                            }
                        }
                        
                        if (chronoOrderNames.isEmpty())
                            chronoReplaying = false;
                    }
                    else // Replaying but in click delay
                        chronoClickDelayTicks--;
                }
                break;
            case ULTRASEQUENCER:
                break;
            case SUPERPAIRS:
                break;
        }
    }
    
    public void toggleAutoSolve()
    {
        c.autoSolve = !c.autoSolve;
        sendChat("ET-AutoSolve: toggled " + c.autoSolve);
    }
}
