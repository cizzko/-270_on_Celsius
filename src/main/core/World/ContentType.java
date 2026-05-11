package core.World;

public interface ContentType {
    String id();

    void load(ContentLoader cnt);

    default void resolve(ContentResolver res) {}
}
