package core.World.Textures;

import java.io.Serializable;

//динамические объекты, могут иметь любые координаты внутри мира и быть перемещены когда угодно
public class DynamicWorldObjects implements Serializable {
    public int framesCount, currentFrame;
    public float x, y, animSpeed;
    public String path;
    public boolean onCamera, isPlayer, isJumping;

    public DynamicWorldObjects(int framesCount, float animSpeed, String path, boolean isPlayer, float x, float y) {
        this.framesCount = framesCount;
        this.animSpeed = animSpeed;
        this.path = path;
        this.currentFrame = 1;
        this.onCamera = true;
        this.isJumping = false;
        this.isPlayer = isPlayer;
        this.x = x;
        this.y = y;
    }
}
