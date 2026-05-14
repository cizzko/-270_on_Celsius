package core.World.Creatures.Player.Inventory.Items;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import core.World.Item;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

@JsonSerialize(using = ItemStack.Serializer.class)
public final class ItemStack {
    public static final ItemStack[] EMPTY_ARRAY = {};

    private Item item;
    private int count;
    private @Nullable ItemData data;

    @JsonCreator
    public ItemStack(@JsonProperty("item") Item item,
                     @JsonProperty("count") int count,
                     @JsonProperty("data") @Nullable ItemData data) {
        this.item = Objects.requireNonNull(item);
        this.count = requireNonNegativeCount(count);
        this.data = data;
    }

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

    public @Nullable ItemData getData() {
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

    public void setData(@Nullable ItemData data) {
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

    public void merge(ItemStack itemStack) {
        add(itemStack.getCount());
    }

    public static class ItemStackGridSerializer extends StdSerializer<ItemStack[][]> {

        public ItemStackGridSerializer() {
            super(ItemStack[][].class);
        }

        @Override
        public void serialize(ItemStack[][] value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeNumberField("width", value.length);
            gen.writeNumberField("height", value[0].length);
            {
                gen.writeArrayFieldStart("items");
                for (ItemStack[] line : value) {
                    for (ItemStack item : line) {
                        if (item != null) {
                            gen.writeObject(item);
                        }
                    }
                }
                gen.writeEndArray();
            }

            gen.writeEndObject();
        }
    }

    public static class Serializer extends StdSerializer<ItemStack> {

        public Serializer() {
            super(ItemStack.class);
        }

        @Override
        public void serialize(ItemStack value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("item", value.item.id);
            if (value.count != 1) {
                gen.writeNumberField("count", value.count);
            }
            // if (value.data instanceof SerializableContent ser) {
            //     gen.writeFieldName("data");
            //     ser.serialize(gen, provider);
            // }
            gen.writeEndObject();
        }
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
