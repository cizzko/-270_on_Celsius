package core.World.Creatures.Player.Inventory.Items.Weapons;

import core.World.Creatures.Player.Inventory.Items.Weapons.Ammo.Bullets;

import java.io.Serializable;

public class Weapons implements Serializable {
    public int magazineSize, currentAmmo;
    public float fireRate, damage, ammoSpeed, reloadTime, bulletSpread;
    public long lastShootTime = System.currentTimeMillis();
    public String sound, bulletPath;
    public Types type;

    public enum Types {
        EXPLOSIVE,
        BULLET
    }

    public Weapons(float fireRate, float damage, float ammoSpeed, float reloadTime, float bulletSpread, int magazineSize, String sound, String bulletPath, Types type) {
        this.fireRate = fireRate;
        this.damage = damage;
        this.ammoSpeed = ammoSpeed;
        this.reloadTime = reloadTime;
        this.bulletSpread = bulletSpread;
        this.magazineSize = magazineSize;
        this.sound = sound;
        this.type = type;
        this.bulletPath = bulletPath;
        this.currentAmmo = magazineSize;
    }

    public static void updateAmmo() {
        Bullets.updateBullets();
    }
}
