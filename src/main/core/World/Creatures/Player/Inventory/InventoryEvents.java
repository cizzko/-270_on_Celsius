package core.World.Creatures.Player.Inventory;

import core.World.Creatures.Player.Inventory.Items.ItemStack;

public interface InventoryEvents {
    default void itemDropped(int blockX, int blockY, ItemStack item) {}
    default void itemCreated(ItemStack item) {}
}
