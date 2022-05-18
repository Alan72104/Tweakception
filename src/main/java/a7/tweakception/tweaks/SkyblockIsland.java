package a7.tweakception.tweaks;

public enum SkyblockIsland
{
    DUNGEON("Dungeon", new String[]
    {
        "The Catacombs"
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
    });

    public String name;
    public String[] places;

    SkyblockIsland(String name, String[] places)
    {
        this.name = name;
        this.places = places;
    }
}
