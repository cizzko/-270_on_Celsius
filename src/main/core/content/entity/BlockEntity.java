package core.content.entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst;

import java.io.IOException;

public interface BlockEntity extends DrawComponent, PositionComponent {

    /** Вызывается при создании сущности на координатах */
    default void init() {}
    /** Вызывается при обновлении мира */
    default void update() {}
    /** Вызывается при уничтожении сущности */
    default void remove() {}

    default <E extends Entity> E asIf(Class<? extends E> type) { return type.isInstance(this) ? type.cast(this) : null; }

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
    void deserialize(JsonParser p, DeserializationContext ctxt) throws IOException;
}
