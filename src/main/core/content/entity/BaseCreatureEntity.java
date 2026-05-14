package core.content.entity;

import core.World.StaticWorldObjects.StaticObjectsConst;
import core.content.creatures.CreatureType;
import core.math.Rectangle;
import core.math.Vector2f;
import it.unimi.dsi.fastutil.HashCommon;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import static core.Global.*;
import static core.World.Textures.TextureDrawing.blockSize;

public abstract class BaseCreatureEntity<C extends CreatureType> implements CreatureEntity {
    public short id;
    public final C creature;

    protected float x, y;
    protected float hp;
    public boolean hasGravity, isUnbreakable, dead;
    protected Vector2f velocity = new Vector2f();

    protected BaseCreatureEntity(C creature) {
        this.creature = creature;
    }

    @Override
    public final short getId() {
        return id;
    }

    @Override
    public final void setId(int id) {
        this.id = (short) id;
    }

    @Override
    @MustBeInvokedByOverriders
    public void init() {
        this.hp = getMaxHp();
        this.hasGravity = creature.hasGravity;
    }

    @Override
    public final C getCreature() {
        return creature;
    }

    @Override
    public final float getWeight() {
        return creature.weight;
    }

    @Override
    public void getHitboxTo(Rectangle entityHitbox) {
        var tex = creature.texture;
        entityHitbox.set(x, y, tex.width(), tex.height());
    }

    @Override
    public float getMaxHp() {
        return creature.maxHp;
    }

    @Override
    public float getHp() {
        return hp;
    }

    @Override
    public boolean isUnbreakable() {
        return isUnbreakable;
    }

    @Override
    public boolean isDead() {
        return dead;
    }

    @Override
    public void setHp(float hp) {
        this.hp = hp;
    }

    @Override
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

    @Override
    public void remove() {
        CreatureEntity.super.remove();

        dead = true;
        hp = 0;
        velocity.set(0, 0);
    }

    @Override
    public void setUnbreakable(boolean unbreakable) {
        this.isUnbreakable = unbreakable;
    }

    @Override
    public final float getX() { return x; }

    @Override
    public final float getY() { return y; }

    @Override
    public final void setPosition(float x, float y) { this.x = x; this.y = y; }

    @Override
    public final void setX(float x) { this.x = x; }

    @Override
    public final void setY(float y) { this.y = y; }

    @Override
    public Vector2f getVelocity() {
        return velocity;
    }

    @Override
    public void moveAt(Vector2f vel) {
    }

    @Override
    public void jump(float impulse) {
    }

    @Override
    public void draw(float drawX) {
        // var shadow = ShadowMap.getColorDynamic(entity);
        batch.draw(creature.texture/*, shadow*/, drawX, y);
    }

    @Override
    public boolean hasFloor() {
        int minX = (int) Math.floor((x ) / blockSize);
        int maxX = (int) Math.floor((x + creature.texture.width() - GAP) / blockSize);
        int minY = (int) Math.floor((y - GAP) / blockSize);

        for (int x = minX; x <= maxX; x++) {
            var block = world.getBlock(x, minY);
            if (block == null || block.type == StaticObjectsConst.Type.SOLID) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseCreatureEntity that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return HashCommon.murmurHash3(id);
    }
}
