package core.content.entity;

import core.content.entity.comp.DrawComponent;
import core.content.entity.comp.HealthComponent;
import core.content.entity.comp.PhysicalBody;

public interface LivingEntity
        extends Entity,
                PhysicalBody,
                HealthComponent,
                DrawComponent {

}
