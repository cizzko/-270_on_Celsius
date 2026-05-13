package core.World.Creatures.Player.WorkbenchMenu;

import core.EventHandling.EventHandler;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Item;
import core.World.ItemBlock;
import core.World.Textures.TextureDrawing;
import core.content.blocks.Factory;
import core.content.blocks.Workbench;
import core.g2d.Fill;
import core.math.Point2i;
import core.util.Color;

import java.util.EnumMap;
import java.util.List;

import static core.Global.*;
import static core.World.Creatures.Player.Inventory.Inventory.inventoryObjects;
import static core.World.Textures.TextureDrawing.drawObjects;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_B;

public class WorkbenchLogic {
    public static EnumMap<Workbench.Tier, Workbench> nearbyWorkbench = new EnumMap<>(Workbench.Tier.class);

    private static boolean isOpen;
    private static Point2i currentObject;
    private static int currentObjectIdx = -1;

    private static float scroll = -276;
    private static final Color fogging = new Color(0, 0, 0, 117);
    //todo не помешает перевести меню в гуи
    private static int current = 0, menuXPos = 680;

    public static void updateInput() {
        updateBenchButton();
        updateOpen();
        updateScroll();
        updateBuildButton();
    }

    private static void updateBenchButton() {
        nearbyWorkbench.clear();
    }

    private static void updateOpen() {
        // Point2i blockUMB = Global.input.mouseBlockPos();
        // todo сделать проверку на нахождение мыши на элте интерфейса
        // if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && getFileName(world.get(blockUMB.x, blockUMB.y)) != null && getFileName(world.get(blockUMB.x, blockUMB.y)).toLowerCase().contains("workbench")) {
        //     isOpen = true;
        // }
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

        if (EventHandler.getRectangleClick(menuXPos + 8, 580, menuXPos + 54, 626)) {
            //todo некрасиво
            currentObject = null;
            current = 0;
        }
        if (!nearbyWorkbench.isEmpty()) {
            if (EventHandler.getRectangleClick(menuXPos + 8, 634, menuXPos + 54, 682) &&
                nearbyWorkbench.containsKey(Workbench.Tier.LARGE)) {
                currentObject = null;
                current = 1;
            }
            if (EventHandler.getRectangleClick(menuXPos + 8, 688, menuXPos + 54, 734) &&
                nearbyWorkbench.containsKey(Workbench.Tier.MEDIUM)) {
                currentObject = null;
                current = 2;
            }
            if (EventHandler.getRectangleClick(menuXPos + 8, 742, menuXPos + 54, 788) &&
                nearbyWorkbench.containsKey(Workbench.Tier.SMALL)) {
                currentObject = null;
                current = 3;
            }
        }

        batch.draw(atlas.byPath("UI/GUI/buildMenu/menuClosed"), 1650, 0);

        if (isOpen) {
            batch.draw(atlas.byPath("UI/GUI/workbenchMenu/menu" + (currentObject == null ? "Small" : "Full")), menuXPos, 400);
            Fill.rect(menuXPos + 3, 587 + (54 * current), 3, 32, Color.fromRgba8888(255, 80, 0, 200));

            if (!nearbyWorkbench.containsKey(Workbench.Tier.SMALL)) {
                Fill.rect(menuXPos + 8, 742, 46, 46, fogging);
            }
            if (!nearbyWorkbench.containsKey(Workbench.Tier.MEDIUM)) {
                Fill.rect(menuXPos + 8, 688, 46, 46, fogging);
            }
            if (!nearbyWorkbench.containsKey(Workbench.Tier.LARGE)) {
                Fill.rect(menuXPos + 8, 634, 46, 46, fogging);
            }

            final int IN_ROW = 9;

            var currentWorkbench = getCurrentItems();
            for (int i = 0, y = 0; i < currentWorkbench.size(); i++) {
                int x = i % IN_ROW;

                float xCoord = menuXPos + 70 + x * 54;
                //float yCoord = 57 + scroll + (smallWorkbenchItems[x][y].type.ordinal() * 20) + y * 54f;
                float yCoord = 1000 + scroll + (y * 54f);

                if (yCoord < 755) {
                    Inventory.drawItem(xCoord, yCoord, currentWorkbench.get(i));

                    if (EventHandler.getRectangleClick((int) xCoord, (int) yCoord, (int) (xCoord + 46), (int) (yCoord + 46))) {
                        currentObject = new Point2i(x, y);
                        currentObjectIdx = i;
                    }
                }
                if (x == 0) {
                    y++;
                }
            }

            //todo описания
            if (currentObjectIdx != -1) {
                TextureDrawing.drawText(menuXPos + 585, 703, currentWorkbench.get(currentObjectIdx).getName());
                drawRequirements(menuXPos + 590,  648);
                batch.draw(atlas.byPath("UI/GUI/inventory/inventoryCurrent.png"), menuXPos + 62 + currentObject.x * 54,  986 + scroll + (currentObject.y * 54));
            }

            // scrollbar
            //Color color = Color.fromRgba8888(0, 0, 0, 200);
            //Fill.rect(1915, (int) Math.abs(scroll / 2f) - 5, 4, 20, color);
        }
    }

