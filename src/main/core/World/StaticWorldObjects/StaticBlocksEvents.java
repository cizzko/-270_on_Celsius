package core.World.StaticWorldObjects;

public interface StaticBlocksEvents {
    void onBlockChanged(int cellX, int cellY, short oldId, short newId);
}
