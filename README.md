# Tweakception

Config path - .minecraft/config/tweakception/config.json

## TODO
- Particle settings profile
- Gui editor

## Modules/commands

###### *(ins)* - Setting is per instance, does not save to config
###### *(dev)* - Requires dev mode to use
All command prefix - /tc

### DungeonTweaks
- dungeon autoclosesecretchest - **This doesn't auto close chests with treasure talisman**
- dungeon autojoinparty
- dungeon autojoinparty add `string name`
- dungeon autojoinparty list
- dungeon autojoinparty remove `string name`
- dungeon autojoinparty togglewhitelist
- dungeon autosalvage - **Automatically presses salvage button when there's a trash mob drop in the slot**
- dungeon autoswapsceptreaote
  - **Combines AOTE and Spirit Sceptre into one item (auto swap)**
  - **Right click for sceptre, left click or shift + left for aote tp**
  - **Left click works like usual when right button is hold**
- dungeon blockopheliaclicks
- dungeon blockrightclick list
- dungeon blockrightclick remove `int index`
- dungeon blockrightclick set
  - **Toggles right click blocking for the current hotbar item, for example shadow fury**
  - **Note that it only checks the uncolored item name**
  - **Hold left alt to bypass**
- dungeon dailyruns `[string name]` - **Displays the daily run count (for other player)**
- dungeon displaymobnametag
  - **Displays the name of the armorstand closest to the mob that took the most hits in last 3 seconds**
  - **Shit btw**
- dungeon frag - **Lists frag drop counts, it only logs when frag tracking is on**
- dungeon frag autoreparty
- dungeon frag endsession
- dungeon frag next - **Warps to dhub and reparty, and starts the next run**
- dungeon frag setfragbot `[string name]` - **Sets or removes the player to reparty with**
- dungeon frag startsession - **Starts frag tracking**
- dungeon frag stats - **Placeholder**
- dungeon gyrowandoverlay
- dungeon hidedamagetags - **Hides any damage tags in everywhere**
- dungeon hidename - **Hides non starred mobs name in dungeon**
- dungeon highlightbats - **(all bats)**
- dungeon highlightdoorkeys - **Shows a beacon beam at where the door key is**
- dungeon highlightshadowsssassin
- dungeon highlightspiritbear
- dungeon highlightstarredmobs
- dungeon nofog - **Toggles no fog, for blindness**
- dungeon nofog auto - **Toggles no fog auto toggle on f5/f7 entering/leaving**
- dungeon partyfinder blacklist `[string name]` `[string reason...]`
  - **Adds or removes the player from the blacklist, blacklisted players will be displayed in dark red strikethrough, hold left ctrl to see the reason**
  - **Leave empty to see the list**
- dungeon partyfinder clearcaches
- dungeon partyfinder quickplayerinfo
  - **Displays quick cata/secret/item info of every player in party tooltip**
  - **Format: <cata> | <secrets per run/total secrets/secrets per 50k exp(optional)> WBlade? Term?**
  - **[35.70 | 11.2/18k/27.1 WBladex2 Term]**
  - **[35.70 | 11.2/18k/27.1 API disabled]** 
- dungeon partyfinder quickplayerinfo secretperexp
- dungeon partyfinder refreshcooldown - **Toggles the refresh cooldown display, blocks click if in cooldown**
- dungeon trackdamage - **Toggles overall damage tag tracking, crit is always enabled**
- dungeon trackdamage noncrit - **Toggles non crit tracking, includes damages to you**
- dungeon trackdamage setcount `int count`
- dungeon trackdamage sethistorytimeout `int ticks` - **Sets damage history timeout ticks**
- dungeon trackdamage wither
- dungeon trackmask
  - **Shows a X under the used masks in inventory,**
  - **also sends a reminder when it becomes available**
