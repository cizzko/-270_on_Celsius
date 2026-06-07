package core.content.entity;

import org.jetbrains.annotations.MustBeInvokedByOverriders;

import static core.Global.entityPool;

public interface Entity extends HitboxComponent {

    // TODO unsigned short внутри
    short id();
    void setId(short id);

    /** Вызывается при создании сущности на координатах */
    default void init() {}
    /** Вызывается при обновлении мира */
    default void update() {}
    /** Вызывается при уничтожении сущности */
    @MustBeInvokedByOverriders
    default void remove() {
        entityPool.releaseId(this);
    }

    default boolean isRemoved() { return !entityPool.exists(id()); }
}
