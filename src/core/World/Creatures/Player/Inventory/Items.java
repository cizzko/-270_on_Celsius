package core.World.Creatures.Player.Inventory;

import core.World.Creatures.Player.Inventory.Placeable.PlaceableItems;
import static core.World.Textures.TextureLoader.BufferedImageEncoder;

public class Items {
    public Weapons weapon;
    public PlaceableItems placeable;
    public Tools tool;
    public int id, countInCell;
    public float zoom;
    public String path;
    public Types type;

    public enum Types {
        WEAPON,
        PLACEABLE,
        TOOL
    }

    public Items(Weapons weapon, int id, String path) {
        this.weapon = weapon;
        this.id = id;
        this.path = path;
        this.zoom = findZoom(path);
        this.countInCell = Inventory.findCountID(id);
        this.type = Types.WEAPON;
    }

    public Items(Tools tool, int id, String path) {
        this.tool = tool;
        this.id = id;
        this.path = path;
        this.zoom = findZoom(path);
        this.countInCell = Inventory.findCountID(id);
        this.type = Types.TOOL;
    }

    public Items(PlaceableItems placeable, int id, String path) {
        this.placeable = placeable;
        this.id = id;
        this.path = path;
        this.zoom = findZoom(path);
        this.countInCell = Inventory.findCountID(id);
        this.type = Types.PLACEABLE;
    }

    private static float findZoom(String path) {
        return 64f / (BufferedImageEncoder(path).getHeight() + BufferedImageEncoder(path).getWidth());
    }
}
