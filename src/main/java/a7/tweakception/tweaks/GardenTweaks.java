package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.mixin.AccessorGuiContainer;
import a7.tweakception.overlay.Anchor;
import a7.tweakception.overlay.TextOverlay;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Pair;
import a7.tweakception.utils.RenderUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.util.*;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static a7.tweakception.tweaks.GlobalTweaks.getCurrentIsland;
import static a7.tweakception.utils.McUtils.*;
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
    }
    private static final Map<String, Integer> FUELS = new HashMap<>();
    private final GardenTweaksConfig c;
    private final MilestoneOverlay milestoneOverlay;
    private final Map<Instant, ContestInfo> contests = new TreeMap<>();
    private final Matcher composterAmountMatcher = Pattern.compile("((?:\\d{1,3},?)+(?:\\.\\d)?)/(\\d*)k").matcher("");
    private final Matcher sackAmountMatcher = Pattern.compile("Stored: ((?:\\d+,?)+)/\\d+k").matcher("");
    private boolean snapYaw = false;
    private float snapYawPrevAngle = 0.0f;
    private boolean snapPitch = false;
    private float snapPitchPrevAngle = 0.0f;
    private boolean inContestsMenu = false;
    private final List<BlockPos> invalidCrops = new ArrayList<>();
    
    static
    {
        FUELS.put("BIOFUEL", 3000);
        FUELS.put("OIL_BARREL", 10000);
        FUELS.put("VOLTA", 10000);
    }
    
    public GardenTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.gardenTweaks;
        Tweakception.overlayManager.addOverlay(milestoneOverlay = new MilestoneOverlay());
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
                
                for (int i = 0; i < chest.getSizeInventory(); i++)
                {
                    ItemStack stack = chest.getStackInSlot(i);
                    String[] lore = McUtils.getDisplayLore(stack);
                    if (lore != null && lore[lore.length - 1].equals("§eClick to claim reward!"))
                    {
                        sendChat("GardenTweaks-AutoClaimContest: claiming slot " + i);
                        getMc().playerController.windowClick(getPlayer().openContainer.windowId,
                            i, 2, 3, getPlayer());
                        getPlayer().closeScreen();
                        return;
                    }
                }
            }
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
        
        String name = itemStack.getDisplayName();
        if (name == null)
            return;
        
        boolean isCompost = name.equals("§eOrganic Matter");
        boolean isFuel = name.equals("§2Fuel");
        if (isCompost || isFuel)
        {
            for (int i = 0; i < tooltip.size(); i++)
            {
                if (composterAmountMatcher.reset(McUtils.cleanColor(tooltip.get(i))).find())
                {
                    float amount = Utils.parseFloat(composterAmountMatcher.group(1));
                    int limit = Integer.parseInt(composterAmountMatcher.group(2)) * 1000;
                    float needed = limit - amount;
                    if (isCompost)
                    {
                        float count = Utils.roundToDigits(needed / 25600, 1);
                        tooltip.add(i + 1, "§6 " + count + "x §9Box of Seeds §6needed");
                    }
                    else
                    {
                        float count = Utils.roundToDigits(needed / 10000, 1);
                        tooltip.add(i + 1, "§6 " + count + "x §9Volta §6needed");
                        float countBiofuel = Utils.roundToDigits(needed / 3000, 1);
                        tooltip.add(i + 1, "§6 " + countBiofuel + "x §9Biofuel §6needed");
                    }
                    return;
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
        IInventory chest;
        if (c.contestDataDumper & inContestsMenu)
        {
            AccessorGuiContainer accessor = (AccessorGuiContainer) event.gui;
            int xSize = accessor.getXSize();
            int guiLeft = accessor.getGuiLeft();
            int guiTop = accessor.getGuiTop();
            
            Color color = new Color(50, 50, 50);
            int x = guiLeft + xSize + 20;
            int y = guiTop;
            
            FontRenderer fr = getMc().fontRendererObj;
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
        }
        else if ((chest = getOpenedChest()) != null &&
            chest.getName().equals("Enchanted Agronomy Sack"))
        {
            AccessorGuiContainer accessor = (AccessorGuiContainer) event.gui;
            int guiLeft = accessor.getGuiLeft();
            int guiTop = accessor.getGuiTop();
            FontRenderer fr = getMc().fontRendererObj;
            
            List<Pair<String, String>> list = new ArrayList<>();
            
            for (int i = 0; i < chest.getSizeInventory(); i++)
            {
                ItemStack stack = chest.getStackInSlot(i);
                String[] lore = McUtils.getDisplayLore(stack);
                if (lore != null &&
                    stack.hasDisplayName() &&
                    !stack.getDisplayName().equals("§aEnchanted Bread") &&
                    !stack.getDisplayName().equals("§aEnchanted Cactus") &&
                    !stack.getDisplayName().equals("§aEnchanted Poisonous Potato"))
                {
                    for (String line : lore)
                    {
                        if (sackAmountMatcher.reset(McUtils.cleanColor(line)).matches())
                        {
                            int count = Utils.parseInt(sackAmountMatcher.group(1));
                            if (count < 2048)
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
    
    public void onWorldUnload(WorldEvent.Unload event)
    {
        invalidCrops.clear();
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
            sendChat("GT-VerifyCrops: selection must be a point, line, or a vertical plane!");
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
        
        List<Block> cropBlocks = new ArrayList<>(Arrays.asList(
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
            Blocks.nether_wart));
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
        sendChat("GT-VerifyCrops: found " + invalidCrops.size() + " invalid crops");
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
            if (milestoneText != null && getCurrentIsland() == SkyblockIsland.THE_GARDEN)
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
    
    // endregion Overlays
    
    // region Commands
    
    public void toggleSimulateCactusKnifeInstaBreak()
    {
        c.simulateCactusKnifeInstaBreak = !c.simulateCactusKnifeInstaBreak;
        sendChat("GardenTweaks-SimulateCactusKnifeInstaBreak: toggled " + c.simulateCactusKnifeInstaBreak);
    }
    
    public void toggleMilestoneOverlay()
    {
        boolean state = Tweakception.overlayManager.toggle(MilestoneOverlay.NAME);
        sendChat("GardenTweaks: toggled milestone overlay " + state);
    }
    
    public void toggleSnapYaw()
    {
        snapYaw = !snapYaw;
        sendChat("GardenTweaks-SnapYaw: toggled " + snapYaw);
    }
    public void setSnapYawAngle(int angle)
    {
        c.snapYawAngle = angle < 0 ? 45 : Utils.clamp(angle, 1, 180);
        sendChat("GardenTweaks-SnapYaw: set snap angle to " + c.snapYawAngle);
    }
    
    public void setSnapYawRange(int range)
    {
        c.snapYawRange = range < 0 ? 5 : Utils.clamp(range, 0, 90);
        sendChat("GardenTweaks-SnapYaw: set snap range to " + c.snapYawRange);
    }
    
    public void toggleSnapPitch()
    {
        snapPitch = !snapPitch;
        sendChat("GardenTweaks-SnapPitch: toggled " + snapPitch);
    }
    
    public void setSnapPitchAngle(int angle)
    {
        c.snapPitchAngle = angle < 0 ? 45 : Utils.clamp(angle, 1, 180);
        sendChat("GardenTweaks-SnapPitch: set snap angle to " + c.snapPitchAngle);
    }
    
    public void setSnapPitchRange(int range)
    {
        c.snapPitchRange = range < 0 ? 5 : Utils.clamp(range, 0, 90);
        sendChat("GardenTweaks-SnapPitch: set snap range to " + c.snapPitchRange);
    }
    
    public void toggleContestDataDumper()
    {
        c.contestDataDumper = !c.contestDataDumper;
        sendChat("GardenTweaks-ContestDataDumper: toggled " + c.contestDataDumper);
    }
    
    public void toggleContestDataDumperDumpHeader()
    {
        c.contestDataDumperDumpHeader = !c.contestDataDumperDumpHeader;
        sendChat("GardenTweaks-ContestDataDumper: toggled csv header " + c.contestDataDumperDumpHeader);
    }
    
    public void verifyCrops()
    {
        if (Tweakception.globalTweaks.isInAreaEditMode())
            verifyCrops(Tweakception.globalTweaks.getAreaEditBlockSelection());
        else
            sendChat("GardenTweaks-VerifyCrops: global tweaks AreaEdit feature is off");
    }
    
    public void verifyCropsClear()
    {
        invalidCrops.clear();
    }
    
    public void toggleAutoClaimContests()
    {
        c.autoClaimContest = !c.autoClaimContest;
        sendChat("GardenTweaks-AutoClaimContest: toggled " + c.autoClaimContest);
    }
    
    // endregion Commands
}
