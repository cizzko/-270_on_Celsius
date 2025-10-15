package core.content;

import core.World.ContentLoader;
import core.entity.CreatureEntity;

public class ButterflyType extends CreatureType {

    public int animationSpeed, framesCount;

    public ButterflyType(String id) {
        super(id);
    }

    @Override
    public void load(ContentLoader cnt) {
        super.load(cnt);
        this.animationSpeed = cnt.node().path("AnimationSpeed").asInt();
        this.framesCount = cnt.node().path("FramesCount").asInt();
    }

    @Override
    protected CreatureEntity constructEntity() {
        return new ButterflyEntity(this);
    }
}
