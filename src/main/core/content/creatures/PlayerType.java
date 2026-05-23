package core.content.creatures;

import core.content.ContentLoader;

public class PlayerType extends CreatureType {

    public byte inventoryWidth, inventoryHeight;

    public PlayerType(String id) {
        super(id);
    }

    @Override
    public void load(ContentLoader cnt) {
        super.load(cnt);
        var sizeNode = cnt.node().required("InventorySize");

        inventoryWidth = (byte) sizeNode.required("Width").asInt();
        if (inventoryWidth <= 0) {
            throw cnt.malformed("'Size.Width' must be greater than 0");
        }

        inventoryHeight = (byte) sizeNode.required("Height").asInt();
        if (inventoryHeight <= 0) {
            throw cnt.malformed("'Size.Height' must be greater than 0");
        }
    }

    @Override
    protected PlayerEntity constructEntity() {
        return new PlayerEntity(this);
    }
}
