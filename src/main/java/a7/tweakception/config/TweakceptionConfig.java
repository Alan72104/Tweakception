package a7.tweakception.config;

import a7.tweakception.overlay.OverlayManager;
import a7.tweakception.tweaks.*;

public class TweakceptionConfig
{
    public APIManager.APIManagerConfig apiManager = new APIManager.APIManagerConfig();
    public GlobalTracker.GlobalTrackerConfig globalTracker = new GlobalTracker.GlobalTrackerConfig();
    public OverlayManager.OverlayManagerConfig overlayManager = new OverlayManager.OverlayManagerConfig();
    public TuningTweaks.TuningTweaksConfig tuningTweaks = new TuningTweaks.TuningTweaksConfig();
    public FairyTracker.FairyTrackerConfig fairyTracker = new FairyTracker.FairyTrackerConfig();
    public DungeonTweaks.DungeonTweaksConfig dungeonTweaks = new DungeonTweaks.DungeonTweaksConfig();
    public MiningTweaks.MiningTweaksConfig miningTweaks = new MiningTweaks.MiningTweaksConfig();
    public ForagingTweaks.ForagingTweaksConfig foragingTweaks = new ForagingTweaks.ForagingTweaksConfig();
    public CrimsonTweaks.CrimsonTweaksConfig crimsonTweaks = new CrimsonTweaks.CrimsonTweaksConfig();
    public SlayerTweaks.SlayerTweaksConfig slayerTweaks = new SlayerTweaks.SlayerTweaksConfig();
    public FishingTweaks.AutoFishConfig autoFish = new FishingTweaks.AutoFishConfig();
}
