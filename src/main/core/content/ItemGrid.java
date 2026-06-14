package core.content;

import core.content.items.Item;
import core.content.entity.BlockEntity;
import core.content.entity.comp.InventoryComponent.TransitionResult;
import core.math.Point2i;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Nullable;

public final class ItemGrid {
    private ItemGrid() {}

    public static Point2i findItemOrFree(ObjectArrayList<ObjectArrayList<@Nullable ItemStack>> items, Point2i except, Item item) {
        Point2i free = null;

        for (int x = 0; x < items.size(); x++) {
            var line = items.get(x);
            for (int y = 0; y < line.size(); y++) {
                var t = line.get(y);
                if (t != null && t.item() == item) {
                    return new Point2i(x, y);
                }

                //не ретурн потому что приоритет на поиске
                if (item == null && free == null && (except == null || !(x == except.x && y == except.y))) {
                    free = new Point2i(x, y);
                }
            }
        }
        return free;
    }

    public static Point2i findItemOrFree(ItemStack[][] items, Point2i except, Item item) {
        Point2i free = null;

        for (int x = 0; x < items.length; x++) {
            for (int y = 0; y < items[x].length; y++) {
                if (items[x][y] != null && items[x][y].item() == item) {
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

    public static TransitionResult tryAddTo(ObjectArrayList<ObjectArrayList<@Nullable ItemStack>> grid, ItemStack toAdd,
                                            int expectX, int expectY) {
        int si = -1, sj = -1;
        outer:
        for (int i = 0; i < grid.size(); i++) {
            var line = grid.get(i);
            for (int j = 0; j < line.size(); j++) {
                ItemStack itemStack = line.get(j);
                // Приоритет: добавление в уже существующие ячейки с предметами такого же типа
                if (itemStack != null && itemStack.isSame(toAdd)) {
                    si = i;
                    sj = j;
                    break outer;
                }

                if (itemStack == null && ((si == -1 /* || sj == -1 */) ||
                                          si == expectX && sj == expectY)) {
                    si = i;
                    sj = j;
                }
            }
        }

        if (si != -1 /* && sj != -1 */) {
            // assert sj != -1;
            var line = grid.get(si);
            var itemStack = line.get(sj);
            if (itemStack == null) {
                line.set(sj, toAdd);
                return TransitionResult.MOVE;
            } else {
                int d = itemStack.add(toAdd.count());
                if (d >= 0) {
                    toAdd.decrement(d);
                    return TransitionResult.PARTIAL_MOVE;
                }
                return TransitionResult.FAILED;
            }
        }

        return TransitionResult.FAILED;
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
            ItemStack itemStack = grid[si][sj];
            if (itemStack == null) {
                grid[si][sj] = toAdd;
                return BlockEntity.TransitionResult.MOVE;
            } else {
                int d = itemStack.add(toAdd.count());
                if (d >= 0) {
                    toAdd.decrement(d);
                    return BlockEntity.TransitionResult.PARTIAL_MOVE;
                }
                return BlockEntity.TransitionResult.FAILED;
            }
        }

        return BlockEntity.TransitionResult.FAILED;
    }

    public static void moveTo(ItemStack[][] from, ItemStack[][] to, Point2i fromCell, Point2i toCell) {
        // TODO: а что происходит с to[toCell.x][toCell.y] ?
        to[toCell.x][toCell.y] = from[fromCell.x][fromCell.y];
        from[fromCell.x][fromCell.y] = null;
    }

    public static void moveTo(ItemStack[][] from,
                              ObjectArrayList<ObjectArrayList<@Nullable ItemStack>> to,
                              Point2i fromCell, Point2i toCell) {
        // TODO: а что происходит с to[toCell.x][toCell.y] ?
        to.get(toCell.x).set(toCell.y, from[fromCell.x][fromCell.y]);
        from[fromCell.x][fromCell.y] = null;
    }

    @CheckReturnValue
    public static int insertCopy(ItemStack[] to, int toIndex, ItemStack[] from, int fromIndex) {
        if (to[toIndex] == null) {
            to[toIndex] = from[fromIndex].copy();
            return 0;
        } else {
            return to[toIndex].merge(from[fromIndex]);
        }
    }
}
