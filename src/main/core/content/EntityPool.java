package core.content;

import core.content.blocks.HashSpatialIndex;
import core.entity.CreatureEntity;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import org.jetbrains.annotations.Nullable;

public class EntityPool {
    private final Int2ObjectAVLTreeMap<CreatureEntity> entities = new Int2ObjectAVLTreeMap<>();
    private final HashSpatialIndex<CreatureEntity> worldIndex = new HashSpatialIndex<>(4);
    private final IntArrayFIFOQueue freeIds = new IntArrayFIFOQueue();
    private final int maxCreatureCount;

    private int idCounter = 0;

    public EntityPool(int maxCreatureCount) {
        this.maxCreatureCount = maxCreatureCount;
    }

    public void update() {
        worldIndex.clear();
        entities.values().forEach(worldIndex::insert);
    }

    public void add(CreatureEntity ent) {
        entities.put(ent.getId(), ent);
    }

    public Int2ObjectAVLTreeMap<CreatureEntity> entities() { return entities; }

    public @Nullable CreatureEntity getEntity(int id) {
        return entities.get(id);
    }

    public int acquireId() {
        if (freeIds.isEmpty()) {
            if (idCounter > maxCreatureCount) {
                throw new IllegalStateException();
            }
            return idCounter++;
        }
        return freeIds.dequeueInt();
    }

    public HashSpatialIndex<CreatureEntity> worldIndex() {
        return worldIndex;
    }

    public void releaseId(CreatureEntity creature) {
        freeIds.enqueue(creature.getId());
        entities.remove(creature.getId());
    }
}
