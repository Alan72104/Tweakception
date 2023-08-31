package a7.tweakception.overlay;

public class OverlayConfig
{
    public boolean enable = false;
    public int x = 0;
    public int y = 0;
    // The origin of the overlay
    public int origin = Anchor.TopLeft;
    // The relative screen position
    public int anchor = Anchor.TopLeft;
    // 0 for left to right, 1 for right to left
    public int textAlignment = 0;
}
