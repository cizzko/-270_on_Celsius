package core.content;

import com.fasterxml.jackson.databind.EnumNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.EnumNaming;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import core.util.Config;
import core.Global;
import core.content.blocks.Block;
import core.content.creatures.Creature;
import core.content.items.Item;
import core.content.items.ItemBlock;
import core.content.strctures.Structure;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.*;

public final class ContentManager {
    private static final Logger log = LogManager.getLogger();

    public Registry<Item> itemsRegistry;
    public Registry<Block> blocksRegistry;
    public Registry<Creature> creaturesRegistry;
    public Registry<Structure> structuresRegistry;
    public Registry<Tag<?>> tagsRegistry;

    private final short[] blockTypeEndId = new short[Block.Type.VALUES.length];

    private final ArrayList<Item> craftableByPlayer = new ArrayList<>();
    private final Short2ObjectOpenHashMap<ArrayList<Item>> craftableByWorkbench = new Short2ObjectOpenHashMap<>();

    public boolean isBlockType(short blockId, Block.Type type) {
        int i = type.ordinal();
        int start = i == 0 ? 0 : blockTypeEndId[i - 1] + 1;
        int end = blockTypeEndId[i];
        return blockId >= start && blockId <= end;
    }

    record ContentSource(Type type, Path dir) {}
    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    record TagData(Type classType, List<String> elements) {}

    public void loadAll() {
        var contentDir = Global.assets.assetsDir().resolve("content");
        // чтение конфига структуры. Можно будет так сделать моды)
        // var structureJsonFile = contentDir.resolve("structure.json");

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

        var contentMap = new Reference2ObjectOpenHashMap<Class<? extends ContentType>, HashMap<String, ContentType>>();
        var loader = new ContentLoader();

        for (ContentSource source : sources) {
            loadContentType(source, contentMap, loader);
        }

        generateBlockItems(contentMap);
        generateIds(contentMap);
        loadTags(contentDir, contentMap);
        resolveAll(contentMap);
        loadCrafts(contentMap);
    }

