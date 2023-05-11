package a7.tweakception.tweaks;

import net.minecraft.util.AxisAlignedBB;

public enum SkyblockIsland
{
    PRIVATE_ISLAND("Private Island", new String[]
    {
        "Your Island"
    }),
    HUB("Hub", new String[]
    {
        "Hub",
        "Archery Range",
        "Auction House",
        "Bank",
        "Bazaar Alley",
        "Blacksmith",
        "Builder's House",
        "Canvas Room",
        "Coal Mine",
        "Colosseum Arena",
        "Colosseum",
        "Community Center",
        "Election Room",
        "Farm",
        "Farmhouse",
        "Fashion Shop",
        "Fisherman's Hut",
        "Flower House",
        "Forest",
        "Graveyard",
        "High Level",
        "Library",
        "Mountain",
        "Museum",
        "Ruins",
        "Tavern",
        "Village",
        "Wilderness",
        "Wizard Tower"
    }),
    DUNGEON("Catacombs", new String[]
    {
        "The Catacombs"
    }),
    DUNGEON_HUB("Dungeon Hub", new String[]
    {
        "Dungeon Hub"
    }),
    GARDEN("Garden", new String[]
    {
        "The Garden",
        "Plot 1",
        "Plot 2",
        "Plot 3",
        "Plot 4",
        "Plot 5",
        "Plot 6",
        "Plot 7",
        "Plot 8",
        "Plot 9",
        "Plot 10",
        "Plot 11",
        "Plot 12",
        "Plot 13",
        "Plot 14",
        "Plot 15",
        "Plot 16",
        "Plot 17",
        "Plot 18",
        "Plot 19",
        "Plot 20",
        "Plot 21",
        "Plot 22",
        "Plot 23",
        "Plot 24",
        "Plot 25",
    }),
    THE_END("The End", new String[]
    {
        "The End",
        "Void Sepulture",
        "Dragon's Nest"
    }),
    CRIMSON_ISLE("Crimson Isle", new String[]
    {
        "Crimson Isle",
        "Barbarian Outpost",
        "The Bastion",
        "Blazing Volcano",
        "Burning Desert",
        "Cathedral",
        "Crimson Fields",
        "Dojo",
        "Dragontail",
        "Forgotten Skull",
        "Kuudra's End",
        "Mage Outpost",
        "Magma Chamber",
        "Mystic Marsh",
        "Odger's Hut",
        "Plhlegblast Pool",
        "Ruins of Ashfang",
        "Scarleton",
        "Stronghold",
        "The Wasteland"
    }, new SubArea[]
    {
        new SubArea("Stronghold back right", "BackRight", "fishing",
            new AxisAlignedBB(-367, 178, -486, -332, 130, -462)),
        new SubArea("Stronghold back left", "BackLeft", "fishing",
            new AxisAlignedBB(-393, 178, -488, -368, 130, -462)),
        new SubArea("Stronghold front right", "FrontRight", "fishing",
            new AxisAlignedBB(-319, 132, -534, -267, 153, -591))
    }),
    CRYSTAL_HOLLOWS("Crystal Hollows", new String[]
    {
        "Crystal Nucleus",
        "Dragon's Lair",
        "Fairy Grotto",
        "Goblin Holdout",
        "Goblin Queen's Den",
        "Jungle Temple",
        "Jungle",
        "Khazad-dm", // û char will be removed during string cleaning
        "Lost Precursor City",
        "Magma Fields",
        "Mines of Divan",
        "Mithril Deposits",
        "Precursor Remnants"
    }),
    DWARVEN_MINES("Dwarven Mines", new String[]
    {
        "Dwarven Mines",
        "The Forge",
        "Forge Basin",
        "Palace Bridge",
        "Royal Palace",
        "Aristocrat Passage",
        "Hanging Court",
        "Divan's Gateway",
        "Far Reserve",
        "Goblin Burrows",
        "Miner's Guild",
        "Great Ice Wall",
        "The Mist",
        "C Minecarts Co.",
        "Grand Library",
        "Barracks of Heroes",
        "Dwarven Village",
        "The Lift",
        "Royal Quarters",
        "Lava Springs",
        "Cliffside Veins",
        "Rampart's Quarry",
        "Upper Mines",
        "Royal Mines"
    }),
    THE_PARK("The Park", new String[]
    {
        "The Park",
        "Birch Park",
        "Spruce Woods",
        "Dark Thicket",
        "Savanna Woodland",
        "Jungle Island",
        "Howling Cave",
        "Lonely Island",
        "Viking Longhouse",
        "Melody's Plateau"
    }, new SubArea[]
    {
        new SubArea("Dark oak area", "DarkOak", "foraging",
            new AxisAlignedBB(-316, 98, -15, -374, 111, -120)),
        new SubArea("Wolf cave", "WolfCave", "slayer",
            new AxisAlignedBB(-351, 78, -102, -399, 49, 36))
    });
    
    /**
     * The server name in tab list "Area: ..."
     */
    public final String name;
    
    /**
     * The location names in the scoreboard
     */
    public final String[] locations;
    
    /**
     * The areas for internal use
     */
    public final SubArea[] areas;
    
    SkyblockIsland(String name, String[] locations)
    {
        this.name = name;
        this.locations = locations;
        this.areas = null;
    }
    
    SkyblockIsland(String name, String[] locations, SubArea[] areas)
    {
        this.name = name;
        this.locations = locations;
        this.areas = areas;
        for (SubArea area : areas)
            area.setIsland(this);
    }
    
    public static class SubArea
    {
        public final String name;
        public final String shortName;
        public final String type;
        public final AxisAlignedBB box;
        public SkyblockIsland island = null;
        
        public SubArea(String name, String shortName, String type, AxisAlignedBB box)
        {
            this.name = name;
            this.shortName = shortName;
            this.type = type;
            // Block range to bounding box
            box = box.addCoord(1, 1, 1);
            this.box = box;
        }
        
        public void setIsland(SkyblockIsland island)
        {
            this.island = island;
        }
    }
}
