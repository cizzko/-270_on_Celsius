package core.content.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public interface SerializableContent {

    void serialize(JsonGenerator gen, SerializerProvider provider) throws IOException;
}
