package core.World.Creatures.Player.BuildMenu;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.StaticWorldObjects.Structures.Factories;
import core.Utils.SimpleColor;
import core.World.Textures.TextureDrawing;
import core.assets.AssetsManager;
import core.g2d.Fill;
import core.math.Point2i;
import java.util.Properties;

import static core.Global.*;
import static core.World.Creatures.Player.Inventory.Inventory.*;
import static core.World.Textures.TextureDrawing.*;

public class BuildMenu {
    private static boolean created, isOpen = true, infoCreated;
    private static Items[][] items = new Items[5][30];
    private static Point2i currentObject;
    private static float scroll = 0;

    public static void create() {
        addDefaultItems();
        created = true;
    }

    private static void addDefaultItems() {
        Properties defaultItems = Config.getProperties(assets.assetsDir("World/ItemsCharacteristics/DefaultBuildMenuItems.properties"));

        String[] details = ((String) defaultItems.getOrDefault("Details", "")).split(",");
        String[] tools = ((String) defaultItems.getOrDefault("Tools", "")).split(",");
        String[] weapons = ((String) defaultItems.getOrDefault("Weapons", "")).split(",");
        String[] placeables = ((String) defaultItems.getOrDefault("Placeables", "")).split(",");

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

    public static void updateLogic() {
        if (created) {
            updateBuildButton();
            updateCollapseButton();
            updateInfoButton();
            updateScroll();
        }
    }

    private static void updateBuildButton() {
        if (isOpen && EventHandler.getRectangleClick(1769, 325, 1810, 366)) {
            Point2i[] required = hasRequiredItems();

            if (required != null) {
                for (Point2i obj : required) {
                    if (obj != null) {
                        Inventory.decrementItem(obj.x, obj.y);
                    }
                }
                Items currentItem = items[currentObject.x][currentObject.y];

                switch (items[currentObject.x][currentObject.y].type) {
                    case TOOL -> Inventory.createElementTool(currentItem.filename);
                    case DETAIL -> Inventory.createElementDetail(currentItem.filename);
                    case WEAPON -> Inventory.createElementWeapon(currentItem.filename);
                    case PLACEABLE -> Inventory.createElementPlaceable(currentItem.placeable);
                }
            }
        }
    }

    private static void updateCollapseButton() {
        if (isOpen && EventHandler.getRectangleClick(1832, 325, 1864, 366)) {
            isOpen = false;
        } else if (!isOpen && EventHandler.getRectangleClick(1832, 0, 1864, 40)) {
            isOpen = true;
        }
    }

    private static void updateInfoButton() {
        if (currentObject != null && items[currentObject.x][currentObject.y] != null && isOpen && !infoCreated && EventHandler.getRectangleClick(1877, 325, 1918, 366)) {
            infoCreated = true;
        } else if (infoCreated && EventHandler.getRectangleClick(607, 991, 649, 1032)) {
            infoCreated = false;
        }
    }

    private static void updateScroll() {
        scroll = Math.clamp(-1 * input.getScrollOffset() * 6, -276, 0);
    }

    private static Point2i[] hasRequiredItems() {
        Point2i menuCurrent = currentObject;

        if (menuCurrent != null && items[menuCurrent.x][menuCurrent.y].requiredForBuild != null) {
            Items[] required = items[menuCurrent.x][menuCurrent.y].requiredForBuild;
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

    public static void draw() {
        if (created && isOpen) {
            batch.draw(atlas.byPath("UI/GUI/buildMenu/menuOpen"), 1650, 0);

            for (int x = 0; x < items.length; x++) {
                for (int y = 0; y < items[x].length; y++) {
                    if (items[x][y] != null) {
                        float xCoord = 1660 + x * 54;
                        // float yCoord = 57 + scroll + (items[x][y].type.ordinal() * 20) + y * 54f;
                        float yCoord = 57 + scroll + (y * 54f);

                        if (yCoord < 115 && yCoord > -60) {
                            Inventory.drawInventoryItem(xCoord, yCoord, items[x][y].texture);

                            if (EventHandler.getRectangleClick((int) xCoord, (int) yCoord, (int) (xCoord + 46), (int) (yCoord + 46))) {
                                currentObject = new Point2i(x, y);
                            }
                        }
                    }
                }
            }
            if (currentObject != null && items[currentObject.x][currentObject.y] != null) {
                // float yCoord = 47 + scroll + (items[currentObject.x][currentObject.y].type.ordinal() * 20) + currentObject.y * 54;
                float yCoord = 47 + scroll + (currentObject.y * 54);

                if (yCoord < 105 && yCoord > -60) {
                    batch.draw(atlas.byPath("UI/GUI/inventory/inventoryCurrent.png"), 1650 + currentObject.x * 54, yCoord);
                }
            }
            // scrollbar
            SimpleColor color = SimpleColor.fromRGBA(0, 0, 0, 200);
            Fill.rect(1915, (int) Math.abs(scroll / 2f) - 5, 4, 20, color);

            drawRequirements(1663, 156);

        } else if (!isOpen) {
            batch.draw(atlas.byPath("UI/GUI/buildMenu/menuClosed"), 1650, 0);
        }

        if (infoCreated && currentObject != null && items[currentObject.x][currentObject.y] != null) {
            Fill.rect(0, 0, 1920, 1080, SimpleColor.DIRTY_BLACK);
            Fill.rect(560, 0, 800, 1080, SimpleColor.DIRTY_BLACK);
            batch.draw(atlas.byPath("UI/GUI/buildMenu/exitBtn"), 605, 989);

            TextureDrawing.drawText(650, 730, items[currentObject.x][currentObject.y].description);
            Inventory.drawInventoryItem(650, 915, items[currentObject.x][currentObject.y].texture);

            drawRequirements(650, 760);
        }
    }

    private static void drawRequirements(float x, float y) {
        if (currentObject != null && items[currentObject.x][currentObject.y] != null) {
            Items item = items[currentObject.x][currentObject.y];
            Factories factory = Factories.getFactoryConst(StaticWorldObjects.getFileName(item.placeable));

            drawText((int) x, (int) (y + 130), item.name);

            if (factory != null) {
                if (factory.inputObjects != null) {
                    Factories.drawObjects(x, y + 82, factory.inputObjects, atlas.byPath("UI/GUI/buildMenu/factoryIn.png"));
                }
                if (factory.outputObjects != null) {
                    Factories.drawObjects(x, y + 41, factory.outputObjects, atlas.byPath("UI/GUI/buildMenu/factoryOut.png"));
                }
            }
            if (item.requiredForBuild != null) {
                Factories.drawObjects(x, y, item.requiredForBuild, atlas.byPath("UI/GUI/buildMenu/build.png"));
            }
        }
    }

    // todo categories
    public static void addItem(Items item) {
        for (int x = 0; x < items.length; x++) {
            for (int y = 0; y < items[0].length; y++) {
                if (items[x][y] == null) {
                    items[x][y] = item;
                    return;
                }
            }
        }
    }
}
