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
    // Whether this command can be seen in tab completion or not, global tracker devMode shows all cmd
    protected boolean visibility = true;

    public Command(String name)
    {
        this.name = name;
        this.func = null;
    }

    public Command(String name, Consumer<String[]> func, Command... subs)
    {
        this.name = name;
        this.func = func;
        for (Command sub : subs)
            addSub(sub);
    }

    protected void setFunc(Consumer<String[]> func)
    {
        this.func = func;
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

    public String getName()
    {
        return name;
    }

    public void processCommands(String[] args)
    {
        if (args.length > 0)
            for (Command sub : subCommands)
                if (args[0].equals(sub.getName()))
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
            return getVisibleSubCommandStrings();

        for (Command sub : subCommands)
            if (args[0].equals(sub.getName()))
                return sub.getTabCompletions(Arrays.copyOfRange(args, 1, args.length));

        return getPossibleCompletions(args[0], getVisibleSubCommandStrings());
    }

    public static List<String> getPossibleCompletions(String arg, List<String> opts)
    {
        List<String> list = new ArrayList<String>();

        for (String opt : opts)
            if (opt.startsWith(arg))
                list.add(opt);

        return list;
    }

    private List<String> getVisibleSubCommandStrings()
    {
        return subCommands.stream().
                filter(cmd -> cmd.visibility || Tweakception.globalTracker.isInDevMode()).
                map(Command::getName).
                collect(Collectors.toList());
    }

    protected static void sendCommandNotFound()
    {
        sendChat("Tweakception: command not found or wrong syntax");
    }
}
