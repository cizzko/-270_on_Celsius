package core.content;

import core.content.blocks.BlockUnresolved;
import core.content.blocks.Block;
import core.content.items.Item;
import core.content.items.ItemUnresolved;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.HashMap;

public final class ContentResolver {
    private final Reference2ObjectOpenHashMap<Class<? extends ContentType>, HashMap<String, ContentType>> contentMap;

    public ContentResolver(Reference2ObjectOpenHashMap<Class<? extends ContentType>, HashMap<String, ContentType>> contentMap) {
        this.contentMap = contentMap;
    }

    public ItemStack[] resolveItemStacks(ItemStack[] itemStacks) {
        if (itemStacks == null) {
            return null;
        }
        for (ItemStack itemStack : itemStacks) {
            if (itemStack == null) {
                continue;
            }
            if (itemStack.item() instanceof ItemUnresolved r) {
                itemStack.setItem(ContentManager.content(contentMap, Item.class).get(r.key()));
            }
        }
        return itemStacks;
    }

    public Block resolveBlock(Block block) {
        if (block instanceof BlockUnresolved) {
            return ContentManager.content(contentMap, Block.class).get(block.key());
        }
        return block;
    }
}
