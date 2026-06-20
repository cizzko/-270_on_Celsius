package core.math;

import static core.Global.world;
import static core.WorldCoordinates.*;
import static java.lang.Math.abs;
import static java.lang.Math.fma;

public final class AABB {
    public double minX, minY;
    public double maxX, maxY;

    public float width()  { return (float)(maxX - minX); }
    public float height() { return (float)(maxY - minY); }

    public double centerX() { return minX + (maxX - minX) * .5; }
    public double centerY() { return minY + (maxY - minY) * .5; }

    public short blockMinX() { return toBlock(minX); }
    public short blockMinY() { return toBlock(minY); }

    public short blockMaxX() { return toBlock(maxX); }
    public short blockMaxY() { return toBlock(maxY); }

    public void move(Vector2f dv) {
        move(dv.x, dv.y);
    }

    public void move(float dx, float dy) {
        this.minX += dx;
        this.minY += dy;
        this.maxX += dx;
        this.maxY += dy;
    }

    public Vector2f overlapTo(AABB b, Vector2f out) {
        float dx = (float) (centerX() - b.centerX());
        float dy = (float) (centerY() - b.centerY());

        float xOverlap = fma(width() + b.width(), .5f, -abs(dx));
        float yOverlap = fma(height() + b.height(), .5f, -abs(dy));

        if (xOverlap > 0 && yOverlap > 0) {
            if (xOverlap < yOverlap) {
                // Выталкиваем по оси X
                // dx>0 значит двигаем вправо (1), иначе влево (-1)
                out.x = dx > 0 ? 1f : -1f;
                out.y = 0f;
                return out.scale(xOverlap);
            } else {
                // Выталкиваем по оси X
                // dy>0 значит двигаем вверх (1), иначе вниз (-1)
                out.x = 0f;
                out.y = dy > 0 ? 1f : -1f;
                return out.scale(yOverlap);
            }
        }

        out.x = 0f;
        out.y = 0f;
        return out;
    }

    public boolean intersects(AABB rhs) {
        return minX < rhs.maxX && maxX > rhs.minX &&
               minY < rhs.maxY && maxY > rhs.minY;
    }

    public boolean intersects(double rhsMinX, double rhsMinY, double rhsMaxX, double rhsMaxY) {
        return minX < rhsMaxX && maxX > rhsMinX &&
               minY < rhsMaxY && maxY > rhsMinY;
    }

    public boolean overlaps(double rhsMinX, double rhsMinY, double rhsMaxX, double rhsMaxY) {
        return minX <= rhsMaxX && maxX >= rhsMinX &&
               minY <= rhsMaxY && maxY >= rhsMinY;
    }

    public boolean overlaps(AABB rhs) {
        return minX <= rhs.maxX && maxX >= rhs.minX &&
               minY <= rhs.maxY && maxY >= rhs.minY;
    }

    public void setRectangle(Rectangle rect) {
        setRectangle(rect.x, rect.y, rect.width, rect.height);
    }

    public void setRectangle(double x, double y, float w, float h) {
        this.minX = x;
        this.minY = y;

        this.maxX = x + w;
        this.maxY = y + h;
    }

    public void clamp(float minX, float minY, float maxX, float maxY) {
        this.minX = Math.max(minX, this.minX);
        this.minY = Math.max(minY, this.minY);
        this.maxX = Math.min(maxX, this.maxX);
        this.maxY = Math.min(maxY, this.maxY);
    }

    public void clampToWorld() {
        clamp(0, 0, world.sizeX - 1, world.sizeY - 1);
    }

    public void clampToWorldMargin(int margin) {
        minX = Math.max(0, -margin + minX);
        minY = Math.max(0, -margin + minY);
        maxX = Math.min(world.sizeX - 1, margin + maxX);
        maxY = Math.min(world.sizeY - 1, margin + maxY);
    }

    public void expand(int margin) {
        this.minX -= margin;
        this.maxX += margin;
        this.minY -= margin;
        this.maxY += margin;
    }

    public void floorToBlock() {
        minX = toBlock(minX);
        minY = toBlock(minY);
        maxX = toBlock(maxX);
        maxY = toBlock(maxY);
    }

    public void set(AABB rhs) {
        this.minX = rhs.minX;
        this.minY = rhs.minY;
        this.maxX = rhs.maxX;
        this.maxY = rhs.maxY;
    }

    @Override
    public String toString() {
        return "AABB[" +
               "min=(" + minX +
               ", " + minY +
               "), max=(" + maxX +
               ", " + maxY +
               ")]";
    }
}
