package core.content;

import core.content.blocks.BlockUnresolved;
import core.content.blocks.Block;
import core.content.items.Item;
import core.content.items.ItemUnresolved;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.HashMap;
import java.util.Objects;

public final class ContentResolver {
    private final Reference2ObjectOpenHashMap<Class<? extends ContentType>, HashMap<String, ContentType>> contentMap;

    public ContentResolver(Reference2ObjectOpenHashMap<Class<? extends ContentType>, HashMap<String, ContentType>> contentMap) {
        this.contentMap = contentMap;
    }

    public void resolveItemStacks(ItemStack[] itemStacks) {
        for (ItemStack itemStack : itemStacks) {
            if (itemStack.item() instanceof ItemUnresolved r) {
                itemStack.setItem(ContentManager.content(contentMap, Item.class).get(r.key()));
            }
        }
    }

    public Block resolveBlock(Block block) {
        if (block instanceof BlockUnresolved) {
            return ContentManager.content(contentMap, Block.class).get(block.key());
        }
        return block;
    }

    public void resolveItemStacksPredicates(ItemStackPredicate[] itemStacks) {
        for (var itemStack : itemStacks) {
            if (itemStack.ref() instanceof TagReference.OfUnresolved<Item> ref) {
                itemStack.setRef(resolveTagReference(ref));
            }
        }
    }

    private <C extends ContentType> TagReference<C> resolveTagReference(TagReference.OfUnresolved<C> ref) {
        if (ref.key().startsWith("#")) {
            var tagMap = ContentManager.content(contentMap, Tag.class);
            @SuppressWarnings("unchecked")
            Tag<C> tag = tagMap.get(ref.key().substring(1));
            Objects.requireNonNull(tag, ref.key()); // TODO
            return new TagReference.OfTag<>(tag);
        }
        var map = ContentManager.content(contentMap, ref.type());
        var content = map.get(ref.key());
        Objects.requireNonNull(content, ref.key());
        return new TagReference.OfType<>(content);
    }
}
