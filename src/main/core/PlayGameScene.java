package core;

import core.EventHandling.EventHandler;
import core.World.WorldGenerator.Backdrop;
import core.util.Color;
import core.util.Commandline;
import core.World.Creatures.DynamicWorldObjects;
import core.World.Creatures.Physics;
import core.World.Creatures.Player.BuildMenu.BuildMenu;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Weapons.Weapons;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.Textures.TextureDrawing;
import core.World.Weather.Sun;
import core.g2d.Fill;
import core.graphic.Layer;
import core.math.Rectangle;
import core.math.Vector2f;
import core.ui.Styles;

import static core.EventHandling.EventHandler.debugLevel;
import static core.EventHandling.EventHandler.updateHotkeys;
import static core.Global.*;
import static core.World.Creatures.Player.Player.*;
import static core.World.StaticWorldObjects.Structures.Factories.updateFactoriesOutput;
import static core.World.Textures.TextureDrawing.*;
import static core.World.WorldGenerator.WorldGenerator.DynamicObjects;

public final class PlayGameScene extends GameScene {
    public final Sun sun = new Sun();
    public final PostEffect postEffect = new PostEffect();
    //todo фелс ибо надо что то думать с круглым миром
    public static boolean smoothedCamera = false;

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

        Inventory.createElementPlaceable(StaticWorldObjects.createStatic("Factories/lowTemperatureOven"));
        Inventory.createElementPlaceable(StaticWorldObjects.createStatic("Factories/stoneCrusher"));
        Inventory.createElementPlaceable(StaticWorldObjects.createStatic("Blocks/smallStone"));

        var player = DynamicObjects.getFirst();
        camera.position.set(player.getX(), player.getY());
        EventHandler.setDebugValue(() -> "Camera pos: " + camera.position);
        EventHandler.setDebugValue(() -> "Current time: " + sun.currentTime);
    }

    @Override
    protected void inputUpdate() {
        updateHotkeys(this);
        BuildMenu.inputUpdate();
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
        Backdrop.update();
        updateInventoryInteraction();
        Weapons.updateAmmo();
        updateFactoriesOutput();
        updateBlocksInteraction();
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
        BuildMenu.draw();
        Inventory.draw();
        drawCurrentHP();
        drawBuildGrid();
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
                "HasFloor: " + player.hasFloor() + ", Velocity: " + player.velocity, black);

        // Интегрированный прямоугольник, который используется как хитбокс
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                Fill.rectangleBorder(x * blockSize, y * blockSize, blockSize, blockSize, white);
            }
        }

        TextureDrawing.drawText(player.getX(), player.getY() + size.height(),
                "Size: " + w + "x" + h + " (" + size.width() + "x" + size.height() + ")", Styles.DIRTY_BRIGHT_BLACK);

        // Ближайший к центру игрока блок
        if (false) Fill.rectangleBorder(cx * blockSize, cy * blockSize, blockSize, blockSize, green);
        // Прямоугольник, который показывает занятое текстурой пространство
        Fill.rectangleBorder(player.getX(), player.getY(), size.width(), size.height(), red);

        // Две пересекающиеся перпендикулярные прямые, точкой пересечения которых является центр текстуры
        Fill.line(player.getX() + size.width() / 2f, player.getY(), player.getX() + size.width() / 2f, player.getY() + size.height(), blue);
        Fill.line(player.getX(), player.getY() + size.height() / 2f, player.getX() + size.width(), player.getY() + size.height() / 2f, blue);
    }
}
