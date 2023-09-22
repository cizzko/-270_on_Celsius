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
import core.World.Textures.StaticWorldObjects.StaticObjectsConst;
import core.World.Textures.StaticWorldObjects.StaticWorldObjects;
import java.awt.*;
import java.awt.geom.Point2D;
import static core.EventHandling.EventHandler.getMousePos;
import static core.Window.defPath;
import static core.Window.start;
import static core.World.Creatures.Player.Inventory.Inventory.*;
import static core.World.HitboxMap.*;
import static core.World.Textures.StaticWorldObjects.StaticWorldObjects.*;
import static core.World.WorldGenerator.*;
import static org.lwjgl.glfw.GLFW.*;

public class Player {
    public static boolean noClip = false;

    public static void updatePlayerJump() {
        if (EventHandler.getKey(GLFW_KEY_SPACE)) {
            DynamicObjects.get(0).jump(0.45f);
        }
    }

    public static void updatePlayerMove() {
        float increment = noClip ? 0.5f : 0.1f;

        if (EventHandler.getKeyClick(GLFW_KEY_Q) && DynamicObjects.get(0).animSpeed == 0) {
            DynamicObjects.get(0).path = defPath + "\\src\\assets\\World\\creatures\\playerLeft\\player";
            DynamicObjects.get(0).animSpeed = 0.03f;
            setObject((int) ((DynamicObjects.get(0).x - 1) / 16), (int) (DynamicObjects.get(0).y / 16 + 1), StaticWorldObjects.decrementHp(getObject((int) ((DynamicObjects.get(0).x - 1) / 16), (int) (DynamicObjects.get(0).y / 16 + 1)), 10));
        }
        if (EventHandler.getKeyClick(GLFW_KEY_E) && DynamicObjects.get(0).animSpeed == 0) {
            DynamicObjects.get(0).path = defPath + "\\src\\assets\\World\\creatures\\playerRight\\player";
            DynamicObjects.get(0).animSpeed = 0.03f;
            setObject((int) (DynamicObjects.get(0).x / 16 + 2), (int) (DynamicObjects.get(0).y / 16 + 1), StaticWorldObjects.decrementHp(getObject((int) (DynamicObjects.get(0).x / 16 + 2), (int) (DynamicObjects.get(0).y / 16 + 1)), 10));
        }

        if (EventHandler.getKey(GLFW_KEY_D) && DynamicObjects.get(0).x + 24 < SizeX * 16 && (noClip || !checkIntersStaticR(DynamicObjects.get(0).x + 0.1f, DynamicObjects.get(0).y, 24, 24))) {
            DynamicObjects.get(0).motionVector.x = increment;
        }
        if (EventHandler.getKey(GLFW_KEY_A) && DynamicObjects.get(0).x > 0 && (noClip || !checkIntersStaticL(DynamicObjects.get(0).x - 0.1f, DynamicObjects.get(0).y, 24))) {
            DynamicObjects.get(0).motionVector.x = -increment;
        }
        if (noClip && EventHandler.getKey(GLFW_KEY_S)) {
            DynamicObjects.get(0).motionVector.y = -increment;
        }
        if (noClip && EventHandler.getKey(GLFW_KEY_W)) {
            DynamicObjects.get(0).motionVector.y = increment;
        }
        DynamicObjects.get(0).notForDrawing = noClip;
    }

    public static void updateInventoryInteraction() {
        if (currentObject != null) {
            updatePlaceableInteraction();
        }
    }

