package core.entity;

import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst_V2;

public class TileEntity extends BaseBlockEntity<StaticObjectsConst_V2> {

    public TileEntity(StaticObjectsConst_V2 block) {
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
