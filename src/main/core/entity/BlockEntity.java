package core.entity;

import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst_V2;
import core.math.Rectangle;

public interface BlockEntity extends Entity, DrawComponent, HealthComponent {
    StaticObjectsConst_V2 getBlock();
    boolean canInsert(ItemStack stack);
    boolean itemInsertion(ItemStack item);
    void getHitbox(Rectangle out);
}
