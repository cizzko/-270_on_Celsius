package core.World.StaticWorldObjects;

import core.entity.BlockEntity;

public interface StaticBlocksEvents {
    void placeStatic(int cellX, int cellY, BlockEntity id);
    void destroyStatic(int cellX, int cellY, BlockEntity id);
}
