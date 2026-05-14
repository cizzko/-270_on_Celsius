package core.content.entity;

import org.jetbrains.annotations.MustBeInvokedByOverriders;

import static core.Global.entityPool;

public interface Entity extends HitboxComponent {

    short getId();
    void setId(int id);

    /** Вызывается при создании сущности на координатах */
    default void init() {}
    /** Вызывается при обновлении мира */
    default void update() {}
    /** Вызывается при уничтожении сущности */
    @MustBeInvokedByOverriders
    default void remove() {
        entityPool.releaseId(this);
    }

    default <E extends Entity> E asIf(Class<? extends E> type) { return type.isInstance(this) ? type.cast(this) : null; }

    default boolean isRemoved() { return entityPool.getEntity(getId()) == null; }
}
