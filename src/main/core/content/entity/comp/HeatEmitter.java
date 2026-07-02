package core.content.entity.comp;

import core.content.entity.Entity;

public interface HeatEmitter extends Entity {
    default int heatRadius() { return 0; }
    default float heatPower() { return 0; }

    default boolean isEmitting() { return true; }
}