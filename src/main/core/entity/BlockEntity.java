package core.entity;

import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst;

public interface BlockEntity extends Entity, DrawComponent {

    StaticObjectsConst getBlock();

    /**
     * Проба добавления предмета в блок.
     *
     * @param item Не нулевой, не пустой стек предметов
     * @return {@code true} если предмет принят, как полностью, так может и частично
     */
    default boolean insertItem(ItemStack item) { return false; }

    default void onMouseClick() {}

    default void onMouseHover() {}
}
