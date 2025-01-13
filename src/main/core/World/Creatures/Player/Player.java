package core.World.Creatures.Player;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.Utils.SimpleColor;
import core.World.Creatures.DynamicWorldObjects;
import core.World.Creatures.Player.BuildMenu.BuildMenu;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.Creatures.Player.Inventory.Items.Tools;
import core.World.Creatures.Player.Inventory.Items.Weapons.Ammo.Bullets;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.TemperatureMap;
import core.World.Textures.ShadowMap;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator;
import core.g2d.Fill;
import core.g2d.Texture;
import core.graphic.Layer;
import core.math.Point2i;

import static core.Global.*;
import static core.Window.start;
import static core.World.Creatures.Player.Inventory.Inventory.*;
import static core.World.StaticWorldObjects.StaticWorldObjects.*;
import static core.World.WorldGenerator.*;
import static core.World.WorldUtils.getBlockUnderMousePoint;
import static core.World.WorldUtils.getDistanceToMouse;
import static org.lwjgl.glfw.GLFW.*;

public class Player {
    public static Thread currentInteraction;
    public static boolean noClip = false, placeRules = true;
    private static int transparencyHPline = Config.getFromConfig("AlwaysOnPlayerHPLine").equals("true") ? 220 : 0;
    public static final int playerSize = 72;
    public static int lastDamage = 0;
    public static long lastDamageTime = System.currentTimeMillis();
    private static long lastChangeTransparency = System.currentTimeMillis(), lastChangeLengthDamage = System.currentTimeMillis();

    public static void createPlayer(boolean randomSpawn) {
        DynamicObjects.addFirst(DynamicWorldObjects.createDynamic("player", randomSpawn ? (int) (Math.random() * (SizeX * TextureDrawing.blockSize)) : SizeX * 8f));
    }

    public static void updatePlayerJump() {
        if (EventHandler.isKeylogging()) {
            return;
        }

        if (input.pressed(GLFW_KEY_SPACE)) {
            DynamicObjects.getFirst().jump(1.05f);
        }
    }

    public static void updatePlayerMove() {
        // todo тут надо проверять элемент UI на фокусировку, т.е. на порядок отображения (фокусирован = самый последний элемент)
        if (EventHandler.isKeylogging()) {
            return;
        }

        float speed = noClip ? 1.6f : 0.4f;
        DynamicWorldObjects player = DynamicObjects.getFirst();

        int xf = input.axis(GLFW_KEY_A, GLFW_KEY_D);
        int yf = input.axis(GLFW_KEY_S, GLFW_KEY_W);

        if (!noClip) {
            if (xf != 0) {
                player.setMotionVectorX(speed * xf);
            }
            if (yf != 0) {
                player.setMotionVectorY(speed * yf);
            }
        } else {
            player.setMotionVectorY(0);

            player.setX(player.getX() + speed * xf);
            player.setY(player.getY() + speed * yf);
        }
        EventHandler.putDebugValue(false, "[Player] x: " + player.getX() + ", y: " + player.getY(), "PlayerPos");
    }

    public static void updateInventoryInteraction() {
        if (currentObject != null) {
            updatePlaceableInteraction();
        }
    }

    // todo это наверное все же инвентарь, нежели игрок?
    private static void updatePlaceableInteraction() {
        if (underMouseItem == null && currentObjectType == Items.Types.PLACEABLE && input.clicked(GLFW_MOUSE_BUTTON_LEFT)) {
            if (input.mousePos().x > (Inventory.inventoryOpen ? 1488 : 1866)) {
                if (input.mousePos().y > 756) {
                    return;
                }
            }
            Point2i blockUMB = getBlockUnderMousePoint();

            if (getType(getObject(blockUMB.x, blockUMB.y)) == StaticObjectsConst.Types.GAS && getDistanceToMouse() <= 9) {
                Items item = Inventory.getCurrent();
                int blockX = blockUMB.x;
                int blockY = blockUMB.y;

                if (item != null && item.placeable != 0) {
                    updatePlaceableBlock(item.placeable, blockX, blockY);
                }
            }
        }
    }

    private static void updatePlaceableBlock(short placeable, int blockX, int blockY) {
        if (!placeRules || WorldGenerator.checkPlaceRules(blockX, blockY, placeable)) {
            decrementItem(currentObject.x, currentObject.y);
            setObject(blockX, blockY, placeable, false);
            ShadowMap.update();
        }
    }

    private static void updateToolInteraction() {
        Items item = Inventory.getCurrent();
        if (item != null && item.tool != null) {

            Tools tool = item.tool;
            Point2i blockUMB = getBlockUnderMousePoint();
            int blockX = blockUMB.x;
            int blockY = blockUMB.y;
            short object = getObject(blockX, blockY);

            if (object != 0 && getTexture(object) != null && !StaticObjectsConst.getConst(getId(object)).hasMotherBlock && StaticObjectsConst.getConst(getId(object)).optionalTiles == null) {
                updateNonStructure(blockX, blockY, object, tool);
            } else if (StaticObjectsConst.getConst(getId(object)).hasMotherBlock || StaticObjectsConst.getConst(getId(object)).optionalTiles != null) {
                updateStructure(blockX, blockY, object, tool);
            }
        }
    }

