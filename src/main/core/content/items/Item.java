package core.content.items;

import com.fasterxml.jackson.annotation.JsonCreator;
import core.Global;
import core.content.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.content.ContentLoader;
import core.content.ContentResolver;
import core.content.ContentType;
import core.g2d.Atlas;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static core.World.Textures.TextureDrawing.itemSize;

public sealed class Item implements ContentType permits ItemBlock, ItemTool, ItemUnresolved, ItemWeapon {
    public static final int DEFAULT_MAX_STACK_SIZE = 99;

    public final String id;

    public int maxStackSize = DEFAULT_MAX_STACK_SIZE;

    public float weight;
    public Atlas.Region texture;
    public ItemStack[] requirements;
    public @Nullable StaticObjectsConst createWith; // null если доступно из кармана игрока

    @JsonCreator
    public static Item deserializer(String id) {
        return Global.content.itemById(id);
    }

    public Item(String id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    @MustBeInvokedByOverriders
    public void load(ContentLoader cnt) {
        this.texture = cnt.readTexture("Texture");
        this.requirements = cnt.readItemStacksUnresolved(cnt.node().path("Requirements"));
        // TODO: не должно быть дефолтного значения
        this.weight = (float) cnt.node().path("Weight").asDouble(50);
        this.maxStackSize = cnt.node().path("MaxStackSize").asInt(DEFAULT_MAX_STACK_SIZE);

        String createWithId = cnt.node().path("CreateWith").asText(null);
        this.createWith = (createWithId == null || createWithId.equals("player")) ? null : cnt.readBlockUnresolved("CreateWith");
    }

    @Override
    public void resolve(ContentResolver res) {
        this.requirements = res.resolveItemStacks(requirements);
        if (createWith != null) {
            this.createWith = res.resolveBlock(createWith);
        }
    }

    public String getName() {
        return Global.lang.get("item." + id + ".name");
    }

    public String getDescription() {
        return Global.lang.get("item." + id + ".description");
    }

    public float uiScale() {
        return itemSize / Math.max(texture.width(), texture.height());
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Item item)) {
            return false;
        }
        return id.equals(item.id);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Item['" + id + "']";
    }
}
