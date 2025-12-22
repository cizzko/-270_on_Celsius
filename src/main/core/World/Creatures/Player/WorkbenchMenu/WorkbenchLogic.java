package core.World.Creatures.Player.WorkbenchMenu;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.Global;
import core.World.Creatures.Player.BuildMenu.BuildMenu;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.StaticWorldObjects.StaticBlocksEvents;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.Textures.TextureDrawing;
import core.assets.AssetsManager;
import core.g2d.Fill;
import core.math.Point2i;
import core.util.Color;

import java.util.HashSet;

import static core.Global.*;
import static core.World.StaticWorldObjects.StaticWorldObjects.getFileName;
import static core.World.StaticWorldObjects.StaticWorldObjects.getType;
import static core.World.WorldGenerator.WorldGenerator.DynamicObjects;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class WorkbenchLogic implements StaticBlocksEvents {
    private static boolean isOpen, isSmall = true, smallNearby, mediumNearby, largeNearby;
    private static Point2i currentObject;
    private static Items[][] smallWorkbenchItems = new Items[10][20], mediumWorkbenchItems, largeWorkbenchItems;
    //private static float scroll = -276;
    private static final Color fogging = new Color(0, 0, 0, 117);
    private static int current = 0;
    private static final HashSet<Point2i> workbenchs = new HashSet<>();

    public static void create() {
        var defaultItems = Config.getProperties(("World/ItemsCharacteristics/DefaultBuildMenuItems.properties"));

        String[] details = defaultItems.getOrDefault("Details", "").split(",");
        String[] tools = defaultItems.getOrDefault("Tools", "").split(",");
        String[] weapons = defaultItems.getOrDefault("Weapons", "").split(",");
        String[] placeables = defaultItems.getOrDefault("Placeables", "").split(",");

        if (details[0].length() > 1) {
            for (String detail : details) {
                addItem(Items.createItem(AssetsManager.normalizePath(detail)));
            }
        }
        if (tools[0].length() > 1) {
            for (String tool : tools) {
                addItem(Items.createItem(AssetsManager.normalizePath(tool)));
            }
        }
        if (weapons[0].length() > 1) {
            for (String weapon : weapons) {
                addItem(Items.createItem(AssetsManager.normalizePath(weapon)));
            }
        }
        if (placeables[0].length() > 1) {
            for (String placeable : placeables) {
                addItem(Items.createItem(StaticWorldObjects.createStatic(AssetsManager.normalizePath(placeable))));
            }
        }
    }

    private static void addItem(Items item) {
        for (int y = 0; y < smallWorkbenchItems[0].length; y++) {
            for (int x = 0; x < smallWorkbenchItems.length; x++) {
                if (smallWorkbenchItems[x][y] == null) {
                    smallWorkbenchItems[x][y] = item;
                    return;
                }
            }
        }
    }

    private static void updateBenchButton() {
        if (EventHandler.getRectangleClick(698, 580, 744, 626)) {
            current = 0;
        }
        if (EventHandler.getRectangleClick(698, 634, 744, 682) && largeNearby) {
            current = 1;
        }
        if (EventHandler.getRectangleClick(698, 688, 744, 734) && mediumNearby) {
            current = 2;
        }
        if (EventHandler.getRectangleClick(698, 742, 744, 788) && smallNearby) {
            current = 3;
        }
    }

    private static void updateNearby() {
        workbenchs.forEach(point -> {

            //todo что то придумать, проверять по имени файла это кпец
            if (Math.abs(DynamicObjects.getFirst().getX() / TextureDrawing.blockSize - point.x) < 16) {
                String name = getFileName(world.get(point.x, point.y)).toLowerCase();

                if (name.contains("small")) {
                    smallNearby = true;
                }
                if (name.contains("medium")) {
                    mediumNearby = true;
                }
                if (name.contains("large")) {
                    largeNearby = true;
                }
            } else {
                smallNearby = false;
                mediumNearby = false;
                largeNearby = false;
                current = 0;
            }
        });
    }

    private static void updateOpen() {
        Point2i blockUMB = Global.input.mouseBlockPos();
        Point2i mousePos = input.mousePos();

        if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT)) {
            if ((isSmall && (mousePos.x < 690 || mousePos.x > 1270)) || (!isSmall && (mousePos.x < 690 || mousePos.x > 1407)) || mousePos.y < 400 || mousePos.y > 796) {
                isOpen = false;
            }
            if (getFileName(world.get(blockUMB.x, blockUMB.y)) != null && getFileName(world.get(blockUMB.x, blockUMB.y)).toLowerCase().contains("workbench")) {
                isOpen = true;
            }
        }
    }

    public static void draw() {
        updateOpen();
        updateBenchButton();
        updateNearby();
        if (isOpen) {
            //todo я знаю что оно криво но не могу доказать вамент выровняй пожалуйста
            batch.draw(atlas.byPath("UI/GUI/workbenchMenu/menuSmall"), 690, 400);
            Fill.rect(693, 587 + (54 * current), 3, 32, Color.fromRgba8888(255, 80, 0, 200));

            if (!smallNearby) {
                Fill.rect(698, 742, 46, 46, fogging);
            }
            if (!mediumNearby) {
                Fill.rect(698, 688, 46, 46, fogging);
            }
            if (!largeNearby) {
                Fill.rect(698, 634, 46, 46, fogging);
            }
            
            for (int x = 0; x < smallWorkbenchItems.length; x++) {
                for (int y = 0; y < smallWorkbenchItems[x].length; y++) {
                    if (smallWorkbenchItems[x][y] != null) {
                        float xCoord = 760 + x * 54;
                        //float yCoord = 57 + scroll + (smallWorkbenchItems[x][y].type.ordinal() * 20) + y * 54f;
                        float yCoord = 1000 + BuildMenu.scroll + (y * 54f);

                        if (yCoord < 755) {
                            Inventory.drawInventoryItem(xCoord, yCoord, smallWorkbenchItems[x][y].texture);

                            if (EventHandler.getRectangleClick((int) xCoord, (int) yCoord, (int) (xCoord + 46), (int) (yCoord + 46))) {
                                currentObject = new Point2i(x, y);
                            }
                        }
                    }
                }
            }
            if (currentObject != null && smallWorkbenchItems[currentObject.x][currentObject.y] != null) {
                // float yCoord = 47 + scroll + (smallWorkbenchItems[currentObject.x][currentObject.y].type.ordinal() * 20) + currentObject.y * 54;
                float yCoord = 47 + BuildMenu.scroll + (currentObject.y * 54);

                if (yCoord < 105 && yCoord > -60) {
                    batch.draw(atlas.byPath("UI/GUI/inventory/inventoryCurrent.png"), 1650 + currentObject.x * 54, yCoord);
                }
            }
            // scrollbar
            Color color = Color.fromRgba8888(0, 0, 0, 200); // TODO убрать аллокацию
            //Fill.rect(1915, (int) Math.abs(scroll / 2f) - 5, 4, 20, color);
        }
    }

    @Override
    public void placeStatic(int cellX, int cellY, short id) {
        //todo что то придумать, проверять по имени файла это кпец
        if (getFileName(id).toLowerCase().contains("workbench")) {
            workbenchs.add(new Point2i(cellX, cellY));
        }
    }

    @Override
    public void destroyStatic(int cellX, int cellY, short id) {

    }
}
