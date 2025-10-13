package core.World;

import core.World.StaticWorldObjects.StaticObjectsConst;

public non-sealed class ItemBlock extends Item {
    // То, что можно поставить
    public StaticObjectsConst block;

    public ItemBlock(String id) {
        super(id);
    }

    @Override
    public void load(ContentLoader cnt) {
        super.load(cnt);
        this.block = cnt.readBlockUnresolved("block");
    }

    @Override
    public void resolve(ContentResolver res) {
        block = res.resolveBlock(block);
    }
}
