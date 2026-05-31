package core.content.blocks;

import core.content.ContentLoader;
import core.content.ContentResolver;

/**
 * Блок-заглушка, который используется во время первой фазы загрузки контента (чтения).
 * <p>
 * Во второй фазе загрузки все заглушки должны быть заменены на настоящие блоки.
 * В противном случае выводится диагностическое сообщение
 */
public final class BlockUnresolved extends Block {
    public BlockUnresolved(String id) {
        super(id);
    }

    @Override
    public void load(ContentLoader cnt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resolve(ContentResolver res) {
        throw new UnsupportedOperationException();
    }
}
