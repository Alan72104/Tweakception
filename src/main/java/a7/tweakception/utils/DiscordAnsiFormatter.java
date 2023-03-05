package a7.tweakception.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DiscordAnsiFormatter
{
    private static final Map<Character, Integer> MC_TO_ANSI_MAP = new HashMap<>();
    private static final char ANSI_PREFIX = 0x1B;
    private static final Random rand = new Random();
    private static final String lineSeparator = System.lineSeparator();
    
//    public static String optimizeColorCodes(String s)
//    {
//        return transform(false, s);
//    }
    
    public static String colorCodesToAnsi(String s)
    {
        return transformToAnsi(s);
    }
    
    public static String normalizeNewline(String s)
    {
        return s.replaceAll("\\r?\\n", lineSeparator);
    }
    
    private static String transformToAnsi(String s)
    {
        s = normalizeNewline(s);
        StringBuilder sb = new StringBuilder();
        Format cur = new Format();
        Format target = new Format();
        for (String line : s.split("\\R"))
        {
            boolean colorCode = false;
            boolean randomizing = false;
            for (char c : line.toCharArray())
            {
                if (c == '§' || c == '&')
                {
                    if (!colorCode)
                        colorCode = true;
                    else // The previous char is the prefix
                    {
                        flushFormat(sb, cur, target);
                        sb.append(randomizing ? getRandomChar() : '§');
                    }
                }
                else if (colorCode && MC_TO_ANSI_MAP.containsKey(c))
                {
                    colorCode = false;
                    int code = MC_TO_ANSI_MAP.get(c);
                    // Special colors
                    if (c == 'r')
                    {
                        randomizing = false;
                        target.format = F.NORMAL;
                        target.color = F.NORMAL;
                        target.background = F.NORMAL;
                    }
                    else if (c == 'z' || c == 'Z')
                    {
                        target.color = C.YELLOW;
                        target.background = B.DARKBLUE;
                    }
                    else if (isFormat(code))
                    {
                        target.format = code;
                    }
                    else if (isColor(code))
                    {
                        target.format = F.NORMAL;
                        target.color = code;
                        target.background = F.NORMAL;
                    }
                    else if (isBg(code))
                    {
                        target.background = code;
                    }
                }
                else if (colorCode && (c == 'o' || c == 'm'))
                {
                    colorCode = false;
                    target.format = F.NORMAL;
                }
                else if (colorCode && c == 'k')
                {
                    colorCode = false;
                    randomizing = true;
                }
                else
                {
                    colorCode = false;
                    if (!Character.isWhitespace(c))
                        flushFormat(sb, cur, target);
                    sb.append(randomizing ? getRandomChar() : c);
                }
            } // For each chars
            sb.append(lineSeparator);
            target.format = F.NORMAL;
            target.color = F.NORMAL;
            target.background = F.NORMAL;
        } // For each lines
        
        return "```ansi" + lineSeparator +
            sb.toString().trim() + ANSI_PREFIX + '[' + MC_TO_ANSI_MAP.get('r') + 'm' + lineSeparator;
    }
    
    // Appends only the required format codes to the string
    private static void flushFormat(StringBuilder sb, Format cur, Format target)
    {
        // Whether to reset format, color, or background
        boolean requireReformat = false;
        StringBuilder params = new StringBuilder();
        if (cur.format != target.format)
        {
            if (cur.format == 0)
                requireReformat = true;
            else
                params.append(cur.format).append(';');
            cur.format = target.format;
        }
        if (cur.color != target.color)
        {
            if (cur.color == 0)
                requireReformat = true;
            else
                params.append(cur.color).append(';');
            cur.color = target.color;
        }
        if (cur.background != target.background)
        {
            if (cur.background == 0)
                requireReformat = true;
            else
                params.append(cur.background).append(';');
            cur.background = target.background;
        }
        if (requireReformat)
        {
            params.setLength(0);
            params.append(MC_TO_ANSI_MAP.get('r')).append(';');
            // Add them back to account for unchanged formats, when some other format needs to be reset
            if (target.format != 0)
                params.append(target.format).append(';');
            if (target.color != 0)
                params.append(target.color).append(';');
            if (target.background != 0)
                params.append(target.background).append(';');
        }
        if (params.length() > 0)
        {
            params.deleteCharAt(params.length() - 1);
            sb.append(ANSI_PREFIX).append('[').append(params).append('m');
        }
    }
    
    private static char getRandomChar()
    {
        String chars = "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ!\\\"\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\\u0000ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■\"";
        return chars.charAt(rand.nextInt(chars.length()));
    }
    
    // Returns whether the ansi code is for format
    private static boolean isFormat(int code)
    {
        return code == 0 || code == 1 || code == 4;
    }
    
    // Returns whether the ansi code is for color
    private static boolean isColor(int code)
    {
        return code >= 30 && code <= 37;
    }
    
    // Returns whether the ansi code is for background
    private static boolean isBg(int code)
    {
        return code >= 40 && code <= 47;
    }
    
    // Stores the current ansi color format
    private static class Format
    {
        private int format = F.NORMAL;
        private int color = F.NORMAL;
        private int background = F.NORMAL;
    }
    
    static
    {
        MC_TO_ANSI_MAP.put('0', C.WHITE);
        MC_TO_ANSI_MAP.put('1', C.BLUE);
        MC_TO_ANSI_MAP.put('2', C.GREEN);
        MC_TO_ANSI_MAP.put('3', C.CYAN);
        MC_TO_ANSI_MAP.put('4', C.RED);
        MC_TO_ANSI_MAP.put('5', C.PINK);
        MC_TO_ANSI_MAP.put('6', C.YELLOW);
        MC_TO_ANSI_MAP.put('7', C.WHITE);
        MC_TO_ANSI_MAP.put('8', C.GRAY);
        MC_TO_ANSI_MAP.put('9', C.BLUE);
        MC_TO_ANSI_MAP.put('a', C.GREEN);
        MC_TO_ANSI_MAP.put('b', C.BLUE);
        MC_TO_ANSI_MAP.put('c', C.RED);
        MC_TO_ANSI_MAP.put('d', C.PINK);
        MC_TO_ANSI_MAP.put('e', C.YELLOW);
        MC_TO_ANSI_MAP.put('f', C.WHITE);
        MC_TO_ANSI_MAP.put('l', F.BOLD);
        MC_TO_ANSI_MAP.put('n', F.UNDERLINE);
        MC_TO_ANSI_MAP.put('r', F.NORMAL);
        MC_TO_ANSI_MAP.put('z', C.WHITE);
        MC_TO_ANSI_MAP.put('Z', C.WHITE);
    }
    
    private static class F // Ansi format code
    {
        private static final int NORMAL    = 0;
        private static final int BOLD      = 1;
        private static final int UNDERLINE = 4;
    }
    
    private static class C // Ansi color code
    {
        private static final int NORMAL = 0;
        private static final int GRAY   = 30;
        private static final int RED    = 31;
        private static final int GREEN  = 32;
        private static final int YELLOW = 33;
        private static final int BLUE   = 34;
        private static final int PINK   = 35;
        private static final int CYAN   = 36;
        private static final int WHITE  = 37;
    }
    
    private static class B // Ansi background code
    {
        private static final int NORMAL   = 0;
        private static final int DARKBLUE = 40;
        private static final int ORANGE   = 41;
        private static final int BLUE     = 42;
        private static final int GRAY3    = 43;
        private static final int GRAY2    = 44;
        private static final int GRAY1    = 45;
        private static final int INDIGO   = 46;
        private static final int WHITE    = 47;
    }
}
