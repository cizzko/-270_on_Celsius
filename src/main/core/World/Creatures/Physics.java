package core.World.Creatures;

import core.Constants;
import core.PlayGameScene;
import core.Time;
import core.content.blocks.Block;
import core.content.entity.Entity;
import core.content.entity.HealthComponent;
import core.content.entity.LivingEntity;
import core.g2d.Drawable;
import core.graphic.ShadowMap;
import core.math.MathUtil;
import core.math.Rectangle;
import core.math.Vector2f;
import core.util.FixedBitset;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.Arrays;

import static core.Global.*;
import static core.World.Creatures.Player.Player.noClip;
import static core.WorldCoordinates.toBlock;
import static core.WorldCoordinates.toWorld;
import static java.lang.Math.*;

public class Physics {
    // Для какого веса считались коэффициенты физики. Проверяли на игроке. Меньше весит - меньше влияет вес
    public static final float WEIGHT_FACTOR   = 1f / 80;
    // 44 блока / секунда²
    public static final float GRAVITY         = 2f * 2f / (float)pow(18, 2);
    public static final float FRICTION_FACTOR = 0.250f;
    // Ле, куда летишь?
    public static final float MAX_SPEED = 10000f;
    // Минимальное смещение, при котором происходит движение. Не вижу смысла сжигать процессор ради меньших значений
    private static final float MOVE_THRESHOLD = 1e-6f;

    // Скорость, которая набирается при падении ровно с 5 блоков
    private static final float FALL_DAMAGE_SPEED_THRESHOLD = 0.35f; // sqrt(2 * GRAVITY * 5)
    // Множитель, гарантирующий ровно 10 ХП урона при падении с 5 блоков для сущности с массой 1.0 (эталон - игрок)
    private static final float FALL_DAMAGE_MULTIPLIER = 10f / (FALL_DAMAGE_SPEED_THRESHOLD*FALL_DAMAGE_SPEED_THRESHOLD);

    public static void updatePhysics(PlayGameScene scene) {
        if (scene.isPaused()) {
            return;
        }
        updateMovement();
        processCollisions();
    }

    static final float STEPS = 1f / Time.ONE_SECOND;

    static long[] resistanceSet; // TODO SparseFixedBitSet ?
    static final IntOpenHashSet completedCollisions = new IntOpenHashSet();

    static final Rectangle hitbox = new Rectangle();
    static final Rectangle entityHitbox = new Rectangle();
    static final Rectangle blockHitbox = new Rectangle();
    static final Vector2f tmp1 = new Vector2f();

    static int combine(short a, short b) {
        if (a > b) { // коммутативный id; меньший в старших битах, больший в младших
            short tmp = a;
            a = b;
            b = tmp;
        }
        return a << 16 | b & 0xffff;
    }

    private static void processCollisions() {
        completedCollisions.clear();
        entityPool.updatePositions();

        entityPool.forEach(me -> {
            entityPool.index().intersect(me, them -> {
                if (me == them) {
                    return;
                }

                if (completedCollisions.add(combine(me.id(), them.id()))) {
                    me.onCollide(them);
                    if (!me.isRemoved()) {
                        them.onCollide(me);
                    }
                }
            });
        });
    }

    private static void updateEntities() {
        entityPool.forEach(Entity::update);
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

        float m = max(penetration, 0.0f);
        return tmp1.scale(-m);
    }

