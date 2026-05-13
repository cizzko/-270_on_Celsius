package core.World.Creatures;

import core.PlayGameScene;
import core.Time;
import core.World.HitboxMap;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.entity.CreatureEntity;
import core.math.Point2i;
import core.math.Rectangle;
import core.math.Vector2f;

import java.util.BitSet;

import static core.Global.*;
import static core.World.Creatures.DynamicWorldObjects.GAP;
import static core.World.Creatures.Player.Player.*;
import static core.World.Textures.TextureDrawing.blockSize;
import static core.World.WorldGenerator.WorldGenerator.intersDamageMultiplier;
import static core.World.WorldGenerator.WorldGenerator.minVectorIntersDamage;
import static java.lang.Math.abs;

public class Physics {
    private static final float ANSWER = 42; // хихи, хаха
    public static final float GRAVITY = 1.25f * ANSWER * 1e-4f;
    public static final short swap = 25;
    public static final float FRICTION_FACTOR = 0.97f / 76f;

    public static void updatePhysics(PlayGameScene scene) {
        if (scene.isPaused()) {
            return;
        }
        update();
    }

    static final Rectangle hitbox = new Rectangle();
    static final Rectangle entityHitbox = new Rectangle();
    static final Rectangle blockHitbox = new Rectangle();
    static final Vector2f tmp1 = new Vector2f();

    private static Vector2f overlap(Rectangle a, Rectangle b) {
        float penetration = 0f;

        float ax = a.x + a.width / 2, bx = b.x + b.width / 2;
        float nx = ax - bx;
        float aex = a.width / 2, bex = b.width / 2;

        float xoverlap = aex + bex - abs(nx);
        if (abs(xoverlap) > 0) {
            float aey = a.height / 2, bey = b.height / 2;

            float ay = a.y + a.height / 2, by = b.y + b.height / 2;
            float ny = ay - by;
            float yoverlap = aey + bey - abs(ny);
            if (abs(yoverlap) > 0) {
                if (abs(xoverlap) < abs(yoverlap)) {
                    tmp1.x = nx < 0 ? 1 : -1;
                    tmp1.y = 0;
                    penetration = xoverlap;
                } else {
                    tmp1.x = 0;
                    tmp1.y = ny < 0 ? 1 : -1;
                    penetration = yoverlap;
                }
            }
        }

        float m = Math.max(penetration, 0.0f);

        tmp1.x *= -m;
        tmp1.y *= -m;

        return tmp1;
    }

