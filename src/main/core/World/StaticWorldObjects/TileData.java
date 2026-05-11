package core.World.StaticWorldObjects;

/**
 * У некоторых блоков есть дополнительная информация.
 * <p>
 * Такая информация должна определять только особенности взаимодействия с блоком
 * и не меняться в течении жизни блока.
 */
public abstract class TileData {
    // TODO: для сохранения мира
    // void serialize()
    // void deserialize()

    public static class Workbench extends TileData {
        public enum Type {SMALL, MEDIUM, LARGE}

        public Type type;

        public Workbench(Type type) {
            this.type = type;
        }
    }

    /**
     * У больших блоков, текстура которых занимает больше одного тайла есть связанные блоки.
     * Как раз у этих блоков и присутствует информация о смещении относительно корневого блока.
     */
    public static class MultiblockPart extends TileData {
        public byte rootOffsetX, rootOffsetY;

        public MultiblockPart(byte rootOffsetX, byte rootOffsetY) {
            this.rootOffsetX = rootOffsetX;
            this.rootOffsetY = rootOffsetY;
        }
    }
}
