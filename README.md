# Tweakception

config path - .minecraft/config/tweakception/config.json

## TODO
- Particle settings profile
- Gui editor

## Modules/commands

###### *(ins)* - Setting is per instance, does not save to config
###### *(dev)* - Requires dev mode to use

### DungeonTweaks
- dungeon nofog - **Toggles no fog, for blindness**
- dungeon nofog auto - **Toggles no fog auto toggle on f5 entering/leaving**
- dungeon hidename - **Hides non starred mobs name in dungeon**
- dungeon hidedamagetags - **Hides any damage tags in everywhere**
- dungeon highlightstarredmobs
- dungeon highlightbats - **(all bats)**
- dungeon highlightspiritbear
- dungeon highlightshadowsssassin
- dungeon highlightdoorkeys - **Shows a beacon beam at where the door key is**
- dungeon blockrightclick set
  - **Toggles right click block for the current hotbar item, for items like shadow fury**
  - **Note that it only checks the item name**
  - **Hold left alt to bypass**
- dungeon blockrightclick list
- dungeon trackdamage - **Toggles overall damage tag tracking, crit is always enabled**
- dungeon trackdamage setcount `int count`
- dungeon trackdamage sethistorytimeout `int ticks` - **Sets damage history timeout ticks**
- dungeon trackdamage noncrit - **Toggles non crit tracking, includes damages to you**
- dungeon trackdamage wither
- dungeon autoclosesecretchest - **This doesn't auto close chests with treasure talisman**
- dungeon autosalvage - **Automatically presses salvage button when there's a trash mob drop in the slot**
- dungeon autojoinparty
- dungeon autojoinparty list
- dungeon autojoinparty add `string name`
- dungeon autojoinparty remove `string name`
- dungeon frag - **Lists frag drop counts, it only logs when frag tracking is on**
- dungeon frag next - **Warps to dhub and reparty, and starts the next run**
- dungeon frag startsession - **Starts frag tracking**
- dungeon frag endsession
- dungeon frag setfragbot `[string name]` - **Sets or removes the player to reparty with**
- dungeon frag stats
- next - **An alias for `dungeon frag next`**
- dungeon trackshootingspeed
- dungeon trackshootingspeed setsamplesecs `[int secs]` - **Leave empty or 0/-1 to reset**
- dungeon trackshootingspeed setspawnrange `[int blocks]`
- dungeon displaymobnametag
  - **Displays the name of the armorstand closest to the mob that took the most hits in last 3 seconds**
- dungeon trackmask
  - **Shows a X under the used masks in inventory,**
  - **also sends a reminder when it becomes available**
- dungeon blockopheliaclicks
- dungeon partyfinder quickplayerinfo
  - **Displays quick cata/secret/item info of every player in party tooltip**
  - **Format: <cata> | <secrets per run/total secrets/secrets per 50k exp(optional)> WBlade? Term?**
  - **[35.70 | 11.2/18k/27.1 WBladex2 Term]**
  - **[35.70 | 11.2/18k/27.1 API disabled]** 
- dungeon partyfinder quickplayerinfo secretperexp
- dungeon partyfinder blacklist `[string name]` `[string reason...]`
  - **Adds or removes the player from the blacklist, blacklisted players will be displayed in dark red strikethrough, hold left ctrl to see the reason**
  - **Leave empty to see the list**
- dungeon clearcaches
### CrimsonTweaks
- crimson map - **Toggles map**
- crimson map pos `int x` `int y`
- crimson map scale `float scale`
- crimson map markerscale `float scale`
- crimson sulfur - **Toggles sponge highlight**
### MiningTweaks
- mining highlightchests - **Toggles all chests highlighting in crystal hollows**
### SlayerTweaks
- slayer autothrowfishingrod *(ins)* - **Automatically throws fishing rod in hotbar when slayer boss hp goes below a threshold percent**
- slayer autothrowfishingrod setthreshold `[int percent]` - **Defaults to 15%**
- slayer eman highlightglyph
### FairyTracker
- fairy - **Toggles fairy scanner**
- fairy trackonce  - **Scans loaded entities for fairy soul**
- fairy toggleauto - **Toggles auto scanning**
- fairy setdelay `int ticks` - **Sets auto scanning delay**
- fairy setnotfound - **Sets nearest fairy soul as not found**
- fairy count
- fairy list
- fairy dump - **Dumps to clipboard and console**
- fairy import - **Imports from clipboard**
- fairy reset - **Resets without warning**
### GlobalTracker
- gt island - **Prints current location**
- gt forcesetisland `[string name...]` - **Sets or removes location override**
- gt copylocation - **Copies raw current location to clipboard**
- gt usefallbackdetection - **Toggles slightly slower location detection**
- gt highlightshinypigs
- gt highlightshinypigs setname - **Example:** `setname [vip] alan72104`
- gt hideplayers - **Hides ALL online players**
- gt entertoclosesign
  - **Closes number typing signs when enter is pressed, hold left alt to override**
  - **It only activates if the second line equals "^^^^^^^^^^^^^^^"**
- gt renderinvisibleenities
- gt renderinvisiblearmorstands
- gt setinvisibleentityalphapercentage `int percentage`
- gt skipworldrendering
- gt rightctrlcopy nbt *(dev)* *(ins)*
- gt rightctrlcopy tooltip *(dev)* *(ins)*
### APIManager
- api set `string key`
- api clearcaches
- api debug *(dev)* - **Toggles request printing**
- api copyprofile `[string playerName]` *(dev)* - **Copies/gets the player info of the most recently used profile**
<br> </br>
## Dev commands
- dev - **Toggles dev mode**
  - **Press right ctrl to copy the nbt of current hovered item in a container, or double press to save to file**
  - **Enables dev commands' usage**
- looktrace `[double reach]` `[bool adjacent]` `[bool liquid]` 
  - **Does looktrace for block/entity and dumps data to file**
  - **If result is entity then all hit entities are dumped (sorted by distance)**
  - **Don't click the file link you will dead freeze**
- dumpentityinrange `[double range]`
  - **The bound is a box** 
  - **Useful for 0 size armor stands**
- clientsetblock `string blockName`
  - **Sets the block at foot level**
  - **blockName is the registry name in net.minecraft.init.Blocks**
- notifylagspike *(ins)* - **Toggles lag spike notifying**
  - setthreshold `[float threshold]` *(ins)* - **Sets the threshold at which to notify, defaults to 1000.0x**
  - setaggregation `[float aggregation]` *(ins)* - **Sets the aggregation of average tick time, defaults to 0.4 new**
### AutoFish
- autofish - **Toggles auto fish**
- autofish setretrievedelay `[int min]` `[int max]` - **Leave 1 or more empty to reset**
- autofish setrecastdelay `[int min]` `[int max]`
- autofish setcatchestomove `[int min]` `[int max]`
- autofish setheadmovingticks `[int ticks]`
- autofish setheadmovingyawrange `[float range]`
- autofish setheadmovingpitchrange `[float range]`
- autofish toggledebug - **Toggles variable showing on screen**
- autofish toggleslugfish - **Toggles waiting for 30 secs before setting the hook**