    private static List<Item> getCurrentItems() {
        return switch (current) {
            case 0 -> content.getCraftsFor(null);
            case 1 -> content.getCraftsFor(nearbyWorkbench.get(Workbench.Tier.LARGE));
            case 2 -> content.getCraftsFor(nearbyWorkbench.get(Workbench.Tier.MEDIUM));
            case 3 -> content.getCraftsFor(nearbyWorkbench.get(Workbench.Tier.SMALL));
            default -> throw new IllegalArgumentException();
        };
    }

    private static void updateBuildButton() {
        if (!isOpen || !EventHandler.getRectangleClick(menuXPos + 580, 742, menuXPos + 625, 788)) {
            return;
        }
        var required = hasRequiredItems();
        if (required == null) {
            return;
        }
        var currentItems = getCurrentItems();
        if (Inventory.addItem(currentItems.get(currentObjectIdx))) {
            for (var obj : required) {
                if (obj != null) {
                    Inventory.decrementItem(obj.x, obj.y, obj.count);
                }
            }
        }
    }

    record ItemCraftTransaction(int x, int y, int count) {}

    private static ItemCraftTransaction[] hasRequiredItems() {
        if (currentObjectIdx == -1) {
            return null;
        }


        var currentItems = getCurrentItems();

        Item item = currentItems.get(currentObjectIdx);
        if (item.requirements != null) {
            var required = item.requirements;
            ItemCraftTransaction[] hasNeededObject = new ItemCraftTransaction[required.length];
            int neededCounter = 0;

            for (int i = 0; i < required.length; i++) {
                for (int x = 0; x < inventoryObjects.length; x++) {
                    for (int y = 0; y < inventoryObjects[x].length; y++) {
                        if (inventoryObjects[x][y] != null && inventoryObjects[x][y].getItem() == required[i].getItem()) {
                            hasNeededObject[i] = new ItemCraftTransaction(x, y, required[i].getCount());
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
        if (currentObjectIdx == -1) {
            return;
        }
        var currentItems = getCurrentItems();
        var stack = currentItems.get(currentObjectIdx);
        if (stack instanceof ItemBlock itemBlock && itemBlock.block instanceof Factory factory) {
            drawObjects(x, y - 41, factory.input, atlas.byPath("UI/GUI/buildMenu/factoryIn.png"));
            drawObjects(x, y - 82, factory.output, atlas.byPath("UI/GUI/buildMenu/factoryOut.png"));
        }
        drawObjects(x, y, stack.requirements, atlas.byPath("UI/GUI/buildMenu/build.png"));
    }
}
