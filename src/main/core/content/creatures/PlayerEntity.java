package core.content.creatures;

import core.Global;
import core.Time;
import core.World.Creatures.Player.WorkbenchMenu.WorkbenchLogic;
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

import static core.Global.*;
import static core.PlayGameScene.CAMERA_OFFSET_X;
import static core.PlayGameScene.CAMERA_OFFSET_Y;
import static core.World.Creatures.Physics.GRAVITY;
import static core.World.Creatures.Player.Player.noClip;
import static core.WorldCoordinates.toWorld;
import static org.lwjgl.glfw.GLFW.*;

public class PlayerEntity
        extends BaseCreatureEntity<PlayerType>
        implements InventoryComponent {

    private static final Vector2f tmp = new Vector2f();
    private static final Vector2f direction = new Vector2f();

    public final Point2i itemInHandIdx = new Point2i(), draggingItemIdx = new Point2i();

    private float jumpedTicks; // откат прыжка
    private final ObjectArrayList<ObjectArrayList<@Nullable ItemStack>> items;

    public float lastDamage = 0;
    public long lastDamageTime = 0;

    protected PlayerEntity(PlayerType creature) {
        super(creature);

        this.items = new ObjectArrayList<>(creature.inventoryWidth);
        for (byte i = 0; i < creature.inventoryWidth; i++) {
            var array = new ObjectArrayList<ItemStack>(creature.inventoryHeight);
            for (byte j = 0; j < creature.inventoryHeight; j++) {
                array.add(null);
            }
            items.add(array);
        }
    }

    public void init() {
        super.init();
        resetDraggingItem();
        resetItemInHand();
    }

    public double centerX() { return x + toWorld(creature.texture.width()) / 2f; }
    public double centerY() { return y + toWorld(creature.texture.height()) / 2f; }

    protected void onDamage(float d) {
        lastDamage += d;
        lastDamageTime = System.currentTimeMillis();
    }

    protected void onDead() {
        lastDamage = 0;
        lastDamageTime = 0;

        scheduler.post(() -> {
            Global.player = WorldUtils.spawn(creature, true);
            camera.position.set(player.x() + CAMERA_OFFSET_X, player.x() + CAMERA_OFFSET_Y);
            camera.update();
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
        if (input.justPressed(GLFW_KEY_B)) {
            WorkbenchLogic.toggleBuildMenu();
        }

        float speed = noClip ? 2f : (1.25f / Time.ONE_SECOND);
        if (input.pressed(GLFW_KEY_LEFT_SHIFT) || input.pressed(GLFW_KEY_RIGHT_SHIFT)) {
            speed *= 1.5f;
        }

        if (noClip) {
            speed *= Math.max(0, input.getScrollOffset());
        }

        int xf = input.axis(GLFW_KEY_A, GLFW_KEY_D);

        if (!noClip) {
            direction.set(xf, 0);
        } else {
            velocity.set(0, 0);

            int yf = input.axis(GLFW_KEY_S, GLFW_KEY_W);
            // setX(x() + speed * xf);
            // setY(y() + speed * yf);
        }

        boolean hasFloor = hasFloor();
        if (jumpedTicks > 0) {
            jumpedTicks -= Time.delta;
            if (jumpedTicks < 0)
                jumpedTicks = 0;
        } else {
            if (hasFloor && Math.abs(velocity.y) <= GAP && input.pressed(GLFW_KEY_SPACE)) {
                velocity.y += 18.35f * GRAVITY;
                jumpedTicks = 5f / Time.ONE_SECOND;
            }
        }

        if (hasFloor) {
            tmp.set(direction).scale(speed);
            acceleration.add(tmp);
        } else if (direction.x != 0) {
            float wishX = Math.signum(direction.x);
            float currentSpeedInWishDir = velocity.x * wishX;

            // Лимит воздушной скорости за один кадр.
            // 99% от обычной скорости
            float airSpeedCap = 0.99f * speed;

            float d = airSpeedCap - currentSpeedInWishDir;
            if (d > 0) {
                // Коэффициент отзывчивости управления в воздухе за кадр.
                // 0.1f означает, что лимит наберется примерно за 10 кадров.
                float airAcceleration = 0.45f * speed;
                if (airAcceleration > d) {
                    airAcceleration = d;
                }

                // избегаем накопление скорости при применении ускорения
                velocity.x += wishX * airAcceleration;
            }
        }

        Point2i blockPos = Global.input.mouseBlockPos();
        if (world.getBlockId(blockPos) <= 0) {
            return;
        }
        var blockEntity = world.getEntity(blockPos);
        if (blockEntity != null) {
            if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT)) {
                blockEntity.onMouseClick();
            }

            blockEntity.onMouseHover();
        }
    }

    public CollisionResult onCollide(HitboxComponent them) {
        return CollisionResult.WALKTHROUGH;
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

    public ObjectArrayList<ObjectArrayList<@Nullable ItemStack>> items() {
        return items;
    }

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
