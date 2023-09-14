package a7.tweakception.tweaks;

import a7.tweakception.DevSettings;
import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.mixin.AccessorGuiContainer;
import a7.tweakception.overlay.Anchor;
import a7.tweakception.overlay.TextOverlay;
import a7.tweakception.utils.*;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.util.*;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.io.*;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static a7.tweakception.tweaks.GlobalTweaks.getCurrentIsland;
import static a7.tweakception.tweaks.GlobalTweaks.getTicks;
import static a7.tweakception.utils.McUtils.*;
import static a7.tweakception.utils.McUtils.sendChat;
import static a7.tweakception.utils.Utils.f;

public class GardenTweaks extends Tweak
{
    public static class GardenTweaksConfig
    {
        public boolean simulateCactusKnifeInstaBreak = false;
        public int snapYawAngle = 45;
        public int snapYawRange = 5;
        public int snapPitchAngle = 15;
        public int snapPitchRange = 5;
        public boolean contestDataDumper = false;
        public boolean contestDataDumperDumpHeader = false;
        public boolean autoClaimContest = false;
        public boolean composterAmountNeededOverlay = false;
        public boolean autoTurnOnHideFromStrangersWithSnapYaw = false;
        public int speedOverlayAveragePeriodSecs = 5;
    }
    private static final Map<String, Integer> FUELS = new HashMap<>();
    private static final Map<String, Integer> AGRO_SACK_ITEMS = new HashMap<>();
    private static final List<Block> CROP_BLOCKS = Utils.list(
        Blocks.wheat,
        Blocks.cocoa,
        Blocks.cactus,
        Blocks.carrots,
        Blocks.potatoes,
        Blocks.brown_mushroom,
        Blocks.red_mushroom,
        Blocks.reeds,
        Blocks.melon_block,
        Blocks.pumpkin,
        Blocks.nether_wart);
    private final GardenTweaksConfig c;
    private final MilestoneOverlay milestoneOverlay;
    private final SpeedOverlay speedOverlay;
    private final Map<Instant, ContestInfo> contests = new TreeMap<>();
    private final Matcher composterAmountMatcher = Pattern.compile("((?:\\d{1,3},?)+(?:\\.\\d)?)/(\\d*)k").matcher("");
    private final Matcher sackAmountMatcher = Pattern.compile("Stored: ((?:\\d+,?)+)/\\d+k").matcher("");
    private boolean snapYaw = false;
    private float snapYawPrevAngle = 0.0f;
    private boolean snapPitch = false;
    private float snapPitchPrevAngle = 0.0f;
    private boolean inContestsMenu = false;
    private final List<BlockPos> invalidCrops = new ArrayList<>();
    private boolean logCropBreaks = false;
    private boolean logCropBreaksVerboseToConsole = false;
    private int logCropBreaksStartTicks = 0;
    private int logCropBreaksRollingBpsStartTicks = 0;
    private int logCropBreaksRollingBpsLast = 0;
    private int logCropBreaksMissedBlocks = 0;
    private final Deque<Integer> logCropBreaksBreaks = new ArrayDeque<>();
    // Start tick (inclusive), end tick, amount
    private final List<TriPair<Integer, Integer, Integer>> logCropBreakBpsList = new ArrayList<>();
    private boolean cropGrowRateAnalysis = false;
    // For crops that are broken client side and replaced by air
    private final Long2IntOpenHashMap cropGrowthRateSpecialCropsCache = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap cropGrowthRateBreakTimes = new Long2IntOpenHashMap();
    private int cropGrowthRateCountIllegal = 0;
    private final Map<String, CropGrowthRateData> cropGrowthRateData = new HashMap<>();
    private final LongArrayList tempLongs = new LongArrayList();
    
    private static class CropGrowthRateData
    {
        public final Int2IntOpenHashMap frequencyMap = new Int2IntOpenHashMap();
        public int count = 0;
        public long totalTicks = 0;
        public int averageTicks = 0;
        public int maxTicks = 0;
        public int minTicks = Integer.MAX_VALUE;
    }
    
    static
    {
        FUELS.put("BIOFUEL", 3000);
        FUELS.put("OIL_BARREL", 10000);
        FUELS.put("VOLTA", 10000);
        int compressed = 2048;
        int compressedDouble = 16;
        AGRO_SACK_ITEMS.put("§aEnchanted Baked Potato", compressedDouble);
        AGRO_SACK_ITEMS.put("§aEnchanted Brown Mushroom", compressed);
        AGRO_SACK_ITEMS.put("§aEnchanted Brown Mushroom Block", compressedDouble);
        AGRO_SACK_ITEMS.put("§aEnchanted Cactus", compressedDouble);
        AGRO_SACK_ITEMS.put("§aEnchanted Cactus Green", compressed);
        AGRO_SACK_ITEMS.put("§aEnchanted Carrot", compressed);
        AGRO_SACK_ITEMS.put("§aEnchanted Cocoa Bean", compressed);
        AGRO_SACK_ITEMS.put("§aEnchanted Cookie", compressedDouble);
        AGRO_SACK_ITEMS.put("§aEnchanted Golden Carrot", compressedDouble);
        AGRO_SACK_ITEMS.put("§aEnchanted Hay Bale", compressed);
        AGRO_SACK_ITEMS.put("§aEnchanted Melon", compressed);
        AGRO_SACK_ITEMS.put("§aEnchanted Melon Block", compressedDouble);
        AGRO_SACK_ITEMS.put("§aEnchanted Nether Wart", compressed);
        AGRO_SACK_ITEMS.put("§aEnchanted Potato", compressed);
        AGRO_SACK_ITEMS.put("§aEnchanted Pumpkin", compressed);
        AGRO_SACK_ITEMS.put("§aEnchanted Red Mushroom", compressed);
        AGRO_SACK_ITEMS.put("§aEnchanted Red Mushroom Block", compressedDouble);
        AGRO_SACK_ITEMS.put("§aEnchanted Sugar", compressed);
        AGRO_SACK_ITEMS.put("§aEnchanted Sugar Cane", compressedDouble);
        AGRO_SACK_ITEMS.put("§aMutant Nether Wart", compressedDouble);
        AGRO_SACK_ITEMS.put("§aPolished Pumpkin", compressedDouble);
        AGRO_SACK_ITEMS.put("§aTightly-Tied Hay Bale", compressedDouble);
    }
    
