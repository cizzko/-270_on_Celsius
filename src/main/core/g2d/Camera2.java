package core.g2d;

import core.math.Mat3;
import core.math.Vector2f;

public class Camera2 {
    public final Vector2f position = new Vector2f();
    public final Mat3 projection = new Mat3();

    private float width, height;

    public void update() {
        projection.setOrthographic(
                position.x - width / 2f, position.y - height / 2f,
                width, height
        );
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public void setToOrthographic(float width, float height) {
        position.set(width / 2f, height / 2f);
        resizeViewport(width, height);
    }

    public void resizeViewport(float width, float height) {
        this.width = width;
        this.height = height;

        update();
    }
}
