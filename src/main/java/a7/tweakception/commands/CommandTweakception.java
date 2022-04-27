package a7.tweakception.commands;

import a7.tweakception.Tweakception;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

import java.util.*;

import static a7.tweakception.utils.McUtils.sendChat;

public class CommandTweakception extends CommandBase
{
    public ArrayList<String> subCommands = new ArrayList<String>();
    public ArrayList<String> fairySubCommands = new ArrayList<String>();
    public ArrayList<String> dungeonTweaksSubCommands = new ArrayList<String>();
    public ArrayList<String> crimsonSubCommands = new ArrayList<String>();
    public ArrayList<String> crimsonMapSubCommands = new ArrayList<String>();
    public ArrayList<String> globalTrackerSubCommands = new ArrayList<String>();

    public CommandTweakception()
    {
        subCommands.add("fairy");
            fairySubCommands.add("toggle");
            fairySubCommands.add("toggleauto");
            fairySubCommands.add("setdelay");
            fairySubCommands.add("setnotfound");
            fairySubCommands.add("count");
            fairySubCommands.add("list");
            fairySubCommands.add("dump");
            fairySubCommands.add("import");
            fairySubCommands.add("reset");
        subCommands.add("dungeon");
            dungeonTweaksSubCommands.add("nofog");
            dungeonTweaksSubCommands.add("hidename");
        subCommands.add("crimson");
            crimsonSubCommands.add("map");
                crimsonMapSubCommands.add("pos");
                crimsonMapSubCommands.add("scale");
                crimsonMapSubCommands.add("markerscale");
        subCommands.add("gt");
            globalTrackerSubCommands.add("island");
            globalTrackerSubCommands.add("forcesetisland");
            globalTrackerSubCommands.add("copylocation");
    }

    @Override
    public String getCommandName()
    {
        return "tc";
    }

    @Override
    public String getCommandUsage(ICommandSender icommandsender)
    {
        return "/" + this.getCommandName() + " help";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 4;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos)
    {
        if (args.length == 0)
            return null;
        else if (args.length == 1)
            return getListOfStringsMatchingLastWord(args, subCommands);
        else
        {
            if (args[0].equals("fairy"))
            {
                if (args.length == 2)
                    return getListOfStringsMatchingLastWord(args, fairySubCommands);
            }
            else if (args[0].equals("dungeon"))
            {
                if (args.length == 2)
                    return getListOfStringsMatchingLastWord(args, dungeonTweaksSubCommands);
            }
            else if (args[0].equals("crimson"))
            {
                if (args.length == 2)
                    return getListOfStringsMatchingLastWord(args, crimsonSubCommands);
                else if (args.length == 3)
                {
                    if (args[1].equals("map"))
                        return getListOfStringsMatchingLastWord(args, crimsonMapSubCommands);
                }
            }
            else if (args[0].equals("gt"))
            {
                if (args.length == 2)
                    return getListOfStringsMatchingLastWord(args, globalTrackerSubCommands);
            }
        }
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length == 0) return;

        if (args[0].equals("fairy"))
        {
            if (args.length == 1)
                Tweakception.fairyTracker.trackOnce();
            else if (args.length >= 2)
            {
                if (args[1].equals("toggle"))
                    Tweakception.fairyTracker.toggle();
                else if (args[1].equals("toggleauto"))
                    Tweakception.fairyTracker.toggleAutoTracking();
                else if (args[1].equals("setdelay"))
                {
                    if (args.length >= 3)
                        Tweakception.fairyTracker.setDelay(Integer.parseInt(args[2]));
                }
                else if (args[1].equals("setnotfound"))
                    Tweakception.fairyTracker.setNotFound();
                else if (args[1].equals("count"))
                    Tweakception.fairyTracker.count();
                else if (args[1].equals("list"))
                    Tweakception.fairyTracker.list();
                else if (args[1].equals("dump"))
                    Tweakception.fairyTracker.dump();
                else if (args[1].equals("import"))
                    Tweakception.fairyTracker.load();
                else if (args[1].equals("reset"))
                    Tweakception.fairyTracker.reset();
                else
                    sendCommandNotFound();
            }
            else
                sendCommandNotFound();
        }
        else if (args[0].equals("dungeon"))
        {
            if (args.length >= 2)
            {
                if (args[1].equals("nofog"))
                {
                    if (args.length == 2)
                        Tweakception.dungeonTweaks.toggleNoFog();
                    else if (args.length >= 3)
                        if (args[2].equals("auto"))
                            Tweakception.dungeonTweaks.toggleNoFogAutoToggle();
                }
                else if (args[1].equals("hidename"))
                    Tweakception.dungeonTweaks.toggleHideName();
                else
                    sendCommandNotFound();
            }
            else
                sendCommandNotFound();
        }
        else if (args[0].equals("crimsonmap"))
        {
            if (args.length == 1)
                Tweakception.crimsonTweaks.toggle();
            else if (args.length >= 2)
            {
                if (args[1].equals("pos"))
                {
                    if (args.length == 4)
                        Tweakception.crimsonTweaks.setPos(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                }
                else if (args[1].equals("scale"))
                {
                    if (args.length == 3)
                        Tweakception.crimsonTweaks.setMapScale(Float.parseFloat(args[2]));
                }
                else if (args[1].equals("markerscale"))
                {
                    if (args.length == 3)
                        Tweakception.crimsonTweaks.setMapMarkerScale(Float.parseFloat(args[2]));
                }
                else
                    sendCommandNotFound();
            }
            else
                sendCommandNotFound();
        }
        else if (args[0].equals("gt"))
        {
            if (args.length >= 2)
            {
                if (args[1].equals("island"))
                    Tweakception.globalTracker.printIsland();
                else if (args[1].equals("forcesetisland"))
                {
                    if (args.length >= 3)
                        Tweakception.globalTracker.forceSetIsland(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                    else
                        Tweakception.globalTracker.forceSetIsland("");
                }
                else if (args[1].equals("copylocation"))
                    Tweakception.globalTracker.copyLocation();
                else
                    sendCommandNotFound();
            }
            else
                sendCommandNotFound();
        }
        else if (args[0].equals("t"))
        {
            sendChat("executed t");
            Tweakception.dungeonTweaks.t = true;
        }
        else
            sendCommandNotFound();
    }

    private void sendCommandNotFound()
    {
        sendChat("Tweakception: command not found");
    }
}
