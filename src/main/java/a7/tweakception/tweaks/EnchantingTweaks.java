package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class EnchantingTweaks extends Tweak
{
    public static class EnchantingTweaksConfig
    {
        public boolean autoSolve = false;
    }
    
    private final EnchantingTweaksConfig c;
    private SolverType currentType = SolverType.NONE;
    
    private enum SolverType
    {
        NONE,
        CHRONOMATRON,
        ULTRASEQUENCER,
        SUPERPAIRS
    }
    
    public EnchantingTweaks(Configuration configuration)
    {
        super(configuration, "ET");
        c = configuration.config.enchantingTweaks;
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
    }
    
    public void onGuiOpen(GuiOpenEvent event)
    {
    }
    
    public void onGuiDrawPost(GuiScreenEvent.DrawScreenEvent.Post event)
    {
    }
    
    public void toggleAutoSolve()
    {
        c.autoSolve = !c.autoSolve;
        sendChat("AutoSolve: Toggled " + c.autoSolve);
    }
}
