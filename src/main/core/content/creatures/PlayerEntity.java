package core.content.creatures;

import core.Application;
import core.EventHandling.Config;
import core.EventHandling.EventHandler;
import core.Global;
import core.Time;
import core.World.World;
import core.content.entity.BaseCreatureEntity;
import core.content.entity.HitboxComponent;
import core.math.Point2i;
import core.math.Vector2f;

import java.io.StringWriter;
import java.nio.file.Files;
import java.util.concurrent.ThreadLocalRandom;

import static core.EventHandling.Config.json;
import static core.Global.*;
import static core.World.Creatures.Player.Player.*;
import static org.lwjgl.glfw.GLFW.*;

public class PlayerEntity extends BaseCreatureEntity<PlayerType> {
    private float jumpedTicks; // откат прыжка

    protected PlayerEntity(PlayerType creature) {
        super(creature);
    }

    private static final Vector2f tmp = new Vector2f();

    @Override
    protected void onDamage(float d) {
        lastDamage += d;
        lastDamageTime = System.currentTimeMillis();
    }

    public void updateInput() {
        // todo тут надо проверять элемент UI на фокусировку, т.е. на порядок отображения (фокусирован = самый последний элемент)
        if (EventHandler.isKeylogging() || isDead()) {
            return;
        }

        if (EventHandler.debugLevel >= 2) {
            if (input.justPressed(GLFW_KEY_F1)) app.setFramerate(60);
            if (input.justPressed(GLFW_KEY_F2)) app.setFramerate(1000);
            if (input.justPressed(GLFW_KEY_F3)) serializeWorld();
            if (input.justPressed(GLFW_KEY_F4)) deserializeWorld();
            if (input.justClicked(GLFW_MOUSE_BUTTON_RIGHT)) serializeTargetBlock();
        }

        //todo было 9
        float speed = noClip ? 2f : 9f * ThreadLocalRandom.current().nextFloat(1, 1.15f);
        if (input.pressed(GLFW_KEY_LEFT_SHIFT) || input.pressed(GLFW_KEY_RIGHT_SHIFT)) {
            speed *= ThreadLocalRandom.current().nextFloat(1.5f, 1.75f);
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
//                batch.draw(atlas.get("UI/GUI/interactionIcon"), iconX, iconY);
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

    private void deserializeWorld() {
        long t = System.currentTimeMillis();
        try {
            var reader = Files.readString(assets.workingDir().resolve("open worl.json"));
            World w = json.readValue(reader, World.class);
            var tree = json.valueToTree(w);
            Files.writeString(assets.workingDir().resolve("open worl (2).json"), tree.toString());

            var refTree = json.readTree(reader);
            if (!tree.equals(refTree)) {
                Application.log.info("РАЗНЫЕ !!!");
            }

        } catch (Exception e) {
            Application.log.error(e);
            e.printStackTrace();
        }
        Application.log.info("Time took: {}ms", (System.currentTimeMillis() - t));
    }

    private static void serializeTargetBlock() {
        Point2i blockUnderMouse = Global.input.mouseBlockPos();

        if (world.getBlockId(blockUnderMouse.x, blockUnderMouse.y) > 0) {
            var blockEntity = world.getEntity(blockUnderMouse.x, blockUnderMouse.y);
            if (blockEntity != null) {
                long t = System.currentTimeMillis();

                var str = new StringWriter();
                try (var out = json.createGenerator(str)) {
                    blockEntity.serialize(out, json.getSerializerProvider());
                } catch (Exception e) {
                    Application.log.error(e);
                }
                System.out.println(str);

                Application.log.info("Time took: {}ms", (System.currentTimeMillis() - t));
            }
        }
    }

    private static void serializeWorld() {
        long t = System.currentTimeMillis();
        try {
            Files.writeString(assets.workingDir().resolve("open worl.json"), Config.json.writeValueAsString(world));
        } catch (Exception e) {
            Application.log.error(e);
        }
        Application.log.info("Time took: {}ms", (System.currentTimeMillis() - t));
    }

    @Override
    public CollisionResult onCollide(HitboxComponent them) {
        return CollisionResult.WALKTHROUGH;
    }

    @Override
    public String toString() {
        return "Player#" + id;
    }
}
