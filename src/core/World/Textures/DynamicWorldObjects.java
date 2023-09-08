package core.World.Textures;

import core.World.HitboxMap;
import core.World.Textures.StaticWorldObjects.StaticObjectsConst;
import core.World.Textures.StaticWorldObjects.StaticWorldObjects;
import java.awt.geom.Point2D;
import java.io.Serializable;
import static core.World.WorldGenerator.*;

//динамические объекты, могут иметь любые координаты внутри мира и быть перемещены когда угодно
public class DynamicWorldObjects implements Serializable {
    public int framesCount, currentFrame;
    public float x, y, animSpeed, weight, currentHp, maxHp;
    public String path;
    public long lastFrameTime = System.currentTimeMillis();
    public boolean onCamera, isJumping, isDropping, isFlying, mirrored, notForDrawing;
    public Point2D.Float motionVector = new Point2D.Float(0, 0);

    public DynamicWorldObjects(boolean isFlying, int framesCount, float animSpeed, float x, float y, float maxHp, float weight, String path) {
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
        this.weight = weight;
    }

    public DynamicWorldObjects(boolean isFlying, float weight, int framesCount, float animSpeed, float x, float maxHp, String path) {
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
        this.weight = weight;

        float y = 1;
        int sizeX = (int) Math.ceil(TextureLoader.getSize(path).width / 16f);
        sizeX += 1;

        for (int worldX = 0; worldX < sizeX; worldX++) {
            for (int worldY = 1; worldY < SizeY - 2; worldY++) {
                StaticWorldObjects objUp = getObject((int) (x / 16 + worldX), worldY + 1);
                StaticWorldObjects obj = getObject((int) (x / 16 + worldX), worldY);

                if (objUp != null && objUp.getType() != StaticObjectsConst.Types.SOLID && obj.getType() == StaticObjectsConst.Types.SOLID && obj.y > y) {
                    y = objUp.y;
                }
            }
        }
        if (HitboxMap.checkIntersInside(x, y, TextureLoader.getSize(path).width + 10, TextureLoader.getSize(path).height + 10) != null) {
            y += 16;
        }

        this.y = y;
    }

    public void jump(float impulse) {
        if (HitboxMap.checkIntersStaticD(x, y, TextureLoader.getSize(path).width, TextureLoader.getSize(path).height)) {
            motionVector.y += impulse;
        }
    }
}