    private static void moveDelta(LivingEntity entity, float deltaX, float deltaY) {
        entity.getHitboxTo(hitbox);
        entity.getHitboxTo(entityHitbox);
        entityHitbox.x += deltaX;
        entityHitbox.y += deltaY;

        int minX = toBlock(entityHitbox.x);
        int minY = toBlock(entityHitbox.y);
        int maxX = toBlock(entityHitbox.x + entityHitbox.width);
        int maxY = toBlock(entityHitbox.y + entityHitbox.height);
        boolean collided = false;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int blockId = world.getBlockId(x, y);
                if (blockId <= 0) {
                    continue;
                }
                var block = content.blocksRegistry.typeById(blockId);
                if (block.type != Block.Type.SOLID) {
                    continue;
                }
                blockHitbox.set(x, y, block.tileCountX, block.tileCountY);

                if (blockHitbox.contains(entityHitbox)) {
                    // TODO при горизонтальных столкновениях сбрасывать скорость / начислять урон
                    var offsetVec = overlap(entityHitbox, blockHitbox);

                    entityHitbox.x += offsetVec.x;
                    entityHitbox.y += offsetVec.y;
                    collided = true;
                }
            }
        }


        if (collided) {
            var vel = entity.velocity();
            if (abs(deltaX) > 0) vel.x = 0;
            // vel.y необходим для просчёта урона от падения. Там же он и обнуляется
            // TODO: Для vel.x необходимо такое же сделать
        }

        entity.setPosition(
                entity.x() - hitbox.x + entityHitbox.x,
                entity.y() - hitbox.y + entityHitbox.y);
    }

    private static void updateMovement() {

        player.updateInput();

        updateEntities();

        // Физика не будет оставаться на главном потоке, но пока это прототип.

        entityPool.forEach(ent -> {
            if (ent instanceof LivingEntity livingEntity) {
                Vector2f vel = livingEntity.velocity();
                Vector2f acc = livingEntity.acceleration();

                vel.x += acc.x * Time.delta;
                vel.y += acc.y * Time.delta;

                acc.set(0, 0);
            }
        });

        float dt = Time.delta;
        while (dt >= STEPS) {
            simulate(STEPS);
            dt -= STEPS;
        }

        simulate(dt);
    }

    private static void simulate(float dt) {
        entityPool.forEach(ent -> simulateEntity(dt, ent));
    }

    private static void simulateEntity(float dt, Entity ent) {
        float rightBorder = (world.sizeX - Constants.World.SWAP_AREA);
        float leftBorder = Constants.World.SWAP_AREA;
        float dx = rightBorder - leftBorder;

        // TODO:  при передвижении справа налево движение засчитывается только у ent.getX() (а это левый нижний пиксель)
        //        Логично что касание должно быть от ent.getX()+hitbox.width
        if (ent.x() >= rightBorder) {
            ent.setX(ent.x() - dx);
            if (player == ent) {
                camera.position.x -= dx;
                ShadowMap.update();
            }
        } else if (ent.x() <= leftBorder) {
            ent.setX(ent.x() + dx);
            if (player == ent) {
                camera.position.x += dx;
                ShadowMap.update();
            }
        }

        if (ent == player && noClip) {
            return;
        }

        if (!(ent instanceof LivingEntity livingEntity)) {
            return;
        }

        boolean hasFloor = ent.hasFloor();
        Vector2f vel = livingEntity.velocity();
        if (!hasFloor) {
            vel.y -= (livingEntity.getWeight() * WEIGHT_FACTOR) * GRAVITY * dt;
        }

        if (hasFloor) {
            float k = calculateFriction(livingEntity);
            float frictionCoefficient = k * livingEntity.getWeight() * WEIGHT_FACTOR * FRICTION_FACTOR;
            vel.x *= (float) Math.exp(-frictionCoefficient * dt);
        } else {
            // TODO по сути это сопротивление в газах (воздух в т.ч.)
            float k = 1f/8;
            float drag = k * Math.abs(vel.x) * dt;
            if (drag > 1) {
                drag = 1;
            }
            vel.x -= vel.x * drag;
        }

        if (abs(vel.x) >= MAX_SPEED) {
            vel.x = signum(vel.x) * MAX_SPEED;
        }
        if (abs(vel.y) >= MAX_SPEED) {
            vel.y = signum(vel.y) * MAX_SPEED;
        }
        move(livingEntity, dt);

        if (abs(vel.x) <= MathUtil.EPSILON) vel.x = 0;
    }

    private static void move(LivingEntity ent, float dt) {
        var vel = ent.velocity();
        float dx = vel.x * dt;
        float dy = vel.y * dt;

        if (abs(dx) > MOVE_THRESHOLD)
            moveDelta(ent, dx, 0);
        if (abs(dy) > MOVE_THRESHOLD) {
            moveDelta(ent, 0, dy);
            entityFall(ent);
        }
    }

    private static void entityFall(LivingEntity ent) {
        var vel = ent.velocity();
        boolean hasFloor = ent.hasFloor();

        if (vel.y < -FALL_DAMAGE_SPEED_THRESHOLD && hasFloor) {
            float impact = (ent.getWeight() * WEIGHT_FACTOR) * (vel.y*vel.y)/2f;
            int damage = (int) floor(impact * FALL_DAMAGE_MULTIPLIER);

            // TODO: Необходимо что-то придумать с распределением урона по площади контакта
            //       Это явно не будет сделано здесь из-за направления обхода и применения урона
            if (damage > 0) {
                ent.damage(damage, HealthComponent.DamageSource.FALL);
                // world.damage(x, y, damage);
            }
        }
        if (hasFloor)
            vel.y = 0;
    }

    private static float calculateFriction(LivingEntity ent) {
        ent.getHitboxTo(entityHitbox);

        int minX = Math.max(0, toBlock(entityHitbox.x));
        int minY = Math.max(0, toBlock(entityHitbox.y) - 1);
        int maxX = Math.min(world.sizeX - 1, toBlock(entityHitbox.x + entityHitbox.width));
        int maxY = Math.min(world.sizeY - 1, toBlock(entityHitbox.y + entityHitbox.height));

        float resistance = 100;
        if (resistanceSet == null) {
             resistanceSet = FixedBitset.createBitSet(content.blocksRegistry.count());
        } else {
            Arrays.fill(resistanceSet, 0);
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                var block = world.getBlock(x, y);
                if (block != null && block.resistance > 0) {
                    blockHitbox.set(x, y, block.tileCountX, block.tileCountY);

                    var blockId = world.getBlockId(x, y);
                    if (blockHitbox.overlaps(entityHitbox)) {
                        if (!FixedBitset.isSet(resistanceSet, blockId)) {
                            FixedBitset.setBit(resistanceSet, blockId);

                            resistance = max(resistance, block.resistance);
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

    public static boolean checkIntersection(float wx, float wy, Drawable texture) {
        entityHitbox.set(wx, wy, toWorld(texture.width()), toWorld(texture.height()));

        int minX = Math.max(0, toBlock(entityHitbox.x));
        int minY = Math.max(0, toBlock(entityHitbox.y));
        int maxX = Math.min(world.sizeX - 1, toBlock(entityHitbox.x + entityHitbox.width));
        int maxY = Math.min(world.sizeY - 1, toBlock(entityHitbox.y + entityHitbox.height));

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (world.getBlockType(x, y) == Block.Type.SOLID) {
                    return true;
                }
            }
        }
        return false;
    }
}
