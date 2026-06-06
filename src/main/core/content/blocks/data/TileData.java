package core.content.blocks.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.content.serialize.SerializableContent;
import core.math.MathUtil;

import java.io.IOException;

/**
 * У некоторых блоков есть дополнительная информация.
 * <p>
 * Такая информация должна определять только особенности взаимодействия с блоком
 * и не меняться в течении жизни блока.
 */
public abstract class TileData implements SerializableContent {

    /**
     * У больших блоков, текстура которых занимает больше одного тайла есть связанные блоки.
     * Как раз у этих блоков и присутствует информация о смещении относительно корневого блока.
     */
    public static class MultiblockPart extends TileData {
        public byte rootOffsetX, rootOffsetY;

        public MultiblockPart(@JsonProperty("rootOffsetX") int rootOffsetX,
                              @JsonProperty("rootOffsetY") int rootOffsetY) {
            this.rootOffsetX = MathUtil.toByteExact(rootOffsetX);
            this.rootOffsetY = MathUtil.toByteExact(rootOffsetY);
        }

        @Override
        public void serialize(JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeNumberField("rootOffsetX", rootOffsetX);
            gen.writeNumberField("rootOffsetY", rootOffsetY);
            gen.writeEndObject();
        }
    }
}
