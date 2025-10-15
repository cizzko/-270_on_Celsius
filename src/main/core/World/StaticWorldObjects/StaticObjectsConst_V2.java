package core.World.StaticWorldObjects;

import core.World.ContentLoader;
import core.World.ContentResolver;
import core.World.ContentType;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.Textures.TextureDrawing;
import core.entity.BlockEntity;
import core.entity.TileEntity;
import core.g2d.Atlas;

import java.util.Locale;

public class StaticObjectsConst_V2 implements ContentType {
    public final String id;

    // public short[][] optionalTiles; TODO
    public float maxHp, density, resistance;
    public int lightTransmission;
    public boolean hasMotherBlock;
    public Atlas.Region texture;
    public ItemStack[] requirements;
    public Types type;

    public StaticObjectsConst_V2(String id) {
        this.id = id;
    }

    @Override
    public void load(ContentLoader cnt) {
        this.maxHp = cnt.node().path("max-hp").asInt(100);
        this.texture = cnt.readTexture("texture");
        this.requirements = cnt.readItemStacksUnresolved(cnt.node().path("requirements"));

        this.hasMotherBlock = (cnt.node().path("HasMotherBlock").asBoolean(false));
        this.density = (float) cnt.node().path("density").asDouble(1);
        this.resistance = (float) cnt.node().path("resistance").asDouble(100);
        this.lightTransmission = (cnt.node().path("light-transmission").asInt(100));
        this.type = Types.valueOf(cnt.node().path("type").asText(Types.SOLID.name()).toUpperCase(Locale.ROOT));
    }

    @Override
    public void resolve(ContentResolver res) {
        this.requirements = res.resolveItemStacks(requirements);
    }

    @Override
    public final String id() {
        return id;
    }

    public BlockEntity createEntity(int x, int y) {
        var ent = new TileEntity(this);
        ent.setPosition(x * TextureDrawing.blockSize, y * TextureDrawing.blockSize); // TODO а где функция та?
        ent.setHp(maxHp);
        return ent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StaticObjectsConst_V2 that)) {
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "StaticObjectsConst['" + id + "']";
    }

    public enum Types {
        GAS,
        LIQUID,
        SOLID,
        PLASMA
    }
}
