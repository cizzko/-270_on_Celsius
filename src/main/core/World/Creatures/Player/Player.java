package core.World.Creatures.Player;

import core.EventHandling.Logging.Config;
import core.Global;
import core.World.Creatures.DynamicWorldObjects;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.ItemData;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.ItemBlock;
import core.World.ItemTool;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.Textures.ShadowMap;
import core.World.Textures.TextureDrawing;
import core.g2d.Fill;
import core.math.Point2i;
import core.util.Color;

import static core.Global.input;
import static core.Global.world;
import static core.World.Creatures.Player.Inventory.Inventory.*;
import static core.World.WorldGenerator.WorldGenerator.DynamicObjects;
import static core.World.WorldUtils.getDistanceToMouse;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class Player {
    public static Thread currentInteraction;
    public static boolean noClip = false, placeRules = true;
    private static int transparencyHPline = Config.getBoolean("AlwaysOnPlayerHPLine") ? 220 : 0;
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
    //todo хотелось бы иметь рисование и взаимодействие поближе
    private static void updatePlaceableInteraction() {
        ItemStack item = Inventory.getCurrent();

        if (input.clicked(GLFW_MOUSE_BUTTON_LEFT) && underMouseItem == null && item != null && item.getItem() instanceof ItemBlock itemBlock) {
            if (input.mousePos().x > (Inventory.inventoryOpen ? 1487 : 1866)) {
                if (input.mousePos().y > 756) {
                    return;
                }
            }
            Point2i blockUMB = Global.input.mouseBlockPos();

            var block = world.getBlock(blockUMB.x, blockUMB.y);
            if (block != null && block.type == StaticObjectsConst.Type.GAS && getDistanceToMouse() <= 9) {
                updatePlaceableBlock(itemBlock.block, blockUMB.x, blockUMB.y);
            }
        }
    }

    private static void updatePlaceableBlock(StaticObjectsConst placeable, int blockX, int blockY) {
        if (!placeRules || world.checkPlaceRules(blockX, blockY, placeable)) {
            decrementItem(currentObject.x, currentObject.y);
            world.set(blockX, blockY, placeable, false);
            ShadowMap.update();
        }
    }

    public static void updateToolInteraction() {
        ItemStack item = Inventory.getCurrent();
        if (item != null && item.getItem() instanceof ItemTool tool) {
            // TODO: Проверка на тип даты
            var data = item.getOrCreateData(ItemData.Tool::new);

            Point2i blockUMB = Global.input.mouseBlockPos();
            int blockX = blockUMB.x;
            int blockY = blockUMB.y;
            var object = world.getBlock(blockX, blockY);
            if (object == null || object == StaticObjectsConst.AIR) {
                return;
            }

            if (object.isMultiblock()) {
                updateStructure(blockX, blockY, object, tool, data);
            } else {
                updateNonStructure(blockX, blockY, object, tool, data);
            }
        }
    }

    //тулы
    private static void updateNonStructure(int blockX, int blockY, StaticObjectsConst object, ItemTool tool, ItemData.Tool data) {
        int blockId = world.getBlockId(blockX, blockY);
        int hp = world.getHp(blockX, blockY);

        if (getDistanceToMouse() <= tool.maxInteractionRange && ShadowMap.getDegree(blockX, blockY) == 0) {
            TextureDrawing.addBlockPreview(blockX, blockY, (short) blockId, (byte) hp, true);

            long nowTime = System.currentTimeMillis();
            if (input.clicked(GLFW_MOUSE_BUTTON_LEFT) && nowTime - data.lastHitTime >= tool.secBetweenHits && hp > 0) {
                data.lastHitTime = nowTime;

                if (Global.world.damage(blockX, blockY, tool.damage)) {
                    //todo сделать лежачие предметы (подойти подобрать). Тут ещё выпадание предметов должно быть
                    Inventory.addItem(Global.content.itemById(object));
                }
            }
        } else {
            TextureDrawing.addBlockPreview(blockX, blockY, (short) blockId, (byte) hp, false);
        }
    }

    private static void updateStructure(int blockX, int blockY, StaticObjectsConst object, ItemTool tool, ItemData.Tool data) {
        Point2i root = world.getRootBlockPos(blockX, blockY);

        assert root != null;

        blockX = root.x;
        blockY = root.y;

        updateNonStructure(blockX, blockY, object, tool, data);
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

        long nowTime = System.currentTimeMillis();
        if (currentHp == maxHp && transparencyHPline > 0 && nowTime - lastChangeTransparency >= 10 && !Config.getBoolean("AlwaysOnPlayerHPLine")) {
            lastChangeTransparency = nowTime;
            transparencyHPline--;
        } else if (currentHp != maxHp) {
            transparencyHPline = 220;
        }
        if (lastDamage > 0 && nowTime - lastChangeLengthDamage >= 15 && nowTime - lastDamageTime >= 300) {
            lastChangeLengthDamage = nowTime;
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
