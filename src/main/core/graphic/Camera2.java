package core.graphic;

import core.math.AABB;
import core.math.Rectangle;
import core.math.Vector2d;
import core.math.Vector2f;

public final class Camera2 {
    public final Vector2d position = new Vector2d();

    // Размер экрана в логических единицах
    public final Vector2f logicalScreenSize = new Vector2f();
    public final Vector2f projectionScale = new Vector2f();

    private float width, height;
    private final float pixelsPerUnit; // пиксели на логический блок

    private final Vector2f tmp = new Vector2f();
    private double invWidth2, invHeight2; // (2.0 / width) и (2.0 / height)
    private double invScaleX, invScaleY; // 1.0 / projectionScale

    public Camera2(float pixelsPerUnit) {
        this.pixelsPerUnit = pixelsPerUnit;
    }

    public void update() {
        float w = width;
        float h = height;
        float invPPU = 1f / pixelsPerUnit;

        float lx = w * invPPU;
        float ly = h * invPPU;
        logicalScreenSize.set(lx, ly);
        projectionScale.set(2f / lx, 2f / ly);

        invWidth2 = 2. / w;
        invHeight2 = 2. / h;

        invScaleX = lx * 0.5;
        invScaleY = ly * 0.5;
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
    public Vector2f projectTo(Vector2d worldCoordinates, Vector2f screenCoordinates) {
        double localX = worldCoordinates.x - position.x;
        double localY = worldCoordinates.y - position.y;

        double ndcX = localX * projectionScale.x;
        double ndcY = localY * projectionScale.y;

        float hw = width * 0.5f;
        float hh = height * 0.5f;
        screenCoordinates.x = (float)Math.fma(ndcX, hw, hw);
        screenCoordinates.y = (float)Math.fma(ndcY, hh, hh);
        return screenCoordinates;
    }

    // Перевод координат экрана в координаты мира
    public void unprojectTo(Vector2f screenCoordinates, Vector2d worldCoordinates) {
        double ndcX = Math.fma(screenCoordinates.x, invWidth2, -1.);
        double ndcY = Math.fma(screenCoordinates.y, invHeight2, -1.);

        // NDC * (1/scale) + position
        worldCoordinates.x = Math.fma(ndcX, invScaleX, position.x);
        worldCoordinates.y = Math.fma(ndcY, invScaleY, position.y);
    }

    public void boundsTo(AABB aabb) {
        float w = logicalScreenSize.x;
        float h = logicalScreenSize.y;
        aabb.minX = position.x - w * .5;
        aabb.minY = position.y - h * .5;
        aabb.maxX = aabb.minX + w;
        aabb.maxY = aabb.minY + h;
    }

    public void boundsTo(Rectangle out) {
        out.setSize(logicalScreenSize.x, logicalScreenSize.y).setCenter(position.xf(), position.yf());
    }

    public Vector2f relativize(double worldX, double worldY) {
        return tmp.set(
                (float)(worldX - position.x),
                (float)(worldY - position.y)
        );
    }

    public Vector2f relativize(Vector2d worldCoordinates) {
        return tmp.set(
                (float)(worldCoordinates.x - position.x),
                (float)(worldCoordinates.y - position.y)
        );
    }
}
