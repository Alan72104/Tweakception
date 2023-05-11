package a7.tweakception.utils;

public class StringBuilderCache
{
    // Circular buffer so nesting get() is supported
    private static final StringBuilder[] sbs;
    private static int index = 0;
    
    static
    {
        sbs = new StringBuilder[5];
        for (int i = 0; i < 5; i++)
            sbs[i] = new StringBuilder();
    }
    
    /**
     * Gets the global {@link StringBuilder} instance after resetting the position
     */
    public static StringBuilder get()
    {
        StringBuilder sb = sbs[index];
        sb.setLength(0);
        index++;
        if (index == 5)
            index = 0;
        return sb;
    }
}
