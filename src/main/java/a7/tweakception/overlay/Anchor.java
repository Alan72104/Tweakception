package a7.tweakception.overlay;

public class Anchor
{
    public static final int x0 = 1;
    public static final int x1 = 1 << 1;
    public static final int x2 = 1 << 2;
    public static final int y0 = 1 << 3;
    public static final int y1 = 1 << 4;
    public static final int y2 = 1 << 5;
    public static final int TopLeft = x0 | y0;
    public static final int TopCenter = x1 | y0;
    public static final int TopRight = x2 | y0;
    public static final int CenterLeft = x0 | y1;
    public static final int Center = x1 | y1;
    public static final int CenterRight = x2 | y1;
    public static final int BottomLeft = x0 | y2;
    public static final int BottomCenter = x1 | y2;
    public static final int BottomRight = x2 | y2;
    public static final int[] All =
        {TopLeft, TopCenter, TopRight, CenterLeft, Center, CenterRight, BottomLeft, BottomCenter, BottomRight};
    
    public static int[] apply(int x, int y, int width, int height, int anchor)
    {
        if ((anchor & Anchor.x1) != 0)
            x += width / 2;
        else if ((anchor & Anchor.x2) != 0)
            x += width;
        if ((anchor & Anchor.y1) != 0)
            y += height / 2;
        else if ((anchor & Anchor.y2) != 0)
            y += height;
        return new int[]{x, y};
    }
}
