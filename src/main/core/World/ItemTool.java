package core.World;

import java.io.Serializable;

public non-sealed class ItemTool extends Item implements Serializable {
    public float maxHp, damage, secBetweenHits, maxInteractionRange;

    public long lastHitTime;

    public ItemTool(String id) {
        super(id);
    }

    @Override
    public void load(ContentLoader cnt) {
        super.load(cnt);
        this.maxHp = (float) cnt.node().path("max-hp").asDouble(100);
        this.damage = (float) cnt.node().path("damage").asDouble(30);
        this.secBetweenHits = (float) cnt.node().path("sec-between-hits").asDouble(100);
        this.maxInteractionRange = (float) cnt.node().path("max-interaction-range").asDouble(8);
    }
}
