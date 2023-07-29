package core.World.Creatures.Player.Inventory.Placeable;
import core.World.Textures.StaticWorldObjects;

public class PlaceableItems {
    public StaticWorldObjects staticWorldObject;
    public Factories factoryObject;

    public PlaceableItems(StaticWorldObjects object) {
        this.staticWorldObject = object;
    }

    public PlaceableItems(Factories object) {
        this.factoryObject = object;
    }
}
