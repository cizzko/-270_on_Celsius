package core.content;

import core.World.ContentLoader;
import core.World.ContentResolver;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.Structures.Factories;

import java.util.Locale;

public class Factory extends StaticObjectsConst {
    public float needEnergy, maxHp;
    public int productionSpeed;
    public int maxItemCapacity;
    public Factories.Breaking breakingType;
    public ItemStack[] input, output, fuel;

    public Factory(String id) {
        super(id);
    }

    @Override
    public void load(ContentLoader cnt) {
        super.load(cnt);
        var json = cnt.node();
        this.breakingType = Factories.Breaking.valueOf(json.path("breaking").asText("CRITICAL").toUpperCase(Locale.ROOT));
        this.needEnergy = json.path("need-energy").floatValue();
        this.maxHp = json.path("max-hp").floatValue();
        this.maxItemCapacity = json.path("max-item-capacity").intValue();
        this.productionSpeed = json.path("production-speed").intValue();

        this.input = cnt.readItemStacksUnresolved(json.path("input"));
        this.output = cnt.readItemStacksUnresolved(json.path("output"));
        this.fuel = cnt.readItemStacksUnresolved(json.path("fuel"));
    }

    @Override
    public void resolve(ContentResolver res) {
        super.resolve(res);
        this.input = res.resolveItemStacks(input);
        this.output = res.resolveItemStacks(output);
        this.fuel = res.resolveItemStacks(fuel);
    }
}
