package core.World.Textures;

public class DynamicWorldObjects {
    public int framesCount, currentFrame;
    public float x, y, animSpeed;
    public String path;
    public boolean onCamera, isPlayer, isJumping;

    public DynamicWorldObjects(int framesCount, float animSpeed, String path, boolean onCamera, boolean isPlayer, float x, float y) {
        this.framesCount = framesCount;
        this.animSpeed = animSpeed;
        this.path = path;
        this.currentFrame = 1;
        this.onCamera = onCamera;
        this.isJumping = false;
        this.isPlayer = isPlayer;
        this.x = x;
        this.y = y;
    }
}
