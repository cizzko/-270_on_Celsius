package core.content.creatures;

import core.Global;
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
        this.weight = cnt.node().path("Weight").floatValue();
        this.maxHp = cnt.node().path("MaxHp").intValue();
        this.texture = cnt.readTexture("Texture");
        this.hasGravity = cnt.node().path("HasGravity").asBoolean(true);
    }

    public CreatureEntity create(float x, float y) {
        int id = Global.entityPool.acquireId();

        var ent = constructEntity();
        ent.setId(id);
        ent.setPosition(x, y);
        ent.init();

        Global.entityPool.add(ent);
        return ent;
    }

    protected abstract CreatureEntity constructEntity();

    @Override
    public final String id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreatureType that)) {
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + id.hashCode();
        return h;
    }
}
