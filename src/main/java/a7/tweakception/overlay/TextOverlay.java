package a7.tweakception.overlay;

import a7.tweakception.utils.Utils;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

import static a7.tweakception.utils.McUtils.getMc;

public class TextOverlay extends Overlay
{
    private List<String> list = getDefaultContent();
    
    public TextOverlay(String name)
    {
        super(name);
    }
    
    protected void setContent(List<String> list)
    {
        this.list = list;
        changed = true;
    }
    
    protected List<String> getDefaultContent()
    {
        List<String> list = new ArrayList<>();
        list.add("default");
        list.add("overlay");
        return list;
    }
    
    @Override
    public void draw()
    {
        super.draw();
        
        FontRenderer r = getMc().fontRendererObj;
        int x = drawX;
        int y = drawY;
        for (String s : list)
        {
            if (config.textAlignment == 1)
                r.drawStringWithShadow(s, x + (width - r.getStringWidth(s)), y, 0xffffffff);
            else
                r.drawStringWithShadow(s, x, y, 0xffffffff);
            y += r.FONT_HEIGHT;
        }
    }
    
    @Override
    public void updateLayout()
    {
        width = Utils.getMaxStringWidth(list);
        height = getMc().fontRendererObj.FONT_HEIGHT * list.size();
        if (width == 0)
            width = 15;
        if (height == 0)
            height = 15;
    }
}
