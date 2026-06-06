package core.content;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.lang.reflect.Array;

final class RegistryGenerator<C extends ContentType> {
    final Class<C> type;
    final Object2ObjectOpenHashMap<String, C> name2Type = new Object2ObjectOpenHashMap<>();
    final Object2IntOpenHashMap<C> type2Id = new Object2IntOpenHashMap<>();
    final C[] id2Type;
    int id;

    @SuppressWarnings("unchecked")
    RegistryGenerator(Class<C> type, int count) {
        this.type = type;
        this.id2Type = (C[]) Array.newInstance(type, count);
    }

    void putName(C type) {
        name2Type.put(type.id(), type);
    }

    void putId(C type) {
        type2Id.put(type, id);
        id2Type[id] = type;
        id++;
    }

    Registry<C> complete() {
        type2Id.trim();
        return new Registry<>(name2Type, type2Id, id2Type);
    }
}
