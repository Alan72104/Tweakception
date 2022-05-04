# Tweakception

config path - .minecraft/config/tweakception/config.json

## TODO
- Particle settings profile
- Gui editor

## Modules/commands
### DungeonTweaks
- dungeon hidename - **Toggles hide non starred mobs name**
- dungeon highlightstarredmobs - **Toggles highlight starred mobs**
- dungeon highlightbats - **Toggles highlight bats (*all bats*)**
- dungeon blockrightclick set
  - **Toggles right click block for the current hotbar item, for items like shadow fury**
  - **Note that it only checks the item name**
  - **Hold left alt to bypass**
- dungeon blockrightclick list
- dungeon trackdamage - **Toggles crit damage tag tracking**
- dungeon trackdamage setcount `int count`
### CrimsonTweaks
- crimson map - **Toggles map**
- crimson map pos `int x` `int y`
- crimson map scale `float scale`
- crimson map markerscale `float scale`
- crimson sulfur - **Toggles sponge highlight, very laggy**
### SlayerTweaks
- slayer eman highlightglyph - **Might be slightly laggy**
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
- gt forcesetisland - **Removes location override**
- gt forcesetisland `string names...`
- gt copylocation - **Copies current location to clipboard**
- gt useFallbackDetection - **Toggles slightly slower location detection**
<br> </br>
### Misc
- looktrace `[double reach]` `[bool adjacent]` `[bool liquid]` 
  - **Does looktrace for block/entity and dumps data to file**
  - **If result is entity then all hit entities are dumped (sorted by distance)**
  - **Don't click the file link you will dead freeze**
- dumpentityinrange `[double range]`
  - **The bound is a box** 
  - **Useful for 0 size armor stands**