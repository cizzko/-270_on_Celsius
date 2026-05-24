package core.content.entity;

import core.content.blocks.Block;
import core.content.creatures.Creature;
import core.g2d.Atlas;
import core.g2d.StackfulRender;
import core.math.Rectangle;
import core.math.TmpShapes;
import core.math.Vector2f;
import it.unimi.dsi.fastutil.HashCommon;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import static core.Global.world;
import static core.WorldCoordinates.*;

public abstract class BaseCreatureEntity<C extends Creature> implements CreatureEntity {
    public short id;
    public final C creature;

    protected float x, y;
    protected float prevX, prevY;
    protected float hp;
    public boolean hasGravity, isUnbreakable, dead;
    protected Vector2f velocity = new Vector2f();
    protected Vector2f acceleration = new Vector2f();

    protected BaseCreatureEntity(C creature) {
        this.creature = creature;
    }

    public final short id() {
        return id;
    }

    public final void setId(int id) {
        this.id = (short) id;
    }

    public final float prevX() { return prevX; }

    public final float prevY() { return prevY; }

    public abstract float centerX();
    public abstract float centerY();

    protected void updateLastPosition() {
        prevX = x;
        prevY = y;
    }

    @MustBeInvokedByOverriders
    public void init() {
        this.hp = getMaxHp();
        this.hasGravity = creature.hasGravity;
    }

    public final C getCreature() {
        return creature;
    }

    public final float getWeight() {
        return creature.weight;
    }

    public void getHitboxTo(Rectangle entityHitbox) {
        var tex = creature.texture;
        entityHitbox.set(x, y, toWorld(tex.width()), toWorld(tex.height()));
    }

    public float getMaxHp() {
        return creature.maxHp;
    }

    public float getHp() {
        return hp;
    }

    public boolean isUnbreakable() {
        return isUnbreakable;
    }

    public boolean isDead() {
        return dead;
    }

    public void setHp(float hp) {
        this.hp = hp;
    }

    public void damage(float d, DamageSource source) {
        if (dead) {
            return;
        }

        onDamage(d);

        this.hp -= d;
        if (hp <= 0) {
            remove();
        }
    }

    protected void onDamage(float d) {
    }

    protected void onDead() {

    }

    public void remove() {
        CreatureEntity.super.remove();
        dead = true;

        onDead();

        hp = 0;
        velocity.set(0, 0);
        acceleration.set(0, 0);
    }

    public void setUnbreakable(boolean unbreakable) {
        this.isUnbreakable = unbreakable;
    }

    public final float x() { return x; }

    public final float y() { return y; }

    public final void setPosition(float x, float y) { this.x = x; this.y = y; }

    public final void setX(float x) { this.x = x; }

    public final void setY(float y) { this.y = y; }

    public final Vector2f velocity() { return velocity; }
    public final Vector2f acceleration() { return acceleration; }

    public void draw(float drawX) {
        Atlas.Region tex = creature.texture;
        StackfulRender.draw(tex, drawX, y, toWorld(tex.width()), toWorld(tex.height()));
    }

    public boolean hasFloor() {

        Rectangle hitbox = TmpShapes.r1;
        getHitboxTo(hitbox);

        int minX = toBlock(hitbox.x + GAP);
        int maxX = toBlock(hitbox.x + hitbox.width - GAP);
        int minY = toBlock(hitbox.y - GAP);

        for (int x = minX; x <= maxX; x++) {
            var block = world.getBlock(x, minY);
            if (block == null || block.type == Block.Type.SOLID) {
                return true;
            }
        }
        return false;
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
        return HashCommon.murmurHash3(id);
    }
}
