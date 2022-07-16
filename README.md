# Tweakception

config path - .minecraft/config/tweakception/config.json

## TODO
- Particle settings profile
- Gui editor

## Modules/commands

###### *(ins)* - Setting is per instance, does not save to config
###### *(dev)* - Requires dev mode to use

### DungeonTweaks
- dungeon autoclosesecretchest - **This doesn't auto close chests with treasure talisman**
- dungeon autojoinparty
- dungeon autojoinparty add `string name`
- dungeon autojoinparty list
- dungeon autojoinparty remove `string name`
- dungeon autosalvage - **Automatically presses salvage button when there's a trash mob drop in the slot**
- dungeon blockopheliaclicks
- dungeon blockrightclick list
- dungeon blockrightclick remove `int index`
- dungeon blockrightclick set
  - **Toggles right click blocking for the current hotbar item, for example shadow fury**
  - **Note that it only checks the uncolored item name**
  - **Hold left alt to bypass**
- dungeon dailyruns `[string name]` - **Displays the daily run count**
- dungeon displaymobnametag
  - **Displays the name of the armorstand closest to the mob that took the most hits in last 3 seconds**
- dungeon frag - **Lists frag drop counts, it only logs when frag tracking is on**
- dungeon frag endsession
- dungeon frag next - **Warps to dhub and reparty, and starts the next run**
- dungeon frag setfragbot `[string name]` - **Sets or removes the player to reparty with**
- dungeon frag startsession - **Starts frag tracking**
- dungeon frag stats
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
### CrimsonTweaks
- crimson map - **Toggles map**
- crimson map markerscale `float scale`
- crimson map pos `int x` `int y`
- crimson map scale `float scale`
- crimson sulfur - **Toggles sponge highlight**
### MiningTweaks
- mining highlightchests - **Toggles all chests highlighting in crystal hollows**
### GlobalTracker
- gt blockquickcraft - **Toggles quick craft whitelist, press left alt on the item to (un)whitelist it**
- gt blockquickcraft remove `[int index]` - **Removes an item from the whitelist, leave empty to see the list**
- gt copylocation - **Copies raw current location to clipboard**
- gt entertoclosesign
  - **Closes number typing signs when enter is pressed, hold left alt to override**
  - **It only activates if the second line equals "^^^^^^^^^^^^^^^"**
- gt forcesetisland `[string name...]` - **Sets or removes location override**
- gt hideplayers - **Hides ALL online players**
- gt highlightshinypigs
- gt highlightshinypigs setname - **Example:** `setname [vip] alan72104`
- gt island - **Prints current location**
- gt renderinvisiblearmorstands
- gt renderinvisibleenities
- gt setinvisibleentityalphapercentage `int percentage`
- gt skipworldrendering
- gt usefallbackdetection - **Toggles slightly slower location detection**
- gt rightctrlcopy nbt *(dev)* *(ins)*
- gt rightctrlcopy tooltip *(dev)* *(ins)*
### SlayerTweaks
- slayer autohealwand
- slayer autohealwand setthreshold `[int percent]` - **Defaults to 50%**
- slayer autothrowfishingrod *(ins)* - **Automatically throws fishing rod in hotbar when slayer boss hp goes below a threshold percent**
- slayer autothrowfishingrod setthreshold `[int percent]` - **Defaults to 20%**
- slayer eman highlightglyph
- slayer highlightslayerminiboss
- slayer highlightslayers
- slayer playercount park - **Displays the "loaded" player count in the slayer area of park in the current world**
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
- fairy setdelay `int ticks` - **Sets auto scanning delay**
- fairy setnotfound - **Sets nearest fairy soul as not found**
- fairy toggleauto - **Toggles auto scanning**
- fairy trackonce  - **Scans loaded entities for fairy soul**
### APIManager
- api set `string key`
- api clearcaches
- api copyprofile `[string playerName]` *(dev)* - **Copies the player info of the most recently used profile**
- api debug *(dev)* - **Toggles request printing**
- api printcaches *(dev)*
### AutoFish
- autofish - **Toggles auto fish**
- autofish setcatchestomove `[int min]` `[int max]`
- autofish setheadmovingpitchrange `[float range]`
- autofish setheadmovingticks `[int ticks]`
- autofish setheadmovingyawrange `[float range]`
- autofish setrecastdelay `[int min]` `[int max]`
- autofish setretrievedelay `[int min]` `[int max]` - **Leave 1 or more empty to reset**
- autofish toggledebug - **Toggles variable showing on screen**
- autofish toggleslugfish - **Toggles waiting for 30 secs before setting the hook**
<br> </br>
## Dev commands
- dev - **Toggles dev mode**
  - **Press right ctrl to copy the nbt of current hovered item in a container, or double press to save to file**
  - **Enables dev commands' usage**
- clientsetblock `string blockName`
  - **Sets the block at foot level**
  - **blockName is the registry name in net.minecraft.init.Blocks**
- dumpentityinrange `[double range]`
  - **The bound is a box** 
  - **Useful for 0 size armor stands**
- looktrace `[double reach]` `[bool adjacent]` `[bool liquid]` 
  - **Does looktrace for block/entity and dumps data to file**
  - **If result is entity then all hit entities are dumped (sorted by distance)**
  - **Don't click the file link you will dead freeze**
- notifylagspike *(ins)* - **Toggles lag spike notifying**
- notifylagspike setaggregation `[float aggregation]` *(ins)*
  - **Sets the aggregation of average tick time, defaults to 0.4 new**
- notifylagspike setthreshold `[float threshold]` *(ins)*
  - **Sets the threshold at which to notify, defaults to 1000.0x**
