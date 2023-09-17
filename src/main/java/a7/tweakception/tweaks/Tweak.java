package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;
import a7.tweakception.utils.McUtils;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import javax.annotation.Nonnull;

public abstract class Tweak
{
    private final String chatName;
    protected Configuration configuration;
    
    protected Tweak(Configuration configuration)
    {
        this.configuration = configuration;
        this.chatName = "Tweakception";
    }
    
    protected Tweak(Configuration configuration, String chatName)
    {
        this.configuration = configuration;
        this.chatName = chatName;
    }
    
    /**
     * Saves the config
     * @return true if no exception, false otherwise
     */
    protected boolean saveConfig()
    {
        try
        {
            configuration.writeConfig();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    /**
     * Prefixed {@link McUtils#sendChat},
     * adds {@code "§3[chatName] "} before the component
     */
    public void sendChat(@Nonnull IChatComponent comp)
    {
        String prefix = "§3[" + chatName + "] ";
        McUtils.sendChat(new ChatComponentText(prefix).appendSibling(comp));
    }
    
    /**
     * Prefixed {@link McUtils#sendChat},
     * adds {@code "§3[chatName] §r"} to the string
     */
    public void sendChat(String s)
    {
        String prefix = "§3[" + chatName + "] §r";
        McUtils.sendChat(prefix + s);
    }
    
    /**
     * Prefixed {@link McUtils#sendChatf},
     * adds {@code "§3[chatName] §r"} to the string
     */
    public void sendChatf(String s, Object... args)
    {
        String prefix = "§3[" + chatName + "] §r";
        McUtils.sendChatf(prefix + s, args);
    }
}
