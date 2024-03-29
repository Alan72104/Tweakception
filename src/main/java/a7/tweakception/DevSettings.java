package a7.tweakception;

import java.lang.reflect.Field;

import static a7.tweakception.utils.McUtils.sendChat;

public class DevSettings
{
    public static boolean printClicks = false;
    public static boolean printLocationChange = false;
    public static boolean printSchedulerUpdate = false;
    public static boolean printLogCropBreaksNumber = false;
    public static boolean printSimMiningSpeedNums = false;
    public static boolean printArmorStandNameFilterCache = false;
    public static boolean printExpTableDebug = false;
    public static boolean printStringReplace = false;
    public static boolean copySpeedNums = false;
    
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
                    sendChat("§3[DevSettings] §rSet " + field.getName() + " to " + !val);
                }
                catch (IllegalAccessException ignored)
                {
                }
                return;
            }
        }
        sendChat("§3[DevSettings] §rNo boolean field of name \"" + name + "\"");
    }
}
