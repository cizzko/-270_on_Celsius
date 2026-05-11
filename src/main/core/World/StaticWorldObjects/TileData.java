package core.World.StaticWorldObjects;

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

    public static class MultiblockPart extends TileData {
        public byte rootOffsetX, rootOffsetY;

        public MultiblockPart(byte rootOffsetX, byte rootOffsetY) {
            this.rootOffsetX = rootOffsetX;
            this.rootOffsetY = rootOffsetY;
        }
    }
}
