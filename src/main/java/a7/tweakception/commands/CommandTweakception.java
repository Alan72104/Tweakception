package a7.tweakception.commands;

import a7.tweakception.Tweakception;
import a7.tweakception.utils.DataDumps;
import a7.tweakception.utils.McUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

import java.util.*;

import static a7.tweakception.utils.McUtils.*;

public class CommandTweakception extends CommandBase
{
    public ArrayList<String> subCommands = new ArrayList<String>();
    public ArrayList<String> fairySubCommands = new ArrayList<String>();
    public ArrayList<String> dungeonTweaksSubCommands = new ArrayList<String>();
    public ArrayList<String> blockRightClickSubCommands = new ArrayList<String>();
    public ArrayList<String> crimsonSubCommands = new ArrayList<String>();
    public ArrayList<String> crimsonMapSubCommands = new ArrayList<String>();
    public ArrayList<String> globalTrackerSubCommands = new ArrayList<String>();

    public CommandTweakception()
    {
        subCommands.add("fairy");
            fairySubCommands.add("trackonce");
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
            dungeonTweaksSubCommands.add("highlightstarredmobs");
            dungeonTweaksSubCommands.add("blockrightclick");
                blockRightClickSubCommands.add("set");
                blockRightClickSubCommands.add("list");
        subCommands.add("crimson");
            crimsonSubCommands.add("map");
                crimsonMapSubCommands.add("pos");
                crimsonMapSubCommands.add("scale");
                crimsonMapSubCommands.add("markerscale");
            crimsonSubCommands.add("sulfur");
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
            switch (args[0])
            {
                case "fairy":
                    if (args.length == 2)
                        return getListOfStringsMatchingLastWord(args, fairySubCommands);
                    break;
                case "dungeon":
                    if (args.length == 2)
                        return getListOfStringsMatchingLastWord(args, dungeonTweaksSubCommands);
                    else if (args.length == 3)
                    {
                        switch (args[1])
                        {
                            case "blockrightclick":
                                return getListOfStringsMatchingLastWord(args, blockRightClickSubCommands);
                        }
                    }
                    break;
                case "crimson":
                    if (args.length == 2)
                        return getListOfStringsMatchingLastWord(args, crimsonSubCommands);
                    else if (args.length == 3)
                    {
                        switch (args[1])
                        {
                            case "map":
                                return getListOfStringsMatchingLastWord(args, crimsonMapSubCommands);
                        }
                    }
                    break;
                case "gt":
                    if (args.length == 2)
                        return getListOfStringsMatchingLastWord(args, globalTrackerSubCommands);
                    break;
            }
        }
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length == 0) return;

        switch (args[0])
        {
            case "fairy":
                if (args.length == 1)
                    Tweakception.fairyTracker.toggle();
                else
                {
                    switch (args[1])
                    {
                        case "trackonce":
                            Tweakception.fairyTracker.trackOnce();
                            break;
                        case "toggleauto":
                            Tweakception.fairyTracker.toggleAutoTracking();
                            break;
                        case "setdelay":
                            if (args.length >= 3)
                                Tweakception.fairyTracker.setDelay(Integer.parseInt(args[2]));
                            break;
                        case "setnotfound":
                            Tweakception.fairyTracker.setNotFound();
                            break;
                        case "count":
                            Tweakception.fairyTracker.count();
                            break;
                        case "list":
                            Tweakception.fairyTracker.list();
                            break;
                        case "dump":
                            Tweakception.fairyTracker.dump();
                            break;
                        case "import":
                            Tweakception.fairyTracker.load();
                            break;
                        case "reset":
                            Tweakception.fairyTracker.reset();
                            break;
                        default:
                            sendCommandNotFound();
                            break;
                    }
                }
                break;
            case "dungeon":
                if (args.length >= 2)
                    switch (args[1])
                    {
                        case "nofog":
                            if (args.length == 2)
                                Tweakception.dungeonTweaks.toggleNoFog();
                            else
                            {
                                if (args[2].equals("auto"))
                                    Tweakception.dungeonTweaks.toggleNoFogAutoToggle();
                            }
                            break;
                        case "hidename":
                            Tweakception.dungeonTweaks.toggleHideName();
                            break;
                        case "highlightstarredmobs":
                            Tweakception.dungeonTweaks.toggleHighlightStarredMobs();
                            break;
                        case "blockrightclick":
                            if (args.length >= 3)
                            {
                                switch (args[2])
                                {
                                    case "set":
                                        Tweakception.dungeonTweaks.blockRightClickSet();
                                        break;
                                    case "list":
                                        Tweakception.dungeonTweaks.blockRightClickList();
                                        break;
                                    default:
                                        sendCommandNotFound();
                                        break;
                                }
                            }
                            break;
                        default:
                            sendCommandNotFound();
                            break;
                    }
                else
                    sendCommandNotFound();
                break;
            case "crimson":
                if (args.length >= 2)
                {
                    switch (args[1])
                    {
                        case "map":
                            if (args.length == 2)
                                Tweakception.crimsonTweaks.toggleMap();
                            else if (args.length >= 3)
                                switch (args[2])
                                {
                                    case "pos":
                                        if (args.length == 5)
                                            Tweakception.crimsonTweaks.setMapPos(Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                                        else
                                            sendCommandNotFound();
                                        break;
                                    case "scale":
                                        if (args.length == 4)
                                            Tweakception.crimsonTweaks.setMapScale(Float.parseFloat(args[3]));
                                        else
                                            sendCommandNotFound();
                                        break;
                                    case "markerscale":
                                        if (args.length == 4)
                                            Tweakception.crimsonTweaks.setMapMarkerScale(Float.parseFloat(args[3]));
                                        else
                                            sendCommandNotFound();
                                        break;
                                    default:
                                        sendCommandNotFound();
                                        break;
                                }
                            else
                                sendCommandNotFound();
                            break;
                        case "sulfur":
                            Tweakception.crimsonTweaks.toggleSponge();
                            break;
                        default:
                            sendCommandNotFound();
                            break;
                    }
                } else
                    sendCommandNotFound();
                break;
            case "gt":
                if (args.length >= 2)
                {
                    switch (args[1])
                    {
                        case "island":
                            Tweakception.globalTracker.printIsland();
                            break;
                        case "forcesetisland":
                            if (args.length >= 3)
                                Tweakception.globalTracker.forceSetIsland(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                            else
                                Tweakception.globalTracker.forceSetIsland("");
                            break;
                        case "copylocation":
                            Tweakception.globalTracker.copyLocation();
                            break;
                        case "useFallbackDetection":
                            Tweakception.globalTracker.toggleFallbackDetection();
                            break;
                        default:
                            sendCommandNotFound();
                            break;
                    }
                }
                else
                    sendCommandNotFound();
                break;
            case "looktrace":
                // /ctc looktrace reach adjacent liquid
                // /ctc looktrace 5.0   false    false
                DataDumps.doLookTrace(getWorld(), McUtils.getPlayer(),
                        args.length >= 2 ? Double.parseDouble(args[1]) : 5.0,
                        args.length >= 3 && args[2].equals("true"),
                        args.length >= 4 && args[3].equals("true"));
                break;
            case "t":
                sendChat("executed t");
                break;
            default:
                sendCommandNotFound();
                break;
        }
    }

    private void sendCommandNotFound()
    {
        sendChat("Tweakception: command not found or wrong syntax");
    }
}
