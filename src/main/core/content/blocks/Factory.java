package core.content.blocks;

import core.World.ContentLoader;
import core.World.ContentResolver;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class Factory extends StaticObjectsConst {
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

    /*

    public void breakFactory(Malfunction breakingType) {
        this.breakingType = breakingType;

        switch (breakingType) {
            case WEAK_SLOW -> maxProductionProgress *= (int) ((Math.random() * 4) + 1);
            case AVERAGE_STOP -> maxProductionProgress = Integer.MAX_VALUE;
            case AVERAGE_MISWORKING -> System.arraycopy(outputObjects, 0, outputObjects, 0, outputObjects.length - 1);
            case WEAK_OVERCONSUMPTION -> needEnergy *= (float) ((Math.random() * 4) + 1);
        }
    }

    public void removeBreakEffect(Malfunction breakingType) {
        this.breakingType = (breakingType == Malfunction.CRITICAL ? Malfunction.CRITICAL : null);

        switch (breakingType) {
            case WEAK_SLOW, AVERAGE_STOP -> maxProductionProgress = Integer.parseInt(Config.getProperties(path).get("ProductionSpeed"));
            case AVERAGE_MISWORKING -> outputObjects = transformItemStack(Config.getProperties(path).get("OutputObjects"));
            case WEAK_OVERCONSUMPTION -> needEnergy = Integer.parseInt(Config.getProperties(path).get("NeedEnergy"));
        }
    }
    */
}
