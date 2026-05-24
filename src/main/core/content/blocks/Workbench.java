package core.content.blocks;

import core.content.ContentLoader;

import java.util.Locale;

public class Workbench extends Block {

    public Tier tier;

    public Workbench(String id) {
        super(id);
    }

    @Override
    public void load(ContentLoader cnt) {
        super.load(cnt);
        tier = Tier.valueOf(cnt.node().required("Tier").asText().toUpperCase(Locale.ROOT));
    }

    @Override
    protected WorkbenchEntity constructEntity() {
        return new WorkbenchEntity(this);
    }

    public enum Tier {
        SMALL,
        MEDIUM,
        LARGE
    }
}
