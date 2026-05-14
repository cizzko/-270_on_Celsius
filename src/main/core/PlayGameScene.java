package core;

import core.EventHandling.EventHandler;
import core.EventHandling.Config;
import core.World.Creatures.Physics;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Bullets;
import core.World.Creatures.Player.WorkbenchMenu.WorkbenchLogic;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.TileData;
import core.World.Textures.TextureDrawing;
import core.World.Weather.Sun;
import core.g2d.Fill;
import core.graphic.Layer;
import core.math.Rectangle;
import core.util.Commandline;
import it.unimi.dsi.fastutil.HashCommon;

import static core.EventHandling.EventHandler.debugLevel;
import static core.EventHandling.EventHandler.updateHotkeys;
import static core.Global.*;
import static core.World.Creatures.Physics.swap;
import static core.World.Creatures.Player.Player.*;
import static core.World.Textures.TextureDrawing.blockSize;
import static core.content.entity.DrawComponent.GAP;
import static core.util.Color.*;

public final class PlayGameScene extends GameScene {
    public final Sun sun = new Sun();
    public final PostEffect postEffect = new PostEffect();
    //надо что то думать с круглым миром
    public static boolean smoothedCamera;

    private boolean paused;

    public void togglePaused() {
        paused = !paused;
    }

    public void setPaused(boolean state) {
        paused = state;
    }

    public boolean isPaused() {
        return paused;
    }

    @Override
    public void onInit() {
        camera.position.set(player.getX(), player.getY());
        EventHandler.setDebugValue(() -> "Camera Pos: " + camera.position);
        EventHandler.setDebugValue(() -> "Velocity: " + player.getVelocity());
        EventHandler.setDebugValue(() -> {
            var mouseBlockPos = (input.mouseBlockPos());
            var mouseBlock = world.getBlock(mouseBlockPos.x, mouseBlockPos.y);
            return "MouseBlock: " +
                   mouseBlockPos.toString() + " " +
                   (mouseBlock != null ? mouseBlock.id + " (NID: " + Global.content.blocksRegistry.idByType(mouseBlock) + ")" : "<void>");
        });
        EventHandler.setDebugValue(() -> {
            var mouseBlockPos = (input.mouseBlockPos());
            return "BlockHp: " + world.getHp(mouseBlockPos.x, mouseBlockPos.y);
        });
        EventHandler.setDebugValue(() -> "PlayerHp: " + player.getHp());

        //EventHandler.setDebugValue(() -> "Current time: " + sun.currentTime);

        smoothedCamera = Config.getBoolean("SmoothedCamera");

        //todo у предметы
        Inventory.addItem(content.itemById("blockDeleter"));
        for (int i = 0; i < 10; i++) {
            Inventory.addItem(content.itemById("aluminum"));
            Inventory.addItem(content.itemById("chest"));
            Inventory.addItem(content.itemById("stick"));
            Inventory.addItem(content.itemById("redHammer"));
            Inventory.addItem(content.itemById("grass"));
            Inventory.addItem(content.itemById("workbenchSmall"));
            Inventory.addItem(content.itemById("smallStone"));
            Inventory.addItem(content.itemById("stoneOven"));
        }
    }

    @Override
    protected void inputUpdate() {
        updateHotkeys(this);
        WorkbenchLogic.updateInput();
        Commandline.inputUpdate();
        updateToolInteraction();
        Inventory.inputUpdate();
    }

    @Override
    protected void update() {
        Physics.updatePhysics(this);
        updateCamera();
        postEffect.update();
        sun.update();
        updateInventoryInteraction();
        Bullets.updateBullets();
        world.update();
        Inventory.updateStaticBlocksPreview();
    }

    @Override
    protected void draw() {

        batch.z(Layer.BACKGROUND);
        sun.draw();
        postEffect.draw();
        batch.z(Layer.STATIC_OBJECTS);
        batch.matrix(camera.projection); // Центрируем камеру на позицию игрока
        TextureDrawing.drawBlocks();
        batch.z(Layer.DYNAMIC_OBJECTS);
        TextureDrawing.drawEntities();

        drawDebug();

        uiScene.draw();
        Commandline.draw();
        WorkbenchLogic.draw();
        Inventory.draw();
        drawCurrentHP();
    }

    @Override
    protected void drawLoading() {

    }


