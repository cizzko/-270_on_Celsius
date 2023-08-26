package core.World.Creatures.Player;

import core.EventHandling.EventHandler;
import core.World.Creatures.Player.BuildMenu.BuildMenu;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.Creatures.Player.Inventory.Items.Placeable.Factories;
import core.World.Creatures.Player.Inventory.Items.Placeable.PlaceableItems;
import core.World.Creatures.Player.Inventory.Items.Tools;
import core.World.Creatures.Player.Inventory.Items.Weapons.Ammo.Bullets;
import core.World.HitboxMap;
import core.World.Textures.*;
import java.awt.*;
import java.awt.geom.Point2D;
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
        if (EventHandler.getKeyClick(GLFW_KEY_SPACE)) {
            DynamicObjects[0].jump(0.45f);
        }
    }

    public static void updatePlayerMove() {
        float increment = noClip ? 0.5f : 0.1f;

        if (EventHandler.getKey(GLFW_KEY_D) && DynamicObjects[0].x + 24 < SizeX * 16 && (noClip || !checkIntersStaticR(DynamicObjects[0].x + 0.1f, DynamicObjects[0].y, 24, 24))) {
            DynamicObjects[0].motionVector.x = increment;
        }
        if (EventHandler.getKey(GLFW_KEY_A) && DynamicObjects[0].x > 0 && (noClip || !checkIntersStaticL(DynamicObjects[0].x - 0.1f, DynamicObjects[0].y, 24))) {
            DynamicObjects[0].motionVector.x = -increment;
        }
        if (noClip && EventHandler.getKey(GLFW_KEY_S)) {
            DynamicObjects[0].motionVector.y = -increment;
        }
        if (noClip && EventHandler.getKey(GLFW_KEY_W)) {
            DynamicObjects[0].motionVector.y = increment;
        }
        DynamicObjects[0].notForDrawing = noClip;
    }

    public static void updateInventoryInteraction() {
        if (currentObject != null) {
            new Thread(() -> {
                updatePlaceableInteraction();
                updateToolInteraction();
            }).start();
        }
    }

    private static void updatePlaceableInteraction() {
        if (currentObjectType == Items.Types.PLACEABLE_BLOCK || currentObjectType == Items.Types.PLACEABLE_FACTORY) {
            if (getMousePos().x > (Inventory.inventoryOpen ? 1488 : 1866) && getMousePos().y > 756) {
                return;
            }
            if (StaticObjects[getBlockUnderMousePoint().x][getBlockUnderMousePoint().y].gas && EventHandler.getMousePress() && Player.getDistanceUMB() < 9) {
                int blockX = getBlockUnderMousePoint().x;
                int blockY = getBlockUnderMousePoint().y;

                if (currentObject != null) {
                    PlaceableItems placeable = inventoryObjects[currentObject.x][currentObject.y].placeable;

                    updatePlaceableFactory(placeable, blockX, blockY);
                    updatePlaceableBlock(placeable, blockX, blockY);
                }
            }
        }
    }

    private static void updatePlaceableFactory(PlaceableItems placeable, int blockX, int blockY) {
        if (placeable.factoryObject != null) {
            StaticWorldObjects obj = HitboxMap.checkIntersectionsInside(blockX * 16, blockY * 16, TextureLoader.getSize(placeable.factoryObject.path).width, TextureLoader.getSize(placeable.factoryObject.path).height);

            if (obj == null || !obj.solid) {
                Factories factory = placeable.factoryObject;
                factory.x = blockX * 16;
                factory.y = blockY * 16;

                Factories.factories.add(placeable.factoryObject);
                if (currentObject != null) {
                    decrementItem(currentObject.x, currentObject.y);
                }
            }
        }
    }

    private static void updatePlaceableBlock(PlaceableItems placeable, int blockX, int blockY) {
        if (placeable.staticWorldObject != null && (StaticObjects[blockX][blockY + 1].solid || StaticObjects[blockX][blockY - 1].solid || StaticObjects[blockX + 1][blockY].solid || StaticObjects[blockX - 1][blockY].solid)) {
            decrementItem(currentObject.x, currentObject.y);
            StaticObjects[blockX][blockY] = new StaticWorldObjects(placeable.staticWorldObject.path, blockX * 16, blockY * 16, placeable.staticWorldObject.type);
            StaticObjects[blockX][blockY].solid = true;
            TextureDrawing.loadObjects();
            ShadowMap.update();

        }
    }

    private static void updateToolInteraction() {
        if (currentObjectType == Items.Types.TOOL) {
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
                    createElementPlaceable(new StaticWorldObjects(object.path, object.x, object.y, object.type), "none");
                    object.destroyObject();
                }
            }
        }
    }

    public static Point getBlockUnderMousePoint() {
        int blockX = (int) Math.max(0, Math.min(getWorldMousePoint().x / 16, StaticObjects.length));
        int blockY = (int) Math.max(0, Math.min(getWorldMousePoint().y / 16, StaticObjects.length));

        return new Point(blockX, blockY);
    }

    public static Point2D.Float getWorldMousePoint() {
        float blockX = ((getMousePos().x - 960) / 3f + 16) + DynamicObjects[0].x;
        float blockY = ((getMousePos().y - 540) / 3f + 64) + DynamicObjects[0].y;

        return new Point2D.Float(blockX, blockY);
    }

    public static int getDistanceUMB() {
        return (int) Math.abs((DynamicObjects[0].x / 16 - getBlockUnderMousePoint().x) + (DynamicObjects[0].y / 16 - getBlockUnderMousePoint().y));
    }

    public static void updatePlayerGUI() {
        if (start) {
            Inventory.update();
            Bullets.drawBullets();
            BuildMenu.draw();
        }
    }

    public static void updatePlayerGUILogic() {
        if (start) {
            BuildMenu.updateLogic();
        }
    }

    public static float getDistanceToPlayerABS(float x) {
        if (DynamicObjects[0] != null) {
            return Math.abs((DynamicObjects[0].x - x));
        }
        return 0;
    }

    public static float getDistanceToPlayer(float x) {
        if (DynamicObjects[0] != null) {
            return (DynamicObjects[0].x - x);
        }
        return 0;
    }
}
