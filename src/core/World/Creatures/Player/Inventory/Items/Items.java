package core.World.Creatures.Player.Inventory.Items;

import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Placeable.PlaceableItems;
import core.World.Creatures.Player.Inventory.Items.Weapons.Weapons;
import static core.World.Textures.TextureLoader.getSize;

public class Items {
    public Weapons weapon;
    public PlaceableItems placeable;
    public Tools tool;
    public Details detail;
    public int id, countInCell;
    public float zoom;
    public String path, description;
    public Types type;

    public enum Types {
        TOOL,
        WEAPON,
        PLACEABLE_FACTORY,
        DETAIL,
        PLACEABLE_BLOCK
    }

    public Items(Weapons weapon, String path, String description) {
        this.weapon = weapon;
        this.id = path.hashCode();
        this.path = path;
        this.zoom = findZoom(path);
        this.countInCell = Inventory.findCountID(id);
        this.type = Types.WEAPON;
        this.description = description;
    }

    public Items(Tools tool, String path, String description) {
        this.tool = tool;
        this.id = path.hashCode();
        this.path = path;
        this.zoom = findZoom(path);
        this.countInCell = Inventory.findCountID(id);
        this.type = Types.TOOL;
        this.description = description;
    }

    public Items(PlaceableItems placeable, Types type, String description) {
        this.placeable = placeable;
        this.id = placeable.hashCode();
        //this.path = placeable.factoryObject != null ? placeable.factoryObject.path : placeable.staticWorldObject.path;
        this.zoom = findZoom(path);
        this.countInCell = Inventory.findCountID(id);
        this.type = type;
        this.description = description;
    }

    public Items(Details detail, String description) {
        this.detail = detail;
        this.id = detail.path.hashCode();
        this.path = detail.path;
        this.zoom = findZoom(path);
        this.countInCell = Inventory.findCountID(id);
        this.type = Types.DETAIL;
        this.description = description;
    }

    public static float findZoom(String path) {
        int width = getSize(path).width;
        int height = getSize(path).height;

        return 32f / (Math.max(width, height));
    }
}
