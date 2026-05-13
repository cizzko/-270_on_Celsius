package core.World.Creatures.Player.Inventory.Items;

import core.World.Item;
import core.entity.BlockEntity;
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

    public static BlockEntity.TransitionResult tryAddTo(ItemStack[][] grid, ItemStack toAdd) {
        int si = -1, sj = -1;
        outer:
        for (int i = 0; i < grid.length; i++) {
            ItemStack[] line = grid[i];
            for (int j = 0; j < line.length; j++) {
                ItemStack itemStack = line[j];
                // Приоритет: добавление в уже существующие ячейки с предметами такого же типа
                if (itemStack != null && itemStack.isSame(toAdd)) {
                    si = i;
                    sj = j;
                    break outer;
                }

                if (itemStack == null && (si == -1 /* || sj == -1 */)) {
                    si = i;
                    sj = j;
                }
            }
        }

        if (si != -1 /* && sj != -1 */) {
            // assert sj != -1;
            if (grid[si][sj] == null) {
                grid[si][sj] = toAdd;
                return BlockEntity.TransitionResult.MOVE;
            } else {
                grid[si][sj].add(toAdd.getCount());
                toAdd.setCount(0);
                return BlockEntity.TransitionResult.PARTIAL_MOVE;
            }
        }

        return BlockEntity.TransitionResult.FAILED;
    }

    public static void moveTo(ItemStack[][] from, ItemStack[][] to, Point2i fromCell, Point2i toCell) {
        // TODO: а что происходит с to[toCell.x][toCell.y] ?
        to[toCell.x][toCell.y] = from[fromCell.x][fromCell.y];
        from[fromCell.x][fromCell.y] = null;
    }

    public static void insertCopy(ItemStack[] to, int toIndex, ItemStack[] from, int fromIndex) {
        if (to[toIndex] == null) {
            to[toIndex] = from[fromIndex].copy();
        } else {
            to[toIndex].add(from[fromIndex].getCount());
        }
    }
}
