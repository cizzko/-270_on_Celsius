package core.World.Creatures;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Logger;
import core.Time;
import core.World.HitboxMap;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.WorldGenerator;
import core.g2d.Atlas;
import core.math.Rectangle;
import core.math.Vector2f;

import java.io.Serializable;
import java.util.HashMap;

import static core.Global.*;
import static core.World.Creatures.Player.Player.noClip;
import static core.World.StaticWorldObjects.StaticWorldObjects.*;
import static core.World.Textures.TextureDrawing.blockSize;
import static org.lwjgl.glfw.GLFW.*;

// dynamic objects, can have any coordinates within the world and be moved at any time
public class DynamicWorldObjects implements Serializable {
    private static final HashMap<String, Byte> ids = new HashMap<>();
    private static byte lastId = -128;
    private final byte id;
    private short currentFrame;
    private long lastFrameTime = System.currentTimeMillis();
    private float x, y, currentHp;
    private float jumpedTicks; // откат прыжка

    public Vector2f velocity = new Vector2f();

    private DynamicWorldObjects(byte id, float x, float y, float maxHp) {
        this.id = id;
        this.currentFrame = 0;
        this.x = x;
        this.y = y;
        this.currentHp = maxHp;
    }

    public static DynamicWorldObjects createDynamic(String name, float x) {
        byte id = generateId(name);
        DynamicObjectsConst obj = DynamicObjectsConst.bindDynamic(name, id);
        int topmostBlock = WorldGenerator.findTopmostSolidBlock((int) (x / blockSize), 5) + 1;

        if (HitboxMap.checkIntersInside(x, topmostBlock * blockSize, obj.texture.width(), obj.texture.height()) != null) {
            Logger.log("Unable spawning player at: x - " + x + ", y - " + topmostBlock * blockSize);
            return createDynamic(name, x + blockSize);
        }

        return new DynamicWorldObjects(generateId(name), x, topmostBlock * blockSize, DynamicObjectsConst.getConst(id).maxHp);
    }

    public static DynamicWorldObjects createDynamic(String name, float x, float y) {
        byte id = generateId(name);
        DynamicObjectsConst.bindDynamic(name, id);
        return new DynamicWorldObjects(generateId(name), x, y, DynamicObjectsConst.getConst(id).maxHp);
    }

    private static byte generateId(String name) {
        if (name == null) {
            return 0;
        }
        byte id = ids.getOrDefault(name, (byte) 0);

        if (id != 0) {
            return id;
        } else {
            lastId++;

            if (lastId == 126) {
                Logger.log("Number of id's dynamic objects exceeded, errors will occur");
            }
            ids.put(name, lastId);
            return lastId;
        }
    }

    private static final Vector2f tmp = new Vector2f();

    protected void updateInput() {
        // todo тут надо проверять элемент UI на фокусировку, т.е. на порядок отображения (фокусирован = самый последний элемент)
        if (EventHandler.isKeylogging()) {
            return;
        }

        float speed = noClip ? 2f : 1f;
        if (input.pressed(GLFW_KEY_LEFT_SHIFT) || input.pressed(GLFW_KEY_RIGHT_SHIFT)) {
            speed *= 1.5f;
        }

        if (noClip) {
            speed *= Math.max(0, input.getScrollOffset());
        }

        int xf = input.axis(GLFW_KEY_A, GLFW_KEY_D);

        if (!noClip) {
            tmp.set(xf, 0).scale(speed);
        } else {
            velocity.set(0, 0);

            setX(getX() + speed * xf);
            int yf = input.axis(GLFW_KEY_S, GLFW_KEY_W);
            setY(getY() + speed * yf);
        }

        if (jumpedTicks > 0) {
            jumpedTicks = Math.max(jumpedTicks - Time.delta, 0);
        } else {
            boolean hasFixture = hasFixture();
            if (hasFixture && Math.abs(velocity.y) <= GAP && input.pressed(GLFW_KEY_SPACE)) {
                tmp.y += 7;
                jumpedTicks = 5;
            }
        }

        velocity.add(tmp);
    }

    public boolean hasFixture() {
        int minX = (int) Math.floor(x / blockSize);
        int maxX = (int) Math.floor((x + getTexture().width()) / blockSize);
        int minY = (int) Math.floor((y - GAP) / blockSize);
        for (int x = minX; x <= maxX; x++) {
            short block = world.get(x, minY);
            if (block == -1) {
                return true;
            }
            if (getResistance(block) == 100 && getType(block) == StaticObjectsConst.Types.SOLID) {
                return true;
            }
        }
        return false;
    }

    public float getX() {
        return x;
    }

    public void incrementX(float increment) {
        this.x += increment;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void incrementY(float increment) {
        this.y += increment;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getMaxHp() {
        return DynamicObjectsConst.getConst(id).maxHp;
    }

    public void setCurrentHp(float hp) {
        this.currentHp = hp;
    }

    public float getCurrentHP() {
        return currentHp;
    }

    public void incrementCurrentHP(float increment) {
        this.currentHp += increment;
    }

    public void incrementCurrentFrame() {
        DynamicObjectsConst dynamicConst = DynamicObjectsConst.getConst(id);

        if (dynamicConst.animSpeed != 0 && dynamicConst.framesCount != 0 && System.currentTimeMillis() - lastFrameTime >= dynamicConst.animSpeed) {
            if (currentFrame >= dynamicConst.framesCount) {
                currentFrame = 0;
                lastFrameTime = System.currentTimeMillis();
                return;
            }
            lastFrameTime = System.currentTimeMillis();
            currentFrame++;
        }
    }

    public void setCurrentFrame(short currentFrame) {
        this.currentFrame = currentFrame;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public int getFramesCount() {
        return DynamicObjectsConst.getConst(id).framesCount;
    }

    public int getAnimationSpeed() {
        return DynamicObjectsConst.getConst(id).animSpeed;
    }

    public void setAnimationSpeed(int speed) {
        DynamicObjectsConst.getConst(id).animSpeed = speed;
    }

    public float getWeight() {
        return DynamicObjectsConst.getConst(id).weight;
    }

    public Atlas.Region getTexture() {
        return DynamicObjectsConst.getConst(id).texture;
    }

    public boolean getIsFlying() {
        return DynamicObjectsConst.getConst(id).isFlying;
    }

    public float getMotionVectorX() {
        return velocity.x;
    }

    public void incrementMotionVectorX(float vectorX) {
        this.velocity.x += vectorX;
    }

    public void setMotionVectorX(float vectorX) {
        this.velocity.x = vectorX;
    }

    public float getMotionVectorY() {
        return velocity.y;
    }

    public void incrementMotionVectorY(float vectorY) {
        this.velocity.y += vectorY;
    }

    public void setMotionVectorY(float vectorY) {
        this.velocity.y = vectorY;
    }

    // Лучшее решение, которое вообще можно принять.
    // Из-за проблем с неточными числами можно просто 2-3 пикселя отступать и этого даже не будет заметно
    public static final float GAP = 1f / blockSize;

    public void getHitboxTo(Rectangle entityHitbox) {
        var tex = getTexture();
        entityHitbox.set(x, y, tex.width(), tex.height());
        entityHitbox.width += GAP;
        entityHitbox.height += GAP;
    }
}
