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
        if (System.currentTimeMillis() - Global.input.getLastMouseMoveTimestamp() > 1000) {
            float xMouse = Global.input.mouseWorldPos().x;
            float yMouse = Global.input.mouseWorldPos().y;
            // SimpleColor color = SimpleColor.fromRGBA(0, 0, 0, 170);

            // if (input.total() > 0 && ArrayUtils.findFreeCell(this.inputStoredObjects) != 0) {
            //     int width1 = ArrayUtils.findDistinctObjects(this.inputStoredObjects) * 54 + playerSize;
            //
            //     Fill.rect(xMouse, yMouse, width1, 64, color);
            //     drawObjects(xMouse, yMouse, input, atlas.byPath("UI/GUI/buildMenu/factoryIn.png"));
            // }
            // if (this.outputStored.length > 0 && ArrayUtils.findFreeCell(this.outputStoredObjects) != 0) {
            //     xMouse += (ArrayUtils.findFreeCell(this.inputStoredObjects) != 0 ? 78 : 0);
            //     int width = ArrayUtils.findDistinctObjects(this.outputStoredObjects) * 54 + playerSize;
            //
            //     Fill.rect(xMouse, yMouse, width, 64, color);
            //     drawObjects(xMouse, yMouse, outputStored, atlas.byPath("UI/GUI/buildMenu/factoryOut.png"));
            // }
        }
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
