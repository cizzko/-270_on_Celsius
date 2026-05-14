package core.content;

import core.content.blocks.HashSpatialIndex;
import core.content.entity.Entity;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import org.jetbrains.annotations.Nullable;

import static core.World.Textures.TextureDrawing.blockSize;

public class EntityPool {
    private final Int2ObjectAVLTreeMap<Entity> entities = new Int2ObjectAVLTreeMap<>();
    private final HashSpatialIndex<Entity> worldIndex = new HashSpatialIndex<>(blockSize * 16);
    private final IntArrayFIFOQueue freeIds = new IntArrayFIFOQueue();
    private final int maxCreatureCount;

    private int idCounter = 0;
    private boolean needIndexRebuild;

    public EntityPool(int maxCreatureCount) {
        this.maxCreatureCount = maxCreatureCount;
    }

    public void updatePositions() {
        needIndexRebuild = false;
        worldIndex.hash.clear();
        entities.values().forEach(worldIndex::insert);
    }

    public void add(Entity ent) {
        entities.put(ent.getId(), ent);
        needIndexRebuild = true;
    }

    public Int2ObjectAVLTreeMap<Entity> entities() { return entities; }

    public @Nullable Entity getEntity(int id) {
        return entities.get(id);
    }

    public int acquireId() {
        if (freeIds.isEmpty()) {
            if (idCounter > maxCreatureCount) {
                throw new IllegalStateException("Maximum number of creatures exceeded");
            }
            return idCounter++;
        }
        return freeIds.dequeueInt();
    }

    public HashSpatialIndex<Entity> worldIndex() {
        if (needIndexRebuild) {
            updatePositions();
        }
        return worldIndex;
    }

    public void releaseId(Entity creature) {
        freeIds.enqueue(creature.getId());
        entities.remove(creature.getId());
        needIndexRebuild = true;
    }
}
