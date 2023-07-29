package core.World.Creatures.Player;

import core.EventHandling.EventHandler;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Placeable.PlaceableItems;
import core.World.Creatures.Player.Inventory.Tools;
import core.World.Textures.DynamicWorldObjects;
import core.World.Textures.ShadowMap;
import core.World.Textures.StaticWorldObjects;
import core.World.Textures.TextureDrawing;
import java.awt.*;
import static core.EventHandling.EventHandler.getMousePos;
import static core.Window.start;
import static core.World.Creatures.Player.Inventory.Inventory.*;
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
        if (currentObject != null) {
            new Thread(() -> {

                if (currentObjectType.equals("tool")) {
                    Tools tool = inventoryObjects[currentObject.x][currentObject.y].tool;

                    int blockX = getBlockUnderMousePoint().x;
                    int blockY = getBlockUnderMousePoint().y;
                    StaticWorldObjects object = StaticObjects[getBlockUnderMousePoint().x][getBlockUnderMousePoint().y];

                    if (!lastDestroySet) {
                        Color color = ShadowMap.getColor(blockX, blockY);
                        int a = (color.getRed() + color.getGreen() + color.getBlue()) / 3;

                        lastDestroySet = true;
                        lastDestroyIndexX = blockX;
                        lastDestroyIndexY = blockY;

                        if (ShadowMap.colorDegree[blockX][blockY] == 0 && getDistanceUMB() <= tool.maxInteractionRange) {
                            ShadowMap.setColor(blockX, blockY, new Color(Math.max(0, a - 150), Math.max(0, a - 150), a, 255));
                        } else {
                            ShadowMap.setColor(blockX, blockY, new Color(a, Math.max(0, a - 150), Math.max(0, a - 150), 255));
                        }
                    }
                    if (blockX != lastDestroyIndexX || blockY != lastDestroyIndexY) {
                        lastDestroySet = false;
                        ShadowMap.update();
                    }
                    if (EventHandler.getMousePress() && System.currentTimeMillis() - tool.lastHitTime >= tool.secBetweenHits && getDistanceUMB() <= tool.maxInteractionRange && object.currentHp > 0 && ShadowMap.colorDegree[blockX][blockY] == 0) {
                        tool.lastHitTime = System.currentTimeMillis();
                        object.currentHp -= tool.damage;

                        if (object.currentHp <= 0 && object.type != StaticWorldObjects.Types.GAS) {
                            createElementPlaceable(new StaticWorldObjects(object.path, object.x, object.y, object.type));
                            object.destroyObject();
                        }
                    }
                } else if (currentObjectType.equals("placeable")) {
                    if (getMousePos().x > (Inventory.inventoryOpen ? 1488 : 1866) && getMousePos().y > 756) {
                        return;
                    }

                    int blockX = getBlockUnderMousePoint().x;
                    int blockY = getBlockUnderMousePoint().y;
                    PlaceableItems placeable = inventoryObjects[currentObject.x][currentObject.y].placeable;

                    if (StaticObjects[getBlockUnderMousePoint().x][getBlockUnderMousePoint().y].gas && EventHandler.getMousePress() && Player.getDistanceUMB() < 9) {
                        if (placeable.factoryObject != null) {
                            //закос на будущее
                        } else if (placeable.staticWorldObject != null && (StaticObjects[blockX][blockY + 1].solid || StaticObjects[blockX][blockY - 1].solid || StaticObjects[blockX + 1][blockY].solid || StaticObjects[blockX - 1][blockY].solid)) {
                            inventoryObjects[currentObject.x][currentObject.y].countInCell--;
                            StaticObjects[blockX][blockY] = new StaticWorldObjects(placeable.staticWorldObject.path, blockX * 16, blockY * 16, placeable.staticWorldObject.type);
                            StaticObjects[blockX][blockY].solid = true;
                            TextureDrawing.loadObjects();
                            ShadowMap.update();

                            if (inventoryObjects[currentObject.x][currentObject.y].countInCell <= 0) {
                                inventoryObjects[currentObject.x][currentObject.y] = null;
                                currentObject = null;
                            }
                        }
                    }
                }
            }).start();
        }
    }

    public static Point getBlockUnderMousePoint() {
        float mouseX = (getMousePos().x - 960) / 3f + 16;
        float mouseY = (getMousePos().y - 540) / 3f + 64;

        int blockX = (int) ((mouseX + DynamicObjects[0].x) / 16);
        int blockY = (int) ((mouseY + DynamicObjects[0].y) / 16);

        return new Point(blockX, blockY);
    }

    public static int getDistanceUMB() {
        return (int) Math.abs((DynamicObjects[0].x / 16 - getBlockUnderMousePoint().x) + (DynamicObjects[0].y / 16 - getBlockUnderMousePoint().y));
    }

    public static void updatePlayerGUI() {
        if (start) {
            Inventory.update();
        }
    }
}
