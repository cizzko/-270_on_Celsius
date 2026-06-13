package core.content.entity;

import core.World.Creatures.Physics;
import core.content.blocks.Block;
import core.content.creatures.Creature;
import core.g2d.StackfulRender;
import core.math.*;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import static core.Global.camera;
import static core.Global.world;
import static core.WorldCoordinates.*;

public abstract class BaseCreatureEntity<C extends Creature> implements CreatureEntity {
    private static final byte FLAG_DEAD = 1 << 0;

    public short id;
    public final C creature;
    public long flags;

    protected double x, y;
    protected double lastX, lastY;

    protected float hp;

    protected final Vector2f velocity = new Vector2f();
    protected final Vector2f acceleration = new Vector2f();

    protected BaseCreatureEntity(C creature) {
        this.creature = creature;
    }

    protected void setFlag(int flag, boolean st) {
        if (st) {
            this.flags |= flag;
        } else {
            this.flags &= ~flag;
        }
    }
    protected void flipFlag(int flag) {
        flags ^= flag;
    }
    protected boolean isFlag(int flag) { return (flags & flag) != 0; }

    public final short id() {
        return id;
    }

    public final void setId(short id) {
        this.id = id;
    }

    public abstract double centerX();
    public abstract double centerY();

    public void updateLastPosition() {
        lastX = x;
        lastY = y;
    }

    @MustBeInvokedByOverriders
    public void init() {
        this.hp = maxHp();
    }

    public final C creature() {
        return creature;
    }

    public final float weight() {
        return creature.weight;
    }

    public void hitboxTo(AABB out) {
        var tex = creature.texture;
        out.setRectangle(x, y, toWorld(tex.width()), toWorld(tex.height()));
    }

    public float maxHp() {
        return creature.maxHp;
    }

    public final float hp() {
        return hp;
    }

    public final boolean isDead() {
        return isFlag(FLAG_DEAD);
    }

    public final void setHp(float hp) {
        this.hp = hp;
    }

    public void damage(float d, DamageSource source) {
        if (isDead()) {
            return;
        }

        onDamage(d);

        this.hp -= d;
        if (hp <= 0) {
            remove();
        }
    }

    public void remove() {
        CreatureEntity.super.remove();
        setFlag(FLAG_DEAD, true);

        onDead();

        hp = 0;
        velocity.set(0, 0);
        acceleration.set(0, 0);
    }

    public final short blockX()   { return toBlock(x); }
    public final short blockY()   { return toBlock(y); }

    public final float offsetX() { return (float) (x - blockX()); }
    public final float offsetY() { return (float) (y - blockY()); }

    public final double x() { return x; }
    public final double y() { return y; }

    public final double lastX() { return lastX; }
    public final double lastY() { return lastY; }

    public final void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public final void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public final void mirrorX(double dx) { this.x += dx; this.lastX += dx; }
    public final void setY(double y) { this.y = y; }

    public final Vector2f velocity() { return velocity; }
    public final Vector2f acceleration() { return acceleration; }

    public float width()  { return creature.texture.width(); }
    public float height() { return creature.texture.height(); }

    public void draw(float dx) {
        var tex = creature.texture;
        double rx = Physics.applyAlpha(lastX, x) + dx;
        double ry = Physics.applyAlpha(lastY, y);
        var rel = camera.relativize(rx, ry);
        StackfulRender.draw(tex, rel.x, rel.y, toWorld(tex.width()), toWorld(tex.height()));
    }

    public boolean hasFloor() {
        var hitbox = TmpShapes.aabb1;
        hitboxTo(hitbox);
        hitbox.maxY = hitbox.minY;
        hitbox.minY -= GAP;
        hitbox.maxX -= GAP;
        hitbox.minX += GAP;

        short minX = hitbox.blockMinX();
        short maxX = hitbox.blockMaxX();
        short minY = hitbox.blockMinY();
        short maxY = hitbox.blockMaxY();

        for (; minY <= maxY; minY++) {
            for (short x = minX; x <= maxX; x++) {
                var block = world.getBlock(x, minY);
                if (block == null || block.type == Block.Type.SOLID) {
                    return true;
                }
            }
        }
        return false;
    }

    public final double dstSq(double x, double y) {
        double dx = x - this.x;
        double dy = y - this.y;
        return dx * dx + dy * dy;
    }

    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseCreatureEntity<?> that)) {
            return false;
        }
        return id == that.id;
    }

    public final int hashCode() {
        return Short.toUnsignedInt(id);
    }

    public String toString() {
        return getClass().getSimpleName() + "#" + id;
    }

    // region to override
    protected void onDamage(float d) {
    }

    protected void onDead() {
    }
    // endregion
}
