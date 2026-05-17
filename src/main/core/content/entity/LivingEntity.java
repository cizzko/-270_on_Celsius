package core.content.entity;

public interface LivingEntity
        extends Entity, WeightComponent,
                VelocityComponent, HealthComponent,
                DrawComponent {

    @Override
    default void draw() { draw(x()); }
}
