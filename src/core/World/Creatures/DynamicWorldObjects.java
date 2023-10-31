package core.World.Creatures;

import core.World.HitboxMap;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.Textures.TextureLoader;
import java.awt.geom.Point2D;
import java.io.Serializable;
import static core.World.WorldGenerator.*;

//динамические объекты, могут иметь любые координаты внутри мира и быть перемещены когда угодно
public class DynamicWorldObjects implements Serializable {
    public int framesCount, currentFrame;
    public float x, y, animSpeed, weight, currentHp, maxHp;
    public String path;
    public long lastFrameTime = System.currentTimeMillis();
    public boolean isFlying, mirrored, notForDrawing, oneoffAnimation;
    public Point2D.Float motionVector = new Point2D.Float(0, 0);

    public DynamicWorldObjects(boolean oneoffAnimation, boolean isFlying, int framesCount, float animSpeed, float x, float y, float maxHp, float weight, String path) {
        this.oneoffAnimation = oneoffAnimation;
        this.framesCount = framesCount - 1;
        this.animSpeed = animSpeed;
        this.path = path;
        this.currentFrame = 0;
        this.isFlying = isFlying;
        this.mirrored = false;
        this.x = x;
        this.y = y;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.weight = weight;
    }

    public DynamicWorldObjects(boolean oneoffAnimation, boolean isFlying, float weight, int framesCount, float animSpeed, float x, float maxHp, String path) {
        this.oneoffAnimation = oneoffAnimation;
        this.framesCount = framesCount - 1;
        this.animSpeed = animSpeed;
        this.path = path;
        this.currentFrame = 0;
        this.isFlying = isFlying;
        this.mirrored = false;
        this.x = x;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.weight = weight;

        float y = 1;
        int sizeX = (int) Math.ceil(TextureLoader.getSize(path).width / 16f);
        sizeX += 1;

        for (int worldX = 0; worldX < sizeX; worldX++) {
            for (int worldY = 1; worldY < SizeY - 2; worldY++) {
                short objUp = getObject((int) (x / 16 + worldX), worldY + 1);
                short obj = getObject((int) (x / 16 + worldX), worldY);

                if (StaticWorldObjects.getType(objUp) != StaticObjectsConst.Types.SOLID && StaticWorldObjects.getType(obj) == StaticObjectsConst.Types.SOLID && findY((int) (x / 16 + worldX), worldY) > y) {
                    y = findY((int) (x / 16 + worldX), worldY + 1);
                }
            }
        }
        if (HitboxMap.checkIntersInsideAll(x, y, TextureLoader.getSize(path).width + 10, TextureLoader.getSize(path).height + 10) > 0) {
            y += 16;
        }

        this.y = y;
    }

    public void jump(float impulse) {
        if (HitboxMap.checkIntersStaticD(x, y, TextureLoader.getSize(path).width, TextureLoader.getSize(path).height) && motionVector.y == 0) {
            motionVector.y += impulse;
        }
    }
}