    private static void updatePlaceableInteraction() {
        if ((currentObjectType == Items.Types.PLACEABLE_BLOCK || currentObjectType == Items.Types.PLACEABLE_FACTORY) && EventHandler.getMousePress()) {
            if (getMousePos().x > (Inventory.inventoryOpen ? 1488 : 1866) && getMousePos().y > 756) {
                return;
            }
            Point blockUMB = getBlockUnderMousePoint();

            if (getType(getObject(blockUMB.x, blockUMB.y)) == StaticObjectsConst.Types.GAS && Player.getDistanceUMB() < 9) {
                int blockX = blockUMB.x;
                int blockY = blockUMB.y;

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
            short obj = HitboxMap.checkIntersInsideAll(blockX * 16, blockY * 16, TextureLoader.getSize(placeable.factoryObject.path).width, TextureLoader.getSize(placeable.factoryObject.path).height);

            if (obj == 0 || getType(obj) != StaticObjectsConst.Types.SOLID) {
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
        if (getType(getObject(blockX, blockY)) == StaticObjectsConst.Types.GAS && (getType(getObject(blockX, blockY + 1)) == StaticObjectsConst.Types.SOLID || getType(getObject(blockX, blockY - 1)) == StaticObjectsConst.Types.SOLID || getType(getObject(blockX + 1, blockY)) == StaticObjectsConst.Types.SOLID || getType(getObject(blockX - 1, blockY)) == StaticObjectsConst.Types.SOLID)) {
            decrementItem(currentObject.x, currentObject.y);
            setObject(blockX, blockY, placeable.staticWorldObject);
            ShadowMap.update();
        }
    }

    private static void updateToolInteraction() {
        if (currentObjectType == Items.Types.TOOL && currentObject != null) {
            Tools tool = inventoryObjects[currentObject.x][currentObject.y].tool;

            Point blockUMB = getBlockUnderMousePoint();
            int blockX = blockUMB.x;
            int blockY = blockUMB.y;
            short object = getObject(blockX, blockY);

            if (object > 0 && getPath(object) != null) {
                SimpleColor color = ShadowMap.getColor(blockX, blockY);
                int a = (color.getRed() + color.getGreen() + color.getBlue()) / 3;

                if (getDistanceUMB() <= tool.maxInteractionRange && ShadowMap.getDegree(blockX, blockY) == 0) {
                    TextureDrawing.drawTexture(getPath(object), blockX, blockY, 3, new SimpleColor(Math.max(0, a - 150), Math.max(0, a - 150), a, 255), false, false);

                    if (EventHandler.getMousePress() && getId(object) != 0 && System.currentTimeMillis() - tool.lastHitTime >= tool.secBetweenHits && getHp(object) > 0) {
                        tool.lastHitTime = System.currentTimeMillis();
                        setObject(blockX, blockY, decrementHp(object, (int) tool.damage));

                        if (getHp(object) <= 0) {
                            createElementPlaceable(object, "none");
                            setObject(blockX, blockY, destroyObject(object));
                        }
                    }
                } else {
                    TextureDrawing.drawTexture(getPath(object), blockX, blockY, 3, new SimpleColor(a, Math.max(0, a - 150), Math.max(0, a - 150), 255), false, false);
                }
            }
        }
    }

    public static Point getBlockUnderMousePoint() {
        int blockX = (int) Math.max(0, Math.min(getWorldMousePoint().x / 16, SizeX));
        int blockY = (int) Math.max(0, Math.min(getWorldMousePoint().y / 16, SizeY));

        return new Point(blockX, blockY);
    }

    public static Point2D.Float getWorldMousePoint() {
        float blockX = ((getMousePos().x - 960) / 3f + 16) + DynamicObjects.get(0).x;
        float blockY = ((getMousePos().y - 540) / 3f + 64) + DynamicObjects.get(0).y;

        return new Point2D.Float(blockX, blockY);
    }

    public static int getDistanceUMB() {
        return (int) Math.abs((DynamicObjects.get(0).x / 16 - getBlockUnderMousePoint().x) + (DynamicObjects.get(0).y / 16 - getBlockUnderMousePoint().y));
    }

    public static void updatePlayerGUI() {
        if (start) {
            Inventory.update();
            Bullets.drawBullets();
            BuildMenu.draw();
            updateToolInteraction();
        }
    }

    public static void updatePlayerGUILogic() {
        if (start) {
            BuildMenu.updateLogic();
        }
    }
}
