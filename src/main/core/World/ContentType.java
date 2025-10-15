package core.World;

public interface ContentType {
    String id();

    default void load(ContentLoader cnt) {}

    default void resolve(ContentResolver res) {}
}
