package core.World.Creatures.Player.Inventory;

import static core.World.Textures.TextureLoader.BufferedImageEncoder;

public class Tools {
    public float maxHp, currentHp, damage, secBetweenHits, maxInteractionRange, zoom;
    public int countInCell, id;
    public String path;

    public Tools(float maxHp, float damage, float secBetweenHits, float maxInteractionRange, int id, String path) {
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.damage = damage;
        this.secBetweenHits = secBetweenHits;
        this.maxInteractionRange = maxInteractionRange;
        this.id = id;
        this.path = path;
        this.zoom = 64f / (BufferedImageEncoder(path).getHeight() + BufferedImageEncoder(path).getWidth());
        this.countInCell = Inventory.findCountID(id);
    }
}
