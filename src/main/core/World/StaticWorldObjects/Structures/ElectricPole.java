package core.World.StaticWorldObjects.Structures;

import core.World.StaticWorldObjects.StaticBlocksEvents;
import core.World.StaticWorldObjects.StaticWorldObjects;
import java.util.Objects;
import static core.Window.defPath;

public class ElectricPole implements StaticBlocksEvents {
    @Override
    public void placeStatic(int cellX, int cellY, short id) {
        if (id != 0 && Objects.requireNonNull(StaticWorldObjects.getPath(id)).substring(defPath.length()).toLowerCase().contains("electricpole")) {

        }
    }

    @Override
    public void destroyStatic(int cellX, int cellY, short id) {
        if (id != 0 && Objects.requireNonNull(StaticWorldObjects.getPath(id)).substring(defPath.length()).toLowerCase().contains("electricpole")) {
        }
    }
}
