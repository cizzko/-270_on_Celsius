package core.World.Creatures.Player.BuildMenu;

import core.World.Creatures.Player.Inventory.Items.Items;

public class BuildItems {
    public String name;
    public Items[] inputObjects, outputObjects, requiredForBuild;
    public Items item;

    public BuildItems(Items item, Items[] inputObjects, Items[] outputObjects, String name) {
        this.name = name;
        this.item = item;
        this.inputObjects = inputObjects;
        this.outputObjects = outputObjects;
    }
}
