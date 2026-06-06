package core.content;

import core.math.Rectangle;
import core.content.entity.Entity;
import core.util.QuadTree;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import org.jetbrains.annotations.Nullable;

public class EntityPool {
    private final Int2ObjectOpenHashMap<Entity> entities = new Int2ObjectOpenHashMap<>();
    private final QuadTree<Entity> worldIndex = new QuadTree<>(new Rectangle());
    private final IntArrayFIFOQueue freeIds = new IntArrayFIFOQueue();
    private final int maxCreatureCount;

    private int idCounter = 0;
    private boolean needIndexRebuild;

    public EntityPool(int maxCreatureCount) {
        this.maxCreatureCount = maxCreatureCount;
    }

    public void updatePositions() {
        needIndexRebuild = false;
        worldIndex.clear();
        entities.values().forEach(worldIndex::insert);
    }

    public void add(Entity ent) {
        entities.put(ent.id(), ent);
        needIndexRebuild = true;
    }

    public Int2ObjectOpenHashMap<Entity> entities() { return entities; }

    public boolean exists(int id) { return entities.containsKey(id); }

    public @Nullable Entity getEntity(int id) {
        return entities.get(id);
    }

    public int acquireId() {
        if (freeIds.isEmpty()) {
            if (idCounter >= maxCreatureCount) {
                throw new IllegalStateException("Maximum number of creatures exceeded");
            }
            return idCounter++;
        }
        return freeIds.dequeueInt();
    }

    public QuadTree<Entity> worldIndex() {
        if (needIndexRebuild) {
            updatePositions();
        }
        return worldIndex;
    }

    public void releaseId(Entity creature) {
        freeIds.enqueue(creature.id());
        entities.remove(creature.id());
        needIndexRebuild = true;
    }

    public void clear() {
        entities.clear();
        worldIndex.clear();
        freeIds.clear();
        idCounter = 0;
        needIndexRebuild = false;
    }
}
