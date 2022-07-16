package a7.tweakception.commands;

import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.GlobalTracker;
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

import static a7.tweakception.utils.McUtils.*;

public class TweakceptionCommand extends CommandBase
{
    private final List<Command> subCommands = new ArrayList<>();

    @SuppressWarnings("SpellCheckingInspection")
    public TweakceptionCommand()
    {
        addSub(new Command("dungeon",
            null,
            new Command("autoclosesecretchest",
                args -> Tweakception.dungeonTweaks.toggleAutoCloseSecretChest()),
            new Command("autojoinparty",
                args -> Tweakception.dungeonTweaks.toggleAutoJoinParty(),
                new Command("add",
                    args -> Tweakception.dungeonTweaks.autoJoinPartyAdd(args.length > 0 ? args[0] : "")),
                new Command("list",
                    args -> Tweakception.dungeonTweaks.autoJoinPartyList()),
                new Command("remove",
                    args -> Tweakception.dungeonTweaks.autoJoinPartyRemove(args.length > 0 ? args[0] : ""))
            ),
            new Command("autosalvage",
                args -> Tweakception.dungeonTweaks.toggleAutoSalvage()),
            new Command("blockopheliaclicks",
                args -> Tweakception.dungeonTweaks.toggleBlockOpheliaShopClicks()),
            new Command("blockrightclick",
                null,
                new Command("list",
                    args -> Tweakception.dungeonTweaks.blockRightClickList()),
                new Command("set",
                    args -> Tweakception.dungeonTweaks.blockRightClickSet()),
                new Command("remove",
                    args -> Tweakception.dungeonTweaks.blockRightClickRemove(
                            args.length >= 1 ? Integer.parseInt(args[0]) : 0))
            ),
            new Command("dailyruns",
                args -> Tweakception.dungeonTweaks.getDailyRuns(args.length > 0 ? args[0] : "")),
            new Command("displaymobnametag",
                args -> Tweakception.dungeonTweaks.toggleDisplayMobNameTag()),
            new Command("frag",
                    args -> Tweakception.dungeonTweaks.listFragCounts(),
                new Command("endsession",
                    args -> Tweakception.dungeonTweaks.fragEndSession()),
                new Command("next",
                    args -> Tweakception.dungeonTweaks.fragNext()),
                new Command("setfragbot",
                    args -> Tweakception.dungeonTweaks.setFragBot(args.length > 0 ? args[0] : "")),
                new Command("startsession",
                    args -> Tweakception.dungeonTweaks.fragStartSession()),
                new Command("stats", null)
            ),
            new Command("gyrowandoverlay",
                args -> Tweakception.dungeonTweaks.toggleGyroWandOverlay()),
            new Command("hidedamagetags",
                args -> Tweakception.dungeonTweaks.toggleHideDamageTags()),
            new Command("hidename",
                args -> Tweakception.dungeonTweaks.toggleHideName()),
            new Command("highlightbats",
                args -> Tweakception.dungeonTweaks.toggleHighlightBats()),
            new Command("highlightdoorkeys",
                args -> Tweakception.dungeonTweaks.toggleHighlightDoorKeys()),
            new Command("highlightshadowsssassin",
                args -> Tweakception.dungeonTweaks.toggleHighlightShadowAssassin()),
            new Command("highlightspiritbear",
                args -> Tweakception.dungeonTweaks.toggleHighlightSpiritBear()),
            new Command("highlightstarredmobs",
                args -> Tweakception.dungeonTweaks.toggleHighlightStarredMobs()),
            new Command("nofog",
                args -> Tweakception.dungeonTweaks.toggleNoFog(),
                new Command("auto",
                    args -> Tweakception.dungeonTweaks.toggleNoFogAutoToggle())
            ),
            new Command("partyfinder",
                null,
                new Command("blacklist",
                    args -> Tweakception.dungeonTweaks.partyFinderPlayerBlacklistSet(
                            args.length > 0 ? args[0] : "",
                            args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "")),
                new Command("clearcaches",
                    args -> Tweakception.dungeonTweaks.freeCaches()),
                new Command("quickplayerinfo",
                    args -> Tweakception.dungeonTweaks.partyFinderQuickPlayerInfoToggle(),
                    new Command("secretperexp",
                        args -> Tweakception.dungeonTweaks.partyFinderQuickPlayerInfoToggleShowSecretPerExp())
                ),
                new Command("refreshcooldown",
                    args -> Tweakception.dungeonTweaks.partyFinderRefreshCooldownToggle())
            ),
            new Command("trackdamage",
                args -> Tweakception.dungeonTweaks.toggleTrackDamageTags(),
                new Command("noncrit",
                    args -> Tweakception.dungeonTweaks.toggleTrackNonCritDamageTags()),
                new Command("setcount",
                    args -> Tweakception.dungeonTweaks.setDamageTagTrackingCount(
                            args.length > 0 ? Integer.parseInt(args[0]) : 0)),
                new Command("sethistorytimeout",
                    args -> Tweakception.dungeonTweaks.setDamageTagHistoryTimeoutTicks(
                            args.length > 0 ? Integer.parseInt(args[0]) : 0)),
                new Command("wither",
                    args -> Tweakception.dungeonTweaks.toggleTrackWitherDamageTags())
            ),
            new Command("trackmask",
                args -> Tweakception.dungeonTweaks.toggleTrackMaskUsage()),
            new Command("trackshootingspeed",
                args -> Tweakception.dungeonTweaks.toggleTrackShootingSpeed(),
                    new Command("setsamplesecs",
                        args -> Tweakception.dungeonTweaks.setShootingSpeedTrackingSampleSecs(
                                args.length >= 1 ? Integer.parseInt(args[0]) : 0)),
                    new Command("setspawnrange",
                        args -> Tweakception.dungeonTweaks.setShootingSpeedTrackingRange(
                                args.length >= 1 ? Integer.parseInt(args[0]) : 0))
            )
        ));
        addSub(new Command("crimson",
            null,
            new Command("map",
                args -> Tweakception.crimsonTweaks.toggleMap(),
                new Command("markerscale",
                    args -> Tweakception.crimsonTweaks.setMapMarkerScale(
                            args.length > 0 ? Float.parseFloat(args[0]) : 0.0f)),
                new Command("pos",
                    args -> Tweakception.crimsonTweaks.setMapPos(
                            args.length > 0 ? Integer.parseInt(args[0]) : -1,
                            args.length > 1 ? Integer.parseInt(args[1]) : -1)),
                new Command("scale",
                    args -> Tweakception.crimsonTweaks.setMapScale(
                            args.length > 0 ? Float.parseFloat(args[0]) : 0.0f))
            ),
            new Command("sulfur",
                args -> Tweakception.crimsonTweaks.toggleSulfurHighlight())
        ));
        addSub(new Command("mining",
            null,
            new Command("highlightchests",
                args -> Tweakception.miningTweaks.toggleHighlightChests())
        ));
        addSub(new Command("gt",
            null,
            new Command("blockquickcraft",
                args -> Tweakception.globalTracker.toggleBlockQuickCraft(),
                new Command("remove",
                    args -> Tweakception.globalTracker.removeBlockQuickCraftWhitelist(
                            args.length > 0 ? Integer.parseInt(args[0]) : 0))
            ),
            new Command("copylocation",
                args -> Tweakception.globalTracker.copyLocation()),
            new Command("entertoclosesign",
                args -> Tweakception.globalTracker.toggleEnterToCloseNumberTypingSign()),
            new Command("forcesetisland",
                args -> Tweakception.globalTracker.forceSetIsland(args.length > 0 ? String.join(" ", args) : "")),
            new Command("hideplayers",
                args -> Tweakception.globalTracker.toggleHidePlayers()),
            new Command("highlightshinypigs",
                args -> Tweakception.globalTracker.toggleHighlightShinyPigs(),
                new Command("setname",
                    args -> Tweakception.globalTracker.setHighlightShinyPigsName(args.length > 0 ? String.join(" ", args) : ""))
            ),
            new Command("island",
                args -> Tweakception.globalTracker.printIsland()),
            new Command("renderinvisiblearmorstands",
                args -> Tweakception.globalTracker.toggleRenderInvisibleArmorStands()),
            new Command("renderinvisibleenities",
                args -> Tweakception.globalTracker.toggleRenderInvisibleEntities()),
            new Command("setinvisibleentityalphapercentage",
                args -> Tweakception.globalTracker.setInvisibleEntityAlphaPercentage(
                    args.length > 0 ? Integer.parseInt(args[0]) : 0)),
            new Command("skipworldrendering",
                args -> Tweakception.globalTracker.toggleSkipWorldRendering()),
            new Command("usefallbackdetection",
                args -> Tweakception.globalTracker.toggleFallbackDetection()),
            new Command("rightctrlcopy",
                null,
                new Command("nbt",
                    args -> Tweakception.globalTracker.rightCtrlCopySet("nbt")),
                new Command("tooltip",
                    args -> Tweakception.globalTracker.rightCtrlCopySet("tooltip"))
            ).setVisibility(false)
        ));
        addSub(new Command("slayer",
            null,
            new Command("autohealwand",
                args -> Tweakception.slayerTweaks.toggleAutoHealWand(),
                new Command("setthreshold",
                    args -> Tweakception.slayerTweaks.setAutoHealWandHealthThreshold(
                            args.length >= 1 ? Integer.parseInt(args[0]) : 0))
            ),
            new Command("autothrowfishingrod",
                args -> Tweakception.slayerTweaks.toggleAutoThrowFishingRod(),
                new Command("setthreshold",
                    args -> Tweakception.slayerTweaks.setAutoThrowFishingRodThreshold(
                            args.length >= 1 ? Integer.parseInt(args[0]) : 0))
            ),
            new Command("eman",
                null,
                new Command("highlightglyph",
                    args -> Tweakception.slayerTweaks.toggleHighlightGlyph())
            ),
            new Command("highlightslayerminiboss",
                args -> Tweakception.slayerTweaks.toggleHighlightSlayerMiniboss()),
            new Command("highlightslayers",
                args -> Tweakception.slayerTweaks.toggleHighlightSlayers()),
            new Command("playercount",
                null,
                new Command("park",
                    args -> Tweakception.slayerTweaks.getPlayerCountInArea(0))
            )
        ));
        addSub(new Command("tuning",
            null,
            new Command("clickdelayticks",
                args -> Tweakception.tuningTweaks.setTuningClickDelay(
                        args.length > 0 ? Integer.parseInt(args[0]) : 0)),
            new Command("toggletemplate",
                args -> Tweakception.tuningTweaks.toggleTemplate())
        ));
        addSub(new Command("fairy",
            args -> Tweakception.fairyTracker.toggle(),
            new Command("count",
                args -> Tweakception.fairyTracker.count()),
            new Command("dump",
                args -> Tweakception.fairyTracker.dump()),
            new Command("import",
                args -> Tweakception.fairyTracker.load()),
            new Command("list",
                args -> Tweakception.fairyTracker.list()),
            new Command("reset",
                args -> Tweakception.fairyTracker.reset()),
            new Command("setdelay",
                args -> Tweakception.fairyTracker.setDelay(args.length > 0 ? Integer.parseInt(args[0]) : 0)),
            new Command("setnotfound",
                args -> Tweakception.fairyTracker.setNotFound()),
            new Command("toggleauto",
                args -> Tweakception.fairyTracker.toggleAutoTracking()),
            new Command("trackonce",
                args -> Tweakception.fairyTracker.trackOnce())
        ));
        addSub(new Command("api",
            null,
            new Command("set",
                args -> Tweakception.apiManager.setApiKey(args.length > 0 ? args[0] : "")),
            new Command("clearcaches",
                args -> Tweakception.apiManager.freeCaches()),
            new Command("copyprofile",
                args -> Tweakception.apiManager.copySkyblockProfile(args.length > 0 ? args[0] : "")
            ).setVisibility(false),
            new Command("debug",
                args -> Tweakception.apiManager.toggleDebug()).setVisibility(false),
            new Command("printcaches",
                args -> Tweakception.apiManager.printCaches()).setVisibility(false)
        ));
        addSub(new Command("next",
            args -> Tweakception.dungeonTweaks.fragNext()));
        addSub(new Command("autofish",
            args -> Tweakception.autoFish.toggleAutoFish(),
            new Command("setcatchestomove",
                args -> Tweakception.autoFish.setCatchesToMove(
                        args.length > 0 ? Integer.parseInt(args[0]) : -1,
                        args.length > 1 ? Integer.parseInt(args[1]) : -1)),
            new Command("setheadmovingpitchrange",
                args -> Tweakception.autoFish.setHeadMovingPitchRange(
                        args.length > 0 ? Float.parseFloat(args[0]) : 0.0f)),
            new Command("setheadmovingticks",
                args -> Tweakception.autoFish.setHeadMovingTicks(args.length > 0 ? Integer.parseInt(args[0]) : 0)),
            new Command("setheadmovingyawrange",
                args -> Tweakception.autoFish.setHeadMovingYawRange(args.length > 0 ? Float.parseFloat(args[0]) : 0.0f)),
            new Command("setrecastdelay",
                args -> Tweakception.autoFish.setRecastDelay(
                        args.length > 0 ? Integer.parseInt(args[0]) : -1,
                        args.length > 1 ? Integer.parseInt(args[1]) : -1)),
            new Command("setretrievedelay",
                args -> Tweakception.autoFish.setRetrieveDelay(
                        args.length > 0 ? Integer.parseInt(args[0]) : -1,
                        args.length > 1 ? Integer.parseInt(args[1]) : -1)),
            new Command("toggledebug",
                args -> Tweakception.autoFish.toggleDebugInfo()),
            new Command("toggleslugfish",
                args -> Tweakception.autoFish.toggleSlugfish())
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
                    args.length > 0 ? Double.parseDouble(args[0]) : 5.0)
        ).setVisibility(false));
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
        addSub(new Command("notifylagspike",
            args -> Tweakception.inGameEventDispatcher.toggleNotifyLagSpike(),
            new Command("setthreshold",
                args -> Tweakception.inGameEventDispatcher.setNotifyThreshold(
                        args.length > 0 ? Float.parseFloat(args[0]) : 0.0f)),
            new Command("setaggregation",
                args -> Tweakception.inGameEventDispatcher.setAggregationValue(
                        args.length > 0 ? Float.parseFloat(args[0]) : 0.0f))
        ).setVisibility(false));
        addSub(new Command("dev",
            args -> Tweakception.globalTracker.toggleDevMode()));
        addSub(new Command("t",
            args ->
            {
                GlobalTracker.t = !GlobalTracker.t;
                sendChat("t = " + GlobalTracker.t);
            }).setVisibility(false));
        addSub(new Command("action",
            args ->
            {
                if (args.length == 1)
                    Tweakception.globalTracker.doChatAction(args[0]);
            }));
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
        List<String> list = new ArrayList<>();

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
