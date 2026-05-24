package core.content;

public interface Loadable {

    void load(ContentLoader cnt);

    default void resolve(ContentResolver res) {}
}
