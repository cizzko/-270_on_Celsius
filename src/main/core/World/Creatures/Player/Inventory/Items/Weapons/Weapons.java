package core.World.Creatures.Player.Inventory.Items.Weapons;

import core.World.Creatures.Player.Inventory.Items.Weapons.Ammo.Bullets;

public class Weapons {
    public int magazineSize, currentAmmo;
    public float fireRate, damage, ammoSpeed, reloadTime, bulletSpread;
    public long lastShootTime = System.currentTimeMillis();
    public String sound, bulletPath, name;
    public Types type;

    public enum Types {
        EXPLOSIVE,
        BULLET
    }

    public Weapons(float fireRate, float damage, float ammoSpeed, float reloadTime, float bulletSpread, int magazineSize, String sound, String bulletPath, String name, Types type) {
        this.fireRate = fireRate;
        this.damage = damage;
        this.ammoSpeed = ammoSpeed;
        this.reloadTime = reloadTime;
        this.bulletSpread = bulletSpread;
        this.magazineSize = magazineSize;
        this.sound = sound;
        this.type = type;
        this.bulletPath = bulletPath;
        this.name = name;
        this.currentAmmo = magazineSize;
    }

    public static void updateAmmo() {
        Bullets.updateBullets();
    }
}
