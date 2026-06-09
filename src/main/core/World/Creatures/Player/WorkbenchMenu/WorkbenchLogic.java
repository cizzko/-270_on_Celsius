package core.World.Creatures.Player.WorkbenchMenu;

import core.EventHandling.EventHandler;
import core.World.Creatures.Player.Inventory.Inventory;
import core.content.ItemStack;
import core.content.blocks.Factory;
import core.content.blocks.Workbench;
import core.content.items.Item;
import core.content.items.ItemBlock;
import core.g2d.Fill;
import core.g2d.StackfulRender;
import core.graphic.Color;
import core.graphic.GuiDrawing;
import core.math.Point2i;

import java.util.EnumMap;
import java.util.List;

import static core.Global.*;
import static core.graphic.GuiDrawing.drawObjects;

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
        updateScroll();
        updateBuildButton();
    }

    private static void updateBenchButton() {
        nearbyWorkbench.clear();
    }

    private static void updateScroll() {
        int x = input.mousePos().x;
        int y = input.mousePos().y;

        if (x > menuXPos && x < menuXPos + 580 && y > 400 && y < 796) {
            scroll = Math.clamp(scroll - input.getScrollChange() * 6, -246, 0);
        }
    }

    public static void draw() {
        //чем дальше в лес иф елс иф елс иф елс
        if (nearbyWorkbench.isEmpty()) {
            if (current != 0) {
                resetObject();
            }
            current = 0;
        } else {
            //некрасиво
            if (EventHandler.isMouseClickedIn(menuXPos + 8, 580, menuXPos + 54, 626)) {
                currentObject = null;
                resetObject();
                current = 0;
            }
            if (EventHandler.isMouseClickedIn(menuXPos + 8, 634, menuXPos + 54, 682) &&
                nearbyWorkbench.containsKey(Workbench.Tier.LARGE)) {
                resetObject();
                current = 1;
            }
            if (EventHandler.isMouseClickedIn(menuXPos + 8, 688, menuXPos + 54, 734) &&
                nearbyWorkbench.containsKey(Workbench.Tier.MEDIUM)) {
                resetObject();
                current = 2;
            }
            if (EventHandler.isMouseClickedIn(menuXPos + 8, 742, menuXPos + 54, 788) &&
                nearbyWorkbench.containsKey(Workbench.Tier.SMALL)) {
                resetObject();
                current = 3;
            }
        }

        if (isOpen) {
            StackfulRender.draw(atlas.get("UI/GUI/workbenchMenu/menu" + (currentObject == null ? "Small" : "Full")), menuXPos, 400);
            Fill.rect(menuXPos + 3, 587 + (54 * current), 3, 32, Color.rgba8888(255, 80, 0, 200));

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

                float xCoord = menuXPos + 72 + x * 54;
                float yCoord = 982 + scroll - (y * 54f);

                if (yCoord < 741) {
                    GuiDrawing.drawItem(xCoord, yCoord, currentWorkbench.get(i));

                    if (EventHandler.isMouseClickedIn(xCoord, yCoord, xCoord + 46, yCoord + 46)) {
                        currentObject = new Point2i(x, y);
                        currentObjectIdx = i;
                    }
                }
                if (x == 0 && i > 0) {
                    y++;
                }
            }

            //todo описания
            //todo перенести все в гуи!! но позже
            if (currentObjectIdx != -1 && 986 + scroll + (currentObject.y * 54) < 741) {
                GuiDrawing.drawText(menuXPos + 585, 703, currentWorkbench.get(currentObjectIdx).getName());
                drawRequirements(menuXPos + 590,  648);
                StackfulRender.draw(atlas.get("UI/GUI/inventory/inventoryCurrent"), menuXPos + 62 + currentObject.x * 54, 972 + scroll + (currentObject.y * 54));
            }

            // scrollbar
            Color color = Color.fromRgba8888(0, 0, 0, 200);
            Fill.rect(1915, (int) Math.abs(scroll / 2f) - 5, 4, 20, color);
        }
    }

    private static void resetObject() {
        currentObject = null;
        currentObjectIdx = -1;
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
        if (!isOpen || !EventHandler.isMouseClickedIn(menuXPos + 580, 742, menuXPos + 625, 788)) {
            return;
        }
        var required = hasRequiredItems();
        if (required == null) {
            return;
        }
        var currentItems = getCurrentItems();
        Item item = currentItems.get(currentObjectIdx);
        if (Inventory.addItemStack(new ItemStack(item, item.createCount))) {
            for (var obj : required) {
                if (obj != null) {
                    player.takeItem(obj.x, obj.y, obj.count);
                }
            }
        }
    }

    public static void toggleBuildMenu() {
        isOpen = !isOpen;
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
                var items = player.items();
                for (int x = 0; x < items.size(); x++) {
                    var line = items.get(x);
                    for (int y = 0; y < line.size(); y++) {
                        var itemStack = line.get(y);
                        if (itemStack != null && itemStack.isSame(required[i])) {
                            hasNeededObject[i] = new ItemCraftTransaction(x, y, required[i].count());
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
            drawObjects(x, y - 41, factory.input, atlas.get("UI/GUI/buildMenu/factoryIn"));
            drawObjects(x, y - 82, factory.output, atlas.get("UI/GUI/buildMenu/factoryOut"));
        }
        drawObjects(x, y, stack.requirements, atlas.get("UI/GUI/buildMenu/build"));
    }
}
