package core.World;

import core.Application;
import core.Constants;
import core.EventHandling.Config;
import core.Global;
import core.World.Creatures.Physics;
import core.World.WorldGenerator.WorldGenerator;
import core.content.ItemStack;
import core.content.creatures.CreatureType;
import core.content.creatures.ItemEntity;
import core.content.entity.CreatureEntity;
import core.content.strctures.Structure;
import core.math.Point2i;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

import static core.Constants.World.COPY_SIZE;
import static core.Global.*;
import static core.World.Textures.TextureDrawing.blockSize;

public class WorldUtils {

    public static int mirrorPos(int x) {
        int rightBorder = (world.sizeX - Constants.World.COPY_SIZE);
        int leftBorder = Constants.World.COPY_SIZE;
        int dx = rightBorder - leftBorder;

        if (x > rightBorder) {
            return x - dx;
        } else if (x < leftBorder) {
            return x + dx;
        }
        return x;
    }

    /// @return абсолютное значение в блоках от игрока до мыши
    public static int getDistanceToMouse() {
        Point2i blockPos = input.mouseBlockPos();
        return Math.abs(player.blockX() - blockPos.x + player.blockY() - blockPos.y);
    }

    public static int getDistanceBetweenBlocks(Point2i mainPoint, Point2i secondPoint) {
        return Math.abs(mainPoint.x - secondPoint.x) + Math.abs(mainPoint.y - secondPoint.y);
    }

    /// @param spawnRules предохраняет от спавна в потенциально опасном для логики отрезке COPY_SIZE*2
    public static <E extends CreatureEntity> E spawn(CreatureType entity, boolean spawnRules) {
        int bx;
        if (!spawnRules) {
            bx = ThreadLocalRandom.current().nextInt(0, world.sizeX);
        } else {
            bx = Math.clamp(ThreadLocalRandom.current().nextInt(0, world.sizeX), COPY_SIZE * 2, world.sizeX - COPY_SIZE * 2);
        }
        return spawn0(entity, bx);
    }

    public static void dropItem(ItemStack itemStack, float x, float y) {
        float rx = x + ThreadLocalRandom.current().nextFloat(0, blockSize/3f);
        float ry = y + ThreadLocalRandom.current().nextFloat(0, blockSize/3f);
        spawnItemEntity(itemStack, rx, ry);
    }

    public static ItemEntity spawnItemEntity(ItemStack itemStack, float x, float y) {
        int id = Global.entityPool.acquireId();
        var ent = new ItemEntity(itemStack);

        ent.setId(id);
        ent.setPosition(x, y);
        ent.init();

        Global.entityPool.add(ent);
        return ent;
    }

    private static <E extends CreatureEntity> E spawn0(CreatureType entity, int bx) {
        float wx = bx * blockSize;
        float wy = (WorldGenerator.findTopmostSolidBlock(bx, 5) + 1) * blockSize;

        if (Physics.checkIntersection(wx, wy, entity.texture)) {
            Application.log.warn("Unable spawning at: ({}, {})", wx, wy);
            return spawn0(entity, bx + 1);
        }

        @SuppressWarnings("unchecked")
        var ent = (E) entity.create(wx, wy);
        return ent;
    }

    public static boolean checkPlaceRules(int x, int y, Structure structure) {
        for (Structure.Part p : structure.blocks) {
            if (!world.checkPlaceRules(x + p.offsetX, y + p.offsetY, p.block())) {
                return false;
            }
        }
        return true;
    }

    public static void setStructure(int x, int y, Structure tree) {
        // if (!checkPlaceRules(x, y, tree)) {
        //     return;
        // }
        for (Structure.Part p : tree.blocks) {
            world.set(x + p.offsetX, y + p.offsetY, p.block(), false);
        }
    }

    public static void saveWorld(World world, Path file) {
        try {
            Config.json.writeValue(file.toFile(), world);
        } catch (IOException e) {
            Application.log.error("Failed to world to file '{}'", file, e);
        }
    }

    public static World loadWorld(Path file) {
        return null;
    }
}
