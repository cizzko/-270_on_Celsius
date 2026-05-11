package core.World;

import core.Global;
import core.World.Creatures.Player.ItemControl;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.content.CreatureType;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

public class ContentManager {
    private static final Logger log = LogManager.getLogger();

    private final EnumMap<Type, HashMap<String, ContentType>> contentById = new EnumMap<>(Type.class);

    // TODO: отдельный класс (регистр?)
    private final Object2IntOpenHashMap<StaticObjectsConst> block2Id = new Object2IntOpenHashMap<>();
    private final Int2ObjectOpenHashMap<StaticObjectsConst> id2Block = new Int2ObjectOpenHashMap<>();

    public void loadAll() {
        var contentDir = Global.assets.assetsDir().resolve("World/ItemsCharacteristics");
        // чтение конфига структуры. Можно будет так сделать моды)
        // var structureJsonFile = contentDir.resolve("structure.json");

        record ContentSource(Type type, Path dir) {}
        var sources = List.of(
                // textures       content
                // | items        | items
                //   | air.png      | ...
                // | blocks       | blocks
                //   | ...          | air.json
                // | creatures    | creatures
                //   | ...          | ...

                new ContentSource(Type.ITEM, contentDir.resolve("Details")),
                new ContentSource(Type.ITEM, contentDir.resolve("Tools")),

                new ContentSource(Type.BLOCK, contentDir.resolve("Factories")),
                new ContentSource(Type.BLOCK, contentDir.resolve("Blocks"))


        );

        var loader = new ContentLoader();

        for (ContentSource source : sources) {
            // TODO Можно читать index.json если есть. Если нет - автоматически
            try (var dirstr = Files.newDirectoryStream(source.dir, file -> file.getFileName().toString().endsWith(".json"))) {

                var index = contentById.computeIfAbsent(source.type, k -> new HashMap<>());
                for (Path file : dirstr) {
                    loader.init(source.type, file);
                    var cont = loader.readContent();
                    index.put(cont.id(), cont);
                }
            } catch (IOException e) {
                log.error("Failed to list directory: '{}'", source.dir, e);
                throw new UncheckedIOException(e);
            } catch (Exception e) {
                log.error("Failed to read directory: '{}'", source.dir, e);
            }
        }

        generateBlockItems();
        resolveAll();
        generateIds();

        ItemControl.create();
    }

    private void generateBlockItems() {
        var itemIndex = contentById.computeIfAbsent(Type.ITEM, k -> new HashMap<>());
        for (ContentType type : contentById.get(Type.BLOCK).values()) {
            if (!(type instanceof StaticObjectsConst s)) {
                throw new IllegalStateException(); // ??
            }
            var itemBlock = new ItemBlock(s.id);
            itemBlock.block = s;
            itemBlock.texture = s.texture;
            itemBlock.requirements = s.requirements;
            itemBlock.createWith = s.createWith;
            itemIndex.put(itemBlock.id(), itemBlock);
        }
    }

    private void resolveAll() {
        var res = new ContentResolver();
        for (var e : contentById.entrySet()) {
            Type type = e.getKey();
            var index = e.getValue();
            for (ContentType cont : index.values()) {
                try {
                    cont.resolve(res);
                } catch (Exception ex) {
                    log.error("[{}: '{}'] Failed to resolve", type, cont.id(), ex);
                }
            }
        }
    }

    private void generateIds() {
        // TODO: У воздуха хочется ID всегда равный 0
        int id = 0;

        {
            var air = getConstById("air");
            StaticObjectsConst.AIR = air;
            block2Id.put(air, id);
            id2Block.put(id, air);
            id++;
        }

        for (ContentType value : contentById.get(Type.BLOCK).values()) {
            if (!(value instanceof StaticObjectsConst block)) {
                throw new IllegalStateException(); // ??
            }
            if (block.id.equals("air")) {
                continue;
            }
            int blockId = id++;
            block2Id.put(block, blockId);
            id2Block.put(blockId, block);
        }
    }

    public Collection<Item> items() {
        @SuppressWarnings("unchecked")
        var collection = (Collection<Item>) (Collection<?>) contentById.get(Type.ITEM).values();
        return collection;
    }

    public int getBlockIdByType(StaticObjectsConst block) {
        return block2Id.getOrDefault(block, -1);
    }

    public StaticObjectsConst getConstByBlockId(int blockId) {
        return id2Block.get(blockId);
    }

    public Item itemById(StaticObjectsConst block) {
        return itemById(block.id);
    }

    public Item itemById(String id) {
        @SuppressWarnings("unchecked")
        var map = (HashMap<String, Item>) (HashMap<?, ?>) contentById.get(Type.ITEM);

        var cont = map.get(id);
        if (cont == null) {
            throw new IllegalStateException("Unknown item '" + id + "'");
        }
        return cont;
    }

    public StaticObjectsConst getConstById(String id) {
        @SuppressWarnings("unchecked")
        var map = (HashMap<String, StaticObjectsConst>) (HashMap<?, ?>) contentById.get(Type.BLOCK);

        var cont = map.get(id);
        if (cont == null) {
            throw new IllegalStateException("Unknown block '" + id + "'");
        }
        return cont;
    }

    public CreatureType creatureById(String id) {
        @SuppressWarnings("unchecked")
        var map = (HashMap<String, CreatureType>) (HashMap<?, ?>) contentById.get(Type.CREATURE);

        var cont = map.get(id);
        if (cont == null) {
            throw new IllegalStateException("Unknown creature '" + id + "'");
        }
        return cont;
    }

    public enum Type {
        ITEM,
        BLOCK,
        CREATURE
    }
}
