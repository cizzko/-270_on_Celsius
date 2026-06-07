package core.content;

import core.content.entity.Entity;
import core.math.Rectangle;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Comparator;
import java.util.function.Consumer;

import static core.WorldCoordinates.*;

/// Линейное Квадродерево на основе кодов Мортона
public final class EntityIndex {
    final Rectangle tmp = new Rectangle();
    final ObjectArrayList<EntityRef> elements = new ObjectArrayList<>();

    public void presize(int size) {
        elements.ensureCapacity(size);
    }

    static final class EntityRef {
        final Entity entity;
        final int mortonStart, mortonEnd;

        EntityRef(Entity entity, int mortonStart, int mortonEnd) {
            this.entity = entity;
            this.mortonStart = mortonStart;
            this.mortonEnd = mortonEnd;
        }
    }

    public void insert(Entity entity) {
        entity.getHitboxTo(tmp);

        short minX = (short) toBlock(tmp.x);
        short minY = (short) toBlock(tmp.y);

        short maxX = (short) toBlock(tmp.x + tmp.width);
        short maxY = (short) toBlock(tmp.y + tmp.height);

        int mortonStart = encode(minX, minY);
        int mortonEnd = encode(maxX, maxY);
        elements.add(new EntityRef(entity, mortonStart, mortonEnd));
    }

    public void build() {
        elements.sort(MortonComparator.INSTANCE);
    }

    public void intersect(Entity entity, Consumer<Entity> action) {
        entity.getHitboxTo(tmp);
        intersect(tmp.x, tmp.y, tmp.width, tmp.height, action);
    }

    public boolean any(float x, float y, float width, float height) {
        short minX = (short) toBlock(x);
        short minY = (short) toBlock(y);
        short maxX = (short) toBlock(x + width);
        short maxY = (short) toBlock(y + height);

        int searchMin = encode(minX, minY);
        int startIndex = lowerBound(searchMin);

        int size = elements.size();
        if (startIndex >= size) {
            return false;
        }

        Object[] e = elements.elements();
        int searchMax = encode(maxX, maxY);
        for (int i = startIndex; i < size; i++) {
            var ent = (EntityRef) e[i];

            if (Integer.compareUnsigned(ent.mortonStart, searchMax) > 0) {
                break;
            }
            if (Integer.compareUnsigned(ent.mortonEnd, searchMin) < 0) {
                continue;
            }

            ent.entity.getHitboxTo(tmp);
            if (tmp.overlaps(x, y, width, height)) {
                return true;
            }
        }
        return false;
    }

    public void intersect(float x, float y, float width, float height, Consumer<Entity> action) {

        short minX = (short) toBlock(x);
        short minY = (short) toBlock(y);
        short maxX = (short) toBlock(x + width);
        short maxY = (short) toBlock(y + height);

        int searchMin = encode(minX, minY);
        int startIndex = lowerBound(searchMin);

        int size = elements.size();
        if (startIndex >= size) {
            return;
        }

        Object[] e = elements.elements();
        int searchMax = encode(maxX, maxY);
        for (int i = startIndex; i < size; i++) {
            var ent = (EntityRef) e[i];

            if (Integer.compareUnsigned(ent.mortonStart, searchMax) > 0) {
                break;
            }
            if (Integer.compareUnsigned(ent.mortonEnd, searchMin) < 0) {
                continue;
            }

            ent.entity.getHitboxTo(tmp);
            if (tmp.overlaps(x, y, width, height)) {
                action.accept(ent.entity);
            }
        }
    }

    public void clear() {
        elements.clear();
    }

    static int encode(int x, int y) { // как хорошо что в jdk есть методы которые знают для чего использовать 1.5 человека
        return Integer.expand(x, 0x55555555) | Integer.expand(y, 0xAAAAAAAA);
    }

    static int decodeX(int code) { return Integer.compress(code, 0x55555555); }
    static int decodeY(int code) { return Integer.compress(code, 0xAAAAAAAA); }

    enum MortonComparator implements Comparator<EntityRef> {
        INSTANCE;

        public int compare(EntityRef o1, EntityRef o2) {
            return Integer.compareUnsigned(o1.mortonStart, o2.mortonStart);
        }
    }

    private int lowerBound(int targetMin) {
        int low = 0;
        int high = elements.size() - 1;
        int result = elements.size();

        Object[] e = elements.elements();
        while (low <= high) {
            int mid = (low + high) >>> 1;
            var ent = (EntityRef) e[mid];
            if (Integer.compareUnsigned(ent.mortonEnd, targetMin) >= 0) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }
}
