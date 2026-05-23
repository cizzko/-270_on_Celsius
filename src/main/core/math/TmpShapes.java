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

    public static final Vector2f v1 = new Vector2f();
    public static final Vector2f v2 = new Vector2f();
    public static final Vector2f v3 = new Vector2f();

    public static final Rectangle r1 = new Rectangle();
    public static final Rectangle r2 = new Rectangle();
    public static final Rectangle r3 = new Rectangle();

    public static final Point2i p1 = new Point2i();
    public static final Point2i p2 = new Point2i();
    public static final Point2i p3 = new Point2i();

    public static final Color c1 = new Color();
    public static final Color c2 = new Color();
    public static final Color c3 = new Color();

    public static final Colorf cf1 = new Colorf();
    public static final Colorf cf2 = new Colorf();
    public static final Colorf cf3 = new Colorf();
}
