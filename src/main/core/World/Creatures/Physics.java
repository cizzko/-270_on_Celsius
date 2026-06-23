package core.World.Creatures;

import core.Constants;
import core.PlayGameScene;
import core.Time;
import core.content.blocks.Block;
import core.content.entity.Entity;
import core.content.entity.LivingEntity;
import core.content.entity.comp.HealthComponent;
import core.graphic.ShadowMap;
import core.math.AABB;
import core.math.MathUtil;
import core.math.Vector2f;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import static core.Global.*;
import static core.World.Creatures.Player.Player.noClip;
import static core.WorldCoordinates.toBlock;
import static java.lang.Math.*;

public final class Physics {
    // Для какого веса считались коэффициенты физики. Проверяли на игроке. Меньше весит - меньше влияет вес
    public static final float WEIGHT_FACTOR   = 1f / 80;
    // 44 блока / секунда²
    public static final float GRAVITY         = 2f * 2f / (float)pow(18, 2);
    public static final float FRICTION_FACTOR = 0.850f;
    // Ле, куда летишь?
    public static final float MAX_SPEED = 10000f;
    public static final float EPS = 1e-10f;
    // Минимальное смещение, при котором происходит движение. Не вижу смысла сжигать процессор ради меньших значений
    private static final float MOVE_THRESHOLD = 0;// 1e-6f;

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

    // Целевой тикрейт физики. Можно считать как 60 тиков / сек
    public static final float TPS = 60;
    // Физика работает на фиксированном времени, чтобы обеспечить стабильность системы
    public static final float FDT = 1f / TPS;
    public static final float SPEED_FACTOR = FDT;
    static final double INV_FDT = 1d / FDT;

    static long[] resistanceSet; // TODO SparseFixedBitSet ?
    static final IntOpenHashSet completedCollisions = new IntOpenHashSet();
    static float pdt;
    /// Поскольку движок имеет фиксированный шаг, то очень мелкие изменения времени
    /// нецелесообразно обрабатывать, а лучше интерполировать на рендере. Вот это и есть параметр интерполяции
    /// с.м. [#applyAlpha(double, double)]
    static double alpha;

    static final AABB entityHitbox = new AABB();
    static final AABB blockHitbox = new AABB();
    static final Vector2f tmp1 = new Vector2f();

    public static double alpha() { return alpha; }

    public static double applyAlpha(double last, double curr) {
        return MathUtil.lerp(last, curr, alpha);
    }

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

