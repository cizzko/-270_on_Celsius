package core.World.Creatures.Player;

import core.Global;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.Item;

import static core.Application.log;

public class ItemControl {
    public static ItemStack[][]
            smallWorkbenchItems = new ItemStack[10][20],
            mediumWorkbenchItems = new ItemStack[10][20],
            largeWorkbenchItems = new ItemStack[10][20],
            buildMenuItems = new ItemStack[5][30];

    public static void create() {
        for (Item item : Global.content.items()) {
            if (item.requirements == ItemStack.EMPTY_ARRAY) {
                continue;
            }
            if (item.createWith == null) {
                // Крафт из рук
                put(buildMenuItems, item);
            } else {
                switch (item.createWith.id) {
                    case "workbenchSmall" -> put(smallWorkbenchItems, item);
                    case "mediumWorkbench" -> put(mediumWorkbenchItems, item);
                    case "largeWorkbench" -> put(largeWorkbenchItems, item);
                    default -> log.warn("Unknown 'CreateWith' in item {}: {}", item.createWith, item.id);
                }
            }
        }
    }

    private static void put(ItemStack[][] grid, Item item) {
        for (int y = 0; y < grid[0].length; y++) {
            for (int x = 0; x < grid.length; x++) {
                if (grid[x][y] == null) {
                    grid[x][y] = new ItemStack(item);
                    return;
                }
            }
        }
    }
}
