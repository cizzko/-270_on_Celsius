package core.World.StaticWorldObjects;

public interface StaticBlocksEvents {
    void onBlockChanged(int cellX, int cellY, StaticObjectsConst old, StaticObjectsConst newie);
}
