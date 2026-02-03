package core.World.Creatures.Player.WorkbenchMenu;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.Global;
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
import static core.World.Creatures.Player.ItemControl.*;
import static core.World.StaticWorldObjects.StaticWorldObjects.getFileName;
import static core.World.WorldGenerator.WorldGenerator.DynamicObjects;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

//todo скат доделай поиск гуи элементов под мышью пж
public class WorkbenchLogic implements StaticBlocksEvents {
    private static boolean isOpen, isSmall = true, smallNearby, mediumNearby, largeNearby;
    private static Point2i currentObject;
    private static float scroll = -276;
    private static final Color fogging = new Color(0, 0, 0, 117);
    private static int current = 0;
    private static final HashSet<Point2i> workbenchs = new HashSet<>();

    public static void updateInput() {
        updateBenchButton();
        updateNearby();
        updateOpen();
        updateScroll();
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

    private static void updateScroll() {
        int x = input.mousePos().x;
        int y = input.mousePos().y;

        if (x > 690 && x < 1270 && y > 400 && y < 796) {
            scroll = Math.clamp(scroll - input.getScrollChange() * 6, -246, 0);
        }
    }

    public static void draw() {
        if (isOpen) {
            //todo я знаю что оно криво но не могу доказать вамент выровняй пожалуйста
            batch.draw(atlas.byPath("UI/GUI/workbenchMenu/menu" + (currentObject == null ? "Small" : "Full")), 690, 400);
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

            Items[][] currentWorkbench = switch (current) {
                case 1 -> largeWorkbenchItems;
                case 2 -> mediumWorkbenchItems;
                case 3 -> smallWorkbenchItems;
                default -> buildMenuItems;
            };

            for (int x = 0; x < currentWorkbench.length; x++) {
                for (int y = 0; y < currentWorkbench[x].length; y++) {
                    if (currentWorkbench[x][y] != null) {
                        float xCoord = 760 + x * 54;
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

            if (currentObject != null) {
                float yCoord = 847 + scroll + (currentObject.y * 54);

                //todo а это что
                if (true) {
                    batch.draw(atlas.byPath("UI/GUI/inventory/inventoryCurrent.png"), 690 + currentObject.x * 54, yCoord);
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
        if (id != 0 && id != -1 && StaticWorldObjects.getTexture(id) != null && getFileName(id).toLowerCase().contains("workbench")) {
            workbenchs.add(new Point2i(cellX, cellY));
        }
    }

    @Override
    public void destroyStatic(int cellX, int cellY, short id) {

    }
}
