package core.content;

import core.entity.BaseCreatureEntity;

public class PlayerEntity extends BaseCreatureEntity<PlayerType> {
    protected PlayerEntity(PlayerType creature) {
        super(creature);
    }

    @Override
    protected void onDamage(float d) {
        // TODO
    }

    @Override
    public void update() {

    }

    @Override
    public void remove() {

    }
}
