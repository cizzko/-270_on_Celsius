package core.content.blocks;

import core.content.ContentLoader;
import core.content.ContentResolver;
import core.content.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class Factory extends Block {
    public float maxHp;
    public float productionSpeed; // За сколько 1 предмет производится в мс
    public int maxItemCapacity;
    public @Nullable Malfunction malfunction;
    public ItemStack[] input, output, fuel;

    public Factory(String id) {
        super(id);
    }

    @Override
    public void load(ContentLoader cnt) {
        super.load(cnt);
        var json = cnt.node();
        var malfunctionNode = json.path("Malfunction").asText(null);
        if (malfunctionNode != null) {
            this.malfunction = Malfunction.valueOf(malfunctionNode.toUpperCase(Locale.ROOT));
        }
        this.maxHp = json.path("MaxHp").floatValue();
        this.maxItemCapacity = json.path("MaxStoredObjects").intValue();
        this.productionSpeed = json.path("ProductionSpeed").floatValue();

        this.input = cnt.readItemStacksUnresolved(json.path("Input"));
        this.output = cnt.readItemStacksUnresolved(json.path("Output"));
        this.fuel = cnt.readItemStacksUnresolved(json.path("Fuel"));
    }

    @Override
    public void resolve(ContentResolver res) {
        super.resolve(res);
        this.input = res.resolveItemStacks(input);
        this.output = res.resolveItemStacks(output);
        this.fuel = res.resolveItemStacks(fuel);
    }

    @Override
    protected FactoryEntity constructEntity() { return new FactoryEntity(this); }

    public enum Malfunction {
        WEAK_SLOW, // slow working
        WEAK_OVERCONSUMPTION, // high consumption
        AVERAGE_STOP, // stop working
        AVERAGE_MISWORKING, // misworking
        CRITICAL // full stop working, need rebuild
    }
}
