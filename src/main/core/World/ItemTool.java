package core.World;

import java.io.Serializable;

public non-sealed class ItemTool extends Item implements Serializable {
    public int maxHp, damage;
    public float secBetweenHits, maxInteractionRange;

    public ItemTool(String id) {
        super(id);
    }

    @Override
    public void load(ContentLoader cnt) {
        super.load(cnt);
        this.maxHp = cnt.node().path("MaxHp").asInt(100);
        this.damage = cnt.node().path("Damage").asInt(30);
        this.secBetweenHits = (float) cnt.node().path("SecBetweenHits").asDouble(100);
        this.maxInteractionRange = (float) cnt.node().path("MaxInteractionRange").asDouble(8);
    }
}