    private static void loadContentType(
            ContentSource source,
            Reference2ObjectOpenHashMap<Class<? extends ContentType>, HashMap<String, ContentType>> contentMap,
            ContentLoader loader)
    {
        var index = contentMap.computeIfAbsent(source.type.classType, k -> new HashMap<>());
        try (var dirstr = Files.newDirectoryStream(source.dir, file -> file.getFileName().toString().endsWith(".json"))) {
            for (Path file : dirstr) {
                try {
                    loader.init(source.type, file);
                    var cont = loader.readContent();
                    index.put(cont.key(), cont);
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

    private void loadTags(
            Path contentDir,
            Reference2ObjectOpenHashMap<Class<? extends ContentType>, HashMap<String, ContentType>> contentMap)
    {

        var tagsDir = contentDir.resolve("tags");
        var tagMap = contentMap.computeIfAbsent(Tag.class, k -> new HashMap<>());

        try (var dirstr = Files.newDirectoryStream(tagsDir, file -> file.getFileName().toString().endsWith(".json"))) {
            for (Path file : dirstr) {
                TagData tagData;
                try (var is = Files.newInputStream(file)) {
                    tagData = Config.json.readValue(is, TagData.class);
                } catch (Exception e) {
                    log.error("Failed to load content: '{}'", file, e);
                    continue;
                }
                var byName = contentMap.get(tagData.classType.classType);
                Objects.requireNonNull(byName);

                tagData.elements.removeIf(id -> {
                    if (!byName.containsKey(id)) {
                        log.error("['{}'] can't find tagged content {} with id '{}'",
                                file, tagData.classType.name().toLowerCase(Locale.ROOT), id);
                        return true;
                    }
                    return false;
                });
                String id = ContentLoader.pathToId(file);
                //noinspection SimplifyStreamApiCallChains
                var elements = tagData.elements.stream()
                        .map(byName::get)
                        .collect(Collectors.toUnmodifiableList());

                @SuppressWarnings("unchecked")
                var value = new Tag<>(id, (Class<ContentType>) tagData.classType.classType, Set.copyOf(tagData.elements), elements);
                tagMap.put(id, value);
            }
        } catch (IOException e) {
            log.error("Failed to list directory: '{}'", tagsDir, e);
            throw new UncheckedIOException(e);
        } catch (Exception e) {
            log.error("Failed to read directory: '{}'", tagsDir, e);
            Global.app.quit();
        }

        @SuppressWarnings("unchecked") // java sucks
        var javaSucks = (Registry<Tag<?>>) (Registry<?>) indexContent(contentMap, Tag.class);
        tagsRegistry = javaSucks;
    }

    @SuppressWarnings("unchecked")
    static <C extends ContentType> HashMap<String, C> content(
            Reference2ObjectOpenHashMap<Class<? extends ContentType>, HashMap<String, ContentType>> contentMap,
            Class<C> type)
    {
        return (HashMap<String, C>) (HashMap<String, ?>) contentMap.get(type);
    }

    private void loadCrafts(Reference2ObjectOpenHashMap<Class<? extends ContentType>, HashMap<String, ContentType>> contentMap) {
        for (var item : content(contentMap, Item.class).values()) {
            if (item.requirements == ItemStack.EMPTY_ARRAY) {
                continue;
            }
            if (item.createWith == null) {
                // Крафт из рук
                craftableByPlayer.add(item);
            } else {
                craftableByWorkbench.computeIfAbsent(item.createWith.id, k -> new ArrayList<>()).add(item);
            }
        }

        craftableByPlayer.trimToSize();
        craftableByWorkbench.values().forEach(ArrayList::trimToSize);
    }

    private void generateBlockItems(Reference2ObjectOpenHashMap<Class<? extends ContentType>, HashMap<String, ContentType>> contentMap) {
        var itemIndex = contentMap.computeIfAbsent(Item.class, k -> new HashMap<>());
        for (var block : content(contentMap, Block.class).values()) {
            var itemBlock = new ItemBlock(block.key);
            itemBlock.block = block;
            itemBlock.mass = 50;
            itemBlock.texture = block.texture;
            itemBlock.requirements = block.requirements;
            itemBlock.createWith = block.createWith;

            itemIndex.put(itemBlock.key(), itemBlock);
        }
    }

    private void resolveAll(Reference2ObjectOpenHashMap<Class<? extends ContentType>, HashMap<String, ContentType>> contentMap) {
        var res = new ContentResolver(contentMap);
        contentMap.forEach((type, index) -> {
            for (var cont : index.values()) {
                if (cont instanceof Loadable loadable) {
                    try{
                        loadable.resolve(res);
                    } catch (Exception ex) {
                        log.error("[{}: '{}'] Failed to resolve", type, cont.key(), ex);
                    }
                }
            }
        });
    }

    public Block.Type getBlockType(short blockId) {
        for (int i = 0; i < blockTypeEndId.length; i++) {
            int start = i == 0 ? 0 : blockTypeEndId[i - 1];
            int end = blockTypeEndId[i];
            if (blockId >= start && blockId <= end) {
                return Block.Type.VALUES[i];
            }
        }
        throw new IllegalArgumentException("Invalid block id: " + blockId);
    }

    private void generateIds(Reference2ObjectOpenHashMap<Class<? extends ContentType>, HashMap<String, ContentType>> contentMap) {

        // TODO в идеале предмет воздуха тоже с id 0
        var blockList = content(contentMap, Block.class).values().stream()
                .sorted(comparing((Block a) -> a.type)
                        .thenComparing(a -> a.key))
                .toList();

        var blgen = new RegistryGenerator<>(Block.class, blockList.size());
        for (var block : blockList) {
            blgen.putName(block);
        }

        {
            var air = blgen.name2Type.get("air");
            Block.AIR = air;
            blgen.putId(air);
        }

        for (Block block : blockList) {
            if (block.key.equals("air")) {
                continue;
            }
            blgen.putId(block);
            blockTypeEndId[block.type.ordinal()] = block.id;
        }

        itemsRegistry = indexContent(contentMap, Item.class);
        creaturesRegistry = indexContent(contentMap, Creature.class);
        structuresRegistry = indexContent(contentMap, Structure.class);
        blocksRegistry = blgen.complete();
    }

    private <C extends ContentType> Registry<C> indexContent(
            Reference2ObjectOpenHashMap<Class<? extends ContentType>, HashMap<String, ContentType>> contentMap,
            Class<C> contentType)
    {
        var cntList = content(contentMap, contentType).values().stream()
                .sorted(comparing(ContentType::key))
                .toList();
        var gen = new RegistryGenerator<>(contentType, cntList.size());
        for (var value : cntList) {
            C cnt = contentType.cast(value);
            gen.putName(cnt);
            gen.putId(cnt);
        }
        return gen.complete();
    }

    public List<Item> getCraftsFor(Block createWith) {
        return createWith == null
                ? craftableByPlayer
                : craftableByWorkbench.get(createWith.id);
    }

    // TODO странные названия
    public Item itemById(Block block) {
        return itemById(block.key);
    }

    public Item itemById(String id) {
        var cont = itemsRegistry.typeByName(id);
        if (cont == null) {
            throw new IllegalStateException("Unknown item '" + id + "'");
        }
        return cont;
    }

    public Block blockById(String id) {
        var cont = blocksRegistry.typeByName(id);
        if (cont == null) {
            throw new IllegalStateException("Unknown block '" + id + "'");
        }
        return cont;
    }

    public Creature creatureById(String id) {
        var cont = creaturesRegistry.typeByName(id);
        if (cont == null) {
            throw new IllegalStateException("Unknown creature '" + id + "'");
        }
        return cont;
    }

    public <C extends ContentType> Tag<C> tagById(String id) {
        var cont = tagsRegistry.typeByName(id);
        if (cont == null) {
            throw new IllegalStateException("Unknown tag '" + id + "'");
        }
        @SuppressWarnings("unchecked")
        var javaSucks = (Tag<C>) cont;
        return javaSucks;
    }

    @EnumNaming(EnumNamingStrategies.LowerCamelCaseStrategy.class)
    public enum Type {
        ITEM(Item.class),
        BLOCK(Block.class),
        CREATURE(Creature.class),
        STRUCTURE(Structure.class);

        public final Class<? extends ContentType> classType;

        Type(Class<? extends ContentType> classType) {
            this.classType = classType;
        }
    }
}
