package core.content.creatures;

public class PlayerType extends CreatureType {

    public PlayerType(String id) {
        super(id);
    }

    @Override
    protected PlayerEntity constructEntity() {
        return new PlayerEntity(this);
    }
}
