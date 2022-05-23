package a7.tweakception.commands;

import a7.tweakception.Tweakception;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static a7.tweakception.utils.McUtils.sendChat;

public class Command
{
    protected final String name;
    protected Consumer<String[]> func;
    protected final List<Command> subCommands = new ArrayList<Command>();
    protected boolean visibility = true; // Whether this command can be used or not, overriden by global tracker devMode

    public Command(String name, Consumer<String[]> func, Command... subs)
    {
        this.name = name;
        this.func = func;
        for (Command sub : subs)
            addSub(sub);
    }

    public String getName()
    {
        return name;
    }

    public boolean isVisible()
    {
        return visibility || Tweakception.globalTracker.isInDevMode();
    }

    public void processCommands(String[] args)
    {
        if (args.length > 0)
            for (Command sub : subCommands)
                if (sub.isVisible() && args[0].equals(sub.getName()))
                {
                    sub.processCommands(Arrays.copyOfRange(args, 1, args.length));
                    return;
                }
        if (func == null)
            sendCommandNotFound();
        else
            func.accept(args);
    }

    public List<String> getTabCompletions(String[] args)
    {
        if (args.length == 0)
            return null;
        else if (args.length == 1)
            return getPossibleCompletions(args[0], getVisibleSubCommandNames());
        else
            for (Command sub : subCommands)
                if (sub.isVisible() && args[0].equals(sub.getName()))
                    return sub.getTabCompletions(Arrays.copyOfRange(args, 1, args.length));
        return null;
    }

    protected Command setVisibility(boolean visibility)
    {
        this.visibility = visibility;
        return this;
    }

    protected void addSub(Command cmd)
    {
        subCommands.add(cmd);
    }

    private List<String> getVisibleSubCommandNames()
    {
        return subCommands.stream().
                filter(Command::isVisible).
                map(Command::getName).
                collect(Collectors.toList());
    }

    private static List<String> getPossibleCompletions(String arg, List<String> opts)
    {
        List<String> list = new ArrayList<String>();

        for (String opt : opts)
            if (opt.startsWith(arg))
                list.add(opt);

        return list;
    }

    protected void sendCommandNotFound()
    {
        sendChat("Tweakception: command not found or wrong syntax");
    }
}
