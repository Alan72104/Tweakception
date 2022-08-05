package a7.tweakception.overlay;

import a7.tweakception.Tweakception;
import a7.tweakception.config.Configuration;
import a7.tweakception.tweaks.Tweak;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.*;
import java.util.List;

import static a7.tweakception.utils.McUtils.getMc;

public class OverlayManager extends Tweak
{
    private final OverlayManagerConfig c;
    
    public static class OverlayManagerConfig
    {
        public TreeMap<String, OverlayConfig> configs = new TreeMap<>();
    }
    
    private List<Overlay> overlays = new ArrayList<>();
    
    public OverlayManager(Configuration configuration)
    {
        super(configuration);
        c = configuration.config.overlayManager;
    }
    
    public void onTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;
        
        for (Overlay overlay : overlays)
        {
            if (overlay.getConfig().enable)
                overlay.update();
        }
    }
    
    public void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event)
    {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL)
            return;
        
        for (Overlay overlay : overlays)
        {
            if (overlay.getConfig().enable)
                overlay.draw();
        }
    }
    
    public void addOverlay(TextOverlay overlay)
    {
        String name = overlay.getName();
        if (c.configs.containsKey(name))
        {
            OverlayConfig config = c.configs.get(name);
            config.textAlignment = overlay.getConfig().textAlignment;
            overlay.setConfig(config);
        }
        else
            c.configs.put(name, overlay.getConfig());
        overlays.add(overlay);
    }
    
    public void enable(String name)
    {
        if (c.configs.containsKey(name))
            c.configs.get(name).enable = true;
    }
    
    public void disable(String name)
    {
        if (c.configs.containsKey(name))
            c.configs.get(name).enable = false;
    }
    
    public void setEnable(String name, boolean enable)
    {
        if (c.configs.containsKey(name))
            c.configs.get(name).enable = enable;
    }
    
    public void editOverlays()
    {
        Tweakception.scheduler.add(() -> getMc().displayGuiScreen(new OverlayEditScreen()));
    }
    
    public class OverlayEditScreen extends GuiScreen
    {
        private Overlay selectedOverlay = null;
        private boolean draggedAnchor = false;
        private boolean draggedOrigin = false;
        private int[] originalOverlayPos = {0, 0};
        private int[] dragStartPos = {0, 0};
        // A quality addition that allows dragging the origin after mouse left the parent overlay which isn't selected,
        // in other words, keeps the overlay hovered when its origin is kept hovered
        private Overlay originHoveredOverlay = null;
        
        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) // a.k.a update()
        {
            drawDefaultBackground();
            boolean oneIsHovered = false;
            
            if (originHoveredOverlay != null)
            {
                int x = originHoveredOverlay.getDrawX();
                int y = originHoveredOverlay.getDrawY();
                int w = originHoveredOverlay.getWidth();
                int h = originHoveredOverlay.getHeight();
                int[] originPos = Anchor.apply(x, y, w, h, originHoveredOverlay.getOrigin());
                // Check it again
                boolean hovered = inBox(mouseX, mouseY, originPos[0] - 3, originPos[1] - 3, 6, 6);
                if (hovered)
                    oneIsHovered = true;
                else
                    originHoveredOverlay = null;
                
                if (originHoveredOverlay != null)
                    drawOverlayState(originHoveredOverlay, mouseX, mouseY);
            }
            
            for (Overlay overlay : overlays)
            {
                overlay.draw();
                int x = overlay.getDrawX();
                int y = overlay.getDrawY();
                int w = overlay.getWidth();
                int h = overlay.getHeight();
                if (!oneIsHovered)
                {
                    boolean hovered = inBox(mouseX, mouseY, x, y, w, h);
                    boolean displayState = hovered || selectedOverlay == overlay;
                    if (displayState)
                    {
                        oneIsHovered = true;
                        drawOverlayState(overlay, mouseX, mouseY);
                    }
                    
                    int[] originPos = Anchor.apply(x, y, w, h, overlay.getOrigin());
                    hovered = inBox(mouseX, mouseY, originPos[0] - 3, originPos[1] - 3, 6, 6);
                    if (hovered)
                        originHoveredOverlay = overlay;
                }
            }
        }
        
        @Override
        protected void mouseClicked(int mouseX, int mouseY, int btn)
        {
            if (btn != 0)
                return;
            
            if (originHoveredOverlay != null)
            {
                int x = originHoveredOverlay.getDrawX();
                int y = originHoveredOverlay.getDrawY();
                int w = originHoveredOverlay.getWidth();
                int h = originHoveredOverlay.getHeight();
                int[] originPos = Anchor.apply(x, y, w, h, originHoveredOverlay.getOrigin());
                // Check it again
                boolean hovered = inBox(mouseX, mouseY, originPos[0] - 3, originPos[1] - 3, 6, 6);
                if (hovered)
                {
                    draggedOrigin = true;
                    return;
                }
                else
                    originHoveredOverlay = null;
            }
            
            if (selectedOverlay != null)
            {
                draggedAnchor = false;
                draggedOrigin = false;
                int x = selectedOverlay.getDrawX();
                int y = selectedOverlay.getDrawY();
                int w = selectedOverlay.getWidth();
                int h = selectedOverlay.getHeight();
                int origin = selectedOverlay.getOrigin();
                int anchor = selectedOverlay.getAnchor();
                ScaledResolution res = new ScaledResolution(getMc());
                
                int[] anchorPos = Anchor.apply(0, 0, res.getScaledWidth(), res.getScaledHeight(), anchor);
                boolean hovered = inBox(mouseX, mouseY, anchorPos[0] - 4, anchorPos[1] - 4, 8, 8);
                if (hovered)
                {
                    draggedAnchor = true;
                    return;
                }
                
                int[] originPos = Anchor.apply(x, y, w, h, origin);
                hovered = inBox(mouseX, mouseY, originPos[0] - 3, originPos[1] - 3, 6, 6);
                if (hovered)
                {
                    draggedOrigin = true;
                    return;
                }
            }
            
            // Nothing was hovered or selected, find the selected overlay
            for (Overlay overlay : overlays)
            {
                int x = overlay.getDrawX();
                int y = overlay.getDrawY();
                int w = overlay.getWidth();
                int h = overlay.getHeight();
                boolean hovered = inBox(mouseX, mouseY, x, y, w, h);
                if (hovered)
                {
                    selectedOverlay = overlay;
                    originalOverlayPos = new int[]{overlay.getX(), overlay.getY()};
                    dragStartPos = new int[]{mouseX, mouseY};
                    return;
                }
            }
            
            selectedOverlay = null;
        }
        
        @Override
        protected void mouseClickMove(int mouseX, int mouseY, int btn, long timeSinceClick)
        {
            if (btn != 0)
                return;
            
            if (originHoveredOverlay != null)
            {
                originHoveredOverlay.moveOrigin(findClosestOrigin(mouseX, mouseY, originHoveredOverlay));
                return;
            }
            
            if (selectedOverlay != null)
            {
                if (draggedAnchor)
                    selectedOverlay.moveAnchor(findClosestAnchor(mouseX, mouseY));
                else if (draggedOrigin)
                    selectedOverlay.moveOrigin(findClosestOrigin(mouseX, mouseY, selectedOverlay));
                else // Dragged item is the overlay
                {
                    selectedOverlay.setX(originalOverlayPos[0] + mouseX - dragStartPos[0]);
                    selectedOverlay.setY(originalOverlayPos[1] + mouseY - dragStartPos[1]);
                    int oldAnchor = selectedOverlay.getAnchor();
                    selectedOverlay.moveAnchor(findClosestAnchor(mouseX, mouseY));
                    if (selectedOverlay.getAnchor() != oldAnchor)
                    {
                        originalOverlayPos = new int[]{selectedOverlay.getX(), selectedOverlay.getY()};
                        dragStartPos = new int[]{mouseX, mouseY};
                    }
                }
            }
        }
        
        @Override
        protected void mouseReleased(int mouseX, int mouseY, int btn)
        {
            draggedOrigin = false;
            draggedAnchor = false;
            originHoveredOverlay = null;
        }
        
        @Override
        public boolean doesGuiPauseGame()
        {
            return false;
        }
        
        private void drawOverlayState(Overlay overlay, int mouseX, int mouseY)
        {
            int x = overlay.getDrawX();
            int y = overlay.getDrawY();
            int w = overlay.getWidth();
            int h = overlay.getHeight();
            int origin = overlay.getOrigin();
            int anchor = overlay.getAnchor();
            ScaledResolution res = new ScaledResolution(getMc());
            
            Color overlayBgColor = new Color(255, 255, 255, 64);
            Color overlayBgColorHovered = new Color(255, 255, 255, 128);
            boolean hovered = inBox(mouseX, mouseY, x, y, w, h);
            drawRect(x, y, x + w, y + h, (hovered ? overlayBgColorHovered : overlayBgColor).getRGB());
            
            Color originColor = new Color(0, 255, 0, 128);
            Color originColorHovered = new Color(0, 255, 0, 192);
            int[] originPos = Anchor.apply(x, y, w, h, origin);
            hovered = inBox(mouseX, mouseY, originPos[0] - 3, originPos[1] - 3, 6, 6);
            drawRect(originPos[0] - 3, originPos[1] - 3, originPos[0] + 3, originPos[1] + 3,
                (hovered ? originColorHovered : originColor).getRGB());
            
            Color anchorColor = new Color(255, 0, 0, 128);
            Color anchorColorHovered = new Color(255, 0, 0, 192);
            int[] anchorPos = Anchor.apply(0, 0, res.getScaledWidth(), res.getScaledHeight(), anchor);
            hovered = inBox(mouseX, mouseY, anchorPos[0] - 4, anchorPos[1] - 4, 8, 8);
            drawRect(anchorPos[0] - 4, anchorPos[1] - 4, anchorPos[0] + 4, anchorPos[1] + 4,
                (hovered ? anchorColorHovered : anchorColor).getRGB());
            
        }
        
        private boolean inRange(int i, int a, int l)
        {
            return i >= a && i <= a + l;
        }
        
        private boolean inBox(int i, int j, int x, int y, int w, int h)
        {
            return inRange(i, x, w) && inRange(j, y, h);
        }
        
        private int findClosestOrigin(int mouseX, int mouseY, Overlay overlay)
        {
            return findClosestAnchor(mouseX, mouseY, overlay.getX(), overlay.getY(),
                overlay.getWidth(), overlay.getHeight());
        }
        
        private int findClosestAnchor(int mouseX, int mouseY)
        {
            ScaledResolution res = new ScaledResolution(getMc());
            return findClosestAnchor(mouseX, mouseY, 0, 0, res.getScaledWidth(), res.getScaledHeight());
        }
        
        private int findClosestAnchor(int mouseX, int mouseY, int x, int y, int width, int height)
        {
            int closest = 0;
            float closestDis = Float.MAX_VALUE;
            for (int anchor : Anchor.All)
            {
                int[] pos = Anchor.apply(x, y, width, height, anchor);
                float dis = (pos[0] - mouseX) * (pos[0] - mouseX) + (pos[1] - mouseY) * (pos[1] - mouseY);
                if (dis < closestDis)
                {
                    closestDis = dis;
                    closest = anchor;
                }
            }
            return closest;
        }
    }
}
