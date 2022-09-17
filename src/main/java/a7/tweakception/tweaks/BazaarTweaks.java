package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static a7.tweakception.tweaks.GlobalTracker.getTicks;
import static a7.tweakception.utils.McUtils.*;

public class BazaarTweaks extends Tweak
{
    private final BazaarTweaksConfig c;
    public static class BazaarTweaksConfig
    {
    }
    private final Map<String, List<BazaarOrder>> sellOrders = new HashMap<>();
    private final Map<String, List<BazaarOrder>> buyOrders = new HashMap<>();
    // §7Offer amount: §a5§7x
    // §7Order amount: §a1,280§7x
    // §7Offer amount: §a1,609§7x
    private final Matcher orderAmountMatcher = Pattern.compile(
        "^§7(?:Offer|Order) amount: §a((?:\\d+,?)+)§7x$").matcher("");
    // §7Filled: §a1k§7/1.2k §8(79.5%)
    // §7Filled: §a1.2k§7/1.2k §a§l100%!
    private final Matcher orderStatusMatcher = Pattern.compile(
        "^§7Filled: §[\\da-f]((\\d+(?:\\.\\d+)?)(k?)§7/(\\d+(?:\\.\\d+)?)(k?)) (?:(§a§l100%!)|§8\\((\\d+(?:\\.\\d+)?%)\\))$")
        .matcher("");
    // §7Price per unit: §6343,998.5 coins
    private final Matcher orderPriceMatcher = Pattern.compile(
        "^§7Price per unit: §6((?:\\d+,?)+(?:\\.\\d+)?) coins$").matcher("");
    // §7§5§o§8- §6862.6 coins §7each | §a2,044§7x §7in §f2 §7orders
    // §7§5§o§8- §6868,484.2 coins §7each | §a1§7x §7from §f1 §7offer
    private final Matcher orderListMatcher = Pattern.compile(
        "^§5§o§8- §6((?:\\d,?)+(?:\\.\\d+)?) coins §7each \\| §a(?:\\d,?)+§7x §7(?:in|from) §f(?:\\d,?)+ §7(?:order|offer)s?$").matcher("");
    private boolean hadMenu = false;
    private int lastUpdateTicks = 0;
    private static class BazaarOrder
    {
        public double price;
        public String priceString;
        public int currentAmount;
        public int totalAmount;
        public String amountString;
        public BazaarOrder(double p, int c, int t, String a, String ps)
        { price = p; currentAmount = c; totalAmount = t; amountString = a; priceString = ps; }
    }
    
    public BazaarTweaks(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.bazaarTweaks;
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;
        
        if (getMc().currentScreen instanceof GuiChest)
        {
            GuiChest chest = (GuiChest)getMc().currentScreen;
            ContainerChest container = (ContainerChest)chest.inventorySlots;
            IInventory inv = container.getLowerChestInventory();
            if (inv.getName().endsWith("Bazaar Orders") &&
                (!hadMenu || getTicks() - lastUpdateTicks >= 5))
            {
                hadMenu = true;
                lastUpdateTicks = getTicks();
                
                // For some reason this place is triggered when entering the bz main menu?? and orders will be reset??
                // But this fixes it????????
                boolean hasItem = false;
                for (int j = 1; j < inv.getSizeInventory() / 9 - 1; j++)
                {
                    for (int i = 1; i < 8; i++)
                    {
                        ItemStack stack = inv.getStackInSlot(j * 9 + i);
                        if (stack != null)
                        {
                            hasItem = true;
                            break;
                        }
                    }
                }
                
                if (hasItem)
                {
                    sellOrders.clear();
                    buyOrders.clear();
                    for (int j = 1; j < inv.getSizeInventory() / 9 - 1; j++)
                    {
                        for (int i = 1; i < 8; i++)
                        {
                            ItemStack stack = inv.getStackInSlot(j * 9 + i);
                            if (stack != null)
                                detectItem(stack);
                        }
                    }
                }
            }
            else
                hadMenu = false;
        }
        else
            hadMenu = false;
        
        if (getTicks() - lastUpdateTicks >= 20 * 60)
        {
            sellOrders.clear();
            buyOrders.clear();
        }
    }
    
