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
import static a7.tweakception.tweaks.GlobalTracker.getTicks;
import static a7.tweakception.tweaks.GlobalTracker.isInSkyblock;
import static a7.tweakception.utils.McUtils.isInGame;
import static a7.tweakception.utils.McUtils.sendChat;

public class InGameEventDispatcher
{
    private boolean trackTickTime = false;
    private long clientTickStart = 0L;
    private float clientTickTime = 0.0f;
    private long renderWorldStart = 0L;
    private float renderWorldTime = 0.0f;
    private long renderOverlayStart = 0L;
    private float renderOverlayTime = 0.0f;
    private long livingRenderStart = 0L;
    private float livingRenderTime = 0.0f;
    private long livingSpecialRenderStart = 0L;
    private float livingSpecialRenderTime = 0.0f;

    public void toggleTickTimeTracking()
    {
        trackTickTime = !trackTickTime;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (trackTickTime)
            clientTickStart = System.nanoTime();
        if (!isInGame()) return;

        globalTracker.onTick(event);

        if (!isInSkyblock()) return;

        fairyTracker.onTick(event);
        dungeonTweaks.onTick(event);
        crimsonTweaks.onTick(event);
        slayerTweaks.onTick(event);

        if (trackTickTime)
        {
            clientTickTime = clientTickTime * 0.2f + (System.nanoTime() - clientTickStart) * 0.8f;
            if (event.phase == TickEvent.Phase.END && getTicks() % 20 == 0)
            {
                sendChat("Client tick: " + clientTickTime / 1000.0f + " us");
                sendChat("World render: " + renderWorldTime / 1000.0f + " us");
                sendChat("Overlay render: " + renderOverlayTime / 1000.0f + " us");
                sendChat("Living Render: " + livingRenderTime / 1000.0f + " us");
                sendChat("Living special render: " + livingSpecialRenderTime / 1000.0f + " us");
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
            renderWorldStart = System.nanoTime();
        if (!isInGame()) return;
        if (!isInSkyblock()) return;

        fairyTracker.onRenderLast(event);
        dungeonTweaks.onRenderLast(event);
        crimsonTweaks.onRenderLast(event);
        slayerTweaks.onRenderLast(event);
        miningTweaks.onRenderLast(event);

        if (trackTickTime)
            renderWorldTime = renderWorldTime * 0.2f + (System.nanoTime() - renderWorldStart) * 0.8f;
    }

    @SubscribeEvent
    public void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event)
    {
        if (trackTickTime)
            renderOverlayStart = System.nanoTime();
        if (!isInGame()) return;
        if (!isInSkyblock()) return;

        dungeonTweaks.onRenderGameOverlayPost(event);
        crimsonTweaks.onRenderGameOverlayPost(event);

        if (trackTickTime)
            renderOverlayTime = renderOverlayTime * 0.2f + (System.nanoTime() - renderOverlayStart) * 0.8f;
    }

    @SubscribeEvent
    public void onLivingRenderPost(RenderLivingEvent.Post event)
    {
        if (trackTickTime)
            livingRenderStart = System.nanoTime();
        if (!isInSkyblock()) return;

        dungeonTweaks.onLivingRenderPost(event);
        if (trackTickTime)
            livingRenderTime = livingRenderTime * 0.2f + (System.nanoTime() - livingRenderStart) * 0.8f;
    }

    // Called on RenderLivingEntity.renderName()
    @SubscribeEvent
    public void onLivingSpecialRenderPre(RenderLivingEvent.Specials.Pre event)
    {
        if (trackTickTime)
            livingSpecialRenderStart = System.nanoTime();
        if (!isInSkyblock()) return;

        dungeonTweaks.onLivingSpecialRenderPre(event);
        if (trackTickTime)
            livingSpecialRenderTime = livingSpecialRenderTime * 0.2f + (System.nanoTime() - livingSpecialRenderStart) * 0.8f;
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
