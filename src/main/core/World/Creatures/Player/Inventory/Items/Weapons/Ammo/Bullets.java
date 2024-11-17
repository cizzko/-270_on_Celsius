package core.World.Creatures.Player.Inventory.Items.Weapons.Ammo;

import core.UI.Sounds.Sound;
import core.World.Creatures.DynamicWorldObjects;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.Creatures.Player.Inventory.Items.Weapons.Weapons;
import core.World.HitboxMap;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.Textures.TextureDrawing;
import core.g2d.Atlas;
import core.math.Point2i;

import java.util.ArrayList;
import java.util.Iterator;

import static core.Global.*;
import static core.World.WorldGenerator.*;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class Bullets {
    public float bulletSpeed, damage, angle, x, y;
    public static ArrayList<Bullets> bullets = new ArrayList<>();

    private Bullets(float x, float y, float bulletSpeed, float damage, float angle) {
        this.x = x;
        this.y = y;
        this.bulletSpeed = bulletSpeed;
        this.damage = damage;
        this.angle = angle;
    }

    public static void createBullet(float x, float y, float bulletSpeed, float damage, float angle) {
        bullets.add(new Bullets(x, y, bulletSpeed, damage, angle));
    }

    public static void updateBullets() {
        if (Inventory.currentObjectType == Items.Types.WEAPON) {
            Weapons weapon = Inventory.getCurrent().weapon;

            if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && System.currentTimeMillis() - weapon.lastShootTime >= weapon.fireRate) {
                weapon.lastShootTime = System.currentTimeMillis();
                Bullets.createBullet(DynamicObjects.getFirst().getX(), DynamicObjects.getFirst().getY(), weapon.ammoSpeed, weapon.damage, Math.abs((float) Math.toDegrees(Math.atan2(input.mousePos().y - 540, input.mousePos().x - 960)) - 180));
                Sound.playSound(weapon.sound, Sound.types.EFFECT, false);
            }
        }

        Iterator<Bullets> bulletsIter = bullets.iterator();
        while (bulletsIter.hasNext()) {
            Bullets bullet = bulletsIter.next();

            if (bullet != null) {
                float deltaX = (float) (bullet.bulletSpeed * Math.cos(Math.toRadians(bullet.angle)));
                float deltaY = (float) (bullet.bulletSpeed * Math.sin(Math.toRadians(bullet.angle)));
                float x = bullet.x - deltaX;
                float y = bullet.y + deltaY;

                bullet.x -= deltaX;
                bullet.y += deltaY;
                bullet.damage -= 0.01f;

                Point2i staticObjectPoint = HitboxMap.checkIntersInside(x, y, 8, 8);

                if (staticObjectPoint != null) {
                    short staticObject = getObject(staticObjectPoint.x, staticObjectPoint.y);
                    DynamicWorldObjects dynamicObject = HitboxMap.checkIntersectionsDynamic(x, y, 8, 8);

                    if (staticObject > 0) {
                        float hp = StaticWorldObjects.getHp(staticObject);
                        setObject(staticObjectPoint.x, staticObjectPoint.y, StaticWorldObjects.decrementHp(staticObject, (int) bullet.damage), false);
                        bulletsIter.next().damage -= hp;

                        if (getObject(staticObjectPoint.x, staticObjectPoint.y) <= 0) {
                            destroyObject(staticObjectPoint.x, staticObjectPoint.y);
                        }
                    } else if (dynamicObject != null) {
                        float hp = dynamicObject.getCurrentHP();
                        dynamicObject.incrementCurrentHP(-bullet.damage);
                        bullet.damage -= hp;

                        if (dynamicObject.getCurrentHP() <= 0) {
                            DynamicObjects.remove(dynamicObject);
                        }
                    }
                }
                if (bullet.damage <= 0 || bullet.x < 0 || bullet.y < 0 || bullet.x / TextureDrawing.blockSize > SizeX || bullet.y / TextureDrawing.blockSize > SizeY) {
                    bulletsIter.remove();
                }
            }
        }
    }

    public static void drawBullets() {
        Atlas.Region bulletRegion = atlas.byPath("World/Items/someBullet.png");

        for (Bullets bullet : bullets) {
            // todo пути для пуль
            if (bullet != null && !(bullet.x > DynamicObjects.getFirst().getX() + 350 || bullet.x < DynamicObjects.getFirst().getX() - 350)) {
                batch.draw(bulletRegion, bullet.x, bullet.y);
            }
        }
    }
}