    public GardenTweaks(Configuration configuration)
    {
        super(configuration, "GardenTweaks");
        c = configuration.config.gardenTweaks;
        Tweakception.overlayManager.addOverlay(milestoneOverlay = new MilestoneOverlay());
        Tweakception.overlayManager.addOverlay(speedOverlay = new SpeedOverlay());
        Tweakception.overlayManager.addOverlay(new CropGrowthRateOverlay());
    }
    
    // region Events
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            if (snapYaw)
            {
                float yaw = getPlayer().rotationYaw;
                if (snapYawPrevAngle != yaw)
                    snapYawPrevAngle = getPlayer().rotationYaw = snapAngle(yaw, c.snapYawAngle, c.snapYawRange);
            }
            if (snapPitch)
            {
                float pitch = getPlayer().rotationPitch;
                if (snapPitchPrevAngle != pitch)
                    snapPitchPrevAngle = getPlayer().rotationPitch = snapAngle(pitch, c.snapPitchAngle, c.snapPitchRange);
            }
            
            inContestsMenu = false;
            IInventory chest = McUtils.getOpenedChest();
            if (chest != null)
            {
                if (chest.getName().equals("Your Contests") && chest.getSizeInventory() == 54)
                    inContestsMenu = true;
                
                if (c.autoClaimContest)
                {
                    for (int i = 0; i < chest.getSizeInventory(); i++)
                    {
                        ItemStack stack = chest.getStackInSlot(i);
                        String[] lore = McUtils.getDisplayLore(stack);
                        if (lore != null && lore[lore.length - 1].equals("§eClick to claim reward!"))
                        {
                            sendChat("AutoClaimContest: Claiming slot " + i);
                            getMc().playerController.windowClick(getPlayer().openContainer.windowId,
                                i, 2, 3, getPlayer());
                            getPlayer().closeScreen();
                            return;
                        }
                    }
                }
            }
        }
        else // Tick end
        {
            if (logCropBreaks)
            {
                int c = Utils.removeWhile(logCropBreaksBreaks, ticks -> getTicks() - ticks >= 20);
                
                int rollingBps = logCropBreaksBreaks.size();
                
                if (DevSettings.printLogCropBreaksNumber)
                    sendChatf("%d, %d, removed %d", rollingBps, getTicks(), c);
                if (logCropBreaksVerboseToConsole && (rollingBps < logCropBreaksRollingBpsLast || rollingBps == 0))
                    System.out.println(f("You missed a block (%d) on tick %d",
                        ++logCropBreaksMissedBlocks, getTicks() - logCropBreaksStartTicks));
                else
                    logCropBreaksMissedBlocks = 0;
                
                if (logCropBreaksRollingBpsLast != rollingBps)
                {
                    logCropBreakBpsList.add(new TriPair<>(
                        logCropBreaksRollingBpsStartTicks,
                        getTicks(),
                        logCropBreaksRollingBpsLast
                    ));
                    logCropBreaksRollingBpsLast = rollingBps;
                    logCropBreaksRollingBpsStartTicks = getTicks();
                }
            }
            
            if (cropGrowRateAnalysis)
            {
                for (Long2IntMap.Entry entry : cropGrowthRateSpecialCropsCache.long2IntEntrySet())
                    if (getTicks() - entry.getIntValue() > 200)
                        tempLongs.add(entry.getLongKey());
                tempLongs.forEach(cropGrowthRateSpecialCropsCache::remove);
                tempLongs.clear();
            }
            Tweakception.overlayManager.setEnable(CropGrowthRateOverlay.NAME, cropGrowRateAnalysis);
        }
    }
    
    public void onPacketSend(Packet<?> packet)
    {
        if (!(logCropBreaks || cropGrowRateAnalysis))
            return;
        if (!(packet instanceof C07PacketPlayerDigging))
            return;
        C07PacketPlayerDigging event = (C07PacketPlayerDigging) packet;
        if (event.getStatus() != C07PacketPlayerDigging.Action.START_DESTROY_BLOCK)
            return;
        Block block = getWorld().getBlockState(event.getPosition()).getBlock();
        
        if (cropGrowRateAnalysis)
        {
            if (block == Blocks.brown_mushroom ||
                block == Blocks.red_mushroom ||
                block == Blocks.reeds ||
                block == Blocks.pumpkin ||
                block == Blocks.melon_block)
            {
                cropGrowthRateSpecialCropsCache.put(event.getPosition().toLong(), getTicks());
            }
        }
        
        if (logCropBreaks)
        {
            if (DevSettings.printLogCropBreaksNumber)
                sendChatf("%s, %d", block.getUnlocalizedName(), getTicks());
            if (CROP_BLOCKS.contains(block))
            {
                if (logCropBreaksBreaks.size() > 0 &&
                    logCropBreaksBreaks.peekLast() == getTicks())
                {
                    sendChatf("How did you broke 2 crops in the same tick? Tick %d",
                        getTicks() - logCropBreaksStartTicks);
                }
                else
                {
                    logCropBreaksBreaks.offer(getTicks());
                }
            }
            else
            {
                sendChatf("You broke a non crop block %s on tick %d",
                    block.getUnlocalizedName(),
                    getTicks() - logCropBreaksStartTicks);
            }
        }
    }
    
    public void onPacketBlockChange(BlockPos pos, Block newBlock, IBlockState state)
    {
        // Detect on server events so both break and grow have an equal delay relative to client time
        if (!cropGrowRateAnalysis) return;
        long posLong = pos.toLong();
        
        if (newBlock instanceof BlockCrops)
        {
            int age = state.getValue(BlockCrops.AGE);
            if (age == 0)
                cropGrowthRateBreakTimes.put(posLong, getTicks());
            else if (age == 7)
                onCropGrown(newBlock, pos, getTicks() - cropGrowthRateBreakTimes.get(posLong));
        }
        else if (newBlock == Blocks.cocoa)
        {
            int age = state.getValue(BlockCocoa.AGE);
            if (age == 0)
                cropGrowthRateBreakTimes.put(posLong, getTicks());
            else if (age == 3)
                onCropGrown(newBlock, pos, getTicks() - cropGrowthRateBreakTimes.get(posLong));
        }
        else if (newBlock == Blocks.nether_wart)
        {
            int age = state.getValue(BlockNetherWart.AGE);
            if (age == 0)
                cropGrowthRateBreakTimes.put(posLong, getTicks());
            else if (age == 3)
                onCropGrown(newBlock, pos, getTicks() - cropGrowthRateBreakTimes.get(posLong));
        }
        else if (newBlock == Blocks.cactus ||
            newBlock == Blocks.brown_mushroom ||
            newBlock == Blocks.red_mushroom ||
            newBlock == Blocks.reeds ||
            newBlock == Blocks.pumpkin ||
            newBlock == Blocks.melon_block)
        {
            Block oldBlock = getWorld().getBlockState(pos).getBlock();
            if (oldBlock == Blocks.air)
                onCropGrown(newBlock, pos, getTicks() - cropGrowthRateBreakTimes.get(posLong));
        }
        else if (newBlock == Blocks.air)
        {
            Block oldBlock = getWorld().getBlockState(pos).getBlock();
            if (oldBlock == Blocks.cactus ||
                oldBlock == Blocks.brown_mushroom ||
                oldBlock == Blocks.red_mushroom ||
                oldBlock == Blocks.reeds ||
                oldBlock == Blocks.pumpkin ||
                oldBlock == Blocks.melon_block)
                cropGrowthRateBreakTimes.put(posLong, getTicks());
            else if (cropGrowthRateSpecialCropsCache.containsKey(posLong))
            {
                cropGrowthRateBreakTimes.put(posLong, getTicks());
                cropGrowthRateSpecialCropsCache.remove(posLong);
            }
        }
    }
    
    private void onCropGrown(Block block, BlockPos pos, int ticksTaken)
    {
        long posLong = pos.toLong();
        if (cropGrowthRateBreakTimes.containsKey(posLong))
        {
//            if (ticksTaken < 200 || ticksTaken > 20 * 60 * 15)
            if (ticksTaken > 20 * 60 * 15) // Crops like pumpkin can insta grow
            {
                sendChatf("CropGrowRateAnalysis: Got an illegal growth time of %d ticks, block: %s, pos: %s",
                    ticksTaken, block.getUnlocalizedName(), pos.toString());
                cropGrowthRateCountIllegal++;
            }
            else
            {
                String name = block.getUnlocalizedName();
                CropGrowthRateData data = cropGrowthRateData.computeIfAbsent(name, key -> new CropGrowthRateData());
                data.frequencyMap.mergeInt(ticksTaken, 1, Integer::sum);
                if (ticksTaken < data.minTicks)
                    data.minTicks = ticksTaken;
                if (ticksTaken > data.maxTicks)
                    data.maxTicks = ticksTaken;
                data.totalTicks += ticksTaken;
                data.count++;
                data.averageTicks = (int) (data.totalTicks / (long) data.count);
            }
            cropGrowthRateBreakTimes.remove(posLong);
        }
    }
    
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        if (getMc().currentScreen == null && Keyboard.getEventKeyState())
        {
            int key = Keyboard.getEventKey();
            if (key == Tweakception.keybindToggleSnapYaw.getKeyCode())
                toggleSnapYaw();
            else if (key == Tweakception.keybindToggleSnapPitch.getKeyCode())
                toggleSnapPitch();
        }
    }
    
    public void onItemTooltip(ItemTooltipEvent event)
    {
        List<String> tooltip = event.toolTip;
        ItemStack itemStack = event.itemStack;
        
        if (tooltip == null || itemStack == null)
            return;
        
        if (McUtils.getOpenedChest() != null &&
            McUtils.getOpenedChest().getName().startsWith("Auctions"))
        {
            String id = Utils.getSkyblockItemId(itemStack);
            if (FUELS.containsKey(id))
            {
                for (int i = 0; i < tooltip.size(); i++)
                {
                    if (Utils.auctionPriceMatcher.reset(tooltip.get(i)).find())
                    {
                        int count = itemStack.stackSize;
                        double price = Utils.parseDouble(Utils.auctionPriceMatcher.group("price"));
                        double unitPrice = price / count / FUELS.get(id) * 10000;
                        String str = Utils.formatCommas((long) unitPrice);
                        tooltip.add(i + 1, "§6 " + str + " coins/10k fuel");
                        return;
                    }
                }
            }
        }
    }
    
    public void onPlayerListItemUpdateDisplayName(S38PacketPlayerListItem.AddPlayerData addPlayerData,
                                                  NetworkPlayerInfo networkPlayerInfo)
    {
        IChatComponent nameComponent = addPlayerData.getDisplayName();
        if (nameComponent != null)
        {
            String name = nameComponent.getFormattedText();
            if (name.startsWith("§r Milestone: "))
            {
                milestoneOverlay.milestoneText = name.substring(3);
            }
        }
    }
    
    public void onGuiMouseInput(GuiScreenEvent.MouseInputEvent.Pre event, int x, int y)
    {
        if (!c.contestDataDumper || !inContestsMenu)
            return;
        
        AccessorGuiContainer accessor = (AccessorGuiContainer) event.gui;
        int xSize = accessor.getXSize();
        int guiLeft = accessor.getGuiLeft();
        int guiTop = accessor.getGuiTop();
        
        if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0)
        {
            if (Utils.isMouseInsideRect(x, y, guiLeft + xSize + 20, guiTop, 100, 10))
            {
                getContests();
                event.setCanceled(true);
            }
            else if (Utils.isMouseInsideRect(x, y, guiLeft + xSize + 20, guiTop + (9 + 5) * 2, 100, 10))
            {
                dumpContests();
                event.setCanceled(true);
            }
        }
    }
    
    public void onGuiDrawPost(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (!(event.gui instanceof GuiContainer))
            return;
        FontRenderer fr = getMc().fontRendererObj;
        AccessorGuiContainer accessor = (AccessorGuiContainer) event.gui;
        int xSize = accessor.getXSize();
        int guiLeft = accessor.getGuiLeft();
        int guiTop = accessor.getGuiTop();
        
        if (c.contestDataDumper & inContestsMenu)
        {
            Color color = new Color(50, 50, 50);
            int x = guiLeft + xSize + 20;
            int y = guiTop;
            
            GuiScreen.drawRect(x, y,
                x + 100, y + 10,
                color.getRGB());
            fr.drawString("Click to add the contests in this page",
                x + 1, y + 1, 0xFFFFFFFF);
            y += fr.FONT_HEIGHT + 5;
            fr.drawString("Contest count: " + contests.size(),
                x, y, 0xFFFFFFFF);
            y += fr.FONT_HEIGHT + 5;
            GuiScreen.drawRect(x, y,
                x + 100, y + 10,
                color.getRGB());
            fr.drawString("Click to dump csv",
                x + 1, y + 1, 0xFFFFFFFF);
            return;
        }
        
        IInventory chest = getOpenedChest();
        if (chest == null)
            return;
        
        if (chest.getName().equals("Enchanted Agronomy Sack"))
        {
            List<Pair<String, String>> list = new ArrayList<>();
            for (int i = 0; i < chest.getSizeInventory(); i++)
            {
                ItemStack stack = chest.getStackInSlot(i);
                String[] lore = McUtils.getDisplayLore(stack);
                if (lore != null &&
                    stack.hasDisplayName() && AGRO_SACK_ITEMS.containsKey(stack.getDisplayName()))
                {
                    int minimumCount = AGRO_SACK_ITEMS.get(stack.getDisplayName());
                    for (String line : lore)
                    {
                        if (sackAmountMatcher.reset(McUtils.cleanColor(line)).matches())
                        {
                            int count = Utils.parseInt(sackAmountMatcher.group(1));
                            if (count < minimumCount)
                                list.add(new Pair<>(stack.getDisplayName(), line));
                            break;
                        }
                    }
                }
            }
            
            if (!list.isEmpty())
            {
                int x = guiLeft - 5 - list.stream().mapToInt(p -> fr.getStringWidth(p.b)).max().getAsInt();
                int y = guiTop;
                for (Pair<String, String> p : list)
                {
                    fr.drawString(p.a,
                        x - fr.getStringWidth(p.a) - 5, y, 0xFFFFFFFF);
                    fr.drawString(p.b,
                        x, y, 0xFFFFFFFF);
                    y += fr.FONT_HEIGHT + 5;
                }
            }
        }
        else if (c.composterAmountNeededOverlay &&
            chest.getName().equals("Composter"))
        {
            ItemStack crop = chest.getStackInSlot(2 - 1);
            ItemStack fuel = chest.getStackInSlot(8 - 1);
            if (crop == null || fuel == null)
                return;
            if (crop.getItem() != Item.getItemFromBlock(Blocks.stained_glass_pane) ||
                fuel.getItem() != Item.getItemFromBlock(Blocks.stained_glass_pane))
                return;
            
            String cropNeeded = "";
            String fuelNeeded = "";
            
            String[] lore = McUtils.getDisplayLore(crop);
            if (lore == null)
                return;
            for (String line : lore)
            {
                if (composterAmountMatcher.reset(McUtils.cleanColor(line)).find())
                {
                    float amount = Utils.parseFloat(composterAmountMatcher.group(1));
                    int limit = Integer.parseInt(composterAmountMatcher.group(2)) * 1000;
                    float needed = limit - amount;
                    float count = Utils.roundToDigits(needed / 25600, 1);
                    cropNeeded = "§6 " + count + "x §9Box of Seeds §6needed";
                    break;
                }
            }
            lore = McUtils.getDisplayLore(fuel);
            if (lore == null)
                return;
            for (String line : lore)
            {
                if (composterAmountMatcher.reset(McUtils.cleanColor(line)).find())
                {
                    float amount = Utils.parseFloat(composterAmountMatcher.group(1));
                    int limit = Integer.parseInt(composterAmountMatcher.group(2)) * 1000;
                    float needed = limit - amount;
                    float count = Utils.roundToDigits(needed / 10000, 1);
                    float countBiofuel = Utils.roundToDigits(needed / 3000, 1);
                    fuelNeeded = "§6 "+count+"x §9Volta§6/"+countBiofuel+"x §9Biofuel §6needed";
                    break;
                }
            }
            
            int x = guiLeft + xSize / 2;
            int y = guiTop - fr.FONT_HEIGHT - 5;
            int w = fr.getStringWidth("§8|") / 2;
            fr.drawString(cropNeeded,
                x - fr.getStringWidth(cropNeeded) - w - 5, y, 0xFFFFFFFF);
            fr.drawString("§8|",
                x - w, y, 0xFFFFFFFF);
            fr.drawString(fuelNeeded,
                x + w + 3, y, 0xFFFFFFFF);
        }
    }
    
    public void onRenderLast(RenderWorldLastEvent event)
    {
        if (!invalidCrops.isEmpty())
        {
            Color color = new Color(255, 0, 0, 128);
            for (BlockPos pos : invalidCrops)
            {
                RenderUtils.drawBeaconBeamOrBoundingBox(pos, color, getPartialTicks(), 0, 50);
            }
        }
    }
    
    public void onWorldLoad(WorldEvent.Load event)
    {
        speedOverlay.reset(c.speedOverlayAveragePeriodSecs);
    }
    
    public void onWorldUnload(WorldEvent.Unload event)
    {
        invalidCrops.clear();
        resetCropGrowRates();
    }
    
    // endregion Events
    
    // region Misc
    
    private void getContests()
    {
        IInventory inv = McUtils.getOpenedChest();
        List<String> seasons = Arrays.asList("Spring", "Summer", "Autumn", "Winter");
        
        final Matcher contestDateMatcher = Pattern.compile(
            "^§a([\\w ]+) (\\d+)(?:rd|st|nd|th), Year (\\d+)$").matcher("");
        final Matcher contestTypeMatcher = Pattern.compile(
            "^(.*) Contest$").matcher("");
        final Matcher medalBracketMatcher = Pattern.compile(
            "^(GOLD|SILVER|BRONZE) \\(Top \\d+%\\): ((?:\\d+,?)+)$").matcher("");
        final Matcher yourScoreMatcher = Pattern.compile(
            "^Your score: ((?:\\d+,?)+) collected!$").matcher("");
        
        for (int i = 0; i < 54; i++)
        {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack == null)
                continue;
            if (!contestDateMatcher.reset(stack.getDisplayName()).matches())
                continue;
            ContestInfo contestInfo = new ContestInfo();
            {
                String[] monthStrings = contestDateMatcher.group(1).split(" ", 2);
                int month = seasons.indexOf(monthStrings[monthStrings.length - 1]) * 3 + 1;
                if (monthStrings.length == 2 && monthStrings[0].equals("Early"))
                    ;
                else if (monthStrings.length == 1)
                    month += 1;
                else if (monthStrings.length == 2 && monthStrings[0].equals("Late"))
                    month += 2;
                int day = Utils.parseInt(contestDateMatcher.group(2));
                int year = Utils.parseInt(contestDateMatcher.group(3));
                long skyblockEpoch = 1_559_829_300L;
                long secs = skyblockEpoch +
                    year * 60L * 20 * 31 * 12 +
                    (month - 1) * 60L * 20 * 31 +
                    (day - 1) * 60L * 20;
                contestInfo.date = Instant.ofEpochMilli(secs * 1000L);
                contestInfo.sbDate = contestDateMatcher.group().substring(2);
            }
            {
                String[] lore = McUtils.getDisplayLore(stack);
                for (String line : lore)
                {
                    line = McUtils.cleanColor(line);
                    if (contestTypeMatcher.reset(line).matches())
                        contestInfo.type = contestTypeMatcher.group(1);
                    else if (medalBracketMatcher.reset(line).matches())
                        switch (medalBracketMatcher.group(1))
                        {
                            case "GOLD":
                                contestInfo.gold = Utils.parseInt(medalBracketMatcher.group(2));
                                break;
                            case "SILVER":
                                contestInfo.silver = Utils.parseInt(medalBracketMatcher.group(2));
                                break;
                            case "BRONZE":
                                contestInfo.bronze = Utils.parseInt(medalBracketMatcher.group(2));
                                break;
                        }
                    else if (yourScoreMatcher.reset(line).matches())
                        contestInfo.score = Utils.parseInt(yourScoreMatcher.group(1));
                    else if (line.equals("Contest boosted by Finnegan!"))
                        contestInfo.finneganBoosted = true;
                }
            }
            contests.put(contestInfo.date, contestInfo);
        }
    }
    
    private void dumpContests()
    {
        List<String> list = new ArrayList<>();
        if (c.contestDataDumperDumpHeader)
            list.add("datetime,millis,sb date,type,bronze,silver,gold,score,finnegan");
        for (ContestInfo contestInfo : contests.values())
        {
            list.add(f("%s,%d,\"%s\",%s,%d,%d,%d,%d,%d",
                contestInfo.date,
                contestInfo.date.toEpochMilli(),
                contestInfo.sbDate, // Has comma
                contestInfo.type,
                contestInfo.bronze,
                contestInfo.silver,
                contestInfo.gold,
                contestInfo.score,
                contestInfo.finneganBoosted ? 1 : 0
            ));
        }
        contests.clear();
        
        try
        {
            File file = Tweakception.configuration.createWriteFileWithCurrentDateTime("jacob_contests_$.csv", list);
            sendChat("Dumped contests");
            getPlayer().addChatMessage(new ChatComponentTranslation("Output written to file %s",
                McUtils.makeFileLink(file)));
            sendChat("Also copied to clipboard");
            Utils.setClipboard(String.join(System.lineSeparator(), list));
        }
        catch (Exception e)
        {
            sendChat("Error occurred");
            e.printStackTrace();
        }
    }
    
    private float snapAngle(float angle, int snapAngle, int snapRange)
    {
        float diffToSnapPoint = Math.abs(angle % snapAngle);
        if (diffToSnapPoint <= snapRange / 2.0f ||
            diffToSnapPoint >= snapAngle - snapRange / 2.0f)
        {
            return Math.round(angle / snapAngle) * snapAngle;
        }
        return angle;
    }
    
    public void verifyCrops(AxisAlignedBB aabb)
    {
        boolean hasAxisX = aabb.minX != aabb.maxX;
        boolean hasAxisZ = aabb.minZ != aabb.maxZ;
        int dims2d = 0;
        if (hasAxisX) dims2d++;
        if (hasAxisZ) dims2d++;
        if (dims2d == 2)
        {
            sendChat("VerifyCrops: Selection must be a point, line, or a vertical plane!");
            return;
        }
        EnumFacing[] facings;
        if (dims2d == 0)
            facings = new EnumFacing[]
                {
                    getPlayer().getHorizontalFacing(),
                    getPlayer().getHorizontalFacing().getOpposite(),
                };
        else
            facings = new EnumFacing[]
                {
                    hasAxisX ? EnumFacing.NORTH : EnumFacing.EAST,
                    hasAxisX ? EnumFacing.SOUTH : EnumFacing.WEST,
                };
        
        // The const list but with stems
        List<Block> cropBlocks = Utils.list(
            Blocks.wheat,
            Blocks.cocoa,
            Blocks.cactus,
            Blocks.carrots,
            Blocks.potatoes,
            Blocks.brown_mushroom,
            Blocks.red_mushroom,
            Blocks.reeds,
            Blocks.melon_stem,
            Blocks.pumpkin_stem,
            Blocks.nether_wart);
        
        // Invalid blocks in invalidCropsTemp are added to invalidCrops after a valid block is found in that line
        List<BlockPos> invalidCropsTemp = new ArrayList<>();
        WorldClient world = getWorld();
        invalidCrops.clear();
        for (EnumFacing facing : facings)
        {
            for (BlockPos.MutableBlockPos startPos : BlockPos.getAllInBoxMutable(
                new BlockPos(aabb.minX, aabb.minY, aabb.minZ),
                new BlockPos(aabb.maxX, aabb.maxY, aabb.maxZ)))
            {
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(startPos.getX(), startPos.getY(), startPos.getZ());
                invalidCropsTemp.clear();
                int continuousInvalidCount = 0;
                boolean started = false;
                boolean isCactus = false;
                Block targetCrop = Blocks.wheat;
                while (continuousInvalidCount <= 5)
                {
                    Block block = world.getBlockState(pos).getBlock();
                    if (!started)
                    {
                        if (cropBlocks.contains(block))
                        {
                            started = true;
                            continuousInvalidCount = 0;
                            targetCrop = block;
                            if (block == Blocks.cactus)
                                isCactus = true;
                        }
                        else
                        {
                            continuousInvalidCount++;
                        }
                    }
                    else
                    {
                        if (block == targetCrop)
                        {
                            if (continuousInvalidCount > 0)
                            {
                                continuousInvalidCount = 0;
                                invalidCrops.addAll(invalidCropsTemp);
                                invalidCropsTemp.clear();
                            }
                        }
                        else
                        {
                            continuousInvalidCount++;
                            if (!(continuousInvalidCount == 1 && isCactus))
                                invalidCropsTemp.add(pos.getImmutable());
                        }
                    }
                    pos.set(pos.getX() + facing.getFrontOffsetX(),
                        pos.getY(),
                        pos.getZ() + facing.getFrontOffsetZ());
                }
            }
        }
        sendChat("VerifyCrops: Found " + invalidCrops.size() + " invalid crops");
    }
    
    private static class ContestInfo
    {
        public Instant date;
        public String sbDate;
        public String type;
        public int bronze;
        public int silver;
        public int gold;
        public int score;
        public boolean finneganBoosted;
    }
    
    private static class CropBreakLogExcelDumper implements Closeable
    {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("new sheet");
        Row rowTime = sheet.createRow(0);
        Row rowBps = sheet.createRow(1);
        Row rowMissed = sheet.createRow(2);
        String time;
        int bps;
        int missed;
        int column;
        
        public CropBreakLogExcelDumper()
        {
            sheet.setDefaultColumnWidth(2);
        }
        
        public void setTime(String time)
        {
            this.time = time;
        }
        
        public void setBps(int bps)
        {
            this.bps = bps;
        }
        
        public void setMissed(int missed)
        {
            this.missed = missed;
        }
        
        public void write(int column)
        {
            this.column = column;
            rowTime.createCell(column).setCellValue(time);
            rowBps.createCell(column).setCellValue(bps);
            rowMissed.createCell(column).setCellValue(missed);
        }
        
        public File dump() throws IOException
        {
            SheetConditionalFormatting cf = sheet.getSheetConditionalFormatting();
            {
                ConditionalFormattingRule rule = cf.createConditionalFormattingColorScaleRule();
                ColorScaleFormatting cs = rule.getColorScaleFormatting();
                cs.getThresholds()[0].setRangeType(ConditionalFormattingThreshold.RangeType.MIN);
                cs.getThresholds()[1].setRangeType(ConditionalFormattingThreshold.RangeType.PERCENTILE);
                cs.getThresholds()[1].setValue(50.0);
                cs.getThresholds()[2].setRangeType(ConditionalFormattingThreshold.RangeType.MAX);
                ((ExtendedColor) cs.getColors()[0]).setARGBHex("FFF8696B");
                ((ExtendedColor) cs.getColors()[1]).setARGBHex("FFFFEB84");
                ((ExtendedColor) cs.getColors()[2]).setARGBHex("FF63BE7B");
                cf.addConditionalFormatting(new CellRangeAddress[] {new CellRangeAddress(1, 1, 0, column - 1)}, rule);
            }
            {
                ConditionalFormattingRule rule = cf.createConditionalFormattingColorScaleRule();
                ColorScaleFormatting cs = rule.getColorScaleFormatting();
                cs.getThresholds()[0].setRangeType(ConditionalFormattingThreshold.RangeType.MAX);
                cs.getThresholds()[1].setRangeType(ConditionalFormattingThreshold.RangeType.PERCENTILE);
                cs.getThresholds()[1].setValue(50.0);
                cs.getThresholds()[2].setRangeType(ConditionalFormattingThreshold.RangeType.MIN);
                ((ExtendedColor) cs.getColors()[0]).setARGBHex("FFF8696B");
                ((ExtendedColor) cs.getColors()[1]).setARGBHex("FFFFEB84");
                ((ExtendedColor) cs.getColors()[2]).setARGBHex("FF63BE7B");
                cf.addConditionalFormatting(new CellRangeAddress[] {new CellRangeAddress(2, 2, 0, column - 1)}, rule);
            }
            
            
            try
            {
                File file = Tweakception.configuration.createFileWithCurrentDateTime("cropbreaklog_$.xlsx");
                try (FileOutputStream stream = new FileOutputStream(file))
                {
                    wb.write(stream);
                }
                return file;
            }
            catch (IOException e)
            {
                e.printStackTrace();
                throw e;
            }
        }
        
        @Override
        public void close() throws IOException
        {
            wb.close();
        }
    }
    
    private void dumpCropBreakLog()
    {
        StringBuilder time = new StringBuilder();
        int lastBps = 0;
        int missedTicks = 0;
        boolean hasExcel = false; // Tweakception.isPoiPresent();
        File file;
        
        try (CropBreakLogExcelDumper excel = hasExcel ? new CropBreakLogExcelDumper() : null;
            PrintWriter writer = new PrintWriter(Tweakception.configuration.createWriterFor(
                file = Tweakception.configuration.createFileWithCurrentDateTime("cropbreaklog_$.csv")
            ))
        )
        {
            int column = 0;
            
            for (TriPair<Integer, Integer, Integer> tri : logCropBreakBpsList)
            {
                for (int ticks = tri.a; ticks < tri.b; ticks++)
                {
                    int ticksFromStart = ticks - logCropBreaksStartTicks;
                    int min = ticksFromStart / 1200;
                    int sec = ticksFromStart / 20 % 60;
                    int tick = ticksFromStart % 20;
                    
                    if (min < 10)
                        time.append('0');
                    time.append(min);
                    time.append(':');
                    if (sec < 10)
                        time.append('0');
                    time.append(sec);
                    time.append(':');
                    if (tick < 10)
                        time.append('0');
                    time.append(tick);
                    
                    if (hasExcel)
                    {
                        excel.setTime(time.toString());
                    }
                    else
                    {
                        writer.print(time);
                        writer.print(',');
                    }
                    time.setLength(0);

                    if (hasExcel)
                        excel.setBps(tri.c);
                    else
                    {
                        writer.print(tri.c);
                        writer.print(',');
                    }
                    
                    if (tri.c == 0 || tri.c < lastBps)
                        missedTicks++;
                    else
                        missedTicks = 0;
                    
                    if (hasExcel)
                    {
                        excel.setMissed(missedTicks);
                        excel.write(column);
                    }
                    else
                    {
                        writer.print(missedTicks);
                        writer.println();
                    }
                    
                    lastBps = tri.c;
                    column++;
                }
            }
            
            if (hasExcel)
            {
                file = excel.dump();
            }
            
            sendChat("Dumped cropbreaklog");
            getPlayer().addChatMessage(new ChatComponentTranslation("Output written to file %s",
                McUtils.makeFileLink(file)));
        }
        catch (IOException e)
        {
            sendChat("Failed to write file");
            e.printStackTrace();
        }
    }
    
    private void resetCropGrowRates()
    {
        cropGrowthRateCountIllegal = 0;
        cropGrowthRateBreakTimes.clear();
        cropGrowthRateSpecialCropsCache.clear();
        cropGrowthRateData.clear();
    }
    
    // endregion Misc
    
    // region Feature access
    
    public boolean isSimulateCactusKnifeInstaBreakOn()
    {
        return c.simulateCactusKnifeInstaBreak;
    }
    
    // endregion Feature access
    
    // region Overlays
    
    private static class MilestoneOverlay extends TextOverlay
    {
        public static final String NAME = "MilestoneOverlay";
        public String milestoneText = null;
        
        public MilestoneOverlay()
        {
            super(NAME);
            setAnchor(Anchor.CenterRight);
            setOrigin(Anchor.CenterRight);
            setX(-100);
            setY(-100);
            setTextAlignment(1);
        }
        
        @Override
        public void update()
        {
            super.update();
            List<String> list = new ArrayList<>();
            if (milestoneText != null && getCurrentIsland() == SkyblockIsland.GARDEN)
                list.add(milestoneText);
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("Milestone: h");
            return list;
        }
    }
    
    private class SpeedOverlay extends TextOverlay
    {
        public static final String NAME = "SpeedOverlay";
        private int averagingPeriodSecs;
        private int index;
        private double[] bpss;
        private double avgBps;
        private double lastX;
        private double lastY;
        private double lastZ;
        
        public SpeedOverlay()
        {
            super(NAME);
            setAnchor(Anchor.TopLeft);
            setOrigin(Anchor.TopLeft);
            setX(300);
            setY(300);
            reset(c.speedOverlayAveragePeriodSecs);
        }
        
        public void reset(int secs)
        {
            averagingPeriodSecs = secs;
            bpss = new double[averagingPeriodSecs * 20];
            index = 0;
            avgBps = 0.0;
        }
        
        @Override
        public void update()
        {
            super.update();
            List<String> list = getContent();
            list.clear();
            double x = getPlayer().posX;
            double y = getPlayer().posY;
            double z = getPlayer().posZ;
            double bps = Math.sqrt((x-lastX)*(x-lastX) + (y-lastY)*(y-lastY) + (z-lastZ)*(z-lastZ)) * 20.0;
            lastX = x;
            lastY = y;
            lastZ = z;
            
            bpss[index] = bps;
            if (DevSettings.copySpeedNums && getTicks() % (averagingPeriodSecs * 20) == 0)
            {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < averagingPeriodSecs * 20; i++)
                    sb.append(bpss[i]).append(System.lineSeparator());
                Utils.setClipboard(sb.toString());
                sendChatf("Copied speed numbers in the last %d secs", averagingPeriodSecs);
            }
            index += 1;
            index %= averagingPeriodSecs * 20;
            double totalBps = 0.0;
            for (int i = 0; i < averagingPeriodSecs * 20; i++)
                totalBps += bpss[i];
//            avgBps = totalBps / averagingPeriodSecs;
            avgBps = (avgBps * 0.5) + (totalBps / averagingPeriodSecs / 20 * 0.5);
            list.add(f("Speed: %.3fm/s", avgBps));
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("SpeedOverlay");
            return list;
        }
    }
    
    private class CropGrowthRateOverlay extends TextOverlay
    {
        public static final String NAME = "CropGrowthRateOverlay";
        
        public CropGrowthRateOverlay()
        {
            super(NAME);
            setAnchor(Anchor.TopLeft);
            setOrigin(Anchor.TopLeft);
            setX(400);
            setY(100);
        }
        
        @Override
        public void update()
        {
            super.update();
            List<String> list = getContent();
            list.clear();
            list.add(f("Pending: %d blocks", cropGrowthRateBreakTimes.size()));
            list.add(f("Illegal: %d blocks", cropGrowthRateCountIllegal));
            list.add(f("Special cache: %d blocks", cropGrowthRateSpecialCropsCache.size()));
            for (Map.Entry<String, CropGrowthRateData> entry : cropGrowthRateData.entrySet())
            {
                CropGrowthRateData data = entry.getValue();
                list.add("");
                list.add(f("%s: %d blocks", entry.getKey(), data.count));
                list.add(f("Average: %.2fs %dt", data.averageTicks / 20.0f, data.averageTicks));
                list.add(f("Min: %.2fs %dt", data.minTicks / 20.0f, data.minTicks));
                list.add(f("Max: %.2fs %dt", data.maxTicks / 20.0f, data.maxTicks));
            }
            setContent(list);
        }
        
        @Override
        public List<String> getDefaultContent()
        {
            List<String> list = new ArrayList<>();
            list.add("CropGrowthRateOverlay");
            return list;
        }
    }
    
    // endregion Overlays
    
    // region Commands
    
    public void toggleSimulateCactusKnifeInstaBreak()
    {
        c.simulateCactusKnifeInstaBreak = !c.simulateCactusKnifeInstaBreak;
        sendChat("SimulateCactusKnifeInstaBreak: Toggled " + c.simulateCactusKnifeInstaBreak);
    }
    
    public void toggleMilestoneOverlay()
    {
        boolean state = Tweakception.overlayManager.toggle(MilestoneOverlay.NAME);
        sendChat("Toggled milestone overlay " + state);
    }
    
    public void toggleSnapYaw()
    {
        snapYaw = !snapYaw;
        sendChat("SnapYaw: Toggled " + snapYaw);
        if (c.autoTurnOnHideFromStrangersWithSnapYaw)
        {
            sendChat("SnapYaw: Also toggling HideFromStrangers");
            Tweakception.globalTweaks.setHideFromStrangers(snapYaw);
        }
    }
    public void setSnapYawAngle(int angle)
    {
        c.snapYawAngle = angle < 0 ? 45 : Utils.clamp(angle, 1, 180);
        sendChat("SnapYaw: Set snap angle to " + c.snapYawAngle);
    }
    
    public void setSnapYawRange(int range)
    {
        c.snapYawRange = range < 0 ? 5 : Utils.clamp(range, 0, 90);
        sendChat("SnapYaw: Set snap range to " + c.snapYawRange);
    }
    
    public void toggleSnapPitch()
    {
        snapPitch = !snapPitch;
        sendChat("SnapPitch: Toggled " + snapPitch);
    }
    
    public void setSnapPitchAngle(int angle)
    {
        c.snapPitchAngle = angle < 0 ? 45 : Utils.clamp(angle, 1, 180);
        sendChat("SnapPitch: Set snap angle to " + c.snapPitchAngle);
    }
    
    public void setSnapPitchRange(int range)
    {
        c.snapPitchRange = range < 0 ? 5 : Utils.clamp(range, 0, 90);
        sendChat("SnapPitch: Set snap range to " + c.snapPitchRange);
    }
    
    public void toggleContestDataDumper()
    {
        c.contestDataDumper = !c.contestDataDumper;
        sendChat("ContestDataDumper: Toggled " + c.contestDataDumper);
    }
    
    public void toggleContestDataDumperDumpHeader()
    {
        c.contestDataDumperDumpHeader = !c.contestDataDumperDumpHeader;
        sendChat("ContestDataDumper: Toggled csv header " + c.contestDataDumperDumpHeader);
    }
    
    public void verifyCrops()
    {
        if (Tweakception.globalTweaks.isInAreaEditMode())
            verifyCrops(Tweakception.globalTweaks.getAreaEditBlockSelection());
        else
            sendChat("VerifyCrops: Global tweaks AreaEdit feature is off");
    }
    
    public void verifyCropsClear()
    {
        invalidCrops.clear();
    }
    
    public void toggleAutoClaimContests()
    {
        c.autoClaimContest = !c.autoClaimContest;
        sendChat("AutoClaimContest: Toggled " + c.autoClaimContest);
    }
    
    public void toggleComposterAmountNeededOverlay()
    {
        c.composterAmountNeededOverlay = !c.composterAmountNeededOverlay;
        sendChat("ComposterAmountNeededOverlay: Toggled " + c.composterAmountNeededOverlay);
    }
    
    public void toggleAutoTurnOnHideFromStrangersWithSnapYaw()
    {
        c.autoTurnOnHideFromStrangersWithSnapYaw = !c.autoTurnOnHideFromStrangersWithSnapYaw;
        sendChat("AutoTurnOnHideFromStrangersWithSnapYaw: Toggled " + c.autoTurnOnHideFromStrangersWithSnapYaw);
    }
    
    public void toggleLogCropBreaks()
    {
        logCropBreaks = !logCropBreaks;
        sendChat("LogCropBreaks: Toggled " + logCropBreaks);
        if (logCropBreaks)
        {
            logCropBreaksStartTicks = getTicks();
            logCropBreaksRollingBpsLast = 0;
            logCropBreaksRollingBpsStartTicks = getTicks();
            logCropBreaksMissedBlocks = 0;
        }
        else
        {
            logCropBreakBpsList.add(new TriPair<>(
                logCropBreaksRollingBpsStartTicks,
                getTicks(),
                logCropBreaksBreaks.size()
            ));
            dumpCropBreakLog();
            logCropBreakBpsList.clear();
            logCropBreaksBreaks.clear();
        }
    }
    
    public void toggleLogCropBreaksVerboseConsole()
    {
        logCropBreaksVerboseToConsole = !logCropBreaksVerboseToConsole;
        sendChat("LogCropBreaks: Toggled verbose console " + logCropBreaksVerboseToConsole);
    }
    
    public void toggleSpeedOverlay()
    {
        boolean state = Tweakception.overlayManager.toggle(SpeedOverlay.NAME);
        sendChat("SpeedOverlay: Toggled " + state);
    }
    
    public void setSpeedOverlayAveragePeriodSecs(int secs)
    {
        c.speedOverlayAveragePeriodSecs = secs == -1 ? 5 : Utils.clamp(secs, 1, 60);
        speedOverlay.reset(c.speedOverlayAveragePeriodSecs);
        sendChat("SpeedOverlay: Set average period to " + c.speedOverlayAveragePeriodSecs + " secs");
    }
    
    public void toggleCropGrowthRateAnalysis()
    {
        cropGrowRateAnalysis = !cropGrowRateAnalysis;
        sendChat("CropGrowRateAnalysis: Toggled " + cropGrowRateAnalysis);
        if (!cropGrowRateAnalysis)
            resetCropGrowRates();
    }
    
    public void resetCropGrowthRateAnalysis()
    {
        if (!cropGrowRateAnalysis)
        {
            sendChat("CropGrowRateAnalysis: Feature isn't on");
            return;
        }
        sendChat("CropGrowRateAnalysis: Reset");
        resetCropGrowRates();
    }
    
    public void dumpCropGrowthRateAnalysis()
    {
        if (!cropGrowRateAnalysis)
        {
            sendChat("CropGrowRateAnalysis: Feature isn't on");
            return;
        }
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, CropGrowthRateData> entry : cropGrowthRateData.entrySet())
        {
            lines.add("===========================================================");
            CropGrowthRateData data = entry.getValue();
            lines.add(f("%s", entry.getKey()));
            lines.add(f("%d", data.count));
            lines.add(f("Avg: %.2fs %dt", data.averageTicks / 20.0f, data.averageTicks));
            lines.add(f("Min: %.2fs %dt", data.minTicks / 20.0f, data.minTicks));
            lines.add(f("Max: %.2fs %dt", data.maxTicks / 20.0f, data.maxTicks));
            lines.add("Individual data below =====================================");
            for (Int2IntMap.Entry e : data.frequencyMap.int2IntEntrySet()
                .stream()
                .sorted(Comparator
                    .comparing(Int2IntMap.Entry::getIntValue, Comparator.reverseOrder())
                    .thenComparing(Int2IntMap.Entry::getIntKey, Comparator.reverseOrder()))
                .collect(Collectors.toList()))
            {
                lines.add(e.getIntKey() + ", " + e.getIntValue());
            }
            lines.add("===========================================================");
            lines.add("");
        }
        try
        {
            File file = Tweakception.configuration.createWriteFileWithCurrentDateTime("cropgrowthrates_$.txt", lines);
            sendChat("Dumped growth rates");
            getPlayer().addChatMessage(new ChatComponentTranslation("Output written to file %s",
                McUtils.makeFileLink(file)));
        }
        catch (IOException e)
        {
            sendChat(e.toString());
            e.printStackTrace();
        }
    }
    
    // endregion Commands
}
