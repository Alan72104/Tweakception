package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.mixin.AccessorGuiContainer;
import a7.tweakception.overlay.Anchor;
import a7.tweakception.overlay.TextOverlay;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

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
        public boolean contestDataDumperDumpTitle = false;
    }
    private final GardenTweaksConfig c;
    private static final Map<String, Integer> FUELS = new HashMap<>();
    private final MilestoneOverlay milestoneOverlay;
    private boolean snapYaw = false;
    private float snapYawPrevAngle = 0.0f;
    private boolean snapPitch = false;
    private float snapPitchPrevAngle = 0.0f;
    private final Map<Instant, ContestInfo> contests = new TreeMap<>();
    private boolean inContestsMenu = false;
    private final Matcher composterAmountMatcher = Pattern.compile("((?:\\d{1,3},?)+(?:\\.\\d)?)/(\\d*)k").matcher("");
    
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
            if (getMc().currentScreen instanceof GuiChest)
            {
                GuiChest chest = (GuiChest) getMc().currentScreen;
                ContainerChest container = (ContainerChest) chest.inventorySlots;
                IInventory inv = container.getLowerChestInventory();
                if (inv.getName().equals("Your Contests") && inv.getSizeInventory() == 54)
                    inContestsMenu = true;
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
        
        if (getMc().currentScreen instanceof GuiChest &&
            ((ContainerChest) ((GuiChest) getMc().currentScreen).inventorySlots)
                .getLowerChestInventory().getName().startsWith("Auctions"))
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
                    if (isCompost)
                    {
                        int count = (int) ((limit - amount) / 25600);
                        tooltip.add(i + 1, "§6 " + count + "x §9Box of Seeds §6needed");
                    }
                    else
                    {
                        int count = (int) ((limit - amount) / 10000);
                        tooltip.add(i + 1, "§6 " + count + "x §9Volta §6needed");
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
        if (!c.contestDataDumper || !inContestsMenu)
            return;
        
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
    
    public boolean isSimulateCactusKnifeInstaBreakOn()
    {
        return c.simulateCactusKnifeInstaBreak;
    }
    
    private void getContests()
    {
        GuiChest chest = (GuiChest) getMc().currentScreen;
        ContainerChest container = (ContainerChest) chest.inventorySlots;
        IInventory inv = container.getLowerChestInventory();
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
                    year *        60L * 20 * 31 * 12 +
                    (month - 1) * 60L * 20 * 31 +
                    (day - 1) *   60L * 20;
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
                }
            }
            contests.put(contestInfo.date, contestInfo);
        }
    }
    
    private void dumpContests()
    {
        List<String> list = new ArrayList<>();
        if (c.contestDataDumperDumpTitle)
            list.add("datetime,millis,sb date,type,bronze,silver,gold,score");
        for (ContestInfo contestInfo : contests.values())
        {
            list.add(f("%s,%d,\"%s\",%s,%d,%d,%d,%d",
                contestInfo.date,
                contestInfo.date.toEpochMilli(),
                contestInfo.sbDate, // Has comma
                contestInfo.type,
                contestInfo.bronze,
                contestInfo.silver,
                contestInfo.gold,
                contestInfo.score
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
    
    private static class ContestInfo
    {
        public Instant date;
        public String sbDate;
        public String type;
        public int bronze;
        public int silver;
        public int gold;
        public int score;
    }
    
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
    
    public void toggleContestDataDumperDumpTitle()
    {
        c.contestDataDumperDumpTitle = !c.contestDataDumperDumpTitle;
        sendChat("GardenTweaks-ContestDataDumper: toggled title " + c.contestDataDumperDumpTitle);
    }
}
