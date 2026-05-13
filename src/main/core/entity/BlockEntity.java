package core.entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst;

import java.io.IOException;

public interface BlockEntity extends Entity, DrawComponent {

    StaticObjectsConst getBlock();

    enum TransitionResult { MOVE, PARTIAL_MOVE, FAILED }

    /**
     * Проба добавления предмета в блок.
     *
     * @param item Не нулевой, не пустой стек предметов
     * @return {@code true} если предмет принят, как полностью, так может и частично
     */
    default TransitionResult insertItem(ItemStack item) { return TransitionResult.FAILED; }

    default void onMouseClick() {}

    default void onMouseHover() {}

    void serialize(JsonGenerator gen, SerializerProvider provider) throws IOException;
}
