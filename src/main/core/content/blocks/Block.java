package core.content.blocks;

import core.content.*;
import core.content.entity.BlockEntity;
import core.g2d.Atlas;
import core.math.MathUtil;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import static core.WorldCoordinates.toBlock;
import static core.WorldCoordinates.toWorld;
import static core.math.MathUtil.toByteExact;
import static core.math.MathUtil.toShortExact;

public class Block implements ContentType, Loadable {
    public static Block AIR;

    public final String key;
    public short id;

    public byte tileCountX, tileCountY;

    public short maxHp;
    public float density; // unused
    public float resistance;
    public int lightTransmission; // unused
    public Atlas.Region texture;
    public ItemStack[] requirements;
    public @Nullable Block createWith;
    public Type type;

    public Block(String key) {
        this.key = key;
    }

    @Override
    @MustBeInvokedByOverriders
    public void load(ContentLoader cnt) {
        this.maxHp = toShortExact(cnt.node().path("MaxHp").asInt(100));
        this.texture = cnt.readTexture("Texture");
        this.requirements = cnt.readItemStacksUnresolved(cnt.node().path("Requirements"));

        this.tileCountX = toByteExact(toBlock(toWorld(texture.width())));
        this.tileCountY = toByteExact(toBlock(toWorld(texture.height())));

        String createWithId = cnt.node().path("CreateWith").asText(null);
        this.createWith = (createWithId == null || createWithId.equals("player")) ? null : cnt.readBlockUnresolved("CreateWith");

        this.density = (float) cnt.node().path("Density").asDouble(1);
        this.resistance = (float) cnt.node().path("Resistance").asDouble(90);
        this.lightTransmission = cnt.node().path("LightTransmission").asInt(100);
        this.type = Type.valueOf(cnt.node().path("Type").asText(Type.SOLID.name()).toUpperCase(Locale.ROOT));
    }

    @Override
    @MustBeInvokedByOverriders
    public void resolve(ContentResolver res) {
        this.requirements = res.resolveItemStacks(requirements);
        if (createWith != null) {
            this.createWith = res.resolveBlock(createWith);
        }
    }

    public final String key() {
        return key;
    }

    public final short id() { return id; }
    public final void setId(short id) { this.id = id; }

    public boolean isMultiblock() { return tileCountX > 1 || tileCountY > 1; }

    public @Nullable BlockEntity createEntity(int x, int y) {
        var ent = constructEntity();
        if (ent != null) {
            ent.setPosition(MathUtil.toShortExact(x), MathUtil.toShortExact(y));
            ent.init();
        }
        return ent;
    }

    protected @Nullable BlockEntity constructEntity() { return null; }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Block that)) {
            return false;
        }
        return key.equals(that.key);
    }

    @Override
    public final int hashCode() {
        return key.hashCode();
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "['" + key + "']";
    }

    // Никогда не переставляйте порядок констант в этом перечислении
    // От этого зависит работа ContentManager
    public enum Type {
        GAS,
        LIQUID,
        SOLID,    // Твёрдая поверхность на которой можно стоять и строить
        PLASMA,
        WALKABLE;  // Листва, паутина и остальные блоки, через которые можно проходить (падать)

        public static final Type[] VALUES = values();
    }
}
