package core.World.Creatures;

import core.PlayGameScene;
import core.Time;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.Textures.ShadowMap;
import core.content.entity.*;
import core.math.Rectangle;
import core.math.Vector2f;
import core.util.FixedBitset;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import static core.Global.*;
import static core.World.Creatures.Player.Player.*;
import static core.World.Textures.TextureDrawing.blockSize;
import static core.content.entity.DrawComponent.GAP;
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
        updateMovement();
        processCollisions();
    }

    static final Rectangle hitbox = new Rectangle();
    static final Rectangle entityHitbox = new Rectangle();
    static final Rectangle blockHitbox = new Rectangle();
    static final Vector2f tmp1 = new Vector2f();
    static final Vector2f normal = new Vector2f();

    private static void processCollisions() {
        completedCollisions.clear();
        entityPool.updatePositions();

        entityPool.entities().values().forEach(me -> {
            entityPool.worldIndex().findIntersections(me, them -> {
                var meColId   = combine(me.getId(), them.getId());
                var themColId = combine(them.getId(), me.getId());
                if (completedCollisions.contains(meColId) || completedCollisions.contains(themColId)) {
                    return;
                }

                me.onCollide(them);
                if (!me.isRemoved()) {
                    them.onCollide(me);
                }
                // var collisionResult = meResult.combine(themResult);
                // if (collisionResult == HitboxComponent.CollisionResult.RESISTANT) {
                //     var offsetVec = overlap(hitbox, entityHitbox);
                //     me.setPosition(
                //             me.getX() + offsetVec.x,
                //             me.getY() + offsetVec.y);
                //     me.getHitboxTo(hitbox);
                // }

                completedCollisions.add(meColId);
                completedCollisions.add(themColId);
            });
        });
    }

    static final IntOpenHashSet completedCollisions = new IntOpenHashSet();

    static int combine(short a, short b) {
        return HashCommon.mix(a << 16 | b & 0xffff);
    }

    private static void updateEntities() {
        for (var ent : entityPool.entities().values()) {
            ent.update();
        }
    }

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
        return tmp1.scale(-m);
    }

    private static void moveDelta(LivingEntity entity, float deltaX, float deltaY) {
        entity.getHitboxTo(hitbox);
        entity.getHitboxTo(entityHitbox);
        entityHitbox.x += deltaX;
        entityHitbox.y += deltaY;

        int minX = (int) Math.floor(entityHitbox.x / blockSize);
        int minY = (int) Math.floor(entityHitbox.y / blockSize);

        int maxX = (int) Math.floor((entityHitbox.x + entityHitbox.width) / blockSize);
        int maxY = (int) Math.floor((entityHitbox.y + entityHitbox.height) / blockSize);

        Vector2f vel = entity.getVelocity();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                int blockId = world.getBlockId(x, y);
                if (blockId <= 0)
                    continue;
                var block = content.blocksRegistry.typeById(blockId);
                if (block.type != StaticObjectsConst.Type.SOLID) {
                    continue;
                }
                blockHitbox.set(x * blockSize, y * blockSize, blockSize, blockSize);

                if (blockHitbox.overlaps(entityHitbox)) {
                    var offsetVec = overlap(entityHitbox, blockHitbox);

                    float penetration = offsetVec.hypot();
                    if (penetration > 0f) {
                        normal.x = -offsetVec.x / penetration; // так как v = normal * -penetration
                        normal.y = -offsetVec.y / penetration;
                        if (Math.abs(normal.y) >= Math.abs(normal.x)) {
                            if (normal.y < 0) {
                                normal.x = -normal.x;
                                normal.y = -normal.y;
                            }
                        } else { // боковые столкновения: сделаем normal.x положительным (направление вправо от блока к сущности)
                            if (normal.x < 0) {
                                normal.x = -normal.x;
                                normal.y = -normal.y;
                            }
                        }
                    } else {
                        normal.set(0, 0);
                    }


                    final float DAMAGE_MULTIPLIER = 5f/550;
                    final float IMPACT_MULTIPLIER = 0.25f;
                    final float FALL_DAMAGE_SPEED_THRESHOLD = 15.5f;
                    final float FALL_DAMAGE_MULTIPLIER = 10f / 1000.054f;

                    // проекция скорости на нормаль
                    float vn = normal.projectTo(vel.x, vel.y);

                    // if (vel.x > 0)
                    //     System.out.println("penetration = " + penetration + ", vn = " + vn + ", normal = " + normal + ", vel = " + vel);

                    if (vn < 0 && vel.y < -FALL_DAMAGE_SPEED_THRESHOLD && hasFloor(entityHitbox)) {
                        float impact = entity.getWeight() * Math.abs(vel.y);
                        int damage = (int) Math.floor(impact * FALL_DAMAGE_MULTIPLIER);

                        // System.out.println("(VERTICAL) impact=" + impact + ", damage=" + damage + " (" + x + "," + y + ") vel=" + vel);

                        // TODO: Необходимо что-то придумать с распределением урона по площади контакта
                        //       Это явно не будет сделано здесь из-за направления обхода и применения урона
                        if (damage > 0) {
                            entity.damage(damage, HealthComponent.DamageSource.FALL);
                            // world.damage(x, y, damage);
                            vel.y = 0;
                        }
                    }

                    // if (Math.abs(vn) >= 15) {
                    //     // линейный импульс P = m * |vn|
                    //     float weight = entity.getWeight();
                    //     float impact = weight * Math.abs(vn);
                    //     int damage = (int)Math.floor((impact - weight * IMPACT_MULTIPLIER) * DAMAGE_MULTIPLIER);
                    //     System.out.println("impact=" + impact + " | damage=" + damage + " | vn=" + vn+ " | velX=" + vel.x);
                    //     if (damage > 0) {
                    //         // entity.damage(damage);
                    //         // world.damage(x, y, damage);
                    //     }
                    //
                    //     vel.x -= vn * normal.x;
                    //     // vel.y -= vn * normal.y;
                    // }

                    entityHitbox.x += offsetVec.x;
                    entityHitbox.y += offsetVec.y;
                }
            }
        }

        entity.setPosition(
                entity.getX() + entityHitbox.x - hitbox.x,
                entity.getY() + entityHitbox.y - hitbox.y);
    }

    private static boolean hasFloor(Rectangle entityHitbox) {
        int minX = (int) Math.floor(entityHitbox.x / blockSize);
        int maxX = (int) Math.floor((entityHitbox.x + entityHitbox.width - GAP) / blockSize);
        int minY = (int) Math.floor((entityHitbox.y - GAP) / blockSize);

        for (int x = minX; x <= maxX; x++) {
            int blockId = world.getBlockId(x, minY);
            if (blockId < 0 || blockId > 0 && content.blocksRegistry.typeById(blockId).type == StaticObjectsConst.Type.SOLID) {
                return true;
            }
        }
        return false;
    }

    static final float STEPS = 1f / Time.ONE_SECOND;

    private static void updateMovement() {

        player.updateInput();

        updateEntities();

        // Физика не будет оставаться на главном потоке, но пока это прототип.

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
            float rightBorder = (world.sizeX - swap) * blockSize;
            float leftBorder = swap * blockSize;
            float dx = rightBorder - leftBorder;

            // TODO:  при передвижении справа налево движение засчитывается только у ent.getX() (а это левый нижний пиксель)
            //        Логично что касание должно быть от ent.getX()+hitbox.width
            if (ent.getX() >= rightBorder) {
                ent.setX(ent.getX() - dx);
                if (player == ent) {
                    camera.position.x -= dx;
                    ShadowMap.update();
                }
            } else if (ent.getX() <= leftBorder) {
                ent.setX(ent.getX() + dx);
                if (player == ent) {
                    camera.position.x += dx;
                    ShadowMap.update();
                }
            }

            if (ent == player && noClip) {
                continue;
            }

            if (!(ent instanceof LivingEntity livingEntity)) {
                continue;
            }

            boolean hasFloor = ent.hasFloor();
            Vector2f vel = livingEntity.getVelocity();
            if (!hasFloor) {
                vel.y -= livingEntity.getWeight() * GRAVITY * dt;
            }

            float k = calculateFriction(livingEntity);
            float perSecondRetention = Math.clamp(1.0f - k * livingEntity.getWeight() * FRICTION_FACTOR, 0.0f, 1.0f);
            float retentionForDt = (float) Math.pow(perSecondRetention, dt);
            vel.x *= retentionForDt;

            if (abs(vel.x) >= maxSpeed) {
                vel.x = Math.signum(vel.x) * maxSpeed;
            }
            if (abs(vel.y) >= maxSpeed) {
                vel.y = Math.signum(vel.y) * maxSpeed;
            }
            move(livingEntity, dt);

            if (vel.y < 0 && ent.hasFloor()) {
                vel.y = 0;
            }
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

    private static void move(LivingEntity ent, float dt) {
        var vel = ent.getVelocity();
        float deltax = vel.x * dt, deltay = vel.y * dt;

        while (abs(deltay) > moveThreshold) {
            float sgn = Math.signum(deltay);
            moveDelta(ent, 0, Math.min(abs(deltay), MOVEMENT_SEGMENT) * sgn);

            if (abs(deltay) >= MOVEMENT_SEGMENT) {
                deltay -= MOVEMENT_SEGMENT * sgn;
            } else {
                deltay = 0f;
            }
        }

        while (abs(deltax) > moveThreshold) {
            float sgn = Math.signum(deltax);
            moveDelta(ent, Math.min(abs(deltax), MOVEMENT_SEGMENT) * sgn, 0);

            if (abs(deltax) >= MOVEMENT_SEGMENT) {
                deltax -= MOVEMENT_SEGMENT * sgn;
            } else {
                deltax = 0f;
            }
        }

        if (abs(deltax) == 0) vel.x = 0;
        if (abs(deltay) == 0) vel.y = 0;
    }

    private static float calculateFriction(LivingEntity ent) {
        ent.getHitboxTo(entityHitbox);

        int minX = (int) Math.floor(entityHitbox.x / blockSize);
        int minY = (int) Math.floor((entityHitbox.y - GAP) / blockSize);

        int maxX = (int) Math.floor((entityHitbox.x + entityHitbox.width) / blockSize);
        int maxY = (int) Math.floor((entityHitbox.y + entityHitbox.height) / blockSize);

        float resistance = 100;
        var appliedResistances = FixedBitset.createBitSet(content.blocksRegistry.getMaxId());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                var block = world.getBlock(x, y);
                if (block != null && block.resistance > 0) {
                    blockHitbox.set(x * blockSize, y * blockSize, blockSize, blockSize);

                    if (blockHitbox.overlaps(entityHitbox)) {
                        int blockId = content.blocksRegistry.idByType(block);
                        if (!FixedBitset.isSet(appliedResistances, blockId)) {
                            FixedBitset.setBit(appliedResistances, blockId);

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
