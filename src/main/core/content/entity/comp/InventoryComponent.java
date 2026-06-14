package core.content.entity.comp;

import core.content.ItemStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

public interface InventoryComponent  {

    ObjectArrayList<ObjectArrayList<@Nullable ItemStack>> items();

    enum TransitionResult { MOVE, PARTIAL_MOVE, FAILED }

    TransitionResult addItem(ItemStack itemStack);
}