    public static void updateCamera() {
        if (player.isDead()) {
            return;
        }

        if (smoothedCamera) {
            float base = 0.08f * Math.max(1, player.getVelocity().len() / 4f);
            base = Math.min(1f, base);
            float alpha = 1 - (float)Math.pow(1 - base, Time.delta);
            camera.position.lerp(player.getX() + 32, player.getY() + 200, alpha);
            if (Float.isNaN(Global.camera.position.x) || Float.isNaN(Global.camera.position.y)) {
                System.out.println("NAN CAM POS: x=" + player.getX() + " y=" + player.getY() +
                                   " v=" + player.getVelocity() + " base=" + base + " alpha=" + alpha);
            }
        } else {
            camera.position.set(player.getX() + 32, player.getY() + 200);
        }

        camera.update();
    }

    final Rectangle rect = new Rectangle();
    final int red = rgba8888(255, 0, 0, 255);
    final int blue = rgba8888(0, 0, 255, 255);
    final int white = rgba8888(255, 255, 255, 255);
    final int acid = 0x8ffe09ff;
    final int black = rgba8888(0, 0, 0, 255);


    public static int leftInt(long field) { return (int)(field >> 32); }
    public static int rightInt(long field) { return (int)(field); }

    private void drawDebug() {
        if (debugLevel < 2) {
            return;
        }

        entityPool.entities().values().forEach(e -> {
            e.getHitboxTo(rect);
            Fill.rectangleBorder(rect.x, rect.y, rect.width, rect.height, red);
            TextureDrawing.drawText(rect.x, rect.y,
                    "HasFloor: " + e.hasFloor(), black);
        });

        if (debugLevel >= 3) {
            var r = entityPool.worldIndex().resolution;
            var hashIndex = entityPool.worldIndex().hash;
            hashIndex.keySet().forEach(hash -> {
                long key = HashCommon.invMix(hash);
                float x = leftInt(key) * r;
                float y = rightInt(key) * r;

                Fill.rectangleBorder(x, y, r, r, acid);
                var group = hashIndex.get(hash);
                TextureDrawing.drawText(x, y, "GroupSize: " + group.size());
            });
        }

        camera.getBoundsTo(rect);
        // правая граница
        Fill.line(
                (world.sizeX) * blockSize, rect.y,
                (world.sizeX) * blockSize, rect.y + rect.height,
                4,
                red);
        Fill.line(
                (world.sizeX - swap) * blockSize, rect.y,
                (world.sizeX - swap) * blockSize, rect.y + rect.height,
                4,
                black);
        // левая граница
        Fill.line(
                0, rect.y,
                0, rect.y + rect.height,
                4,
                blue);
        Fill.line(
                swap * blockSize, rect.y,
                swap * blockSize, rect.y + rect.height,
                4,
                black);

        if (!player.isDead()) {
            player.getHitboxTo(rect);
            { // Блоки интегрированной модели
                int minX = (int) Math.floor(rect.x / blockSize);
                int minY = (int) Math.floor(rect.y / blockSize);
                int maxX = (int) Math.floor((rect.x + rect.width) / blockSize);
                int maxY = (int) Math.floor((rect.y + rect.height) / blockSize);
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        Fill.rectangleBorder(x * blockSize, y * blockSize, blockSize, blockSize, white);
                    }
                }
            }

            { // Блоки которые считаются за пол. Черная обводка
                int minX = (int) Math.floor(player.getX() / blockSize);
                int maxX = (int) Math.floor((player.getX() + player.creature.texture.width() - GAP) / blockSize);
                int minY = (int) Math.floor((player.getY() - GAP) / blockSize);

                for (int x = minX; x <= maxX; x++) {
                    var block = world.getBlock(x, minY);
                    if (block == null || block.type == StaticObjectsConst.Type.SOLID) {
                        Fill.rectangleBorder(x*blockSize,minY*blockSize, blockSize, blockSize, BLACK);
                    }
                }
            }
        }

        { // Корень красный, дочерние синие
            camera.getBoundsTo(rect);
            int minX = (int) Math.floor(rect.x / blockSize);
            int maxX = (int) Math.floor((rect.x + rect.width) / blockSize);
            int minY = (int) Math.floor(rect.y / blockSize);
            int maxY = (int) Math.floor((rect.y + rect.height) / blockSize);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    if (!world.inBounds(x, y)) {
                        continue;
                    }

                    var obj = world.getBlock(x, y);
                    if (obj == null || obj == StaticObjectsConst.AIR) {
                        continue;
                    }

                    var data = world.getData(x, y);
                    if (data instanceof TileData.MultiblockPart) {
                        Fill.rectangleBorder(x * blockSize, y * blockSize, blockSize, blockSize, blue);
                    } else {
                        var rootPos = world.getRootBlockPos(x, y);
                        if (rootPos != null && rootPos.x == x && rootPos.y == y) {
                            Fill.rectangleBorder(x * blockSize, y * blockSize, blockSize, blockSize, red);
                        }
                    }
                }
            }
        }
    }
}
