# Tweakception

config path - .minecraft/config/tweakception/config.json

## TODO
- Particle settings profile
- Gui editor
- Dungeon key lightlight (armorstand data below)
```
Equipment:[
  ...,
  4:{
    id:"minecraft:skull",
    Count:1b,
    tag:{
        SkullOwner:{
            Id:"8ad39a3a-2fc8-44d0-adfc-7824b1e0f802",
            hypixelPopulated:1b,
            Properties:{
                textures:[
                    0:{
                        Value:"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjU2MTU5NWQ5Yzc0NTc3OTZjNzE5ZmFlNDYzYTIyMjcxY2JjMDFjZjEwODA5ZjVhNjRjY2IzZDZhZTdmOGY2In19fQ=="}]}},
        display:{
            Name:"BOSS"}},
    Damage:3s
  }
]
```

## Modules/commands
### DungeonTweaks
- dungeon nofog - **Toggles no fog, for blindness**
- dungeon nofog auto - **Toggles no fog auto toggle on f5 entering/leaving**
- dungeon hidename - **Toggles hide non starred mobs name**
- dungeon highlightstarredmobs
- dungeon highlightbats - **(all bats)**
- dungeon highlightspiritbear
- dungeon highlightshadowsssassin
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
- dungeon autosalvage - **Toggles salvage button auto pressing when there's a trash mob drop in the slot**
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
  - **Toggles shooting speed tracker**
  - **Arrows that spawn in 4 blocks is added to the counter, and removed after 2 seconds**
- dungeon trackshootingspeed setsamplesecs `[int secs]` - **Leave empty to reset**
- dungeon trackshootingspeed setspawnrange `[int blocks]`
### CrimsonTweaks
- crimson map - **Toggles map**
- crimson map pos `int x` `int y`
- crimson map scale `float scale`
- crimson map markerscale `float scale`
- crimson sulfur - **Toggles sponge highlight, very laggy**
### MiningTweaks
- mining highlightchests - **Toggles all chests highlighting in crystal hollows**
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
- gt forcesetisland `[string name...]` - **Sets or removes location override**
- gt copylocation - **Copies raw current location to clipboard**
- gt usefallbackdetection - **Toggles slightly slower location detection**
<br> </br>
## Dev commands
- dev - **Toggles dev mode**
  - **Press right ctrl to copy the nbt of current hovered item in container to clipboard**
  - **Enables dev commands' tab completion**
- tracelook `[double reach]` `[bool adjacent]` `[bool liquid]` 
  - **Does looktrace for block/entity and dumps data to file**
  - **If result is entity then all hit entities are dumped (sorted by distance)**
  - **Don't click the file link you will dead freeze**
- dumpentityinrange `[double range]`
  - **The bound is a box** 
  - **Useful for 0 size armor stands**
- clientsetblock `string blockName`
  - **Sets the block at foot level**
  - **blockName is the registry name in net.minecraft.init.Blocks**
- trackticktime
  - **Toggles tick times tracking, in microseconds**
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