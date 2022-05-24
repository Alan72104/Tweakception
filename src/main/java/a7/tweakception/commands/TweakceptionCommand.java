package a7.tweakception.commands;

import a7.tweakception.Tweakception;
import a7.tweakception.utils.DumpUtils;
import a7.tweakception.utils.McUtils;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static a7.tweakception.utils.McUtils.getWorld;
import static a7.tweakception.utils.McUtils.sendChat;

public class TweakceptionCommand extends CommandBase
{
    private final List<Command> subCommands = new ArrayList<>();

    public TweakceptionCommand()
    {
        addSub(new Command("dungeon",
            null,
            new Command("nofog",
                args -> Tweakception.dungeonTweaks.toggleNoFog(),
                new Command("auto",
                    args -> Tweakception.dungeonTweaks.toggleNoFogAutoToggle())),
            new Command("hidename",
                args -> Tweakception.dungeonTweaks.toggleHideName()),
            new Command("highlightstarredmobs",
                args -> Tweakception.dungeonTweaks.toggleHighlightStarredMobs()),
            new Command("highlightbats",
                args -> Tweakception.dungeonTweaks.toggleHighlightBats()),
            new Command("highlightspiritbear",
                args -> Tweakception.dungeonTweaks.toggleHighlightSpiritBear()),
            new Command("highlightshadowsssassin",
                args -> Tweakception.dungeonTweaks.toggleHighlightShadowAssassin()),
            new Command("blockrightclick",
                null,
                new Command("set",
                    args -> Tweakception.dungeonTweaks.blockRightClickSet()),
                new Command("list",
                    args -> Tweakception.dungeonTweaks.blockRightClickList())),
            new Command("trackdamage",
                args -> Tweakception.dungeonTweaks.toggleTrackDamageTags(),
                new Command("setcount",
                    args -> Tweakception.dungeonTweaks.setDamageTagTrackingCount(
                            args.length >= 1 ? Integer.parseInt(args[0]) : 10)),
                new Command("sethistorytimeout",
                    args -> Tweakception.dungeonTweaks.setDamageTagHistoryTimeoutTicks(
                            args.length >= 1 ? Integer.parseInt(args[0]) : 20 * 30)),
                new Command("noncrit",
                    args -> Tweakception.dungeonTweaks.toggleTrackNonCritDamageTags()),
                new Command("wither",
                    args -> Tweakception.dungeonTweaks.toggleTrackWitherDamageTags())),
            new Command("autoclosesecretchest",
                args -> Tweakception.dungeonTweaks.toggleAutoCloseSecretChest()),
            new Command("autosalvage",
                args -> Tweakception.dungeonTweaks.toggleAutoSalvage()),
            new Command("autojoinparty",
                args -> Tweakception.dungeonTweaks.toggleAutoJoinParty(),
                new Command("list",
                    args -> Tweakception.dungeonTweaks.autoJoinPartyList()),
                new Command("add",
                    args -> Tweakception.dungeonTweaks.autoJoinPartyAdd(args.length >= 1 ? args[0] : "")),
                new Command("remove",
                    args -> Tweakception.dungeonTweaks.autoJoinPartyRemove(args.length >= 1 ? args[0] : ""))),
            new Command("frag",
                    args -> Tweakception.dungeonTweaks.listFragCounts(),
                new Command("startsession",
                    args -> Tweakception.dungeonTweaks.fragStartSession()),
                new Command("endsession",
                    args -> Tweakception.dungeonTweaks.fragEndSession()),
                new Command("next",
                    args -> Tweakception.dungeonTweaks.fragNext()),
                new Command("setfragbot",
                    args -> Tweakception.dungeonTweaks.setFragBot(args.length >= 1 ? args[0] : "")),
                new Command("stats",
                    null)),
            new Command("trackshootingspeed",
                args -> Tweakception.dungeonTweaks.toggleTrackShootingSpeed(),
                    new Command("setsamplesecs",
                            args -> Tweakception.dungeonTweaks.setShootingSpeedTrackingSampleSecs(
                                    args.length >= 1 ? Integer.parseInt(args[0]) : 2)),
                    new Command("setspawnrange",
                            args -> Tweakception.dungeonTweaks.setShootingSpeedTrackingRange(
                                    args.length >= 1 ? Integer.parseInt(args[0]) : 2)))
        ));
        addSub(new Command("crimson",
            null,
            new Command("map",
                args -> Tweakception.crimsonTweaks.toggleMap(),
                new Command("pos",
                    args ->
                    {
                        if (args.length >= 2)
                            Tweakception.crimsonTweaks.setMapPos(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
                        else
                            sendCommandNotFound();
                    }),
                new Command("scale",
                    args ->
                    {
                        if (args.length >= 1)
                            Tweakception.crimsonTweaks.setMapScale(Float.parseFloat(args[0]));
                        else
                            sendCommandNotFound();
                    }),
                new Command("markerscale",
                    args ->
                    {
                        if (args.length >= 1)
                            Tweakception.crimsonTweaks.setMapMarkerScale(Float.parseFloat(args[0]));
                        else
                            sendCommandNotFound();
                    })),
            new Command("sulfur",
                args -> Tweakception.crimsonTweaks.toggleSulfurHighlight())
        ));
        addSub(new Command("mining",
            null,
            new Command("highlightchests",
                args -> Tweakception.miningTweaks.toggleHighlightChests())));
        addSub(new Command("gt",
            null,
            new Command("island",
                args -> Tweakception.globalTracker.printIsland()),
            new Command("forcesetisland",
                args ->
                {
                    if (args.length >= 1)
                        Tweakception.globalTracker.forceSetIsland(String.join(" ", args));
                    else
                        Tweakception.globalTracker.forceSetIsland("");
                }),
            new Command("copylocation",
                args -> Tweakception.globalTracker.copyLocation()),
            new Command("usefallbackdetection",
                args -> Tweakception.globalTracker.toggleFallbackDetection())
        ));
        addSub(new Command("fairy",
            args -> Tweakception.fairyTracker.toggle(),
            new Command("trackonce",
                args -> Tweakception.fairyTracker.trackOnce()),
            new Command("toggleauto",
                args -> Tweakception.fairyTracker.toggleAutoTracking()),
            new Command("setdelay",
                args ->
                {
                    if (args.length >= 1)
                        Tweakception.fairyTracker.setDelay(Integer.parseInt(args[0]));
                    else
                        sendCommandNotFound();
                }),
            new Command("setnotfound",
                args -> Tweakception.fairyTracker.setNotFound()),
            new Command("count",
                args -> Tweakception.fairyTracker.count()),
            new Command("list",
                args -> Tweakception.fairyTracker.list()),
            new Command("dump",
                args -> Tweakception.fairyTracker.dump()),
            new Command("import",
                args -> Tweakception.fairyTracker.load()),
            new Command("reset",
                args -> Tweakception.fairyTracker.reset())
        ));
        addSub(new Command("slayer",
            null,
            new Command("eman",
                null,
                new Command("highlightglyph",
                    args -> Tweakception.slayerTweaks.toggleHighlightGlyph()))
        ));
        addSub(new Command("next",
            args -> Tweakception.dungeonTweaks.fragNext()));
        addSub(new Command("autofish",
            args -> Tweakception.autoFish.toggleAutoFish(),
            new Command("setretrievedelay",
                args ->
                {
                    if (args.length >= 2)
                        Tweakception.autoFish.setRetrieveDelay(Integer.parseInt(args[0]), Integer.parseInt(args[1]), false);
                    else
                        Tweakception.autoFish.setRetrieveDelay(0, 0, true);
                }),
            new Command("setrecastdelay",
                args ->
                {
                    if (args.length >= 2)
                        Tweakception.autoFish.setRecastDelay(Integer.parseInt(args[0]), Integer.parseInt(args[1]), false);
                    else
                        Tweakception.autoFish.setRecastDelay(0, 0, true);
                }),
            new Command("setcatchestomove",
                args ->
                {
                    if (args.length >= 2)
                        Tweakception.autoFish.setCatchesToMove(Integer.parseInt(args[0]), Integer.parseInt(args[1]), false);
                    else
                        Tweakception.autoFish.setCatchesToMove(0, 0, true);
                }),
            new Command("toggledebug",
                args -> Tweakception.autoFish.toggleDebugInfo()),
            new Command("setheadmovingticks",
                args -> Tweakception.autoFish.setHeadMovingTicks(args.length >= 1 ? Integer.parseInt(args[0]) : 0)),
            new Command("setheadmovingyawrange",
                args -> Tweakception.autoFish.setHeadMovingYawRange(args.length >= 1 ? Float.parseFloat(args[0]) : 0.0f)),
            new Command("setheadmovingpitchrange",
                args -> Tweakception.autoFish.setHeadMovingPitchRange(args.length >= 1 ? Float.parseFloat(args[0]) : 0.0f))
            ).setVisibility(false));
        addSub(new Command("looktrace",
            args ->
            {
                // /tc looktrace reach adjacent liquid
                // /tc looktrace 5.0   false    false
                DumpUtils.doLookTrace(getWorld(), McUtils.getPlayer(),
                        args.length >= 1 ? Double.parseDouble(args[0]) : 5.0,
                        args.length >= 2 && args[1].equals("true"),
                        args.length >= 3 && args[2].equals("true"));
            }).setVisibility(false));
        addSub(new Command("dumpentityinrange",
            args -> DumpUtils.dumpEntitiesInRange(getWorld(), McUtils.getPlayer(),
                    args.length >= 1 ? Double.parseDouble(args[0]) : 5.0)).setVisibility(false));
        addSub(new Command("clientsetblock",
            args ->
            {
                if (args.length == 1)
                    getWorld().setBlockState(McUtils.getPlayer().getPosition(),
                            Block.blockRegistry.getObject(new ResourceLocation(args[0])).getDefaultState(),
                            1);
                else
                    sendChat("Give me 1 arg");
            }).setVisibility(false));
        addSub(new Command("trackticktime",
            args -> Tweakception.inGameEventDispatcher.toggleTickTimeTracking()).setVisibility(false));
        addSub(new Command("dev",
            args -> Tweakception.globalTracker.toggleDevMode()).setVisibility(false));
        addSub(new Command("t",
            args ->
            {
                sendChat("executed t");
            }).setVisibility(false));
    }

    @Override
    public String getCommandName()
    {
        return "tc";
    }

    @Override
    public String getCommandUsage(ICommandSender icommandsender)
    {
        return "/tc help";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        if (args.length == 0)
        {
            sendChat("Available sub commands");
            for (String subName : getVisibleSubCommandNames())
                sendChat("/tc " + subName);
        }
        else
        {
            for (Command sub : subCommands)
                if (sub.isVisible() && args[0].equals(sub.getName()))
                {
                    sub.processCommands(Arrays.copyOfRange(args, 1, args.length));
                    return;
                }
            sendCommandNotFound();
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos)
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

    private List<String> getVisibleSubCommandNames()
    {
        return subCommands.stream().
                filter(Command::isVisible).
                map(Command::getName).
                collect(Collectors.toList());
    }

    private void addSub(Command cmd)
    {
        subCommands.add(cmd);
    }

    private static List<String> getPossibleCompletions(String arg, List<String> opts)
    {
        List<String> list = new ArrayList<String>();

        for (String opt : opts)
            if (opt.startsWith(arg))
                list.add(opt);

        return list;
    }

    private static void sendCommandNotFound()
    {
        sendChat("Tweakception: command not found or wrong syntax");
    }
}
