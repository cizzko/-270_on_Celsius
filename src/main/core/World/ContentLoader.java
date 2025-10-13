package core.World;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.EventHandling.Logging.Config;
import core.Global;
import core.World.ContentManager.Type;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.BlockUnresolved;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.content.ButterflyType;
import core.content.Factory;
import core.content.PlayerType;
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
        constr(Type.ITEM, "detail", ItemDetail::new);
        constr(Type.ITEM, "weapon", ItemWeapon::new);
        constr(Type.ITEM, "tool", ItemTool::new);
        constr(Type.ITEM, "block", ItemBlock::new);

        constr(Type.BLOCK, "block", StaticObjectsConst::new);
        constr(Type.BLOCK, "factory", Factory::new);

        constr(Type.CREATURE, "player", PlayerType::new);
        constr(Type.CREATURE, "butterfly", ButterflyType::new);
    }

    private static void constr(Type type, String classType, Function<String, ContentType> constr) {
        constructors.computeIfAbsent(type, k -> new HashMap<>()).put(classType, constr);
    }

    @SuppressWarnings("unchecked")
    private <T extends ContentType> T createContent(Type type, String classType, String id) {
        var typeTable = constructors.get(type);
        var constr = typeTable.get(classType);
        if (constr == null) {
            throw malformed("Unknown 'class-type': '" + classType + "'");
        }
        T cont;
        try {
            cont = (T) constr.apply(id);
        } catch (Exception e) {
            throw exception("Failed to construct content with 'content-type': '" + classType + "'", e);
        }
        return cont;
    }

    public <T extends ContentType> T readContent() {
        String classType = node.required("class-type").asText(null);
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
        try (var reader = Files.newInputStream(path)) {
            node = (ObjectNode) Config.json.readTree(reader);
        } catch (IOException e) {
            throw exception("Exception while reading json '" + path + "'", e);
        }
        this.node = node;
        if (!node.path("id").isTextual()) {
            throw malformed("'id' property must be specified as string");
        }
        this.id = node.path("id").asText();
        if (!node.path("class-type").isTextual()) {
            throw malformed("'class-type' property must be specified as string");
        }
    }

    public ObjectNode node() {
        return node;
    }

    private RuntimeException exception(String message, Exception e) {
        throw new RuntimeException(message, e);
    }

    private RuntimeException malformed(String message) {
        Path relPath = assets.assetsDir().relativize(path);
        return new RuntimeException("[" + type + ", path='" + relPath +  "', id='" + id + "'] " + message);
    }

    public ItemStack[] readItemStacksUnresolved(JsonNode node) {
        switch (node.getNodeType()) {
            case OBJECT -> {
                var itemStacks = new ItemStack[node.size()];
                int i = 0;
                for (var it = node.fields(); it.hasNext(); ) {
                    var pair = it.next();
                    String itemId = pair.getKey();
                    int count = pair.getValue().asInt(0);
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
        return Global.atlas.byPath(node.path(propName).asText(null));
    }

    public StaticObjectsConst readBlockUnresolved(String propName) {
        return new BlockUnresolved(node.path(propName).asText(null));
    }
}
