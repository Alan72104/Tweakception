package a7.tweakception.utils;

import net.minecraft.util.BlockPos;

import java.util.HashMap;

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
    
    public static final HashMap<String, String> ENCHANTS = new HashMap<>();
    
    static
    {
        ENCHANTS.put("aiming", "Dragon Tracer");
        ENCHANTS.put("angler", "Angler");
        ENCHANTS.put("aqua_affinity", "Aqua Affinity");
        ENCHANTS.put("bane_of_arthropods", "Bane of Arthropods");
        ENCHANTS.put("big_brain", "Big Brain");
        ENCHANTS.put("blast_protection", "Blast Protection");
        ENCHANTS.put("blessing", "Blessing");
        ENCHANTS.put("caster", "Caster");
        ENCHANTS.put("chance", "Chance");
        ENCHANTS.put("cleave", "Cleave");
        ENCHANTS.put("counter_strike", "Counter-Strike");
        ENCHANTS.put("critical", "Critical");
        ENCHANTS.put("cubism", "Cubism");
        ENCHANTS.put("charm", "Charm");
        ENCHANTS.put("corruption", "Corruption");
        ENCHANTS.put("delicate", "Delicate");
        ENCHANTS.put("depth_strider", "Depth Strider");
        ENCHANTS.put("dragon_hunter", "Dragon Hunter");
        ENCHANTS.put("efficiency", "Efficiency");
        ENCHANTS.put("ender_slayer", "Ender Slayer");
        ENCHANTS.put("execute", "Execute");
        ENCHANTS.put("experience", "Experience");
        ENCHANTS.put("feather_falling", "Feather Falling");
        ENCHANTS.put("fire_aspect", "Fire Aspect");
        ENCHANTS.put("fire_protection", "Fire Protection");
        ENCHANTS.put("first_strike", "First Strike");
        ENCHANTS.put("flame", "Flame");
        ENCHANTS.put("fortune", "Fortune");
        ENCHANTS.put("frail", "Frail");
        ENCHANTS.put("frost_walker", "Frost Walker");
        ENCHANTS.put("ferocious_mana", "Ferocious Mana");
        ENCHANTS.put("giant_killer", "Giant Killer");
        ENCHANTS.put("growth", "Growth");
        ENCHANTS.put("harvesting", "Harvesting");
        ENCHANTS.put("hardened_mana", "Hardened Mana");
        ENCHANTS.put("impaling", "Impaling");
        ENCHANTS.put("infinite_quiver", "Infinite Quiver");
        ENCHANTS.put("knockback", "Knockback");
        ENCHANTS.put("lethality", "Lethality");
        ENCHANTS.put("life_steal", "Life Steal");
        ENCHANTS.put("looting", "Looting");
        ENCHANTS.put("luck", "Luck");
        ENCHANTS.put("luck_of_the_sea", "Luck Of The Sea");
        ENCHANTS.put("lure", "Lure");
        ENCHANTS.put("magnet", "Magnet");
        ENCHANTS.put("mana_steal", "Mana Steal");
        ENCHANTS.put("mana_vampire", "Mana Vampire");
        ENCHANTS.put("overload", "Overload");
        ENCHANTS.put("piercing", "Piercing");
        ENCHANTS.put("power", "Power");
        ENCHANTS.put("pristine", "Pristine");
        ENCHANTS.put("projectile_protection", "Projectile Protection");
        ENCHANTS.put("PROSECUTE", "Prosecute");
        ENCHANTS.put("protection", "Protection");
        ENCHANTS.put("punch", "Punch");
        ENCHANTS.put("rainbow", "Rainbow");
        ENCHANTS.put("rejuvenate", "Rejuvenate");
        ENCHANTS.put("replenish", "Replenish");
        ENCHANTS.put("respiration", "Respiration");
        ENCHANTS.put("respite", "Respite");
        ENCHANTS.put("scavenger", "Scavenger");
        ENCHANTS.put("sharpness", "Sharpness");
        ENCHANTS.put("silk_touch", "Silk Touch");
        ENCHANTS.put("smarty_pants", "Smarty Pants");
        ENCHANTS.put("smelting_touch", "Smelting Touch");
        ENCHANTS.put("smite", "Smite");
        ENCHANTS.put("snipe", "Snipe");
        ENCHANTS.put("spiked_hook", "Spiked Hook");
        ENCHANTS.put("sugar_rush", "Sugar Rush");
        ENCHANTS.put("syphon", "Syphon");
        ENCHANTS.put("smoldering", "Smoldering");
        ENCHANTS.put("strong_mana", "Strong Mana");
        ENCHANTS.put("telekinesis", "Telekinesis");
        ENCHANTS.put("thorns", "Thorns");
        ENCHANTS.put("thunderbolt", "Thunderbolt");
        ENCHANTS.put("thunderlord", "Thunderlord");
        ENCHANTS.put("titan_killer", "Titan Killer");
        ENCHANTS.put("triple_strike", "Triple-Strike");
        ENCHANTS.put("true_protection", "True Protection");
        ENCHANTS.put("turbo_cactus", "Cacti");
        ENCHANTS.put("turbo_cane", "Cane");
        ENCHANTS.put("turbo_carrot", "Carrot");
        ENCHANTS.put("turbo_coco", "Cocoa");
        ENCHANTS.put("turbo_melon", "Melon");
        ENCHANTS.put("turbo_mushrooms", "Mushroom");
        ENCHANTS.put("turbo_potato", "Potato");
        ENCHANTS.put("turbo_pumpkin", "Pumpkin");
        ENCHANTS.put("turbo_warts", "Warts");
        ENCHANTS.put("turbo_wheat", "Wheat");
        ENCHANTS.put("vampirism", "Vampirism");
        ENCHANTS.put("venomous", "Venomous");
        ENCHANTS.put("vicious", "Vicious");
        // Ultimate
        ENCHANTS.put("ultimate_bank", "Bank");
        ENCHANTS.put("ultimate_chimera", "Chimera");
        ENCHANTS.put("ultimate_combo", "Combo");
        ENCHANTS.put("ultimate_reiterate", "Duplex");
        ENCHANTS.put("ultimate_flash", "Flash");
        ENCHANTS.put("ultimate_last_stand", "Last Stand");
        ENCHANTS.put("ultimate_fatal_tempo", "Fatal Tempo");
        ENCHANTS.put("ultimate_inferno", "Inferno");
        ENCHANTS.put("ultimate_jerry", "Ultimate Jerry");
        ENCHANTS.put("ultimate_legion", "Legion");
        ENCHANTS.put("ultimate_no_pain_no_gain", "No Pain No Gain");
        ENCHANTS.put("ultimate_one_for_all", "One For All");
        ENCHANTS.put("ultimate_rend", "Rend");
        ENCHANTS.put("ultimate_soul_eater", "Soul Eater");
        ENCHANTS.put("ultimate_swarm", "Swarm");
        ENCHANTS.put("ultimate_wise", "Ultimate Wise");
        ENCHANTS.put("ultimate_wisdom", "Wisdom");
        ENCHANTS.put("ultimate_habaneno_tactics", "Habaneno Tactics");
        // Stacking
        ENCHANTS.put("compact", "Compact");
        ENCHANTS.put("cultivating", "Cultivating");
        ENCHANTS.put("expertise", "Expertise");
        ENCHANTS.put("hecatomb", "Hecatomb");
        ENCHANTS.put("champion", "Champion");
    }
}
