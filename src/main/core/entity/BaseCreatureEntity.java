package core.entity;

import core.World.HitboxMap;
import core.World.Textures.ShadowMap;
import core.content.CreatureType;
import core.g2d.Atlas;
import core.math.Rectangle;
import core.math.Vector2f;

import static core.Global.batch;
import static core.World.WorldGenerator.DynamicObjects;

public abstract class BaseCreatureEntity<C extends CreatureType> implements CreatureEntity {

    private static short lastId;

    private static short generateId() {
        if (lastId == Short.MAX_VALUE)
            lastId = 0;
        return lastId++;
    }

    public final short id = generateId();
    public final C creature;

    protected float x, y;
    protected float hp;
    protected boolean isUnbreakable, dead;
    protected boolean hasGravity;
    protected Vector2f vel = new Vector2f();

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
    public void getHitbox(Rectangle out) {
        out.setCentered(x, y, creature.texture.width(), creature.texture.height());
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

            DynamicObjects.remove(this);
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
        return vel;
    }

    protected static final Vector2f tmp1 = new Vector2f();
    protected static final Vector2f tmp2 = new Vector2f();

    @Override
    public void moveAt(Vector2f vel) {
        this.vel.set(vel);
        // var t = tmp1.set(vel);
        // tmp2.set(t).sub(vel);
        // this.vel.add(tmp2);
    }

    @Override
    public void jump(float impulse) {
        Atlas.Region tex = creature.texture;

        if (HitboxMap.checkIntersStaticD(x, y, tex.width(), tex.height())) {
            vel.y += impulse;
        }
    }

    @Override
    public void draw() {
        // batch.color(ShadowMap.getColorDynamic(this));
        // batch.draw(creature.texture, x, y);
        // batch.resetColor();
    }
}
