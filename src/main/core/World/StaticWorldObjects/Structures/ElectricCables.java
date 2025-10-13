package core.World.StaticWorldObjects.Structures;

import core.World.Creatures.Player.Inventory.InventoryEvents;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator;
import core.World.WorldUtils;
import core.math.Point2i;

import java.util.*;

public class ElectricCables implements InventoryEvents {
    private LinkedHashSet<Point2i> points;
    private float currentVoltage = 0, currentHp = 100;
    private final float maxVoltage = 1;
    private long lastDamageTime = System.currentTimeMillis();

    private static Point2i lastPlacedCable;
    private static ArrayDeque<ElectricCables> cables = new ArrayDeque<>();

    @Override
    public void itemDropped(int blockX, int blockY, ItemStack item) {
        if (WorldGenerator.blockAt(blockX, blockY).getBlock().type == StaticObjectsConst.Types.SOLID &&
                item.getItem().id.equalsIgnoreCase("electric cable")) {
            if (lastPlacedCable != null) {
                if (WorldUtils.getDistanceBetweenBlocks(lastPlacedCable, new Point2i(blockX, blockY)) <= 15) {
                    placeCable(lastPlacedCable, new Point2i(blockX, blockY));
                }
                lastPlacedCable = null;
            } else {
                lastPlacedCable = new Point2i(blockX, blockY);
            }
        }
    }

    public static ElectricCables getNetworkIsHere(Point2i pos) {
        for (ElectricCables cable : cables) {
            if (cable.points.contains(pos)) {
                return cable;
            }
        }
        return null;
    }

    private static void placeCable(Point2i from, Point2i to) {
        ElectricCables fromContainsCable = getNetworkIsHere(from);
        ElectricCables toContainsCable = getNetworkIsHere(to);

        // if two cable lines need connected
        if (fromContainsCable != null && toContainsCable != null) {
            fromContainsCable.points.addAll(toContainsCable.points);

            // if the cable is pulled from a point without cable to a point with cable
        } else if (toContainsCable != null) {
            toContainsCable.points.add(from);

            // if the cable is pulled from a point with cable to a point without cable
        } else if (fromContainsCable != null) {
            fromContainsCable.points.add(to);

            // if start pulling new cable
        } else {
            ElectricCables cable = new ElectricCables();
            cable.points = new LinkedHashSet<>();
            cable.points.add(from);
            cable.points.add(to);

            cables.add(cable);
        }
    }

    public static void drawCables() {
        for (ElectricCables cable : cables) {
            TextureDrawing.drawPointArray(cable.points.toArray(new Point2i[0]));
            updateCables(cable);
        }
    }

    private static void updateCables(ElectricCables cable) {
        long lastHit = System.currentTimeMillis() - cable.lastDamageTime;

        if (cable.currentVoltage > cable.maxVoltage && (((Math.random() * (10000 - lastHit)) <= 100) || (lastHit > 10000))) {
            cable.currentHp -= 10;
            cable.lastDamageTime = System.currentTimeMillis();
        }
        if (cable.currentHp <= 0) {
            Thread.startVirtualThread(() -> cables.remove(cable)).start();
        }
    }
}
