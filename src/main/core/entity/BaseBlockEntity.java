package core.entity;

import core.World.StaticWorldObjects.StaticObjectsConst_V2;
import core.math.Rectangle;

public abstract class BaseBlockEntity<B extends StaticObjectsConst_V2> implements BlockEntity {

    protected float x, y;
    protected float hp;
    protected boolean isUnbreakable, dead;

    protected final B block;

    protected BaseBlockEntity(B block) {
        this.block = block;
    }

    @Override
    public final B getBlock() {
        return block;
    }

    @Override
    public float getMaxHp() {
        return block.maxHp;
    }

    @Override
    public float getHp() {
        return hp;
    }

    @Override
    public void getHitbox(Rectangle out) {
        out.setCentered(x, y, block.texture.width(), block.texture.height());
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

        this.hp -= d;
        if (hp <= 0) {
            dead = true;
            // TODO
        }
    }

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
    public void draw() {
        // TODO
    }

    @Override
    public String toString() {
        return "BaseBlockEntity{" + "x=" + x + ", y=" + y + ", block=" + block + '}';
    }
}
