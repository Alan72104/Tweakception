package a7.tweakception.utils;

import net.minecraft.util.BlockPos;

import java.util.Map;

public class Constants
{
    public static final long[] CATACOMBS_LEVEL_EXPS =
    {
        50, 75, 110, 160, 230,
        330, 470, 670, 950, 1340,
        1890, 2665, 3760, 5260, 7380,
        10300, 14400, 20000, 27600, 38000,
        52500, 71500, 97000, 132000, 180000,
        243000, 328000, 445000, 600000, 800000,
        1065000, 1410000, 1900000, 2500000, 3300000,
        4300000, 5600000, 7200000, 9200000, 12000000,
        15000000, 19000000, 24000000, 30000000, 38000000,
        48000000, 60000000, 75000000, 93000000, 116250000
    };
    
    public static final long[] CHAMPION_EXPS =
    {
        0, 50000, 100000, 250000, 500000,
        1000000, 1500000, 2000000, 2500000, 3000000
    };
    
    public static final String[] ROMAN_NUMERALS =
    {
        "I", "II", "III", "IV", "V",
        "VI", "VII", "VIII", "IX", "X"
    };
    
    public static final BlockPos[] PARK_DARK_TREES =
    {
        new BlockPos(-317, 103, -78),
        new BlockPos(-322, 103, -62),
        new BlockPos(-325, 103, -100),
        new BlockPos(-330, 103, -39),
        new BlockPos(-331, 106, -79),
        new BlockPos(-336, 103, -108),
        new BlockPos(-338, 107, -64),
        new BlockPos(-340, 103, -94),
        new BlockPos(-345, 103, -43),
        new BlockPos(-346, 103, -28),
        new BlockPos(-349, 103, -84),
        new BlockPos(-355, 103, -116),
        new BlockPos(-364, 103, -21),
        new BlockPos(-365, 103, -78),
        new BlockPos(-369, 103, -59),
        new BlockPos(-372, 105, -39)
    };
    
    public static final BlockPos[] ARACHNES_KEEPER_LOCATIONS =
    {
        new BlockPos(-293, 47, -168),
        new BlockPos(-292, 47, -184),
        new BlockPos(-283, 47, -196),
        new BlockPos(-263, 49, -192),
        new BlockPos(-270, 47, -167),
        new BlockPos(-270, 61, -160),
        new BlockPos(-312, 43, -233),
        new BlockPos(-209, 44, -260),
        new BlockPos(-231, 57, -308)
    };
    
    public static final BlockPos[] SPIDERS_DEN_RELIC_LOCATIONS =
    {
        new BlockPos(-342, 122, -253),
        new BlockPos(-384, 89, -225),
        new BlockPos(-274, 100, -178),
        new BlockPos(-178, 136, -297),
        new BlockPos(-147, 83, -335),
        new BlockPos(-188, 80, -346),
        new BlockPos(-206, 63, -301),
        new BlockPos(-342, 89, -221),
        new BlockPos(-355, 86, -213),
        new BlockPos(-372, 89, -242),
        new BlockPos(-354, 73, -285),
        new BlockPos(-317, 69, -273),
        new BlockPos(-296, 37, -270),
        new BlockPos(-275, 64, -272),
        new BlockPos(-303, 71, -318),
        new BlockPos(-311, 69, -251),
        new BlockPos(-348, 65, -202),
        new BlockPos(-328, 50, -238),
        new BlockPos(-313, 58, -250),
        new BlockPos(-300, 51, -254),
        new BlockPos(-284, 49, -234),
        new BlockPos(-300, 50, -218),
        new BlockPos(-236, 51, -239),
        new BlockPos(-183, 51, -252),
        new BlockPos(-217, 58, -304),
        new BlockPos(-272, 48, -291),
        new BlockPos(-225, 70, -316),
        new BlockPos(-254, 57, -279)
    };
    
