package core.content;

import core.World.ContentLoader;
import core.World.ContentType;
import core.entity.CreatureEntity;
import core.g2d.Atlas;

public abstract class CreatureType implements ContentType {
    public final String id;

    public float weight;
    public int maxHp;
    public Atlas.Region texture;
    public boolean hasGravity;

    protected CreatureType(String id) {
        this.id = id;
    }

    @Override
    public void load(ContentLoader cnt) {
        this.weight = cnt.node().path("weight").floatValue();
        this.maxHp = cnt.node().path("maxHp").intValue();
        this.texture = cnt.readTexture("texture");
        this.hasGravity = cnt.node().path("has-gravity").asBoolean(true);
    }

    public CreatureEntity create(float x, float y) {
        var ent = constructEntity();
        ent.setPosition(x, y);
        ent.init();
        return ent;
    }

    protected abstract CreatureEntity constructEntity();

    @Override
    public final String id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreatureType that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + id.hashCode();
        return h;
    }
}
