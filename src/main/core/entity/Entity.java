package core.entity;

public interface Entity {

    void init();
    void update();
    void remove();

    default <E extends Entity> E asIf(Class<? extends E> type) { return type.isInstance(this) ? type.cast(this) : null; }
}
