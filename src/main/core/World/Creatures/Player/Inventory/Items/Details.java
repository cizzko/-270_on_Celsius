package core.World.Creatures.Player.Inventory.Items;

import java.io.Serializable;

public class Details implements Serializable {
    public String path;

    public Details(String path) {
        this.path = path;
    }
}
