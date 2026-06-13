package core.World;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import core.Application;
import core.GameState;
import core.World.WorldGenerator.Biomes;
import core.content.blocks.Block;
import core.content.blocks.data.TileData;
import core.content.entity.BlockEntity;
import core.graphic.ShadowMap;
import core.math.MathUtil;
import core.math.Point2i;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ForkJoinPool;

import static core.Constants.availableProcessors;
import static core.Global.*;
import static core.World.WorldGenerator.WorldGeneratorConstants.COPY_SIZE;

/// Для кеш-локальности мира обходить его построчно, то есть:
/// ```
/// for (int y = 0; y < world.sizeY; y++) {
///     for (int x = 0; x < world.sizeX; x++) {
///
///     }
/// }
/// ```
@JsonSerialize(using = World.WorldSerializer.class)
public final class World {
    static short lastId = -1;
    static Block lastBlock = null;
    static final Point2i tmp = new Point2i();

    public final ForkJoinPool genPool = new ForkJoinPool(availableProcessors);

    public final int sizeX, sizeY;
    public final /* unsigned */ short[] tiles;
    public final /* unsigned */ byte[] hp;

    @JsonDeserialize(using = DataDeserializer.class)
    public final Int2ObjectOpenHashMap<TileData> data;
    @JsonDeserialize(using = EntityDeserializer.class)
    public final Int2ObjectOpenHashMap<BlockEntity> entity;

    @JsonIgnore
    public final Biomes[] biomes;

    public final Meta meta;

    public boolean getRootBlockPosTo(Point2i pos, Point2i out) { return getRootBlockPosTo(pos.x, pos.y, out); }

    public boolean getRootBlockPosTo(int x, int y, Point2i out) {
        var block = getBlock(x, y);
        if (block != null && block.isMultiblock()) {
            if (getData(x, y) instanceof TileData.MultiblockPart part) {
                out.set(x - part.rootOffsetX, y - part.rootOffsetY);
            } else {
                out.set(x, y); // Корень
            }
            return true;
        }
        return false;
    }

    public static final class Meta {
        public final int sizeX, sizeY;
        public final long seed;

        public @Nullable String name, description;
        public long lastPlayTime;  // секунды
        public long totalPlayTime; // секунды

        @JsonCreator
        public Meta(int sizeX, int sizeY, long seed,
                    @Nullable String name,
                    @Nullable String description,
                    long lastPlayTime, long totalPlayTime) {
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.seed = seed;
            this.name = name;
            this.description = description;
            this.lastPlayTime = lastPlayTime;
            this.totalPlayTime = totalPlayTime;
        }
    }

    public static class Tmp {

        public int sizeX, sizeY;
        public /* unsigned */ short[] tiles;
        public /* unsigned */ byte[] hp;
        public Meta meta;

        // Будущее к которому стремимся:
        // public final byte[] temperature;
        // public final byte[] light;
        @JsonDeserialize(using = DataDeserializer.class)
        public Int2ObjectOpenHashMap<TileData> data;
        @JsonDeserialize(using = EntityDeserializer.class)
        public Int2ObjectOpenHashMap<BlockEntity> entity;

        @JsonIgnore
        public Biomes[] biomes;

        private int pos2index(int x, int y) { return x + sizeX * y; }
        private int index2x(int index)      { return index % sizeX; }
        private int index2y(int index)      { return index / sizeX; }
    }

    @JsonCreator
    public World(World.Tmp tmp) {
        this.sizeX = tmp.sizeX;
        this.sizeY = tmp.sizeY;
        this.tiles = tmp.tiles;
        this.hp = tmp.hp;
        this.data = tmp.data;
        this.entity = tmp.entity;
        this.biomes = new Biomes[sizeX];
        this.meta = tmp.meta;
    }

    public World(Meta meta) {
        this.sizeX = meta.sizeX;
        this.sizeY = meta.sizeY;
        this.tiles = new short[sizeX * sizeY];
        this.hp = new byte[sizeX * sizeY];
        this.biomes = new Biomes[sizeX];
        this.data = new Int2ObjectOpenHashMap<>();
        this.entity = new Int2ObjectOpenHashMap<>();
        this.meta = meta;
    }

