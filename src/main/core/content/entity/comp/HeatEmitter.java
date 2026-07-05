package core.content.entity.comp;

import core.content.entity.Entity;

public interface HeatEmitter extends Entity {
    default int heatRadius() { return 0; }

    //потом будет динамически, когда (если) появится одежда
    default float heatEmitting() { return 0; }
    default float heatTransfer() { return 0; }

    default void addHeat(float heat) {}
    default float getHeat() { return 0; }

    default boolean isEmitting() { return true; }
}