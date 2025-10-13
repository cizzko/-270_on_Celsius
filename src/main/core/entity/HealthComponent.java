package core.entity;

public interface HealthComponent {
    float getMaxHp();
    float getHp();
    default float getHpFraction() {
        return getHp() / getMaxHp();
    }
    boolean isUnbreakable();
    boolean isDead();
    void setHp(float hp);
    void damage(float d);
    void setUnbreakable(boolean unbreakable);
}
