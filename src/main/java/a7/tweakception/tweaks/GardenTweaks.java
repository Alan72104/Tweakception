package a7.tweakception.tweaks;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.overlay.Anchor;
import a7.tweakception.overlay.TextOverlay;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Utils;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static a7.tweakception.tweaks.GlobalTweaks.getCurrentIsland;
import static a7.tweakception.utils.McUtils.getMc;
import static a7.tweakception.utils.McUtils.sendChat;
import static a7.tweakception.utils.Utils.f;

public class GardenTweaks extends Tweak
{
    public static class GardenTweaksConfig
    {
        public boolean displayVisitorOrderNeuPrice = false;
        public boolean simulateCactusKnifeInstaBreak = false;
    }
    private final GardenTweaksConfig c;
    private static boolean exceptionThrown = false;
    private boolean reflectionTried = false;
    private boolean reflectionSuccess = false;
    private Object neu = null;
    private Object neuManager = null;
    private Object auctionManager = null;
        private Method getBazaarInfoMethod = null;
    private final Matcher requiredItemMatcher = Pattern.compile("^ (.+) x(\\d+)$").matcher("");
    private final Matcher copperRewardMatcher = Pattern.compile("^ \\+(\\d+) Copper$").matcher("");
    private final MilestoneOverlay milestoneOverlay;
    
    public GardenTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.gardenTweaks;
        Tweakception.overlayManager.addOverlay(milestoneOverlay = new MilestoneOverlay());
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
    }
    
    public void onItemTooltip(ItemTooltipEvent event)
    {
        List<String> tooltip = event.toolTip;
        ItemStack itemStack = event.itemStack;
        
        if (tooltip == null || itemStack == null)
            return;
        
        if (getCurrentIsland() != SkyblockIsland.THE_GARDEN)
            return;
        
        if (!c.displayVisitorOrderNeuPrice)
            return;
        
        if (!reflectionTried)
            getNeuClass();
        
        if (!reflectionSuccess || exceptionThrown)
            return;
        
        if (getMc().currentScreen instanceof GuiChest &&
            ((ContainerChest) ((GuiChest) getMc().currentScreen)
                .inventorySlots)
                .getLowerChestInventory()
                .getSizeInventory() == 54 &&
            itemStack.getDisplayName().equals("§aAccept Offer"))
        {
            float totalBuyOrderPrice = 0.0f;
            for (int i = 0; i < tooltip.size(); i++)
            {
                String ogLine = tooltip.get(i);
                String line = McUtils.cleanColor(ogLine);
                if (requiredItemMatcher.reset(line).matches())
                {
                    String name = requiredItemMatcher.group(1);
                    int count = Integer.parseInt(requiredItemMatcher.group(2));
                    
                    JsonObject bazaarInfo = getBazaarInfoFromNeuId(
                        name.toUpperCase()
                        .replace(" ", "_")
                        .replace("'", "")
                        .replace("BALE", "BLOCK")
                        .replace("WART", "STALK")
                    );
                    
                    if (bazaarInfo == null)
                        continue;
                    
                    float buyOrderPrice = 0.0f;
                    if (bazaarInfo.has("curr_sell"))
                    {
                        buyOrderPrice = bazaarInfo.get("curr_sell").getAsFloat() * count;
                    }
                    
                    if (buyOrderPrice > 0.0f)
                    {
                        totalBuyOrderPrice += buyOrderPrice;
                        tooltip.set(i, ogLine + " §7(§e" + Utils.formatMetric((long)buyOrderPrice) + "§7)");
                    }
                }
                else if (copperRewardMatcher.reset(line).matches())
                {
                    int count = Integer.parseInt(copperRewardMatcher.group(1));
                    if (totalBuyOrderPrice > 0.0f)
                    {
                        float kCoinsPerCopper = totalBuyOrderPrice / count / 1000.0f;
                        tooltip.set(i, f("%s §7(§e%.2fk§7 per copper)", ogLine, kCoinsPerCopper));
                    }
                    break;
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
    
    public boolean isSimulateCactusKnifeInstaBreakOn()
    {
        return c.simulateCactusKnifeInstaBreak;
    }
    
    private void getNeuClass()
    {
        try
        {
            Class<?> neuClass = Class.forName("io.github.moulberry.notenoughupdates.NotEnoughUpdates");
            Field instanceField = neuClass.getDeclaredField("INSTANCE");
            neu = instanceField.get(null);
            Field managerField = neuClass.getDeclaredField("manager");
            neuManager = managerField.get(neu);
            Field auctionManagerField = neuManager.getClass().getDeclaredField("auctionManager");
            auctionManager = auctionManagerField.get(neuManager);
            getBazaarInfoMethod = auctionManager.getClass().getDeclaredMethod("getBazaarInfo", String.class);
        }
        catch (Exception e)
        {
            if (!exceptionThrown)
            {
                sendChat("GardenTweaks: " + e + ", stopping");
                exceptionThrown = true;
            }
        }
        reflectionSuccess = !exceptionThrown;
        reflectionTried = true;
    }
    
    private JsonObject getBazaarInfoFromNeuId(String id)
    {
        try
        {
            Object bazaarInfo = getBazaarInfoMethod.invoke(auctionManager, id);
            if (bazaarInfo != null)
                return (JsonObject) bazaarInfo;
        }
        catch (IllegalAccessException ignored)
        {
        }
        catch (InvocationTargetException e)
        {
            sendChat("GardenTweaks: " + e);
            exceptionThrown = true;
            reflectionSuccess = false;
        }
        return null;
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
            if (milestoneText != null)
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
    
    public void toggleDisplayVisitorOrderNeuPrice()
    {
        c.displayVisitorOrderNeuPrice = !c.displayVisitorOrderNeuPrice;
        sendChat("GardenTweaks-DisplayVisitorOrderNeuPrice: toggled " + c.displayVisitorOrderNeuPrice);
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
}