    public void onItemTooltip(ItemTooltipEvent event)
    {
        if (event.itemStack == null || event.toolTip == null) return;

        int type = -1;
        String itemName = event.itemStack.getDisplayName();
        if (itemName.equals("§6Create Sell Offer"))
            type = 0;
        else if (itemName.equals("§aCreate Buy Order"))
            type = 1;
        else
            return;

        String name = null;
        Map<String, List<BazaarOrder>> orders = type == 0 ? sellOrders : buyOrders;
        List<BazaarOrder> list = null;

        for (int i = 0; i < event.toolTip.size(); i++)
        {
            String s = event.toolTip.get(i);
            if (GlobalTracker.t)
                sendChat(s);
            if (s.startsWith("§5§o§8"))
            {
                if (name == null)
                {
                    name = s.substring("§5§o§8".length());
                    if (name.length() > 3 && orders.containsKey(name))
                        list = new ArrayList<>(orders.get(name)); // Copy the list to "sort" later
                    else
                        return;
                }
                else if (orderListMatcher.reset(s).matches())
                {
                    double price = Utils.parseDouble(orderListMatcher.group(1));

                    if (GlobalTracker.t)
                        sendChat(""+price);
                    if (price == 0.0)
                        continue;
                    
                    List<BazaarOrder> ordersForThisPrice = new ArrayList<>();
                    
                    for (int j = 0; j < list.size(); j++)
                    {
                        if (list.get(j).price == price)
                        {
                            // Remove the orders that the price is shown on the list (in gui) from the list,
                            // and add the rest to the last empty line later
                            ordersForThisPrice.add(list.remove(j));
                        }
                    }
                    
                    if (!ordersForThisPrice.isEmpty())
                    {
                        StringBuilder sb = new StringBuilder(s);
                        sb.append(" §a<<< ");
                        for (int j = 0; j < ordersForThisPrice.size(); j++)
                        {
                            sb.append("§8(").append(ordersForThisPrice.get(j).amountString).append("§8)");
                            if (j < ordersForThisPrice.size() - 1)
                                sb.append(' ');
                        }
                        event.toolTip.set(i, sb.toString());
                    }
                }
            }
        }
        
        if (name == null)
            return;
        
        int extraOrdersLineIndex = -1;
        for (int i = event.toolTip.size() - 1; i >= 0; i--)
        {
            String s = event.toolTip.get(i);
            if (s.equals("§5§o")) // Start at last empty line
            {
                extraOrdersLineIndex = i + 1;
                break;
            }
        }
        
        if (extraOrdersLineIndex == -1)
            extraOrdersLineIndex = event.toolTip.size();
    
        
        if (getTicks() - lastUpdateTicks >= 20 * 60)
        {
            if (!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
            {
                event.toolTip.add(extraOrdersLineIndex++, "§8Order cache expired");
                event.toolTip.add(extraOrdersLineIndex, "");
            }
        }
        else if (list.size() > 0)
        {
            // Rest items are added like
            // 1, 2
            // 3
            
            int count = 0;
            StringBuilder sb = new StringBuilder("§a>>> §r");
            for (int i = 0; i < list.size(); i++)
            {
                BazaarOrder order = list.get(i);
                
                if (count == 2)
                {
                    count = 0;
                    event.toolTip.add(extraOrdersLineIndex++, sb.toString());
                    sb.setLength(0);
                    sb.append("§a>>> §r");
                }
                
                sb.append(order.priceString);
                sb.append(" §8(").append(order.amountString).append("§8)§r");
                if (i % 2 == 0 && i < list.size() - 1) // First in the line and has more items
                    sb.append(", ");
                count++;
            }
            event.toolTip.add(extraOrdersLineIndex++, sb.toString());
            event.toolTip.add(extraOrdersLineIndex, "");
        }
    }
    
    private void detectItem(ItemStack stack)
    {
        String name = stack.getDisplayName();
        int type = -1;
        if (name.startsWith("§6§lSELL"))
            type = 0;
        else if (name.startsWith("§a§lBUY"))
            type = 1;
        else
            return;
        
        String[] split = name.split(" ", 2);
        if (split.length != 2)
            return;
        
        name = McUtils.cleanColor(split[1]);
        String amountString = null;
        double price = 0.0f;
        String priceString = null;
        int curAmount = 0;
        int totalAmount = 0;
        
        String[] lore = McUtils.getDisplayLore(stack);
        for (String s : lore)
        {
            if (orderAmountMatcher.reset(s).matches())
            {
                totalAmount = Utils.parseInt(orderAmountMatcher.group(1));
            }
            else if (orderStatusMatcher.reset(s).matches())
            {
                amountString = McUtils.cleanColor(orderStatusMatcher.group(1));
                
                float cur = Float.parseFloat(orderStatusMatcher.group(2));
                if (!orderStatusMatcher.group(3).isEmpty())
                    cur *= 1000.0f;
                float total = Float.parseFloat(orderStatusMatcher.group(4));
                if (!orderStatusMatcher.group(5).isEmpty())
                    total *= 1000.0f;
                curAmount = (int)cur;
                totalAmount = (int)total;
                
                String filled = orderStatusMatcher.group(6);
                if (filled != null && !filled.isEmpty())
                {
                    curAmount = totalAmount;
                    amountString = "§a§l100%!";
                }
            }
            else if (orderPriceMatcher.reset(s).matches())
            {
                priceString = orderPriceMatcher.group(1);
                price = Utils.parseDouble(priceString);
            }
        }
        
        if (GlobalTracker.t)
        {
            sendChatf("%d-%s %f$ %d/%d (%s)", type, name, price, curAmount, totalAmount, amountString);
        }
        
        // Total amount will always exist (§7Offer amount: §a5§7x),
        // and price too (§7Price per unit: §6343,998.5 coins)
        
        // Sanity check
        if (totalAmount == 0 || price == 0.0f)
            return;
        
        // But the order status may not exist on empty orders (§7Filled: §a242§7/1.2k §8(18.9%))
        if (amountString == null)
            amountString = "0/" + totalAmount;
        
        BazaarOrder newItem = new BazaarOrder(price, curAmount, totalAmount, amountString, priceString);
        Map<String, List<BazaarOrder>> orders = type == 0 ? sellOrders : buyOrders;
        if (orders.containsKey(name))
        {
            orders.get(name).add(newItem);
        }
        else
        {
            List<BazaarOrder> list = new ArrayList<>();
            list.add(newItem);
            orders.put(name, list);
        }
    }
    
    public void printOrders()
    {
        sendChat("Printing orders");
        for (Map.Entry<String, List<BazaarOrder>> entry : sellOrders.entrySet())
        {
            sendChat("Sell-" + entry.getKey());
            for (BazaarOrder order : entry.getValue())
                sendChat("    " + order.price + " - " + order.amountString);
        }
        for (Map.Entry<String, List<BazaarOrder>> entry : buyOrders.entrySet())
        {
            sendChat("Buy-" + entry.getKey());
            for (BazaarOrder order : entry.getValue())
                sendChat("    " + order.price + " - " + order.amountString);
        }
    }
}
