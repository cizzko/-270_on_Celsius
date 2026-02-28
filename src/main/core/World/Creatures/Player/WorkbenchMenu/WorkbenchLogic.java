package core.World.Creatures.Player.WorkbenchMenu;

import core.EventHandling.EventHandler;
import core.Global;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.StaticWorldObjects.StaticBlocksEvents;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.StaticWorldObjects.Structures.Factories;
import core.World.Textures.TextureDrawing;
import core.g2d.Fill;
import core.math.Point2i;
import core.util.Color;

import java.awt.*;
import java.util.HashSet;

import static core.Global.*;
import static core.World.Creatures.Player.Inventory.Inventory.inventoryObjects;
import static core.World.Creatures.Player.ItemControl.*;
import static core.World.StaticWorldObjects.StaticWorldObjects.getFileName;
import static core.World.Textures.TextureDrawing.drawObjects;
import static core.World.WorldGenerator.WorldGenerator.DynamicObjects;
import static org.lwjgl.glfw.GLFW.*;

public class WorkbenchLogic implements StaticBlocksEvents {
    private static boolean isOpen, smallNearby, mediumNearby, largeNearby;
    private static Point2i currentObject;
    private static float scroll = -276;
    private static final Color fogging = new Color(0, 0, 0, 117);
    //todo не помешает перевести меню в гуи
    private static int current = 0, menuXPos = 680;
    private static final HashSet<Point2i> workbenchs = new HashSet<>();

    public static void updateInput() {
        updateBenchButton();
        updateNearby();
        updateOpen();
        updateScroll();
        updateBuildButton();
    }

