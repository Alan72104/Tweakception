package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.utils.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static a7.tweakception.tweaks.GlobalTweaks.getCurrentIsland;
import static a7.tweakception.tweaks.GlobalTweaks.getTicks;
import static a7.tweakception.utils.McUtils.*;

public class MiningTweaks extends Tweak
{
    public static class MiningTweaksConfig
    {
        public boolean highlightChests = false;
        public boolean simulateBlockHardness = false;
        public int simulateBlockHardnessExtraTicks = 0;
        public int simulateBlockHardnessExtraTicksOnBoost = 0;
        public int miningSpeedBoostLevel = 2;
        public boolean treasureChestHelper = false;
        public int toolMiningSpeedOverride = 0;
        public float baseMiningSpeed = 0.0f;
    }
    
    private static final Color CHEST_COLOR_OPENED = new Color(0, 255, 0, 48);
    private static final Color CHEST_COLOR_CLOSED = new Color(255, 0, 0, 48);
    private static final Color CHEST_COLOR_WARNING = new Color(255, 255, 0, 48);
    private static final SpecialBlock[] SPECIAL_BLOCKS;
    private final MiningTweaksConfig c;
    private final Set<TreasureChest> treasureChests = new ConcurrentSkipListSet<>(Comparator.comparing(a -> a.pos));
    private final Matcher miningStatMatcher = Pattern.compile(
        "^ §6⸕ Mining Speed §f(\\d+(?:,\\d+)*)").matcher("");
    private final Matcher miningItemStatMatcher = Pattern.compile(
        "^§7Mining Speed: §a\\+(\\d+(?:,\\d+)*)").matcher("");
    private final StopwatchTimer miningSpeedBoostTimer;
    private float lastBaseMiningSpeed;
    private BlockPos targetTreasureChest = null;
    private Vec3 targetParticlePos = null;
    private Vec3 targetRandomPoint = null;
    
    public static class SpecialBlock
    {
        public final Block block;
        /**
         * Block meta, -1 for all
         */
        public final int meta;
        public final int hardness;
        public final int breakingPower;
        /**
         * Effective island, null for all
         */
        public final SkyblockIsland island;
        public SpecialBlock(Block b, int m, int h, int bp)
            { block = b; meta = m; hardness = h; breakingPower = bp; island = null; }
        public SpecialBlock(Block b, int m, int h, int bp, SkyblockIsland i)
            { block = b; meta = m; hardness = h; breakingPower = bp; island = i; }
        public SpecialBlock(Block b, EnumDyeColor m, int h, int bp, SkyblockIsland i)
            { block = b; meta = m.getMetadata(); hardness = h; breakingPower = bp; island = i; }
    }
    
    static
    {
        SkyblockIsland dm = SkyblockIsland.DWARVEN_MINES;
        SkyblockIsland ch = SkyblockIsland.CRYSTAL_HOLLOWS;
        SPECIAL_BLOCKS = new SpecialBlock[]
        {
            new SpecialBlock(Blocks.stained_hardened_clay, 9, 500, 4, dm), // Gray Mithril, cyan hardened clay
            new SpecialBlock(Blocks.wool, 7, 500, 4),                      // Gray Mithril, gray wool
            new SpecialBlock(Blocks.wool, 3, 1500, 4),                     // Blue Mithril, light blue wool
            new SpecialBlock(Blocks.prismarine, -1, 800, 4),               // Green Mithril, all prismarine bricks
            new SpecialBlock(Blocks.stone, 4, 2000, 5, dm),                // Titanium, smooth diorite
            new SpecialBlock(Blocks.gold_block, -1, 600, 3),               // Dwarven Gold, gold block
            // Doesn't work well with insta breaking
            // new SpecialBlock(Blocks.stone, -1, 50, 4, ch),                 // Hard Stone, all stone
            // new SpecialBlock(Blocks.stained_hardened_clay, -1, 50, 4, ch), // Hard Stone, all hardened clay
            new SpecialBlock(Blocks.stained_glass, EnumDyeColor.RED,             2500, 6, ch),
            new SpecialBlock(Blocks.stained_glass_pane, EnumDyeColor.RED,        2300, 6, ch),
            new SpecialBlock(Blocks.stained_glass, EnumDyeColor.ORANGE,          3200, 7, ch),
            new SpecialBlock(Blocks.stained_glass_pane, EnumDyeColor.ORANGE,     3000, 7, ch),
            new SpecialBlock(Blocks.stained_glass, EnumDyeColor.PURPLE,          3200, 7, ch),
            new SpecialBlock(Blocks.stained_glass_pane, EnumDyeColor.PURPLE,     3000, 7, ch),
            new SpecialBlock(Blocks.stained_glass, EnumDyeColor.LIME,            3200, 7, ch),
            new SpecialBlock(Blocks.stained_glass_pane, EnumDyeColor.LIME,       3000, 7, ch),
            new SpecialBlock(Blocks.stained_glass, EnumDyeColor.LIGHT_BLUE,      3200, 7, ch),
            new SpecialBlock(Blocks.stained_glass_pane, EnumDyeColor.LIGHT_BLUE, 3000, 7, ch),
            new SpecialBlock(Blocks.stained_glass, EnumDyeColor.YELLOW,          4000, 8, ch),
            new SpecialBlock(Blocks.stained_glass_pane, EnumDyeColor.YELLOW,     3800, 8, ch),
            new SpecialBlock(Blocks.stained_glass, EnumDyeColor.MAGENTA,         5000, 9, ch),
            new SpecialBlock(Blocks.stained_glass_pane, EnumDyeColor.MAGENTA,    4800, 9, ch),
        };
    }
    
