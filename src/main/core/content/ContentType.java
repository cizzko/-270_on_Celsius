package core.content;

import com.fasterxml.jackson.annotation.JsonValue;

public interface ContentType {
    @JsonValue
    String key();

    short id();
    void setId(short id);
}
