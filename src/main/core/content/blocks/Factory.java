package core.content.blocks;

import core.content.ContentLoader;
import core.content.ContentResolver;
import core.content.ItemStack;
import core.content.ItemStackPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import static core.math.MathUtil.toShortExact;

public class Factory extends Block {
    public float maxHp;
    public float productionSpeed; // За сколько 1 предмет производится в мс
    public short maxItemCapacity;
    public @Nullable Malfunction malfunction;
    public ItemStackPredicate[] input, fuel;
    public ItemStack[] output;

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
        this.maxItemCapacity = toShortExact(json.path("MaxStoredObjects").intValue());
        this.productionSpeed = json.path("ProductionSpeed").floatValue();

        this.input = cnt.readItemStacksPredicatesUnresolved(json.path("Input"));
        this.fuel = cnt.readItemStacksPredicatesUnresolved(json.path("Fuel"));
        this.output = cnt.readItemStacksUnresolved(json.path("Output"));
    }

    @Override
    public void resolve(ContentResolver res) {
        super.resolve(res);
        res.resolveItemStacksPredicates(input);
        res.resolveItemStacksPredicates(fuel);
        res.resolveItemStacks(output);
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
