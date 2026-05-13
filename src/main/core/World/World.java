package core.World;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import core.Application;
import core.GameState;
import core.Global;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.TileData;
import core.World.Textures.ShadowMap;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator.Biomes;
import core.World.WorldGenerator.WorldGenerator;
import core.entity.BlockEntity;
import core.math.MathUtil;
import core.math.Point2i;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.IOException;
import java.io.UncheckedIOException;

@JsonSerialize(using = World.WorldSerializer.class)
public class World {
    public final int sizeX, sizeY;
    public final /* unsigned */ short[] tiles;
    public final /* unsigned */ byte[] hp;

    // Будущее к которому стремимся:
    // public final byte[] temperature;
    // public final byte[] light;
    public final Int2ObjectOpenHashMap<TileData> data = new Int2ObjectOpenHashMap<>();
    public final Int2ObjectOpenHashMap<BlockEntity> entity = new Int2ObjectOpenHashMap<>();

    public final Biomes[] biomes;

    public World(int sizeX, int sizeY) {
        // assert sizeX > 0 && sizeY > 0;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.tiles = new short[sizeX * sizeY];
        this.hp = new byte[sizeX * sizeY];
        this.biomes = new Biomes[sizeX];
    }

    public void update() {
        for (BlockEntity entity : entity.values()) {
            entity.update();
        }
    }

    public void setBiomes(int x, Biomes biomes) {
        this.biomes[x] = biomes;
    }

    /// @param damage должно быть в отрезке `[0, 255]`
    /// @return `true` если блок уничтожен
    public boolean damage(int x, int y, int damage) {
        int newHp = getHp(x, y) - damage;

        if (newHp <= 0) {
            set(x, y, null, false);
            return true;
        }
        setHp(x, y, newHp);
        return false;
    }

    public void destroy(int x, int y) {
        set(x, y, null, false);
    }

    public void set(int x, int y, StaticObjectsConst object, boolean followingRules) {
        if (object == null)
            object = StaticObjectsConst.AIR;

        setImpls(x, y, object, followingRules);

        if (Global.gameState == GameState.PLAYING) {
            if (x < WorldGenerator.copySize) {
                copyFromTo(x, y, sizeX - WorldGenerator.copySize + x, y, object, followingRules);
            } else if (x > sizeX - WorldGenerator.copySize) {
                copyFromTo(x, y, x - (sizeX - WorldGenerator.copySize), y, object, followingRules);
            }
        }
    }

    private void copyFromTo(int fromX, int fromY, int toX, int toY,
                            StaticObjectsConst object, boolean followingRules) {
        setImpls(toX, toY, object, followingRules);
        setHp(toX, toY, getHp(fromX, fromY));
        setData(toX, toY, getData(fromX, fromY));
    }

    public void setData(int x, int y, TileData data) {
        this.data.put(pos2index(x, y), data);
    }

    public TileData getData(int x, int y) {
        return data.get(pos2index(x, y));
    }

    public BlockEntity getEntity(int x, int y) {
        var rootPos = getRootBlockPos(x, y);
        if (rootPos != null) {
            return entity.get(pos2index(rootPos.x, rootPos.y));
        }
        return entity.get(pos2index(x, y));
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < sizeX && y >= 0 && y < sizeY;
    }

    public Biomes getBiomes(int x) {
        if (x < 0 || x >= sizeX) {
            return Biomes.getDefault();
        }

        return biomes[x];
    }

    /// @return {@code null} в случае выхода за границу. Если это воздух, то возвращается {@link StaticObjectsConst#AIR}
    public StaticObjectsConst getBlock(int x, int y) {
        // Global.app.ensureMainThread();
        if (!inBounds(x, y)) {
            return null;
        }
        int blockId = Short.toUnsignedInt(tiles[pos2index(x, y)]);
        return Global.content.getConstByBlockId(blockId);
    }

    /// @return {@code -1} в случае выхода за границу. В остальных случаях здоровье в отрезке `[0, 255]`
    public int getHp(int x, int y) {
        // Global.app.ensureMainThread();
        return inBounds(x, y) ? hp[pos2index(x, y)] : -1;
    }

    /// @param newHp новое значение здоровья блока. Должно быть в отрезке `[0, 255]`
    public void setHp(int x, int y, int newHp) {
        // Global.app.ensureMainThread();
        if (newHp < 0 || newHp >= (1 << 8))
            throw new IllegalArgumentException("HP out of range: [0, 255]");

        if (inBounds(x, y)) {
            var root = getRootBlockPos(x, y);

            if (root != null) {
                var rootBlock = getBlock(root.x, root.y);

                for (int blockX = 0; blockX < rootBlock.tileCountX; blockX++) {
                    for (int blockY = 0; blockY < rootBlock.tileCountY; blockY++) {
                        hp[pos2index(x + blockX, y + blockY)] = (byte) newHp;
                    }
                }
            } else {
                hp[pos2index(x, y)] = (byte) newHp;
            }
        }
    }

    /// @return {@code -1} в случае выхода за границу. В остальных случаях неотрицательный blockId
    public int getBlockId(int x, int y) {
        // Global.app.ensureMainThread();
        return inBounds(x, y) ? Short.toUnsignedInt(tiles[pos2index(x, y)]) : -1;
    }

    // region Приватные методы

    private int pos2index(int x, int y) {
        return x + sizeX * y;
    }

