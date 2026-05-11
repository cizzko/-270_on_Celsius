package core.World.StaticWorldObjects;

import core.World.ContentLoader;
import core.World.ContentResolver;
import core.World.ContentType;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.Textures.TextureDrawing;
import core.entity.BlockEntity;
import core.g2d.Atlas;

import java.util.Locale;

public class StaticObjectsConst implements ContentType {
    public static StaticObjectsConst AIR;

    public final String id;

    public int tileCountX, tileCountY;

    public int maxHp;
    public float density, resistance;
    public int lightTransmission;
    public boolean hasMotherBlock;
    public Atlas.Region texture;
    public ItemStack[] requirements;
    public StaticObjectsConst createWith;
    public Types type;

    public StaticObjectsConst(String id) {
        this.id = id;
    }

    @Override
    public void load(ContentLoader cnt) {
        this.maxHp = cnt.node().path("MaxHp").asInt(100);
        this.texture = cnt.readTexture("Texture");
        this.requirements = cnt.readItemStacksUnresolved(cnt.node().path("Requirements"));

        this.tileCountX = texture.width() / TextureDrawing.blockSize;
        this.tileCountY = texture.height() / TextureDrawing.blockSize;

        String createWithId = cnt.node().path("CreateWith").asText(null);
        this.createWith = (createWithId == null || createWithId.equals("player")) ? null : cnt.readBlockUnresolved("CreateWith");

        this.hasMotherBlock = (cnt.node().path("HasMotherBlock").asBoolean(false));
        this.density = (float) cnt.node().path("Density").asDouble(1);
        this.resistance = (float) cnt.node().path("Resistance").asDouble(90);
        this.lightTransmission = (cnt.node().path("LightTransmission").asInt(100));
        this.type = Types.valueOf(cnt.node().path("Type").asText(Types.SOLID.name()).toUpperCase(Locale.ROOT));
    }

    @Override
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

    public TileData createData() {
        return switch (id) {
            case "workbenchSmall" -> new TileData.Workbench(TileData.Workbench.Type.SMALL);
            default -> null;
        };
    }

    public /* @Nullable */ BlockEntity createEntity() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StaticObjectsConst that)) {
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
        PLASMA,
        WALKABLE  // Листва, паутина и остальные блоки, через которые можно проходить (падать)
    }
}
