package core.entity;

import core.content.CreatureType;
import core.math.Rectangle;

public interface CreatureEntity extends Entity, DrawComponent, PositionComponent, VelocityComponent, HealthComponent {
    short getId();

    CreatureType getCreature();

    void getHitbox(Rectangle out);

    boolean hasGravity();
}
