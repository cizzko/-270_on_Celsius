package core.content.creatures;

import core.EventHandling.EventHandler;
import core.Global;
import core.Time;
import core.entity.BaseCreatureEntity;
import core.math.Point2i;
import core.math.Vector2f;

import static core.Global.*;
import static core.World.Creatures.Player.Player.noClip;
import static org.lwjgl.glfw.GLFW.*;

public class PlayerEntity extends BaseCreatureEntity<PlayerType> {
    private float jumpedTicks; // откат прыжка

    protected PlayerEntity(PlayerType creature) {
        super(creature);
    }

    private static final Vector2f tmp = new Vector2f();

    public void updateInput() {
        // todo тут надо проверять элемент UI на фокусировку, т.е. на порядок отображения (фокусирован = самый последний элемент)
        if (EventHandler.isKeylogging()) {
            return;
        }

        if (input.justPressed(GLFW_KEY_F1)) app.setFramerate(60);
        if (input.justPressed(GLFW_KEY_F2)) app.setFramerate(1000);

//        if (input.justPressed(GLFW_KEY_F3)) {
//            final ObjectMapper json = new ObjectMapper();
//            {
//                var m = new SimpleModule();
//                m.addSerializer(new ItemStack.ItemStackSerializer());
//                m.addSerializer(new World.WorldSerializer());
//                m.addSerializer(new ItemStack.ItemStackGridSerializer());
//                json.registerModule(m);
//            }
//
//            long t = System.currentTimeMillis();
//            try {
//                Files.writeString(assets.workingDir().resolve("open_worl.json"), Config.json.writeValueAsString(world));
//            } catch (Exception e) {
//                Application.log.error(e);
//            }
//            Application.log.info("Time took: {}ms", (System.currentTimeMillis() - t));
//        }
//
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
}
