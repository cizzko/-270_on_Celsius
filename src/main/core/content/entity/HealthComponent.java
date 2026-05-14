package core.content.entity;

public interface HealthComponent {
    float getMaxHp();
    float getHp();
    default float getHpFraction() {
        return getHp() / getMaxHp();
    }
    boolean isUnbreakable();
    boolean isDead();
    void setHp(float hp);
    default void damage(float d) { damage(d, DamageSource.UNKNOWN); }
    void damage(float d, DamageSource source);
    void setUnbreakable(boolean unbreakable);

    enum DamageSource { UNKNOWN, FALL }
}
