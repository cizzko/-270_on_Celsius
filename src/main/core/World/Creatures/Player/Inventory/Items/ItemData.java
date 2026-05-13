package core.World.Creatures.Player.Inventory.Items;

public abstract class ItemData {

    public static final class Tool extends ItemData {
        public long lastHitTime;
    }

    public static final class Weapon extends ItemData {
        public long lastShootTime;
    }
}
