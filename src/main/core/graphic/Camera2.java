package core.graphic;

import core.math.Rectangle;
import core.math.Vector2f;

public final class Camera2 {
    public final Vector2f position = new Vector2f();

    // Размер экрана в логических единицах
    public final Vector2f logicalScreenSize = new Vector2f();
    public final Vector2f projectionScale = new Vector2f();

    private float width, height;
    private final float pixelsPerUnit; // пиксели на логический блок

    public Camera2(float pixelsPerUnit) {
        this.pixelsPerUnit = pixelsPerUnit;
    }

    public void update() {
        logicalScreenSize.set(
                width / pixelsPerUnit,
                height / pixelsPerUnit
        );
        projectionScale.set(
                2f / logicalScreenSize.x,
                2f / logicalScreenSize.y
        );
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
        position.set(width / (2f * pixelsPerUnit), height / (2f * pixelsPerUnit));
        resizeViewport(width, height);
    }

    public void resizeViewport(float width, float height) {
        this.width = width;
        this.height = height;
        update();
    }

    // Перевод координат мира в координаты экрана
    public Vector2f project(Vector2f worldCoordinates) {
        float localX = worldCoordinates.x - this.position.x;
        float localY = worldCoordinates.y - this.position.y;

        float ndcX = localX * projectionScale.x;
        float ndcY = localY * projectionScale.y;

        worldCoordinates.x = width * (ndcX + 1.0f) / 2.0f;
        worldCoordinates.y = height * (ndcY + 1.0f) / 2.0f;
        return worldCoordinates;
    }

    // Перевод координат экрана в координаты мира
    public Vector2f unproject(Vector2f screenCoordinates) {
        float ndcX = (2.0f * screenCoordinates.x) / width - 1.0f;
        float ndcY = (2.0f * screenCoordinates.y) / height - 1.0f;

        float localX = ndcX / projectionScale.x;
        float localY = ndcY / projectionScale.y;

        screenCoordinates.x = localX + this.position.x;
        screenCoordinates.y = localY + this.position.y;
        return screenCoordinates;
    }

    public void getBoundsTo(Rectangle out) {
        out.setSize(logicalScreenSize.x, logicalScreenSize.y).setCenter(position.x, position.y);
    }
}
