package core.World.Creatures.Player.BuildMenu;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.EventHandling.MouseScrollCallback;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.StaticWorldObjects.Structures.Factories;
import core.Utils.SimpleColor;
import core.World.Textures.TextureDrawing;
import java.awt.Point;
import java.util.Properties;
import static core.Window.assetsDir;
import static core.World.Creatures.Player.Inventory.Inventory.*;
import static core.World.Textures.TextureDrawing.*;

public class BuildMenu {
    private static boolean create, isOpen = true, infoCreated;
    private static Items[][] items = new Items[5][30];
    private static Point currentObject;
    private static float scroll = 0;

    public static void create() {
        addDefaultItems();
        create = true;
    }

    private static void addDefaultItems() {
        Properties defaultItems = Config.getProperties(assetsDir("\\World\\ItemsCharacteristics\\BuildMenu\\DefaultBuildMenuItems.properties"));

        //todo выглядит странно
        String[] details = ((String) defaultItems.getOrDefault("Details", "")).split(",");
        String[] factories = ((String) defaultItems.getOrDefault("Factories", "")).split(",");
        String[] tools = ((String) defaultItems.getOrDefault("Tools", "")).split(",");
        String[] weapons = ((String) defaultItems.getOrDefault("Weapons", "")).split(",");
        String[] placeables = ((String) defaultItems.getOrDefault("Placeables", "")).split(",");

        if (details.length > 1) {
            for (String detail : details) {
                createElementDetail(detail);
            }
        }
        if (factories.length > 1) {
            for (String factory : factories) {
                createElementPlaceable((short) 0);
            }
        }
        if (tools.length > 1) {
            for (String tool : tools) {
                createElementTool(tool);
            }
        }
        if (weapons.length > 1) {
            for (String weapon : weapons) {
                createElementWeapon(weapon);
            }
        }
        if (placeables.length > 1) {
            for (String placeable : placeables) {
                createElementPlaceable(StaticWorldObjects.createStatic("Blocks/" + placeable));
            }
        }
    }

    public static void updateLogic() {
        if (create) {
            updateBuildButton();
            updateCollapseButton();
            updateInfoButton();
            updateScroll();
        }
    }

    private static void updateBuildButton() {
        //todo press -> click
        if (isOpen && EventHandler.getRectanglePress(1769, 325, 1810, 366)) {
            Point[] required = hasRequiredItems();

            if (required != null) {
                for (Point obj : required) {
                    if (obj != null) {
                        Inventory.decrementItem(obj.x, obj.y);
                    }
                }
                //todo тоже выглядит стремно
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
        if (isOpen && EventHandler.getRectanglePress(1832, 325, 1864, 366)) {
            isOpen = false;
        } else if (!isOpen && EventHandler.getRectanglePress(1832, 0, 1864, 40)) {
            isOpen = true;
        }
    }

    private static void updateInfoButton() {
        if (currentObject != null && items[currentObject.x][currentObject.y] != null && isOpen && !infoCreated && EventHandler.getRectanglePress(1877, 325, 1918, 366)) {
            infoCreated = true;
        } else if (infoCreated && EventHandler.getRectanglePress(607, 991, 649, 1032)) {
            infoCreated = false;
        }
    }

    private static void updateScroll() {
        double scrollM = MouseScrollCallback.getScroll() * 6;

        if (scrollM >= -276 && scrollM <= 0) {
            scroll = (float) scrollM;
        }
        else if (scroll < -276) {
            scroll = -276;
        } else if (scroll > 0) {
            scroll = -0f;
        }
    }

    private static Point[] hasRequiredItems() {
        Point menuCurrent = currentObject;

        if (menuCurrent != null && items[menuCurrent.x][menuCurrent.y].requiredForBuild != null) {
            Items[] required = items[menuCurrent.x][menuCurrent.y].requiredForBuild;
            Point[] hasNeededObject = new Point[required.length];

            for (int i = 0; i < required.length; i++) {
                for (int x = 0; x < inventoryObjects.length; x++) {
                    for (int y = 0; y < inventoryObjects[x].length; y++) {
                        if (inventoryObjects[x][y] != null && inventoryObjects[x][y].id == required[i].id) {
                            hasNeededObject[i] = new Point(x, y);
                        }
                    }
                }
            }
            return hasNeededObject[hasNeededObject.length - 1] == null ? null : hasNeededObject;
        }
        return null;
    }

    public static void draw() {
        if (create && isOpen) {
            drawTexture(1650, 0, true, assetsDir("UI/GUI/buildMenu/menuOpen.png"));

            for (int x = 0; x < items.length; x++) {
                for (int y = 0; y < items[x].length; y++) {
                    if (items[x][y] != null) {
                        float xCoord = 1660 + x * 54;
                        float yCoord = 57 + scroll + (items[x][y].type.ordinal() * 20) + y * 54f;

                        if (yCoord < 115 && yCoord > -60) {
                            Inventory.drawInventoryItem(xCoord, yCoord, items[x][y].path);

                            if (EventHandler.getRectanglePress((int) xCoord, (int) yCoord, (int) (xCoord + 46), (int) (yCoord + 46))) {
                                currentObject = new Point(x, y);
                            }
                        }
                    }
                }
            }
            if (currentObject != null && items[currentObject.x][currentObject.y] != null) {
                float yCoord = 47 + scroll + (items[currentObject.x][currentObject.y].type.ordinal() * 20) + currentObject.y * 54;

                if (yCoord < 105 && yCoord > -60) {
                    drawTexture(1650 + currentObject.x * 54, yCoord, true, assetsDir("UI/GUI/inventory/inventoryCurrent.png"));
                }
            }
            drawRectangle(1915, (int) Math.abs(scroll / 2f) - 5, 4, 20, new SimpleColor(0, 0, 0, 200));

            drawRequirements(1663, 156);

        } else if (!isOpen) {
            drawTexture(1650, 0, true, assetsDir("UI/GUI/buildMenu/menuClosed.png"));
        }

        if (infoCreated && currentObject != null && items[currentObject.x][currentObject.y] != null) {
            TextureDrawing.drawRectangle(0, 0, 1920, 1080, new SimpleColor(0, 0, 0, 50));
            TextureDrawing.drawRectangle(560, 0, 800, 1080, new SimpleColor(0, 0, 0, 50));
            TextureDrawing.drawTexture(605, 989, true, assetsDir("UI/GUI/buildMenu/exitBtn.png"));
            TextureDrawing.drawText(694, 730, items[currentObject.x][currentObject.y].description);
            Inventory.drawInventoryItem(694, 915, items[currentObject.x][currentObject.y].path);

            drawRequirements(694, 760);
        }
    }

    private static void drawRequirements(float x, float y) {
        if (currentObject != null && items[currentObject.x][currentObject.y] != null) {
            Items item = items[currentObject.x][currentObject.y];
            Factories factory = Factories.getFactoryConst(StaticWorldObjects.getFileName(item.placeable));

            drawText((int) x, (int) (y + 130), item.name);

            if (factory != null) {
                if (factory.inputObjects != null) {
                    Factories.drawObjects(x, y + 82, factory.inputObjects, assetsDir("UI/GUI/buildMenu/factoryIn.png"));
                }
                if (factory.outputObjects != null) {
                    Factories.drawObjects(x, y + 41, factory.outputObjects, assetsDir("UI/GUI/buildMenu/factoryOut.png"));
                }
            }
            if (item.requiredForBuild != null) {
                Factories.drawObjects(x, y, item.requiredForBuild, assetsDir("UI/GUI/buildMenu/build.png"));
            }
        }
    }

    //todo categories
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
