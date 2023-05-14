package a7.tweakception.commands;

import a7.tweakception.Tweakception;
import a7.tweakception.mixin.AccessorGuiPlayerTabOverlay;
import a7.tweakception.utils.Matchers;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static a7.tweakception.utils.McUtils.getMc;
import static a7.tweakception.utils.McUtils.sendChat;

public class Command implements Comparable<Command>
{
    protected final String name;
    @Nullable protected Consumer<String[]> func;
    @Nullable protected Function<String[], List<String>> optionProvider;
    protected final Set<Command> subCommands = new TreeSet<>();
    protected boolean visible = true; // Whether this command can be used or not, override by global tracker devMode
    
    public Command(String name,
                   @Nullable Consumer<String[]> func,
                   Command... subs)
    {
        this.name = name;
        this.func = func;
        for (Command sub : subs)
            addSub(sub);
    }
    
    public Command(String name,
                   @Nullable Consumer<String[]> func,
                   @Nonnull Function<String[],List<String>> optionProvider,
                   Command... subs)
    {
        this.name = name;
        this.func = func;
        this.optionProvider = optionProvider;
        for (Command sub : subs)
            addSub(sub);
    }
    
    public String getName()
    {
        return name;
    }
    
    public boolean isVisible()
    {
        return visible || Tweakception.globalTweaks.isInDevMode();
    }
    
    public void processCommands(String[] args)
    {
        if (args.length > 0)
            for (Command sub : subCommands)
                if (sub.isVisible() && args[0].equalsIgnoreCase(sub.getName()))
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
        List<String> customOptions = null;
        if (optionProvider != null)
            customOptions = optionProvider.apply(args);
        
        List<String> commandOptions = null;
        if (args.length == 1)
            commandOptions = getPossibleCompletions(args[0], getVisibleSubCommandNames());
        else if (args.length > 1)
            for (Command sub : subCommands)
                if (sub.isVisible() && args[0].equalsIgnoreCase(sub.getName()))
                {
                    commandOptions = sub.getTabCompletions(Arrays.copyOfRange(args, 1, args.length));
                    break;
                }
        
        if (commandOptions != null && customOptions != null)
        {
            commandOptions.addAll(customOptions);
            return commandOptions.stream()
                .distinct()
                .sorted()
                .filter(getPossibleCompletionPredicate(args[0]))
                .collect(Collectors.toList());
        }
        else if (commandOptions != null)
            return commandOptions;
        else if (customOptions != null)
            return customOptions.stream()
                .distinct()
                .sorted()
                .filter(getPossibleCompletionPredicate(args[0]))
                .collect(Collectors.toList());
        else
            return null;
    }
    
    protected Command setVisibility(boolean visible)
    {
        this.visible = visible;
        return this;
    }
    
    protected void addSub(Command cmd)
    {
        subCommands.add(cmd);
    }
    
    private List<String> getVisibleSubCommandNames()
    {
        return subCommands.stream()
            .filter(Command::isVisible)
            .map(Command::getName)
            .collect(Collectors.toList());
    }
    
    public int compareTo(Command that)
    {
        return this.name.toLowerCase(Locale.ROOT).compareTo(that.name.toLowerCase(Locale.ROOT));
    }
    
    protected void sendCommandNotFound()
    {
        sendChat("Tweakception: command not found or wrong syntax");
    }
    
    /**
     * Returns an option provider providing the real player names detected from {@link NetHandlerPlayClient#getPlayerInfoMap()}
     */
    public static Function<String[], List<String>> getPlayerNameProvider()
    {
        return args ->
            AccessorGuiPlayerTabOverlay.getTabListOrdering()
                .sortedCopy(getMc().getNetHandler().getPlayerInfoMap())
                .stream()
                .filter(info ->
                    info.getDisplayName() == null &&
                    info.getGameProfile() != null &&
                    Matchers.minecraftUsername.reset(info.getGameProfile().getName()).matches() &&
                    info.getResponseTime() == 1)
                .map(info -> info.getGameProfile().getName())
                .collect(Collectors.toList());
    }
    
    public static List<String> getPossibleCompletions(String arg, List<String> opts)
    {
        return opts.stream().filter(getPossibleCompletionPredicate(arg)).collect(Collectors.toList());
    }
    
    /**
     * Returns a predicate filtering string starting with {@code arg}, case-insensitive
     */
    public static Predicate<String> getPossibleCompletionPredicate(String arg)
    {
        return s -> s.toLowerCase(Locale.ROOT).startsWith(arg.toLowerCase(Locale.ROOT));
    }
}
