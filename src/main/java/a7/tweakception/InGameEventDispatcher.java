package a7.tweakception;

import a7.tweakception.events.IslandChangedEvent;
import a7.tweakception.utils.McUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import static a7.tweakception.Tweakception.*;
import static a7.tweakception.tweaks.GlobalTweaks.isInSkyblock;
import static a7.tweakception.utils.McUtils.*;

public class InGameEventDispatcher
{
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        McUtils.chestUpdatedThisTick = false;
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
        gardenTweaks.onRenderLast(event);
    }
    
    @SubscribeEvent
    public void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event)
    {
        if (!isInGame()) return;
        if (!isInSkyblock()) return;
        
        overlayManager.onRenderGameOverlayPost(event);
        crimsonTweaks.onRenderGameOverlayPost(event);
    }
    
    @SubscribeEvent()
    public void onLivingRenderPre(RenderLivingEvent.Pre<?> event)
    {
        if (!isInSkyblock()) return;
        
        dungeonTweaks.onLivingRenderPre(event);
        globalTweaks.onLivingRenderPre(event);
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
        if (!isInSkyblock()) return;
        
        dungeonTweaks.onLivingSpecialRenderPre(event);
        globalTweaks.onLivingSpecialRenderPre(event);
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
        gardenTweaks.onWorldUnload(event);
    }
    
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event)
    {
        globalTweaks.onChunkLoad(event);
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
        
        globalTweaks.onGuiOpen(event);
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
        gardenTweaks.onGuiDrawPost(event);
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
        final int scaledHeight = scaledresolution.getScaledHeight();
        int mouseX = Mouse.getX() / scaledresolution.getScaleFactor();
        int mouseY = scaledHeight - Mouse.getY() / scaledresolution.getScaleFactor();
        
        autoRunes.onGuiMouseInput(event, mouseX, mouseY);
        if (event.isCanceled())
            return;
        gardenTweaks.onGuiMouseInput(event, mouseX, mouseY);
        if (event.isCanceled())
            return;
    }
    
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        if (!isInGame()) return;
        if (!isInSkyblock()) return;
        
        fairyTracker.onKeyInput(event);
        gardenTweaks.onKeyInput(event);
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
