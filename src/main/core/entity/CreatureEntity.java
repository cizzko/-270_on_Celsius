package core.entity;

import core.content.creatures.CreatureType;
import core.math.Rectangle;

public interface CreatureEntity extends Entity, DrawComponent, PositionComponent, VelocityComponent, HealthComponent {
    short getId();
    void setId(int id);

    CreatureType getCreature();

    void getHitboxTo(Rectangle out);

    boolean hasGravity();
    boolean hasFloor();
}
