package core.content.entity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.content.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst;

import java.io.IOException;

public interface BlockEntity extends BlockDrawComponent, PositionComponent {

    /** Вызывается при создании сущности на координатах */
    default void init() {}
    /** Вызывается при обновлении мира */
    default void update() {}
    /** Вызывается при уничтожении сущности */
    default void remove() {}

    StaticObjectsConst getBlock();

    enum TransitionResult {
        /// Полное перемещение предмета с переходом владения объектом
        MOVE,
        /// Предмет частично или полностью был взят (изменилось количество)
        /// Вызывается если в свободном слоте уже есть стек такого же типа
        /// Если [ItemStack#isEmpty()] возвращает `true`, то этот стек считается пустым и должен быть уничтожен
        PARTIAL_MOVE,
        /// Не нашлось свободного слота для предмета
        FAILED
    }

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
