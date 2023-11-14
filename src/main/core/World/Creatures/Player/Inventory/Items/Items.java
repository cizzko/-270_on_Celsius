package core.World.Creatures.Player.Inventory.Items;

import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Weapons.Weapons;
import core.World.StaticWorldObjects.StaticWorldObjects;
import java.io.Serializable;
import static core.World.Textures.TextureLoader.getSize;

public class Items implements Serializable {
    public Weapons weapon;
    public short placeable;
    public Tools tool;
    public Details detail;
    public int id, countInCell;
    public float zoom;
    public String path, description;
    public Types type;

    public enum Types {
        TOOL,
        WEAPON,
        PLACEABLE,
        DETAIL
    }

    public Items(Weapons weapon, String path, String description) {
        this.weapon = weapon;
        this.id = weapon.name.hashCode();
        this.path = path;
        this.zoom = findZoom(path);
        this.countInCell = Inventory.findCountID(id);
        this.type = Types.WEAPON;
        this.description = description;
    }

    public Items(Tools tool, String path, String description) {
        this.tool = tool;
        this.id = tool.name.hashCode();
        this.path = path;
        this.zoom = findZoom(path);
        this.countInCell = Inventory.findCountID(id);
        this.type = Types.TOOL;
        this.description = description;
    }

    public Items(short placeable, String description) {
        this.placeable = placeable;
        this.id = StaticWorldObjects.getId(placeable);
        this.path = StaticWorldObjects.getPath(placeable);
        this.zoom = findZoom(path);
        this.countInCell = Inventory.findCountID(id);
        this.type = Types.PLACEABLE;
        this.description = description;
    }

    public Items(Details detail, String description) {
        this.detail = detail;
        this.id = detail.name.hashCode();
        this.path = detail.path;
        this.zoom = findZoom(path);
        this.countInCell = Inventory.findCountID(id);
        this.type = Types.DETAIL;
        this.description = description;
    }

    public static float findZoom(String path) {
        int width = getSize(path).width();
        int height = getSize(path).height();

        return 32f / (Math.max(width, height));
    }
}
