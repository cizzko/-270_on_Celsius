package core.content;

import core.content.entity.LivingEntity;
import core.math.AABB;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Comparator;
import java.util.function.Consumer;

import static core.WorldCoordinates.toBlock;
import static core.content.entity.comp.DrawComponent.GAP;

/// Линейное Квадродерево на основе кодов Мортона
public final class EntityIndex {
    final AABB tmp = new AABB();
    final ObjectArrayList<EntityRef> elements = new ObjectArrayList<>();

    public void presize(int size) {
        elements.ensureCapacity(size);
    }

    static final class EntityRef {
        final LivingEntity entity;
        final int mortonStart, mortonEnd;

        EntityRef(LivingEntity entity, int mortonStart, int mortonEnd) {
            this.entity = entity;
            this.mortonStart = mortonStart;
            this.mortonEnd = mortonEnd;
        }
    }

    public void insert(LivingEntity entity) {
        entity.hitboxTo(tmp);

        tmp.minX += GAP;
        tmp.maxX -= GAP;
        tmp.minY += GAP;
        tmp.maxY -= GAP;

        short minX = tmp.blockMinX();
        short minY = tmp.blockMinY();
        short maxX = tmp.blockMaxX();
        short maxY = tmp.blockMaxY();

        int mortonStart = encode(minX, minY);
        int mortonEnd = encode(maxX, maxY);
        elements.add(new EntityRef(entity, mortonStart, mortonEnd));
    }

    public void build() {
        elements.sort(MortonComparator.INSTANCE);
    }

    public void intersect(LivingEntity entity, Consumer<LivingEntity> action) {
        entity.hitboxTo(tmp);
        intersect(tmp.minX, tmp.minY, tmp.width(), tmp.height(), action);
    }

    public boolean any(double x, double y, double width, double height) {
        short minX = toBlock(x);
        short minY = toBlock(y);
        short maxX = toBlock(x + width);
        short maxY = toBlock(y + height);

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

            ent.entity.hitboxTo(tmp);
            if (tmp.intersects(x, y, x+width, y+height)) {
                return true;
            }
        }
        return false;
    }

    public void intersect(double x, double y, float width, float height, Consumer<LivingEntity> action) {

        short minX = toBlock(x);
        short minY = toBlock(y);
        short maxX = toBlock(x + width);
        short maxY = toBlock(y + height);

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

            ent.entity.hitboxTo(tmp);
            if (tmp.intersects(x, y, x+width, y+height)) {
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
