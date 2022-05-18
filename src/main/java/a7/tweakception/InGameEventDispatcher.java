package a7.tweakception;

import net.minecraftforge.client.event.*;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import static a7.tweakception.Tweakception.*;
import static a7.tweakception.tweaks.GlobalTracker.isInSkyblock;
import static a7.tweakception.utils.McUtils.isInGame;

public class InGameEventDispatcher
{
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (!isInGame()) return;

        globalTracker.onTick(event);

        if (!isInSkyblock()) return;

        fairyTracker.onTick(event);
        dungeonTweaks.onTick(event);
        crimsonTweaks.onTick(event);
        slayerTweaks.onTick(event);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (!isInSkyblock()) return;

        autoFish.onPlayerTick(event);
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
    }

    @SubscribeEvent
    public void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event)
    {
        if (!isInGame()) return;
        if (!isInSkyblock()) return;

        dungeonTweaks.onRenderGameOverlayPost(event);
        crimsonTweaks.onRenderGameOverlayPost(event);
    }

    @SubscribeEvent
    public void onLivingRenderPost(RenderLivingEvent.Post event)
    {
        if (!isInSkyblock()) return;

        dungeonTweaks.onLivingRenderPost(event);
    }

    // Called on RenderLivingEntity.renderName()
    @SubscribeEvent
    public void onLivingSpecialRenderPre(RenderLivingEvent.Specials.Pre event)
    {
        if (!isInSkyblock()) return;

        dungeonTweaks.onLivingSpecialRenderPre(event);
    }

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event)
    {
        if (!isInSkyblock()) return;

        dungeonTweaks.onInteract(event);
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event)
    {
        if (!isInSkyblock()) return;

    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onClientChatReceived(ClientChatReceivedEvent event)
    {
        if (!isInSkyblock()) return;

        dungeonTweaks.onChatReceived(event);
    }

    @SubscribeEvent
    public void onLivingEntityUpdate(LivingEvent.LivingUpdateEvent event)
    {
        if (!isInSkyblock()) return;

        dungeonTweaks.onEntityUpdate(event);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event)
    {
        dungeonTweaks.onWorldUnload(event);
        crimsonTweaks.onWorldUnload(event);
        slayerTweaks.onWorldUnload(event);
        miningTweaks.onWorldUnload(event);
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
    }

    @SubscribeEvent
    public void onGuiKeyInputPre(GuiScreenEvent.KeyboardInputEvent.Pre event)
    {
        if (!isInSkyblock()) return;

        globalTracker.onGuiKeyInputPre(event);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        if (!isInGame()) return;
        if (!isInSkyblock()) return;

        fairyTracker.onKeyInput(event);
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
        if (!isInSkyblock()) return;

        fairyTracker.onEntityJoinWorld(event);
        dungeonTweaks.onEntityJoinWorld(event);
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
