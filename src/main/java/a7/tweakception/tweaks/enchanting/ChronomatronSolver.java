package a7.tweakception.tweaks.enchanting;

import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.EnchantingTweaks;
import a7.tweakception.utils.WindowClickContants;
import a7.tweakception.utils.timers.TicksStopwatch;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static a7.tweakception.utils.McUtils.*;

public class ChronomatronSolver extends EnchantingTweaks.Solver
{
    private final List<Integer> order = new ArrayList<>();
    private boolean showingOrder = false;
    private boolean nextColor = false;
    private int replayIndex = 0;
    
    public ChronomatronSolver(ContainerChest container)
    {
        super(container);
    }
    
    @Override
    public void update()
    {
        if (!showingOrder && replayIndex < order.size() && timer.checkAndResetIfElapsed())
        {
            for (int i = 9 + 2 - 1; i <= 9 * 4 + 8 - 1; i++)
            {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack == null) continue;
                Item item = stack.getItem();
                if (item == Item.getItemFromBlock(Blocks.stained_hardened_clay) ||
                    item == Item.getItemFromBlock(Blocks.stained_glass))
                {
                    if (stack.getItemDamage() == order.get(replayIndex))
                    {
                        printDebug("Clicking slot %d, %d, %s", i % 9, i / 9, item.getUnlocalizedName());
                        getMc().playerController.windowClick(container.windowId,
                            i,
                            WindowClickContants.Middle.BTN,
                            WindowClickContants.Middle.MODE, getPlayer());
                        replayIndex++;
                        break;
                    }
                }
            }
            if (replayIndex == order.size() && replayIndex == c.maxChronomatronRounds)
            {
                Tweakception.enchantingTweaks.sendChat("Closed Chronomatron at round " + c.maxChronomatronRounds);
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
                if (newShowingOrder)
                {
                    order.clear();
                    replayIndex = 0;
                }
                else
                    timer.reset();
                showingOrder = newShowingOrder;
                printDebug("showingOrder: " + showingOrder);
            }
        }
        else if (showingOrder && slot.getSlotIndex() >= 9 + 2 - 1 && slot.getSlotIndex() <= 9 * 4 + 8 - 1)
        {
            printDebug("Slot %d, %d to %s", slot.getSlotIndex() % 9, slot.getSlotIndex() / 9, item.getUnlocalizedName());
            if (item == Item.getItemFromBlock(Blocks.stained_hardened_clay) &&
                (nextColor || order.isEmpty()))
            {
                nextColor = false;
                order.add(stack.getItemDamage());
                printDebug("nextColor: " + nextColor);
            }
            else if (item == Item.getItemFromBlock(Blocks.stained_glass) &&
                !order.isEmpty() &&
                stack.getItemDamage() == order.get(order.size() - 1) &&
                !nextColor)
            {
                nextColor = true;
                printDebug("nextColor: " + nextColor);
            }
        }
    }
    
    @Override
    public void drawDebug(int xSize, int guiLeft, int guiTop)
    {
        FontRenderer fr = getMc().fontRendererObj;
        int x = guiLeft + xSize + 10;
        int y = guiTop;
        fr.drawString("showingOrder: " + showingOrder, x, y, 0xFFFFFFFF); y += fr.FONT_HEIGHT;
        fr.drawString("nextColor: " + nextColor, x, y, 0xFFFFFFFF); y += fr.FONT_HEIGHT;
        fr.drawString("replayIndex: " + replayIndex, x, y, 0xFFFFFFFF); y += fr.FONT_HEIGHT;
        fr.drawString("timer: " + timer.elapsedTime(), x, y, 0xFFFFFFFF); y += fr.FONT_HEIGHT;
        for (int i = 0; i < order.size(); i++)
        {
            fr.drawString((i+1) + ": " + order.get(i), x, y, 0xFFFFFFFF); y += fr.FONT_HEIGHT;
        }
    }
}
