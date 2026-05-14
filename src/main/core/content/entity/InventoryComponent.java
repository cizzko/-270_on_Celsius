package core.content.entity;

import core.World.Creatures.Player.Inventory.Items.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface InventoryComponent  {

    List<List<@Nullable ItemStack>> items();

    enum TransitionResult { MOVE, PARTIAL_MOVE, FAILED }

    TransitionResult addItem(ItemStack itemStack);
}
