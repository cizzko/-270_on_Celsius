package core.World.Creatures.Player.Inventory;

import core.World.Creatures.Player.Inventory.Items.Items;

public interface InventoryEvents {
    default void itemDropped(int blockX, int blockY, Items item) {}
    default void itemCreated(Items item) {}
}