    public static final Map<String, String> ENCHANTS = MapBuilder.stringHashMap()
        .put("aiming", "Dragon Tracer")
        .put("angler", "Angler")
        .put("aqua_affinity", "Aqua Affinity")
        .put("bane_of_arthropods", "Bane of Arthropods")
        .put("big_brain", "Big Brain")
        .put("blast_protection", "Blast Protection")
        .put("blessing", "Blessing")
        .put("caster", "Caster")
        .put("chance", "Chance")
        .put("cleave", "Cleave")
        .put("counter_strike", "Counter-Strike")
        .put("critical", "Critical")
        .put("cubism", "Cubism")
        .put("charm", "Charm")
        .put("corruption", "Corruption")
        .put("delicate", "Delicate")
        .put("depth_strider", "Depth Strider")
        .put("dragon_hunter", "Dragon Hunter")
        .put("efficiency", "Efficiency")
        .put("ender_slayer", "Ender Slayer")
        .put("execute", "Execute")
        .put("experience", "Experience")
        .put("feather_falling", "Feather Falling")
        .put("fire_aspect", "Fire Aspect")
        .put("fire_protection", "Fire Protection")
        .put("first_strike", "First Strike")
        .put("flame", "Flame")
        .put("fortune", "Fortune")
        .put("frail", "Frail")
        .put("frost_walker", "Frost Walker")
        .put("ferocious_mana", "Ferocious Mana")
        .put("giant_killer", "Giant Killer")
        .put("growth", "Growth")
        .put("harvesting", "Harvesting")
        .put("hardened_mana", "Hardened Mana")
        .put("impaling", "Impaling")
        .put("infinite_quiver", "Infinite Quiver")
        .put("knockback", "Knockback")
        .put("lethality", "Lethality")
        .put("life_steal", "Life Steal")
        .put("looting", "Looting")
        .put("luck", "Luck")
        .put("luck_of_the_sea", "Luck Of The Sea")
        .put("lure", "Lure")
        .put("magnet", "Magnet")
        .put("mana_steal", "Mana Steal")
        .put("mana_vampire", "Mana Vampire")
        .put("overload", "Overload")
        .put("piercing", "Piercing")
        .put("power", "Power")
        .put("pristine", "Pristine")
        .put("projectile_protection", "Projectile Protection")
        .put("PROSECUTE", "Prosecute")
        .put("protection", "Protection")
        .put("punch", "Punch")
        .put("rainbow", "Rainbow")
        .put("rejuvenate", "Rejuvenate")
        .put("replenish", "Replenish")
        .put("respiration", "Respiration")
        .put("respite", "Respite")
        .put("scavenger", "Scavenger")
        .put("sharpness", "Sharpness")
        .put("silk_touch", "Silk Touch")
        .put("smarty_pants", "Smarty Pants")
        .put("smelting_touch", "Smelting Touch")
        .put("smite", "Smite")
        .put("snipe", "Snipe")
        .put("spiked_hook", "Spiked Hook")
        .put("sugar_rush", "Sugar Rush")
        .put("syphon", "Syphon")
        .put("smoldering", "Smoldering")
        .put("strong_mana", "Strong Mana")
        .put("telekinesis", "Telekinesis")
        .put("thorns", "Thorns")
        .put("thunderbolt", "Thunderbolt")
        .put("thunderlord", "Thunderlord")
        .put("titan_killer", "Titan Killer")
        .put("triple_strike", "Triple-Strike")
        .put("true_protection", "True Protection")
        .put("turbo_cactus", "Cacti")
        .put("turbo_cane", "Cane")
        .put("turbo_carrot", "Carrot")
        .put("turbo_coco", "Cocoa")
        .put("turbo_melon", "Melon")
        .put("turbo_mushrooms", "Mushroom")
        .put("turbo_potato", "Potato")
        .put("turbo_pumpkin", "Pumpkin")
        .put("turbo_warts", "Warts")
        .put("turbo_wheat", "Wheat")
        .put("vampirism", "Vampirism")
        .put("venomous", "Venomous")
        .put("vicious", "Vicious")
        // Ultimate
        .put("ultimate_bank", "Bank")
        .put("ultimate_chimera", "Chimera")
        .put("ultimate_combo", "Combo")
        .put("ultimate_reiterate", "Duplex")
        .put("ultimate_flash", "Flash")
        .put("ultimate_last_stand", "Last Stand")
        .put("ultimate_fatal_tempo", "Fatal Tempo")
        .put("ultimate_inferno", "Inferno")
        .put("ultimate_jerry", "Ultimate Jerry")
        .put("ultimate_legion", "Legion")
        .put("ultimate_no_pain_no_gain", "No Pain No Gain")
        .put("ultimate_one_for_all", "One For All")
        .put("ultimate_rend", "Rend")
        .put("ultimate_soul_eater", "Soul Eater")
        .put("ultimate_swarm", "Swarm")
        .put("ultimate_wise", "Ultimate Wise")
        .put("ultimate_wisdom", "Wisdom")
        .put("ultimate_habaneno_tactics", "Habaneno Tactics")
        // Stacking
        .put("compact", "Compact")
        .put("cultivating", "Cultivating")
        .put("expertise", "Expertise")
        .put("hecatomb", "Hecatomb")
        .put("champion", "Champion")
        .map();
}
