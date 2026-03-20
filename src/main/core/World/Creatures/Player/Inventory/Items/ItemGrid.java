package core.World.Creatures.Player.Inventory.Items;

import core.math.Point2i;

public class ItemGrid {
    public static Point2i findItemOrFree(Items[][] items, Point2i except, int id) {
        Point2i free = null;

        for (int x = 0; x < items.length; x++) {
            for (int y = 0; y < items[x].length; y++) {
                if (items[x][y] != null && items[x][y].id == id) {
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

    public static Point2i findItemOrFree(Items[][] items, int id) {
        return findItemOrFree(items, null, id);
    }
}
