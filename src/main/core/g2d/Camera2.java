package core.g2d;

import core.math.Mat3;
import core.math.Vector2f;

public class Camera2 {
    public final Vector2f position = new Vector2f();
    public final Mat3 projection = new Mat3(), invProjection = new Mat3();

    private float width, height;

    public void update() {
        projection.setOrthographic(
                position.x - width / 2f, position.y - height / 2f,
                width, height
        );
        invProjection.set(projection).inv();
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

    // Перевод вектора с координатами мира в координаты экрана
    public Vector2f project(Vector2f worldCoordinates) {
        worldCoordinates.mul(projection);
        worldCoordinates.x = width * (worldCoordinates.x + 1) / 2;
        worldCoordinates.y = height * (worldCoordinates.y + 1) / 2;
        return worldCoordinates;
    }
    // Перевод вектора с координатами экрана в координаты мира
    public Vector2f unproject(Vector2f screenCoordinates) {
        screenCoordinates.x = (2 * screenCoordinates.x) / width - 1;
        screenCoordinates.y = (2 * screenCoordinates.y) / height - 1;
        screenCoordinates.mul(invProjection);
        return screenCoordinates;
    }
}