    public MiningTweaks(Configuration configuration)
    {
        super(configuration, "MT");
        c = configuration.config.miningTweaks;
        miningSpeedBoostTimer = new StopwatchTimer(getMiningSpeedBoostDuration() * 1000);
        lastBaseMiningSpeed = c.baseMiningSpeed;
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END) return;
        
        if (getCurrentIsland() == SkyblockIsland.CRYSTAL_HOLLOWS)
        {
            if (!treasureChests.isEmpty())
            {
                Iterator<TreasureChest> it = treasureChests.iterator();
                while (it.hasNext())
                {
                    TreasureChest chest = it.next();
                    BlockPos bp = chest.pos;
                    TileEntity te = getWorld().getTileEntity(bp);
                    if (te instanceof TileEntityChest)
                    {
                        TileEntityChest teChest = (TileEntityChest) te;
                        if (!chest.opened && teChest.numPlayersUsing > 0)
                            chest.opened = true;
                    }
                    else
                        it.remove();
                }
            }
        }
        
        if (McUtils.getOpenedChest() != null)
        {
            IInventory inv = McUtils.getOpenedChest();
            if (inv.getSizeInventory() == 54 &&
                inv.getName().equals("SkyBlock Menu"))
            {
                ItemStack statsBtn = inv.getStackInSlot(9 + 4);
                if (statsBtn != null)
                {
                    String[] lore = McUtils.getDisplayLore(statsBtn);
                    for (String s : lore)
                    {
                        if (miningStatMatcher.reset(s).matches())
                        {
                            c.baseMiningSpeed = Utils.parseFloat(miningStatMatcher.group(1));
                            c.baseMiningSpeed /= getMiningSpeedMultiplier();
                            float toolSpeed = getHeldToolMiningSpeedWithoutOverride();
                            c.baseMiningSpeed -= toolSpeed;
                            if (lastBaseMiningSpeed != c.baseMiningSpeed)
                            {
                                sendChat("§8Cached base mining speed of " + c.baseMiningSpeed);
                                lastBaseMiningSpeed = c.baseMiningSpeed;
                                Tweakception.threadPool.submit(this::saveConfig);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
    
    public void onRenderLast(RenderWorldLastEvent event)
    {
        if (getCurrentIsland() == SkyblockIsland.CRYSTAL_HOLLOWS)
        {
            if (!treasureChests.isEmpty())
            {
                RenderUtils.Vector3d pos = RenderUtils.getInterpolatedViewingPos(event.partialTicks);
                
                for (TreasureChest chest : treasureChests)
                {
                    BlockPos bp = chest.pos;
                    Color color;
                    if (chest.opened)
                        color = CHEST_COLOR_OPENED;
                    else
                    {
                        int elapsed = getTicks() - chest.spawnTicks;
                        if (elapsed >= 20 * 50)
                        {
                            if (elapsed % 10 < 5)
                                color = CHEST_COLOR_WARNING;
                            else
                                continue;
                        }
                        else if (elapsed >= 20 * 30)
                            color = CHEST_COLOR_WARNING;
                        else
                            color = CHEST_COLOR_CLOSED;
                    }
                    RenderUtils.drawFilledBoundingBoxChestSize(bp.getX() - pos.x, bp.getY() - pos.y, bp.getZ() - pos.z, color);
                }
            }
        }
    }
    
    public void onPacketMultiBlockChanged(S22PacketMultiBlockChange packet)
    {
        if (getCurrentIsland() != SkyblockIsland.CRYSTAL_HOLLOWS) return;
        
        for (S22PacketMultiBlockChange.BlockUpdateData change : packet.getChangedBlocks())
        {
            Block block = change.getBlockState().getBlock();
            BlockPos pos = change.getPos();
            
            if (block == Blocks.chest)
                treasureChests.add(new TreasureChest(pos, getTicks()));
            else if (block == Blocks.air)
                treasureChests.remove(new TreasureChest(pos));
        }
    }
    
    public void onPacketBlockChanged(S23PacketBlockChange packet)
    {
        if (getCurrentIsland() != SkyblockIsland.CRYSTAL_HOLLOWS) return;
        
        Block block = packet.getBlockState().getBlock();
        BlockPos pos = packet.getBlockPosition();
        
        if (block == Blocks.chest)
            treasureChests.add(new TreasureChest(pos, getTicks()));
        else if (block == Blocks.air)
            treasureChests.remove(new TreasureChest(pos));
    }
    
    public void onPacketParticles(S2APacketParticles packet)
    {
        if (getCurrentIsland() != SkyblockIsland.CRYSTAL_HOLLOWS) return;
        if (!c.treasureChestHelper) return;
        if (targetTreasureChest == null) return;
        if (packet.getParticleType() != EnumParticleTypes.CRIT) return;
        if (packet.getParticleCount() != 1) return;
        if (packet.getParticleSpeed() != 0.0f) return;
        if (packet.getXOffset() != 0.0f) return;
        if (packet.getYOffset() != 0.0f) return;
        if (packet.getZOffset() != 0.0f) return;
        if (!packet.isLongDistance()) return;
        
        int facing = 0;
        double x = packet.getXCoordinate(), xFrat = x - (int) x;
        double z = packet.getZCoordinate(), zFrat = z - (int) z;
        if (xFrat == 0.1)
            facing += 5; // E
        else if (xFrat == 0.9)
            facing += 4; // W
        if (zFrat == 0.1)
            facing += 3; // S
        else if (zFrat == 0.9)
            facing += 2; // N
        
        if (facing == 0 || facing > 5) // Returns if both are valid, but rarely happens
            return;
        
        BlockPos chestPos = new BlockPos(
            x + (facing == 5 ? -1 : facing == 4 ? 1 : 0),
            packet.getYCoordinate(),
            z + (facing == 3 ? -1 : facing == 2 ? 1 : 0));
        if (!chestPos.equals(targetTreasureChest))
            return;
        IBlockState state = getWorld().getBlockState(chestPos);
        if (state.getValue(BlockChest.FACING).getIndex() != facing)
            return;
        
        Vec3 pos = new Vec3(
            packet.getXCoordinate(),
            packet.getYCoordinate(),
            packet.getZCoordinate());
        if (targetParticlePos.equals(pos))
            return;
        targetParticlePos = pos;
        targetRandomPoint = new Vec3(
            packet.getXCoordinate() + getWorld().rand.nextGaussian() * 0.3 - 0.15,
            packet.getYCoordinate() + getWorld().rand.nextGaussian() * 0.3 - 0.15,
            packet.getZCoordinate() + getWorld().rand.nextGaussian() * 0.3 - 0.15);
    }
    
    public void onPacketBlockBreakAnim(S25PacketBlockBreakAnim packet, CallbackInfo ci)
    {
        if (isSimulateBlockHardnessOn() &&
            (getCurrentIsland() == SkyblockIsland.DWARVEN_MINES ||
            getCurrentIsland() == SkyblockIsland.CRYSTAL_HOLLOWS) &&
            packet.getBreakerId() == 0 &&
            getSpecialBlock(getWorld(), packet.getPosition()) != null)
        {
            ci.cancel();
        }
    }
    
    public void onChatReceived(ClientChatReceivedEvent event)
    {
        String msg = event.message.getUnformattedText();
        if (event.type == 0 || event.type == 1)
        {
            if (msg.equals("You used your Mining Speed Boost Pickaxe Ability!"))
            {
                miningSpeedBoostTimer.start();
            }
            else if (msg.equals("Your Mining Speed Boost has expired!"))
            {
                miningSpeedBoostTimer.stop();
            }
        }
    }
    
    public void onWorldUnload(WorldEvent.Unload event)
    {
        treasureChests.clear();
    }
    
    private static class TreasureChest
    {
        public BlockPos pos;
        public int spawnTicks = 0;
        public boolean opened = false;
        
        public TreasureChest(BlockPos pos)
        {
            this.pos = pos;
        }
        
        public TreasureChest(BlockPos pos, int spawnTicks)
        {
            this.pos = pos;
            this.spawnTicks = spawnTicks;
        }
    }
    
    public boolean isSimulateBlockHardnessOn()
    {
        return c.simulateBlockHardness;
    }
    
    public float getBaseMiningSpeed()
    {
        return c.baseMiningSpeed;
    }
    
    public float getHeldToolMiningSpeed()
    {
        float speed = getHeldToolMiningSpeedWithoutOverride();
        if (speed > 0 && c.toolMiningSpeedOverride > 0)
            return c.toolMiningSpeedOverride;
        return speed;
    }
    
    public float getHeldToolMiningSpeedWithoutOverride()
    {
        ItemStack stack = getPlayer().inventory.getCurrentItem();
        if (stack == null)
            return 0.0f;
        String[] lore = McUtils.getDisplayLore(stack);
        if (lore == null)
            return 0.0f;
        for (String s : lore)
            if (miningItemStatMatcher.reset(s).find())
                return Utils.parseFloat(miningItemStatMatcher.group(1));
        return 0.0f;
    }
    
    public int getMiningSpeedBoostDuration()
    {
        return c.miningSpeedBoostLevel == 0 ? 0 : 15 + (c.miningSpeedBoostLevel - 1) * 5;
    }
    
    public float getMiningSpeedMultiplier()
    {
        return isMiningSpeedBoostActivated()
            ? 1.0f + getMiningSpeedBoost()
            : 1.0f;
    }
    
    public int getMiningSpeedBoost()
    {
        return c.miningSpeedBoostLevel > 0 ? 1 + c.miningSpeedBoostLevel : 0;
    }
    
    public boolean isMiningSpeedBoostActivated()
    {
        return miningSpeedBoostTimer.isRunning() && c.miningSpeedBoostLevel > 0;
    }
    
    public int getSimulateBlockHardnessExtraTicks()
    {
        return c.simulateBlockHardnessExtraTicks;
    }
    
    public int getSimulateBlockHardnessExtraTicksOnBoost()
    {
        return c.simulateBlockHardnessExtraTicksOnBoost;
    }
    
    public SpecialBlock getSpecialBlock(World world, BlockPos pos)
    {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        int meta = block.getMetaFromState(block.getActualState(state, world, pos));
        SkyblockIsland island = getCurrentIsland();
        for (SpecialBlock special : SPECIAL_BLOCKS)
        {
            if (special.block == block &&
                (special.meta == meta || special.meta == -1) &&
                (special.island == island || special.island == null))
            {
                return special;
            }
        }
        return null;
    }
    
    public void toggleHighlightChests()
    {
        c.highlightChests = !c.highlightChests;
        sendChat("HighlightChests: Toggled " + c.highlightChests);
    }
    
    public void toggleSimulateBlockHardness()
    {
        c.simulateBlockHardness = !c.simulateBlockHardness;
        sendChat("SimulateBlockHardness: Toggled " + c.simulateBlockHardness);
    }
    
    public void setMiningSpeedBoostLevel(int v)
    {
        v = Utils.clamp(v, 0, 3);
        c.miningSpeedBoostLevel = v;
        miningSpeedBoostTimer.period = getMiningSpeedBoostDuration() * 1000;
        sendChat("Set mining speed boost level to " + c.miningSpeedBoostLevel);
    }
    
    public void printMiningSpeedCache()
    {
        boolean inBoost = isMiningSpeedBoostActivated();
        int extra = getSimulateBlockHardnessExtraTicks();
        int extraOnBoost = getSimulateBlockHardnessExtraTicksOnBoost();
        sendChat("Current tool mining speed is " + getHeldToolMiningSpeed() + (c.toolMiningSpeedOverride > 0 ? " (overridden)" : ""));
        sendChat("Base mining speed is " + getBaseMiningSpeed());
        sendChat("Multiplier is " + getMiningSpeedMultiplier() + ", boost level is " + c.miningSpeedBoostLevel + ", on boost: " + inBoost);
        sendChat("Total mining speed is " +
            ((getHeldToolMiningSpeed() + getBaseMiningSpeed()) * getMiningSpeedMultiplier()));
        sendChatf("Extra ticks is %d, on boost: %d + %d = %d", extra, extra, extraOnBoost, extra + extraOnBoost);
    }
    
    public void setToolMiningSpeedOverride(int speed)
    {
        c.toolMiningSpeedOverride = Math.max(speed, 0);
        sendChat("Set tool mining speed override to " + c.toolMiningSpeedOverride);
    }
    
    public void setSimulateBlockHardnessExtraTicks(int ticks)
    {
        c.simulateBlockHardnessExtraTicks = Math.max(ticks, 0);
        sendChat("SimulateBlockHardness: Set extra ticks to " + c.simulateBlockHardnessExtraTicks);
    }
    
    public void setSimulateBlockHardnessExtraTicksOnBoost(int ticks)
    {
        c.simulateBlockHardnessExtraTicksOnBoost = Math.max(ticks, 0);
        sendChat("SimulateBlockHardness: Set extra ticks on boost to " + c.simulateBlockHardnessExtraTicksOnBoost +
            " (added on top of extra ticks when on boost)");
    }
}
