package a7.tweakception.tweaks;

import a7.tweakception.DevSettings;
import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.mixin.AccessorGuiContainer;
import a7.tweakception.tweaks.enchanting.ChronomatronSolver;
import a7.tweakception.tweaks.enchanting.SuperpairsSolver;
import a7.tweakception.tweaks.enchanting.UltrasequencerSolver;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Utils;
import a7.tweakception.utils.timers.TicksStopwatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import static a7.tweakception.tweaks.GlobalTweaks.getCurrentIsland;

public class EnchantingTweaks extends Tweak
{
    public static class EnchantingTweaksConfig
    {
        public boolean autoSolve = false;
        public int clickDelayTicks = 5;
        public boolean drawDebugInfo = false;
        public int maxChronomatronRounds = 15;
        public int maxUltrasequencerRounds = 20;
    }
    
    private final EnchantingTweaksConfig c;
    private Solver solver = null;
    
    public abstract static class Solver
    {
        protected final EnchantingTweaks.EnchantingTweaksConfig c = Tweakception.configuration.config.enchantingTweaks;
        protected final TicksStopwatch timer = new TicksStopwatch(c.clickDelayTicks);
        protected ContainerChest container;
        protected IInventory inv;
        public Solver(ContainerChest container)
        {
            this.container = container;
            this.inv = container.getLowerChestInventory();
        }
        public abstract void update();
        public abstract void onSlotSet(Container container, Slot slot, ItemStack stack);
        public abstract void drawDebug(int xSize, int guiLeft, int guiTop);
        protected void printDebug(String s, Object... args)
        {
            if (DevSettings.printExpTableDebug)
                McUtils.sendChatf(s, args);
        }
    }
    
    public EnchantingTweaks(Configuration configuration)
    {
        super(configuration, "ET");
        c = configuration.config.enchantingTweaks;
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (!c.autoSolve || solver == null)
            return;
        if (event.phase == TickEvent.Phase.END)
        {
            if (!(Minecraft.getMinecraft().currentScreen instanceof GuiChest))
                solver = null;
            if (solver != null)
                solver.update();
        }
    }
    
    public void onSlotSet(Container container, Slot slot, ItemStack stack)
    {
        if (!isAutoSolveAndExperimentOn())
            return;
        solver.onSlotSet(container, slot, stack);
    }
    
    public void onGuiOpen(GuiOpenEvent event)
    {
        solver = null;
        if (!c.autoSolve)
            return;
        if (getCurrentIsland() != SkyblockIsland.PRIVATE_ISLAND)
            return;
        
        if (event.gui instanceof GuiChest)
        {
            GuiChest chest = (GuiChest) event.gui;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            String name = container.getLowerChestInventory().getName();
            if (container.getLowerChestInventory().getSizeInventory() != 54)
                return;
            if (name.startsWith("Chronomatron (") && name.endsWith(")"))
                solver = new ChronomatronSolver(container);
            else if (name.startsWith("Ultrasequencer (") && name.endsWith(")"))
                solver = new UltrasequencerSolver(container);
            else if (name.startsWith("Superpairs (") && name.endsWith(")"))
                solver = new SuperpairsSolver(container);
        }
    }
    
    public void onGuiDrawPost(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (!isAutoSolveAndExperimentOn())
            return;
        if (!c.drawDebugInfo)
            return;
        AccessorGuiContainer accessor = (AccessorGuiContainer) event.gui;
        int xSize = accessor.getXSize();
        int guiLeft = accessor.getGuiLeft();
        int guiTop = accessor.getGuiTop();
        solver.drawDebug(xSize, guiLeft, guiTop);
    }
    
    public boolean isAutoSolveAndExperimentOn()
    {
        return c.autoSolve && solver != null;
    }
    
    public void toggleAutoSolve()
    {
        c.autoSolve = !c.autoSolve;
        sendChat("AutoSolve: Toggled " + c.autoSolve);
    }
    
    public void setClickDelayTicks(int ticks)
    {
        c.clickDelayTicks = ticks < 0 ? new EnchantingTweaksConfig().clickDelayTicks : Utils.clamp(ticks, 0, 20);
        sendChat("AutoSolve: Set click delay ticks to " + c.clickDelayTicks);
        sendChat("AutoSolve: Low delay = use at your own risk + have a chance of failing");
        sendChat("AutoSolve: Leave empty to reset");
    }
    
    public void toggleDrawDebugInfo()
    {
        c.drawDebugInfo = !c.drawDebugInfo;
        sendChat("AutoSolve: Toggled debug info " + c.drawDebugInfo);
    }
    
    public void setMaxChronomatronRounds(int rounds)
    {
        c.maxChronomatronRounds = Utils.clamp(rounds, 0, 49);
        sendChat("AutoSolve: Set max Chronomatron rounds to " + c.maxChronomatronRounds);
        sendChat("AutoSolve: (max xp = 15, max clicks = 12, disable auto close = 0)");
    }
    
    public void setMaxUltrasequencerRounds(int rounds)
    {
        c.maxUltrasequencerRounds = Utils.clamp(rounds, 0, 36);
        sendChat("AutoSolve: Set max Ultrasequencer rounds to " + c.maxUltrasequencerRounds);
        sendChat("AutoSolve: (max xp = 20, max clicks = 9, disable auto close = 0)");
    }
}
