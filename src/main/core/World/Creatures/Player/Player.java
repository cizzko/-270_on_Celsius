package core.World.Creatures.Player;

import core.EventHandling.Logging.Config;
import core.Global;
import core.Utils.SimpleColor;
import core.World.Creatures.DynamicWorldObjects;
import core.World.Creatures.Player.BuildMenu.BuildMenu;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.Creatures.Player.Inventory.Items.Tools;
import core.World.Creatures.Player.Inventory.Items.Weapons.Ammo.Bullets;
import core.World.StaticWorldObjects.TemperatureMap;
import core.World.Textures.*;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.StaticWorldObjects;
import java.awt.Point;
import java.awt.geom.Point2D;

import static core.Window.assetsDir;
import static core.Window.start;
import static core.World.Creatures.Player.Inventory.Inventory.*;
import static core.World.HitboxMap.*;
import static core.World.StaticWorldObjects.StaticWorldObjects.*;
import static core.World.WorldGenerator.*;
import static org.lwjgl.glfw.GLFW.*;

public class Player {
    public static boolean noClip = false;
    private static int transparencyHPline = Config.getFromConfig("AlwaysOnPlayerHPLine").equals("true") ? 220 : 0;
    public static int lastDamage = 0;
    public static long lastDamageTime = System.currentTimeMillis();
    private static long lastChangeTransparency = System.currentTimeMillis(), lastChangeLengthDamage = System.currentTimeMillis();

    public static void updatePlayerJump() {
        if (Global.input.pressed(GLFW_KEY_SPACE)) {
            DynamicObjects.get(0).jump(0.45f);
        }
    }

    public static void updatePlayerMove() {
        float increment = noClip ? 0.5f : 0.1f;

//        if (EventHandler.getKeyClick(GLFW_KEY_Q) && DynamicObjects.get(0).getAnimationSpeed() == 0) {
//            DynamicObjects.get(0).setPath(assetsDir("World/Creatures/playerLeft/player"));
//            DynamicObjects.get(0).setAnimationSpeed(30);
//            setObject((int) ((DynamicObjects.get(0).getX() - 1) / 16), (int) (DynamicObjects.get(0).getY() / 16 + 1), StaticWorldObjects.decrementHp(getObject((int) ((DynamicObjects.get(0).getX() - 1) / 16), (int) (DynamicObjects.get(0).getY() / 16 + 1)), 10));
//        }
//        if (EventHandler.getKeyClick(GLFW_KEY_E) && DynamicObjects.get(0).getAnimationSpeed() == 0) {
//            DynamicObjects.get(0).setPath(assetsDir("World/Creatures/playerRight/player"));
//            DynamicObjects.get(0).setAnimationSpeed(30);
//            setObject((int) (DynamicObjects.get(0).getX() / 16 + 2), (int) (DynamicObjects.get(0).getY() / 16 + 1), StaticWorldObjects.decrementHp(getObject((int) (DynamicObjects.get(0).getX() / 16 + 2), (int) (DynamicObjects.get(0).getY() / 16 + 1)), 10));
//        }

        if (Global.input.pressed(GLFW_KEY_D) && DynamicObjects.get(0).getX() + 24 < SizeX * 16 && (noClip || !checkIntersStaticR(DynamicObjects.get(0).getX() + 0.1f, DynamicObjects.get(0).getY(), 24, 24))) {
            DynamicObjects.get(0).setMotionVectorX(increment);
        }
        if (Global.input.pressed(GLFW_KEY_A) && DynamicObjects.get(0).getX() > 0 && (noClip || !checkIntersStaticL(DynamicObjects.get(0).getX() - 0.1f, DynamicObjects.get(0).getY(), 24))) {
            DynamicObjects.get(0).setMotionVectorX(-increment);
        }
        if (noClip && Global.input.pressed(GLFW_KEY_S)) {
            DynamicObjects.get(0).setMotionVectorY(-increment);
        }
        if (noClip && Global.input.pressed(GLFW_KEY_W)) {
            DynamicObjects.get(0).setMotionVectorY(increment);
        }
    }

    public static void updateInventoryInteraction() {
        if (currentObject != null) {
            updatePlaceableInteraction();
        }
    }

