package core;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.UI.Styles;
import core.World.Creatures.DynamicWorldObjects;
import core.World.Creatures.Physics;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Weapons.Weapons;
import core.World.Creatures.Player.WorkbenchMenu.WorkbenchLogic;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.TileData;
import core.World.Textures.TextureDrawing;
import core.World.Weather.Sun;
import core.World.WorldUtils;
import core.g2d.Fill;
import core.graphic.Layer;
import core.math.Rectangle;
import core.math.Vector2f;
import core.util.Color;
import core.util.Commandline;

import static core.EventHandling.EventHandler.debugLevel;
import static core.EventHandling.EventHandler.updateHotkeys;
import static core.Global.*;
import static core.World.Creatures.Player.Player.*;
import static core.World.Textures.TextureDrawing.blockSize;
import static core.World.WorldGenerator.WorldGenerator.DynamicObjects;

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
        var player = DynamicObjects.getFirst();
        camera.position.set(player.getX(), player.getY());
        EventHandler.setDebugValue(() -> "Camera Pos: " + camera.position);
        EventHandler.setDebugValue(() -> "Velocity: " + player.velocity);
        EventHandler.setDebugValue(() -> {
            var mouseBlockPos = (WorldUtils.getBlockUnderMousePoint());
            var mouseBlock = world.getBlock(mouseBlockPos.x, mouseBlockPos.y);
            return "MouseBlock: " + (mouseBlock != null ? mouseBlock.id + " (NID: " + content.getBlockIdByType(mouseBlock) + ")" : "<void>");
        });
        EventHandler.setDebugValue(() -> {
            var mouseBlockPos = (WorldUtils.getBlockUnderMousePoint());
            return "BlockHp: " + world.getHp(mouseBlockPos.x, mouseBlockPos.y);
        });

        //EventHandler.setDebugValue(() -> "Current time: " + sun.currentTime);

        smoothedCamera = Config.getBoolean("SmoothedCamera");

        //todo у предметы
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
        updatePlayerPos();
        postEffect.update();
        sun.update();
        updateInventoryInteraction();
        Weapons.updateAmmo();
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
        TextureDrawing.drawStatic();
        batch.z(Layer.DYNAMIC_OBJECTS);
        TextureDrawing.drawDynamic();

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

    // Изменения, связанные с координатами игрока
    private void updatePlayerPos() {
        DynamicWorldObjects player = DynamicObjects.getFirst();

        float playerX = player.getX();
        float playerY = player.getY();

        if (smoothedCamera) {
            camera.position.lerpDeltaTime(playerX + 32, playerY + 200, 0.05f * Math.max(1, player.velocity.len() / 4f));
        } else {
            camera.position.set(playerX + 32, playerY + 200);
        }

        camera.update();
    }

    final Rectangle rect = new Rectangle();
    final Vector2f vec = new Vector2f();
    final Color green = Color.fromRgba8888(0, 255, 0, 255);
    final Color red = Color.fromRgba8888(255, 0, 0, 255);
    final Color blue = Color.fromRgba8888(0, 0, 255, 255);
    final Color white = Color.fromRgba8888(255, 255, 255, 255);
    final Color black = Color.fromRgba8888(0, 0, 0, 255);

    private void drawDebug() {
        if (debugLevel < 2) {
            return;
        }
        {
            var player = DynamicObjects.getFirst();
            var size = player.getTexture();

            player.getHitboxTo(rect);
            var center = rect.getCenterTo(vec);

            int cx = (int) Math.floor(center.x / blockSize);
            int cy = (int) Math.floor(center.y / blockSize);

            float width = size.width();
            float height = size.height();
            int w = (int) Math.ceil(width / blockSize);
            int h = (int) Math.ceil(height / blockSize);

            int minX = (int) Math.floor(player.getX() / blockSize);
            int minY = (int) Math.floor(player.getY() / blockSize);

            int maxX = (int) Math.floor((player.getX() + width) / blockSize);
            int maxY = (int) Math.floor((player.getY() + height) / blockSize);

            TextureDrawing.drawText(player.getX(), player.getY() + size.height() - 32,
                    "HasFloor: " + player.hasFloor(), black);

            // Интегрированный прямоугольник, который используется как хитбокс
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    Fill.rectangleBorder(x * blockSize, y * blockSize, blockSize, blockSize, white);
                }
            }

            TextureDrawing.drawText(player.getX(), player.getY() + size.height(),
                    "Size: " + w + "x" + h + " (" + size.width() + "x" + size.height() + ")", Styles.DIRTY_BRIGHT_BLACK);

            // Ближайший к центру игрока блок
            //<место для вашего условия>
            Fill.rectangleBorder(cx * blockSize, cy * blockSize, blockSize, blockSize, green);

            // Прямоугольник, который показывает занятое текстурой пространство
            Fill.rectangleBorder(player.getX(), player.getY(), size.width(), size.height(), red);

            // Две пересекающиеся перпендикулярные прямые, точкой пересечения которых является центр текстуры
            Fill.line(player.getX() + size.width() / 2f, player.getY(), player.getX() + size.width() / 2f, player.getY() + size.height(), blue);
            Fill.line(player.getX(), player.getY() + size.height() / 2f, player.getX() + size.width(), player.getY() + size.height() / 2f, blue);
        }

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
                if (obj == null || obj == StaticObjectsConst.AIR || obj.texture == atlas.getErrorRegion()) {
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
