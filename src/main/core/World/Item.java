package core.World;

import core.Global;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.g2d.Atlas;

import java.util.Objects;

public abstract sealed class Item implements ContentType permits ItemBlock, ItemDetail, ItemTool, ItemUnresolved, ItemWeapon {
    public final String id;

    public Atlas.Region texture;
    public ItemStack[] requirements;
    public StaticObjectsConst createWith; // null если доступно из кармана игрока

    public Item(String id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    public void load(ContentLoader cnt) {
        this.texture = cnt.readTexture("Texture");
        this.requirements = cnt.readItemStacksUnresolved(cnt.node().path("Requirements"));

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

    public float getUiScale() {
        // 32 - target structure size
        return 32f / Math.max(texture.width(), texture.height());
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
