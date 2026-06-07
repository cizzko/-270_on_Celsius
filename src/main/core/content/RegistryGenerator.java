package core.content;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;

import java.lang.reflect.Array;

import static core.math.MathUtil.toShortExact;

final class RegistryGenerator<C extends ContentType> {
    final Class<C> type;
    final Object2ObjectOpenHashMap<String, C> name2Type = new Object2ObjectOpenHashMap<>();
    final Object2ShortOpenHashMap<C> type2Id = new Object2ShortOpenHashMap<>();
    final C[] id2Type;
    int id;

    @SuppressWarnings("unchecked")
    RegistryGenerator(Class<C> type, int count) {
        this.type = type;
        this.id2Type = (C[]) Array.newInstance(type, count);
    }

    void putName(C type) {
        name2Type.put(type.key(), type);
    }

    void putId(C type) {
        short cid = toShortExact(id);
        type.setId(cid);
        type2Id.put(type, cid);
        id2Type[id] = type;
        id++;
    }

    Registry<C> complete() {
        type2Id.trim();
        return new Registry<>(name2Type, type2Id, id2Type);
    }
}
