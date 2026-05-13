package core.entity;

import core.Global;
import core.World.HitboxMap;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.content.creatures.CreatureType;
import core.g2d.Atlas;
import core.math.Rectangle;
import core.math.Vector2f;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import static core.Global.world;
import static core.World.Textures.TextureDrawing.blockSize;

public abstract class BaseCreatureEntity<C extends CreatureType> implements CreatureEntity {

    public short id;
    public final C creature;

    protected float x, y;
    protected float hp;
    protected boolean isUnbreakable, dead;
    protected boolean hasGravity;
    protected Vector2f velocity = new Vector2f();

    // Лучшее решение, которое вообще можно принять.
    // Из-за проблем с неточными числами можно просто 2-3 пикселя отступать и этого даже не будет заметно
    public static final float GAP = 1f / blockSize;

    protected BaseCreatureEntity(C creature) {
        this.creature = creature;
    }

    @Override
    public void init() {
        this.hp = getMaxHp();
        this.hasGravity = creature.hasGravity;
    }

    @Override
    public final C getCreature() {
        return creature;
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
    public void getHitboxTo(Rectangle entityHitbox) {
        var tex = creature.texture;
        entityHitbox.set(x, y, tex.width(), tex.height());
        entityHitbox.width += GAP;
        entityHitbox.height += GAP;
    }

    @Override
    public boolean hasGravity() {
        return hasGravity;
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
    public void damage(float d) {
        if (dead) {
            return;
        }

        onDamage(d);

        this.hp -= d;
        if (hp <= 0) {
            dead = true;
            // TODO
        }
    }

    protected void onDamage(float d) {}

    @Override
    public void setUnbreakable(boolean unbreakable) {
        this.isUnbreakable = unbreakable;
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void setX(float x) {
        this.x = x;
    }

    @Override
    public void setY(float y) {
        this.y = y;
    }

    @Override
    public Vector2f getVelocity() {
        return velocity;
    }

    @Override
    public void moveAt(Vector2f vel) {
        this.velocity.set(vel); // TODO
    }

    @Override
    public void jump(float impulse) {
        Atlas.Region tex = creature.texture;

        if (HitboxMap.checkIntersStaticD(x, y, tex.width(), tex.height())) {
            velocity.y += impulse;
        }
    }

    @Override
    @MustBeInvokedByOverriders
    public void remove() {
        Global.entityPool.releaseId(this);
    }

    @Override
    public void draw() {
        // TODO
    }

    public boolean hasFloor() {
        int minX = (int) Math.floor(x / blockSize);
        int maxX = (int) Math.floor((x + creature.texture.width()) / blockSize);
        int minY = (int) Math.floor((y - GAP) / blockSize);
        for (int x = minX; x <= maxX; x++) {
            var block = world.getBlock(x, minY);
            if (block == null) {
                return true;
            }
            if (block.type == StaticObjectsConst.Type.SOLID) {
                return true;
            }
        }
        return false;
    }
}
