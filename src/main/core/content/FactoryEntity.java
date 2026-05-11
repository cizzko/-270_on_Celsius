package core.content;

import core.Global;
import core.Time;
import core.Window;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.Textures.TextureDrawing;
import core.entity.BaseBlockEntity;
import core.entity.BlockItemStorage;
import core.g2d.Fill;
import core.util.ArrayUtils;
import core.util.Color;

import static core.Global.atlas;
import static core.Global.batch;
import static core.World.Creatures.Player.Player.playerSize;
import static core.World.Textures.TextureDrawing.blockSize;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class FactoryEntity extends BaseBlockEntity<Factory> {
    public float energy, progress;
    public long lastMouseClickTime;

    public ItemStack[] outputStored;
    public boolean isSelected, isHovered;

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
        if (isDoubleClick() && outputStored[0] != null) {
            for (int i = 0; i < outputStored.length; i++) {
                if (outputStored[i] == null || !Inventory.addItem(outputStored[i].getItem())) {
                    break;
                }
                outputStored[i] = null;
            }
        }

        if (hasRequiredInput() && block.malfunction != Factory.Malfunction.CRITICAL && energy >= block.needEnergy) {
            progress += block.productionSpeed * Time.delta;
            int produced = (int) Math.floor(progress);
            if (produced > 0) {
                for (int c = 0; c < produced; ++c) { produceItem(); }
                progress -= produced;
            }
        }
    }

    private void produceItem() {
        for (int i = 0; i < block.output.length; i++) {
            for (int j = 0; j < outputStored.length; j++) {
                int cell = ArrayUtils.findFreeCell(outputStored);
                if (cell != -1) {
                    fuel.removeFirst();
                    input.removeFirst();
                    outputStored[cell] = block.output[i].copy();
                    // Sound.playSound(block.sound, Sound.types.EFFECT, false);
                }
            }
        }
    }

    private boolean hasRequiredInput() {
        int length = 0;
        for (ItemStack required : block.input) {
            for (ItemStack that : input.items) {
                if (required != null && that != null && required.isSame(that)) {
                    length++;
                    break;
                }
            }
        }

        return length == block.input.length;
    }

    private boolean isDoubleClick() {
        return System.currentTimeMillis() - lastMouseClickTime <= 60 && !Global.input.clicked(GLFW_MOUSE_BUTTON_LEFT);
    }

    @Override
    public void onMouseClick() {
        isSelected = !isSelected;
        lastMouseClickTime = System.currentTimeMillis();
    }

    @Override
    public void onMouseHover() {
        isHovered = true;
    }

    @Override
    public void draw() {
        if (isSelected) {
            float addedX = block.texture.width();
            float addedY = block.texture.height();
            float x = this.x * blockSize + addedX - (blockSize / 2f);
            float y = this.y * blockSize + addedY - (blockSize / 2f);

            Color color = Color.fromRgba8888(0, 0, 0, 170);
            if (!input.isEmpty()) {
                int width1 = ArrayUtils.findDistinctObjects(input.items) * 54 + playerSize;

                Fill.rect(x, y, width1, 64, color);
                TextureDrawing.drawObjects(x, y, input.items, atlas.byPath("UI/GUI/buildMenu/factoryIn.png"));
            }
            if (ArrayUtils.findFreeCell(outputStored) != 0) {
                x += (ArrayUtils.findFreeCell(outputStored) != 0 ? 78 : 0);
                int width = ArrayUtils.findDistinctObjects(outputStored) * 54 + playerSize;

                Fill.rect(x, y, width, 64, color);
                TextureDrawing.drawObjects(x, y, outputStored, atlas.byPath("UI/GUI/buildMenu/factoryOut.png"));
            }
        }
        if (isHovered) {
            isHovered = false;

            // TODO: координаты не те
            float wy = blockSize;
            float wx = blockSize;

            int iconY = (int) (wy + blockSize);
            int iconX = (int) (wx + blockSize);

            batch.draw(atlas.byPath("UI/GUI/interactionIcon.png"), iconX, iconY);
            batch.draw(Window.defaultFont.getGlyph('E'), (wx + 16) + blockSize, (wy + 12) + blockSize);
        }
    }

    @Override
    public void remove() {

    }

    @Override
    public boolean insertItem(ItemStack item) {
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
