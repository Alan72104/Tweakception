package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.utils.McUtils;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.function.Supplier;

import static a7.tweakception.tweaks.GlobalTracker.*;
import static a7.tweakception.utils.McUtils.*;
import static a7.tweakception.utils.Utils.*;

public class AutoFish extends Tweak
{
    private final AutoFishConfig c;
    public static class AutoFishConfig
    {
        public boolean enableDebugInfo = false;
        public int minRetrieveDelay = 3;
        public int maxRetrieveDelay = 7;
        public int minRecastDelay = 10;
        public int maxRecastDelay = 18;
        public int minCatchesToMove = 6;
        public int maxCatchesToMove = 9;
        public int headMovingTicks = 7;
        public float headMovingYawRange = 4.0f;
        public float headMovingPitchRange = 4.0f;
    }
    private static boolean exceptionThrown = false;
    private boolean enable = false; // Do not save
    private boolean reflectionTried = false;
    private boolean reflectionSuccess = false;
    private Class<?> neuFishingHelperClass = null;
    private Object neuFishingHelperInstance = null;
    private Field neuWarningStateField = null;
    private Field neuLastRodCastMillisField = null;
    private Enum<?> neuWarningState = null;
    private long neuLastRodCastMillis = 0L;
//    private boolean lastTickFishing = false;
    private NeuState neuState = NeuState.NOTHING;
    private FishingState state = FishingState.NOTHING;
    private int retrieveWaitingTicks = 0;
    private int lastRetrieveClickTicks = 0;
    private int pendingRecastTicks = 0;
    private int lastRecastClickTicks = 0;
    private int catchesLeftToMove = 0;
    private int lastCatchTicks = 0;
    private int movingTicks = 0;
    private int movingTicksTarget = 0;
    private float movingTargetDeltaYaw = 0.0f;
    private float movingTargetDeltaPitch = 0.0f;
    private boolean waitForSlugfish = false;
    private enum NeuState
    {
        NOTHING,
        FISH_INCOMING,
        FISH_HOOKED
    }
    private enum FishingState
    {
        NOTHING,
        WAITING,
        HOOKED,
        WAITING_TO_CAST
    }

    public AutoFish(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.autoFish;
    }

    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        if (!enable) return;

        if (!event.player.getUniqueID().equals(getPlayer().getUniqueID())) return;

        if (!reflectionTried)
            getNeuClass();

