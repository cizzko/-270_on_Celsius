package core.World.Creatures.Player;

import core.EventHandling.Logging.Config;
import core.Global;
import core.util.Color;
import core.World.Creatures.DynamicWorldObjects;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.Creatures.Player.Inventory.Items.Tools;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.Textures.ShadowMap;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator;
import core.g2d.Fill;
import core.math.Point2i;
import core.math.Rectangle;

import static core.Global.*;
import static core.World.Creatures.Player.Inventory.Inventory.*;
import static core.World.StaticWorldObjects.StaticWorldObjects.*;
import static core.World.WorldGenerator.*;
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
        DynamicObjects.addFirst(DynamicWorldObjects.createDynamic("player", randomSpawn ? (int) (Math.random() * (world.sizeX * TextureDrawing.blockSize)) : world.sizeX * 8f));
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
            Point2i blockUMB = Global.input.mouseBlockPos();

            if (getType(world.get(blockUMB.x, blockUMB.y)) == StaticObjectsConst.Types.GAS && getDistanceToMouse() <= 9) {
                Items item = Inventory.getCurrent();
                if (item != null && item.placeable != 0) {
                    updatePlaceableBlock(item.placeable, blockUMB.x, blockUMB.y);
                }
            }
        }
    }

    private static void updatePlaceableBlock(short placeable, int blockX, int blockY) {
        if (!placeRules || WorldGenerator.checkPlaceRules(blockX, blockY, placeable)) {
            decrementItem(currentObject.x, currentObject.y);
            world.set(blockX, blockY, placeable, false);
            ShadowMap.update();
        }
    }

    public static void updateToolInteraction() {
        Items item = Inventory.getCurrent();
        if (item != null && item.tool != null) {

            Tools tool = item.tool;
            Point2i blockUMB = Global.input.mouseBlockPos();
            int blockX = blockUMB.x;
            int blockY = blockUMB.y;
            short object = world.get(blockX, blockY);

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
                    world.destroy(blockX, blockY);
                } else {
                    world.set(blockX, blockY, decrementHp(object, (int) tool.damage), false);
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
                TextureDrawing.addToBlocksQueue(blockX, blockY, world.get(root.x, root.y), true);

                if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && getId(object) != 0 && System.currentTimeMillis() - tool.lastHitTime >= tool.secBetweenHits && getHp(object) > 0) {
                    tool.lastHitTime = System.currentTimeMillis();
                    decrementHpMulti(blockX, blockY, (int) tool.damage, root);
                }
            } else {
                TextureDrawing.addToBlocksQueue(blockX, blockY, world.get(root.x, root.y), false);
            }
        }
    }

    // searches for the root of a structure within a radius of 4 blocks
    public static Point2i findRoot(int cellX, int cellY) {
        if (!world.inBounds(cellX, cellY)) {
            return null;
        }

        if (!StaticObjectsConst.getConst(getId(world.get(cellX, cellY))).hasMotherBlock) {
            if (StaticObjectsConst.getConst(getId(world.get(cellX, cellY))).optionalTiles == null) {
                return null;
            }
        }
        int maxCellsX = 4;
        int maxCellsY = 4;

        for (int blockX = 0; blockX < maxCellsX; blockX++) {
            for (int blockY = 0; blockY < maxCellsY; blockY++) {
                StaticObjectsConst objConst = StaticObjectsConst.getConst(getId(world.get(cellX - blockX, cellY - blockY)));
                if (objConst != null && objConst.optionalTiles != null) {
                    return new Point2i(cellX - blockX, cellY - blockY);
                }
            }
        }
        return null;
    }

    private static void decrementHpMulti(int cellX, int cellY, int hp, Point2i root) {
        if (root != null) {
            if (world.get(root.x, root.y) != 0) {
                short rootObj = world.get(root.x, root.y);

                for (int x = -(cellX - root.x); x < StaticObjectsConst.getConst(getId(rootObj)).optionalTiles.length - (cellX - root.x); x++) {
                    for (int y = -(cellY - root.y); y < StaticObjectsConst.getConst(getId(rootObj)).optionalTiles[0].length - (cellY - root.y); y++) {
                        short object = world.get(x + cellX, y + cellY);

                        if (getHp(decrementHp(object, hp)) <= 0 && getType(object) != StaticObjectsConst.Types.GAS) {
                            createElementPlaceable(rootObj);
                            world.destroy(cellX, cellY);
                            break;
                        } else if (getType(object) != StaticObjectsConst.Types.GAS) {
                            world.tiles[(x + cellX) + world.sizeX * (y + cellY)] = decrementHp(object, hp);
                        }
                    }
                }
            }
        }
    }

    public static void drawBuildGrid() {
        if (!Config.getFromConfig("BuildGrid").equalsIgnoreCase("true")) {
            return;
        }

        Point2i current = currentObject;
        if (current != null) {
            short placeable = inventoryObjects[current.x][current.y].placeable;
            var mousePos = input.mousePos();
            if (placeable != 0 && underMouseItem == null && !Rectangle.contains(1488, 756, 500, 500, mousePos)) {
                batch.draw(atlas.byPath("World/buildGrid.png"),
                        Color.rgba8888(230, 230, 230, 150),
                        mousePos.x - 243f, mousePos.y - 244f);
            }
        }
    }

    public static void playerMaxHP() {
        DynamicObjects.getFirst().setCurrentHp(DynamicObjects.getFirst().getMaxHp());
    }

    public static void playerKill() {
        DynamicObjects.getFirst().setCurrentHp(0);
    }

    public static void drawCurrentHP() {
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
            Fill.rectangleBorder(30, 30, 200, 35, Color.fromRgba8888(10, 10, 10, transparencyHPline));
            Fill.rect(31, 31, currentHp * 2 - 2, 33, Color.fromRgba8888(150, 0, 20, transparencyHPline));

            if (lastDamage > 0) {
                Fill.rect(29 + currentHp * 2, 31, Math.min(lastDamage * 2, 200), 33, Color.fromRgba8888(252, 161, 3, transparencyHPline));
            }
        }
    }
}
