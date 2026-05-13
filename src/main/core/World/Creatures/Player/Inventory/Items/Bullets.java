package core.World.Creatures.Player.Inventory.Items;

import core.UI.Sounds.Sound;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.HitboxMap;
import core.World.ItemWeapon;
import core.World.Textures.TextureDrawing;
import core.g2d.Atlas;
import core.math.Point2i;

import java.util.ArrayList;
import java.util.Iterator;

import static core.Global.*;
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
        var curr = Inventory.getCurrent();
        if (curr != null && curr.getItem() instanceof ItemWeapon w) {
            long nowTime = System.currentTimeMillis();
            var data = curr.getOrCreateData(ItemData.Weapon::new);

            if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && nowTime - data.lastShootTime >= w.fireRate) {
                data.lastShootTime = nowTime;
                Bullets.createBullet(player.getX(), player.getY(),
                        w.ammoSpeed, w.damage, Math.abs((float) Math.toDegrees(Math.atan2(input.mousePos().y - 540, input.mousePos().x - 960)) - 180));
                Sound.playSound(w.sound, Sound.types.EFFECT, false);
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

                Point2i staticPos = HitboxMap.checkIntersInside(x, y, 8, 8);

                if (staticPos != null) {
                    var staticObject = world.getBlock(staticPos.x, staticPos.y);
                    var dynamicObject = HitboxMap.checkIntersectionsDynamic(x, y, 8, 8);

                    if (staticObject != null) {
                        float hp = world.getHp(staticPos.x, staticPos.y);
                        world.damage(staticPos.x, staticPos.y, (int) bullet.damage);
                        bulletsIter.next().damage -= hp;
                    } else if (dynamicObject != null) {
                        //todo
                        // float hp = dynamicObject.getCurrentHP();
                        // dynamicObject.incrementCurrentHP(-bullet.damage);
                        // bullet.damage -= hp;

                        // if (dynamicObject.getCurrentHP() <= 0) {
                            // player.remove(dynamicObject);
                        // }
                    }
                }
                if (bullet.damage <= 0 || bullet.x < 0 || bullet.y < 0 || bullet.x / TextureDrawing.blockSize > world.sizeX || bullet.y / TextureDrawing.blockSize > world.sizeY) {
                    bulletsIter.remove();
                }
            }
        }
    }

    public static void drawBullets() {
        if (bullets.isEmpty()) {
            return;
        }

        Atlas.Region bulletRegion = atlas.byPath("textures/items/someBullet.png");
        for (Bullets bullet : bullets) {
            // todo пути для пуль
            // if (bullet != null && !(bullet.x > DynamicObjects.getFirst().getX() + 350 || bullet.x < DynamicObjects.getFirst().getX() - 350)) {
            //     batch.draw(bulletRegion, bullet.x, bullet.y);
            // }
        }
    }
}
