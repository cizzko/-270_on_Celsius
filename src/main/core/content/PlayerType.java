package core.content;

import core.entity.CreatureEntity;

public class PlayerType extends CreatureType {

    public PlayerType(String id) {
        super(id);
    }

    @Override
    protected CreatureEntity constructEntity() {
        return new PlayerEntity(this);
    }
}