    private static void moveDelta(CreatureEntity entity, float deltaX, float deltaY) {
        entity.getHitboxTo(hitbox);
        entity.getHitboxTo(entityHitbox);
        entityHitbox.x += deltaX;
        entityHitbox.y += deltaY;

        int minX = (int) Math.floor(entityHitbox.x / blockSize);
        int minY = (int) Math.floor(entityHitbox.y / blockSize);

        int maxX = (int) Math.floor((entityHitbox.x + entityHitbox.width) / blockSize);
        int maxY = (int) Math.floor((entityHitbox.y + entityHitbox.height) / blockSize);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                var block = world.getBlock(x, y);
                if (block == null || block.type == StaticObjectsConst.Type.SOLID) {
                    blockHitbox.set(x * blockSize, y * blockSize, blockSize, blockSize);

                    if (blockHitbox.overlaps(entityHitbox)) {
                        var v = overlap(entityHitbox, blockHitbox);
                        entityHitbox.x += v.x;
                        entityHitbox.y += v.y;
                    }
                }
            }
        }

        entity.setPosition(entity.getX() + entityHitbox.x - hitbox.x, entity.getY() + entityHitbox.y - hitbox.y);
    }

    private static void decrementHp(DynamicWorldObjects entity, float dt) {
        float vectorX = dt * entity.getMotionVectorX();
        float vectorY = dt * entity.getMotionVectorY();

        if (vectorX > minVectorIntersDamage || vectorX < -minVectorIntersDamage ||
                vectorY > minVectorIntersDamage || vectorY < -minVectorIntersDamage) {
            Point2i[] staticObjectPoint = HitboxMap.checkIntersOutside(entity.getX() + vectorX * 2, entity.getY() + vectorY, entity.getTexture().width(), entity.getTexture().height() + 4);

            if (staticObjectPoint != null) {
                float damage = 0;
                for (Point2i point : staticObjectPoint) {
                    var block = world.getBlock(point.x, point.y);
                    float currentDamage = ((((block.resistance / 100) * block.density)
                            + (entity.getWeight() + (Math.max(abs(vectorY), abs(vectorX)) - minVectorIntersDamage)) * intersDamageMultiplier))
                            / staticObjectPoint.length;

                    damage += currentDamage;
                    int blockDamage = (int) (currentDamage + (block.resistance / 100) * block.density) / staticObjectPoint.length;
                    world.damage(point.x, point.y, blockDamage);
                }
                entity.incrementCurrentHP(-damage);

                // todo переписать
                if (entity.getTexture().name().toLowerCase().contains("player")) {
                    lastDamage = (int) damage;
                    lastDamageTime = System.currentTimeMillis();
                }
                if (entity.getCurrentHP() <= 0 && !entity.getTexture().name().toLowerCase().contains("player")) {
                    // DynamicObjects.remove(entity);
                //todo
                }
            }
        }
    }

    static final float STEPS = 1f / Time.ONE_SECOND;

    private static void update() {
        player.updateInput();
        entityPool.update();

        // Физика не будет оставаться на главном потоке, но пока это прототип.

        // Тут я обезопасил кусок кода от пролагов. Если Time.delta >= STEPS(=Time.ONE_SECOND)
        // то это значит, что игра и вся её логика зависла на секунду. Для каких-нибудь заводов это
        // может быть не столь критично (нет, критично. Может тогда вынести подобный цикл по фиксированным интервалам в самых верх игрового цикла?)
        // Но для физики это может быть ещё как критично, поскольку на dt домножаются вектора скорости (и ускорения)
        // Да, значение фиксированного интервала в 1 секунду это много (в масштабах игры).
        //                                                                      (Изменю позже)
        float dt = Time.delta;
        while (dt >= STEPS) {
            simulate(STEPS);
            dt -= STEPS;
        }

        simulate(dt);
    }

    // Ле, куда летишь?
    static final float maxSpeed = blockSize * 1.5f;
    // Минимальное смещение, при котором происходит движение. Не вижу смысла сжигать процессор ради меньших значений
    static final float moveThreshold = GAP;

    private static void simulate(float dt) {
        for (var ent : entityPool.entities().values()) {
            if (ent.getX() > (world.sizeX - swap) * blockSize) {
                ent.setX(swap * blockSize);
                camera.position.set(ent.getX() + 32, ent.getY() + 200);
            } else if (ent.getX() < swap * blockSize) {
                ent.setX((world.sizeX - swap) * blockSize);
                camera.position.set(ent.getX() + 32, ent.getY() + 200);
            }

            if (ent == player && noClip) {
                continue;
            }

            boolean hasFloor = ent.hasFloor();

            Vector2f vel = ent.getVelocity();

            if (!hasFloor) {
                vel.y -= ent.getCreature().weight * GRAVITY * dt;
            }

            float k = calculateFriction(ent);
            float perSecondRetention = Math.clamp(1.0f - k * ent.getCreature().weight * FRICTION_FACTOR, 0.0f, 1.0f);
            float retentionForDt = (float) Math.pow(perSecondRetention, dt);
            vel.x *= retentionForDt;


            // TODO Тут или в логике игрока (Player#update()) должен быть расчёт силы удара.
            //      Как я описал в ЛС, это F=m*a, то есть можно нужно рассчитать ускорение
            //      как изменение скорости за время и умножить на массу игрока. Так мы получим численное нечто,
            //      что можно в дальнейшем перевести в hp. Урон блокам, на которые падает игрок, равносилен урону игрока.
            //      По 3 закону Ньютона жеж)

            if (abs(vel.x) >= maxSpeed) {
                vel.x = Math.signum(vel.x) * maxSpeed;
            }
            if (abs(vel.y) >= maxSpeed) {
                vel.y = Math.signum(vel.y) * maxSpeed;
            }
            move(ent, dt);

            if (vel.y < 0 && hasFloor) {
                vel.y = 0;
            }
            if (abs(vel.x) < moveThreshold) vel.x = 0;
        }
    }

    // Алгоритм движение основам на том, что сделан в Mindustry, но есть идеи как его улучшить.
    // Данная реализация в цикле отнимает от вектора скорости MOVEMENT_SEGMENT = 2f/blockSize
    // То есть с каждой итерацией смещение будет происходить на всё меньшее приращение. Этот подход
    // позволяет добиваться более точного определения коллизий.
    // Очевиден вопрос: почему именно 2f/blockSize и что значит GAP?
    // Ответ: GAP это насколько в ширину и высоту хитбокс больше, чем сама текстура. Это позволяет элегантно решить
    // проблему с округлением координат, что в свою очередь избавляет от возможной "тряски" при движении
    private static final float MOVEMENT_SEGMENT = GAP;

    private static void move(CreatureEntity ent, float dt) {
        var vel = ent.getVelocity();
        float deltax = vel.x * dt, deltay = vel.y * dt;

        while (abs(deltax) > moveThreshold) {
            float sgn = Math.signum(deltax);
            moveDelta(ent, Math.min(abs(deltax), MOVEMENT_SEGMENT) * sgn, 0);

            if (abs(deltax) >= MOVEMENT_SEGMENT) {
                deltax -= MOVEMENT_SEGMENT * sgn;
            } else {
                deltax = 0f;
            }
        }

        while (abs(deltay) > moveThreshold) {
            float sgn = Math.signum(deltay);
            moveDelta(ent, 0, Math.min(abs(deltay), MOVEMENT_SEGMENT) * sgn);

            if (abs(deltay) >= MOVEMENT_SEGMENT) {
                deltay -= MOVEMENT_SEGMENT * sgn;
            } else {
                deltay = 0f;
            }
        }

        if (abs(deltax) == 0) {
            vel.x = 0;
        }
        if (abs(deltay) == 0) {
            vel.y = 0;
        }
    }

    private static float calculateFriction(CreatureEntity ent) {
        ent.getHitboxTo(entityHitbox);

        int minX = (int) Math.floor(entityHitbox.x / blockSize);
        int minY = (int) Math.floor((entityHitbox.y - GAP) / blockSize);

        int maxX = (int) Math.floor((entityHitbox.x + entityHitbox.width) / blockSize);
        int maxY = (int) Math.floor((entityHitbox.y + entityHitbox.height) / blockSize);

        float resistance = 100;
        BitSet appliedResistances = new BitSet();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                var block = world.getBlock(x, y);
                if (block != null && block.resistance > 0) {
                    blockHitbox.set(x * blockSize, y * blockSize, blockSize, blockSize);

                    if (blockHitbox.overlaps(entityHitbox)) {
                        int blockId = content.getBlockIdByType(block);
                        if (!appliedResistances.get(blockId)) {
                            appliedResistances.set(blockId);

                            resistance = Math.min(resistance, block.resistance);
                        }
                    }
                }
            }
        }
        float friction = resistance / 100f;
        if (friction > 1) {
            friction = 1;
        }

        return friction;
    }
}
