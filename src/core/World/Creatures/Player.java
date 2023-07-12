package core.World.Creatures;

import core.EventHandling.EventHandler;
import core.World.Textures.DynamicWorldObjects;
import static core.World.HitboxMap.*;
import static core.World.WorldGenerator.DynamicObjects;
import static core.World.WorldGenerator.SizeX;
import static org.lwjgl.glfw.GLFW.*;

public class Player {
    public static boolean noClip = false;

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
}
