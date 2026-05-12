package core.content;

import core.World.ContentType;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public final class Registry<T extends ContentType> {

    private final Object2ObjectAVLTreeMap<String, T> name2Type = new Object2ObjectAVLTreeMap<>();
    private final Object2IntOpenHashMap<T> type2Id = new Object2IntOpenHashMap<>();
    private final Int2ObjectOpenHashMap<T> id2Type = new Int2ObjectOpenHashMap<>();
    private int id;

    public T typeByName(String name) {
        return name2Type.get(name);
    }

    public int idByType(T type) {
        return type2Id.getOrDefault(type, -1);
    }

    public T typeById(int id) {
        return id2Type.get(id);
    }

    public void put1(T type) {
        name2Type.put(type.id(), type);
    }

    public void put2(T type) {
        type2Id.put(type, id);
        id2Type.put(id, type);
        id++;
    }

    public int getMaxId() {
        return id;
    }

    public void trim() {
        type2Id.trim();
        id2Type.trim();
    }

    public ObjectSet<T> values() {
        return type2Id.keySet();
    }
}
