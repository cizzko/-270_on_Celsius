package core.content.items;

import core.content.ContentLoader;
import core.content.ContentResolver;
import core.content.blocks.Block;

public non-sealed class ItemBlock extends Item {
    // То, что можно поставить
    public Block block;

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
        super.resolve(res);
        block = res.resolveBlock(block);
    }
}
