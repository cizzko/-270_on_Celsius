package core.World.Creatures.Player.Inventory.Items;

public class Tools {
    public float maxHp, currentHp, damage, secBetweenHits, maxInteractionRange;
    public String name;
    public long lastHitTime = System.currentTimeMillis();

    public Tools(float maxHp, float damage, float secBetweenHits, float maxInteractionRange, String name) {
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.damage = damage;
        this.secBetweenHits = secBetweenHits;
        this.maxInteractionRange = maxInteractionRange;
        this.name = name;
    }
}
