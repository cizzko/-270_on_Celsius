package core.World;

import java.io.Serializable;

import static core.Global.assets;

public non-sealed class ItemWeapon extends Item implements Serializable {
    public int magazineSize;
    public float fireRate, damage, ammoSpeed, reloadTime, bulletSpread;
    public long lastShootTime = System.currentTimeMillis();
    public String sound, bulletPath;
    public Types type;

    public enum Types {
        EXPLOSIVE,
        BULLET
    }

    public ItemWeapon(String id) {
        super(id);
    }

    @Override
    public void load(ContentLoader cnt) {
        super.load(cnt);
        this.fireRate = (float) cnt.node().get("FireRate").asDouble(100);
        this.damage = (float) cnt.node().path("Damage").asDouble(100);
        this.ammoSpeed = (float) cnt.node().path("AmmoSpeed").asDouble(100);
        this.reloadTime = (float) cnt.node().path("ReloadTime").asDouble(100);
        this.bulletSpread = (float) cnt.node().path("BulletSpread").asDouble(0);
        this.magazineSize = cnt.node().path("MagazineSize").asInt(10);
        // TODO тут должны загружаться [XXX]Unresolved типы и потом проверяться. То есть звук и пуля(?) соответственно
        // this.sound = assets.assetsDir().resolve(cnt.node().path("Sound").asText(null));
        // this.bulletPath = assets.assetsDir(cnt.node().path("BulletPath").asText("World/Items/someBullet.png"));
        this.type = ItemWeapon.Types.valueOf(cnt.node().path("type").asText("BULLET"));
    }
}
