package a7.tweakception.utils;

public class StringBuilderCache
{
    private static final StringBuilder sb = new StringBuilder();
    
    /**
     * Gets the global {@link StringBuilder} instance after resetting the position
     */
    public static StringBuilder get()
    {
        sb.setLength(0);
        return sb;
    }
}
