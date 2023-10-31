package core.World.StaticWorldObjects;

public interface StaticBlocksEvents {
    void placeStatic(int cellX, int cellY, short id);
    void destroyStatic(int cellX, int cellY, short id);
}
