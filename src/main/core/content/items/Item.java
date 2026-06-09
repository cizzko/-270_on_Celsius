package core.content.items;

import com.fasterxml.jackson.annotation.JsonCreator;
import core.Global;
import core.content.*;
import core.content.blocks.Block;
import core.g2d.Atlas;
import core.math.MathUtil;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static core.graphic.GuiDrawing.itemSize;
import static core.math.MathUtil.toShortExact;

public sealed class Item implements ContentType, Loadable
        permits ItemBlock, ItemTool, ItemUnresolved, ItemWeapon {
    public static final short DEFAULT_MAX_STACK_SIZE = 99;

    public final String key;

    public short id;

    public short maxStackSize = DEFAULT_MAX_STACK_SIZE;
    public short createCount;

    public float weight;
    public Atlas.Region texture;
    public ItemStack[] requirements;
    public @Nullable Block createWith; // null если доступно из кармана игрока

    @JsonCreator
    public static Item deserializer(String key) {
        return Global.content.itemById(key);
    }

    public Item(String key) {
        this.key = Objects.requireNonNull(key);
    }

    public final String key() {
        return key;
    }

    public final short id() { return id; }
    public final void setId(short id) { this.id = id; }

    @Override
    @MustBeInvokedByOverriders
    public void load(ContentLoader cnt) {
        this.texture = cnt.readTexture("Texture");
        this.requirements = cnt.readItemStacksUnresolved(cnt.node().path("Requirements"));
        // TODO: не должно быть дефолтного значения
        this.weight = (float) cnt.node().path("Weight").asDouble(50);
        this.maxStackSize = toShortExact(cnt.node().path("MaxStackSize").asInt(DEFAULT_MAX_STACK_SIZE));
        this.createCount = toShortExact(cnt.node().path("CreateCount").asInt(1));

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
        return Global.lang.get("items." + key + ".name");
    }

    public String getDescription() {
        return Global.lang.get("items." + key + ".description");
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
        return key.equals(item.key);
    }

    @Override
    public final int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "Item['" + key + "']";
    }
}
