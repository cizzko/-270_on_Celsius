package core.content.creatures;

import core.Time;
import core.World.Creatures.Physics;
import core.World.Creatures.Player.Inventory.Inventory;
import core.content.blocks.Block;
import core.content.ItemStack;
import core.content.entity.HitboxComponent;
import core.content.entity.LivingEntity;
import core.g2d.Atlas;
import core.g2d.Fill;
import core.g2d.StackfulRender;
import core.graphic.WorldDrawing;
import core.math.Rectangle;
import core.math.TmpShapes;
import core.math.Vector2f;
import core.graphic.Color;

import static core.Global.*;
import static core.WorldCoordinates.*;

public class ItemEntity implements LivingEntity {
    public static final int ITEM_DROPPED_SIZE   = 32; // пиксели
    public static final int MOVE_DST            = 3;  // блоки

    protected short id;
    protected float x, y;
    protected float prevX, prevY;
    protected ItemStack itemStack;
    protected float hp, phase;
    protected boolean isUnbreakable, dead;
    protected Vector2f velocity = new Vector2f();
    protected Vector2f acceleration = new Vector2f();

    public ItemEntity(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public final short id() {
        return id;
    }

    public final void setId(int id) {
        this.id = (short) id;
    }

    public final float x() { return x; }

    public final float y() { return y; }

    public final void setX(float x) { this.x = x; }
    public final void setY(float y) { this.y = y; }

    public final float getHp() { return hp; }

    public final void setPosition(float x, float y) { this.x = x; this.y = y; }

    public final float prevX() { return prevX; }

    public final float prevY() { return prevY; }

    public float centerX() {
        return x + toWorld(ITEM_DROPPED_SIZE)/2f;
    }

    public float centerY() {
        return y + toWorld(ITEM_DROPPED_SIZE)/2f;
    }

    public void getHitboxTo(Rectangle out) {
        out.set(x, y, toWorld(ITEM_DROPPED_SIZE), toWorld(ITEM_DROPPED_SIZE));
    }

    public CollisionResult onCollide(HitboxComponent them) {
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

    public void update() {
        var tmp = TmpShapes.r1;
        player.getHitboxTo(tmp);
        float pcx = tmp.x+tmp.width/2f;
        float pcy = tmp.y;

        float dstToPlayer = (float) Math.sqrt(dst2(pcx, pcy));
        if (dstToPlayer <= MOVE_DST && dstToPlayer > 0.01f &&
                    raycastTo(player.blockX(), player.blockY(), (x, y) ->
                            world.getBlockId(x, y) != 0)) {
            float acceleration = Math.min(3,
                    1.45f * (getWeight() * Physics.WEIGHT_FACTOR) * (1f - (dstToPlayer / MOVE_DST)));
            var dir = TmpShapes.v1.set(pcx, pcy)
                    .sub(x, y)
                    .nor()
                    .scale(acceleration * Time.delta);
            velocity.add(dir);
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

    public void draw(float drawX) {
        float amplitude = 1 / 4f;
        float frequency = 0.45f;
        float periodTicks = Time.ONE_SECOND / frequency;
        phase = (phase + Time.delta) % periodTicks;

        if (phase < 0f) {
            phase += periodTicks;
        }
        float tSeconds = phase / Time.ONE_SECOND;
        float yOffset = amplitude * 0.5f * (1f - (float)Math.cos(2f * Math.PI * frequency * tSeconds));
        Atlas.Region tex = itemStack.item().texture;
        StackfulRender.draw(tex, drawX, y + yOffset, toWorld(ITEM_DROPPED_SIZE), toWorld(ITEM_DROPPED_SIZE));
        Fill.rectangleBorder(drawX, y + yOffset, toWorld(ITEM_DROPPED_SIZE), toWorld(ITEM_DROPPED_SIZE), toWorld(1), Color.white);
        if (itemStack.count() > 1) {
            // TODO выглядит всрато из-за не той матрицы
            WorldDrawing.drawGameText(drawX, y + yOffset, String.valueOf(itemStack.count()), Color.white);
        }
    }

    public boolean hasFloor() {
        int minX = toBlock(x - GAP);
        int maxX = toBlock(x + toWorld(ITEM_DROPPED_SIZE) - GAP);
        int minY = toBlock(y - GAP);

        for (int x = minX; x <= maxX; x++) {
            int blockId = world.getBlockId(x, minY);
            if (blockId < 0) {
                return true;
            }
            if (blockId == 0) {
                continue;
            }
            var block = content.blocksRegistry.typeById(blockId);
            if (block.type == Block.Type.SOLID) {
                return true;
            }
        }
        return false;
    }

    public float getMaxHp() {
        return 100;
    }

    public boolean isUnbreakable() {
        return isUnbreakable;
    }

    public boolean isDead() {
        return dead;
    }

    public void setHp(float hp) {
        this.hp = hp;
        if (hp <= 0) {
            remove();
        }
    }

    public void damage(float d, DamageSource source) {
        if (source == DamageSource.FALL || dead) {
            return;
        }

        this.hp -= d;
        if (hp <= 0) {
            remove();
        }
    }

    public void setUnbreakable(boolean unbreakable) {

    }

    public Vector2f velocity() {
        return velocity;
    }

    @Override
    public Vector2f acceleration() {
        return acceleration;
    }

    public float getWeight() {
        return itemStack.item().weight;
    }

    public void remove() {
        LivingEntity.super.remove();

        dead = true;
        hp = 0;
        velocity.set(0, 0);
        acceleration.set(0, 0);
    }

    public String toString() {
        return "Item[" + itemStack + "]#" + id;
    }
}
