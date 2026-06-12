package core.content.creatures;

import core.Constants;
import core.EventHandling.Config;
import core.Global;
import core.Time;
import core.World.Creatures.Physics;
import core.World.Creatures.Player.WorkbenchMenu.WorkbenchLogic;
import core.World.WorldUtils;
import core.content.ItemGrid;
import core.content.ItemStack;
import core.content.entity.BaseCreatureEntity;
import core.content.entity.HitboxComponent;
import core.content.entity.InventoryComponent;
import core.g2d.StackfulRender;
import core.math.MathUtil;
import core.math.Point2i;
import core.math.Vector2f;
import core.util.Debug;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import static core.Global.*;
import static core.PlayGameScene.*;
import static core.World.Creatures.Physics.GRAVITY;
import static core.World.Creatures.Player.Player.noClip;
import static core.WorldCoordinates.toWorld;
import static org.lwjgl.glfw.GLFW.*;

public class PlayerEntity
        extends BaseCreatureEntity<PlayerType>
        implements InventoryComponent {

    private static final Vector2f tmp = new Vector2f();
    private static final Vector2f direction = new Vector2f();

    // Базовая скорость прыжка. Чуть больше 2 блоков
    private static final float BASE_JUMP = 18.35f * GRAVITY;
    // Прыгаем где-то на 3 блока
    private static final float MAX_JUMP  = 1.25f * BASE_JUMP;
    private static final float ADDICTION = 0.15f * GRAVITY;
    // 99% от обычной скорости
    private static final float AIR_SPEED_LIMIT = 0.99f;
    // Коэффициент отзывчивости управления в воздухе за кадр
    private static final float AIR_SPEED_RESISTANCE = 0.45f;

    public final Point2i itemInHandIdx = new Point2i(), draggingItemIdx = new Point2i();

    private final ObjectArrayList<ObjectArrayList<@Nullable ItemStack>> items;

    public float lastDamage = 0;
    public long lastDamageTime = 0; // ms

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

        camera.position.set(x + Constants.Camera.OFFSET_X, y + Constants.Camera.OFFSET_Y);
        camera.update();
    }

    public double centerX() { return x + weight() / 2f; }
    public double centerY() { return y + height() / 2f; }

    public void draw(float dx) {
        var tex = creature.texture;
        double rx = Physics.applyAlpha(lastX, x) + dx;
        double ry = Physics.applyAlpha(lastY, y);
        var rel = camera.relativize(rx, ry);
        StackfulRender.draw(tex, rel.x, rel.y, toWorld(tex.width()), toWorld(tex.height())
                 * Math.min(1, (float)(1d/(accumulatedJump * 5))));
    }

    protected void onDamage(float d) {
        lastDamage += d;
        lastDamageTime = System.currentTimeMillis();
    }

    protected void onDead() {
        lastDamage = 0;
        lastDamageTime = 0;

        scheduler.post(() -> { Global.player = WorldUtils.spawn(creature, true); }, Time.ONE_SECOND * 5);
    }

    private float accumulatedJump;

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

        float speed = noClip ? 2f : (1.25f / Time.ONE_SECOND / Physics.SPEED_FACTOR);
        if (input.pressed(GLFW_KEY_LEFT_SHIFT) || input.pressed(GLFW_KEY_RIGHT_SHIFT)) {
            speed *= 1.5f;
        }

        if (noClip) {
            speed *= Math.max(0, input.getScrollOffset());
        }

        int xf = input.axis(GLFW_KEY_A, GLFW_KEY_D);

        if (!noClip) {
            direction.set(xf, 0);
        } else { // TODO сделать элегантнее, но сделаю когда доделаю интерфейс
            velocity.set(0, 0);

            int yf = input.axis(GLFW_KEY_S, GLFW_KEY_W);
            // setX(x() + speed * xf);
            // setY(y() + speed * yf);
        }

        boolean hasFloor = hasFloor();
        if (hasFloor) {
            if (input.releasedKey(GLFW_KEY_SPACE)) {
                velocity.y += accumulatedJump;
                accumulatedJump = 0;
            } else if (input.pressed(GLFW_KEY_SPACE)) {
                if (accumulatedJump == 0) {
                    accumulatedJump += BASE_JUMP;
                } else if (accumulatedJump < MAX_JUMP) {
                    accumulatedJump = Math.min(MAX_JUMP, accumulatedJump + ADDICTION * Time.delta);
                }
            }
        } else {
            accumulatedJump = 0;
        }

        if (hasFloor) {
            tmp.set(direction).scale(speed * Time.delta);
            acceleration.add(tmp);
        } else if (direction.x != 0) {
            float wishX = Math.signum(direction.x);
            float currentSpeedInWishDir = velocity.x * wishX;

            // Лимит воздушной скорости за один кадр.
            float airSpeedCap = AIR_SPEED_LIMIT * (speed * Physics.SPEED_FACTOR);

            float d = airSpeedCap - currentSpeedInWishDir;
            if (d > 0) {
                float airAcceleration = AIR_SPEED_RESISTANCE * (speed * Physics.SPEED_FACTOR);
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

    public static boolean smoothedCamera = Config.getBoolean("SmoothedCamera");

    public void updateCamera() {
        if (isDead()) {
            return;
        }

        if (smoothedCamera) {
            float base = 0.08f * Math.max(1, velocity.len() / 4f);
            base = Math.min(1f, base);
            float alpha = 1 - (float)Math.pow(1 - base, Time.delta);
            camera.position.lerp(x + Constants.Camera.OFFSET_X, y + Constants.Camera.OFFSET_Y, alpha);
        } else {
            camera.position.set(x + Constants.Camera.OFFSET_X, y + Constants.Camera.OFFSET_Y);
        }

        camera.update();
    }
}
