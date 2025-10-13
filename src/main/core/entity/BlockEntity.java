package core.entity;

import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.math.Rectangle;

public interface BlockEntity extends Entity, DrawComponent, HealthComponent {
    StaticObjectsConst getBlock();
    boolean canInsert(ItemStack stack);
    boolean itemInsertion(ItemStack item);
    void getHitbox(Rectangle out);
}
