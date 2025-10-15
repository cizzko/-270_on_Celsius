package core.World;

import core.g2d.Atlas;

import java.util.Objects;

public abstract sealed class Item implements ContentType permits ItemBlock, ItemDetail, ItemTool, ItemUnresolved, ItemWeapon {
    public final String id;

    public Atlas.Region texture;

    public Item(String id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    public void load(ContentLoader cnt) {
        this.texture = cnt.readTexture("texture");
    }

    public String getDescription() {
        // return Json.getName("item." + id + ".description"); TODO продумать правило формирование имён
        return null;
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
