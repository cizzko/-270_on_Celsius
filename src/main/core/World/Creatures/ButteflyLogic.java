package core.World.Creatures;

import static core.World.HitboxMap.*;
import static core.World.WorldGenerator.*;
import static core.World.WorldGenerator.DynamicObjects;

public class ButteflyLogic {
    private static long deltaTime = System.currentTimeMillis();

    public static void update() {
        if (System.currentTimeMillis() - deltaTime > 100) {
            deltaTime = System.currentTimeMillis();

            for (DynamicWorldObjects dynamicObject : DynamicObjects) {
                if (dynamicObject != null && dynamicObject.path.contains("butterfly") && dynamicObject.animSpeed != 0) {
                    int randX = (int) (Math.random() * 60) - 30;
                    int randY = (int) (Math.random() * 60) - 30;

                    if (randX + dynamicObject.x < SizeX * 16 - 32 && randY + dynamicObject.y < SizeY * 16 - 32 && randX + dynamicObject.x > 32 && randY + dynamicObject.y > SizeY / 2f * 16) {
                        dynamicObject.x += (randX < 0 && !checkIntersStaticL(dynamicObject.x, dynamicObject.y, 32)) || (randX > 0 && !checkIntersStaticR(dynamicObject.x, dynamicObject.y, 32, 32)) ? randX : 0;
                        dynamicObject.y += (randY < 0 && !checkIntersStaticD(dynamicObject.x, dynamicObject.y, 32, 32)) || (randY > 0 && !checkIntersStaticU(dynamicObject.x, dynamicObject.y, 32, 32)) ? randY : 0;
                    }
                }
            }
        }
    }
}