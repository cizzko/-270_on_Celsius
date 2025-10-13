package core.World;

public final class ItemUnresolved extends Item {

    public ItemUnresolved(String id) {
        super(id);
    }

    @Override
    public void load(ContentLoader cnt) { throw new UnsupportedOperationException(); }
    @Override
    public String getDescription() { throw new UnsupportedOperationException(); }
}