    private void setImpls(int x, int y, StaticObjectsConst object, boolean followingRules) {
        destroyBlock(x, y);
        if (object == StaticObjectsConst.AIR) {
            return;
        }

        var newEntity = object.createEntity(x, y);
        if (newEntity != null) {
            entity.put(pos2index(x, y), newEntity);
        }

        setHp(x, y, object.maxHp);

        if (object.isMultiblock()) {
            for (int currentX = 0; currentX < object.tileCountX; currentX++) {
                for (int currentY = 0; currentY < object.tileCountY; currentY++) {
                    int partX = x + currentX, partY = y + currentY;

                    setImpl(partX, partY, object, followingRules);

                    if (partX != x || partY != y) {
                        setHp(partX, partY, object.maxHp);
                        setData(partX, partY, new TileData.MultiblockPart((partX - x), (partY - y)));
                    }
                }
            }
        } else {
            setImpl(x, y, object, followingRules);
        }
    }

    private void destroyBlock(int x, int y) {
        var old = getBlock(x, y);
        if (old == null) {
            return;
        }
        var root = getRootBlockPos(x, y);
        if (root != null)  {
            deleteMultiblockFromRoot(root.x, root.y);
        } else {
            deleteEntity(x, y);
            int idx = pos2index(x, y);
            data.remove(idx);
            tiles[idx] = 0;
            setHp(x, y, 0);
            ShadowMap.update();
        }
    }

    private void setImpl(int x, int y, StaticObjectsConst block, boolean followingRules) {
        if (!followingRules || checkPlaceRules(x, y, block)) {
            tiles[pos2index(x, y)] = (short) Global.content.getBlockIdByType(block);
        }
    }

    private void deleteEntity(int x, int y) {
        var rootEntity = entity.remove(pos2index(x, y));
        if (rootEntity != null) {
            rootEntity.remove();
        }
    }

    private void deleteMultiblockFromRoot(int x, int y) {
        var rootBlock = getBlock(x, y);
        assert rootBlock != null;
        deleteEntity(x, y);
        data.remove(pos2index(x, y));

        for (int blockX = 0; blockX < rootBlock.tileCountX; blockX++) {
            for (int blockY = 0; blockY < rootBlock.tileCountY; blockY++) {
                int idx = pos2index(x + blockX, y + blockY);
                tiles[idx] = 0;
                hp[idx] = 0;
                data.remove(idx);
            }
        }
        ShadowMap.update();
    }

    public Point2i getRootBlockPos(int x, int y) {
        if (getData(x, y) instanceof TileData.MultiblockPart part) {
            return new Point2i(x - part.rootOffsetX, y - part.rootOffsetY);
        } else {
            var block = getBlock(x, y);
            if (block != null && block.isMultiblock()) {
                return new Point2i(x, y); // Корень
            } else {
                return null;
            }
        }
    }

    public boolean checkPlaceRules(int x, int y, StaticObjectsConst root) {
        var currentBlock = getBlock(x, y);
        if (currentBlock != StaticObjectsConst.AIR) {
            return false;
        }

        if (root.isMultiblock()) {
            for (int xBlock = 0; xBlock < root.tileCountX; xBlock++) {
                var underBlock = getBlock(x + xBlock, y - 1);

                // под нижними блоками есть твёрдые блоки
                if (underBlock == null || underBlock.type != StaticObjectsConst.Type.SOLID) {
                    return false;
                }
                // площадь не занята
                for (int yBlock = 0; yBlock < root.tileCountY; yBlock++) {
                    var block = getBlock(x + xBlock, y);
                    if (block != StaticObjectsConst.AIR) {
                        return false;
                    }
                }
            }

            if (root.type == StaticObjectsConst.Type.SOLID) {
                boolean anyCollision = Global.entityPool.worldIndex().findAnyInRange(
                        x * TextureDrawing.blockSize, y * TextureDrawing.blockSize,
                        root.texture.width(), root.texture.height(),
                        e -> true);
                return !anyCollision;
            }
        } else {
            if (root.type == StaticObjectsConst.Type.SOLID) {
                boolean anyCollision = Global.entityPool.worldIndex().findAnyInRange(
                        x * TextureDrawing.blockSize, y * TextureDrawing.blockSize,
                        root.texture.width(), root.texture.height(),
                        e -> true);
                if (anyCollision) {
                    return false;
                }
            }

            // рядышком есть твёрдый блок
            for (Point2i d : MathUtil.CROSS_OFFSETS) {
                var block = getBlock(x + d.x, y + d.y);
                if (block == null || block.type == StaticObjectsConst.Type.SOLID) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    // endregion

    public static class WorldSerializer extends StdSerializer<World> {

        public WorldSerializer() {
            super(World.class);
        }

        @Override
        public void serialize(World value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeNumberField("sizeX", value.sizeX);
            gen.writeNumberField("sizeY", value.sizeY);
            gen.writeObjectField("tiles", value.tiles);
            gen.writeObjectField("hp", value.hp);

            if (!value.data.isEmpty()) {
                gen.writeObjectFieldStart("data");

                value.data.int2ObjectEntrySet().fastForEach(e -> {
                    try {
                        gen.writeFieldName(Integer.toString(e.getIntKey()));
                        e.getValue().serialize(gen, provider);
                    } catch (IOException exc) {
                        Application.log.error(exc);
                        throw new UncheckedIOException(exc);
                    }
                });
                gen.writeEndObject();
            }


            if (!value.entity.isEmpty()) {
                gen.writeObjectFieldStart("entity");

                value.entity.int2ObjectEntrySet().fastForEach(e -> {
                    try {
                        gen.writeFieldName(Integer.toString(e.getIntKey()));
                        e.getValue().serialize(gen, provider);
                    } catch (IOException exc) {
                        Application.log.error(exc);
                        throw new UncheckedIOException(exc);
                    }
                });
                gen.writeEndObject();
            }

            // public final Biomes[] biomes;

            gen.writeEndObject();
        }
    }
}
