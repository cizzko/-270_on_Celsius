package core.content.block;

import core.World.ContentLoader;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.entity.BlockEntity;

public class Chest extends StaticObjectsConst {

    public int width, height;

    public Chest(String id) {
        super(id);
    }

    @Override
    public void load(ContentLoader cnt) {
        super.load(cnt);
        var sizeNode = cnt.node().required("Size");
        width = sizeNode.required("Width").asInt();
        if (width <= 0) throw cnt.malformed("'Size.Width' must be greater than 0");
        height = sizeNode.required("Height").asInt();
        if (height <= 0) throw cnt.malformed("'Size.Height' must be greater than 0");
    }

    @Override
    protected BlockEntity constructEntity() {
        return super.constructEntity();
    }
}
