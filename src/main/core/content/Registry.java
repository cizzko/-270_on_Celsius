package core.content;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.Nullable;

public final class Registry<T extends ContentType> {

    private final Object2ObjectAVLTreeMap<String, T> name2Type;
    private final Object2IntOpenHashMap<T> type2Id;
    private final T[] id2Type;

    Registry(Object2ObjectAVLTreeMap<String, T> name2Type, Object2IntOpenHashMap<T> type2Id, T[] id2Type) {
        this.name2Type = name2Type;
        this.type2Id = type2Id;
        this.id2Type = id2Type;
    }

    public T typeByName(String name) {
        return name2Type.get(name);
    }

    public int idByType(T type) {
        return type2Id.getOrDefault(type, -1);
    }

    public T typeById(int id) {
        return id2Type[id];
    }

    public @Nullable T typeByIdNull(int id) {
        if (id < 0 || id >= id2Type.length) {
            return null;
        }
        return typeById(id);
    }

    public int maxId() {
        return id2Type.length - 1;
    }

    public int count() {
        return id2Type.length;
    }

    public ObjectSet<T> values() {
        return type2Id.keySet();
    }
}