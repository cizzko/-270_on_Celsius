package core.World.Creatures;

import core.PlayGameScene;
import core.Time;
import core.World.Creatures.Player.Player;
import core.World.HitboxMap;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.Textures.TextureDrawing;
import core.math.Point2i;
import core.math.Rectangle;
import core.math.Vector2f;

import java.util.Locale;

import static core.Global.*;
import static core.World.Creatures.DynamicWorldObjects.GAP;
import static core.World.Creatures.Player.Player.*;
import static core.World.StaticWorldObjects.StaticWorldObjects.getResistance;
import static core.World.StaticWorldObjects.StaticWorldObjects.getType;
import static core.World.Textures.TextureDrawing.blockSize;
import static core.World.WorldGenerator.WorldGenerator.*;

public class Physics {
    private static final float ANSWER = 42; // хихи, хаха
    private static final float GRAVITY = 1.25f * ANSWER * 1e-4f;
    public static final short swap = 25;

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

        float xoverlap = aex + bex - Math.abs(nx);
        if (Math.abs(xoverlap) > 0) {
            float aey = a.height / 2, bey = b.height / 2;

            float ay = a.y + a.height / 2, by = b.y + b.height / 2;
            float ny = ay - by;
            float yoverlap = aey + bey - Math.abs(ny);
            if (Math.abs(yoverlap) > 0) {
                if (Math.abs(xoverlap) < Math.abs(yoverlap)) {
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

    private static void moveDelta(DynamicWorldObjects entity, float deltaX, float deltaY) {
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
                short block = world.get(x, y);
                if (block == -1 || getResistance(block) >= 100 && getType(block) == StaticObjectsConst.Types.SOLID) {
                    blockHitbox.set(x * blockSize, y * blockSize, blockSize, blockSize);

                    if (blockHitbox.overlaps(entityHitbox)) {
                        var v = overlap(entityHitbox, blockHitbox);
                        entityHitbox.x += v.x;
                        entityHitbox.y += v.y;
                    }
                }
            }
        }

        entity.incrementX(entityHitbox.x - hitbox.x);
        entity.incrementY(entityHitbox.y - hitbox.y);
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
                    short staticObject = world.get(point.x, point.y);
                    float currentDamage = ((((StaticWorldObjects.getResistance(staticObject) / 100) * StaticWorldObjects.getDensity(staticObject)) + (entity.getWeight() + (Math.max(Math.abs(vectorY), Math.abs(vectorX)) - minVectorIntersDamage)) * intersDamageMultiplier)) / staticObjectPoint.length;

                    damage += currentDamage;
                    short object1 = StaticWorldObjects.decrementHp(staticObject, (int) (currentDamage + (getResistance(staticObject) / 100) * StaticWorldObjects.getDensity(staticObject)) / staticObjectPoint.length);
                    world.set(point.x, point.y, object1, false);
                }
                entity.incrementCurrentHP(-damage);

                // todo переписать
                if (entity.getTexture().name().toLowerCase().contains("player")) {
                    lastDamage = (int) damage;
                    lastDamageTime = System.currentTimeMillis();
                }
                if (entity.getCurrentHP() <= 0 && !entity.getTexture().name().toLowerCase().contains("player")) {
                    DynamicObjects.remove(entity);
                }
            }
        }
    }

    static final float STEPS = Time.ONE_SECOND;

    private static void update() {
        DynamicObjects.getFirst().updateInput();

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
        for (DynamicWorldObjects ent : DynamicObjects) {
            //todo распространить на все динамик без исключений
            if (ent.getX() > (world.sizeX - swap) * blockSize) {
                DynamicWorldObjects player = DynamicObjects.getFirst();
                ent.setX(swap * blockSize);

                camera.position.set(player.getX() + 32, player.getY() + 200);
            }

            if (ent.getX() < swap * blockSize) {
                DynamicWorldObjects player = DynamicObjects.getFirst();
                ent.setX((world.sizeX - swap) * blockSize);

                camera.position.set(player.getX() + 32, player.getY() + 200);
            }
            if (ent.getTexture().name().toLowerCase(Locale.ROOT).contains("player") && noClip) {
                continue;
            }

            float x = ent.getX(), y = ent.getY();
            boolean hasFloor = ent.hasFloor();
            move(ent, dt);
            Vector2f vel = ent.velocity;
            if (Math.abs(x - ent.getX()) <= moveThreshold) vel.x = 0;
            if (Math.abs(y - ent.getY()) <= moveThreshold) vel.y = 0;

            if (!hasFloor) {
                vel.y -= ent.getWeight() * GRAVITY * dt;
            }

            // TODO Вообще, если говорить о силе трения, то вот мои мысли:
            //   1) Сила трения направлена против движения (-Math.signum(vel.x) * ...)
            //      И она должна зависеть от массы объекта, как и должна по 3 закону Ньютона
            //      действовать на другое тело с противоположным направлением. Это о вопросе коллизий между сущностями
            //   2) Я думаю разделить просчёт коэффициента сопротивления в 2 этапа. Задумка следующая:
            //      Есть коэфф. сопротивления среды. Типа сопротивления воздуха (базовое сопротивление),
            //      сопротивление в листве и может что-то подобное. А также есть сопротивление с поверхностью.
            //      В зависимости от вектора скорости алгоритм должен определить грань, с которой происходит сопротивление
            //      и просчитать сопротивление всех блоков (исходя из теоретических соображений это сумма всех коэффициентов)
            //      Потом сопротивление среды и сопротивление с гранью складываются и (опционально) умножаются на массу
            float friction = calculateFriction(ent);
            vel.x *= friction;
            // vel.y *= friction

            // TODO Тут или в логике игрока (Player#update()) должен быть расчёт силы удара.
            //      Как я описал в ЛС, это F=m*a, то есть можно нужно рассчитать ускорение
            //      как изменение скорости за время и умножить на массу игрока. Так мы получим численное нечто,
            //      что можно в дальнейшем перевести в hp. Урон блокам, на которые падает игрок, равносилен урону игрока.
            //      По 3 закону Ньютона жеж)

            if (Math.abs(vel.x) >= maxSpeed) vel.x = Math.signum(vel.x) * maxSpeed;
            if (Math.abs(vel.y) >= maxSpeed) vel.y = Math.signum(vel.y) * maxSpeed;
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

    private static void move(DynamicWorldObjects ent, float dt) {
        var vel = ent.velocity;
        float deltax = vel.x * dt, deltay = vel.y * dt;

        while (Math.abs(deltax) > moveThreshold) {
            float sgn = Math.signum(deltax);
            moveDelta(ent, Math.min(Math.abs(deltax), MOVEMENT_SEGMENT) * sgn, 0);

            if (Math.abs(deltax) >= MOVEMENT_SEGMENT) {
                deltax -= MOVEMENT_SEGMENT * sgn;
            } else {
                deltax = 0f;
            }
        }

        while (Math.abs(deltay) > moveThreshold) {
            float sgn = Math.signum(deltay);
            moveDelta(ent, 0, Math.min(Math.abs(deltay), MOVEMENT_SEGMENT) * sgn);

            if (Math.abs(deltay) >= MOVEMENT_SEGMENT) {
                deltay -= MOVEMENT_SEGMENT * sgn;
            } else {
                deltay = 0f;
            }
        }
    }

    private static float calculateFriction(DynamicWorldObjects ent) {
        ent.getHitboxTo(entityHitbox);

        int minX = (int) Math.floor(entityHitbox.x / blockSize);
        int minY = (int) Math.floor(entityHitbox.y / blockSize);

        int maxX = (int) Math.floor((entityHitbox.x + entityHitbox.width) / blockSize);
        int maxY = (int) Math.floor((entityHitbox.y + entityHitbox.height) / blockSize);

        float resistance = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                short block = world.get(x, y);
                float res = getResistance(block);
                if (res > 0) {
                    blockHitbox.set(x * blockSize, y * blockSize, blockSize, blockSize);

                    if (blockHitbox.overlaps(entityHitbox)) {
                        resistance += res;
                    }
                }
            }
        }

        float friction = resistance / 100f;
        return 1f - Math.max(0.3f, friction);
    }
}
