package core.content.entity;

import core.World.Creatures.Physics;
import core.content.creatures.Creature;
import core.g2d.StackfulRender;
import core.math.AABB;
import core.math.Vector2f;
import core.util.TypeUtil;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import static core.Global.camera;
import static core.WorldCoordinates.toWorld;

public abstract class BaseCreatureEntity<C extends Creature> implements CreatureEntity {
    protected static final byte FLAG_DEAD           = 1 << 0;
    protected static final byte FLAG_ALWAYS_VISIBLE = 1 << 1;

    public final C creature;

    protected short id;
    protected long flags;

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

    public final float mass() {
        return creature.mass;
    }

    public void hitboxTo(AABB out) {
        out.setRectangle(x, y, width(), height());
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

    public float width()  { return toWorld(creature.texture.width()); }
    public float height() { return toWorld(creature.texture.height()); }

    public boolean isVisible(AABB viewport) {
        return isFlag(FLAG_ALWAYS_VISIBLE) || viewport.overlaps(x, y, x+width(), y+height());
    }

    public void draw(float dx) {
        double rx = Physics.applyAlpha(lastX, x) + dx;
        double ry = Physics.applyAlpha(lastY, y);
        var rel = camera.relativize(rx, ry);
        var tex = creature.texture;
        StackfulRender.draw(tex, rel.x, rel.y, width(), height());
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
        return TypeUtil.canonicalNameOrParent(getClass()) + "#" + id;
    }

    // region to override
    protected void onDamage(float d) {
    }

    protected void onDead() {
    }
    // endregion
}
