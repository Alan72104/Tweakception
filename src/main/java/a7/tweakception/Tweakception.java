package a7.tweakception;

import a7.tweakception.commands.TweakceptionCommand;
import a7.tweakception.config.Configuration;
import a7.tweakception.overlay.OverlayManager;
import a7.tweakception.tweaks.*;
import a7.tweakception.utils.DiscordGuildBridge;
import net.minecraft.block.Block;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod(modid = Tweakception.MOD_ID, version = Tweakception.MOD_VERSION, name = Tweakception.MOD_NAME,
    acceptedMinecraftVersions = "1.8.9", clientSideOnly = true)
public class Tweakception
{
    public static final String MOD_ID = "tweakception";
    public static final String MOD_NAME = "Tweakception";
    public static final String MOD_VERSION = "1.0";
    @Mod.Instance(MOD_ID)
    public static Tweakception instance;
    
    public static final KeyBinding keybindToggleSnapYaw =
        new KeyBinding("Toggle snap yaw", Keyboard.KEY_SUBTRACT, "Tweakception");
    public static final KeyBinding keybindToggleSnapPitch =
        new KeyBinding("Toggle snap pitch", Keyboard.KEY_ADD, "Tweakception");
    public static final KeyBinding keybindFastCommand =
        new KeyBinding("Fast command", Keyboard.KEY_BACKSLASH, "Tweakception");
    
    public static Logger logger;
    public static Configuration configuration;
    public static ExecutorService threadPool;
    public static Scheduler scheduler;
    public static InGameEventDispatcher inGameEventDispatcher;
    public static APIManager apiManager;
    public static GlobalTweaks globalTweaks;
    public static OverlayManager overlayManager;
    public static TuningTweaks tuningTweaks;
    public static FairyTracker fairyTracker;
    public static DungeonTweaks dungeonTweaks;
    public static MiningTweaks miningTweaks;
    public static ForagingTweaks foragingTweaks;
    public static CrimsonTweaks crimsonTweaks;
    public static SlayerTweaks slayerTweaks;
    public static FishingTweaks fishingTweaks;
    public static EnchantingTweaks enchantingTweaks;
    public static BazaarTweaks bazaarTweaks;
    public static AutoRunes autoRunes;
    public static GardenTweaks gardenTweaks;
    public static GiftTweaks giftTweaks;
    public static DiscordGuildBridge guildBridge;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event) throws Exception
    {
        instance = this;
        logger = event.getModLog();
        configuration = new Configuration(event.getModConfigurationDirectory().getAbsolutePath() + "/" + MOD_ID + "/");
        configuration.initConfig();
        
        // Load class for static field accessors
        Class.forName("net.minecraft.client.gui.GuiPlayerTabOverlay");

        inGameEventDispatcher = new InGameEventDispatcher();
        threadPool = Executors.newFixedThreadPool(3);
        scheduler = new Scheduler();
        apiManager = new APIManager(configuration);
        overlayManager = new OverlayManager(configuration);
        globalTweaks = new GlobalTweaks(configuration);
        tuningTweaks = new TuningTweaks(configuration);
        fairyTracker = new FairyTracker(configuration);
        dungeonTweaks = new DungeonTweaks(configuration);
        miningTweaks = new MiningTweaks(configuration);
        foragingTweaks = new ForagingTweaks(configuration);
        crimsonTweaks = new CrimsonTweaks(configuration);
        slayerTweaks = new SlayerTweaks(configuration);
        fishingTweaks = new FishingTweaks(configuration);
        enchantingTweaks = new EnchantingTweaks(configuration);
        bazaarTweaks = new BazaarTweaks(configuration);
        autoRunes = new AutoRunes(configuration);
        gardenTweaks = new GardenTweaks(configuration);
        giftTweaks = new GiftTweaks(configuration);
        guildBridge = new DiscordGuildBridge();
        
        ClientCommandHandler.instance.registerCommand(new TweakceptionCommand());
        MinecraftForge.EVENT_BUS.register(inGameEventDispatcher);
        MinecraftForge.EVENT_BUS.register(scheduler);
        
        ClientRegistry.registerKeyBinding(keybindToggleSnapYaw);
        ClientRegistry.registerKeyBinding(keybindToggleSnapPitch);
        ClientRegistry.registerKeyBinding(keybindFastCommand);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
        {
            try
            {
                configuration.writeConfig();
                threadPool.shutdownNow();
                LagSpikeWatcher.stopWatcher();
                globalTweaks.getPacketLogger().stop();
                guildBridge.disconnectNow();
            }
            catch (Exception ignored)
            {
            }
        }));
    }
    
    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
    }
    
    // Not mutable
    public static final class BlockSearchTask implements Runnable
    {
        private final int posX;
        private final int posY;
        private final int posZ;
        private final int toPosX;
        private final int toPosY;
        private final int toPosZ;
        private final World world;
        private final Block targetBlock;
        private final Collection<BlockPos> targetCollection;
        public boolean done = false;
        public boolean cancel = false;
        public long executeNanoTime;
        
        
        public BlockSearchTask(int posX, int posY, int posZ, int range, int rangeY, World world, Block targetBlock, Collection<BlockPos> targetCollection)
        {
            this.posX = posX - range;
            this.posY = posY - rangeY;
            this.posZ = posZ - range;
            this.toPosX = posX + range;
            this.toPosY = posY + rangeY;
            this.toPosZ = posZ + range;
            this.world = world;
            this.targetBlock = targetBlock;
            this.targetCollection = targetCollection;
        }
        
        public BlockSearchTask(int posX, int posY, int posZ, int toPosX, int toPosY, int toPosZ, World world, Block targetBlock, Collection<BlockPos> targetCollection)
        {
            this.posX = Math.min(posX, toPosX);
            this.posY = Math.min(posY, toPosY);
            this.posZ = Math.min(posZ, toPosZ);
            this.toPosX = Math.max(posX, toPosX);
            this.toPosY = Math.max(posY, toPosY);
            this.toPosZ = Math.max(posZ, toPosZ);
            this.world = world;
            this.targetBlock = targetBlock;
            this.targetCollection = targetCollection;
        }
        
        public void run()
        {
//            long startNano = System.nanoTime();
            // Wtf isn't normal loop working here?????
//            for (int x = posX; x < toPosX; x++)
//                for (int y = posY; y < toPosY; y++)
//                    for (int z = posZ; z < toPosZ; z++)
            for (BlockPos pos : BlockPos.getAllInBox(new BlockPos(posX, posY, posZ), new BlockPos(toPosX, toPosY, toPosZ)))
            {
                if (cancel)
                {
                    done = true;
                    return;
                }

//                        BlockPos pos = new BlockPos(posX, posY, posZ);
                if (world.getBlockState(pos).getBlock() == targetBlock)
                    targetCollection.add(pos);
            }
//            executeNanoTime = System.nanoTime() - startNano;
            done = true;
        }
    }
}
