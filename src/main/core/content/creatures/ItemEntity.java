package core.content.creatures;

import core.Time;
import core.World.Creatures.Physics;
import core.World.Creatures.Player.Inventory.Inventory;
import core.content.ItemStack;
import core.content.entity.comp.PhysicalBody;
import core.content.entity.LivingEntity;
import core.g2d.Fill;
import core.g2d.StackfulRender;
import core.graphic.Color;
import core.graphic.WorldDrawing;
import core.math.AABB;
import core.math.TmpShapes;
import core.math.Vector2f;

import static core.Global.*;
import static core.WorldCoordinates.*;

public final class ItemEntity implements LivingEntity {
    public static final int ITEM_DROPPED_SIZE   = 32; // пиксели
    public static final int MOVE_DST            = 3;  // блоки

    private static final byte FLAG_DEAD = 1 << 0;

    public short id;
    public long flags;

    private double x, y;
    private double lastX, lastY;

    private final ItemStack itemStack;
    private float hp, phase;
    private final Vector2f velocity = new Vector2f();
    private final Vector2f acceleration = new Vector2f();

    public ItemEntity(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    private void setFlag(int flag, boolean st) {
        if (st) {
            this.flags |= flag;
        } else {
            this.flags &= ~flag;
        }
    }
    private void flipFlag(int flag) {
        flags ^= flag;
    }
    private boolean isFlag(int flag) { return (flags & flag) != 0; }

    public short id() {
        return id;
    }

    public void setId(short id) {
        this.id = id;
    }

    public float hp() { return hp; }

    public void hitboxTo(AABB out) {
        out.setRectangle(x, y, width(), height());
    }

    public CollisionResult onCollide(PhysicalBody them) {
        if (them instanceof PlayerEntity) {
            Inventory.addItemStack(itemStack);
            remove();
        } else if (them instanceof ItemEntity other &&
                   other.itemStack.isSame(itemStack)) {
            if (itemStack.count() > other.itemStack.count()) {
                itemStack.merge(other.itemStack);
                other.remove();
            } else {
                other.itemStack.merge(itemStack);
                remove();
            }
        }
        return CollisionResult.WALKTHROUGH;
    }

    public void updateLastPosition() {
        lastX = x;
        lastY = y;
    }

    public double lastX() { return lastX; }
    public double lastY() { return lastY; }

    public float width()  { return toWorld(ITEM_DROPPED_SIZE); }
    public float height() { return toWorld(ITEM_DROPPED_SIZE); }

    public void update() {
        var hitbox = TmpShapes.aabb1;
        player.hitboxTo(hitbox);
        double pcx = hitbox.centerX();
        double pcy = hitbox.centerY();

        float dstToPlayer = (float) Math.sqrt(dstSq(pcx, pcy));
        if (dstToPlayer <= MOVE_DST && dstToPlayer > INV_BLOCK_SIZE &&
                    raycastTo(player.blockX(), player.blockY(), (x, y) ->
                            world.getBlockId(x, y) != 0)) {
            float acceleration = Math.min(3, 1.45f * (mass() * Physics.WEIGHT_FACTOR) * (1f - (dstToPlayer / MOVE_DST)));
            var dir = TmpShapes.v1d.set(pcx, pcy)
                    .sub(x, y)
                    .nor()
                    .scale(acceleration * Time.delta);
            velocity.add(dir.xf(), dir.yf());
        }
    }

    interface RaycastChecker {
        boolean check(int x, int y);
    }

    private boolean raycastTo(int x, int y, RaycastChecker checker) {
        int x1 = blockX();
        int y1 = blockY();

        int x2 = x;
        int y2 = y;

        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);

        int sx = (x1 < x2) ? 1 : -1;
        int sy = (y1 < y2) ? 1 : -1;

        int err = dx - dy;

        do {
            int e2 = 2 * err;

            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }

            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }

            if (x1 == x2 && y1 == y2) {
                return true;
            }
            if (checker.check(x1, y1)) {
                return false;
            }
        } while (true);
    }

    @Override
    public boolean isVisible(AABB viewport) {
        return viewport.overlaps(x, y, x+width(), y+height());
    }

    public void draw(float dx) {
        final float amplitude = 1 / 4f;
        final float frequency = 0.45f;
        final float periodTicks = Time.ONE_SECOND / frequency;

        phase = (phase + Time.delta) % periodTicks;

        if (phase < 0f) {
            phase += periodTicks;
        }
        float tSeconds = phase / Time.ONE_SECOND;
        float yOffset = amplitude * 0.5f * (1f - (float)Math.cos(2f * Math.PI * frequency * tSeconds));
        var tex = itemStack.item().texture;
        double rx = Physics.applyAlpha(lastX, x) + dx;
        double ry = Physics.applyAlpha(lastY, y) + yOffset;
        var pos = camera.relativize(rx, ry);

        float w = width();
        StackfulRender.draw(tex, pos.x, pos.y, w, w);
        Fill.rectangleBorder(pos.x, pos.y, w, w, toWorld(1), Color.white);
        if (itemStack.count() > 1) {
            WorldDrawing.drawGameText(pos.x, pos.y, String.valueOf(itemStack.count()), Color.white);
        }
    }

    public float maxHp() {
        return 100;
    }

    public boolean isDead() {
        return isFlag(FLAG_DEAD);
    }

    public void setHp(float hp) {
        this.hp = hp;
        if (hp <= 0) {
            remove();
        }
    }

    public void damage(float d, DamageSource source) {
        if (source == DamageSource.FALL || isDead()) {
            return;
        }

        this.hp -= d;
        if (hp <= 0) {
            remove();
        }
    }

    public double x() { return x; }
    public double y() { return y; }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public void mirrorX(double dx) { this.x += dx; this.lastX += dx; }
    public void setY(double y) { this.y = y; }

    public double dstSq(double x, double y) {
        double dx = x - this.x;
        double dy = y - this.y;
        return dx * dx + dy * dy;
    }

    public Vector2f velocity() {
        return velocity;
    }
    public Vector2f acceleration() {
        return acceleration;
    }

    public float mass() {
        return itemStack.item().mass;
    }

    public void remove() {
        LivingEntity.super.remove();
        setFlag(FLAG_DEAD, true);
        hp = 0;
        velocity.set(0, 0);
        acceleration.set(0, 0);
    }

    public String toString() {
        return "Item[" + itemStack + "]#" + id;
    }
}
