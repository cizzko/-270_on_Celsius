package core.content;

import core.content.blocks.BlockUnresolved;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.content.items.Item;
import core.content.items.ItemUnresolved;

import java.util.EnumMap;
import java.util.HashMap;

public class ContentResolver {
    private final EnumMap<ContentManager.Type, HashMap<String, ContentType>> contentMap;

    public ContentResolver(EnumMap<ContentManager.Type, HashMap<String, ContentType>> contentMap) {
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
                itemStack.setItem((Item) contentMap.get(ContentManager.Type.ITEM).get(r.id()));
            }
        }
        return itemStacks;
    }

    public StaticObjectsConst resolveBlock(StaticObjectsConst block) {
        if (block instanceof BlockUnresolved) {
            return (StaticObjectsConst) contentMap.get(ContentManager.Type.BLOCK).get(block.id());
        }
        return block;
    }
}