- dungeon trackshootingspeed
- dungeon trackshootingspeed setsamplesecs `[int secs]` - **Leave empty or 0/-1 to reset**
- dungeon trackshootingspeed setspawnrange `[int blocks]`
- next - **An alias for `dungeon frag next`**
- dailies - **An alias for `dungeon dailyruns`**
### CrimsonTweaks
- crimson map - **Toggles map**
- crimson map markerscale `float scale`
- crimson map pos `int x` `int y`
- crimson map scale `float scale`
- crimson sulfur - **Toggles sponge highlight**
### MiningTweaks
- mining highlightchests - **Toggles all chests highlighting in crystal hollows**
### GlobalTracker
- gt afkmode - **Limits fps when window is unfocused, optionally skips world rendering**
- gt afkmode fps `[int fpsLimit]`
- gt afkmode onlyunfocused
- gt afkmode skipworldrendering
- gt areaedit - **For area bounding box range finding**
- gt areaedit print
- gt areaedit reset
- gt areaedit setpoint `int index` `int x` `int y` `int z`
- gt areaedit setarea `int x1` `int y1` `int z1` `int x2` `int y2` `int z2`
- gt blockquickcraft - **Toggles quick craft whitelist, press left alt on the item to (un)whitelist it**
- gt blockquickcraft remove `[int index]` - **Removes an item from the whitelist, leave empty to see the list**
- gt championoverlay
- gt championoverlay incrementresetdelay `[int secs]` - **Sets the increment number reset duration, leave blank to reset**
- gt copylocation - **Copies raw current location to clipboard**
- gt disabletooltips
- gt drawselectedentityoutline
- gt drawselectedentityoutline width `[float w]`
- gt drawselectedentityoutline color `[int r]` `[int g]` `[int b]` `[int a]`
- gt entertoclosesign
  - **Closes number typing signs when enter is pressed, hold left alt to override**
  - **It only activates if the second line equals "^^^^^^^^^^^^^^^"**
- gt gift
  - **Toggles all gift features**
  - **In inventory, press D to drop all gift trash, Ctrl+D to throw trash plus emptying the stash, M to move gifts to hotbar**
- gt gift autoswitchgiftslot
  - **Auto switches to slot that has any gift on any entity right click**
- gt hideplayers - **Hides ALL online players**
- gt highlightshinypigs
- gt highlightshinypigs setname - **Example:** `setname [vip] alan72104`
- gt highlightplayer `[string name]` - **Leave blank to clear list**
- gt highlightskulls - **Highlights all skull blocks**
- gt island - **Prints current location**
- gt minionautoclaim *(ins)*
- gt minionautoclaim whitelist `string id`
- gt minionautoclaim whitelist remove `string id`
- gt minionautoclaim setdelay `[int minTicks]`
- gt renderinvisiblearmorstands
- gt renderinvisibleenities
- gt renderenchantedbookstype
- gt rendersackstype
- gt renderpotiontier
- gt setinvisibleentityalphapercentage `[int percentage]`
- gt sendbitsmessage - **Sends bits gaining message to chat**
- gt skipworldrendering - **Reduces cpu usage when afk**
- gt targeting disabledeadmobtargeting - **Best paired with `drawselectedentityoutline`**
- gt targeting disablearmorstandtargeting
- gt targeting onlytargetopenablegifts
- gt targeting reset
- gt tooltip id - **Adds item id to tooltip**
- gt trevor autoaccept
- gt trevor autostart - **Requires abiphone in hotbar**
- gt trevor highlightanimal
- gt usefallbackdetection - **Toggles slightly slower location detection**
- gt onlinestatusoverlay - **To track status, normally ignores online**
- gt onlinestatusoverlay showalreadyon
- gt ping
- gt ping overlay
- gt playercount park
- gt playercount crimson stronghold back topright
- gt playercount crimson stronghold front topright
- gt playersinareas - **Overlay, displays loaded players in implemented areas**
- gt setisland `[string name...]` - **Sets or removes location override**
- gt logpackets *(dev)* *(ins)* - **Logs packets to a file**
- gt logpackets setallowed *(dev)* *(ins)* `string className`
- gt logpackets logall *(dev)* *(ins)* - **Doubt anyone needs**
- gt rightctrlcopy nbt *(dev)* *(ins)*
- gt rightctrlcopy tooltip *(dev)* *(ins)*
- gt rightctrlcopy tooltipfinal *(dev)* *(ins)*
### SlayerTweaks
- slayer autohealwand
- slayer autohealwand setthreshold `[int percent]` - **Defaults to 50%**
- slayer autothrowfishingrod *(ins)* - **Automatically throws fishing rod in hotbar when slayer boss hp goes below a threshold percent**
- slayer autothrowfishingrod setthreshold `[int percent]` - **Defaults to 20%**
- slayer eman highlightglyph
- slayer highlightslayerminiboss~~~~
- slayer highlightslayers
### TuningTweaks
- tuning clickdelayticks `[int ticks]` - **Defaults to 5**
- tuning toggletemplate
  - **Toggles the custom tuning templates**
  - **Shift + left click to apply template**
  - **Shift + right click to set template**
  - **Ctrl + left click to remove template**
