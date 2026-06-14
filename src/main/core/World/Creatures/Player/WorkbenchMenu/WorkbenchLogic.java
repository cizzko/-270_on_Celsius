package core.World.Creatures.Player.WorkbenchMenu;

import core.Hotkeys;
import core.content.ItemStack;
import core.content.blocks.Factory;
import core.content.blocks.Workbench;
import core.content.entity.comp.InventoryComponent;
import core.content.items.Item;
import core.content.items.ItemBlock;
import core.g2d.Fill;
import core.g2d.StackfulRender;
import core.graphic.Color;
import core.graphic.GuiDrawing;
import core.math.Point2i;
import core.math.Rectangle;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;

import static core.Global.*;
import static core.graphic.GuiDrawing.drawObjects;

public class WorkbenchLogic {
    public static final int CELL_SIZE = 54;


    public static EnumMap<Workbench.Tier, Workbench> nearbyWorkbench = new EnumMap<>(Workbench.Tier.class);

    private static boolean isOpen;
    private static Point2i selected;
    private static int selectedIdx = -1;

    private static float scroll = -246;
    private static final Color fogging = new Color(0, 0, 0, 117);
    //todo не помешает перевести меню в гуи
    private static int current = 0, menuXPos = 1240;

    public static void updateInput() {
        nearbyWorkbench.clear();
        updateScroll();
        updateBuildButton();
    }

    private static void updateScroll() {
        var pos = input.mousePos();

        int viewportWidth = input.viewportWidth();
        int viewportHeight = input.viewportHeight();
        if (Rectangle.contains(viewportWidth - menuXPos, viewportHeight - 500, 400, 396, pos)) {
            scroll = Math.clamp(scroll - input.scrollDelta() * 6, -246, 0);
        }
    }

    public static void draw() {
        int viewportWidth = input.viewportWidth();
        int viewportHeight = input.viewportHeight();
        int baseX = viewportWidth - menuXPos;

        //чем дальше в лес иф елс иф елс иф елс
        if (nearbyWorkbench.isEmpty()) {
            if (current != 0) {
                resetObject();
            }
            current = 0;
        } else {
            //некрасиво
            if (Hotkeys.isMouseClickedIn(baseX + 8, 580, baseX + CELL_SIZE, 626)) {
                resetObject();
                current = 0;
            }
            if (Hotkeys.isMouseClickedIn(baseX + 8, 634, baseX + CELL_SIZE, 682) &&
                nearbyWorkbench.containsKey(Workbench.Tier.LARGE)) {
                resetObject();
                current = 1;
            }
            if (Hotkeys.isMouseClickedIn(baseX + 8, 688, baseX + CELL_SIZE, 734) &&
                nearbyWorkbench.containsKey(Workbench.Tier.MEDIUM)) {
                resetObject();
                current = 2;
            }
            if (Hotkeys.isMouseClickedIn(baseX + 8, 742, baseX + CELL_SIZE, 788) &&
                nearbyWorkbench.containsKey(Workbench.Tier.SMALL)) {
                resetObject();
                current = 3;
            }
        }

        if (isOpen) {
            StackfulRender.draw(atlas.get("UI/GUI/workbenchMenu/menu" + (selected == null ? "Small" : "Full")), baseX, 400);
            Fill.rect(baseX + 3, 587 + (CELL_SIZE * current), 3, 32, Color.rgba8888(255, 80, 0, 200));

            if (!nearbyWorkbench.containsKey(Workbench.Tier.SMALL)) {
                Fill.rect(baseX + 8, 742, 46, 46, fogging);
            }
            if (!nearbyWorkbench.containsKey(Workbench.Tier.MEDIUM)) {
                Fill.rect(baseX + 8, 688, 46, 46, fogging);
            }
            if (!nearbyWorkbench.containsKey(Workbench.Tier.LARGE)) {
                Fill.rect(baseX + 8, 634, 46, 46, fogging);
            }

            final int IN_ROW = 9;

            var currentWorkbench = getCurrentItems();
            for (int i = 0, y = 0; i < currentWorkbench.size(); i++) {
                int x = i % IN_ROW;

                float xCoord = baseX + 72 + x * CELL_SIZE;
                float yCoord = 982 + scroll - (y * CELL_SIZE);

                if (yCoord < 741) {
                    GuiDrawing.drawItem(xCoord, yCoord, currentWorkbench.get(i));

                    if (Hotkeys.isMouseClickedIn(xCoord, yCoord, xCoord + 46, yCoord + 46)) {
                        selected = new Point2i(x, y);
                        selectedIdx = i;
                    }
                }
                if (x == 0 && i > 0) {
                    y++;
                }
            }

            //todo описания
            //todo перенести все в гуи!! но позже
            if (selectedIdx != -1 && 986 + scroll + (selected.y * CELL_SIZE) < 741) {
                GuiDrawing.drawText(baseX + 585, 703, currentWorkbench.get(selectedIdx).getName());
                drawRequirements(baseX + 590,  648);
                StackfulRender.draw(atlas.get("UI/GUI/inventory/inventoryCurrent"),
                        baseX + 62 + selected.x * CELL_SIZE,
                        972 + scroll + (selected.y * CELL_SIZE));
            }

            // scrollbar
            var color = Color.rgba8888(0, 0, 0, 200);
            Fill.rect(1915, (int) Math.abs(scroll / 2f) - 5, 4, 20, color);
        }
    }

    private static void resetObject() {
        selected = null;
        selectedIdx = -1;
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

        int viewportWidth = input.viewportWidth();
        int viewportHeight = input.viewportHeight();
        int baseX = viewportWidth - menuXPos;
        if (!isOpen || !Hotkeys.isMouseClickedIn(baseX + 580, viewportHeight - 338, baseX + 625, viewportHeight - 292)) {
            return;
        }
        var required = hasRequiredItems();
        if (required == null) {
            return;
        }
        var currentItems = getCurrentItems();
        Item item = currentItems.get(selectedIdx);
        var stat = player.addItem(new ItemStack(item, item.createCount));
        if (stat != InventoryComponent.TransitionResult.FAILED) {
            for (var obj : required) {
                player.takeItem(obj.x, obj.y, obj.count);
            }
        } else {
            System.out.println("Не получилось добавить :( " + stat);
        }
    }

    public static void toggleBuildMenu() {
        isOpen = !isOpen;
    }

    record ItemCraftTransaction(int x, int y, int count) {}

    private static @Nullable ItemCraftTransaction[] hasRequiredItems() {
        if (selectedIdx == -1) {
            return null;
        }

        var currentItems = getCurrentItems();

        Item item = currentItems.get(selectedIdx);
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
        if (selectedIdx == -1) {
            return;
        }
        var currentItems = getCurrentItems();
        var stack = currentItems.get(selectedIdx);
        if (stack instanceof ItemBlock itemBlock && itemBlock.block instanceof Factory factory) {
            drawObjects(x, y - 41, factory.input, atlas.get("UI/GUI/buildMenu/factoryIn"));
            drawObjects(x, y - 82, factory.output, atlas.get("UI/GUI/buildMenu/factoryOut"));
        }
        drawObjects(x, y, stack.requirements, atlas.get("UI/GUI/buildMenu/build"));
    }
}
