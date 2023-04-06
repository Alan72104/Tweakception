package a7.tweakception;

import a7.tweakception.events.IslandChangedEvent;
import a7.tweakception.events.PacketReceiveEvent;
import a7.tweakception.events.PacketSendEvent;
import a7.tweakception.utils.Utils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.text.DecimalFormat;

import static a7.tweakception.Tweakception.*;
import static a7.tweakception.tweaks.GlobalTweaks.isInSkyblock;
import static a7.tweakception.utils.McUtils.*;

public class InGameEventDispatcher
{
    private static final String[] TICK_TYPES =
        {
            "Tick",
            "World",
            "Overlay",
            "Living",
            "LivingSpe"
        };
    private final DecimalFormat format = new DecimalFormat("#.##");
    private final long[] tickStartTimes = new long[TICK_TYPES.length];
    private final float[] startPhaseTickTimes = new float[TICK_TYPES.length];
    private final float[] fullTickTimes = new float[TICK_TYPES.length];
    private final float[] lastFullTickTimes = new float[TICK_TYPES.length];
    private boolean notifyLagSpike = false;
    private float notifyThreshold = 1000.0f;
    private float avgAggregationValue = 0.4f;
    
    public void toggleNotifyLagSpike()
    {
        notifyLagSpike = !notifyLagSpike;
    }
    
    public void setNotifyThreshold(float f)
    {
        if (f < 1.0f)
            f = 1000.0f;
        notifyThreshold = f;
        sendChat("TC: set lag spike notify threshold to " + format.format(notifyThreshold));
    }
    
    public void setAggregationValue(float f)
    {
        f = Utils.clamp(f, 0.0f, 1.0f);
        if (f == 0.0f)
            f = 0.4f;
        avgAggregationValue = f;
        sendChatf("TC: set tick time avg value aggregation to %.1f old, %.1f new",
            1.0f - avgAggregationValue, avgAggregationValue);
    }
    
    private void startFunc(int i)
    {
        tickStartTimes[i] = System.nanoTime();
    }
    
    private void endFuncAndAddNum(TickEvent.Phase phase, int i)
    {
        long end = System.nanoTime();
        
        if (phase == TickEvent.Phase.START)
        {
            startPhaseTickTimes[i] = end - tickStartTimes[i];
            return;
        }
        
        fullTickTimes[i] = fullTickTimes[i] * (1.0f - avgAggregationValue) +
            ((end - tickStartTimes[i]) + startPhaseTickTimes[i]) * avgAggregationValue;
        
        if (notifyLagSpike && fullTickTimes[i] > lastFullTickTimes[i] * notifyThreshold && getPlayer() != null)
        {
            getPlayer().addChatMessage(new ChatComponentText(
                "TC: " + TICK_TYPES[i] + " is taking " + format.format(notifyThreshold) + "x longer than usual! (" +
                    format.format(fullTickTimes[i] / 1000.0f) + " us)"));
        }
        lastFullTickTimes[i] = fullTickTimes[i];
    }
    
    private void endFuncAndAddNum(int i)
    {
        long end = System.nanoTime();
        
        fullTickTimes[i] = fullTickTimes[i] * (1.0f - avgAggregationValue) +
            (end - tickStartTimes[i]) * avgAggregationValue;
        
        if (notifyLagSpike && fullTickTimes[i] > lastFullTickTimes[i] * notifyThreshold && getPlayer() != null)
        {
            getPlayer().addChatMessage(new ChatComponentText(
                "TC: " + TICK_TYPES[i] + " is taking " + format.format(notifyThreshold) + "x longer than usual! (" +
                    format.format(fullTickTimes[i] / 1000.0f) + " us)"));
        }
        lastFullTickTimes[i] = fullTickTimes[i];
    }
    
