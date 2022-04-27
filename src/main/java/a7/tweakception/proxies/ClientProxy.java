package a7.tweakception.proxies;

import a7.tweakception.commands.ClientCommandTweakception;
import net.minecraftforge.client.ClientCommandHandler;

public class ClientProxy implements IProxy
{
    public void registerClientCommands()
    {
        ClientCommandHandler.instance.registerCommand(new ClientCommandTweakception());
    }

    public void registerServerCommands()
    {
    }
}
