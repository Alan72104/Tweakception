package a7.tweakception;

import a7.tweakception.properties.ProjectInfo;
import a7.tweakception.proxies.IProxy;
import a7.tweakception.tweaks.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = ProjectInfo.MOD_ID, version = ProjectInfo.MOD_VERSION, name = ProjectInfo.MOD_NAME,
    acceptedMinecraftVersions = "1.8.9")
public class Tweakception
{
    @Mod.Instance(ProjectInfo.MOD_ID)
    public static Tweakception instance;
    @SidedProxy(clientSide = "a7.tweakception.proxies.ClientProxy", serverSide = "a7.tweakception.proxies.ServerProxy")
    public static IProxy proxy;
    public static Logger logger;
    public static String configDirPath;
    public static InGameEventDispatcher inGameEventDispatcher;
    public static GlobalTracker globalTracker;
    public static FairyTracker fairyTracker;
    public static DungeonTweaks dungeonTweaks;
    public static CrimsonTweaks crimsonTweaks;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        instance = this;
        logger = event.getModLog();
        configDirPath = event.getModConfigurationDirectory().getAbsolutePath().concat("/" + ProjectInfo.MOD_ID);
        inGameEventDispatcher = new InGameEventDispatcher();
        globalTracker = new GlobalTracker();
        fairyTracker = new FairyTracker();
        dungeonTweaks = new DungeonTweaks();
        crimsonTweaks = new CrimsonTweaks();
        MinecraftForge.EVENT_BUS.register(inGameEventDispatcher);
        proxy.registerClientCommands();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
    }
}