    @SubscribeEvent
    public void onPacketReceive(PacketReceiveEvent event)
    {
        globalTweaks.onPacketReceive(event);
    }
    
    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event)
    {
        globalTweaks.onPacketSend(event);
    }
    
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        startFunc(0);
        
        globalTweaks.onTick(event);
        
        if (!isInGame()) return;
        if (!isInSkyblock()) return;
        
        fairyTracker.onTick(event);
        dungeonTweaks.onTick(event);
        crimsonTweaks.onTick(event);
        slayerTweaks.onTick(event);
        miningTweaks.onTick(event);
        tuningTweaks.onTick(event);
        foragingTweaks.onTick(event);
        fishingTweaks.onTick(event);
        enchantingTweaks.onTick(event);
        bazaarTweaks.onTick(event);
        autoRunes.onTick(event);
        gardenTweaks.onTick(event);
        giftTweaks.onTick(event);
        overlayManager.onTick(event);
        guildBridge.onTick(event);
        
        endFuncAndAddNum(event.phase, 0);
    }
    
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (!isInSkyblock()) return;
        
        fishingTweaks.onPlayerTick(event);
    }
    
    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event)
    {
        startFunc(1);
        if (!isInGame()) return;
        if (!isInSkyblock()) return;
        
        fairyTracker.onRenderLast(event);
        dungeonTweaks.onRenderLast(event);
        crimsonTweaks.onRenderLast(event);
        slayerTweaks.onRenderLast(event);
        miningTweaks.onRenderLast(event);
        foragingTweaks.onRenderLast(event);
        globalTweaks.onRenderLast(event);
        giftTweaks.onRenderLast(event);
        
        endFuncAndAddNum(1);
    }
    
    @SubscribeEvent
    public void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event)
    {
        startFunc(2);
        if (!isInGame()) return;
        if (!isInSkyblock()) return;
        
        overlayManager.onRenderGameOverlayPost(event);
        crimsonTweaks.onRenderGameOverlayPost(event);
        
        endFuncAndAddNum(2);
    }
    
    @SubscribeEvent()
    public void onLivingRenderPre(RenderLivingEvent.Pre<?> event)
    {
        startFunc(3);
        if (!isInSkyblock()) return;
        
        dungeonTweaks.onLivingRenderPre(event);
        globalTweaks.onLivingRenderPre(event);
        
        endFuncAndAddNum(3);
    }
    
    @SubscribeEvent()
    public void onLivingRenderPost(RenderLivingEvent.Post<?> event)
    {
        if (!isInSkyblock()) return;
        
        dungeonTweaks.onLivingRenderPost(event);
    }
    
    // Called on RenderLivingEntity.renderName()
    @SubscribeEvent
    public void onLivingSpecialRenderPre(RenderLivingEvent.Specials.Pre<?> event)
    {
        startFunc(4);
        if (!isInSkyblock()) return;
        
        dungeonTweaks.onLivingSpecialRenderPre(event);
        globalTweaks.onLivingSpecialRenderPre(event);
        
        endFuncAndAddNum(4);
    }
    
    @SubscribeEvent
    public void onRenderBlockOverlay(DrawBlockHighlightEvent event)
    {
        if (!isInSkyblock()) return;
        
        dungeonTweaks.onRenderBlockOverlay(event);
        globalTweaks.onRenderBlockOverlay(event);
    }
    
    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event)
    {
        if (!isInSkyblock()) return;
        
        dungeonTweaks.onInteract(event);
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onClientChatReceived(ClientChatReceivedEvent event)
    {
        globalTweaks.onChatReceivedGlobal(event);
        dungeonTweaks.onChatReceivedGlobal(event);
        guildBridge.onChatReceivedGlobal(event);
        if (!isInSkyblock()) return;
        
        globalTweaks.onChatReceived(event);
        dungeonTweaks.onChatReceived(event);
        fishingTweaks.onChatReceived(event);
        slayerTweaks.onChatReceived(event);
        miningTweaks.onChatReceived(event);
        giftTweaks.onChatReceived(event);
    }
    
    @SubscribeEvent
    public void onLivingEntityUpdate(LivingEvent.LivingUpdateEvent event)
    {
        if (!isInSkyblock()) return;
        
        globalTweaks.onEntityUpdate(event);
        dungeonTweaks.onEntityUpdate(event);
        fairyTracker.onEntityUpdate(event);
        giftTweaks.onEntityUpdate(event);
    }
    
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event)
    {
        globalTweaks.onWorldLoad(event);
        dungeonTweaks.onWorldLoad(event);
    }
    
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event)
    {
        dungeonTweaks.onWorldUnload(event);
        crimsonTweaks.onWorldUnload(event);
        slayerTweaks.onWorldUnload(event);
        miningTweaks.onWorldUnload(event);
        giftTweaks.onWorldUnload(event);
    }
    
    @SubscribeEvent
    public void onIslandChanged(IslandChangedEvent event)
    {
    }
    
    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent event)
    {
        if (!isInGame()) return;
        if (!isInSkyblock()) return;
        
        dungeonTweaks.onPlaySound(event);
    }
    
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event)
    {
        if (!isInSkyblock()) return;
        
        dungeonTweaks.onGuiOpen(event);
        enchantingTweaks.onGuiOpen(event);
    }
    
    @SubscribeEvent
    public void onGuiDrawPost(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (!isInSkyblock()) return;
        
        GlStateManager.disableLighting();
        
        enchantingTweaks.onGuiDrawPost(event);
        autoRunes.onGuiDrawPost(event);
        globalTweaks.onGuiDrawPost(event);
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onItemTooltipHighest(ItemTooltipEvent event)
    {
        if (!isInSkyblock()) return;
        
        bazaarTweaks.onItemTooltip(event);
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemTooltipLowest(ItemTooltipEvent event)
    {
        if (!isInSkyblock()) return;
        
        dungeonTweaks.onItemTooltip(event);
        tuningTweaks.onItemTooltip(event);
        gardenTweaks.onItemTooltip(event);
        globalTweaks.onItemTooltip(event);
    }
    
    @SubscribeEvent
    public void onGuiKeyInputPre(GuiScreenEvent.KeyboardInputEvent.Pre event)
    {
        if (!isInSkyblock()) return;
        
        globalTweaks.onGuiKeyInputPre(event);
    }
    
    @SubscribeEvent
    public void onGuiScreenMouse(GuiScreenEvent.MouseInputEvent.Pre event)
    {
        if (!isInSkyblock()) return;
        
        final ScaledResolution scaledresolution = new ScaledResolution(getMc());
        final int scaledWidth = scaledresolution.getScaledWidth();
        final int scaledHeight = scaledresolution.getScaledHeight();
        int mouseX = Mouse.getX() / scaledresolution.getScaleFactor();
        int mouseY = scaledHeight - Mouse.getY() / scaledresolution.getScaleFactor();
        
        autoRunes.onGuiMouseInput(event, mouseX, mouseY);
        if (event.isCanceled())
            return;
    }
    
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        if (!isInGame()) return;
        if (!isInSkyblock()) return;
        
        fairyTracker.onKeyInput(event);
        globalTweaks.onKeyInput(event);
    }
    
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
        if (!isInSkyblock()) return;
        
        dungeonTweaks.onEntityJoinWorld(event);
        slayerTweaks.onEntityJoinWorld(event);
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFogDensitySet(EntityViewRenderEvent.FogDensity event)
    {
        if (!isInSkyblock()) return;
        
        dungeonTweaks.onFogDensitySet(event);
        if (event.isCanceled())
            return;
    }
}
