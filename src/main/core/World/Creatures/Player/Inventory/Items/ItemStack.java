package core.World.Creatures.Player.Inventory.Items;

import core.World.Item;

import java.util.Objects;
import java.util.function.Supplier;

public final class ItemStack {
    public static final ItemStack[] EMPTY_ARRAY = {};

    private Item item;
    private int count;
    private ItemData data;

    public ItemStack(Item item) {
        this(item, 1);
    }

    public ItemStack copy() {
        return new ItemStack(item, count);
    }

    private static int requireNonNegativeCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative ItemStack size");
        }
        return count;
    }

    public ItemStack(Item item, int count) {
        this.item = Objects.requireNonNull(item);
        this.count = requireNonNegativeCount(count);
    }

    public static ItemStack[] createArray(ItemStack[] same) {
        return same.length == 0 ? EMPTY_ARRAY : new ItemStack[same.length];
    }

    public ItemStack clone() { return copy(); }

    public Item getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }

    public ItemData getData() {
        return data;
    }

    public <T extends ItemData> T getOrCreateData(Supplier<? extends T> constr) {
        if (data == null) {
            data = constr.get();
        }
        @SuppressWarnings("unchecked")
        T t = (T) data;
        return t;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void setCount(int count) {
        this.count = requireNonNegativeCount(count);
    }

    public void setData(ItemData data) {
        this.data = data;
    }

    public void set(Item item, int count) {
        this.item = Objects.requireNonNull(item);
        this.count = requireNonNegativeCount(count);
    }

    public boolean decrement() {
        return decrement(1);
    }

    public boolean decrement(int d) {
        count = Math.max(0, count - d);
        return count == 0;
    }

    public void increment() {
        add(1);
    }

    public void add(int d) {
        count += d;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean isSame(ItemStack other) {
        return this == other || other != null && item.equals(other.item);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemStack itemStack)) {
            return false;
        }
        return count == itemStack.count && item.equals(itemStack.item) && data.equals(itemStack.data);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + item.hashCode();
        h += (h << 5) + count;
        if (data != null) {
            h += (h << 5) + data.hashCode();
        }
        return h;
    }

    @Override
    public String toString() {
        return "ItemStack{" +
                "item=" + item +
                ", count=" + count +
                (data != null ? (", data=" + data) : "") +
                '}';
    }
}
