package core.World;

import core.Global;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.content.Registry;
import core.content.creatures.CreatureType;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
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

public class ContentManager {
    private static final Logger log = LogManager.getLogger();

    public final Registry<Item> itemsRegistry = new Registry<>();
    public final Registry<StaticObjectsConst> blocksRegistry = new Registry<>();
    public final Registry<CreatureType> creaturesRegistry = new Registry<>();

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
                new ContentSource(Type.CREATURE, contentDir.resolve("creatures"))
        );

        final EnumMap<Type, HashMap<String, ContentType>> contentMap = new EnumMap<>(Type.class);
        var loader = new ContentLoader();

        for (ContentSource source : sources) {
            try (var dirstr = Files.newDirectoryStream(source.dir, file -> file.getFileName().toString().endsWith(".json"))) {
                var index = contentMap.computeIfAbsent(source.type, k -> new HashMap<>());

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
                Global.app.quit();
            }
        }

        generateBlockItems(contentMap);
        resolveAll(contentMap);
        loadCrafts(contentMap);
        generateIds(contentMap);
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
        for (ContentType value : contentMap.get(Type.ITEM).values()) {
            if (!(value instanceof Item item)) {
                throw new IllegalStateException(); // ??
            }
            itemsRegistry.put1(item);
            itemsRegistry.put2(item);
        }

        for (ContentType value : contentMap.get(Type.ITEM).values()) {
            if (!(value instanceof Item item)) {
                throw new IllegalStateException(); // ??
            }
            itemsRegistry.put1(item);
        }

        for (ContentType value : contentMap.get(Type.BLOCK).values()) {
            if (!(value instanceof StaticObjectsConst block)) {
                throw new IllegalStateException(); // ??
            }
            blocksRegistry.put1(block);
        }

        {
            var air = getConstById("air");
            StaticObjectsConst.AIR = air;
            blocksRegistry.put2(air);
        }

        for (ContentType value : contentMap.get(Type.BLOCK).values()) {
            if (!(value instanceof StaticObjectsConst block)) {
                throw new IllegalStateException(); // ??
            }
            if (block.id.equals("air")) {
                continue;
            }
            blocksRegistry.put2(block);
        }


        for (ContentType value : contentMap.get(Type.CREATURE).values()) {
            if (!(value instanceof CreatureType creature)) {
                throw new IllegalStateException(); // ??
            }
            creaturesRegistry.put1(creature);
            creaturesRegistry.put2(creature);
        }

        itemsRegistry.trim();
        blocksRegistry.trim();
        creaturesRegistry.trim();
    }

    public List<Item> getCraftsFor(StaticObjectsConst createWith) {
        return createWith == null
                ? craftableByPlayer
                : craftableByWorkbench.get(blocksRegistry.idByType(createWith));
    }

    public ObjectSet<Item> items() {
        return itemsRegistry.values();
    }

    public int getBlockIdByType(StaticObjectsConst block) {
        return blocksRegistry.idByType(block);
    }

    public StaticObjectsConst getConstByBlockId(int blockId) {
        return blocksRegistry.typeById(blockId);
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
        CREATURE
    }
}
