package a7.tweakception.tweaks.enchanting;

import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.EnchantingTweaks;
import a7.tweakception.utils.WindowClickContants;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Map;
import java.util.TreeMap;

import static a7.tweakception.utils.McUtils.getMc;
import static a7.tweakception.utils.McUtils.getPlayer;

public class UltrasequencerSolver extends EnchantingTweaks.Solver
{
    // Index 1 based, slot index
    private final TreeMap<Integer, Integer> order = new TreeMap<>();
    private boolean showingOrder = false;
    private int replayIndex = 0;
    
    public UltrasequencerSolver(ContainerChest container)
    {
        super(container);
    }
    
    @Override
    public void update()
    {
        if (!showingOrder && replayIndex < order.size() && timer.checkAndResetIfElapsed())
        {
            Integer i = order.get(replayIndex + 1);
            if (i != null)
            {
                printDebug("Clicking slot %d", i);
                getMc().playerController.windowClick(container.windowId,
                    i,
                    WindowClickContants.Middle.BTN,
                    WindowClickContants.Middle.MODE, getPlayer());
                replayIndex++;
            }
            if (replayIndex == order.size() && replayIndex == c.maxUltrasequencerRounds)
            {
                Tweakception.enchantingTweaks.sendChat("Closed Ultrasequencer at round " + c.maxUltrasequencerRounds);
                getPlayer().closeScreen();
            }
        }
    }
    
    @Override
    public void onSlotSet(Container container, Slot slot, ItemStack stack)
    {
        Item item = stack.getItem();
        if (slot.getSlotIndex() == 9 * 5 + 5 - 1)
        {
            printDebug("Slot %d, %d to %s", slot.getSlotIndex() % 9, slot.getSlotIndex() / 9, item.getUnlocalizedName());
            boolean newShowingOrder = item == Item.getItemFromBlock(Blocks.glowstone);
            if (showingOrder != newShowingOrder)
            {
                // All items are updated at once, so content comes before the glowstone
                if (newShowingOrder)
                {
                    replayIndex = 0;
                    order.clear();
                    printDebug("Detecting all");
                    for (int i = 9; i <= 9*4+9-1; i++)
                    {
                        ItemStack stack1 = inv.getStackInSlot(i);
                        if (stack1.getItem() == Items.dye)
                        {
                            int count = stack1.stackSize;
                            order.put(count, i);
                        }
                    }
                    printDebug("Got " + order.size());
                }
                else
                    timer.reset();
                showingOrder = newShowingOrder;
                printDebug("showingOrder: " + showingOrder);
            }
        }
        else if (item == Items.dye)
        {
            int count = stack.stackSize;
            printDebug("count: %d, slot: %d", count, slot.getSlotIndex());
        }
    }
    
    @Override
    public void drawDebug(int xSize, int guiLeft, int guiTop)
    {
        FontRenderer fr = getMc().fontRendererObj;
        int x = guiLeft + xSize + 10;
        int y = guiTop;
        fr.drawString("showingOrder: " + showingOrder, x, y, 0xFFFFFFFF); y += fr.FONT_HEIGHT;
        fr.drawString("replayIndex: " + replayIndex, x, y, 0xFFFFFFFF); y += fr.FONT_HEIGHT;
        fr.drawString("timer: " + timer.elapsedTime(), x, y, 0xFFFFFFFF); y += fr.FONT_HEIGHT;
        for (Map.Entry<Integer, Integer> e : order.entrySet())
        {
            fr.drawString(e.getKey() + ": " + e.getValue(), x, y, 0xFFFFFFFF); y += fr.FONT_HEIGHT;
        }
    }
}
