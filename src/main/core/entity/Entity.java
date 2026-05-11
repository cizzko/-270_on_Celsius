package core.entity;

public interface Entity {

    /** Вызывается при создании сущности на координатах */
    default void init() {}
    /** Вызывается при обновлении мира */
    default void update() {}
    /** Вызывается при уничтожении сущности */
    default void remove() {}

    default <E extends Entity> E asIf(Class<? extends E> type) { return type.isInstance(this) ? type.cast(this) : null; }
}
