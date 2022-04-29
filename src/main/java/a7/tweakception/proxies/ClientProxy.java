package a7.tweakception.proxies;

import a7.tweakception.Tweakception;
import a7.tweakception.commands.ClientCommandTweakception;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy implements IProxy
{
    public void registerClientCommands()
    {
        ClientCommandHandler.instance.registerCommand(new ClientCommandTweakception());
    }

    public void registerServerCommands()
    {
    }

    public void registerClientEventHandlers()
    {
        MinecraftForge.EVENT_BUS.register(Tweakception.inGameEventDispatcher);
    }

    public void registerServerEventHandlers()
    {
    }
}
