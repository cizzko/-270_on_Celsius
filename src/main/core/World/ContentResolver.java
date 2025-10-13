package core.World;

import core.Global;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.BlockUnresolved;
import core.World.StaticWorldObjects.StaticObjectsConst;

public class ContentResolver {

    public ItemStack[] resolveItemStacks(ItemStack[] itemStacks) {
        for (ItemStack itemStack : itemStacks) {
            if (itemStack.getItem() instanceof ItemUnresolved r) {
                itemStack.setItem(Global.content.itemById(r.id()));
            }
        }
        return itemStacks;
    }

    public StaticObjectsConst resolveBlock(StaticObjectsConst block) {
        if (block instanceof BlockUnresolved) {
            return Global.content.blockById(block.id());
        }
        return block;
    }
}
