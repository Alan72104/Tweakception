package a7.tweakception.commands;

public class ClientCommandTweakception extends CommandTweakception
{
    @Override
    public String getCommandName()
    {
        return "ctc";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }
}
