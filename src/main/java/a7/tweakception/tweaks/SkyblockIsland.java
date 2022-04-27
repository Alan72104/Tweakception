package a7.tweakception.tweaks;

public enum SkyblockIsland
{
    DUNGEON("Dungeon", new String[]
    {
        "The Catacombs"
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
        "Ruins of Ashfang",
        "Stronghold",
        "The Wasteland"
    });

    public String name;
    public String[] places;

    SkyblockIsland(String name, String[] places)
    {
        this.name = name;
        this.places = places;
    }
}
