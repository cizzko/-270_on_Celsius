package core.World.Textures;

import core.World.HitboxMap;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import static core.World.Creatures.Physics.physicsSpeed;
import static core.World.HitboxMap.checkIntersStaticD;
import static core.World.HitboxMap.checkIntersStaticU;
import static core.World.WorldGenerator.*;

//динамические объекты, могут иметь любые координаты внутри мира и быть перемещены когда угодно
public class DynamicWorldObjects implements Serializable {
    public int framesCount, currentFrame, uniqueId;
    public float x, y, animSpeed, dropSpeed, currentHp, maxHp;
    public String path;
    public long lastFrameTime = System.currentTimeMillis();
    public boolean onCamera, isJumping, isDropping, isFlying, mirrored, notForDrawing;

    public DynamicWorldObjects(boolean isFlying,int framesCount, float animSpeed, float x, float y, float maxHp, int uniqueId, String path) {
        this.framesCount = framesCount;
        this.animSpeed = animSpeed;
        this.path = path;
        this.currentFrame = 1;
        this.isFlying = isFlying;
        this.onCamera = true;
        this.mirrored = false;
        this.isJumping = false;
        this.isDropping = false;
        this.x = x;
        this.y = y;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.uniqueId = uniqueId;
    }

    public DynamicWorldObjects(boolean isFlying, int framesCount, float animSpeed, float x, float maxHp, int uniqueId, String path) {
        this.framesCount = framesCount;
        this.animSpeed = animSpeed;
        this.path = path;
        this.currentFrame = 1;
        this.isFlying = isFlying;
        this.onCamera = true;
        this.mirrored = false;
        this.isJumping = false;
        this.isDropping = false;
        this.x = x;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.uniqueId = uniqueId;

        float y = 1;
        int sizeX = (int) Math.ceil(TextureLoader.getSize(path).width / 16f);
        sizeX += 1;

        for (int worldX = 0; worldX < sizeX; worldX++) {
            for (int worldY = 1; worldY < SizeY - 2; worldY++) {
                StaticWorldObjects objUp = StaticObjects[(int) (x / 16 + worldX)][worldY + 1];
                StaticWorldObjects obj = StaticObjects[(int) (x / 16 + worldX)][worldY];

                if (!objUp.solid && obj.solid && obj.y > y) {
                    y = objUp.y;
                }
            }
        }
        if (HitboxMap.checkIntersectionsInside(x, y, TextureLoader.getSize(path).width + 10, TextureLoader.getSize(path).height + 10) != null) {
            y += 16;
        }

        this.y = y;
    }

    public void jump(float maxHeight, double gravitation) {
        if (!isJumping && !isDropping) {
            isJumping = true;
            new Thread(() -> {

                float y0 = y;
                float yMax = y0 + maxHeight; //высота прыжка
                double g = gravitation + (physicsSpeed / 2f); //гравитация
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
