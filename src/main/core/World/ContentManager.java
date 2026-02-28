package core.World;

import core.Global;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.StaticObjectsConst_V2;
import core.content.CreatureType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

public class ContentManager {
    private static final Logger log = LogManager.getLogger();

    private final EnumMap<Type, HashMap<String, ContentType>> contentById = new EnumMap<>(Type.class);

    public void loadAll() {
        var contentDir = Global.assets.assetsDir().resolve("content");
        // чтение конфига структуры. Можно будет так сделать моды)
        // var structureJsonFile = contentDir.resolve("structure.json");

        record ContentSource(Type type, Path dir) {}
        var sources = List.of(
                new ContentSource(Type.ITEM, contentDir.resolve("items")),
                new ContentSource(Type.BLOCK, contentDir.resolve("blocks")),
                new ContentSource(Type.CREATURE, contentDir.resolve("creatures"))
        );

        var loader = new ContentLoader();

        for (ContentSource source : sources) {
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
            }
        }

        generateBlockItems();
        resolveAll();
    }

    private void generateBlockItems() {
        var itemIndex = contentById.computeIfAbsent(Type.ITEM, k -> new HashMap<>());
        for (ContentType type : contentById.get(Type.BLOCK).values()) {
            if (!(type instanceof StaticObjectsConst_V2 s)) {
                throw new IllegalStateException(); // ??
            }
            var itemBlock = new ItemBlock(s.id);
            itemBlock.block = s;
            itemBlock.texture = s.texture;
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

    public Item itemById(String id) {
        @SuppressWarnings("unchecked")
        var map = (HashMap<String, Item>) (HashMap<?, ?>) contentById.get(Type.ITEM);

        var cont = map.get(id);
        if (cont == null) {
            throw new IllegalStateException("Unknown item '" + id + "'");
        }
        return cont;
    }

    public StaticObjectsConst_V2 blockById(String id) {
        @SuppressWarnings("unchecked")
        var map = (HashMap<String, StaticObjectsConst_V2>) (HashMap<?, ?>) contentById.get(Type.BLOCK);

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
