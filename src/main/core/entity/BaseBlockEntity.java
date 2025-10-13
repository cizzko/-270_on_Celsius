package core.entity;

import core.Utils.SimpleColor;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.StaticWAnimations;
import core.World.StaticWorldObjects.TemperatureMap;
import core.World.Textures.ShadowMap;
import core.math.Point2i;
import core.math.Rectangle;

import static core.Global.atlas;
import static core.Global.batch;
import static core.World.Textures.TextureDrawing.drawTexture;

public abstract class BaseBlockEntity<B extends StaticObjectsConst> implements BlockEntity {

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
            destroyObject(getBlockX(), getBlockY());
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
        SimpleColor color = ShadowMap.getColor(getBlockX(), getBlockY());
        int upperLimit = 100;
        int lowestLimit = -20;
        int maxColor = 65;
        float temp = TemperatureMap.getTemp(getBlockX(), getBlockY());

        int a;
        if (temp > upperLimit) {
            a = (int) Math.min(maxColor, Math.abs((temp - upperLimit) / 3));
            color = SimpleColor.fromRGBA(color.getRed(), color.getGreen() - (a / 2), color.getBlue() - a, color.getAlpha());
        } else if (temp < lowestLimit) {
            a = (int) Math.min(maxColor, Math.abs((temp + lowestLimit) / 3));
            color = SimpleColor.fromRGBA(color.getRed() - a, color.getGreen() - (a / 2), color.getBlue(), color.getAlpha());
        }

        StaticWAnimations.AnimData currentFrame = StaticWAnimations.getCurrentFrame(this, new Point2i(getBlockX(), getBlockY()));
        if (currentFrame != null) {
            drawTexture(x, y, currentFrame.width(), currentFrame.height(), 1, false, currentFrame.currentFrame(), currentFrame.currentFrameImage(), color);
            return;
        }

        batch.color(color);
        batch.draw(block.texture, x, y);

        float maxHp = getMaxHp();
        if (hp > maxHp / 1.5f) {
            // ???
        } else if (hp < maxHp / 3) {
            batch.draw(atlas.byPath("World/Blocks/damaged1.png"), x, y);
        } else {
            batch.draw(atlas.byPath("World/Blocks/damaged0.png"), x, y);
        }
        batch.resetColor();
    }

    @Override
    public String toString() {
        return "BaseBlockEntity{" + "x=" + x + ", y=" + y + ", block=" + block + '}';
    }
}
