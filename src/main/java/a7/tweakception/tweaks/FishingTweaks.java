package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.overlay.Anchor;
import a7.tweakception.overlay.TextOverlay;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static a7.tweakception.tweaks.GlobalTracker.getTicks;
import static a7.tweakception.utils.McUtils.*;
import static a7.tweakception.utils.Utils.f;

public class FishingTweaks extends Tweak
{
    public static class AutoFishConfig
    {
        public boolean enableAutoFish = false;
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
        public boolean displayThunderBottleCharge = false;
        public int thunderBottleChargeIncrementResetDuration = 30;
    }
    private final AutoFishConfig c;
    private static boolean exceptionThrown = false;
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
    private Integer thunderBottleCharge = null;
    private final HashMap<String, Integer> thunderBottleChargeMap = new HashMap<>();
    private int thunderBottleChargeIncrement = 0;
    private int thunderBottleChargeLastIncrementTicks = 0;
    
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
    
    public FishingTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.autoFish;
        Tweakception.overlayManager.addOverlay(new AutoFishOverlay());
        Tweakception.overlayManager.addOverlay(new ThunderBottleOverlay());
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;
        
        if (c.displayThunderBottleCharge)
        {
            if (thunderBottleChargeIncrement > 0 &&
                getTicks() - thunderBottleChargeLastIncrementTicks > 20 * c.thunderBottleChargeIncrementResetDuration)
                thunderBottleChargeIncrement = 0;
            
            if (getTicks() % 5 == 3)
            {
                thunderBottleCharge = null;
                for (int i = 0; i < 36; i++)
                {
                    ItemStack stack = getPlayer().inventory.getStackInSlot(i);
                    String id = Utils.getSkyblockItemId(stack);
                    String uuid = Utils.getSkyblockItemUuid(stack);
                    if (id != null && id.equals("THUNDER_IN_A_BOTTLE_EMPTY") && uuid != null)
                    {
                        int charge = Utils.getExtraAttributes(stack).getInteger("thunder_charge");
    
                        // Only display the first bottle fill count
                        if (thunderBottleCharge == null)
                            thunderBottleCharge = charge;
                        
                        // TODO: When to clear the map?
                        if (thunderBottleChargeMap.containsKey(uuid))
                        {
                            int old = thunderBottleChargeMap.get(uuid);
                            if (charge > old)
                            {
                                thunderBottleChargeIncrement += charge - old;
                                thunderBottleChargeLastIncrementTicks = getTicks();
                                thunderBottleChargeMap.put(uuid, charge);
                            }
                        }
                        else
                            thunderBottleChargeMap.put(uuid, charge);
                    }
                }
            }
        }
    }
    
    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;
        
        if (!c.enableAutoFish) return;
        
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
                    if ((!waitForSlugfish || System.currentTimeMillis() - neuLastRodCastMillis > 32 * 1000) &&
                        getTicks() - lastRetrieveClickTicks > 5)
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
                    movingTargetDeltaYaw * easingOutExpo((float)(movingTicks - 1) / movingTicksTarget);
                float dp = movingTargetDeltaPitch * easingOutExpo((float)movingTicks / movingTicksTarget) -
                    movingTargetDeltaPitch * easingOutExpo((float)(movingTicks - 1) / movingTicksTarget);
                getPlayer().rotationYaw += dy;
                getPlayer().rotationPitch += dp;
            }
        }
    }
    
    public void onChatReceived(ClientChatReceivedEvent event)
    {
        if (c.enableAutoFish)
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
                sendChat("FishingTweaks: " + e.toString() + ", stopping");
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
                sendChat("FishingTweaks: " + e.toString() + ", stopping");
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
    
//    public boolean isDisplayThunderBottleChargeOn()
//    {
//        return c.displayThunderBottleCharge;
//    }
    
    private class AutoFishOverlay extends TextOverlay
    {
        public static final String NAME = "AutoFishOverlay";
        
        public AutoFishOverlay()
        {
            super(NAME);
            setAnchor(Anchor.TopRight);
            setOrigin(Anchor.TopRight);
            setX(-20);
            setY(20);
            setTextAlignment(1);
        }
        
        @Override
        public void update()
        {
            super.update();
            List<String> list = new ArrayList<>();
            list.add("AutoFish on");
            if (c.enableDebugInfo)
            {
                if (reflectionTried && reflectionSuccess)
                {
                    String neuState = "neu state: " + neuWarningState.toString();
                    String s = "state: " + state.toString();
                    String slug = f("slugfish 30s: %s, %.1f", waitForSlugfish, (System.currentTimeMillis() - neuLastRodCastMillis) / 1000.0f);
                    String rt = "retrieve waiting ticks: " + retrieveWaitingTicks;
                    String rc = "pending recast ticks: " + pendingRecastTicks;
                    String k = "catches left to move: " + catchesLeftToMove;
                    list.add(neuState);
                    list.add(s);
                    list.add(slug);
                    list.add(rt);
                    list.add(rc);
                    list.add(k);
                }
                else
                {
                    list.add("Reflection failed");
                }
            }
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("autofish");
            list.add("numbers");
            list.add("numbers");
            return list;
        }
    }
    
    private class ThunderBottleOverlay extends TextOverlay
    {
        public static final String NAME = "ThunderBottleOverlay";
        
        public ThunderBottleOverlay()
        {
            super(NAME);
            setAnchor(Anchor.BottomRight);
            setOrigin(Anchor.BottomRight);
            setX(-100);
            setY(-10);
        }
        
        @Override
        public void update()
        {
            super.update();
            List<String> list = new ArrayList<>();
            if (thunderBottleCharge != null)
            {
                String s;
                if (thunderBottleCharge < 0) // Dark red
                    s = "Charge: §4?" + thunderBottleCharge + "?§r/50000";
                else if (thunderBottleCharge < 10000) // Green
                    s = "Charge: §a" + thunderBottleCharge + "§r/50000";
                else if (thunderBottleCharge < 20000) // Dark green
                    s = "Charge: §2" + thunderBottleCharge + "§r/50000";
                else if (thunderBottleCharge < 30000) // Dark aqua
                    s = "Charge: §3" + thunderBottleCharge + "§r/50000";
                else if (thunderBottleCharge < 40000) // Yellow
                    s = "Charge: §e" + thunderBottleCharge + "§r/50000";
                else if (thunderBottleCharge < 45000) // Gold
                    s = "Charge: §6" + thunderBottleCharge + "§r/50000";
                else if (thunderBottleCharge < 50000) // Light purple
                    s = "Charge: §d" + thunderBottleCharge + "§r/50000";
                else // Dark red
                    s = "Charge: §4???" + thunderBottleCharge + "???§r/50000";
                
                if (thunderBottleChargeIncrement != 0)
                    s += " §2+" + thunderBottleChargeIncrement;
                list.add(s);
            }
            else
                list.add("No available bottle!");
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("No available bottle!");
            return list;
        }
    }
    
    public void toggleAutoFish()
    {
        c.enableAutoFish = !c.enableAutoFish;
        Tweakception.overlayManager.setEnable(AutoFishOverlay.NAME, c.enableAutoFish);
        sendChat("FT-AutoFish: toggled " + c.enableAutoFish);
    }
    
    public void toggleDebugInfo()
    {
        c.enableDebugInfo = !c.enableDebugInfo;
        sendChat("FT-AutoFish: toggled debug info " + c.enableDebugInfo);
    }
    
    public void setRetrieveDelay(int min, int max)
    {
        c.minRetrieveDelay = min >= 0 ? min : new AutoFishConfig().minRetrieveDelay;
        c.maxRetrieveDelay = max >= 0 ? Math.min(max, 20) : new AutoFishConfig().maxRetrieveDelay;
        sendChatf("FT-AutoFish: set retrieve delay to min %d, max %d", c.minRetrieveDelay, c.maxRetrieveDelay);
    }
    
    public void setRecastDelay(int min, int max)
    {
        c.minRecastDelay = min >= 0 ? Math.max(min, 5) : new AutoFishConfig().minRecastDelay;
        c.maxRecastDelay = max >= 0 ? Math.min(max, 50) : new AutoFishConfig().maxRecastDelay;
        sendChatf("FT-AutoFish: set recast delay to min %d, max %d", c.minRecastDelay, c.maxRecastDelay);
    }
    
    public void setCatchesToMove(int min, int max)
    {
        c.minCatchesToMove = min >= 0 ? min : new AutoFishConfig().minCatchesToMove;
        c.maxCatchesToMove = max >= 0 ? Math.min(max, 30) : new AutoFishConfig().maxCatchesToMove;
        sendChatf("FT-AutoFish: set catches to move to min %d, max %d", c.minCatchesToMove, c.maxCatchesToMove);
    }
    
    public void setHeadMovingTicks(int ticks)
    {
        if (ticks > 0)
            c.headMovingTicks = Math.max(ticks, 3);
        else
            c.headMovingTicks = new AutoFishConfig().headMovingTicks;
        sendChat("FT-AutoFish: set head moving ticks to " + c.headMovingTicks);
    }
    
    public void setHeadMovingYawRange(float r)
    {
        if (r > 0.0f)
            c.headMovingYawRange = Math.min(r, 8.0f);
        else
            c.headMovingYawRange = new AutoFishConfig().headMovingYawRange;
        sendChat("FT-AutoFish: set head moving yaw range to +-" + c.headMovingYawRange);
    }
    
    public void setHeadMovingPitchRange(float r)
    {
        if (r > 0.0f)
            c.headMovingPitchRange = Math.min(r, 8.0f);
        else
            c.headMovingPitchRange = new AutoFishConfig().headMovingPitchRange;
        sendChat("FT-AutoFish: set head moving pitch range to +-" + c.headMovingPitchRange);
    }
    
    public void toggleSlugfish()
    {
        waitForSlugfish = !waitForSlugfish;
        sendChat("FT-AutoFish: toggled slugfish waiting " + waitForSlugfish);
    }
    
    public void toggleThunderBottleOverlay()
    {
        c.displayThunderBottleCharge = !c.displayThunderBottleCharge;
        Tweakception.overlayManager.setEnable(ThunderBottleOverlay.NAME, c.displayThunderBottleCharge);
        if (!c.displayThunderBottleCharge)
        {
            thunderBottleChargeMap.clear();
            thunderBottleChargeIncrement = 0;
        }
        sendChat("FT-ThunderBottleOverlay: toggled " + c.displayThunderBottleCharge);
    }
    
    public void setThunderBottleChargeIncrementResetDuration(int d)
    {
        c.thunderBottleChargeIncrementResetDuration =
            d > 0 ? d : new AutoFishConfig().thunderBottleChargeIncrementResetDuration;
        sendChat("FT-ThunderBottleOverlay: set increment reset time to " + c.thunderBottleChargeIncrementResetDuration);
    }
}
