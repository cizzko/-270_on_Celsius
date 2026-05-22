package core.content.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.content.ItemStack;
import core.content.serialize.SerializableContent;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public final class BlockItemStorage implements SerializableContent {
    public final ItemStack[] items;
    public final int[] sizes;
    public final int maxCapacity;

    private int total;

    @JsonCreator
    public BlockItemStorage(@JsonProperty("items") ItemStack[] items,
                            @JsonProperty("sizes") int[] sizes,
                            @JsonProperty("maxCapacity") int maxCapacity,
                            @JsonProperty("total") int total) {
        this.items = Objects.requireNonNull(items);
        this.sizes = Objects.requireNonNull(sizes);
        this.maxCapacity = maxCapacity;
        this.total = total;
    }

    public BlockItemStorage(ItemStack[] items, int maxCapacity) {
        this.items = new ItemStack[items.length];
        this.sizes = new int[items.length];
        for (int i = 0; i < items.length; i++) {
            this.items[i] = new ItemStack(items[i].item(), 0);
            this.sizes[i] = items[i].count();
        }
        this.maxCapacity = maxCapacity;
    }

    @Override
    public void serialize(JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("items", items);
        gen.writeObjectField("sizes", sizes);
        gen.writeNumberField("maxCapacity", maxCapacity);
        gen.writeNumberField("total", total);
        gen.writeEndObject();
    }

    public boolean hasRequired() {
        for (int i = 0; i < items.length; i++) {
            if (items[i].count() < sizes[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean removeFirst() {
        for (int i = 0; i < items.length; i++) {
            ItemStack allowedStack = items[i];
            int maxSize = sizes[i];

            if (allowedStack.count() >= maxSize) {
                allowedStack.decrement(maxSize);
                total -= maxSize;
                return true;
            }
        }
        return false;
    }

    public int add(ItemStack stack) {
        for (ItemStack allowedStack : items) {
            if (allowedStack.isSame(stack)) {
                int toAdd = maxCapacity - allowedStack.count();
                if (toAdd >= 0) {
                    int d = allowedStack.add(toAdd);
                    if (d >= 0) {
                        total += toAdd;
                        return toAdd;
                    }
                }
            }
        }
        return 0;
    }

    public int total() {
        return total;
    }

    public boolean isEmpty() {
        return total > 0;
    }

    @Override
    public String toString() {
        return "BlockItemStorage{" +
               "items=" + Arrays.toString(items) +
               ", sizes=" + Arrays.toString(sizes) +
               ", maxCapacity=" + maxCapacity +
               ", total=" + total +
               '}';
    }
}
