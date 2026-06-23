package core.graphic;

import core.math.AABB;
import core.math.Rectangle;
import core.math.Vector2d;
import core.math.Vector2f;

public final class Camera {
    public final Vector2d position = new Vector2d();

    // Размер экрана в логических единицах
    public final Vector2f logicalScreenSize = new Vector2f();
    public final Vector2f projectionScale = new Vector2f();

    private float width, height;
    private float zoom = 1;
    private final float pixelsPerUnit; // пиксели на логический блок

    private final Vector2f tmp = new Vector2f();

    public Camera(float pixelsPerUnit) {
        this.pixelsPerUnit = pixelsPerUnit;
    }

    public void update() {
        float w = width;
        float h = height;
        float invPPU = 1f / pixelsPerUnit * zoom;

        float lx = w * invPPU;
        float ly = h * invPPU;
        logicalScreenSize.set(lx, ly);
        projectionScale.set(2f / lx, 2f / ly);
    }

    public float zoom() { return zoom; }

    public void setZoom(float zoom) { this.zoom = zoom; update(); }

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
        float ppu = pixelsPerUnit;
        position.set(width / (2f * ppu), height / (2f * ppu));
        resizeViewport(width, height);
    }

    public void resizeViewport(float width, float height) {
        this.width = width;
        this.height = height;
        update();
    }

    // Перевод координат мира в координаты экрана
    public Vector2f projectTo(Vector2d worldCoordinates, Vector2f screenCoordinates) {
        var pos = position;
        var lss = projectionScale;
        double localX = worldCoordinates.x - pos.x;
        double localY = worldCoordinates.y - pos.y;

        double ndcX = localX * lss.x;
        double ndcY = localY * lss.y;

        float hw = width * 0.5f;
        float hh = height * 0.5f;
        screenCoordinates.x = (float)Math.fma(ndcX, hw, hw);
        screenCoordinates.y = (float)Math.fma(ndcY, hh, hh);
        return screenCoordinates;
    }

    // Перевод координат экрана в координаты мира
    public void unprojectTo(Vector2f screenCoordinates, Vector2d worldCoordinates) {
        double ndcX = Math.fma(screenCoordinates.x, 2. / width, -1.);
        double ndcY = Math.fma(screenCoordinates.y, 2. / height, -1.);

        // NDC * (1/scale) + position
        var lss = logicalScreenSize;
        var pos = position;
        worldCoordinates.x = Math.fma(ndcX, lss.x * 0.5, pos.x);
        worldCoordinates.y = Math.fma(ndcY, lss.y * 0.5, pos.y);
    }

    public void boundsTo(AABB aabb) {
        var lss = logicalScreenSize;
        var pos = position;
        float w = lss.x;
        float h = lss.y;
        aabb.setRectangle(pos.x - w * .5, pos.y - h * .5, w, h);
    }

    public void boundsTo(Rectangle out) {
        var pos = position;
        var lss = logicalScreenSize;
        out.setSize(lss.x, lss.y).setCenter(pos.xf(), pos.yf());
    }

    public Vector2f relativize(double worldX, double worldY) {
        var pos = position;
        return tmp.set(
                (float)(worldX - pos.x),
                (float)(worldY - pos.y)
        );
    }

    public Vector2f relativize(Vector2d worldCoordinates) {
        var pos = position;
        return tmp.set(
                (float)(worldCoordinates.x - pos.x),
                (float)(worldCoordinates.y - pos.y)
        );
    }
}
