package a7.tweakception;

import a7.tweakception.events.IslandChangedEvent;
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

import static a7.tweakception.Tweakception.*;
import static a7.tweakception.tweaks.GlobalTracker.getTicks;
import static a7.tweakception.tweaks.GlobalTracker.isInSkyblock;
import static a7.tweakception.utils.McUtils.isInGame;
import static a7.tweakception.utils.McUtils.sendChat;

public class InGameEventDispatcher
{
    private boolean trackTickTime = false;
    private final long[] tickStartTimes = new long[5];
    private final float[] tickTimes = new float[5];
    private final float[] lastTickTimes = new float[5];

    public void toggleTickTimeTracking()
    {
        trackTickTime = !trackTickTime;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (trackTickTime)
            tickStartTimes[0] = System.nanoTime();
        if (!isInGame()) return;

        globalTracker.onTick(event);

        if (!isInSkyblock()) return;

        fairyTracker.onTick(event);
        dungeonTweaks.onTick(event);
        crimsonTweaks.onTick(event);
        slayerTweaks.onTick(event);
        miningTweaks.onTick(event);

        if (trackTickTime)
        {
            tickTimes[0] = tickTimes[0] * 0.2f + (System.nanoTime() - tickStartTimes[0]) * 0.8f;
            if (event.phase == TickEvent.Phase.END && getTicks() % 20 == 0)
            {
                sendChat("Client tick: " + tickTimes[0] / 1000.0f + " us");
                sendChat("World render: " + tickTimes[1] / 1000.0f + " us");
                sendChat("Overlay render: " + tickTimes[2] / 1000.0f + " us");
                sendChat("Living Render: " + tickTimes[3] / 1000.0f + " us");
                sendChat("Living special render: " + tickTimes[4] / 1000.0f + " us");
            }
        }
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
        if (trackTickTime)
            tickStartTimes[1] = System.nanoTime();
        if (!isInGame()) return;
        if (!isInSkyblock()) return;

        fairyTracker.onRenderLast(event);
        dungeonTweaks.onRenderLast(event);
        crimsonTweaks.onRenderLast(event);
        slayerTweaks.onRenderLast(event);
        miningTweaks.onRenderLast(event);

        if (trackTickTime)
            tickTimes[1] = tickTimes[1] * 0.2f + (System.nanoTime() - tickStartTimes[1]) * 0.8f;
    }

    @SubscribeEvent
    public void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event)
    {
        if (trackTickTime)
            tickStartTimes[2] = System.nanoTime();
        if (!isInGame()) return;
        if (!isInSkyblock()) return;

        dungeonTweaks.onRenderGameOverlayPost(event);
        crimsonTweaks.onRenderGameOverlayPost(event);
        autoFish.onRenderGameOverlayPost(event);
        slayerTweaks.onRenderGameOverlayPost(event);

        if (trackTickTime)
            tickTimes[2] = tickTimes[2] * 0.2f + (System.nanoTime() - tickStartTimes[2]) * 0.8f;
    }

    @SubscribeEvent()
    public void onLivingRenderPre(RenderLivingEvent.Pre event)
    {
        if (trackTickTime)
            tickStartTimes[3] = System.nanoTime();
        if (!isInSkyblock()) return;

        dungeonTweaks.onLivingRenderPre(event);
        globalTracker.onLivingRenderPre(event);

        if (trackTickTime)
            tickTimes[3] = tickTimes[3] * 0.2f + (System.nanoTime() - tickStartTimes[3]) * 0.8f;
    }

    // Called on RenderLivingEntity.renderName()
    @SubscribeEvent
    public void onLivingSpecialRenderPre(RenderLivingEvent.Specials.Pre event)
    {
        if (trackTickTime)
            tickStartTimes[4] = System.nanoTime();
        if (!isInSkyblock()) return;

        dungeonTweaks.onLivingSpecialRenderPre(event);
        globalTracker.onLivingSpecialRenderPre(event);

        if (trackTickTime)
            tickTimes[4] = tickTimes[4] * 0.2f + (System.nanoTime() - tickStartTimes[4]) * 0.8f;
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
        autoFish.onChatReceived(event);
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
    public void onIslandChanged(IslandChangedEvent event)
    {
        dungeonTweaks.onIslandChanged(event);
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemTooltip(ItemTooltipEvent event)
    {
        if (!isInSkyblock()) return;

        dungeonTweaks.onItemTooltip(event);
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