        entityPool.forEachType(LivingEntity.class, me -> {
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

    private static void moveDelta(LivingEntity ent, float dx, float dy) {
        ent.hitboxTo(entityHitbox);
        entityHitbox.move(dx, dy);

        final float SCAN_EPS = 0.01f;
        short minX = toBlock(entityHitbox.minX + SCAN_EPS);
        short minY = toBlock(entityHitbox.minY + SCAN_EPS);
        short maxX = toBlock(entityHitbox.maxX - SCAN_EPS);
        short maxY = toBlock(entityHitbox.maxY - SCAN_EPS);

        boolean collided = false;

        for (short y = minY; y <= maxY; y++) {
            for (short x = minX; x <= maxX; x++) {
                int blockId = world.getBlockId(x, y);
                if (blockId <= 0) {
                    continue;
                }
                var block = content.blocksRegistry.typeById(blockId);
                if (block.type != Block.Type.SOLID) {
                    continue;
                }
                blockHitbox.setRectangle(x, y, block.tileCountX, block.tileCountY);

                if (entityHitbox.intersects(blockHitbox)) {
                    var offsetVec = entityHitbox.overlapTo(blockHitbox, tmp1);

                    entityHitbox.move(offsetVec);
                    collided = true;
                }
            }
        }

        if (collided) {
            var vel = ent.velocity();
            if (abs(dx) > 0) vel.x = 0;
            if (abs(dy) > 0) {
                entityFall(ent);
                vel.y = 0;
            }
        }
        ent.setPosition(entityHitbox.minX, entityHitbox.minY);
    }

    private static void updateMovement() {
        player.updateInput();
        updateEntities();

        float dt = pdt += Time.delta;
        while (dt >= FDT) {
            applyAcceleration();
            simulate();
            dt -= FDT;
        }
        pdt = dt;
        alpha = dt * INV_FDT;
    }

    private static void applyAcceleration() {
        entityPool.forEachType(LivingEntity.class, Physics::applyAcceleration);
    }

    private static void applyAcceleration(LivingEntity ent) {
        Vector2f vel = ent.velocity();
        Vector2f acc = ent.acceleration();
        if (acc.isZeroEps(MathUtil.EPSILON)) {
            return;
        }

        // Однажды узнав уже не можешь остановиться
        vel.x = fma(acc.x, FDT, vel.x);
        vel.y = fma(acc.y, FDT, vel.y);

        acc.set(0, 0);
    }

    private static void simulate() {
        entityPool.forEachType(LivingEntity.class, Physics::simulateEntity);
    }

    private static void simulateEntity(LivingEntity ent) {
        final float dt = FDT;
        final int leftBorder = Constants.World.SWAP_AREA;

        int rightBorder = (world.sizeX - Constants.World.SWAP_AREA);
        int dx = rightBorder - leftBorder;

        if (ent.x() >= rightBorder &&
                    ent.x() + ent.width() >= rightBorder) {
            ent.mirrorX(-dx);
            if (player == ent) {
                camera.position.x -= dx;
                ShadowMap.update();
            }
        } else if (ent.x() <= leftBorder) {
            ent.mirrorX(+dx);
            if (player == ent) {
                camera.position.x += dx;
                ShadowMap.update();
            }
        }

        if (ent == player && noClip) {
            return;
        }

        ent.updateLastPosition();

        boolean hasFloor = ent.hasFloor();
        Vector2f vel = ent.velocity();
        if (!hasFloor) {
            vel.y -= (ent.mass() * WEIGHT_FACTOR) * GRAVITY * dt;
        }

        if (hasFloor) {
            float k = calculateFriction(ent);
            if (k >= 1.0f) {
                vel.x = 0f;
                vel.y = 0f;
            } else {
                float frictionCoefficient = k * ent.mass() * WEIGHT_FACTOR * FRICTION_FACTOR;
                float fd = (float) exp(-frictionCoefficient * dt);
                vel.x *= fd;
            }
        } else {
            // TODO по сути это сопротивление в газах (воздух в т.ч.)
            //      Учитывать случаи когда падаешь сквозь walkable (листву например)
            float k = 1f/8;
            float drag = k * abs(vel.x) * dt;
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
        move(ent);

        if (abs(vel.x) <= MathUtil.EPSILON) vel.x = 0;
    }

    private static void move(LivingEntity ent) {
        var vel = ent.velocity();
        float dx = vel.x * FDT;
        float dy = vel.y * FDT;

        if (abs(dx) > MOVE_THRESHOLD)
            moveDelta(ent, dx, 0);
        if (abs(dy) > MOVE_THRESHOLD)
            moveDelta(ent, 0, dy);
    }

    private static void entityFall(LivingEntity ent) {

        var vel = ent.velocity();
        if (vel.y < -FALL_DAMAGE_SPEED_THRESHOLD && ent.hasFloor()) {
            float impact = (ent.mass() * WEIGHT_FACTOR) * (vel.y * vel.y) / 2f;
            int damage = (int) floor(impact * FALL_DAMAGE_MULTIPLIER);

            // TODO: Необходимо что-то придумать с распределением урона по площади контакта
            //       Это явно не будет сделано здесь из-за направления обхода и применения урона
            if (damage > 0) {
                ent.damage(damage, HealthComponent.DamageSource.FALL);
                // world.damage(x, y, damage);
            }
        }
    }

    private static float calculateFriction(LivingEntity ent) {
        ent.hitboxTo(entityHitbox);
        entityHitbox.clampToWorld();

        short minX = entityHitbox.blockMinX();
        short minY = (short) (entityHitbox.blockMinY() - 1);
        short maxX = entityHitbox.blockMaxX();
        short maxY = entityHitbox.blockMaxY();

        float resistance = 0;
        for (; minY <= maxY; minY++) {
            for (short x = minX; x <= maxX; x++) {
                var block = world.getBlock(x, minY);
                if (block != null) {
                    resistance = max(resistance, block.resistance);
                }
            }
        }
        float friction = resistance / 100f;
        if (friction > 1) {
            friction = 1;
        }
        return friction;
    }

    public static boolean checkIntersection(double wx, double wy, float w, float h) {
        entityHitbox.setRectangle(wx, wy, w, h);
        entityHitbox.clampToWorld();

        short minX = entityHitbox.blockMinX();
        short minY = entityHitbox.blockMinY();
        short maxX = entityHitbox.blockMaxX();
        short maxY = entityHitbox.blockMaxY();

        for (; minY <= maxY; minY++) {
            for (short x = minX; x <= maxX; x++) {
                if (world.isBlockType(x, minY, Block.Type.SOLID)) {
                    return true;
                }
            }
        }
        return false;
    }
}
