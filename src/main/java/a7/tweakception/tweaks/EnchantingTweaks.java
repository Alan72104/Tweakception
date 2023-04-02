package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import a7.tweakception.mixin.AccessorGuiContainer;
import a7.tweakception.utils.Pair;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static a7.tweakception.utils.McUtils.getMc;
import static a7.tweakception.utils.McUtils.sendChat;

public class EnchantingTweaks extends Tweak
{
    public static class EnchantingTweaksConfig
    {
        public boolean autoSolve = false;
    }
    
    private final EnchantingTweaksConfig c;
    private SolverType currentType = SolverType.NONE;
    private final boolean hasAdded = false;
    private final int clicks = 0;
    private final long lastClickTime = 0L;
    private final List<Pair<Integer, String>> chronomatronOrder = new ArrayList<>(28);
    private final int lastAdded = 0;
    private final HashMap<Integer, Integer> ultrasequencerOrder = new HashMap<>();
    
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
        if (event.phase != TickEvent.Phase.END)
            return;
        
        if (!c.autoSolve)
            return;
        
        
        if (getMc().currentScreen instanceof GuiChest)
        {
            GuiChest chest = (GuiChest) getMc().currentScreen;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            IInventory inv = container.getLowerChestInventory();
            switch (currentType)
            {
                case CHRONOMATRON:
                {
                    ItemStack stack = inv.getStackInSlot(9 * 5 + 5 - 1);
//                    if (stack !- null)
                    break;
                }
            }
        }
        else
            currentType = SolverType.NONE;
    }
    
    public void onGuiOpen(GuiOpenEvent event)
    {
        currentType = SolverType.NONE;
        
        if (!c.autoSolve)
            return;
        
        if (event.gui instanceof GuiChest)
        {
            GuiChest chest = (GuiChest) event.gui;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            String name = container.getLowerChestInventory().getDisplayName().getUnformattedText();
            
            if (container.getLowerChestInventory().getSizeInventory() == 54)
            {
                if (name.startsWith("Chronomatron ("))
                {
                    currentType = SolverType.CHRONOMATRON;
                }
                else if (name.startsWith("Ultrasequencer ("))
                    currentType = SolverType.ULTRASEQUENCER;
                else if (name.startsWith("Superpairs("))
                    currentType = SolverType.SUPERPAIRS;
            }
        }
    }
    
    public void onGuiDrawPost(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (currentType == SolverType.NONE)
            return;
        
        GuiChest chest = (GuiChest) event.gui;
        AccessorGuiContainer accessor = (AccessorGuiContainer) event.gui;
        FontRenderer r = getMc().fontRendererObj;
        int x = accessor.getGuiLeft() + 176 + 20;
        int y = accessor.getGuiTop();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 500);
        
        switch (currentType)
        {
            case CHRONOMATRON:
                break;
        }
        
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
    }
    
    public void toggleAutoSolve()
    {
        c.autoSolve = !c.autoSolve;
        sendChat("ET-AutoSolve: toggled " + c.autoSolve);
    }
}
