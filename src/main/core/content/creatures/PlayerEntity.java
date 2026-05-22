package core.content.creatures;

import core.Global;
import core.Time;
import core.World.WorldUtils;
import core.content.ItemGrid;
import core.content.ItemStack;
import core.content.entity.BaseCreatureEntity;
import core.content.entity.HitboxComponent;
import core.content.entity.InventoryComponent;
import core.math.Point2i;
import core.math.Vector2f;
import core.util.Debug;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

import static core.Global.*;
import static core.World.Creatures.Player.Player.*;
import static org.lwjgl.glfw.GLFW.*;

public class PlayerEntity
        extends BaseCreatureEntity<PlayerType>
        implements InventoryComponent {

    private static final Vector2f tmp = new Vector2f();

    public final Point2i itemInHandIdx = new Point2i(), draggingItemIdx = new Point2i();

    private float jumpedTicks; // откат прыжка
    private final ObjectArrayList<ObjectArrayList<@Nullable ItemStack>> items;

    protected PlayerEntity(PlayerType creature) {
        super(creature);

        this.items = new ObjectArrayList<>(creature.width);
        for (byte i = 0; i < creature.width; i++) {
            var array = new ObjectArrayList<ItemStack>(creature.height);
            for (byte j = 0; j < creature.height; j++) {
                array.add(null);
            }
            items.add(array);
        }
    }

    @Override
    public void init() {
        super.init();
        resetDraggingItem();
        resetItemInHand();
    }

    @Override
    protected void onDamage(float d) {
        lastDamage += d;
        lastDamageTime = System.currentTimeMillis();
    }

    @Override
    protected void onDead() {
        scheduler.post(() -> {
            Global.player = WorldUtils.spawn(creature, true);
        }, Time.ONE_SECOND * 5);
    }

    public void updateInput() {
        if (isDead()) {
            return;
        }

        Debug.debugHotKeys();

        if (input.justPressed(GLFW_KEY_Q)) {
            player.resetItemInHand();
        }

        //todo было 9
        float speed = noClip ? 2f : 9f * ThreadLocalRandom.current().nextFloat(1, 1.15f);
        if (input.pressed(GLFW_KEY_LEFT_SHIFT) || input.pressed(GLFW_KEY_RIGHT_SHIFT)) {
            speed *= ThreadLocalRandom.current().nextFloat(1.5f, 1.75f);
        }

        if (noClip) {
            speed *= Math.max(0, input.getScrollOffset());
        }

        int xf = input.axis(GLFW_KEY_A, GLFW_KEY_D);

        if (!noClip) {
            tmp.set(xf, 0).scale(speed * Time.delta);
        } else {
            velocity.set(0, 0);

            setX(x() + speed * xf);
            int yf = input.axis(GLFW_KEY_S, GLFW_KEY_W);
            setY(y() + speed * yf);
        }

        if (jumpedTicks > 0) {
            jumpedTicks = Math.max(jumpedTicks - Time.delta, 0);
        } else {
            if (hasFloor() && Math.abs(velocity.y) <= GAP && input.pressed(GLFW_KEY_SPACE)) {
                tmp.y += 9;
                jumpedTicks = 5 * Math.max(Time.delta, 0.01f);
            }
        }

        velocity.add(tmp);

        Point2i blockUnderMouse = Global.input.mouseBlockPos();
        if (world.getBlockId(blockUnderMouse.x, blockUnderMouse.y) <= 0) {
            return;
        }
        var blockEntity = world.getEntity(blockUnderMouse.x, blockUnderMouse.y);
        if (blockEntity != null) {
            if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT)) {
                blockEntity.onMouseClick();
            }

            blockEntity.onMouseHover();

            final char interactionChar = 'E';
            if (input.pressed(interactionChar)) {
//                blockEntity.onInteraction();
                // ====== То есть это хотим ======
//                int iconY = (root.y * blockSize) + blockSize;
//                int iconX = (root.x * blockSize) + blockSize;
//                batch.draw(atlas.get("UI/GUI/interactionIcon"), iconX, iconY);
//                batch.draw(Window.defaultFont.getGlyph(interactionChar),
//                        (root.x * blockSize + 16) + blockSize,
//                        (root.y * blockSize + 12) + blockSize);
//
//                if (interactionButtonPressed) {
//                    currentInteraction = new Thread(interaction);
//                    currentInteraction.start();
//                }
            }
        }

    }

    @Override
    public CollisionResult onCollide(HitboxComponent them) {
        return CollisionResult.WALKTHROUGH;
    }

    @Override
    public String toString() {
        return "Player#" + id;
    }

    public boolean hasDraggingItem() {
        return draggingItemIdx.x != -1 && draggingItemIdx.y != -1;
    }

    public @Nullable ItemStack getDraggingItem() {
        if (hasItemInHand()) {
            return items.get(itemInHandIdx.x).get(itemInHandIdx.y);
        }
        return null;
    }

    public void setItem(int x, int y, @Nullable ItemStack itemStack) { items.get(x).set(y, itemStack); }
    public void setItem(Point2i pos, @Nullable ItemStack itemStack) { setItem(pos.x, pos.y, itemStack); }
    public @Nullable ItemStack getItem(int x, int y) { return items.get(x).get(y); }
    public @Nullable ItemStack getItem(Point2i pos) { return getItem(pos.x, pos.y); }

    public boolean hasItemInHand() {
        return itemInHandIdx.x != -1 && itemInHandIdx.y != -1;
    }

    public @Nullable ItemStack getItemInHand() {
        if (hasItemInHand()) {
            return items.get(itemInHandIdx.x).get(itemInHandIdx.y);
        }
        return null;
    }

    @Override
    public ObjectArrayList<ObjectArrayList<@Nullable ItemStack>> items() {
        return items;
    }

    @Override
    public TransitionResult addItem(ItemStack itemStack) {
        return ItemGrid.tryAddTo(items, itemStack, 7, 5);
    }

    public void takeItem(int x, int y, int count) {
        var item = items.get(x).get(y);
        if (item != null && item.decrement(count)) {
            setItem(x, y, null);
        }
    }

    public void takeItemFromHand(int count) {
        var item = getItemInHand();
        if (item != null && item.decrement(count)) {
            setItem(itemInHandIdx.x, itemInHandIdx.y, null);
            resetItemInHand();
        }
    }

    public void resetItemInHand() {
        itemInHandIdx.set(-1, -1);
    }

    public void resetDraggingItem() {
        draggingItemIdx.set(-1, -1);
    }
}
