package core.World.Creatures.Player.WorkbenchMenu;

import core.EventHandling.EventHandler;
import core.World.StaticWorldObjects.StaticBlocksEvents;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.Textures.TextureDrawing;
import core.g2d.Fill;
import core.math.Point2i;
import core.util.Color;

import java.util.HashSet;

import static core.Global.*;
import static core.World.WorldGenerator.WorldGenerator.DynamicObjects;

public class WorkbenchLogic implements StaticBlocksEvents {
    private static boolean isOpen = true, smallNearby, mediumNearby, largeNearby;
    private static Point2i[] smallWorkbenchItems, mediumWorkbenchItems, largeWorkbenchItems;
    private static float scroll = -276;
    private static final Color fogging = new Color(0, 0, 0, 100);
    private static int current = 0;
    private static final HashSet<Point2i> workbenchs = new HashSet<>();

    public static void create() {

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
                String name = StaticWorldObjects.getFileName(world.get(point.x, point.y)).toLowerCase();

                if (name.contains("small")) {
                    smallNearby = true;
                } else if (name.contains("medium")) {
                    mediumNearby = true;
                } else if (name.contains("large")) {
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

    public static void draw() {
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
        }
    }

    @Override
    public void placeStatic(int cellX, int cellY, short id) {
        //todo что то придумать, проверять по имени файла это кпец
        if (StaticWorldObjects.getFileName(id).toLowerCase().contains("workbench")) {
            workbenchs.add(new Point2i(cellX, cellY));
        }
    }

    @Override
    public void destroyStatic(int cellX, int cellY, short id) {

    }
}
