package core.World;

import core.content.ContentLoader;
import core.content.ContentResolver;

public final class ItemUnresolved extends Item {

    public ItemUnresolved(String id) {
        super(id);
    }

    @Override
    public void load(ContentLoader cnt) { throw new UnsupportedOperationException(); }

    @Override
    public void resolve(ContentResolver res) { throw new UnsupportedOperationException(); }
}
