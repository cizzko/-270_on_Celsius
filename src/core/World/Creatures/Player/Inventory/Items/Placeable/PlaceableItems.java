package core.World.Creatures.Player.Inventory.Items.Placeable;

public class PlaceableItems {
    public short staticWorldObject;
    public Factories factoryObject;

    public PlaceableItems(short object) {
        this.staticWorldObject = object;
    }

    public PlaceableItems(Factories object) {
        this.factoryObject = object;
    }
}
