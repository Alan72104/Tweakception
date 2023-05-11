package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.RenderUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

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
        public int miningSpeedBoostValue = 3;
        public boolean treasureChestHelper = false;
    }
    
    private static final Color CHEST_COLOR_OPENED = new Color(0, 255, 0, 48);
    private static final Color CHEST_COLOR_CLOSED = new Color(255, 0, 0, 48);
    private static final Color CHEST_COLOR_WARNING = new Color(255, 255, 0, 48);
    private final MiningTweaksConfig c;
    private final Set<TreasureChest> treasureChests = new ConcurrentSkipListSet<>(Comparator.comparing(a -> a.pos));
    private float miningSpeedCache = 0.0f;
    private final Matcher miningStatMatcher = Pattern.compile(
        "^ §6⸕ Mining Speed §f(\\d+(?:,\\d+)*)").matcher("");
    private final Matcher miningItemStatMatcher = Pattern.compile(
        "^§7Mining Speed: §a\\+(\\d+(?:,\\d+)*)").matcher("");
    private int miningSpeedBoostStartTicks = 0;
    private BlockPos targetTreasureChest = null;
    private Vec3 targetParticlePos = null;
    private Vec3 targetRandomPoint = null;
    
    
    public MiningTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.miningTweaks;
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
                            miningSpeedCache = Utils.parseFloat(miningStatMatcher.group(1));
                            
                            if (getTicks() - miningSpeedBoostStartTicks <= 20 * 20)
                                miningSpeedCache /= c.miningSpeedBoostValue + 1;
                            
                            float toolSpeed = getHeldToolMiningSpeed();
                            if (toolSpeed != 0.0f)
                                miningSpeedCache -= toolSpeed;
                            
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
    
    public void onPacketMultiBlockChange(S22PacketMultiBlockChange packet)
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
    
    public void onPacketBlockChange(S23PacketBlockChange packet)
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
    
    public void onChatReceived(ClientChatReceivedEvent event)
    {
        String msg = event.message.getUnformattedText();
        if (event.type == 0 || event.type == 1)
        {
            if (msg.equals("You used your Mining Speed Boost Pickaxe Ability!"))
            {
                miningSpeedBoostStartTicks = getTicks();
            }
            else if (msg.equals("Your Mining Speed Boost has expired!"))
            {
                miningSpeedBoostStartTicks = 0;
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
    
    public float getCachedMiningSpeed()
    {
        return miningSpeedCache;
    }
    
    public float getHeldToolMiningSpeed()
    {
        ItemStack stack = getPlayer().inventory.getCurrentItem();
        if (stack == null)
            return 0.0f;
        String[] lore = McUtils.getDisplayLore(stack);
        if (lore == null)
            return 0.0f;
        for (String s : lore)
        {
            if (miningItemStatMatcher.reset(s).find())
            {
                return Utils.parseFloat(miningItemStatMatcher.group(1));
            }
        }
        return 0.0f;
    }
    
    public float getMiningSpeedBoostScale()
    {
        return getTicks() - miningSpeedBoostStartTicks <= 20 * 20 ? 1.0f + c.miningSpeedBoostValue : 1.0f;
    }
    
    public float getSpecialBlockHardness(World world, BlockPos pos)
    {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block == Blocks.stained_hardened_clay)
        {
            int meta = block.getMetaFromState(block.getActualState(state, world, pos));
            if (meta == 9) // Gray Mithril, cyan hardened clay
            {
                return 500;
            }
        }
        else if (block == Blocks.wool)
        {
            int meta = block.getMetaFromState(block.getActualState(state, world, pos));
            if (meta == 7) // Gray Mithril, gray wool
            {
                return 500;
            }
            else if (meta == 3) // Blue Mithril, light blue wool
            {
                return 1500;
            }
        }
        else if (block == Blocks.prismarine)
        {
            // Green Mithril, all prismarine bricks
            return 800;
        }
        else if (block == Blocks.stone)
        {
            int meta = block.getMetaFromState(block.getActualState(state, world, pos));
            if (meta == 4) // Titanium, smooth diorite
            {
                return 2000;
            }
        }
        else if (block == Blocks.gold_block)
        {
            // Dwarven Gold, gold block
            return 600;
        }
        return 0;
    }
    
    public void toggleHighlightChests()
    {
        c.highlightChests = !c.highlightChests;
        sendChat("MT-HighlightChests: toggled " + c.highlightChests);
    }
    
    public void toggleSimulateBlockHardness()
    {
        c.simulateBlockHardness = !c.simulateBlockHardness;
        sendChat("MT-SimulateBlockHardness: toggled " + c.simulateBlockHardness);
    }
    
    public void setMiningSpeedBoostValue(int v)
    {
        v = Utils.clamp(v, 2, 4);
        c.miningSpeedBoostValue = v;
        sendChat("MT: set mining speed boost value to " + c.highlightChests);
    }
    
    public void printMiningSpeedCache()
    {
        sendChat("MT: current tool mining speed is " + getHeldToolMiningSpeed());
        sendChat("MT: cached mining speed is " + getCachedMiningSpeed());
        sendChat("MT: total mining speed is " +
            ((getHeldToolMiningSpeed() + getCachedMiningSpeed()) * getMiningSpeedBoostScale()));
    }
}
