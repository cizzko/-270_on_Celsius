package core.World.Creatures.Player;

import core.EventHandling.EventHandler;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Textures.DynamicWorldObjects;
import core.World.Textures.ShadowMap;
import java.awt.*;
import static core.EventHandling.EventHandler.getMousePos;
import static core.Window.start;
import static core.World.HitboxMap.*;
import static core.World.WorldGenerator.*;
import static org.lwjgl.glfw.GLFW.*;

public class Player {
    public static boolean noClip = false, lastDestroySet = false;
    private static int lastDestroyIndexX = 0, lastDestroyIndexY = 0;

    public static void setPlayerPos(float x, float y) {
        DynamicObjects[0].x = x == 0 ? DynamicObjects[0].x : x;
        DynamicObjects[0].y = y == 0 ? DynamicObjects[0].y : y;
    }

    public static void updatePlayerJump() {
        if (EventHandler.getKey(GLFW_KEY_SPACE)) {
            DynamicObjects[0].jump(52, 600);
        }
    }

    public static void updatePlayerMove() {
        float increment = noClip ? 0.5f : 0.1f;

        if (EventHandler.getKey(GLFW_KEY_D) && DynamicObjects[0].x + 24 < SizeX * 16 && (noClip || !checkIntersStaticR(DynamicObjects[0].x + 0.1f, DynamicObjects[0].y, 24, 24))) {
            DynamicObjects[0].x += increment;
        }
        if (EventHandler.getKey(GLFW_KEY_A) && DynamicObjects[0].x > 0 && (noClip || !checkIntersStaticL(DynamicObjects[0].x - 0.1f, DynamicObjects[0].y, 24))) {
            DynamicObjects[0].x -= increment;
        }
        if (noClip && EventHandler.getKey(GLFW_KEY_S)) {
            DynamicObjects[0].y -= increment;
        }
        if (noClip && EventHandler.getKey(GLFW_KEY_W)) {
            DynamicObjects[0].y += increment;
        }
        DynamicObjects[0].notForDrawing = noClip;
    }

    public static void updatePlayerDrop() {
        if (DynamicObjects[0] != null && !noClip) {
            DynamicWorldObjects obj = DynamicObjects[0];

            if (!checkIntersStaticD(obj.x, obj.y, 24, 24)) {
                obj.isDropping = true;
                obj.dropSpeed += 0.001f;
                obj.y -= obj.dropSpeed;
            } else {
                obj.dropSpeed = 0;
                obj.isDropping = false;
            }
        }
    }

    public static void updateDestroyBlocks() {
        if (Inventory.currentObjectType.equals("tool")) {
            new Thread(() -> {
                float mouseX = (getMousePos().x - 960) / 3f + 16;
                float mouseY = (getMousePos().y - 540) / 3f + 64;

                int blockX = (int) ((mouseX + DynamicObjects[0].x) / 16);
                int blockY = (int) ((mouseY + DynamicObjects[0].y) / 16);

                if (!lastDestroySet) {
                    Color color = ShadowMap.getColor(blockX, blockY);
                    int a = (color.getRed() + color.getGreen() + color.getBlue()) / 3;

                    lastDestroySet = true;
                    lastDestroyIndexX = blockX;
                    lastDestroyIndexY = blockY;

                    if (ShadowMap.colorDegree[blockX][blockY] == 0) {
                        ShadowMap.setColor(blockX, blockY, new Color(Math.max(0, a - 150), Math.max(0, a - 150), a, 255));
                    } else {
                        ShadowMap.setColor(blockX, blockY, new Color(a, Math.max(0, a - 150), Math.max(0, a - 150), 255));
                    }
                }
                if (blockX != lastDestroyIndexX || blockY != lastDestroyIndexY) {
                    lastDestroySet = false;
                    ShadowMap.update();
                }
            }).start();
        }
    }

    public static void updatePlayerGUI() {
        if (start) {
            Inventory.update();
        }
    }
}
