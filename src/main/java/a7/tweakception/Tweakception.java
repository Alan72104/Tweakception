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

import java.io.IOException;

@Mod(modid = ProjectInfo.MOD_ID, version = ProjectInfo.MOD_VERSION, name = ProjectInfo.MOD_NAME,
    acceptedMinecraftVersions = "1.8.9")
public class Tweakception
{
    @Mod.Instance(ProjectInfo.MOD_ID)
    public static Tweakception instance;
    @SidedProxy(clientSide = "a7.tweakception.proxies.ClientProxy", serverSide = "a7.tweakception.proxies.ServerProxy")
    public static IProxy proxy;
    public static Logger logger;
    public static Configuration config;
    public static InGameEventDispatcher inGameEventDispatcher;
    public static GlobalTracker globalTracker;
    public static FairyTracker fairyTracker;
    public static DungeonTweaks dungeonTweaks;
    public static CrimsonTweaks crimsonTweaks;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) throws IOException
    {
        instance = this;
        logger = event.getModLog();
        config = new Configuration(event.getModConfigurationDirectory().getAbsolutePath() + "/" + ProjectInfo.MOD_ID + "/");

        inGameEventDispatcher = new InGameEventDispatcher();
        globalTracker = new GlobalTracker();
        fairyTracker = new FairyTracker();
        dungeonTweaks = new DungeonTweaks();
        crimsonTweaks = new CrimsonTweaks();

        proxy.registerClientCommands();
        proxy.registerClientEventHandlers();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
    }
}