### FairyTracker
- fairy - **Toggles fairy scanner**
- fairy count
- fairy dump - **Dumps to clipboard and console**
- fairy import - **Imports from clipboard**
- fairy list
- fairy reset - **Resets without warning**
- fairy setnotfound - **Sets nearest fairy soul as not found**
- fairy toggletracking
- fairy togglegift - **Toggles gift highlight separately**
### OverlayManager
- overlay - **Edits overlays**
### APIManager
- api set `string key`
- api clearcaches
- api copyprofile `[string playerName]` *(dev)* - **Copies the player info of the most recently used profile**
- api debug *(dev)* - **Toggles request printing**
- api printcaches *(dev)*
### AutoFish
- fish *(dev)* - **Toggles auto fish**
- fish setcatchestomove `[int min]` `[int max]` *(dev)*
- fish setheadmovingpitchrange `[float range]` *(dev)*
- fish setheadmovingticks `[int ticks]` *(dev)*
- fish setheadmovingyawrange `[float range]` *(dev)*
- fish setrecastdelay `[int min]` `[int max]` *(dev)*
- fish setretrievedelay `[int min]` `[int max]` *(dev)* - **Leave 1 or more empty to reset**
- fish toggledebug *(dev)* - **Toggles variable showing on screen**
- fish slugfish *(dev)* - **Toggles 30 secs waiting before setting the hook**
- fish thunderbottleoverlay *(dev)*
- fish thunderbottleoverlay incrementresetdelay `[int secs]` *(dev)*
### ForagingTweaks
- foraging tree - **Toggles grown dark oak tree highlight**
- foraging debug `[int connectedCount = 15]`- **Prints connected dark oak blocks search result**
### BazaarTweaks
- **When hovering on Sell Offer/Buy Order items, displays self offers/orders in the tooltip**
- **View order list to cache orders, data is kept for 60 secs**
- bazaar printorders - **Prints all cached orders**
### LagSpikeWatcher
- lagspikewatcher start *(ins)*
  - **Watches main thread for lag spikes, logs stack when threshold is reached (fml-client-latest.log)**
- lagspikewatcher stop
- lagspikewatcher setthreshold `[int ms = 500]`
- lagspikewatcher dump - **Dumps all stack record sorted by count to a file**
- lagspikewatcher fakelag `[int ms = 1000]` - **Sleeps thread**
- lagspikewatcher keeplogging - **Keeps logging in a long lag**
- lagspikewatcher dumpthreads - **Dumps all JVM threads and their stacks to a file**
<br> </br>
## Dev commands
- dev - **Toggles dev mode**
  - **Press right ctrl to copy the nbt of current hovered item in a container, or double press to save to file**
  - **Enables dev commands' usage**
- clientsetblock *(dev)* `string blockName`
  - **Sets the block at foot level**
  - **blockName is the registry name in net.minecraft.init.Blocks**
- dumpentityinrange *(dev)* `[double range]`
  - **The bound is a box** 
  - **Useful for 0 size armor stands**
- looktrace *(dev)* `[double reach]` `[bool adjacent]` `[bool liquid]` 
  - **Does looktrace for block/entity and dumps data to file**
  - **If result is entity then all hit entities are dumped (sorted by distance)**
  - **Don't click the file link you will dead freeze**
- notifylagspike *(dev)* *(ins)* - **Toggles long tick notifying**
- notifylagspike setaggregation `[float aggregation]` *(dev)* *(ins)*
  - **Sets the aggregation of average tick time, defaults to 0.4 new**
- notifylagspike setthreshold `[float threshold]` *(dev)* *(ins)*
  - **Sets the threshold at which to notify, defaults to 1000.0x**
