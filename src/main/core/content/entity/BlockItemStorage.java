package core.content.entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.content.ItemStack;
import core.content.ItemStackPredicate;
import core.content.serialize.SerializableContent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.IOException;

public final class BlockItemStorage implements SerializableContent {
    public final ObjectArrayList<ItemStack> items;
    public final ItemStackPredicate[] predicates;
    public final int maxCapacity;

    private int total;

    public BlockItemStorage(ItemStackPredicate[] predicates, int maxCapacity) {
        this.predicates = predicates;
        this.items = new ObjectArrayList<>();
        this.maxCapacity = maxCapacity;
    }

    @Override
    public void serialize(JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        // TODO сериализация
        gen.writeEndObject();
    }

    public boolean hasRequired() {
        if (items.isEmpty()) {
            return false;
        }
        for (var predicate : predicates) {
            for (var item : items) {
                if (!predicate.matches(item)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void removeFirst() {
        for (var predicate : predicates) {
            for (var item : items) {
                if (predicate.matches(item)) {
                    short toTake = predicate.count();
                    //noinspection ResultOfMethodCallIgnored
                    item.decrement(toTake);
                    total -= toTake;
                    assert total >= 0;
                    assert toTake >= 0;
                }
            }
        }
        items.removeIf(ItemStack::isEmpty);
    }

    public int add(ItemStack stack) {

        ItemStackPredicate first = null;
        for (var predicate : predicates) {
            if (predicate.isSame(stack)) {
                first = predicate;
                break;
            }
        }

        if (first == null) {
            return 0;
        }
        int freeSpace = maxCapacity - total;
        int toAdd = Math.min(freeSpace, stack.count());
        assert toAdd > 0;
        toAdd = add0(stack, toAdd);
        assert toAdd > 0;
        total += toAdd;
        assert total >= 0;
        assert total <= maxCapacity;
        return toAdd;
    }

    private int add0(ItemStack itemStack, int quantity) {
        for (ItemStack item : items) {
            if (item.isSame(itemStack)) {
                int d = item.add(quantity);
                if (d < 0) {
                    quantity -= Math.abs(d);
                    int res = item.add(quantity);
                    assert res >= 0;
                    return res;
                }
            }
        }
        items.add(itemStack.asCount(quantity));
        return quantity;
    }

    public int total() {
        return total;
    }

    public boolean isEmpty() {
        return total == 0;
    }
}
