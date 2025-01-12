package core.World;

import core.World.Creatures.Player.Player;
import core.World.StaticWorldObjects.StaticBlocksEvents;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.Textures.ShadowMap;
import core.math.MathUtil;
import core.math.Point2i;

import java.util.ArrayList;

import static core.Window.start;
import static core.World.StaticWorldObjects.StaticObjectsConst.getConst;
import static core.World.StaticWorldObjects.StaticWorldObjects.*;

public class World {
    public final int sizeX, sizeY;
    public final short[] tiles;

    private final ArrayList<StaticBlocksEvents> listeners = new ArrayList<>();

    public int dayCount;

    public World(int sizeX, int sizeY) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.tiles = new short[sizeX * sizeY];
    }

    public void registerListener(StaticBlocksEvents listener) {
        listeners.add(listener);
    }

    public void set(int x, int y, short object, boolean followingRules) {
        // Global.app.ensureMainThread();
        assert object != -1;

        if (getConst(getId(object)).optionalTiles != null) {
            short[][] tiles = getConst(getId(object)).optionalTiles;

            for (int blockX = 0; blockX < tiles.length; blockX++) {
                for (int blockY = 0; blockY < tiles[0].length; blockY++) {
                    if (tiles[blockX][blockY] != 0 && getType(tiles[blockX][blockY]) != StaticObjectsConst.Types.GAS) {
                        setImpl(x + blockX, y + blockY, tiles[blockX][blockY], followingRules);
                    }
                }
            }
            setImpl(x, y, object, followingRules);
        } else {
            setImpl(x, y, object, followingRules);
        }

        if (start) {
            for (StaticBlocksEvents listener : listeners) {
                listener.placeStatic(x, y, object);
            }
        }
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < sizeX && y >= 0 && y < sizeY;
    }

    public short get(int x, int y) {
        // Global.app.ensureMainThread();

        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY) {
            return -1;
        }
        return tiles[x + sizeX * y];
    }

    public void destroy(int x, int y) {
        // Global.app.ensureMainThread();

        short id = get(x, y);
        if (id != 0) {
            if (getConst(getId(id)).hasMotherBlock || getConst(getId(id)).optionalTiles != null) {
                Point2i root = Player.findRoot(x, y);

                if (root != null) {
                    deleteTiles(get(root.x, root.y), root.x, root.y);
                }
            } else {
                set(x, y, (short) 0, false);
                ShadowMap.update();
            }

            if (start) {
                for (StaticBlocksEvents listener : listeners) {
                    listener.destroyStatic(x, y, id);
                }
            }
        }
    }

    // region Детали реализации

    private void deleteTiles(short id, int cellX, int cellY) {
        StaticObjectsConst objType = getConst(getId(id));
        for (int blockX = 0; blockX < objType.optionalTiles.length; blockX++) {
            for (int blockY = 0; blockY < objType.optionalTiles[0].length; blockY++) {
                tiles[(cellX + blockX) + sizeX * (cellY + blockY)] = (short) 0;
            }
        }
        ShadowMap.update();
    }

    private void setImpl(int x, int y, short object, boolean followingRules) {
        if (!followingRules || checkPlaceRules(x, y, object)) {
            tiles[x + sizeX * y] = object;
        }
    }

    private boolean checkPlaceRules(int x, int y, short root) {
        if (getId(get(x, y)) != 0) {
            return false;
        }
        if (getConst(getId(root)).optionalTiles != null) {
            short[][] tiles = getConst(getId(root)).optionalTiles;

            for (int xBlock = 0; xBlock < tiles.length; xBlock++) {
                if (getResistance(get(x + xBlock, y - 1)) < 100) {
                    return false;
                }
                for (int yBlock = 0; yBlock < tiles[0].length; yBlock++) {
                    if (getId(get(x + xBlock, y)) != 0) {
                        return false;
                    }
                }
            }
        } else {
            for (Point2i d : MathUtil.CROSS_OFFSETS) {
                if (!(getResistance(get(x + d.x, y + d.y)) < 100)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    // endregion
}
