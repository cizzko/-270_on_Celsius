package core.content.entity;

import core.g2d.RenderList;

public interface BlockDrawComponent {

    /// Состояние, от которого зависит рендер изменилось,
    /// пересобираем сцену
    boolean drawStateChanged();

    default void draw(RenderList rlist) {}

    default void drawGui() {}
}
