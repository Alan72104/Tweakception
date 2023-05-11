package a7.tweakception;

import java.lang.reflect.Field;

import static a7.tweakception.utils.McUtils.sendChat;

public class DevSettings
{
    public static boolean printClicks = false;
    public static boolean printLocationChange = false;
    public static boolean printFishingState = false;
    
    // Toggles a boolean field by their name, case-insensitive
    public static void toggle(String name)
    {
        for (Field field : DevSettings.class.getDeclaredFields())
        {
            if (field.getType() == boolean.class &&
                field.getName().equalsIgnoreCase(name))
            {
                try
                {
                    boolean val = (boolean) field.get(null);
                    field.set(null, !val);
                    sendChat("TC-DevSettings: set " + field.getName() + " to " + !val);
                }
                catch (IllegalAccessException ignored)
                {
                }
                return;
            }
        }
        sendChat("TC-DevSettings: no boolean field of name \"" + name + "\"");
    }
}