    public void update() {
        for (BlockEntity entity : entity.values()) {
            try {
                entity.update();
            } catch (Exception e) {
                Application.log.error("Failed to update block entity {} at ({}. {})",
                        entity, entity.x(), entity.y(), e);
            }
        }
    }

    public Meta meta() { return meta; }

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

    public void set(int x, int y, @Nullable Block object, boolean followingRules) {
        if (object == null)
            object = Block.AIR;

        setImpls(x, y, object, followingRules);

        if (gameState == GameState.PLAYING) {
            if (x < COPY_SIZE) {
                copyFromTo(x, y, sizeX - COPY_SIZE + x, y, object, followingRules);
            } else if (x > sizeX - COPY_SIZE) {
                copyFromTo(x, y, x - (sizeX - COPY_SIZE), y, object, followingRules);
            }
        }
    }

    public void copyFromTo(int fromX, int fromY, int toX, int toY, Block object, boolean followingRules) {
        setImpls(toX, toY, object, followingRules);
        setHp(toX, toY, getHp(fromX, fromY));
        var fromData = getData(fromX, fromY);

        if (fromData != null) {
            setData(toX, toY, fromData);
        }
    }

    public void setData(int x, int y, TileData data) {
        // Objects.requireNonNull(data);
        this.data.put(pos2index(x, y), data);
    }

    public @Nullable TileData getData(int x, int y) {
        return data.get(pos2index(x, y));
    }

    public @Nullable BlockEntity getEntity(Point2i pos) {
        return getEntity(pos.x, pos.y);
    }

