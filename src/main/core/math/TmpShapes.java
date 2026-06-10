package core.math;

import core.graphic.Color;
import core.graphic.Colorf;

/// Этот класс содержит объекты которые рассчитаны на короткие,
/// не вложенные по стеку вычисления. Цель этого класса в уменьшении
/// количества временных объектов на горячих путях.
/// Да, у JIT компилятора есть escape-analysis,
/// но полагать это не избавляет от необходимости от временных объектов
/// Пример использования:
/// ```
/// entity.getHitboxTo(TmpShapes.r1);
/// // операции над r1
/// ```
/// <p>
/// Используйте строго на главном потоке.
/// Любое использование этих структур без предварительной инициализации считается
/// неопределенным поведением
public final class TmpShapes {
    private TmpShapes() {}

    public static final Vector2f v1f = new Vector2f();

    public static final Vector2d v1d = new Vector2d();

    public static final Rectangle r1 = new Rectangle();

    public static final AABB aabb1 = new AABB();

    public static final Point2i p1 = new Point2i();

    public static final Color c1 = new Color();

    public static final Colorf cf1 = new Colorf();
}
