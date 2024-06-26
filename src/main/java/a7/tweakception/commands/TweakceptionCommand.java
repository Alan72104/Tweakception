package a7.tweakception.commands;

import a7.tweakception.DevSettings;
import a7.tweakception.LagSpikeWatcher;
import a7.tweakception.Tweakception;
import a7.tweakception.tweaks.GlobalTweaks;
import a7.tweakception.tweaks.SkyblockIsland;
import a7.tweakception.tweaks.StringReplace;
import a7.tweakception.utils.Constants;
import a7.tweakception.utils.DumpUtils;
import a7.tweakception.utils.McUtils;
import a7.tweakception.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static a7.tweakception.utils.McUtils.*;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class TweakceptionCommand extends CommandBase
{
    private final List<Command> subCommands = new ArrayList<>();
    
    public TweakceptionCommand()
    {
        Method[] methods = TweakceptionCommand.class.getDeclaredMethods();
        for (Method method : methods)
        {
            if (method.getDeclaredAnnotation(RunInCtor.class) != null)
            {
                try
                {
                    method.invoke(this);
                }
                catch (IllegalAccessException | InvocationTargetException ignored)
                {
                }
            }
        }
    }
    
    @RunInCtor
    private void dungeon()
    {
        addSub(new Command("dungeon",
            new Command("autoCloseSecretChest",
                args -> Tweakception.dungeonTweaks.toggleAutoCloseSecretChest()),
            new Command("autojoinparty",
                args -> Tweakception.dungeonTweaks.toggleAutoJoinParty(),
                new Command("add",
                    args -> Tweakception.dungeonTweaks.autoJoinPartyAdd(getString(args, 0, ""))),
                new Command("list",
                    args -> Tweakception.dungeonTweaks.autoJoinPartyList()),
                new Command("remove",
                    args -> Tweakception.dungeonTweaks.autoJoinPartyRemove(getString(args, 0, ""))),
                new Command("togglewhitelist",
                    args -> Tweakception.dungeonTweaks.autoJoinPartyToggleWhitelist())
            ),
            new Command("autoswapsceptreaote",
                args -> Tweakception.dungeonTweaks.toggleAutoSwapSpiritSceptreAote()),
            new Command("autoswaphypeaote",
                args -> Tweakception.dungeonTweaks.toggleAutoSwapHyperionAote()),
            new Command("blockopheliaclicks",
                args -> Tweakception.dungeonTweaks.toggleBlockOpheliaShopClicks()),
            new Command("blockFlowerPlacement",
                args -> Tweakception.dungeonTweaks.toggleBlockFlowerPlacement()),
            new Command("blockrightclick",
                new Command("list",
                    args -> Tweakception.dungeonTweaks.blockRightClickList()),
                new Command("set",
                    args -> Tweakception.dungeonTweaks.blockRightClickSet()),
                new Command("remove",
                    args -> Tweakception.dungeonTweaks.blockRightClickRemove(
                        args.length >= 1 ? toInt(args[0]) : 0))
            ),
            new Command("dailyruns",
                args -> Tweakception.dungeonTweaks.getDailyRuns(getString(args, 0, ""))),
            new Command("displaymobnametag",
                args -> Tweakception.dungeonTweaks.toggleDisplayMobNameTag()),
            new Command("displaysoulname",
                args -> Tweakception.dungeonTweaks.toggleDisplaySoulName()),
            new Command("frag",
                args -> Tweakception.dungeonTweaks.listFragCounts(),
                new Command("autoreparty",
                    args -> Tweakception.dungeonTweaks.toggleFragAutoReparty()),
                new Command("endsession",
                    args -> Tweakception.dungeonTweaks.fragEndSession()),
                new Command("next",
                    args -> Tweakception.dungeonTweaks.fragNext()),
                new Command("setfragbot",
                    args -> Tweakception.dungeonTweaks.setFragBot(getString(args, 0, ""))),
                new Command("startsession",
                    args -> Tweakception.dungeonTweaks.fragStartSession())
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
                new Command("blacklist",
                    args -> Tweakception.dungeonTweaks.partyFinderPlayerBlacklistSet(
                        getString(args, 0, ""),
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
            new Command("pickaxeMiddleClickRemoveBlock",
                args -> Tweakception.dungeonTweaks.togglePickaxeMiddleClickRemoveBlock()),
            new Command("pickaxeMiddleClickRemoveLine",
                args -> Tweakception.dungeonTweaks.togglePickaxeMiddleClickRemoveLine()),
            new Command("trackdamage",
                args -> Tweakception.dungeonTweaks.toggleTrackDamageTags(),
                new Command("noncrit",
                    args -> Tweakception.dungeonTweaks.toggleTrackNonCritDamageTags()),
                new Command("setcount",
                    args -> Tweakception.dungeonTweaks.setDamageTagTrackingCount(
                        args.length > 0 ? toInt(args[0]) : 0)),
                new Command("sethistorytimeout",
                    args -> Tweakception.dungeonTweaks.setDamageTagHistoryTimeoutTicks(
                        args.length > 0 ? toInt(args[0]) : 0)),
                new Command("wither",
                    args -> Tweakception.dungeonTweaks.toggleTrackWitherDamageTags())
            ),
            new Command("trackdamagehistory",
                args -> Tweakception.dungeonTweaks.toggleTrackDamageHistory(),
                new Command("dump",
                    args -> Tweakception.dungeonTweaks.dumpDamageHistories()),
                new Command("reset",
                    args -> Tweakception.dungeonTweaks.resetDamageHistories()),
                new Command("maxlines",
                    args -> Tweakception.dungeonTweaks.setDamageHistoryOverlayMaxLines(
                        args.length > 0 ? toInt(args[0]) : 0))
            ),
            new Command("trackSalvage",
                args -> Tweakception.dungeonTweaks.toggleTrackSalvage(),
                new Command("list",
                    args -> Tweakception.dungeonTweaks.printTrackSalvage())
            ),
            new Command("trackmask",
                args -> Tweakception.dungeonTweaks.toggleTrackMaskUsage()),
            new Command("trackshootingspeed",
                args -> Tweakception.dungeonTweaks.toggleTrackShootingSpeed(),
                new Command("setsamplesecs",
                    args -> Tweakception.dungeonTweaks.setShootingSpeedTrackingSampleSecs(
                        args.length >= 1 ? toInt(args[0]) : 0)),
                new Command("setspawnrange",
                    args -> Tweakception.dungeonTweaks.setShootingSpeedTrackingRange(
                        args.length >= 1 ? toInt(args[0]) : 0))
            )
        ));
    }
    
    @RunInCtor
    private void gt()
    {
        addSub(new Command("gt",
            new Command("abiphonerelayhint",
                args -> Tweakception.globalTweaks.toggleAbiphoneRelayHint()),
            new Command("actionbar",
                new Command("sendbitsmessage",
                    args -> Tweakception.globalTweaks.toggleSendBitsMessage()),
                new Command("sendskyblockexpgainmsg",
                    args -> Tweakception.globalTweaks.toggleSendSkyblockLevelExpGainMessage())),
            new Command("afkmode",
                args -> Tweakception.globalTweaks.toggleAfkMode(),
                new Command("autoRightClick",
                    args -> Tweakception.globalTweaks.toggleAfkAutoRightClick()),
                new Command("fps",
                    args -> Tweakception.globalTweaks.setAfkFpsLimit(args.length > 0 ? toInt(args[0]) : 0)),
                new Command("onlyunfocused",
                    args -> Tweakception.globalTweaks.toggleAfkOnlyUnfocused()),
                new Command("skipworldrendering",
                    args -> Tweakception.globalTweaks.toggleAfkSkipWorldRendering())
            ),
            new Command("areaedit",
                args -> Tweakception.globalTweaks.toggleAreaEdit(),
                new Command("print",
                    args -> Tweakception.globalTweaks.printArea()),
                new Command("reset",
                    args -> Tweakception.globalTweaks.resetArea()),
                new Command("setpoint",
                    args ->
                    {
                        if (args.length >= 4)
                            Tweakception.globalTweaks.setAreaPoint(args[0].equals("2") ? 1 : 0,
                                toInt(args[1]), toInt(args[2]), toInt(args[3]));
                        else
                            sendChat("Give me 4 args");
                    }),
                new Command("setarea",
                    args ->
                    {
                        if (args.length >= 6)
                        {
                            Tweakception.globalTweaks.setAreaPoint(0,
                                toInt(args[0]), toInt(args[1]), toInt(args[2]));
                            Tweakception.globalTweaks.setAreaPoint(1,
                                toInt(args[3]), toInt(args[4]), toInt(args[5]));
                        }
                        else
                            sendChat("Give me 6 args");
                    })
            ),
            new Command("armorColorSortingHelper",
                args -> Tweakception.globalTweaks.toggleArmorColorSortingHelper()),
            new Command("autoConsumeBoosterCookie",
                args -> Tweakception.globalTweaks.toggleAutoConsumeBoosterCookie()),
            new Command("autoHarp",
                args -> Tweakception.globalTweaks.toggleAutoHarp(),
                new Command("autoCloseOnNonPerfect",
                    args -> Tweakception.globalTweaks.toggleAutoHarpAutoClose()),
                new Command("replayMode",
                    args -> Tweakception.globalTweaks.toggleAutoHarpReplayMode()),
                new Command("setClickDelayTicks",
                    args -> Tweakception.globalTweaks.setAutoHarpClickDelayTicks(getInt(args, 0, -1)))
            ),
            new Command("autoGuildWelcome",
                args -> Tweakception.globalTweaks.toggleAutoGuildWelcome()),
            new Command("blockquickcraft",
                args -> Tweakception.globalTweaks.toggleBlockQuickCraft(),
                new Command("remove",
                    args -> Tweakception.globalTweaks.removeBlockQuickCraftWhitelist(
                        args.length > 0 ? toInt(args[0]) : 0))
            ),
            new Command("buildersWandItemsTooltip",
                args -> Tweakception.globalTweaks.toggleBuildersWandItemsTooltip()),
            new Command("championoverlay",
                args -> Tweakception.globalTweaks.toggleChampionOverlay(),
                new Command("incrementresetdelay",
                    args -> Tweakception.globalTweaks.setChampionOverlayIncrementResetDuration(
                        args.length > 0 ? toInt(args[0]) : 0))
            ),
            new Command("copylocation",
                args -> Tweakception.globalTweaks.copyLocation()),
            new Command("centuryCakeCountInChat",
                args -> Tweakception.globalTweaks.toggleCenturyCakeCountInChat()),
            new Command("disabletooltips",
                args -> Tweakception.globalTweaks.toggleDisableTooltips()),
            new Command("displaypersonalcompactoritems",
                args -> Tweakception.globalTweaks.toggleDisplayPersonalCompactorItems()),
            new Command("displaypersonaldeletoritems",
                args -> Tweakception.globalTweaks.toggleDisplayPersonalDeletorItems()),
            new Command("dojo",
                new Command("discipline",
                    args -> Tweakception.globalTweaks.toggleDojoDisciplineHelper())),
            new Command("drawselectedentityoutline",
                args -> Tweakception.globalTweaks.toggleDrawSelectedEntityOutline(),
                new Command("width",
                    args -> Tweakception.globalTweaks.setSelectedEntityOutlineWidth(
                        args.length >= 1 ? toFloat(args[0]) : 0.0f)),
                new Command("color",
                    args ->
                    {
                        if (args.length >= 4)
                            Tweakception.globalTweaks.setSelectedEntityOutlineColor(
                                toInt(args[0]), toInt(args[1]), toInt(args[2]), toInt(args[3]));
                        else
                            Tweakception.globalTweaks.setSelectedEntityOutlineColor(-1, 0, 0, 0);
                    })
            ),
            new Command("entertoclosesign",
                args -> Tweakception.globalTweaks.toggleEnterToCloseNumberTypingSign()),
            new Command("fastCommand",
                args -> Tweakception.globalTweaks.setFastCommand(String.join(" ", args))),
            new Command("fakepowerscrolls",
                args -> Tweakception.globalTweaks.toggleFakePowerScrolls()),
            new Command("fakestars",
                args -> Tweakception.globalTweaks.toggleFakeStars(),
                new Command("red",
                    args -> Tweakception.globalTweaks.setFakeStarsRed(args.length > 0 ? toInt(args[0]) : 0)),
                new Command("purple",
                    args -> Tweakception.globalTweaks.setFakeStarsPurple(args.length > 0 ? toInt(args[0]) : 0)),
                new Command("aqua",
                    args -> Tweakception.globalTweaks.setFakeStarsAqua(args.length > 0 ? toInt(args[0]) : 0))
            ),
            new Command("forceFormattedChatLog",
                args -> Tweakception.globalTweaks.toggleChatLogForceFormatted()),
            new Command("hidefromstrangers",
                args -> Tweakception.globalTweaks.toggleHideFromStrangers(),
                new Command("whitelist",
                    args -> Tweakception.globalTweaks.setHideFromStrangersWhitelist(
                        getString(args, 0, "")))
            ),
            new Command("hideplayers",
                args -> Tweakception.globalTweaks.toggleHidePlayers(),
                new Command("whitelist",
                    args -> Tweakception.globalTweaks.setHidePlayersWhitelist(getString(args, 0, "")),
                    Command.playerNameProvider())
            ),
            new Command("hideMinionStorageFull",
                args -> Tweakception.globalTweaks.toggleHideMinionStorageFull()),
            new Command("hlBlock",
                args ->
                {
                    if (args.length >= 3)
                        Tweakception.globalTweaks.highlightBlock(toInt(args[0]), toInt(args[1]), toInt(args[2]));
                    else if (args.length == 1)
                        Tweakception.globalTweaks.highlightBlocks(new BlockPos[]
                            {
                                McUtils.getPlayer().getPosition()
                            });
                    else
                        sendChat("Give me 3 args");
                },
                new Command("addArachnesKeeper",
                    args -> Tweakception.globalTweaks.highlightBlocks(Constants.ARACHNES_KEEPER_LOCATIONS)),
                new Command("addSpidersDenRelics",
                    args -> Tweakception.globalTweaks.highlightBlocks(Constants.SPIDERS_DEN_RELIC_LOCATIONS)),
                new Command("clear",
                    args -> Tweakception.globalTweaks.highlightBlockClear()),
                new Command("removeNearest",
                    args -> Tweakception.globalTweaks.highlightBlockRemoveNearest()),
                new Command("list",
                    args -> Tweakception.globalTweaks.highlightBlockList()),
                new Command("copy",
                    args -> Tweakception.globalTweaks.highlightBlockCopy()),
                new Command("paste",
                    args -> Tweakception.globalTweaks.highlightBlockPaste())
            ),
            new Command("hlshinypigs",
                args -> Tweakception.globalTweaks.toggleHighlightShinyPigs(),
                new Command("setname",
                    args -> Tweakception.globalTweaks.setHighlightShinyPigsName(args.length > 0 ? String.join(" ", args) : ""))
            ),
            new Command("hlEntityType",
                args -> Tweakception.globalTweaks.highlightEntityType(getString(args, 0, "")),
                new Command("clear",
                    args -> Tweakception.globalTweaks.highlightEntityTypeClear()),
                new Command("list",
                    args -> Tweakception.globalTweaks.highlightEntityTypeList())
            ),
            new Command("hlPlayer",
                args -> Tweakception.globalTweaks.setPlayerToHighlight(getString(args, 0, "")),
                Command.playerNameProvider()),
            new Command("hlPlayers",
                args -> Tweakception.globalTweaks.toggleHighlightPlayers()),
            new Command("hlArmorStand",
                args -> Tweakception.globalTweaks.setArmorStandToHighlight(getString(args, 0, ""))),
            new Command("hlSkulls",
                args -> Tweakception.globalTweaks.toggleHighlightSkulls()),
            new Command("ignoreServerRenderDistance",
                args -> Tweakception.globalTweaks.toggleIgnoreServerRenderDistance()),
            new Command("island",
                args -> Tweakception.globalTweaks.printIsland()),
            new Command("minionautoclaim",
                args -> Tweakception.globalTweaks.toggleMinionAutoClaim(),
                new Command("whitelist",
                    args ->
                    {
                        if (args.length > 0)
                            Tweakception.globalTweaks.addMinionAutoClaimWhitelist(args[0]);
                        else
                            sendChat("Need a id");
                    },
                    new Command("remove",
                        args ->
                        {
                            if (args.length > 0)
                                Tweakception.globalTweaks.removeMinionAutoClaimWhitelist(toInt(args[0]));
                            else
                                sendChat("Need a id");
                        }
                    )
                ),
                new Command("setdelay",
                    args -> Tweakception.globalTweaks.setMinionAutoClaimClickDelayMin(
                        args.length > 0 ? toInt(args[0]) : 0))
            ),
            new Command("minionAutoClaimHopper",
                args -> Tweakception.globalTweaks.toggleMinionAutoClaimHopper()),
            new Command("ranchersbootsspeednote",
                args -> Tweakception.globalTweaks.toggleRanchersBootsTooltipSpeedNote()),
            new Command("renderinvisiblearmorstands",
                args -> Tweakception.globalTweaks.toggleRenderInvisibleArmorStands()),
            new Command("renderinvisibleenities",
                args -> Tweakception.globalTweaks.toggleRenderInvisibleEntities()),
            new Command("renderenchantedbookstype",
                args -> Tweakception.globalTweaks.toggleRenderEnchantedBooksType()),
            new Command("rendersackstype",
                args -> Tweakception.globalTweaks.toggleRenderSacksType()),
            new Command("renderpotiontier",
                args -> Tweakception.globalTweaks.toggleRenderPotionTier()),
            new Command("stepHeight",
                args ->
                {
                    if (McUtils.getPlayer().stepHeight > 0.6f)
                        McUtils.getPlayer().stepHeight = 0.6f;
                    else
                        McUtils.getPlayer().stepHeight = 1.0f;
                    sendChat("Set player step height to " + McUtils.getPlayer().stepHeight + " blocks");
                    sendChat("(this gets reset on lobby swap)");
                }),
            new Command("setinvisibleentityalphapercentage",
                args -> Tweakception.globalTweaks.setInvisibleEntityAlphaPercentage(
                    args.length > 0 ? toInt(args[0]) : 0)),
            new Command("setisland",
                args -> Tweakception.globalTweaks.overrideIsland(args.length > 0 ? String.join(" ", args) : ""),
                args -> Arrays.stream(SkyblockIsland.values()).map(island -> island.name).collect(Collectors.toList())
            ),
            new Command("snipe",
                args -> Tweakception.globalTweaks.startSnipe(
                    getString(args, 0, ""),
                    args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : ""),
                new Command("stop",
                    args -> Tweakception.globalTweaks.stopSnipe()),
                new Command("warpdelay",
                    args -> Tweakception.globalTweaks.setSnipeWarpDelay(
                        args.length > 0 ? toInt(args[0]) : 0))
            ),
            new Command("skipworldrendering",
                args -> Tweakception.globalTweaks.toggleSkipWorldRendering()),
            new Command("targeting",
                new Command("armorStandDisable",
                    args -> Tweakception.globalTweaks.toggleDisableArmorStandTargeting()),
                new Command("batDisable",
                    args -> Tweakception.globalTweaks.toggleDisableBatTargeting()),
                new Command("deadMobDisable",
                    args -> Tweakception.globalTweaks.toggleDisableDeadMobTargeting()),
                new Command("playerDisable",
                    args -> Tweakception.globalTweaks.toggleDisablePlayerTargeting()),
                new Command("reset",
                    args -> Tweakception.globalTweaks.resetTargeting())
            ),
            new Command("tooltip",
                new Command("id",
                    args -> Tweakception.globalTweaks.toggleTooltipDisplayId())
            ),
            new Command("tooltipOveride",
                args -> Tweakception.globalTweaks.setTooltipOverride(false),
                new Command("off",
                    args -> Tweakception.globalTweaks.setTooltipOverride(true))
            ),
            new Command("trevor",
                new Command("autoaccept",
                    args -> Tweakception.globalTweaks.toggleTrevorQuestAutoAccept()),
                new Command("autostart",
                    args -> Tweakception.globalTweaks.toggleTrevorQuestAutoStart()),
                new Command("highlightanimal",
                    args -> Tweakception.globalTweaks.toggleTrevorAnimalHighlight())
            ),
            new Command("onlinestatusoverlay",
                args -> Tweakception.globalTweaks.toggleOnlineStatusOverlay(),
                new Command("showalreadyon",
                    args -> Tweakception.globalTweaks.toggleOnlineStatusOverlayShowAlreadyOn())
            ),
            new Command("ping",
                args -> Tweakception.globalTweaks.pingServer(),
                new Command("overlay",
                    args -> Tweakception.globalTweaks.pingOverlay())),
            new Command("playercount",
                new Command("park",
                    args -> Tweakception.globalTweaks.getPlayerCountInArea(0)),
                new Command("crimson",
                    new Command("stronghold",
                        new Command("back",
                            new Command("topright",
                                args -> Tweakception.globalTweaks.getPlayerCountInArea(1))
                        ),
                        new Command("front",
                            new Command("topright",
                                args -> Tweakception.globalTweaks.getPlayerCountInArea(2))
                        )
                    )
                )
            ),
            new Command("playersinareas",
                args -> Tweakception.globalTweaks.togglePlayersInAreasDisplay()),
            new Command("logpackets",
                args -> Tweakception.globalTweaks.toggleLogPacket(),
                new Command("setallowed",
                    args ->
                    {
                        if (args.length > 0)
                            Tweakception.globalTweaks.setPacketLogAllowedClass(args[0]);
                        else
                            sendChat("Give me 1 arg");
                    }),
                new Command("logall",
                    args -> Tweakception.globalTweaks.toggleLogPacketLogAll())
            ).setVisibility(false),
            new Command("rightctrlcopy",
                new Command("nbt",
                    args -> Tweakception.globalTweaks.rightCtrlCopySet("nbt")),
                new Command("tooltip",
                    args -> Tweakception.globalTweaks.rightCtrlCopySet("tooltip")),
                new Command("tooltipfinal",
                    args -> Tweakception.globalTweaks.rightCtrlCopySet("tooltipfinal"))
            ).setVisibility(false)
        ));
    }
    
    @RunInCtor
    private void garden()
    {
        addSub(new Command("garden",
            new Command("autoClaimContest",
                args -> Tweakception.gardenTweaks.toggleAutoClaimContests()),
            new Command("autoTurnOnHideFromStrangersWithSnapYaw",
                args -> Tweakception.gardenTweaks.toggleAutoTurnOnHideFromStrangersWithSnapYaw()),
            new Command("milestoneOverlay",
                args -> Tweakception.gardenTweaks.toggleMilestoneOverlay()),
            new Command("composterAmountNeededOverlay",
                args -> Tweakception.gardenTweaks.toggleComposterAmountNeededOverlay()),
            new Command("contestDataDumper",
                args -> Tweakception.gardenTweaks.toggleContestDataDumper(),
                new Command("dumpHeader",
                    args -> Tweakception.gardenTweaks.toggleContestDataDumperDumpHeader())
            ),
            new Command("cropGrowthRateAnalysis",
                args -> Tweakception.gardenTweaks.toggleCropGrowthRateAnalysis(),
                new Command("dump",
                    args -> Tweakception.gardenTweaks.dumpCropGrowthRateAnalysis()),
                new Command("reset",
                    args -> Tweakception.gardenTweaks.resetCropGrowthRateAnalysis())
            ),
            new Command("logCropBreaks",
                args -> Tweakception.gardenTweaks.toggleLogCropBreaks(),
                new Command("verbose",
                    args -> Tweakception.gardenTweaks.toggleLogCropBreaksVerboseConsole())
            ),
            new Command("simulateCactusKnifeInstaBreak",
                args -> Tweakception.gardenTweaks.toggleSimulateCactusKnifeInstaBreak()),
            new Command("snapYaw",
                args -> Tweakception.gardenTweaks.toggleSnapYaw(),
                new Command("angle",
                    args -> Tweakception.gardenTweaks.setSnapYawAngle(getInt(args, 0, -1))),
                new Command("range",
                    args -> Tweakception.gardenTweaks.setSnapYawRange(getInt(args, 0, -1)))
            ),
            new Command("snapPitch",
                args -> Tweakception.gardenTweaks.toggleSnapPitch(),
                new Command("angle",
                    args -> Tweakception.gardenTweaks.setSnapPitchAngle(getInt(args, 0, -1))),
                new Command("range",
                    args -> Tweakception.gardenTweaks.setSnapPitchRange(getInt(args, 0, -1)))
            ),
            new Command("speedOverlay",
                args -> Tweakception.gardenTweaks.toggleSpeedOverlay(),
                new Command("setAveragePeriodSecs",
                    args -> Tweakception.gardenTweaks.setSpeedOverlayAveragePeriodSecs(getInt(args, 0, -1)))
            ),
            new Command("verifyCrops",
                args -> Tweakception.gardenTweaks.verifyCrops(),
                new Command("clear",
                    args -> Tweakception.gardenTweaks.verifyCropsClear())
            )
        ));
    }
    
    @RunInCtor
    private void crimson()
    {
        addSub(new Command("crimson",
            new Command("map",
                args -> Tweakception.crimsonTweaks.toggleMap(),
                new Command("markerscale",
                    args -> Tweakception.crimsonTweaks.setMapMarkerScale(
                        args.length > 0 ? toFloat(args[0]) : 0.0f)),
                new Command("pos",
                    args -> Tweakception.crimsonTweaks.setMapPos(
                        args.length > 0 ? toInt(args[0]) : -1,
                        args.length > 1 ? toInt(args[1]) : -1)),
                new Command("scale",
                    args -> Tweakception.crimsonTweaks.setMapScale(
                        args.length > 0 ? toFloat(args[0]) : 0.0f))
            ),
            new Command("sulfur",
                args -> Tweakception.crimsonTweaks.toggleSulfurHighlight())
        ));
    }
    
    @RunInCtor
    private void mining()
    {
        addSub(new Command("mining",
            new Command("ghost",
                new Command("autoAttack",
                    args -> Tweakception.miningTweaks.toggleGhostAutoAttack()),
                new Command("nameTag",
                    args -> Tweakception.miningTweaks.toggleGhostNameTag()),
                new Command("setGlassFloorToStone",
                    args -> Tweakception.miningTweaks.setGlassFloorToStone())
            ),
            new Command("highlightChests",
                args -> Tweakception.miningTweaks.toggleHighlightChests()),
            new Command("simulateBlockHardness",
                args -> Tweakception.miningTweaks.toggleSimulateBlockHardness(),
                new Command("setExtraTicks",
                     args -> Tweakception.miningTweaks.setSimulateBlockHardnessExtraTicks(getInt(args, 0, 0))),
                new Command("setExtraTicksOnBoost",
                     args -> Tweakception.miningTweaks.setSimulateBlockHardnessExtraTicksOnBoost(getInt(args, 0, 0))),
                new Command("printCachedMiningSpeed",
                    args -> Tweakception.miningTweaks.printMiningSpeedCache()),
                new Command("setMiningSpeedBoostLevel",
                    args -> Tweakception.miningTweaks.setMiningSpeedBoostLevel(getInt(args, 0, 0))),
                new Command("setToolMiningSpeedOverride",
                    args -> Tweakception.miningTweaks.setToolMiningSpeedOverride(getInt(args, 0, 0))).setVisibility(false)
            )
        ));
    }
    
    @RunInCtor
    private void slayer()
    {
        addSub(new Command("slayer",
            new Command("autohealwand",
                args -> Tweakception.slayerTweaks.toggleAutoHealWand(),
                new Command("setthreshold",
                    args -> Tweakception.slayerTweaks.setAutoHealWandHealthThreshold(
                        args.length >= 1 ? toInt(args[0]) : 0))
            ),
            new Command("autothrowfishingrod",
                args -> Tweakception.slayerTweaks.toggleAutoThrowFishingRod(),
                new Command("setthreshold",
                    args -> Tweakception.slayerTweaks.setAutoThrowFishingRodThreshold(
                        args.length >= 1 ? toInt(args[0]) : 0))
            ),
            new Command("eman",
                new Command("highlightglyph",
                    args -> Tweakception.slayerTweaks.toggleHighlightGlyph())
            ),
            new Command("highlightslayerminiboss",
                args -> Tweakception.slayerTweaks.toggleHighlightSlayerMiniboss()),
            new Command("highlightslayers",
                args -> Tweakception.slayerTweaks.toggleHighlightSlayers())
        ));
    }
    
    @RunInCtor
    private void tuning()
    {
        addSub(new Command("tuning",
            new Command("clickdelayticks",
                args -> Tweakception.tuningTweaks.setTuningClickDelay(
                    args.length > 0 ? toInt(args[0]) : 0)),
            new Command("toggletemplate",
                args -> Tweakception.tuningTweaks.toggleTemplate())
        ));
    }
    
    @RunInCtor
    private void enchanting()
    {
        addSub(new Command("enchanting",
            new Command("autoSolve",
                args -> Tweakception.enchantingTweaks.toggleAutoSolve()),
            new Command("debugInfo",
                args -> Tweakception.enchantingTweaks.toggleDrawDebugInfo()),
            new Command("maxChronomatronRounds",
                args -> Tweakception.enchantingTweaks.setMaxChronomatronRounds(getInt(args, 0, 0))),
            new Command("maxUltrasequencerRounds",
                args -> Tweakception.enchantingTweaks.setMaxUltrasequencerRounds(getInt(args, 0, 0))),
            new Command("setClickDelayTicks",
                args -> Tweakception.enchantingTweaks.setClickDelayTicks(getInt(args, 0, -1)))
        ));
    }
    
    @RunInCtor
    private void fairy()
    {
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
            new Command("setnotfound",
                args -> Tweakception.fairyTracker.setNotFound()),
            new Command("toggletracking",
                args -> Tweakception.fairyTracker.toggleTracking())
        ));
    }
    
    @RunInCtor
    private void gift()
    {
        addSub(new Command("gift",
            args -> Tweakception.giftTweaks.toggleGiftFeatures(),
//            new Command("autoGiftAndRefillFromInv",
//                args -> Tweakception.giftTweaks.toggleAutoGiftAndRefillFromInv()),
//            new Command("setAutoGiftTarget",
//                args -> Tweakception.giftTweaks.setAutoGiftTarget(getString(args, 0, ""))),
            new Command("autoSwitchGiftSlot",
                args -> Tweakception.giftTweaks.toggleAutoSwitchGiftSlot()),
            new Command("autoSetTargeting",
                args -> Tweakception.giftTweaks.toggleAutoSetTargeting()),
            new Command("autoReleaseRightClick",
                args -> Tweakception.giftTweaks.toggleAutoReleaseRightClick(),
                new Command("setDistance",
                    args -> Tweakception.giftTweaks.setAutoReleaseRightClickDistance(getInt(args, 0, -1))),
                new Command("setWhitelist",
                    args -> Tweakception.giftTweaks.setAutoReleaseRightClickWhitelist(getString(args, 0, "")))
            ),
            new Command("invFeatures",
                args -> Tweakception.giftTweaks.toggleInvFeatures(),
                new Command("setMinDelay",
                    args -> Tweakception.giftTweaks.setInvFeaturesMinDelay(getInt(args, 0, -1))),
                new Command("dumpBlacklist",
                    args -> Tweakception.giftTweaks.toggleRewardDumpBlacklist(getString(args, 0, "")),
                    args -> Tweakception.giftTweaks.getRewardDumpBlacklist())
            ),
            new Command("targeting",
                new Command("armorStandDisable",
                    args -> Tweakception.giftTweaks.toggleDisableArmorStandTargeting()),
                new Command("giftsOpenableOnly",
                    args -> Tweakception.giftTweaks.toggleTargetOnlyOpenableGift())
            ),
            new Command("toggleRecipient",
                args -> Tweakception.giftTweaks.toggleRecipient()),
            new Command("toggleThrowValuable",
                args -> Tweakception.giftTweaks.toggleThrowValuable()),
            new Command("trackWhiteGifts",
                args -> Tweakception.giftTweaks.toggleWhiteGiftTracking())
        ));
    }
    
    @RunInCtor
    private void overlay()
    {
        addSub(new Command("overlay",
            args -> Tweakception.overlayManager.editOverlays()
        ));
    }
    
    @RunInCtor
    private void api()
    {
        addSub(new Command("api",
            new Command("set",
                args -> Tweakception.apiManager.setApiKey(getString(args, 0, ""))),
            new Command("clearcaches",
                args -> Tweakception.apiManager.freeCaches()),
            new Command("copyprofile",
                args -> Tweakception.apiManager.copySkyblockProfile(getString(args, 0, ""))
            ).setVisibility(false),
            new Command("debug",
                args -> Tweakception.apiManager.toggleDebug()).setVisibility(false),
            new Command("printcaches",
                args -> Tweakception.apiManager.printCaches()).setVisibility(false)
        ));
    }
    
    @RunInCtor
    private void next()
    {
        addSub(new Command("next",
            args -> Tweakception.dungeonTweaks.fragNext()));
    }
    
    @RunInCtor
    private void dailies()
    {
        addSub(new Command("dailies",
            args -> Tweakception.dungeonTweaks.getDailyRuns(getString(args, 0, ""))));
    }
    
    @RunInCtor
    private void stopSnipe()
    {
        addSub(new Command("stopSnipe",
            args -> Tweakception.globalTweaks.stopSnipe()));
    }
    
    @RunInCtor
    private void limbo()
    {
        addSub(new Command("limbo",
            args ->
            {
                Tweakception.inGameEventDispatcher.limboCommandSilence = true;
                Tweakception.scheduler.addDelayed(
                    () -> Tweakception.inGameEventDispatcher.limboCommandSilence = false,
                    40);
                for (int i = 0; i < 12; i++)
                    McUtils.getPlayer().sendChatMessage("/");
            }
        ));
    }
    
    @RunInCtor
    private void fish()
    {
        addSub(new Command("fish",
            args -> Tweakception.fishingTweaks.toggleAutoFish(),
            new Command("setcatchestomove",
                args -> Tweakception.fishingTweaks.setCatchesToMove(
                    args.length > 0 ? toInt(args[0]) : -1,
                    args.length > 1 ? toInt(args[1]) : -1)),
            new Command("setheadmovingpitchrange",
                args -> Tweakception.fishingTweaks.setHeadMovingPitchRange(
                    args.length > 0 ? toFloat(args[0]) : 0.0f)),
            new Command("setheadmovingticks",
                args -> Tweakception.fishingTweaks.setHeadMovingTicks(args.length > 0 ? toInt(args[0]) : 0)),
            new Command("setheadmovingyawrange",
                args -> Tweakception.fishingTweaks.setHeadMovingYawRange(args.length > 0 ? toFloat(args[0]) : 0.0f)),
            new Command("setrecastdelay",
                args -> Tweakception.fishingTweaks.setRecastDelay(
                    args.length > 0 ? toInt(args[0]) : -1,
                    args.length > 1 ? toInt(args[1]) : -1)),
            new Command("setretrievedelay",
                args -> Tweakception.fishingTweaks.setRetrieveDelay(
                    args.length > 0 ? toInt(args[0]) : -1,
                    args.length > 1 ? toInt(args[1]) : -1)),
            new Command("toggledebug",
                args -> Tweakception.fishingTweaks.toggleDebugInfo()),
            new Command("slugfish",
                args -> Tweakception.fishingTweaks.toggleSlugfish()),
            new Command("thunderbottleoverlay",
                args -> Tweakception.fishingTweaks.toggleThunderBottleOverlay(),
                new Command("incrementresetdelay",
                    args -> Tweakception.fishingTweaks.setThunderBottleChargeIncrementResetDuration(
                        args.length > 0 ? toInt(args[0]) : 0)))
        ).setVisibility(false));
    }
    
    @RunInCtor
    private void foraging()
    {
        addSub(new Command("foraging",
            new Command("axeMidClickSwapRodBreak",
                args -> Tweakception.foragingTweaks.toggleAxeMidClickSwapRodBreak()),
            new Command("tree",
                args -> Tweakception.foragingTweaks.toggleTreeIndicator()),
            new Command("debug",
                args -> Tweakception.foragingTweaks.debugTreeIndicator(
                    args.length > 0 ? toInt(args[0]) : -1))
        ));
    }
    
    @RunInCtor
    private void bazaar()
    {
        addSub(new Command("bazaar",
            new Command("printorders",
                args -> Tweakception.bazaarTweaks.printOrders())
        ));
    }
    
    @RunInCtor
    private void stringReplace()
    {
        addSub(new Command("stringReplace",
            new Command("replace",
                args ->
                {
                    if (args.length >= 2)
                    {
                        StringReplace.add(args[0], args[1]);
                        sendChatf("Added %s -> %s", args[0], args[1]);
                    }
                    else if (args.length == 1)
                    {
                        StringReplace.remove(args[0]);
                        sendChatf("Removed %s", args[0]);
                    }
                    else
                        sendChat("Needs 2 strings");
                }),
            new Command("clear",
                args ->
                {
                    StringReplace.clear();
                    sendChat("Cleared");
                }),
            new Command("mapType",
                args ->
                {
                    if (args.length <= 1 &&
                        Arrays.stream(StringReplace.MapType.values())
                        .anyMatch(type -> type.toString().equalsIgnoreCase(args[0]))
                    )
                        StringReplace.changeMapType(
                            StringReplace.MapType.valueOf(args[0].toUpperCase(Locale.ROOT)));
                    else
                        sendChat("Invalid map type");
                },
                args -> Arrays.stream(StringReplace.MapType.values())
                    .map(Enum::toString)
                    .collect(Collectors.toList())
            ),
            new Command("overlay",
                args -> StringReplace.toggleOverlay())
        ).setVisibility(false));
    }
    
    @RunInCtor
    private void lagSpikeWatcher()
    {
        addSub(new Command("lagSpikeWatcher",
            new Command("start",
                args ->
                {
                    if (LagSpikeWatcher.isWatcherOn())
                        sendChat("TC: watcher is currently on");
                    else
                    {
                        LagSpikeWatcher.startWatcher();
                        sendChat("TC: watcher turned on");
                        sendChat("TC: it will now find the median frametime over 5 secs, times 2 as the threshold");
                    }
                }),
            new Command("startNow",
                args ->
                {
                    if (LagSpikeWatcher.isWatcherOn())
                        sendChat("TC: watcher is currently on");
                    else
                    {
                        LagSpikeWatcher.startWatcherWithoutThresholdDetection();
                        sendChat("TC: watcher turned on");
                    }
                }),
            new Command("stop",
                args ->
                {
                    if (LagSpikeWatcher.isWatcherOn())
                    {
                        LagSpikeWatcher.stopWatcher();
                        sendChat("TC: watcher turned off");
                    }
                    else
                        sendChat("TC: watcher is currently off");
                }),
            new Command("setThreshold",
                args ->
                {
                    int ms = getInt(args, 0, 17);
                    ms = Utils.clamp(ms, 1, 1000);
                    int t = LagSpikeWatcher.setThreshold(ms);
                    sendChatf("TC: set threshold to %d ms (%.1f fps)", ms, 1000.0 / ms);
                }),
            new Command("setThresholdFps",
                args ->
                {
                    int fps = getInt(args, 0, 60);
                    fps = Utils.clamp(fps, 1, 1000);
                    int t = LagSpikeWatcher.setThreshold(1000 / fps);
                    sendChatf("TC: set threshold to %d fps (%d ms)", fps, 1000 / fps);
                }),
            new Command("dump",
                args ->
                {
                    if (LagSpikeWatcher.isWatcherOn())
                    {
                        File file = LagSpikeWatcher.dump();
                        if (file != null)
                            McUtils.getPlayer().addChatMessage(new ChatComponentTranslation("Output written to file %s",
                                McUtils.makeFileLink(file)));
                        else
                            sendChat("Cannot create file");
                    }
                    else
                        sendChat("TC: watcher is currently off");
                }),
            new Command("fakeLag",
                args ->
                {
                    try
                    {
                        int ms = args.length > 0 ? toInt(args[0]) : 1000;
                        Thread.sleep(ms);
                        sendChat("TC: slept the thread for " + ms + " ms");
                    }
                    catch (InterruptedException e)
                    {
                        sendChat("TC: interrupted");
                    }
                }),
            new Command("keepLogging",
                args ->
                {
                    boolean b = LagSpikeWatcher.toggleKeepLoggingOnLag();
                    sendChat("TC: toggled keep logging on lag " + b);
                }),
            new Command("dumpThreads",
                args ->
                {
                    File file = LagSpikeWatcher.dumpThreads();
                    if (file != null)
                        McUtils.getPlayer().addChatMessage(new ChatComponentTranslation("Output written to file %s",
                            McUtils.makeFileLink(file)));
                    else
                        sendChat("Cannot create file");
                }))
        );
    }
    
    @RunInCtor
    private void guildBridge()
    {
        addSub(new Command("guildBridge",
            args -> Tweakception.guildBridge.status(),
            new Command("connent",
                args ->
                {
                    if (args.length >= 1)
                        Tweakception.guildBridge.connect(toLong(args[0]));
                    else
                        sendChat("Need 1 arg");
                }),
            new Command("disconnect",
                args -> Tweakception.guildBridge.disconnect()),
            new Command("setLevenshteinThreshold",
                args -> Tweakception.guildBridge.setLevenshteinThreshold(args.length > 0 ? toInt(args[0]) : -1))
        ));
    }
    
    @RunInCtor
    private void setYaw()
    {
        addSub(new Command("setYaw",
            args ->
            {
                if (args.length > 0)
                    McUtils.getPlayer().rotationYaw = Utils.clamp(toFloat(args[0]), -180, 180);
            }));
    }
    
    @RunInCtor
    private void setPitch()
    {
        addSub(new Command("setPitch",
            args ->
            {
                if (args.length > 0)
                    McUtils.getPlayer().rotationPitch = Utils.clamp(toFloat(args[0]), -90, 90);
            }));
    }
    
    @RunInCtor
    private void dev()
    {
        addSub(new Command("dev",
            args -> Tweakception.globalTweaks.toggleDevMode(),
            new Command("set",
                args -> DevSettings.toggle(getString(args, 0, "")),
                args -> Arrays.stream(DevSettings.class.getDeclaredFields())
                    .filter(field -> field.getType() == boolean.class)
                    .map(Field::getName)
                    .collect(Collectors.toList())
            ).setVisibility(false),
            new Command("dumpPlayerInfoMap",
                args -> DumpUtils.dumpPlayerInfoMap()
            ).setVisibility(false)
        ));
    }
    
    @RunInCtor
    private void clientSetBlock()
    {
        addSub(new Command("clientSetBlock",
            args ->
            {
                if (args.length == 1)
                    getWorld().setBlockState(McUtils.getPlayer().getPosition(),
                        Block.blockRegistry.getObject(new ResourceLocation(args[0])).getDefaultState(),
                        1);
                else
                    sendChat("Give me 1 arg");
            }).setVisibility(false));
    }
    
    @RunInCtor
    private void dumpEntitiesInRange()
    {
        addSub(new Command("dumpEntitiesInRange",
            args -> DumpUtils.dumpEntitiesInRange(getWorld(), McUtils.getPlayer(),
                args.length > 0 ? toDouble(args[0]) : 5.0)
        ).setVisibility(false));
    }
    
    @RunInCtor
    private void lookTrace()
    {
        addSub(new Command("lookTrace",
            args ->
            {
                // /tc looktrace reach adjacent liquid
                // /tc looktrace 5.0   false    false
                DumpUtils.doLookTrace(getWorld(), McUtils.getPlayer(),
                    args.length >= 1 ? toDouble(args[0]) : 5.0,
                    args.length >= 2 && args[1].equals("true"),
                    args.length >= 3 && args[2].equals("true"));
            }).setVisibility(false));
    }
    
    @RunInCtor
    private void t()
    {
        addSub(new Command("t",
            args ->
            {
                GlobalTweaks.t = !GlobalTweaks.t;
                sendChat("t = " + GlobalTweaks.t);
            }).setVisibility(false));
    }
    
    @RunInCtor
    private void action()
    {
        addSub(new Command("action",
            args ->
            {
                if (args.length == 1)
                    Tweakception.globalTweaks.doChatAction(args[0]);
                else
                    sendChat("Don't use this command!");
            }));
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    private @interface RunInCtor { }
    
    @Override
    public String getCommandName()
    {
        return "tc";
    }
    
    @Override
    public String getCommandUsage(ICommandSender icommandsender)
    {
        return "Use the tab completion or read the source code!";
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
            {
                if (sub.isVisible() && args[0].equalsIgnoreCase(sub.getName()))
                {
                    sub.processCommands(Arrays.copyOfRange(args, 1, args.length));
                    return;
                }
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
            return Command.getPossibleCompletions(args[0], getVisibleSubCommandNames());
        else
            for (Command sub : subCommands)
                if (sub.isVisible() && args[0].equalsIgnoreCase(sub.getName()))
                    return sub.getTabCompletions(Arrays.copyOfRange(args, 1, args.length));
        return null;
    }
    
    private List<String> getVisibleSubCommandNames()
    {
        return subCommands.stream()
            .filter(Command::isVisible)
            .map(Command::getName)
            .collect(Collectors.toList());
    }
    
    private void addSub(Command cmd)
    {
        subCommands.add(cmd);
    }
    
    private static void sendCommandNotFound()
    {
        sendChat("Tweakception: command not found or wrong syntax");
    }
    
    private static String getString(String[] args, int index, String defaultWhenNoArg)
    {
        return index < args.length ? args[index] : defaultWhenNoArg;
    }
    
    private static int getInt(String[] args, int index, int defaultWhenNoArg)
    {
        return index < args.length ? toInt(args[index]) : defaultWhenNoArg;
    }
    
    private static long getLong(String[] args, int index, long defaultWhenNoArg)
    {
        return index < args.length ? toLong(args[index]) : defaultWhenNoArg;
    }
    
    private static float getFloat(String[] args, int index, float defaultWhenNoArg)
    {
        return index < args.length ? toFloat(args[index]) : defaultWhenNoArg;
    }
    
    private static double getDouble(String[] args, int index, double defaultWhenNoArg)
    {
        return index < args.length ? toDouble(args[index]) : defaultWhenNoArg;
    }
    
    private static int toInt(String s)
    {
        return Integer.parseInt(s.replaceAll(",$", ""));
    }
    
    private static long toLong(String s)
    {
        return Long.parseLong(s.replaceAll(",$", ""));
    }
    
    private static float toFloat(String s)
    {
        return Float.parseFloat(s.replaceAll(",$", ""));
    }
    
    private static double toDouble(String s)
    {
        return Double.parseDouble(s.replaceAll(",$", ""));
    }
}
