package core.content;

import core.Global;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.content.creatures.CreatureType;
import core.content.items.Item;
import core.content.items.ItemBlock;
import core.content.strctures.Structure;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

public final class ContentManager {
    private static final Logger log = LogManager.getLogger();

    public Registry<Item> itemsRegistry;
    public Registry<StaticObjectsConst> blocksRegistry;
    public Registry<CreatureType> creaturesRegistry;
    public Registry<Structure> structuresRegistry;

    private final ArrayList<Item> craftableByPlayer = new ArrayList<>();
    private final Int2ObjectOpenHashMap<ArrayList<Item>> craftableByWorkbench = new Int2ObjectOpenHashMap<>();

    public void loadAll() {
        var contentDir = Global.assets.assetsDir().resolve("content");
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

                new ContentSource(Type.ITEM, contentDir.resolve("items")),
                new ContentSource(Type.BLOCK, contentDir.resolve("blocks")),
                new ContentSource(Type.CREATURE, contentDir.resolve("creatures")),
                new ContentSource(Type.STRUCTURE, contentDir.resolve("structures"))
        );

        final EnumMap<Type, HashMap<String, ContentType>> contentMap = new EnumMap<>(Type.class);
        var loader = new ContentLoader();

        for (ContentSource source : sources) {
            try (var dirstr = Files.newDirectoryStream(source.dir, file -> file.getFileName().toString().endsWith(".json"))) {
                var index = contentMap.computeIfAbsent(source.type, k -> new HashMap<>());
                for (Path file : dirstr) {
                    loader.init(source.type, file);
                    try {
                        var cont = loader.readContent();
                        index.put(cont.id(), cont);
                    } catch (Exception e) {
                        log.error("Failed to load content: '{}'", file, e);
                    }
                }
            } catch (IOException e) {
                log.error("Failed to list directory: '{}'", source.dir, e);
                throw new UncheckedIOException(e);
            } catch (Exception e) {
                log.error("Failed to read directory: '{}'", source.dir, e);
                Global.app.quit();
            }
        }

        generateBlockItems(contentMap);
        resolveAll(contentMap);
        generateIds(contentMap);
        loadCrafts(contentMap);
    }

    private void loadCrafts(EnumMap<Type, HashMap<String, ContentType>> contentMap) {
        for (var value : contentMap.get(Type.ITEM).values()) {
            if (!(value instanceof Item item)) {
                throw new IllegalStateException(); // ???
            }
            if (item.requirements == ItemStack.EMPTY_ARRAY) {
                continue;
            }
            if (item.createWith == null) {
                // Крафт из рук
                craftableByPlayer.add(item);
            } else {
                int blockId = blocksRegistry.idByType(item.createWith);
                craftableByWorkbench.computeIfAbsent(blockId, k -> new ArrayList<>()).add(item);
            }
        }

        craftableByPlayer.trimToSize();
        craftableByWorkbench.values().forEach(ArrayList::trimToSize);
    }

    private void generateBlockItems(EnumMap<Type, HashMap<String, ContentType>> contentMap) {
        var itemIndex = contentMap.computeIfAbsent(Type.ITEM, k -> new HashMap<>());
        for (ContentType type : contentMap.get(Type.BLOCK).values()) {
            if (!(type instanceof StaticObjectsConst s)) {
                throw new IllegalStateException(); // ??
            }
            var itemBlock = new ItemBlock(s.id);
            itemBlock.block = s;
            itemBlock.weight = 50;
            itemBlock.texture = s.texture;
            itemBlock.requirements = s.requirements;
            itemBlock.createWith = s.createWith;

            itemIndex.put(itemBlock.id(), itemBlock);
        }
    }

    private void resolveAll(EnumMap<Type, HashMap<String, ContentType>> contentMap) {
        var res = new ContentResolver(contentMap);
        contentMap.forEach((type, index) -> {
            for (ContentType cont : index.values()) {
                try {
                    cont.resolve(res);
                } catch (Exception ex) {
                    log.error("[{}: '{}'] Failed to resolve", type, cont.id(), ex);
                }
            }
        });
    }

    private void generateIds(EnumMap<Type, HashMap<String, ContentType>> contentMap) {

        // TODO в идеале предмет воздуха тоже с id 0
        var blockList = contentMap.get(Type.BLOCK).values();

        // +1 из-за воздуха. см ниже
        var blgen = new RegistryGenerator<>(StaticObjectsConst.class, blockList.size() + 1);
        for (ContentType value : blockList) {
            if (!(value instanceof StaticObjectsConst block)) {
                throw new IllegalStateException(); // ??
            }
            blgen.putName(block);
        }

        {
            var air = blgen.name2Type.get("air");
            StaticObjectsConst.AIR = air;
            blgen.putId(air);
        }

        for (ContentType value : blockList) {
            if (!(value instanceof StaticObjectsConst block)) {
                throw new IllegalStateException(); // ??
            }
            if (block.id.equals("air")) {
                continue;
            }
            blgen.putId(block);
        }

        itemsRegistry = indexContent(contentMap, Type.ITEM, Item.class);
        creaturesRegistry = indexContent(contentMap, Type.CREATURE, CreatureType.class);
        structuresRegistry = indexContent(contentMap, Type.STRUCTURE, Structure.class);
        blocksRegistry = blgen.complete();
    }

    private <C extends ContentType> Registry<C> indexContent(EnumMap<Type, HashMap<String, ContentType>> contentMap,
                                                             Type type, Class<C> contentType) {
        var cntList = contentMap.get(type).values();
        var gen = new RegistryGenerator<>(contentType, cntList.size());
        for (ContentType value : cntList) {
            C cnt = contentType.cast(value);
            gen.putName(cnt);
            gen.putId(cnt);
        }
        return gen.complete();
    }

    public List<Item> getCraftsFor(StaticObjectsConst createWith) {
        return createWith == null
                ? craftableByPlayer
                : craftableByWorkbench.get(blocksRegistry.idByType(createWith));
    }

    public Item itemById(StaticObjectsConst block) {
        return itemById(block.id);
    }

    public Item itemById(String id) {
        var cont = itemsRegistry.typeByName(id);
        if (cont == null) {
            throw new IllegalStateException("Unknown item '" + id + "'");
        }
        return cont;
    }

    public StaticObjectsConst getConstById(String id) {
        var cont = blocksRegistry.typeByName(id);
        if (cont == null) {
            throw new IllegalStateException("Unknown block '" + id + "'");
        }
        return cont;
    }

    public CreatureType creatureById(String id) {
        var cont = creaturesRegistry.typeByName(id);
        if (cont == null) {
            throw new IllegalStateException("Unknown creature '" + id + "'");
        }
        return cont;
    }

    public enum Type {
        ITEM,
        BLOCK,
        CREATURE,
        STRUCTURE,
    }
}
