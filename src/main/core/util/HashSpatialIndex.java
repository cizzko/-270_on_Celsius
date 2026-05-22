package core.util;

import core.content.entity.HitboxComponent;
import core.math.Rectangle;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class HashSpatialIndex<E extends HitboxComponent> {

    public final Long2ObjectOpenHashMap<ObjectArrayList<E>> hash = new Long2ObjectOpenHashMap<>();
    public final float resolution;

    private final Rectangle tmp0 = new Rectangle(), tmp1 = new Rectangle();

    private static long combine(long x, long y) {
        return HashCommon.mix((x << 32) | (y & 0xffffffffL));
    }

    public HashSpatialIndex(float resolution) {
        this.resolution = resolution;
    }

    public void insert(E entry) {
        long key = combine((long) Math.floor(entry.x() / resolution), (long) Math.floor(entry.y() / resolution));
        hash.computeIfAbsent(key, k -> new ObjectArrayList<>()).add(entry);
    }

    public long size() {
        long size = 0;
        for (var value : hash.values()) {
            size += value.size();
        }
        return size;
    }

    public boolean findAnyInRange(float x, float y, float width, float height, Predicate<? super E> consumer) {
        if (hash.isEmpty()) {
            return false;
        }

        int dx = (int) ((width / resolution) + 1);
        int dy = (int) ((height / resolution) + 1);

        int px = (int) (x / resolution);
        int py = (int) (y / resolution);

        tmp0.set(x, y, width, height);

        for (int sx = -dx; sx <= dx; sx++) {
            for (int sy = -dy; sy <= dy; sy++) {
                int rx = px + sx;
                int ry = py + sy;

                var array = hash.get(combine(rx, ry));
                if (array != null) {
                    for (E e : array) {
                        e.getHitboxTo(tmp1);
                        if (tmp0.overlaps(tmp1) && consumer.test(e)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void findIntersections(E ent, Consumer<? super E> consumer) {
        if (hash.isEmpty()) {
            return;
        }

        ent.getHitboxTo(tmp0);
        int dx = (int) ((tmp0.width / resolution) + 1);
        int dy = (int) ((tmp0.height / resolution) + 1);

        int px = (int) (tmp0.x / resolution);
        int py = (int) (tmp0.y / resolution);

        for (int sx = -dx; sx <= dx; sx++) {
            for (int sy = -dy; sy <= dy; sy++) {
                int rx = px + sx;
                int ry = py + sy;

                var array = hash.get(combine(rx, ry));
                if (array != null) {
                    for (E e : array) {
                        if (e == ent) {
                            continue;
                        }
                        e.getHitboxTo(tmp1);
                        if (tmp0.overlaps(tmp1)) {
                            consumer.accept(e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return hash.toString();
    }
}
