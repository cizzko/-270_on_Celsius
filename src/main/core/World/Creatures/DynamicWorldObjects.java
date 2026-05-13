package core.World.Creatures;

import core.EventHandling.EventHandler;
import core.Global;
import core.Time;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.g2d.Atlas;
import core.math.Point2i;
import core.math.Rectangle;
import core.math.Vector2f;

import java.io.Serializable;

import static core.Global.*;
import static core.World.Creatures.Player.Player.noClip;
import static core.World.Textures.TextureDrawing.blockSize;
import static org.lwjgl.glfw.GLFW.*;

// dynamic objects, can have any coordinates within the world and be moved at any time
public class DynamicWorldObjects implements Serializable {
    private final byte id;
    private float x, y, currentHp;
    private float jumpedTicks; // откат прыжка

    public Vector2f velocity = new Vector2f();

    private DynamicWorldObjects(byte id, float x, float y, float maxHp) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.currentHp = maxHp;
    }

    public boolean within(float ox, float oy, float radius) {
        float dx = ox - this.x;
        float dy = oy - this.y;
        return (dx * dx + dy * dy) <= radius*radius;
    }

    private static final Vector2f tmp = new Vector2f();

    protected void updateInput() {
        // todo тут надо проверять элемент UI на фокусировку, т.е. на порядок отображения (фокусирован = самый последний элемент)
        if (EventHandler.isKeylogging()) {
            return;
        }

        if (input.justPressed(GLFW_KEY_F1)) app.setFramerate(60);
        if (input.justPressed(GLFW_KEY_F2)) app.setFramerate(1000);

//        if (input.justPressed(GLFW_KEY_F3)) {
//
//            final ObjectMapper json = new ObjectMapper();
//
//            var m = new SimpleModule();
//            m.addSerializer(new ItemStack.ItemStackSerializer());
//            m.addSerializer(new World.WorldSerializer());
//            m.addSerializer(new ItemStack.ItemStackGridSerializer());
//            json.registerModule(m);
//
//            long t = System.currentTimeMillis();
//            try {
//                Files.writeString(assets.workingDir().resolve("open_worl.json"), Config.json.writeValueAsString(world));
//            } catch (Exception e) {
//                Application.log.error(e);
//            }
//            Application.log.info("Serialization take: {}ms", (System.currentTimeMillis() - t));
//        }

//        if (input.justClicked(GLFW_MOUSE_BUTTON_RIGHT)) {
//            Point2i blockUnderMouse = Global.input.mouseBlockPos();
//
//            if (world.getBlockId(blockUnderMouse.x, blockUnderMouse.y) > 0) {;
//                var blockEntity = world.getEntity(blockUnderMouse.x, blockUnderMouse.y);
//                if (blockEntity != null) {
//                    long t = System.currentTimeMillis();
//
//                    final ObjectMapper json = new ObjectMapper();
//                    {
//                        var m = new SimpleModule();
//                        m.addSerializer(new ItemStack.ItemStackSerializer());
//                        m.addSerializer(new World.WorldSerializer());
//                        m.addSerializer(new ItemStack.ItemStackGridSerializer());
//                        json.registerModule(m);
//                    }
//                    var str = new StringWriter();
//                    try (var out = json.createGenerator(str)) {
//                        blockEntity.serialize(out, json.getSerializerProvider());
//                    } catch (Exception e) {
//                        Application.log.error(e);
//                    }
//
//                    Application.log.info("Time took: {}ms", (System.currentTimeMillis() - t));
//                }
//            }
//        }

        float speed = noClip ? 2f : 7f;
        if (input.pressed(GLFW_KEY_LEFT_SHIFT) || input.pressed(GLFW_KEY_RIGHT_SHIFT)) {
            speed *= 1.5f;
        }

        if (noClip) {
            speed *= Math.max(0, input.getScrollOffset());
        }

        int xf = input.axis(GLFW_KEY_A, GLFW_KEY_D);

        if (!noClip) {
            tmp.set(xf, 0).scale(speed * Time.delta);
        } else {
            velocity.set(0, 0);

            setX(getX() + speed * xf);
            int yf = input.axis(GLFW_KEY_S, GLFW_KEY_W);
            setY(getY() + speed * yf);
        }

        if (jumpedTicks > 0) {
            jumpedTicks = Math.max(jumpedTicks - Time.delta, 0);
        } else {
            if (hasFloor() && Math.abs(velocity.y) <= GAP && input.pressed(GLFW_KEY_SPACE)) {
                tmp.y += 9;
                jumpedTicks = 5 * Math.max(Time.delta, 0.01f);
            }
        }

        velocity.add(tmp);

        Point2i blockUnderMouse = Global.input.mouseBlockPos();
        if (world.getBlockId(blockUnderMouse.x, blockUnderMouse.y) <= 0) {
            return;
        }
        var blockEntity = world.getEntity(blockUnderMouse.x, blockUnderMouse.y);
        if (blockEntity != null) {
            if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT)) blockEntity.onMouseClick();

            blockEntity.onMouseHover();

            final char interactionChar = 'E';
            if (input.pressed(interactionChar)) {
//                blockEntity.onInteraction();
                // ====== То есть это хотим ======
//                int iconY = (root.y * blockSize) + blockSize;
//                int iconX = (root.x * blockSize) + blockSize;
//                batch.draw(atlas.byPath("UI/GUI/interactionIcon.png"), iconX, iconY);
//                batch.draw(Window.defaultFont.getGlyph(interactionChar),
//                        (root.x * blockSize + 16) + blockSize,
//                        (root.y * blockSize + 12) + blockSize);
//
//                if (interactionButtonPressed) {
//                    currentInteraction = new Thread(interaction);
//                    currentInteraction.start();
//                }
            }
        }

    }

    public boolean hasFloor() {
        int minX = (int) Math.floor(x / blockSize);
        int maxX = (int) Math.floor((x + getTexture().width()) / blockSize);
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

    public float getCurrentHP() {
        return currentHp;
    }

    public void incrementCurrentHP(float increment) {
        this.currentHp += increment;
    }

    public float getWeight() {
        return DynamicObjectsConst.getConst(id).weight;
    }

    public Atlas.Region getTexture() {
        return DynamicObjectsConst.getConst(id).texture;
    }

    public float getMotionVectorX() {
        return velocity.x;
    }

    public float getMotionVectorY() {
        return velocity.y;
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