        if (reflectionTried && reflectionSuccess && !exceptionThrown)
        {
            updateNeuState();
            if (exceptionThrown) return;

            Random rand = getWorld().rand;

            if (catchesLeftToMove == 0)
                catchesLeftToMove = rand.nextInt(c.maxCatchesToMove - c.minCatchesToMove + 1) + c.minCatchesToMove;

            switch (state)
            {
                case NOTHING:
                case WAITING:
                    if (!waitForSlugfish || System.currentTimeMillis() - neuLastRodCastMillis > 32 * 1000)
                    {
                        state = FishingState.values()[neuState.ordinal()];

                        if (state == FishingState.HOOKED)
                        {
                            retrieveWaitingTicks = rand.nextInt(c.maxRetrieveDelay - c.minRetrieveDelay + 1) + c.minRetrieveDelay;
                        }
                    }

                    break;
                case HOOKED:
                    state = FishingState.values()[neuState.ordinal()];

                    retrieveWaitingTicks--;

                    if (retrieveWaitingTicks <= 0)
                    {
                        if (getTicks() - lastRetrieveClickTicks > 15)
                        {
                            lastRetrieveClickTicks = getTicks();
                            getMc().rightClickMouse();

                            if (getTicks() - lastCatchTicks >= 20 * 60)
                                catchesLeftToMove = rand.nextInt(c.maxCatchesToMove - c.minCatchesToMove + 1) + c.minCatchesToMove;
                            lastCatchTicks = getTicks();

                            catchesLeftToMove--;
                            if (catchesLeftToMove <= 0)
                            {
                                catchesLeftToMove = rand.nextInt(c.maxCatchesToMove - c.minCatchesToMove + 1) + c.minCatchesToMove;
                                movingTicks = 0;
                                movingTicksTarget = c.headMovingTicks + rand.nextInt(3 + 3 + 1) - 3;
                                movingTargetDeltaYaw = rand.nextFloat() * 2 * c.headMovingYawRange - c.headMovingYawRange;
                                movingTargetDeltaPitch = rand.nextFloat() * 2 * c.headMovingPitchRange - c.headMovingPitchRange;
                            }

                            state = FishingState.WAITING_TO_CAST;
                            pendingRecastTicks = rand.nextInt(c.maxRecastDelay - c.minRecastDelay + 1) + c.minRecastDelay;
                        }
                    }
                    break;
                case WAITING_TO_CAST:
                    pendingRecastTicks--;

                    if (pendingRecastTicks <= 0)
                    {
                        if (getTicks() - lastRecastClickTicks > 20)
                        {
                            lastRecastClickTicks = getTicks();
                            getMc().rightClickMouse();
                        }
                        state = FishingState.NOTHING;
                    }
                    break;
            }

            if (movingTicks < movingTicksTarget)
            {
                movingTicks++;
                float dy = movingTargetDeltaYaw * easingOutExpo((float)movingTicks / movingTicksTarget) -
                        movingTargetDeltaYaw * easingOutExpo((float)(movingTicks-1) / movingTicksTarget);
                float dp = movingTargetDeltaPitch * easingOutExpo((float)movingTicks / movingTicksTarget) -
                        movingTargetDeltaPitch * easingOutExpo((float)(movingTicks-1) / movingTicksTarget);
                getPlayer().rotationYaw += dy;
                getPlayer().rotationPitch += dp;
            }
        }
    }

    public void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event)
    {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        ScaledResolution res = new ScaledResolution(getMc());
        FontRenderer r = getMc().fontRendererObj;
        int width = res.getScaledWidth();
        int height = res.getScaledHeight();

        if (enable && c.enableDebugInfo && reflectionTried && reflectionSuccess)
        {
            int x = width - 30;
            int y = 30;

            String neuState = "neu state: " + neuWarningState.toString();
            String s = "state: " + state.toString();
            String slug = f("slugfish 30s: %s, %.1f", waitForSlugfish, (System.currentTimeMillis() - neuLastRodCastMillis) / 1000.0f);
            String rt = "retrieve waiting ticks: " + retrieveWaitingTicks;
            String rc = "pending recast ticks: " + pendingRecastTicks;
            String k = "catches left to move: " + catchesLeftToMove;

            r.drawString(neuState, x - r.getStringWidth(neuState), y, 0xffffffff); y += r.FONT_HEIGHT;
            r.drawString(s, x - r.getStringWidth(s), y, 0xffffffff); y += r.FONT_HEIGHT;
            r.drawString(slug, x - r.getStringWidth(slug), y, 0xffffffff); y += r.FONT_HEIGHT;
            r.drawString(rt, x - r.getStringWidth(rt), y, 0xffffffff); y += r.FONT_HEIGHT;
            r.drawString(rc, x - r.getStringWidth(rc), y, 0xffffffff); y += r.FONT_HEIGHT;
            r.drawString(k, x - r.getStringWidth(k), y, 0xffffffff); y += r.FONT_HEIGHT;
        }
    }

    public void onChatReceived(ClientChatReceivedEvent event)
    {
        if (enable)
        {
            if (event.message.getUnformattedText().equals("You spot a Golden Fish surface from beneath the lava!"))
            {
                Runnable playSound = () ->
                {
                    EntityPlayerSP p = McUtils.getPlayer();
                    ISound sound = new PositionedSoundRecord(new ResourceLocation("random.levelup"),
                            1.0f, 2.0f, (float)p.posX, (float)p.posY, (float)p.posZ);
                    getMc().getSoundHandler().playSound(sound);
                };

                playSound.run();
                Tweakception.scheduler.addDelayed(playSound, 10)
                        .thenDelayed(playSound, 10)
                        .thenDelayed(playSound, 10)
                        .thenDelayed(playSound, 10)
                        .thenDelayed(playSound, 10);

                if (getPlayer().fishEntity != null)
                    getMc().rightClickMouse();
            }
        }
    }

    private void updateNeuState()
    {
        int neuStateOrdinal = 0;
        try
        {
            neuWarningState = (Enum<?>)neuWarningStateField.get(neuFishingHelperInstance);
            neuStateOrdinal = neuWarningState.ordinal();
            neuLastRodCastMillis = (long)neuLastRodCastMillisField.get(neuFishingHelperInstance);
        }
        catch (Exception e)
        {
            // Should not throw here
            if (!exceptionThrown)
            {
                sendChat("AutoFish: " + e.toString() + ", stopping");
                exceptionThrown = true;
            }
        }
        neuState = NeuState.values()[neuStateOrdinal];
    }

    private void getNeuClass()
    {
        try
        {
            neuFishingHelperClass = Class.forName("io.github.moulberry.notenoughupdates.miscfeatures.FishingHelper");
            Method getInstance = neuFishingHelperClass.getMethod("getInstance");
            neuFishingHelperInstance = getInstance.invoke(null);
            neuWarningStateField = neuFishingHelperClass.getDeclaredField("warningState");
            neuLastRodCastMillisField = neuFishingHelperClass.getDeclaredField("lastCastRodMillis");
            neuLastRodCastMillisField.setAccessible(true);
            neuWarningState = (Enum<?>)neuWarningStateField.get(neuFishingHelperInstance);
            neuLastRodCastMillis = (long)neuLastRodCastMillisField.get(neuFishingHelperInstance);
        }
        catch (Exception e)
        {
            if (!exceptionThrown)
            {
                sendChat("AutoFish: " + e.toString() + ", stopping");
                exceptionThrown = true;
            }
        }
        reflectionSuccess = !exceptionThrown && neuFishingHelperInstance != null;
        reflectionTried = true;
        if (reflectionSuccess)
            updateNeuState();
    }

    // All normalized
    private static float easingOutExpo(float t)
    {
        return t == 1.0f ? 1.0f : 1.0f - (float)Math.pow(2, -10 * t);
    }

    private static float easingOutQuart(float t)
    {
        return 1.0f - --t * t * t * t;
    }

    public void toggleAutoFish()
    {
        enable = !enable;
        sendChat("AutoFish: toggled " + enable);
    }

    public void toggleDebugInfo()
    {
        c.enableDebugInfo = !c.enableDebugInfo;
        sendChat("AutoFish: toggled debug info " + c.enableDebugInfo);
    }

    public void setRetrieveDelay(int min, int max, boolean reset)
    {
        if (!reset)
        {
            c.minRetrieveDelay = Math.max(min, 0);
            c.maxRetrieveDelay = Math.min(max, 20);
        }
        else
        {
            c.minRetrieveDelay = new AutoFishConfig().minRetrieveDelay;
            c.maxRetrieveDelay = new AutoFishConfig().maxRetrieveDelay;
        }
        sendChatf("AutoFish: set retrieve delay to min %d, max %d", c.minRetrieveDelay, c.maxRetrieveDelay);
    }

    public void setRecastDelay(int min, int max, boolean reset)
    {
        if (!reset)
        {
            c.minRecastDelay = Math.max(min, 5);
            c.maxRecastDelay = Math.min(max, 50);
        }
        else
        {
            c.minRecastDelay = new AutoFishConfig().minRecastDelay;
            c.maxRecastDelay = new AutoFishConfig().maxRecastDelay;
        }
        sendChatf("AutoFish: set recast delay to min %d, max %d", c.minRecastDelay, c.maxRecastDelay);
    }

    public void setCatchesToMove(int min, int max, boolean reset)
    {
        if (!reset)
        {
            c.minCatchesToMove = Math.max(min, 0);
            c.maxCatchesToMove = Math.min(max, 30);
        }
        else
        {
            c.minCatchesToMove = new AutoFishConfig().minCatchesToMove;
            c.maxCatchesToMove = new AutoFishConfig().maxCatchesToMove;
        }
        sendChatf("AutoFish: set catches to move to min %d, max %d", c.minCatchesToMove, c.maxCatchesToMove);
    }

    public void setHeadMovingTicks(int ticks)
    {
        if (ticks != 0)
            c.headMovingTicks = Math.max(ticks, 3);
        else
            c.headMovingTicks = new AutoFishConfig().headMovingTicks;
        sendChat("AutoFish: set head moving ticks to " + c.headMovingTicks);
    }

    public void setHeadMovingYawRange(float r)
    {
        if (r != 0.0f)
            c.headMovingYawRange = Math.min(r, 8.0f);
        else
            c.headMovingYawRange = new AutoFishConfig().headMovingYawRange;
        sendChat("AutoFish: set head moving yaw range to +-" + c.headMovingYawRange);
    }

    public void setHeadMovingPitchRange(float r)
    {
        if (r != 0.0f)
            c.headMovingPitchRange = Math.min(r, 8.0f);
        else
            c.headMovingPitchRange = new AutoFishConfig().headMovingPitchRange;
        sendChat("AutoFish: set head moving pitch range to +-" + c.headMovingPitchRange);
    }

    public void toggleSlugfish()
    {
        waitForSlugfish = !waitForSlugfish;
        sendChat("AutoFish: toggled slugfish waiting " + waitForSlugfish);
    }
}
