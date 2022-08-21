package a7.tweakception.overlay;

import net.minecraft.client.gui.ScaledResolution;

import static a7.tweakception.utils.McUtils.getMc;

public class Overlay
{
    protected OverlayConfig config = new OverlayConfig();
    protected String name = "overlay";
    // Indicates content has changed, but layout and position hasn't been updated
    protected boolean changed = true;
    // Screen position of the top left corner of the overlay
    protected int drawX = 10;
    protected int drawY = 10;
    protected int width = 10;
    protected int height = 10;
    
    public Overlay(String name)
    {
        this.name = name;
    }
    
    // region Getter/setters
    
    public String getName()
    {
        return name;
    }
    
    public int getAnchor()
    {
        return config.anchor;
    }
    
    public void setAnchor(int a)
    {
        if (a == 0)
            throw new RuntimeException("Anchor value cannot be 0");
        setChanged();
        config.anchor = a;
    }
    
    public int getOrigin()
    {
        return config.origin;
    }
    
    public void setOrigin(int o)
    {
        if (o == 0)
            throw new RuntimeException("Origin value cannot be 0");
        setChanged();
        config.origin = o;
    }
    
    public int getX()
    {
        return config.x;
    }
    
    public void setX(int x)
    {
        setChanged();
        config.x = x;
    }
    
    public int getY()
    {
        return config.y;
    }
    
    public void setY(int y)
    {
        setChanged();
        config.y = y;
    }
    
    public int getDrawY()
    {
        updateState();
        return drawY;
    }
    
    public int getDrawX()
    {
        updateState();
        return drawX;
    }
    
    public int getWidth()
    {
        updateState();
        return width;
    }
    
    public int getHeight()
    {
        updateState();
        return height;
    }
    
    public int getTextAlignment()
    {
        return config.textAlignment;
    }
    
    public void setTextAlignment(int alignment)
    {
        if (!(alignment == 0 || alignment == 1))
            throw new RuntimeException("Text alignment cannot be " + alignment);
        setChanged();
        config.textAlignment = alignment;
    }
    
    public void setChanged()
    {
        changed = true;
    }
    
    public void unsetChanged()
    {
        changed = false;
    }
    
    public OverlayConfig getConfig()
    {
        return config;
    }
    
    public void setConfig(OverlayConfig c)
    {
        this.config = c;
    }
    
    // endregion getter/setters
    
    public void moveOrigin(int newOrigin)
    {
        ScaledResolution res = new ScaledResolution(getMc());
        
        int oldOrigin = getOrigin();
        if (newOrigin != oldOrigin)
        {
            int[] oldOriginPos = Anchor.apply(getX(), getY(), getWidth(), getHeight(), oldOrigin);
            int[] newOriginPos = Anchor.apply(getX(), getY(), getWidth(), getHeight(), newOrigin);
            setOrigin(newOrigin);
            setX(getX() + (newOriginPos[0] - oldOriginPos[0]));
            setY(getY() + (newOriginPos[1] - oldOriginPos[1]));
            setChanged();
        }
    }
    
    public void moveAnchor(int newAnchor)
    {
        ScaledResolution res = new ScaledResolution(getMc());
        
        int oldAnchor = getAnchor();
        if (newAnchor != oldAnchor)
        {
            int[] oldAnchorPos = Anchor.apply(0, 0, res.getScaledWidth(), res.getScaledHeight(), oldAnchor);
            int[] newAnchorPos = Anchor.apply(0, 0, res.getScaledWidth(), res.getScaledHeight(), newAnchor);
            setAnchor(newAnchor);
            setX(getX() - (newAnchorPos[0] - oldAnchorPos[0]));
            setY(getY() - (newAnchorPos[1] - oldAnchorPos[1]));
            setChanged();
        }
    }
    
    public int[] getOriginPos()
    {
        return Anchor.apply(getDrawX(), getDrawY(), getWidth(), getHeight(), getOrigin());
    }
    
    public int[] getAnchorPos()
    {
        ScaledResolution res = new ScaledResolution(getMc());
        return Anchor.apply(0, 0, res.getScaledWidth(), res.getScaledHeight(), getAnchor());
    }
    
    // Draws the overlay, subclasses that override this method should call super.draw()
    public void draw()
    {
        updateState();
    }
    
    // Updates the content of the overlay, subclasses that override this method should call super.update()
    public void update()
    {
        setChanged();
    }
    
    // Updates the locations of contents, subclasses should override this method to calculate at least width and height
    protected void updateLayout()
    {
    }
    
    // Updates the screen location of the overlay
    private void updateState()
    {
        if (changed)
        {
            unsetChanged();
            updateLayout();
            
            drawX = config.x;
            drawY = config.y;
            
            ScaledResolution res = new ScaledResolution(getMc());
            int screenWidth = res.getScaledWidth();
            int screenHeight = res.getScaledHeight();
            
            if ((config.anchor & Anchor.x1) != 0)
                drawX += screenWidth / 2;
            else if ((config.anchor & Anchor.x2) != 0)
                drawX += screenWidth;
            if ((config.anchor & Anchor.y1) != 0)
                drawY += screenHeight / 2;
            else if ((config.anchor & Anchor.y2) != 0)
                drawY += screenHeight;
            
            if ((config.origin & Anchor.x1) != 0)
                drawX -= width / 2;
            else if ((config.origin & Anchor.x2) != 0)
                drawX -= width;
            if ((config.origin & Anchor.y1) != 0)
                drawY -= height / 2;
            else if ((config.origin & Anchor.y2) != 0)
                drawY -= height;
        }
    }
}
