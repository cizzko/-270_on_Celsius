package core.World.Creatures.Player.Inventory.Items;

import java.io.Serializable;

public class Details implements Serializable {
    public String path, name;

    public Details(String name, String path) {
        this.name = name;
        this.path = path;
    }
}
