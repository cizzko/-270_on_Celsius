package core.World.Creatures.Player.Inventory.Items;

import core.World.Item;
import core.math.Point2i;

public class ItemGrid {
    public static Point2i findItemOrFree(ItemStack[][] items, Point2i except, Item item) {
        Point2i free = null;

        for (int x = 0; x < items.length; x++) {
            for (int y = 0; y < items[x].length; y++) {
                if (items[x][y] != null && items[x][y].getItem() == item) {
                    return new Point2i(x, y);
                }

                //не ретурн потому что приоритет на поиске
                if (items[x][y] == null && free == null && (except == null || !(x == except.x && y == except.y))) {
                    free = new Point2i(x, y);
                }
            }
        }
        return free;
    }

    public static Point2i findItemOrFree(ItemStack[][] items, Item item) {
        return findItemOrFree(items, null, item);
    }
}
