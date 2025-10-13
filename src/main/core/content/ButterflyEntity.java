package core.content;

import core.World.Textures.ShadowMap;
import core.entity.BaseCreatureEntity;

import static core.Global.batch;

public class ButterflyEntity extends BaseCreatureEntity<ButterflyType> {
    private static final int maxSoarTime = 2000;

    protected short currentFrame;
    protected long lastFrameTime = System.currentTimeMillis();
    protected int animationSpeed;
    protected int flyingTime;

    protected ButterflyEntity(ButterflyType creature) {
        super(creature);
    }

    @Override
    public void init() {
        super.init();
        animationSpeed = creature.animationSpeed;
    }

    @Override
    public void update() {
        if (animationSpeed != 0 && creature.framesCount != 0 && System.currentTimeMillis() - lastFrameTime >= animationSpeed) {
            if (currentFrame >= creature.framesCount) {
                currentFrame = 0;
                lastFrameTime = System.currentTimeMillis();
                return;
            }
            lastFrameTime = System.currentTimeMillis();
            currentFrame++;
        }

        // TODO ButterflyLogic
    }

    @Override
    public void remove() {

    }

    @Override
    public void draw() {
        if (creature.framesCount == 0) {
            // batch.color(ShadowMap.getColorDynamic(this));
            // batch.draw(creature.texture, x, y);
            // batch.resetColor();
        } else {
            // todo дописать
            // drawTexture(dynamicObject.getPath() + dynamicObject.getCurrentFrame() + ".png", dynamicObject.getX(), dynamicObject.getY(), ShadowMap.getColorDynamic(), false, false);
        }
    }
}
