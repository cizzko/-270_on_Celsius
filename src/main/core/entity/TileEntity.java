package core.entity;

import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst;

public class TileEntity extends BaseBlockEntity<StaticObjectsConst> {

    public TileEntity(StaticObjectsConst block) {
        super(block);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }

    @Override
    public boolean itemInsertion(ItemStack item) {
        return false;
    }

    @Override
    public void init() {

    }

    @Override
    public void update() {

    }

    @Override
    public void remove() {

    }
}
