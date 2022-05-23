package a7.tweakception.proxies;

import a7.tweakception.Tweakception;
import a7.tweakception.commands.TweakceptionCommand;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy implements IProxy
{
    public void registerClientCommands()
    {
        ClientCommandHandler.instance.registerCommand(new TweakceptionCommand());
    }

    public void registerServerCommands()
    {
    }

    public void registerClientEventHandlers()
    {
        MinecraftForge.EVENT_BUS.register(Tweakception.inGameEventDispatcher);
        MinecraftForge.EVENT_BUS.register(Tweakception.scheduler);
    }

    public void registerServerEventHandlers()
    {
    }
}
