package core.content.blocks;

import core.content.*;
import core.content.entity.BlockEntity;
import core.g2d.Atlas;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import static core.WorldCoordinates.toWorld;

public class Block implements ContentType, Loadable {
    public static Block AIR;

    public final String id;

    public byte tileCountX, tileCountY;

    public int maxHp;
    public float density, resistance;
    public int lightTransmission;
    public Atlas.Region texture;
    public ItemStack[] requirements;
    public @Nullable Block createWith;
    public Type type;

    public Block(String id) {
        this.id = id;
    }

    @Override
    @MustBeInvokedByOverriders
    public void load(ContentLoader cnt) {
        this.maxHp = cnt.node().path("MaxHp").asInt(100);
        this.texture = cnt.readTexture("Texture");
        this.requirements = cnt.readItemStacksUnresolved(cnt.node().path("Requirements"));

        this.tileCountX = (byte) toWorld(texture.width());
        this.tileCountY = (byte) toWorld(texture.height());

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

    @Override
    public final String id() {
        return id;
    }

    public boolean isMultiblock() { return tileCountX > 1 || tileCountY > 1; }

    public @Nullable BlockEntity createEntity(int x, int y) {
        var ent = constructEntity();
        if (ent != null) {
            ent.setPosition(x, y);
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
        return id.equals(that.id);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "StaticObjectsConst['" + id + "']";
    }

    public enum Type {
        GAS,
        LIQUID,
        SOLID,    // Твёрдая поверхность на которой можно стоять и строить
        PLASMA,
        WALKABLE  // Листва, паутина и остальные блоки, через которые можно проходить (падать)
    }
}
