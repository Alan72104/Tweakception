package a7.tweakception.tweaks;

import a7.tweakception.config.Configuration;

public abstract class Tweak
{
    protected Configuration configuration = null;
    
    protected Tweak(Configuration configuration)
    {
        this.configuration = configuration;
    }
    
    protected void saveConfig() throws Exception
    {
        configuration.writeConfig();
    }
}
