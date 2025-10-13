package core.entity;

import core.World.Creatures.Player.Inventory.Items.ItemStack;

public class BlockItemStorage {
    private final ItemStack[] items;
    private final int maxCapacity;

    private int total;

    public BlockItemStorage(ItemStack[] items, int maxCapacity) {
        this.items = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            this.items[i] = new ItemStack(items[i].getItem(), 0);
        }
        this.maxCapacity = maxCapacity;
    }

    public int add(ItemStack stack) {
        for (ItemStack allowedStack : items) {
            if (allowedStack.isSame(stack)) {
                int toAdd = Math.min(maxCapacity - allowedStack.getCount(), stack.getCount());
                allowedStack.add(toAdd);
                total += toAdd;
                return toAdd;
            }
        }
        return 0;
    }

    public int total() {
        return total;
    }
}