    private static void updatePlaceableInteraction() {
        if (currentObjectType == Items.Types.PLACEABLE && Global.input.justClicked(GLFW_MOUSE_BUTTON_LEFT)) {
            Point mouse = Global.input.mousePos();
            int xBound = (Inventory.inventoryOpen ? 1488 : 1866);
            if (mouse.x > xBound && mouse.y > 756) {
                return;
            }
            Point blockUMB = getBlockUnderMousePoint();

            if (getType(getObject(blockUMB.x, blockUMB.y)) == StaticObjectsConst.Types.GAS && Player.getDistanceUnderMouse() < 9) {
                int blockX = blockUMB.x;
                int blockY = blockUMB.y;

                if (currentObject != null) {
                    short placeable = Inventory.getCurrent().placeable;
                    updatePlaceableBlock(placeable, blockX, blockY);
                }
            }
        }
    }

    private static void updatePlaceableBlock(short placeable, int blockX, int blockY) {
        if (canPlace(placeable, blockX, blockY)) {
            decrementItem(currentObject.x, currentObject.y);
            setObject(blockX, blockY, placeable);
            ShadowMap.update();
        }
    }

    public static boolean canPlace(short placeable, int blockX, int blockY) {
        if (underMouseItem == null) {
            if (StaticObjectsConst.getConst(getId(placeable)).optionalTiles == null && getType(getObject(blockX, blockY)) == StaticObjectsConst.Types.GAS && (getType(getObject(blockX, blockY + 1)) == StaticObjectsConst.Types.SOLID || getType(getObject(blockX, blockY - 1)) == StaticObjectsConst.Types.SOLID || getType(getObject(blockX + 1, blockY)) == StaticObjectsConst.Types.SOLID || getType(getObject(blockX - 1, blockY)) == StaticObjectsConst.Types.SOLID)) {
                return true;
            } else if (StaticObjectsConst.getConst(getId(placeable)).optionalTiles != null && getType(getObject(blockX, blockY - 1)) == StaticObjectsConst.Types.SOLID && getType(getObject(blockX, blockY)) == StaticObjectsConst.Types.GAS) {
                short[][] tiles = StaticObjectsConst.getConst(getId(placeable)).optionalTiles;

                for (int x = 0; x < tiles.length; x++) {
                    for (int y = 0; y < tiles[0].length; y++) {
                        if (getType(getObject(x + blockX, y + blockY)) == StaticObjectsConst.Types.SOLID && getType(tiles[x][y]) == StaticObjectsConst.Types.SOLID) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static void updateToolInteraction() {
        if (currentObjectType == Items.Types.TOOL && currentObject != null) {
            Tools tool = Inventory.getCurrent().tool;
            Point blockUMB = getBlockUnderMousePoint();
            int blockX = blockUMB.x;
            int blockY = blockUMB.y;
            short object = getObject(blockX, blockY);

            if (object != 0 && getPath(object) != null && !StaticObjectsConst.getConst(getId(object)).hasMotherBlock && StaticObjectsConst.getConst(getId(object)).optionalTiles == null) {
                updateNonStructure(blockX, blockY, object, tool);
            } else if (StaticObjectsConst.getConst(getId(object)).hasMotherBlock || StaticObjectsConst.getConst(getId(object)).optionalTiles != null) {
                updateStructure(blockX, blockY, object, tool);
            }
        }
    }

    private static void updateNonStructure(int blockX, int blockY, short object, Tools tool) {
        if (getDistanceUnderMouse() <= tool.maxInteractionRange && ShadowMap.getDegree(blockX, blockY) == 0) {
            drawBlock(blockX, blockY, object, true);

            if (Global.input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && getId(object) != 0 && System.currentTimeMillis() - tool.lastHitTime >= tool.secBetweenHits && getHp(object) > 0) {
                tool.lastHitTime = System.currentTimeMillis();

                if (getHp(decrementHp(object, (int) tool.damage)) <= 0) {
                    createElementPlaceable(object, "none");
                    destroyObject(blockX, blockY);
                } else {
                    setObject(blockX, blockY, decrementHp(object, (int) tool.damage));
                }
            }
        } else {
            drawBlock(blockX, blockY, object, false);
        }
    }

    private static void updateStructure(int blockX, int blockY, short object, Tools tool) {
        Point root = findRoot(blockX, blockY);

        if (root != null) {
            blockX = root.x;
            blockY = root.y;

            if (getDistanceUnderMouse() <= tool.maxInteractionRange && ShadowMap.getDegree(blockX, blockY) == 0) {
                drawStructure(blockX, blockY, true, getObject(root.x, root.y), StaticObjectsConst.getConst(StaticWorldObjects.getId(getObject(blockX, blockY))).optionalTiles);

                if (Global.input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && getId(object) != 0 && System.currentTimeMillis() - tool.lastHitTime >= tool.secBetweenHits && getHp(object) > 0) {
                    tool.lastHitTime = System.currentTimeMillis();
                    decrementHpMulti(blockX, blockY, (int) tool.damage);
                }
            } else {
                drawStructure(blockX, blockY, false, getObject(root.x, root.y), StaticObjectsConst.getConst(StaticWorldObjects.getId(getObject(blockX, blockY))).optionalTiles);
            }
        }
    }

    public static void drawStructure(int blockX, int blockY, boolean breakable, short root, short[][] tiles) {
        drawBlock(blockX, blockY, root, breakable);
        for (int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[0].length; y++) {

                if (tiles[x][y] != 0) {
                    drawBlock(x + blockX, y + blockY, tiles[x][y], breakable);
                }
            }
        }
    }

    public static Point findRoot(int cellX, int cellY) {
        int maxCellsX = 4;
        int maxCellsY = 4;

        for (int blockX = 0; blockX < maxCellsX; blockX++) {
            for (int blockY = 0; blockY < maxCellsY; blockY++) {
                StaticObjectsConst objConst = StaticObjectsConst.getConst(getId(getObject(cellX - blockX, cellY - blockY)));
                if (objConst != null && objConst.optionalTiles != null) {
                    return new Point(cellX - blockX, cellY - blockY);
                }
            }
        }
        return null;
    }

    private static void decrementHpMulti(int cellX, int cellY, int hp) {
        Point root = findRoot(cellX, cellY);

        if (root != null && getObject(root.x, root.y) != 0) {
            short rootObj = getObject(root.x, root.y);

            for (int x = -(cellX - root.x); x < StaticObjectsConst.getConst(getId(rootObj)).optionalTiles.length - (cellX - root.x); x++) {
                for (int y = -(cellY - root.y); y < StaticObjectsConst.getConst(getId(rootObj)).optionalTiles[0].length - (cellY - root.y); y++) {
                    short object = getObject(x + cellX, y + cellY);

                    if (getHp(decrementHp(object, hp)) <= 0 && getType(object) != StaticObjectsConst.Types.GAS) {
                        createElementPlaceable(rootObj, "");
                        destroyObject(cellX, cellY);
                        break;
                    } else if (getType(object) != StaticObjectsConst.Types.GAS) {
                        StaticObjects[(x + cellX) + SizeX * (y + cellY)] = decrementHp(object, hp);
                    }
                }
            }
        }
    }

    public static void drawBlock(int cellX, int cellY, short obj, boolean breakable) {
        SimpleColor color = ShadowMap.getColor(cellX, cellY);
        int a = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
        SimpleColor blockColor = breakable ? new SimpleColor(Math.max(0, a - 150), Math.max(0, a - 150), a, 255) : new SimpleColor(a, Math.max(0, a - 150), Math.max(0, a - 150), 255);
        int xBlock = cellX * 16;
        int yBlock = cellY * 16;

        if (getHp(obj) > getMaxHp(obj) / 1.5f) {
            TextureDrawing.drawTexture(getPath(obj), xBlock, yBlock, 3f, blockColor, false, false);

        } else if (getHp(obj) < getMaxHp(obj) / 3) {
            TextureDrawing.drawMultiTexture(getPath(obj), assetsDir("World/Blocks/damaged1.png"), xBlock, yBlock, 3f, blockColor, false, false);

        } else {
            TextureDrawing.drawMultiTexture(getPath(obj), assetsDir("World/Blocks/damaged0.png"), xBlock, yBlock, 3f, blockColor, false, false);
        }
    }

    public static void drawBlock(int cellX, int cellY, String path, boolean breakable) {
        SimpleColor color = ShadowMap.getColor(cellX, cellY);
        int a = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
        SimpleColor blockColor = breakable ? new SimpleColor(Math.max(0, a - 150), Math.max(0, a - 150), a, 255) : new SimpleColor(a, Math.max(0, a - 150), Math.max(0, a - 150), 255);
        int xBlock = cellX * 16;
        int yBlock = cellY * 16;

        TextureDrawing.drawTexture(path, xBlock, yBlock, 3f, blockColor, false, false);
    }

    public static Point getBlockUnderMousePoint() {
        int blockX = (int) Math.max(0, Math.min(getWorldMousePoint().x / 16, SizeX));
        int blockY = (int) Math.max(0, Math.min(getWorldMousePoint().y / 16, SizeY));

        return new Point(blockX, blockY);
    }

    public static Point2D.Float getWorldMousePoint() {
        float blockX = ((Global.input.mousePos().x - 960) / 3f + 16) + DynamicObjects.get(0).getX();
        float blockY = ((Global.input.mousePos().y - 540) / 3f + 64) + DynamicObjects.get(0).getY();

        return new Point2D.Float(blockX, blockY);
    }

    public static int getDistanceUnderMouse() {
        return (int) Math.abs((DynamicObjects.get(0).getX() / 16 - getBlockUnderMousePoint().x) + (DynamicObjects.get(0).getY() / 16 - getBlockUnderMousePoint().y));
    }

    public static void updatePlayerGUI() {
        if (start) {
            Bullets.drawBullets();
            updateTemperatureEffect();
            Inventory.update();
            BuildMenu.draw();
            updateToolInteraction();
            drawCurrentHP();
        }
    }

    public static void updatePlayerGUILogic() {
        if (start) {
            BuildMenu.updateLogic();
        }
    }

    private static void updateTemperatureEffect() {
        DynamicWorldObjects player = DynamicObjects.get(0);
        int temp = (int) TemperatureMap.getAverageTempAroundDynamic(player.getX(), player.getY(), player.getPath());
        int upperLimit = 100;
        int lowestLimit = -20;
        int maxColor = 90;

        int a = 0;
        if (temp > upperLimit) {
            a = Math.min(maxColor, Math.abs((temp - upperLimit) / 3));
        } else if (temp < lowestLimit) {
            a = Math.min(maxColor, Math.abs((temp + lowestLimit) / 3));
        }

        int r = temp > 0 ? a : 0;
        int b = temp > 0 ? 0 : a;

        TextureDrawing.drawTexture(assetsDir("\\UI\\GUI\\modifiedTemperature.png"), 0, 0, 1, new SimpleColor(r, (int) (b / 2f), b, a), true, false);
    }

    public static void playerMaxHP() {
        DynamicObjects.get(0).setCurrentHp(DynamicObjects.get(0).getMaxHp());
    }

    public static void playerKill() {
        DynamicObjects.get(0).setCurrentHp(0);
    }

    private static void drawCurrentHP() {
        int currentHp = (int) DynamicObjects.get(0).getCurrentHP();
        int maxHp = (int) DynamicObjects.get(0).getMaxHp();

        if (currentHp == maxHp && transparencyHPline > 0 && System.currentTimeMillis() - lastChangeTransparency >= 10 && !Config.getFromConfig("AlwaysOnPlayerHPLine").equals("true")) {
            lastChangeTransparency = System.currentTimeMillis();
            transparencyHPline--;
        } else if (currentHp != maxHp) {
            transparencyHPline = 220;
        }
        if (lastDamage > 0 && System.currentTimeMillis() - lastChangeLengthDamage >= 15 && System.currentTimeMillis() - lastDamageTime >= 300) {
            lastChangeLengthDamage = System.currentTimeMillis();
            lastDamage--;
        }

        if (transparencyHPline > 0) {
            TextureDrawing.drawRectangleBorder(30, 30, 200, 35, 1, new SimpleColor(10, 10, 10, transparencyHPline));
            TextureDrawing.drawRectangle(31, 31, currentHp * 2 - 2, 33, new SimpleColor(150, 0, 20, transparencyHPline));

            if (lastDamage > 0) {
                TextureDrawing.drawRectangle(29 + currentHp * 2, 31, Math.min(lastDamage * 2, 200), 33, new SimpleColor(252, 161, 3, transparencyHPline));
            }
        }
    }
}