    private static void updateBenchButton() {
        if (EventHandler.getRectangleClick(menuXPos + 8, 580, menuXPos + 54, 626)) {
            //todo некрасиво
            currentObject = null;
            current = 0;
        }
        if (EventHandler.getRectangleClick(menuXPos + 8, 634, menuXPos + 54, 682) && largeNearby) {
            currentObject = null;
            current = 1;
        }
        if (EventHandler.getRectangleClick(menuXPos + 8, 688, menuXPos + 54, 734) && mediumNearby) {
            currentObject = null;
            current = 2;
        }
        if (EventHandler.getRectangleClick(menuXPos + 8, 742, menuXPos + 54, 788) && smallNearby) {
            currentObject = null;
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

        //todo сделать проверку на нахождение мыши на элте интерфейса
//        if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && getFileName(world.get(blockUMB.x, blockUMB.y)) != null && getFileName(world.get(blockUMB.x, blockUMB.y)).toLowerCase().contains("workbench")) {
//            isOpen = true;
//        }
        if (EventHandler.getRectangleClick(1768, 0, 1810, 42) || input.justPressed(GLFW_KEY_B)) {
            isOpen = !isOpen;
        }
    }

    private static void updateScroll() {
        int x = input.mousePos().x;
        int y = input.mousePos().y;

        if (x > menuXPos && x < menuXPos + 580 && y > 400 && y < 796) {
            scroll = Math.clamp(scroll - input.getScrollChange() * 6, -246, 0);
        }
    }

    public static void draw() {
        batch.draw(atlas.byPath("UI/GUI/buildMenu/menuClosed"), 1650, 0);

        if (isOpen) {
            batch.draw(atlas.byPath("UI/GUI/workbenchMenu/menu" + (currentObject == null ? "Small" : "Full")), menuXPos, 400);
            Fill.rect(menuXPos + 3, 587 + (54 * current), 3, 32, Color.fromRgba8888(255, 80, 0, 200));

            if (!smallNearby) {
                Fill.rect(menuXPos + 8, 742, 46, 46, fogging);
            }
            if (!mediumNearby) {
                Fill.rect(menuXPos + 8, 688, 46, 46, fogging);
            }
            if (!largeNearby) {
                Fill.rect(menuXPos + 8, 634, 46, 46, fogging);
            }

            Items[][] currentWorkbench = getCurrentItems();

            for (int x = 0; x < currentWorkbench.length; x++) {
                for (int y = 0; y < currentWorkbench[x].length; y++) {
                    if (currentWorkbench[x][y] != null) {
                        float xCoord = menuXPos + 70 + x * 54;
                        //float yCoord = 57 + scroll + (smallWorkbenchItems[x][y].type.ordinal() * 20) + y * 54f;
                        float yCoord = 1000 + scroll + (y * 54f);

                        if (yCoord < 755) {
                            Inventory.drawInventoryItem(xCoord, yCoord, currentWorkbench[x][y].texture);

                            if (EventHandler.getRectangleClick((int) xCoord, (int) yCoord, (int) (xCoord + 46), (int) (yCoord + 46))) {
                                currentObject = new Point2i(x, y);
                            }
                        }
                    }
                }
            }

            //todo описания
            if (currentObject != null && currentWorkbench[currentObject.x][currentObject.y] != null) {
                TextureDrawing.drawText(menuXPos + 585, 703, currentWorkbench[currentObject.x][currentObject.y].name);
                drawRequirements(menuXPos + 590,  648);
                batch.draw(atlas.byPath("UI/GUI/inventory/inventoryCurrent.png"), menuXPos + 62 + currentObject.x * 54,  986 + scroll + (currentObject.y * 54));
            }

            // scrollbar
            //Color color = Color.fromRgba8888(0, 0, 0, 200);
            //Fill.rect(1915, (int) Math.abs(scroll / 2f) - 5, 4, 20, color);
        }
    }

    private static Items[][] getCurrentItems() {
        return switch (current) {
            //todo звездочки
            case 1 -> largeWorkbenchItems;
            case 2 -> mediumWorkbenchItems;
            case 3 -> smallWorkbenchItems;
            default -> buildMenuItems;
        };
    }

    @Override
    public void placeStatic(int cellX, int cellY, short id) {
        //todo что то придумать, проверять по имени файла это кпец
        if (id != 0 && id != -1 && StaticWorldObjects.getTexture(id) != null && getFileName(id).toLowerCase().contains("workbench")) {
            workbenchs.add(new Point2i(cellX, cellY));
        }
    }

    @Override
    public void destroyStatic(int cellX, int cellY, short id) {

    }

    private static void updateBuildButton() {
        if (isOpen && EventHandler.getRectangleClick(menuXPos + 580, 742, menuXPos + 625, 788)) {
            Point2i[] required = hasRequiredItems();
            Items[][] currentItems = getCurrentItems();

            if (required != null) {
                for (Point2i obj : required) {
                    if (obj != null) {
                        Inventory.decrementItem(obj.x, obj.y);
                    }
                }
                Items currentItem = currentItems[currentObject.x][currentObject.y];

                switch (currentItem.type) {
                    case TOOL, DETAIL, WEAPON -> Inventory.createElement(currentItem.filename);
                    case PLACEABLE -> Inventory.createElementPlaceable(currentItem.placeable);
                }
            }
        }
    }

    private static Point2i[] hasRequiredItems() {
        Point2i menuCurrent = currentObject;
        Items[][] currentItems = getCurrentItems();

        if (menuCurrent != null && currentItems[menuCurrent.x][menuCurrent.y].requiredForBuild != null) {
            Items[] required = currentItems[menuCurrent.x][menuCurrent.y].requiredForBuild;
            Point2i[] hasNeededObject = new Point2i[required.length];
            int neededCounter = 0;

            for (int i = 0; i < required.length; i++) {
                for (int x = 0; x < inventoryObjects.length; x++) {
                    for (int y = 0; y < inventoryObjects[x].length; y++) {
                        if (inventoryObjects[x][y] != null && inventoryObjects[x][y].id == required[i].id) {
                            hasNeededObject[i] = new Point2i(x, y);
                            neededCounter++;
                        }
                    }
                }
            }
            return neededCounter == hasNeededObject.length ? hasNeededObject : null;
        }
        return null;
    }

    private static void drawRequirements(float x, float y) {
        Items[][] currentItems = getCurrentItems();

        if (currentObject != null && currentItems[currentObject.x][currentObject.y] != null) {
            Items item = currentItems[currentObject.x][currentObject.y];
            Factories factory = Factories.getFactoryConst(StaticWorldObjects.getFileName(item.placeable));

            if (factory != null) {
                if (factory.inputObjects != null) {
                    drawObjects(x, y - 41, factory.inputObjects, atlas.byPath("UI/GUI/buildMenu/factoryIn.png"));
                }
                if (factory.outputObjects != null) {
                    drawObjects(x, y - 82, factory.outputObjects, atlas.byPath("UI/GUI/buildMenu/factoryOut.png"));
                }
            }
            if (item.requiredForBuild != null) {
                drawObjects(x, y, item.requiredForBuild, atlas.byPath("UI/GUI/buildMenu/build.png"));
            }
        }
    }
}
