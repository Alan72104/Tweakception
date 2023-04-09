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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    }
    private final GardenTweaksConfig c;
    private static final Map<String, Integer> FUELS = new HashMap<>();
    private final MilestoneOverlay milestoneOverlay;
    private boolean snapYaw = false;
    private float snapYawPrevAngle = 0.0f;
    private boolean snapPitch = false;
    private float snapPitchPrevAngle = 0.0f;
    
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
}
