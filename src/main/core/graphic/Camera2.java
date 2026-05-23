package core.graphic;

import core.Global;
import core.content.entity.Hitbox;
import core.math.Mat3;
import core.math.Rectangle;
import core.math.Vector2f;

public final class Camera2 {
    public final Vector2f lastPosition = new Vector2f();
    public final Vector2f position = new Vector2f();
    public final Mat3 projection = new Mat3(), invProjection = new Mat3();

    private float width, height;
    private final float pixelsPerUnit; // пиксели на логический блок

    public Camera2(float pixelsPerUnit) {
        this.pixelsPerUnit = pixelsPerUnit;
    }

    public void updateLastPosition() {
        lastPosition.set(position);
    }

    public void update() {
        if (lastPosition.equalsEps(position, 1e-3f)) {
            return;
        }

        projection.setOrthographic(
                position.x - width / (2f * pixelsPerUnit),
                position.y - height / (2f * pixelsPerUnit),
                width / pixelsPerUnit,
                height / pixelsPerUnit
        );
        invProjection.set(projection).inv();
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public float pixelsPerUnit() {
        return pixelsPerUnit;
    }

    public void setToOrthographic(float width, float height) {
        lastPosition.set(0, 0);
        position.set(width / (2f * pixelsPerUnit), height / (2f * pixelsPerUnit));
        resizeViewport(width, height);
    }

    public void resizeViewport(float width, float height) {
        this.width = width;
        this.height = height;
        update();
    }

    // Перевод вектора с координатами мира в координаты экрана
    public Vector2f project(Vector2f worldCoordinates) {
        worldCoordinates.mul(projection);
        worldCoordinates.x = width * (worldCoordinates.x + 1) / 2;
        worldCoordinates.y = height * (worldCoordinates.y + 1) / 2;
        return worldCoordinates;
    }

    // Перевод вектора с координатами экрана в логические координаты (пиксели / pixelsPerUnit)
    public Vector2f unproject(Vector2f screenCoordinates) {
        screenCoordinates.x = (2 * screenCoordinates.x) / width - 1;
        screenCoordinates.y = (2 * screenCoordinates.y) / height - 1;
        screenCoordinates.mul(invProjection);
        return screenCoordinates;
    }

    // Получить границы камеры в логических координатах
    public void getBoundsTo(Rectangle out) {
        float logicalWidth = width / pixelsPerUnit;
        float logicalHeight = height / pixelsPerUnit;
        out.setSize(logicalWidth, logicalHeight).setCenter(position.x, position.y);
    }
}
