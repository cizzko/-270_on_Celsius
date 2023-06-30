package core.World.Textures;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import static core.World.Creatures.Physics.physicsSpeed;
import static core.World.HitboxMap.checkIntersStaticD;
import static core.World.HitboxMap.checkIntersStaticU;

//динамические объекты, могут иметь любые координаты внутри мира и быть перемещены когда угодно
public class DynamicWorldObjects implements Serializable {
    public int framesCount, currentFrame;
    public float x, y, animSpeed, dropSpeed;
    public String path;
    public boolean onCamera, isPlayer, isJumping, isDropping;

    public DynamicWorldObjects(int framesCount, float animSpeed, String path, boolean isPlayer, float x, float y) {
        this.framesCount = framesCount;
        this.animSpeed = animSpeed;
        this.path = path;
        this.currentFrame = 1;
        this.onCamera = true;
        this.isJumping = false;
        this.isDropping = false;
        this.isPlayer = isPlayer;
        this.x = x;
        this.y = y;
    }

    public void jump(float maxHeight, double gravitation) {
        if (!isJumping && !isDropping) {
            isJumping = true;
            new Thread(() -> {

                float y0 = y;
                float yMax = y0 + maxHeight; //высота прыжка
                double g = gravitation - (physicsSpeed * 200); //гравитация
                double timeToMax = Math.sqrt((2 * (yMax - y0)) / g);
                double totalTime = 2 * timeToMax;
                LocalDateTime startTime = LocalDateTime.now();

                while (true) {
                    LocalDateTime currentTime = LocalDateTime.now();
                    double elapsedTime = Duration.between(startTime, currentTime).toMillis() / 1000.0;

                    if (elapsedTime >= totalTime) {
                        y = y0;
                        isJumping = false;
                        break;
                    } else if (elapsedTime >= timeToMax) {
                        y = (float) (y0 + (yMax - y0) - 0.5 * g * Math.pow(elapsedTime - timeToMax, 2));
                    } else {
                        y = (float) (y0 + 0.5 * g * Math.pow(elapsedTime, 2));
                    }

                    if ((!checkIntersStaticD(x, y, 24, 24) && elapsedTime >= totalTime / 2) || checkIntersStaticU(x, y, 24, 24)) {
                        isJumping = false;
                        break;
                    }
                }
            }).start();
        }
    }
}
