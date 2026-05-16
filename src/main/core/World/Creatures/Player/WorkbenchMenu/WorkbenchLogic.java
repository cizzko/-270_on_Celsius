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
import static core.World.Textures.TextureDrawing.drawItem;
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

        if (EventHandler.isMouseClickedIn(menuXPos + 8, 580, menuXPos + 54, 626)) {
            //todo некрасиво
            currentObject = null;
            current = 0;
        }
        if (!nearbyWorkbench.isEmpty()) {
            if (EventHandler.isMouseClickedIn(menuXPos + 8, 634, menuXPos + 54, 682) &&
                nearbyWorkbench.containsKey(Workbench.Tier.LARGE)) {
                currentObject = null;
                current = 1;
            }
            if (EventHandler.isMouseClickedIn(menuXPos + 8, 688, menuXPos + 54, 734) &&
                nearbyWorkbench.containsKey(Workbench.Tier.MEDIUM)) {
                currentObject = null;
                current = 2;
            }
            if (EventHandler.isMouseClickedIn(menuXPos + 8, 742, menuXPos + 54, 788) &&
                nearbyWorkbench.containsKey(Workbench.Tier.SMALL)) {
                currentObject = null;
                current = 3;
            }
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
        if (!isOpen || !EventHandler.isMouseClickedIn(menuXPos + 580, 742, menuXPos + 625, 788)) {
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
