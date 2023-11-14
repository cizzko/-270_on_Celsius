package core.World.Creatures;

import java.util.HashMap;
import static core.World.HitboxMap.*;
import static core.World.WorldGenerator.*;

public class ButterflyLogic {
    private static final HashMap<DynamicWorldObjects, Long> lastSwapPos = new HashMap<>();
    private static final int swapPosTime = 200;

    //TODO: rewrite
    public static void update(DynamicWorldObjects object) {
        if (object.path.contains("butterfly")) {
            if (lastSwapPos.get(object) != null) {
                if (System.currentTimeMillis() - lastSwapPos.get(object) >= swapPosTime) {
                    int randX = (int) (Math.random() * 60) - 30;
                    int randY = (int) (Math.random() * 60) - 30;

                    if (randX + object.x < SizeX * 16 - 32 && randY + object.y < SizeY * 16 - 32 && randX + object.x > 32 && randY + object.y > SizeY / 2f * 16) {
                        object.x += (randX < 0 && !checkIntersStaticL(object.x, object.y, 32)) || (randX > 0 && !checkIntersStaticR(object.x, object.y, 32, 32)) ? randX : 0;
                        object.y += (randY < 0 && !checkIntersStaticD(object.x, object.y, 32, 32)) || (randY > 0 && !checkIntersStaticU(object.x, object.y, 32, 32)) ? randY : 0;
                    }
                    lastSwapPos.put(object, System.currentTimeMillis());
                }
            } else {
                lastSwapPos.put(object, System.currentTimeMillis());
            }
        }
    }
}