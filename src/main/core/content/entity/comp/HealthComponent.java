package core.content.entity.comp;

public interface HealthComponent {
    float maxHp();
    float hp();
    default float hpFract() {
        return hp() / maxHp();
    }

    boolean isDead();

    void setHp(float hp);
    void damage(float d, DamageSource source);

    enum DamageSource { UNKNOWN, FALL }
}