    private static void updateNonStructure(int blockX, int blockY, short object, Tools tool) {
        if (getDistanceToMouse() <= tool.maxInteractionRange && ShadowMap.getDegree(blockX, blockY) == 0) {
            TextureDrawing.addToBlocksQueue(blockX, blockY, object, true);

            if (input.clicked(GLFW_MOUSE_BUTTON_LEFT) && getId(object) != 0 && System.currentTimeMillis() - tool.lastHitTime >= tool.secBetweenHits && getHp(object) > 0) {
                tool.lastHitTime = System.currentTimeMillis();

                if (getHp(decrementHp(object, (int) tool.damage)) <= 0) {
                    createElementPlaceable(object);
                    destroyObject(blockX, blockY);
                } else {
                    setObject(blockX, blockY, decrementHp(object, (int) tool.damage), false);
                }
            }
        } else {
            TextureDrawing.addToBlocksQueue(blockX, blockY, object, false);
        }
    }

    private static void updateStructure(int blockX, int blockY, short object, Tools tool) {
        Point2i root = findRoot(blockX, blockY);

        if (root != null) {
            blockX = root.x;
            blockY = root.y;

            if (getDistanceToMouse() <= tool.maxInteractionRange && ShadowMap.getDegree(blockX, blockY) == 0) {
                TextureDrawing.addToBlocksQueue(blockX, blockY, getObject(root.x, root.y), true);

                if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && getId(object) != 0 && System.currentTimeMillis() - tool.lastHitTime >= tool.secBetweenHits && getHp(object) > 0) {
                    tool.lastHitTime = System.currentTimeMillis();
                    decrementHpMulti(blockX, blockY, (int) tool.damage, root);
                }
            } else {
                TextureDrawing.addToBlocksQueue(blockX, blockY, getObject(root.x, root.y), false);
            }
        }
    }

    // searches for the root of a structure within a radius of 4 blocks
    public static Point2i findRoot(int cellX, int cellY) {
        if (!StaticObjectsConst.getConst(getId(getObject(cellX, cellY))).hasMotherBlock && StaticObjectsConst.getConst(getId(getObject(cellX, cellY))).optionalTiles == null) {
            return null;
        }
        int maxCellsX = 4;
        int maxCellsY = 4;

        for (int blockX = 0; blockX < maxCellsX; blockX++) {
            for (int blockY = 0; blockY < maxCellsY; blockY++) {
                StaticObjectsConst objConst = StaticObjectsConst.getConst(getId(getObject(cellX - blockX, cellY - blockY)));
                if (objConst != null && objConst.optionalTiles != null) {
                    return new Point2i(cellX - blockX, cellY - blockY);
                }
            }
        }
        return null;
    }

    private static void decrementHpMulti(int cellX, int cellY, int hp, Point2i root) {
        if (root != null && getObject(root.x, root.y) != 0) {
            short rootObj = getObject(root.x, root.y);

            for (int x = -(cellX - root.x); x < StaticObjectsConst.getConst(getId(rootObj)).optionalTiles.length - (cellX - root.x); x++) {
                for (int y = -(cellY - root.y); y < StaticObjectsConst.getConst(getId(rootObj)).optionalTiles[0].length - (cellY - root.y); y++) {
                    short object = getObject(x + cellX, y + cellY);

                    if (getHp(decrementHp(object, hp)) <= 0 && getType(object) != StaticObjectsConst.Types.GAS) {
                        createElementPlaceable(rootObj);
                        destroyObject(cellX, cellY);
                        break;
                    } else if (getType(object) != StaticObjectsConst.Types.GAS) {
                        StaticObjects[(x + cellX) + SizeX * (y + cellY)] = decrementHp(object, hp);
                    }
                }
            }
        }
    }

    public static void updatePlayerGUI() {
        if (start) {
            Bullets.drawBullets();
            updateTemperatureEffect();
            BuildMenu.draw();
            updateToolInteraction();
            drawCurrentHP();
            Inventory.update();
        }
    }

    public static void updatePlayerGUILogic() {
        if (start) {
            BuildMenu.updateLogic();
        }
    }

    private static void updateTemperatureEffect() {
        DynamicWorldObjects player = DynamicObjects.getFirst();
        int temp = (int) TemperatureMap.getAverageTempAroundDynamic(player.getX(), player.getY(), player.getTexture());
        int upperLimit = 100;
        int lowestLimit = -20;
        int maxColor = 90;

        int a;
        if (temp > upperLimit) {
            a = Math.min(maxColor, Math.abs((temp - upperLimit) / 3));
        } else if (temp < lowestLimit) {
            a = Math.min(maxColor, Math.abs((temp + lowestLimit) / 3));
        } else {
            a = 0;
        }

        int r = temp > 0 ? a : 0;
        int b = temp > 0 ? 0 : a;

        Texture modifiedTemperature = assets.getTextureByPath(assets.assetsDir("/UI/GUI/modifiedTemperature.png"));

        batch.pushState(() -> {
            batch.z(Layer.EFFECTS);
            batch.color(SimpleColor.fromRGBA(r, (int) (b / 2f), b, a));
            batch.draw(modifiedTemperature);
        });
    }

    public static void playerMaxHP() {
        DynamicObjects.getFirst().setCurrentHp(DynamicObjects.getFirst().getMaxHp());
    }

    public static void playerKill() {
        DynamicObjects.getFirst().setCurrentHp(0);
    }

    private static void drawCurrentHP() {
        int currentHp = (int) DynamicObjects.getFirst().getCurrentHP();
        int maxHp = (int) DynamicObjects.getFirst().getMaxHp();

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
            Fill.rectangleBorder(30, 30, 200, 35, SimpleColor.fromRGBA(10, 10, 10, transparencyHPline));
            Fill.rect(31, 31, currentHp * 2 - 2, 33, SimpleColor.fromRGBA(150, 0, 20, transparencyHPline));

            if (lastDamage > 0) {
                Fill.rect(29 + currentHp * 2, 31, Math.min(lastDamage * 2, 200), 33, SimpleColor.fromRGBA(252, 161, 3, transparencyHPline));
            }
        }
    }
}
