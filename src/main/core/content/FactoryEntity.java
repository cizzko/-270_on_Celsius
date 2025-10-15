package core.content;

import core.Global;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.entity.BaseBlockEntity;
import core.entity.BlockItemStorage;

public class FactoryEntity extends BaseBlockEntity<Factory> {
    public float currentEnergy, timeSinceBreakdown;
    public int currentProductionProgress;

    public ItemStack[] outputStored;

    public BlockItemStorage input, fuel;

    protected FactoryEntity(Factory block) {
        super(block);
    }

    @Override
    public void init() {
        this.outputStored = ItemStack.createArray(block.output);
        this.input = new BlockItemStorage(block.input, block.maxItemCapacity);
        this.fuel = new BlockItemStorage(block.fuel, block.maxItemCapacity);
    }

    @Override
    public void update() {
    }

    @Override
    public void draw() {
        super.draw();
        // TODO
    }

    @Override
    public void remove() {

    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }

    @Override
    public boolean itemInsertion(ItemStack item) {
        int added = input.add(item);
        if (added <= 0) {
            added = fuel.add(item);
        }
        if (added > 0) {
            item.decrement(added);
            return true;
        }
        return false;
    }
}