    public @Nullable BlockEntity getEntity(int x, int y) {
        var block = getBlock(x, y);
        if (block == null) {
            return null;
        }
        if (!block.isMultiblock()) {
            return entity.get(pos2index(x, y));
        }
        Point2i rootPos = tmp;
        if (getRootBlockPosTo(x, y, rootPos)) {
            return entity.get(pos2index(rootPos.x, rootPos.y));
        }
        // не должно приходить
        return null;
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

    /// @return {@code null} в случае выхода за границу. Если это воздух, то возвращается {@link Block#AIR}
    public Block getBlock(int x, int y) {
        // Global.app.ensureMainThread();
        if (!inBounds(x, y)) {
            return null;
        }
        short blockId = tiles[pos2index(x, y)];

        //прикольно бустит скорость но надо потестить получше
        if (lastId != blockId) {
            lastId = blockId;
            return lastBlock = content.blocksRegistry.typeById(Short.toUnsignedInt(blockId));
        }

        return lastBlock;
    }

    public Block getBlock(Point2i pos) { return getBlock(pos.x, pos.y); }

    public int getHp(Point2i pos) { return getHp(pos.x, pos.y); }

    /// @return {@code -1} в случае выхода за границу. В остальных случаях здоровье в отрезке `[0, 255]`
    public int getHp(int x, int y) {
        // Global.app.ensureMainThread();
        return inBounds(x, y) ? hp[pos2index(x, y)] : -1;
    }

    /// @param newHp новое значение здоровья блока. Должно быть в отрезке `[0, 255]`
    public void setHp(int x, int y, @MathUtil.UnsignedByte int newHp) {
        // Global.app.ensureMainThread();
        // if (newHp < 0 || newHp >= (1 << 8))
        //     throw new IllegalArgumentException("HP out of range: [0, 255]");

        if (!inBounds(x, y)) {
            return;
        }
        if (getRootBlockPosTo(x, y, tmp)) {
            var rootBlock = getBlock(tmp);

            for (int blockY = 0; blockY < rootBlock.tileCountY; blockY++) {
                for (int blockX = 0; blockX < rootBlock.tileCountX; blockX++) {
                    setHpImpl(x + blockX, y + blockY, newHp);
                }
            }
        } else {
            setHpImpl(x, y, newHp);
        }
    }

    private void setHpImpl(int x, int y, int newHp) {
        hp[pos2index(x, y)] = (byte) newHp;
    }

    public int getBlockId(Point2i pos) { return getBlockId(pos.x, pos.y); }

    /// @return {@code -1} в случае выхода за границу. В остальных случаях неотрицательный blockId
    public int getBlockId(int x, int y) {
        // Global.app.ensureMainThread();
        return inBounds(x, y) ? Short.toUnsignedInt(tiles[pos2index(x, y)]) : -1;
    }

    public Block.Type getBlockType(Point2i pos)  { return getBlockType(pos.x, pos.y); }
    public Block.Type getBlockType(int x, int y) { // проверки границ для пусичек
        return content.getBlockType(tiles[pos2index(x, y)]);
    }

    public boolean isBlockType(int x, int y, Block.Type type) {
        return content.isBlockType(tiles[pos2index(x, y)], type);
    }

    // region Приватные методы

    public int pos2index(int x, int y) { return x + sizeX * y; }
    public int index2x(int index)      { return index % sizeX; }
    public int index2y(int index)      { return index / sizeX; }

    private void setImpls(int x, int y, Block object, boolean followingRules) {
        destroyBlock(x, y);
        if (object == Block.AIR) {
            return;
        }

        var newEntity = object.createEntity(x, y);
        if (newEntity != null) {
            entity.put(pos2index(x, y), newEntity);
        }

        setHp(x, y, object.maxHp);

        if (object.isMultiblock()) {
            // TODO тут должна быть проверка для мультиблока на followingRules, чтобы в цикле этим не заниматься
            for (int currentY = 0; currentY < object.tileCountY; currentY++) {
                for (int currentX = 0; currentX < object.tileCountX; currentX++) {
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
        int old = getBlockId(x, y);
        if (old <= 0) {
            return;
        }
        int idx = pos2index(x, y);
        var block = content.blocksRegistry.typeById(old);
        if (block.isMultiblock()) {
            int dx, dy;
            if (data.remove(idx) instanceof TileData.MultiblockPart part) {
                dx = part.rootOffsetX;
                dy = part.rootOffsetY;
            } else {
                dx = dy = 0;
            }
            deleteMultiblockFromRoot(x - dx, y - dy);
        } else {
            deleteEntity(x, y);
            data.remove(idx);
            tiles[idx] = 0;
            hp[idx] = 0;
            ShadowMap.update();
        }
    }

    private void setImpl(int x, int y, Block block, boolean followingRules) {
        if (!followingRules || checkPlaceRules(x, y, block)) {
            tiles[pos2index(x, y)] = block.id;
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

        for (int blockY = 0; blockY < rootBlock.tileCountY; blockY++) {
            for (int blockX = 0; blockX < rootBlock.tileCountX; blockX++) {
                int idx = pos2index(x + blockX, y + blockY);
                tiles[idx] = 0;
                hp[idx] = 0;
                data.remove(idx);
            }
        }
        ShadowMap.update();
    }

    public boolean checkPlaceRules(int x, int y, Block root) {
        var currentBlock = getBlock(x, y);
        if (currentBlock != Block.AIR) {
            return false;
        }

        if (root.isMultiblock()) {
            // под этим y есть твёрдые блоки
            for (int xBlock = 0; xBlock < root.tileCountX; xBlock++) {
                var underBlock = getBlock(x + xBlock, y - 1);

                if (underBlock == null || underBlock.type != Block.Type.SOLID) {
                    return false;
                }
            }

            for (int yBlock = 0; yBlock < root.tileCountY; yBlock++) {
                for (int xBlock = 0; xBlock < root.tileCountX; xBlock++) {
                    var block = getBlockId(x + xBlock, y + yBlock);

                    if (block != 0) {
                        return false;
                    }
                }
            }

            if (root.type == Block.Type.SOLID) {
                boolean anyCollision = entityPool.index().any(x, y, root.tileCountX, root.tileCountY);
                return !anyCollision;
            }
        } else {
            if (root.type == Block.Type.SOLID) {
                boolean anyCollision = entityPool.index().any(x, y, root.tileCountX, root.tileCountY);
                if (anyCollision) {
                    return false;
                }
            }

            // рядышком есть твёрдый блок
            for (Point2i d : MathUtil.CROSS_OFFSETS) {
                var block = getBlock(x + d.x, y + d.y);
                if (block == null || block.type == Block.Type.SOLID) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    // endregion

    public static final class EntityDeserializer extends StdDeserializer<Int2ObjectOpenHashMap<BlockEntity>> {
        public EntityDeserializer() {
            super((Class<?>) null);
        }

        @Override
        public Int2ObjectOpenHashMap<BlockEntity> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            if (!(p.getParsingContext().getParent().getCurrentValue() instanceof World.Tmp worl)) {
                throw new IllegalStateException("WORL!!!");
            }
            if (!p.isExpectedStartObjectToken()) {
                ctxt.reportWrongTokenException(this, JsonToken.START_OBJECT, "OBJECT!!!");
            }

            var entity = new Int2ObjectOpenHashMap<BlockEntity>();

            JsonToken t;
            while ((t = p.nextToken()) != null) {
                if (t == JsonToken.END_OBJECT) {
                    break;
                }
                if (t == JsonToken.FIELD_NAME) {
                    int pos = Integer.parseInt(p.currentName());
                    short blockId = worl.tiles[pos];
                    if (blockId >= 0) {
                        var block = content.blocksRegistry.typeById(blockId);
                        if (block != null) {
                            var ent = block.createEntity(worl.index2x(pos), worl.index2y(pos));
                            if (ent != null) {
                                p.nextToken(); // START_OBJECT
                                ent.deserialize(p, ctxt);
                                entity.put(pos, ent);
                            }
                        }
                    }
                }
            }
            assert t == JsonToken.END_OBJECT;
            return entity;
        }

    }

    public static final class DataDeserializer extends StdDeserializer<Int2ObjectOpenHashMap<TileData>> {

        public DataDeserializer() {
            super(Int2ObjectOpenHashMap.class);
        }

        @Override
        public Int2ObjectOpenHashMap<TileData> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            if (!(p.getParsingContext().getParent().getCurrentValue() instanceof World.Tmp worl)) {
                throw new IllegalStateException("" + p.getParsingContext().getParent().getCurrentValue());
            }
            if (!p.isExpectedStartObjectToken()) {
                ctxt.reportWrongTokenException(this, JsonToken.START_OBJECT, "");
            }
            var data = new Int2ObjectOpenHashMap<TileData>();
            p.assignCurrentValue(data);

            JsonToken t;
            while ((t = p.nextToken()) != null) {
                if (t == JsonToken.END_OBJECT) {
                    break;
                }
                if (t == JsonToken.FIELD_NAME) {
                    int pos = Integer.parseInt(p.currentName());
                    short blockId = worl.tiles[pos];
                    if (blockId >= 0) {
                        var block = content.blocksRegistry.typeById(blockId);
                        if (block != null) {
                            if (block.isMultiblock()) {
                                p.nextToken(); // START_OBJECT
                                var md = ctxt.readValue(p, TileData.MultiblockPart.class);
                                data.put(pos, md);
                            }
                        }
                    }
                }
            }
            assert t == JsonToken.END_OBJECT;
            return data;
        }
    }

    public static final class WorldSerializer extends StdSerializer<World> {

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
            gen.writeEndObject();
        }
    }

    /**
     * Ищет {@code y} первого свободного блока (воздуха) в конкретном {@code x}
     * <p>
     * Проверяет {@code x} с шагом {@code period}, при нахождении твердого блока
     * запускает точечную проверку для поиска точной позиции. Скорость работы увеличивается в {@code period} раз,
     * во столько же снижается точность нахождения одиночных блоков. При {@code period} = 1 поиск становится последовательным
     * </p>
     * @param cellX координата {@code x}
     * @param period шаг поиска
     * @return {@code y} координата воздуха над самым верхним твердым блоком, либо {@code -1}, если земля не найдена
     */

    //впр можно хранить карту высот, но поиск нужен не так часто, чтоб это дало желаемый профит
    //todo @test возможно, последовательный обход будет быстрее работать в силу отсутствия кешмиссов
    public static int findSurfaceY(int cellX, int period) {
        if (!(cellX >= 0 && cellX < world.sizeX)) // TODO generateCaves
            return -1;
        for (int y = world.sizeY - 1; y > 0; y -= period) {
            if (world.isBlockType(cellX, y, Block.Type.SOLID)) {
                for (int i = y + period; i > y - 1; i--) {
                    //если сверху вниз, то первый блок и будет солид, нет смысла проверять над ним
                    if (world.isBlockType(cellX, i, Block.Type.SOLID)) {
                        return i + 1;
                    }
                }
            }
        }
        return -1;
    }
}
