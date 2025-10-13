package core.World.Creatures.Player.Inventory.Items;

import core.Utils.Sized;

@Deprecated(forRemoval = true)
public class Items {

    public static float computeZoom(Sized size) {
        // 32 - target structure size
        return 32f / (Math.max(size.width(), size.height()));
    }
}
