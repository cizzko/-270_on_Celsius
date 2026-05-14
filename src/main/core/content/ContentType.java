package core.content;

import com.fasterxml.jackson.annotation.JsonValue;

public interface ContentType {
    @JsonValue
    String id();

    void load(ContentLoader cnt);

    default void resolve(ContentResolver res) {}
}
