package core.World.StaticWorldObjects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.content.serialize.SerializableContent;

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

        static boolean isByte(int v) {
            return v >= -128 && v <= 127;
        }

        public MultiblockPart(int rootOffsetX, int rootOffsetY) {
            if (!(isByte(rootOffsetX) && isByte(rootOffsetY))) {
                throw new IllegalArgumentException();
            }
            this.rootOffsetX = (byte) rootOffsetX;
            this.rootOffsetY = (byte) rootOffsetY;
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
