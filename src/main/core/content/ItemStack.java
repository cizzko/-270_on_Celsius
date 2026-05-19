package core.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import core.content.items.Item;
import core.content.items.data.ItemData;
import org.jetbrains.annotations.CheckReturnValue;
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
        this.count = Math.min(requirePositive(count), item.maxStackSize);
        this.data = data;
    }

    public ItemStack(Item item) {
        this(item, 1);
    }

    public ItemStack copy() {
        return new ItemStack(item, count);
    }

    private static int requirePositive(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative ItemStack size");
        }
        return count;
    }

    public ItemStack(Item item, int count) {
        this.item = Objects.requireNonNull(item);
        this.count = Math.min(requirePositive(count), item.maxStackSize);
    }

    public static ItemStack itemStack(Item item, int count) { return new ItemStack(item, count); }

    public static ItemStack[] createArray(ItemStack[] same) {
        return same.length == 0 ? EMPTY_ARRAY : new ItemStack[same.length];
    }

    public ItemStack clone() { return copy(); }

    public Item item() {
        return item;
    }

    public int count() {
        return count;
    }

    public @Nullable ItemData data() {
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
        this.count = Math.min(requirePositive(count), item.maxStackSize);
    }

    public void setData(@Nullable ItemData data) {
        this.data = data;
    }

    public void set(Item item, int count) {
        this.item = Objects.requireNonNull(item);
        this.count = Math.min(requirePositive(count), item.maxStackSize);
        this.data = null;
    }

    @CheckReturnValue
    public boolean decrement() { return decrement(1); }

    @CheckReturnValue
    public boolean decrement(int d) {
        assert (count - d) >= 0;
        count = Math.max(0, count - d);
        return count == 0;
    }

    @CheckReturnValue
    public int increment() { return add(1); }

    /// Попытка увеличить количество предметов в стеке
    /// @return В случае успешного добавления вернёт `>=0` число,
    /// в противном случае `<0`, что будет означать на сколько по модулю переполнится стек
    @CheckReturnValue
    public int add(int d) {
        if (count + d <= item.maxStackSize) {
            count += d;
            assert count <= item.maxStackSize;
            return d;
        }
        return item.maxStackSize - (d + count);
    }

    @CheckReturnValue
    public int merge(ItemStack itemStack) {
        return add(itemStack.count());
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean isSame(@Nullable ItemStack other) {
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
        return count == itemStack.count &&
               item.equals(itemStack.item) &&
               Objects.equals(data, itemStack.data);
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
