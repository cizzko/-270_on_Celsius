package core.content;

import core.content.entity.Entity;
import core.content.entity.LivingEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Consumer;

public final class EntityPool {
    private Entity[] entities;
    private final EntityIndex index = new EntityIndex();
    private final ShortArrayList freeIds = new ShortArrayList();
    private final ObjectArrayList<Entity> pendingAddList = new ObjectArrayList<>();
    private final int maxCreatureCount;

    private short idCounter = 0;
    private boolean needIndexRebuild, iterating;

    public EntityPool(int maxCreatureCount) {
        this.maxCreatureCount = maxCreatureCount;
        this.entities = new Entity[16]; // Базовый размер. Пока что никогда не уменьшает свой размер
    }

    public int entityCount() { return idCounter - freeIds.size(); }

    public void updatePositions() {
        needIndexRebuild = false;
        index.clear();
        index.presize(entityCount());
        forEachImpl(entity -> index.insert((LivingEntity) entity)); // при CME мы сами себе злобные буратины
        index.build();
    }

    public void add(Entity ent) {
        if (!iterating) {
            entities[ent.id()] = ent;
            needIndexRebuild = true;
        } else {
            pendingAddList.add(ent);
        }
    }

    private void forEachImpl(Consumer<Entity> consumer) {
        Entity[] entArray = entities;
        for (short i = 0, n = idCounter; i < n; ++i) {
            Entity entity = entArray[i];
            if (entity != null) {
                consumer.accept(entity);
            }
        }
    }

    public <C extends Entity> void forEachType(Class<C> type, Consumer<C> consumer) {
        Entity[] entArray = entities;
        beginIterating();
        try {
            for (short i = 0, n = idCounter; i < n; ++i) {
                Entity entity = entArray[i];
                if (type.isInstance(entity)) {
                    consumer.accept(type.cast(entity));
                }
            }
        } finally {
            endIterating(entArray);
        }
    }

    public void forEach(Consumer<Entity> consumer) {
        Entity[] entArray = entities;
        beginIterating();
        try {
            for (short i = 0, n = idCounter; i < n; ++i) {
                Entity entity = entArray[i];
                if (entity != null) {
                    consumer.accept(entity);
                }
            }
        } finally {
            endIterating(entArray);
        }
    }

    private void beginIterating() {
        iterating = true;
    }

    private void endIterating(Entity[] entArray) {
        iterating = false;
        if (pendingAddList.isEmpty()) {
            return;
        }
        needIndexRebuild = true;
        for (int i = 0, n = pendingAddList.size(); i < n; i++) {
            var newEnt = pendingAddList.get(i);
            entArray[newEnt.id()] = newEnt;
        }
        pendingAddList.clear();
    }

    public boolean exists(short id) {
        return (id >= 0 && id < idCounter) && entities[id] != null;
    }

    public @Nullable Entity getEntity(short id) {
        if (id < 0 || id >= idCounter) return null;
        return entities[id];
    }

    public short acquireId() {
        if (freeIds.isEmpty()) {
            if (idCounter >= maxCreatureCount) {
                throw new IllegalStateException("Maximum number of creatures exceeded");
            }
            short newId = idCounter++;
            ensureCapacity(newId);
            return newId;
        }
        return freeIds.popShort();
    }

    public EntityIndex index() {
        if (needIndexRebuild) {
            updatePositions();
        }
        return index;
    }

    public void releaseId(Entity entity) {
        freeIds.push(entity.id());
        entities[entity.id()] = null;
        needIndexRebuild = true;
    }

    public void clear() {
        Arrays.fill(entities, null);
        index.clear();
        freeIds.clear();
        idCounter = 0;
        needIndexRebuild = false;
    }

    private void ensureCapacity(short id) {
        if (id >= entities.length) {
            int newCapacity = Math.min(entities.length * 2, maxCreatureCount);
            this.entities = Arrays.copyOf(entities, newCapacity);
        }
    }
}
