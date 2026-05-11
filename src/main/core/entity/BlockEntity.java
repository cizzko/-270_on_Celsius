package core.entity;

import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst;

public interface BlockEntity extends Entity, DrawComponent {

    StaticObjectsConst getBlock();

    boolean insertItem(ItemStack item);

    default void onMouseClick() {}

    default void onMouseHover() {}
}
