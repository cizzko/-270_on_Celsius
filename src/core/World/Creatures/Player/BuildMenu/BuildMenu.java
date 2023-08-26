package core.World.Creatures.Player.BuildMenu;

import core.EventHandling.EventHandler;
import core.EventHandling.MouseScrollCallback;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.Creatures.Player.Inventory.Items.Placeable.Factories;
import core.World.Textures.TextureDrawing;
import java.awt.*;
import static core.Window.defPath;
import static core.World.Creatures.Player.Inventory.Inventory.inventoryObjects;
import static core.World.Textures.TextureDrawing.*;

public class BuildMenu {
    public static boolean create = false, isOpen = true, infoCreated;
    public static BuildItems[][] items = new BuildItems[5][30];
    public static Point currentObject;
    private static float scroll = 0;

    public static void create() {
        create = true;
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
        Point[] required = hasRequiredItems();

        if (isOpen && EventHandler.getRectanglePress(1769, 325, 1810, 366) && required != null) {
            for (Point obj : required) {
                if (obj != null) {
                    Inventory.decrementItem(obj.x, obj.y);
                }
            }
            Items currentItem = items[currentObject.x][currentObject.y].item;

            switch (items[currentObject.x][currentObject.y].item.type) {
                case TOOL -> Inventory.createElementTool(currentItem.tool, currentItem.id, currentItem.path, currentItem.description);
                case DETAIL -> Inventory.createElementDetail(currentItem.detail, currentItem.id, currentItem.description);
                case WEAPON -> Inventory.createElementWeapon(currentItem.weapon, currentItem.id, currentItem.path, currentItem.description);
                case PLACEABLE_BLOCK -> Inventory.createElementPlaceable(currentItem.placeable.staticWorldObject, currentItem.description);
                case PLACEABLE_FACTORY -> Inventory.createElementFactory(currentItem.placeable.factoryObject, currentItem.description);
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
        Point inventoryCurrent = currentObject;

        if (inventoryCurrent != null && items[inventoryCurrent.x][inventoryCurrent.y].requiredForBuild != null) {
            Items[] required = items[inventoryCurrent.x][inventoryCurrent.y].requiredForBuild;
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
            return hasNeededObject[0] == null ? null : hasNeededObject;
        }
        return null;
    }

    public static void draw() {
        if (create && isOpen) {
            drawTexture(defPath + "\\src\\assets\\UI\\GUI\\buildMenu\\menuOpen.png", 1650, 0, 1, true);

            for (int x = 0; x < items.length; x++) {
                for (int y = 0; y < items[x].length; y++) {
                    if (items[x][y] != null) {
                        float xCoord = 1660 + x * 54;
                        float yCoord = 57 + scroll + (items[x][y].item.type.ordinal() * 20) + y * 54f;

                        if (yCoord < 115 && yCoord > -60) {
                            Inventory.drawInventoryItem(xCoord, yCoord, items[x][y].item.path);

                            if (EventHandler.getRectanglePress((int) xCoord, (int) yCoord, (int) (xCoord + 46), (int) (yCoord + 46))) {
                                currentObject = new Point(x, y);
                            }
                        }
                    }
                }
            }
            if (currentObject != null && items[currentObject.x][currentObject.y] != null) {
                float yCoord = 47 + scroll + (items[currentObject.x][currentObject.y].item.type.ordinal() * 20) + currentObject.y * 54;

                if (yCoord < 105 && yCoord > -60) {
                    drawTexture(defPath + "\\src\\assets\\UI\\GUI\\inventory\\inventoryCurrent.png", 1650 + currentObject.x * 54, yCoord, 1, true);
                }
            }
            drawRectangle(1915, (int) Math.abs(scroll / 2f) - 5, 4, 20, new Color(0, 0, 0, 200));

            drawRequirements(1663, 156);

        } else if (!isOpen) {
            drawTexture(defPath + "\\src\\assets\\UI\\GUI\\buildMenu\\menuClosed.png", 1650, 0, 1, true);
        }

        if (infoCreated && currentObject != null && items[currentObject.x][currentObject.y] != null) {
            TextureDrawing.drawRectangle(0, 0, 1920, 1080, new Color(0, 0, 0, 50));
            TextureDrawing.drawRectangle(560, 0, 800, 1080, new Color(0, 0, 0, 50));
            TextureDrawing.drawTexture(defPath + "\\src\\assets\\UI\\GUI\\buildMenu\\exitBtn.png", 605, 989, 1, true);
            TextureDrawing.drawText(694, 730, items[currentObject.x][currentObject.y].item.description);
            Inventory.drawInventoryItem(694, 915, items[currentObject.x][currentObject.y].item.path);

            drawRequirements(694, 760);
        }
    }

    private static void drawRequirements(float x, float y) {
        if (currentObject != null && items[currentObject.x][currentObject.y] != null) {
            BuildItems item = items[currentObject.x][currentObject.y];

            drawText((int) x, (int) (y + 130), item.name);
            if (item.inputObjects != null) {
                Factories.drawObjects(x, y + 82, item.inputObjects, defPath + "\\src\\assets\\UI\\GUI\\buildMenu\\factoryIn.png");
            }
            if (item.outputObjects != null) {
                Factories.drawObjects(x, y + 41, item.outputObjects, defPath + "\\src\\assets\\UI\\GUI\\buildMenu\\factoryOut.png");
            }
            if (item.requiredForBuild != null) {
                Factories.drawObjects(x, y, item.requiredForBuild, defPath + "\\src\\assets\\UI\\GUI\\buildMenu\\build.png");
            }
        }
    }

    public static void addItem(BuildItems item) {

        for (int y = 0; y < items[0].length; y++) {
            boolean isAllSameCategory = true;

            for (BuildItems[] buildItems : items) {
                if (buildItems[y] == null) {
                    continue;
                }
                if (!buildItems[y].item.type.equals(item.item.type)) {
                    isAllSameCategory = false;
                    break;
                }
            }
            if (isAllSameCategory) {
                for (int x = 0; x < items.length; x++) {
                    if (items[x][y] == null) {
                        items[x][y] = item;
                        return;
                    }
                }
            }
        }
    }
}
