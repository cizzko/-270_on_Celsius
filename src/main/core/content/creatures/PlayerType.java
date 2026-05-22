package core.content.creatures;

import core.content.ContentLoader;

public class PlayerType extends CreatureType {

    public byte width, height;

    public PlayerType(String id) {
        super(id);
    }

    @Override
    public void load(ContentLoader cnt) {
        super.load(cnt);
        var sizeNode = cnt.node().required("InventorySize");

        width = (byte) sizeNode.required("Width").asInt();
        if (width <= 0) {
            throw cnt.malformed("'Size.Width' must be greater than 0");
        }

        height = (byte) sizeNode.required("Height").asInt();
        if (height <= 0) {
            throw cnt.malformed("'Size.Height' must be greater than 0");
        }
    }

    @Override
    protected PlayerEntity constructEntity() {
        return new PlayerEntity(this);
    }
}
