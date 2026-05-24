package core.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.EventHandling.Config;
import core.Global;
import core.content.ContentManager.Type;
import core.content.blocks.BlockUnresolved;
import core.content.blocks.Block;
import core.content.blocks.Factory;
import core.content.creatures.PlayerType;
import core.content.blocks.Chest;
import core.content.blocks.Workbench;
import core.content.items.*;
import core.content.strctures.Structure;
import core.g2d.Atlas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.function.Function;

import static core.Global.assets;

public class ContentLoader {
    private static final EnumMap<Type, HashMap<String, Function<String, ContentType>>> constructors = new EnumMap<>(Type.class);
    static {
        ctor(Type.ITEM, "item", Item::new);
        ctor(Type.ITEM, "weapon", ItemWeapon::new);
        ctor(Type.ITEM, "tool", ItemTool::new);
        ctor(Type.ITEM, "block", ItemBlock::new);

        ctor(Type.BLOCK, "block", Block::new);
        ctor(Type.BLOCK, "workbench", Workbench::new);
        ctor(Type.BLOCK, "chest", Chest::new);
        ctor(Type.BLOCK, "factory", Factory::new);

        ctor(Type.CREATURE, "player", PlayerType::new);

        ctor(Type.STRUCTURE, "structure", Structure::new);
    }

    private static void ctor(Type type, String classType, Function<String, ContentType> constr) {
        constructors.computeIfAbsent(type, k -> new HashMap<>()).put(classType, constr);
    }

    @SuppressWarnings("unchecked")
    private <T extends ContentType & Loadable> T createContent(Type type, String classType, String id) {
        var typeTable = constructors.get(type);
        var constr = typeTable.get(classType);
        if (constr == null) {
            throw malformed("Unknown 'ClassType': '" + classType + "'");
        }
        T cont;
        try {
            cont = (T) constr.apply(id);
        } catch (Exception e) {
            throw exception("Failed to construct content with 'content-type': '" + classType + "'", e);
        }
        return cont;
    }

    public <T extends ContentType & Loadable> T readContent() {
        String classType = node.required("ClassType").asText(null);
        T content = createContent(type, classType, id);
        content.load(this);
        return content;
    }

    private String id;
    private Type type;
    private Path path;

    private ObjectNode node;

    public void init(Type type, Path path) {
        this.type = type;
        this.path = path;
        ObjectNode node;
        try (var is = Files.newInputStream(path)) {
            node = (ObjectNode) Config.json.readTree(is);
        } catch (IOException e) {
            throw exception("Exception while reading json '" + path + "'", e);
        }
        this.node = node;
        this.id = pathToId(path);
        if (!node.path("ClassType").isTextual()) {
            throw malformed("'ClassType' property must be specified as string");
        }
    }

    static String pathToId(Path path) {
        var fileName = path.getFileName().toString();
        if (fileName.endsWith(".json")) {
            return fileName.substring(0, fileName.length() - 5);
        }
        assert false : fileName + " without extension";
        return fileName;
    }

    public ObjectNode node() {
        return node;
    }

    private RuntimeException exception(String message, Exception e) {
        throw new RuntimeException(message, e);
    }

    public RuntimeException malformed(String message) {
        Path relPath = assets.assetsDir().relativize(path);
        return new RuntimeException("[" + type + ", path='" + relPath +  "', id='" + id + "'] " + message);
    }

    public ItemStack[] readItemStacksUnresolved(JsonNode node) {
        switch (node.getNodeType()) {
            case OBJECT -> {
                if (node.isEmpty()) {
                    return ItemStack.EMPTY_ARRAY;
                }
                var itemStacks = new ItemStack[node.size()];
                int i = 0;
                for (var pair : node.properties()) {
                    String itemId = pair.getKey();
                    int count = pair.getValue().asInt(1);
                    itemStacks[i++] = new ItemStack(new ItemUnresolved(itemId), count);
                }
                return itemStacks;
            }
            case MISSING, NULL -> {
                return ItemStack.EMPTY_ARRAY;
            }
            default -> throw malformed("'requirements' must be an object");
        }
    }

    public Atlas.Region readTexture(String propName) {
        return Global.atlas.get(node.path(propName).asText(null));
    }

    public Block readBlockUnresolved(String propName) {
        return new BlockUnresolved(node.path(propName).asText(null));
    }
}
