package core.content;

import core.content.items.Item;

public final class ItemStackPredicate {

    public static final ItemStackPredicate[] EMPTY_ARRAY = new ItemStackPredicate[0];

    private TagReference<Item> ref;
    private final short count;

    public ItemStackPredicate(TagReference<Item> ref, short count) {
        this.ref = ref;
        this.count = count;
    }

    public boolean isSame(ItemStack itemStack) {
        return ref.matches(itemStack.item());
    }

    public boolean matches(ItemStack itemStack) {
        return itemStack.count() >= count && ref.matches(itemStack.item());
    }

    public void setRef(TagReference<Item> ref) {
        this.ref = ref;
    }

    public TagReference<Item> ref() {
        return ref;
    }

    public short count() {
        return count;
    }
}
